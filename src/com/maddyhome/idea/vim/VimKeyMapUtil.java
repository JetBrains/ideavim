package com.maddyhome.idea.vim;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.maddyhome.idea.vim.ui.VimKeymapDialog;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * @author oleg
 */
public class VimKeyMapUtil {
  private static final Joiner PATH_JOINER = Joiner.on(File.separatorChar);
  public static final String VIM_KEYMAP_NAME = "Vim";

  private static final String VIM_XML = "Vim.xml";
  private static final String KEYMAPS_PATH = PATH_JOINER.join(PathManager.getConfigPath(), "keymaps");
  private static final String INSTALLED_VIM_KEYMAP_PATH = PATH_JOINER.join(KEYMAPS_PATH, VIM_XML);

  private static Logger LOG = Logger.getInstance(VimKeyMapUtil.class);

  public static boolean isVimKeymapInstalled() {
    return KeymapManager.getInstance().getKeymap(VIM_KEYMAP_NAME) != null;
  }

  /**
   * @return true if keymap was installed or was successfully installed
   */
  public static boolean installKeyBoardBindings() {
    LOG.debug("Check for keyboard bindings");
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    if (localFileSystem.refreshAndFindFileByPath(KEYMAPS_PATH) == null) {
      reportError("Failed to install vim keymap. Empty keymaps folder");
      return false;
    }

    LOG.debug("No vim keyboard installed found. Installing");
    try {
      final byte[] bytes = toByteArray(retrieveSourceKeymapStream());
      Files.write(bytes, new File(INSTALLED_VIM_KEYMAP_PATH));
      final Document document = StorageUtil.loadDocument(bytes);
      if (document != null && !ApplicationManager.getApplication().isUnitTestMode()) {
        // Prompt user to select the parent for the Vim keyboard
        configureVimParentKeymap(INSTALLED_VIM_KEYMAP_PATH, document, true);
      }
      installKeymap(document);
    } catch (IOException e) {
      reportError("Source keymap not found", e);
      return false;
    } catch (InvalidDataException e) {
      reportError("Failed to install vim keymap. Vim.xml file is corrupted", e);
      return false;
    } catch (Exception e) {
      reportError("Failed to install vim keymap.\n", e);
      return false;
    }

    return true;
  }

  private static void installKeymap(@Nullable Document document) throws InvalidDataException {
    if (document == null) {
      throw new InvalidDataException();
    }
    final KeymapImpl vimKeyMap = new KeymapImpl();
    final KeymapManagerImpl keymapManager = (KeymapManagerImpl) KeymapManager.getInstance();
    final Keymap[] allKeymaps = keymapManager.getAllKeymaps();
    vimKeyMap.readExternal(document.getRootElement(), allKeymaps);
    keymapManager.addKeymap(vimKeyMap);
  }

  /**
   * Changes parent keymap for the Vim
   *
   * @return true if document was changed successfully
   */
  private static boolean configureVimParentKeymap(final String path, @NotNull final Document document, final boolean showNotification) throws IOException {
    final Element rootElement = document.getRootElement();
    final String parentKeymap = rootElement.getAttributeValue("parent");
    final VimKeymapDialog vimKeymapDialog = new VimKeymapDialog(parentKeymap);
    vimKeymapDialog.show();
    if (vimKeymapDialog.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return false;
    }
    rootElement.removeAttribute("parent");
    final Keymap selectedKeymap = vimKeymapDialog.getSelectedKeymap();
    final String keymapName = selectedKeymap.getName();
    rootElement.setAttribute("parent", keymapName);
    VimPlugin.getInstance().setPreviousKeyMap(keymapName);
    VimKeymapConflictResolveUtil.resolveConflicts(rootElement, selectedKeymap);
    // Save modified keymap to the file
    JDOMUtil.writeDocument(document, path, "\n");
    if (showNotification) {
      Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                "Successfully configured vim keymap to be based on " +
                                                selectedKeymap.getPresentableName(),
                                                NotificationType.INFORMATION));
    }

    return true;
  }

  public static boolean isVimKeymapUsed() {
    return KeymapManager.getInstance().getActiveKeymap().getName().equals(VIM_KEYMAP_NAME);
  }

  /**
   * @return true if keymap was switched successfully, false otherwise
   */
  public static boolean switchKeymapBindings(final boolean enableVimKeymap) {
    LOG.debug("Enabling keymap");

    // In case if Vim keymap is already in use or we don't need it, we have nothing to do
    if (isVimKeymapUsed() == enableVimKeymap) {
      return false;
    }

    final KeymapManagerImpl manager = (KeymapManagerImpl) KeymapManager.getInstance();
    // Get keymap to enable
    final String keymapName2Enable = enableVimKeymap ? VIM_KEYMAP_NAME : VimPlugin.getInstance().getPreviousKeyMap();
    if (keymapName2Enable.isEmpty()) {
      return false;
    }
    if (keymapName2Enable.equals(manager.getActiveKeymap().getName())) {
      return false;
    }

    LOG.debug("Enabling keymap:" + keymapName2Enable);
    final Keymap keymap = manager.getKeymap(keymapName2Enable);
    if (keymap == null) {
      reportError("Failed to enable keymap: " + keymapName2Enable);
      return false;
    }

    // Save previous keymap to enable after VIM emulation is turned off
    if (enableVimKeymap) {
      VimPlugin.getInstance().setPreviousKeyMap(manager.getActiveKeymap().getName());
    }

    manager.setActiveKeymap(keymap);

    final String keyMapPresentableName = keymap.getPresentableName();
    Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                              keyMapPresentableName + " keymap was successfully enabled", NotificationType.INFORMATION));
    LOG.debug(keyMapPresentableName + " keymap was successfully enabled");
    return true;
  }

  @NotNull
  private static InputStream retrieveSourceKeymapStream() throws IOException {
    String keymapPath = PATH_JOINER.join(PathManager.getPluginsPath(), VimPlugin.IDEAVIM_NOTIFICATION_TITLE, VIM_XML);
    try {
      return new FileInputStream(keymapPath);
    } catch (FileNotFoundException e) {
      if (ApplicationManager.getApplication().isInternal()) {
        LOG.debug("Development mode on. Trying to retrieve source keymap from resources");
        return Resources.getResource(VimKeyMapUtil.class, "/" + VIM_XML).openStream();
      }
      throw e;
    }
  }

  private static void reportError(final String message) {
    reportError(message, null);
  }

  private static void reportError(final String message, @Nullable final Exception e) {
    LOG.error(message, e);
    Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                              message + String.valueOf(e), NotificationType.ERROR));
  }
}