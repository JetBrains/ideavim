/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.KeymapManagerImpl;
import com.intellij.openapi.util.SystemInfo;
import com.maddyhome.idea.vim.VimPlugin;
import org.jetbrains.annotations.NotNull;

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
    final ListCellRendererWrapper<Keymap> renderer = new ListCellRendererWrapper<Keymap>() {
      @Override
      public void customize(final JList list,
                            final Keymap value,
                            final int index,
                            final boolean selected,
                            final boolean cellHasFocus) {
        if (value != null) {
          setText(value.getPresentableName());
        }
      }
    };
    myKeymapComboBox.setRenderer(renderer);

    final String previousKeyMap = VimPlugin.getInstance().getPreviousKeyMap();
    myKeymapComboBox.getModel().setSelectedItem(preselectedKeymap != null ? preselectedKeymap :
                                                previousKeyMap.isEmpty() ? manager.getActiveKeymap() : manager.getKeymap(previousKeyMap));
  }

  public JPanel getPanel() {
    return myPanel;
  }

  @NotNull
  public Keymap getSelectedKeyMap(){
    return (Keymap) myKeymapComboBox.getSelectedItem();
  }

  private static boolean matchesPlatform(@NotNull final Keymap keymap) {
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
