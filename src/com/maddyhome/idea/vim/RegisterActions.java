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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.handler.ActionBeanClass;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.key.Shortcut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class RegisterActions {

  public static final ExtensionPointName<ActionBeanClass> VIM_ACTIONS_EP =
    ExtensionPointName.create("IdeaVIM.vimAction");
  private static boolean actionsRegistered = false;

  /**
   * Register all the key/action mappings for the plugin.
   */
  static void registerActions() {
    if (actionsRegistered) return;
    actionsRegistered = true;

    registerVimCommandActions();
    registerEmptyShortcuts();
  }

  @Nullable
  public static EditorActionHandlerBase findAction(@NotNull String id) {
    return VIM_ACTIONS_EP.extensions().filter(vimActionBean -> vimActionBean.getActionId().equals(id)).findFirst()
      .map(ActionBeanClass::getAction).orElse(null);
  }

  @NotNull
  public static EditorActionHandlerBase findActionOrDie(@NotNull String id) {
    EditorActionHandlerBase action = findAction(id);
    if (action == null) throw new RuntimeException("Action " + id + " is not registered");
    return action;
  }

  private static void registerVimCommandActions() {
    KeyGroup parser = VimPlugin.getKey();
    VIM_ACTIONS_EP.extensions().forEach(parser::registerCommandAction);
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
