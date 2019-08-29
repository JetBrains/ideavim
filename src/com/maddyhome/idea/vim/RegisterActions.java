/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.maddyhome.idea.vim.action.VimActionBean;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.key.Shortcut;

import javax.swing.*;
import java.awt.event.KeyEvent;

class RegisterActions {
  /**
   * Register all the key/action mappings for the plugin.
   */
  static void registerActions() {
    Runnable setup = () -> {
      registerVimCommandActions();
      registerEmptyShortcuts();
      VimPlugin.Initialization.actionsInitialized();
    };

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      setup.run();
    } else {
      ApplicationManager.getApplication().executeOnPooledThread(setup);
    }
  }

  private static void registerVimCommandActions() {
    ExtensionPointName.<VimActionBean>create("IdeaVIM.vimAction").extensions().forEach(actionBean -> {
      VimPlugin.getKey().registerCommandAction(actionBean.getAction(), actionBean.getId());
    });
  }

  private static void registerEmptyShortcuts() {
    final KeyGroup parser = VimPlugin.getKey();

    // Digraph shortcuts are handled directly by KeyHandler#handleKey, so they don't have an action. But we still need to
    // register the shortcuts or the editor will swallow them. Technically, the shortcuts will be registered as part of
    // other commands, but it's best to be explicit
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)));
  }
}
