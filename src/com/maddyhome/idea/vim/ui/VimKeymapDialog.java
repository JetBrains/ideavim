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

import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author oleg
 */
public class VimKeymapDialog extends DialogWrapper {

  private VimKeymapPanel myVimKeymapPanel;

  public VimKeymapDialog(final String parentKeymap) {
    super(ProjectManager.getInstance().getDefaultProject());
    myVimKeymapPanel = new VimKeymapPanel(parentKeymap);
    setTitle("Vim Keymap settings");
    init();
  }

  @NotNull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction()};
  }

  protected JComponent createCenterPanel() {
    return myVimKeymapPanel.getPanel();
  }

  @NotNull
  public Keymap getSelectedKeymap(){
    return myVimKeymapPanel.getSelectedKeyMap();
  }
}
