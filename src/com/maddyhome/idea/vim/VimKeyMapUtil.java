package com.maddyhome.idea.vim;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.ui.VimKeymapDialog;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author oleg
 */
public class VimKeyMapUtil {
  private static Logger LOG = Logger.getInstance(VimKeyMapUtil.class);
  private static final String VIM_XML = "Vim.xml";

  public static boolean isVimKeymapInstalled() {
    return KeymapManager.getInstance().getKeymap("Vim") != null;
  }

  /**
   * @return true if keymap was installed or was successfully installed
   */
  public static boolean installKeyBoardBindings() {
    LOG.debug("Check for keyboard bindings");

    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();

    final Keymap keymap = manager.getKeymap("Vim");
    if (keymap != null) {
      return true;
    }

    final String keyMapsPath = PathManager.getConfigPath() + File.separatorChar + "keymaps";
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    final VirtualFile keyMapsFolder = localFileSystem.refreshAndFindFileByPath(keyMapsPath);
    if (keyMapsFolder == null) {
      LOG.error("Failed to install vim keymap. Empty keymaps folder");
      return false;
    }

    LOG.debug("No vim keyboard installed found. Installing");
    String keymapPath = PathManager.getPluginsPath() + File.separatorChar + VimPlugin.IDEAVIM_NOTIFICATION_TITLE + File.separatorChar + VIM_XML;
    File vimKeyMapFile = new File(keymapPath);
    // Look in development path
    if ((!vimKeyMapFile.exists() || !vimKeyMapFile.isFile()) && ApplicationManagerEx.getApplicationEx().isInternal()) {
      final String resource = VimKeyMapUtil.class.getResource("").toString();
      keymapPath = resource.toString().substring("file:".length(), resource.indexOf("out")) + "community/plugins/ideavim/keymap/" + VIM_XML;
      vimKeyMapFile = new File(keymapPath);
    }
    if (!vimKeyMapFile.exists() || !vimKeyMapFile.isFile()) {
      final String error = "Installation of the Vim keymap failed because Vim keymap file not found: " + keymapPath;
      LOG.error(error);
      Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE, error, NotificationType.ERROR));
      return false;
    }
    try {
      final VirtualFile vimKeyMap2Copy = localFileSystem.refreshAndFindFileByIoFile(vimKeyMapFile);
      if (vimKeyMap2Copy == null){
        final String error = "Installation of the Vim keymap failed because Vim keymap file not found: " + keymapPath;
        LOG.error(error);
        Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE, error, NotificationType.ERROR));
        return false;
      }
      final VirtualFile vimKeyMapVFile = localFileSystem.copyFile(VimPlugin.getInstance(), vimKeyMap2Copy, keyMapsFolder, VIM_XML);

      final String path = vimKeyMapVFile.getPath();
      final Document document = StorageUtil.loadDocument(new FileInputStream(path));
      if (document == null) {
        LOG.error("Failed to install vim keymap. Vim.xml file is corrupted");
        Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                  "Failed to install vim keymap. Vim.xml file is corrupted", NotificationType.ERROR));
        return false;
      }

      // Prompt user to select the parent for the Vim keyboard
      configureVimParentKeymap(path, document, false);

      final KeymapImpl vimKeyMap = new KeymapImpl();
      final Keymap[] allKeymaps = manager.getAllKeymaps();
      vimKeyMap.readExternal(document.getRootElement(), allKeymaps);
      manager.addKeymap(vimKeyMap);
      return true;
    }
    catch (Exception e) {
      reportError(e);
      return false;
    }
  }

  private static void requestRestartOrShutdown(final Project project) {
    final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
    if (app.isRestartCapable()) {
      if (Messages.showDialog(project, "Restart " + ApplicationNamesInfo.getInstance().getProductName() + " to activate changes?",
                              VimPlugin.IDEAVIM_NOTIFICATION_TITLE, new String[]{"&Restart", "&Postpone"}, 0, Messages.getQuestionIcon()) == 0) {
        app.restart();
      }
    } else {
      if (Messages.showDialog(project, "Shut down " + ApplicationNamesInfo.getInstance().getProductName() + " to activate changes?",
                              VimPlugin.IDEAVIM_NOTIFICATION_TITLE, new String[]{"&Shut Down", "&Postpone"}, 0, Messages.getQuestionIcon()) == 0){
        app.exit(true);
      }
    }
  }

  /**
   * Changes parent keymap for the Vim
   * @return true if document was changed successfully
   */
  private static boolean configureVimParentKeymap(final String path, final Document document, final boolean showNotification) throws IOException {
    final Element rootElement = document.getRootElement();
    final String parentKeymap = rootElement.getAttributeValue("parent");
    final VimKeymapDialog vimKeymapDialog = new VimKeymapDialog(parentKeymap);
    vimKeymapDialog.show();
    if (vimKeymapDialog.getExitCode() != DialogWrapper.OK_EXIT_CODE){
      return false;
    }
    rootElement.removeAttribute("parent");
    final Keymap selectedKeymap = vimKeymapDialog.getSelectedKeymap();
    final String keymapName = selectedKeymap.getName();
    rootElement.setAttribute("parent", keymapName);

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
    return KeymapManager.getInstance().getActiveKeymap().getName().equals("Vim");
  }

  /**
   * @return true if keymap was switched successfully, false otherwise
   */
  public static boolean switchKeymapBindings(final boolean enableVimKeymap) {
    LOG.debug("Enabling keymap");

    // In case if Vim keymap is already in use or we don't need it, we have nothing to do
    if (isVimKeymapUsed() == enableVimKeymap){
      return false;
    }

    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();
    // Get keymap to enable
    final String keymapName2Enable = enableVimKeymap ? "Vim" : VimPlugin.getInstance().getPreviousKeyMap();
    if (keymapName2Enable.isEmpty()) {
      return false;
    }
    if (keymapName2Enable.equals(manager.getActiveKeymap().getName())) {
      return false;
    }

    LOG.debug("Enabling keymap:" + keymapName2Enable);
    final Keymap keymap = manager.getKeymap(keymapName2Enable);
    if (keymap == null) {
      Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                "Failed to enable keymap: " + keymapName2Enable, NotificationType.ERROR));
      LOG.error("Failed to enable keymap: " + keymapName2Enable);
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


  public static void reconfigureParentKeymap(final Project project) {
    final VirtualFile vimKeymapFile = getVimKeymapFile();
    if (vimKeymapFile == null) {
      LOG.error("Failed to find Vim keymap");
      Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                "Failed to find Vim keymap", NotificationType.ERROR));
      return;
    }

    try {
      final String path = vimKeymapFile.getPath();
      final Document document = StorageUtil.loadDocument(new FileInputStream(path));
      if (document == null) {
        LOG.error("Failed to install vim keymap. Vim.xml file is corrupted");
        Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                  "Vim.xml file is corrupted", NotificationType.ERROR));
        return;
      }
      // Prompt user to select the parent for the Vim keyboard
      if (configureVimParentKeymap(path, document, true)) {
        final KeymapManagerImpl manager = (KeymapManagerImpl) KeymapManager.getInstance();
        final KeymapImpl vimKeyMap = new KeymapImpl();
        final Keymap[] allKeymaps = manager.getAllKeymaps();
        vimKeyMap.readExternal(document.getRootElement(), allKeymaps);
        manager.addKeymap(vimKeyMap);
        requestRestartOrShutdown(project);
      }
    }
    catch (FileNotFoundException e) {
      reportError(e);
    }
    catch (IOException e) {
      reportError(e);
    }
    catch (InvalidDataException e) {
      reportError(e);
    }
  }

  private static void reportError(final Exception e) {
    LOG.error("Failed to reconfigure vim keymap.\n" + e);
    Notifications.Bus.notify(new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                               "Failed to reconfigure vim keymap.\n" + e, NotificationType.ERROR));
  }

  @Nullable
  public static VirtualFile getVimKeymapFile() {
    final String keyMapsPath = PathManager.getConfigPath() + File.separatorChar + "keymaps";
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    return localFileSystem.refreshAndFindFileByPath(keyMapsPath + File.separatorChar + VIM_XML);
  }
}
