package com.maddyhome.idea.vim.ui;

import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.util.SystemInfo;
import com.maddyhome.idea.vim.VimPlugin;

import javax.swing.*;
import java.util.ArrayList;

/**
 * @author oleg
 */
public class VimKeymapPanel {
  private JComboBox myKeymapComboBox;
  private JPanel myPanel;

  public VimKeymapPanel() {
    final ArrayList<Keymap> keymaps = new ArrayList<Keymap>();
    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();
    for (Keymap keymap : manager.getAllKeymaps()) {
      if (!"Vim".equals(keymap.getName()) && matchesPlatform(keymap)) {
        keymaps.add(keymap);
      }
    }

    myKeymapComboBox.setModel(new DefaultComboBoxModel(keymaps.toArray(new Keymap[keymaps.size()])));
    myKeymapComboBox.setRenderer(new ListCellRendererWrapper(myKeymapComboBox.getRenderer()) {
      @Override
      public void customize(final JList list, final Object value, final int index, final boolean selected, final boolean cellHasFocus) {
        Keymap keymap = (Keymap)value;
        if (keymap == null) {
          return;
        }
        if (KeymapManager.DEFAULT_IDEA_KEYMAP.equals(keymap.getName())) {
          setText("IntelliJ IDEA Classic");
        }
        else if ("Mac OS X".equals(keymap.getName())) {
          setText("IntelliJ IDEA Classic - Mac OS X");
        }
        else {
          setText(keymap.getPresentableName());
        }
      }
    });

    final String previousKeyMap = VimPlugin.getInstance().getPreviousKeyMap();
    myKeymapComboBox.getModel().setSelectedItem(previousKeyMap.isEmpty() ? manager.getActiveKeymap() : manager.getKeymap(previousKeyMap));
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public Keymap getSelectedKeyMap(){
    return (Keymap) myKeymapComboBox.getSelectedItem();
  }

  private static boolean matchesPlatform(final Keymap keymap) {
    if (keymap.getName().equals(KeymapManager.DEFAULT_IDEA_KEYMAP)) {
      return !SystemInfo.isMac;
    }
    else if (keymap.getName().equals(KeymapManager.MAC_OS_X_KEYMAP)) {
      return SystemInfo.isMac;
    }
    else if (keymap.getName().equals("Default for GNOME") || keymap.getName().equals("Default for KDE") ||
             keymap.getName().equals("Default for XWin")) {
      return SystemInfo.isLinux;
    }
    return true;
  }
}
