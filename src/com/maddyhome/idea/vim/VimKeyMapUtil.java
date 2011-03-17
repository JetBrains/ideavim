package com.maddyhome.idea.vim;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.ui.VimKeymapDialog;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author oleg
 */
public class VimKeyMapUtil {
  private static Logger LOG = Logger.getInstance(VimKeyMapUtil.class.getName());
  private static final String VIM_XML = "Vim.xml";

  public static void installKeyBoardBindings(final VimPlugin vimPlugin) {
    LOG.debug("Check for keyboard bindings");

    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();

    final Keymap keymap = manager.getKeymap("Vim");
    if (keymap != null) {
      return;
    }

    final String keyMapsPath = PathManager.getConfigPath() + File.separatorChar + "keymaps";
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    final VirtualFile keyMapsFolder = localFileSystem.findFileByPath(keyMapsPath);
    if (keyMapsFolder == null) {
      LOG.debug("Failed to install vim keymap. Empty keymaps folder");
      return;
    }

    LOG.debug("No vim keyboard installed found. Installing");
    final File vimKeyMapFile = new File(PathManager.getPluginsPath() + File.separatorChar + "IdeaVim" + File.separatorChar + VIM_XML);
    if (!vimKeyMapFile.exists() || !vimKeyMapFile.isFile()) {
      LOG.debug("vim keyboard file not found");
      Notifications.Bus.notify(new Notification("ideavim", "IdeaVim",
                                                "Installation of the Vim keymap failed because Vim keymap file not found.\n" +
                                                "Some functions may work incorrect. " +
                                                "For more details please refer to : http://youtrack.jetbrains.net/issues/VIM",
                                                NotificationType.ERROR));
      return;
    }
    try {
      final VirtualFile vimKeyMap2Copy = localFileSystem.findFileByIoFile(vimKeyMapFile);
      final VirtualFile vimKeyMapVFile = localFileSystem.copyFile(vimPlugin, vimKeyMap2Copy, keyMapsFolder, VIM_XML);

      final String path = vimKeyMapVFile.getPath();
      final Document document = StorageUtil.loadDocument(new FileInputStream(path));
      if (document == null) {
        LOG.debug("Failed to install vim keymap. Vim.xml file is corrupted");
        Notifications.Bus.notify(new Notification("ideavim", "IdeaVim",
                                                  "Failed to install vim keymap. Vim.xml file is corrupted", NotificationType.ERROR));
        return;
      }

      // Prompt user to select the parent for the Vim keyboard
      configureVimParentKeymap(path, document);

      final KeymapImpl vimKeyMap = new KeymapImpl();
      final Keymap[] allKeymaps = manager.getAllKeymaps();
      vimKeyMap.readExternal(document.getRootElement(), allKeymaps);
      manager.addKeymap(vimKeyMap);
      Notifications.Bus.notify(new Notification("ideavim", "IdeaVim", "Successfully installed vim keymap", NotificationType.INFORMATION));
    }
    catch (Exception e) {
      LOG.debug("Failed to install vim keymap.\n" + e.getMessage());
      Notifications.Bus
        .notify(new Notification("ideavim", "IdeaVim", "Failed to install vim keymap.\n" + e.getMessage(), NotificationType.ERROR));
      return;
    }
  }

  private static void configureVimParentKeymap(final String path, final Document document) throws IOException {
    final VimKeymapDialog vimKeymapDialog = new VimKeymapDialog();
    vimKeymapDialog.show();

    final Element rootElement = document.getRootElement();
    rootElement.removeAttribute("parent");
    final String keymapName = vimKeymapDialog.getSelectedKeymap().getName();
    rootElement.setAttribute("parent", keymapName);

    // Save modified keymap to the file
    JDOMUtil.writeDocument(document, path, "\n");
    Notifications.Bus.notify(new Notification("ideavim", "IdeaVim", "Successfully configured vim keymap to be based on " +
                                                                    getPresentableKeyMapName(keymapName),
                                              NotificationType.INFORMATION));
  }

  public static void enableKeyBoardBindings(final boolean enabled) {
    LOG.debug("Enabling keymap");
    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();
    final String keymapName2Enable = enabled ? "Vim" : VimPlugin.getInstance().getPreviousKeyMap();
    if (keymapName2Enable.isEmpty()) {
      return;
    }
    if (keymapName2Enable.equals(manager.getActiveKeymap().getName())) {
      return;
    }
    LOG.debug("Enabling keymap:" + keymapName2Enable);
    final Keymap keymap = manager.getKeymap(keymapName2Enable);
    if (keymap == null) {
      Notifications.Bus
        .notify(new Notification("ideavim", "IdeaVim", "Failed to enable keymap: " + keymapName2Enable, NotificationType.ERROR));
      LOG.debug("Failed to enable keymap: " + keymapName2Enable);
      return;
    }
    // Save previous keymap to enable after VIM emulation is turned off
    if (enabled) {
      VimPlugin.getInstance().setPreviousKeyMap(manager.getActiveKeymap().getName());
    }

    manager.setActiveKeymap(keymap);

    final String keyMapPresentableName = getPresentableKeyMapName(keymapName2Enable);
    Notifications.Bus
      .notify(new Notification("ideavim", "IdeaVim", keyMapPresentableName + " keymap was enabled", NotificationType.INFORMATION));
    LOG.debug(keymapName2Enable + " keymap was enabled");
  }

  private static String getPresentableKeyMapName(final String keymapName) {
    if (KeymapManager.DEFAULT_IDEA_KEYMAP.equals(keymapName)) {
      return "IntelliJ IDEA Classic";
    }
    else if ("Mac OS X".equals(keymapName)) {
      return "IntelliJ IDEA Classic - Mac OS X";
    }
    else {
      return keymapName;
    }
  }


  public static void reconfigureParentKeymap() {
    final VirtualFile vimKeymapFile = getVimKeymapFile();
    if (vimKeymapFile == null) {
      Notifications.Bus.notify(new Notification("ideavim", "IdeaVim", "Failed to find Vim keymap", NotificationType.ERROR));
      return;
    }

    try {
      final String path = vimKeymapFile.getPath();
      final Document document = StorageUtil.loadDocument(new FileInputStream(path));
      if (document == null) {
        LOG.debug("Failed to install vim keymap. Vim.xml file is corrupted");
        Notifications.Bus.notify(new Notification("ideavim", "IdeaVim",
                                                  "Vim.xml file is corrupted", NotificationType.ERROR));
        return;
      }
      // Prompt user to select the parent for the Vim keyboard
      configureVimParentKeymap(path, document);
    }
    catch (Exception e) {
      LOG.debug("Failed to reconfigure vim keymap.\n" + e.getMessage());
      Notifications.Bus
        .notify(new Notification("ideavim", "IdeaVim", "Failed to reconfigure vim keymap.\n" + e.getMessage(), NotificationType.ERROR));
    }
  }

  @Nullable
  public static VirtualFile getVimKeymapFile() {
    final String keyMapsPath = PathManager.getConfigPath() + File.separatorChar + "keymaps";
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    return localFileSystem.findFileByPath(keyMapsPath + File.separatorChar + VIM_XML);
  }
}
