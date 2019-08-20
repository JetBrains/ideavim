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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.maddyhome.idea.vim.action.VimCommandActionBase;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.key.Shortcut;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.EnumSet;

class RegisterActions {
  /**
   * Register all the key/action mappings for the plugin.
   */
  static void registerActions() {
    registerVimCommandActions();
    registerSystemMappings();
  }

  private static void registerVimCommandActions() {
    final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
    for (String actionId : manager.getPluginActions(VimPlugin.getPluginId())) {
      final AnAction action = manager.getAction(actionId);
      if (action instanceof VimCommandActionBase) {
        VimPlugin.getKey().registerCommandAction((VimCommandActionBase)action, actionId);
      }
    }
  }

  private static void registerSystemMappings() {
    final KeyGroup parser = VimPlugin.getKey();
    parser.registerAction(MappingMode.NV, "CollapseAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zM"));
    parser.registerAction(MappingMode.NV, "CollapseRegion", Command.Type.OTHER_READONLY, new Shortcut("zc"));
    parser.registerAction(MappingMode.NV, "CollapseRegionRecursively", Command.Type.OTHER_READONLY, new Shortcut("zC"));
    parser.registerAction(MappingMode.NV, "ExpandAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zR"));
    parser.registerAction(MappingMode.NV, "ExpandRegion", Command.Type.OTHER_READONLY, new Shortcut("zo"));
    parser.registerAction(MappingMode.NV, "ExpandRegionRecursively", Command.Type.OTHER_READONLY, new Shortcut("zO"));

    parser.registerAction(MappingMode.I, "EditorBackSpace", Command.Type.INSERT, EnumSet.noneOf(CommandFlags.class),
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))});
    parser.registerAction(MappingMode.I, "EditorDelete", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))});
    parser.registerAction(MappingMode.I, "EditorDown", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES),
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))});
    parser.registerAction(MappingMode.I, "EditorTab", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))});
    parser.registerAction(MappingMode.I, "EditorUp", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES),
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))});

    parser.registerAction(MappingMode.N, "QuickJavaDoc", Command.Type.OTHER_READONLY, new Shortcut('K'));

    // Digraph shortcuts are handled directly by KeyHandler#handleKey, so they don't have an action. But we still need to
    // register the shortcuts or the editor will swallow them. Technically, the shortcuts will be registered as part of
    // other commands, but it's best to be explicit
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)));
  }
}
