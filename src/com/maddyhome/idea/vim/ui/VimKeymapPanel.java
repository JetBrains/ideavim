package com.maddyhome.idea.vim.ui;

import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.util.SystemInfo;
import com.maddyhome.idea.vim.VimPlugin;

import javax.swing.*;
import java.util.ArrayList;

import static com.maddyhome.idea.vim.VimKeyMapUtil.VIM_KEYMAP_NAME;

/**
 * @author oleg
 */
public class VimKeymapPanel {
  private JComboBox myKeymapComboBox;
  private JPanel myPanel;

  public VimKeymapPanel(final String parentKeymap) {
    final ArrayList<Keymap> keymaps = new ArrayList<Keymap>();
    final KeymapManagerImpl manager = (KeymapManagerImpl)KeymapManager.getInstance();
    Keymap preselectedKeymap = null;
    for (Keymap keymap : manager.getAllKeymaps()) {
      final String name = keymap.getName();
      if (!VIM_KEYMAP_NAME.equals(name) && matchesPlatform(keymap)) {
        if (name.equals(parentKeymap)) {
          preselectedKeymap = keymap;
        }
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
        setText(keymap.getPresentableName());
      }
    });

    final String previousKeyMap = VimPlugin.getInstance().getPreviousKeyMap();
    myKeymapComboBox.getModel().setSelectedItem(preselectedKeymap != null ? preselectedKeymap :
                                                previousKeyMap.isEmpty() ? manager.getActiveKeymap() : manager.getKeymap(previousKeyMap));
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
