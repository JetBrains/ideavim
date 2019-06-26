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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.maddyhome.idea.vim.action.VimCommandActionBase;
import com.maddyhome.idea.vim.command.Argument;
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

    registerInsertModeActions();
    registerNormalModeActions();
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
  }

  private static void registerNormalModeActions() {
    final KeyGroup parser = VimPlugin.getKey();
    // Copy/Paste Actions
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursor", Command.Type.PASTE,
                          new Shortcut('P'));
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursor", Command.Type.PASTE,
                          new Shortcut('p'));
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gP"));
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gp"));
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[P"),
      new Shortcut("]P"),
      new Shortcut("[p")
    });
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("]p")
    });
    parser.registerAction(MappingMode.N, "VimCopyYankLine", Command.Type.COPY,
                          new Shortcut('Y'));
    parser.registerAction(MappingMode.N, "VimCopyYankLine", Command.Type.COPY, EnumSet.of(CommandFlags.FLAG_ALLOW_MID_COUNT),
                          new Shortcut("yy"));
    parser.registerAction(MappingMode.N, "VimCopyYankMotion", Command.Type.COPY, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('y'), Argument.Type.MOTION);

    // Insert/Replace/Change Actions
    parser.registerAction(MappingMode.N, "VimChangeCaseLowerMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut("gu"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCaseToggleCharacter", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(MappingMode.N, "VimChangeCaseToggleMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut("g~"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCaseUpperMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut("gU"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCharacter", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_ALLOW_DIGRAPH),
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser
      .registerAction(MappingMode.N, "VimChangeCharacters", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_NO_REPEAT, CommandFlags.FLAG_MULTIKEY_UNDO),
                      new Shortcut('s'));
    parser
      .registerAction(MappingMode.N, "VimChangeEndOfLine", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_NO_REPEAT, CommandFlags.FLAG_MULTIKEY_UNDO),
                      new Shortcut('C'));
    parser.registerAction(MappingMode.N, "VimChangeLine", Command.Type.CHANGE,
                          EnumSet.of(CommandFlags.FLAG_NO_REPEAT, CommandFlags.FLAG_ALLOW_MID_COUNT, CommandFlags.FLAG_MULTIKEY_UNDO), new Shortcut[]{
        new Shortcut("cc"),
        new Shortcut('S')
      });
    parser.registerAction(MappingMode.N, "VimChangeNumberInc", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimChangeNumberDec", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimChangeMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND, CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('c'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeReplace", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('R'));
    parser.registerAction(MappingMode.N, "VimDeleteCharacter", Command.Type.DELETE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(MappingMode.N, "VimDeleteCharacterLeft", Command.Type.DELETE,
                          new Shortcut('X'));
    parser.registerAction(MappingMode.N, "VimDeleteCharacterRight", Command.Type.DELETE,
                          new Shortcut('x'));
    parser.registerAction(MappingMode.N, "VimDeleteEndOfLine", Command.Type.DELETE,
                          new Shortcut('D'));
    parser.registerAction(MappingMode.N, "VimDeleteJoinLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(MappingMode.N, "VimDeleteJoinLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(MappingMode.N, "VimDeleteLine", Command.Type.DELETE, EnumSet.of(CommandFlags.FLAG_ALLOW_MID_COUNT),
                          new Shortcut("dd"));
    parser.registerAction(MappingMode.N, "VimDeleteMotion", Command.Type.DELETE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('d'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimFilterCountLines", Command.Type.CHANGE,
                          new Shortcut("!!"));
    parser.registerAction(MappingMode.N, "VimFilterMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('!'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimInsertAfterCursor", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('a'));
    parser.registerAction(MappingMode.N, "VimInsertAfterLineEnd", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('A'));
    parser.registerAction(MappingMode.N, "VimInsertAtPreviousInsert", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut("gi"));
    parser.registerAction(MappingMode.N, "VimInsertBeforeFirstNonBlank", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('I'));
    parser.registerAction(MappingMode.N, "VimInsertLineStart", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut("gI"));
    parser.registerAction(MappingMode.N, "VimInsertNewLineAbove", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('O'));
    parser.registerAction(MappingMode.N, "VimInsertNewLineBelow", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_MULTIKEY_UNDO),
                          new Shortcut('o'));
    // Misc Actions
    parser.registerAction(MappingMode.N, "VimLastSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut('&'));
    parser.registerAction(MappingMode.N, "VimLastGlobalSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut("g&"));
    parser.registerAction(MappingMode.N, "VimRepeatChange", Command.Type.OTHER_WRITABLE,
                          new Shortcut('.'));
    parser.registerAction(MappingMode.N, "VimRepeatExCommand", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@:"));
    parser.registerAction(MappingMode.N, "QuickJavaDoc", Command.Type.OTHER_READONLY,
                          new Shortcut('K'));
    parser.registerAction(MappingMode.N, "VimRedo", Command.Type.OTHER_SELF_SYNCHRONIZED,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimUndo", Command.Type.OTHER_SELF_SYNCHRONIZED, new Shortcut[]{
      new Shortcut('u'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
    });

    // File Actions
    parser.registerAction(MappingMode.N, "VimFileSaveClose", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut("ZQ"),
      new Shortcut("ZZ")
    });
    parser.registerAction(MappingMode.N, "VimFilePrevious", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CIRCUMFLEX, KeyEvent.CTRL_MASK))
    });

    // Shift Actions
    parser.registerAction(MappingMode.N, "VimAutoIndentLines", Command.Type.CHANGE,
                          new Shortcut("=="));
    parser.registerAction(MappingMode.N, "VimAutoIndentMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('='), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimShiftLeftLines", Command.Type.CHANGE,
                          new Shortcut("<<"));
    parser.registerAction(MappingMode.N, "VimShiftLeftMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('<'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimShiftRightLines", Command.Type.CHANGE,
                          new Shortcut(">>"));
    parser.registerAction(MappingMode.N, "VimShiftRightMotion", Command.Type.CHANGE, EnumSet.of(CommandFlags.FLAG_OP_PEND),
                          new Shortcut('>'), Argument.Type.MOTION);

    // Jump Actions


    parser.registerAction(MappingMode.N, "VimFileGetAscii", Command.Type.OTHER_READONLY,
                          new Shortcut("ga"));
    parser.registerAction(MappingMode.N, "VimFileGetHex", Command.Type.OTHER_READONLY,
                          new Shortcut("g8"));
    parser.registerAction(MappingMode.N, "VimFileGetFileInfo", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)));

    // Macro Actions
    parser.registerAction(MappingMode.N, "VimPlaybackLastRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@@"));
    parser.registerAction(MappingMode.N, "VimPlaybackRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut('@'), Argument.Type.CHARACTER);
    // TODO - support for :map macros
  }

  private static void registerInsertModeActions() {
    final KeyGroup parser = VimPlugin.getKey();
    // Other insert actions
    parser
      .registerAction(MappingMode.I, "EditorBackSpace", Command.Type.INSERT,
                      new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))}
      );
    parser.registerAction(MappingMode.I, "EditorDelete", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(MappingMode.I, "EditorDown", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES), new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
    });
    parser.registerAction(MappingMode.I, "EditorTab", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE), new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(MappingMode.I, "EditorUp", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES), new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
    });
    parser.registerAction(MappingMode.I, "VimInsertCharacterAboveCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertCharacterBelowCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertDeleteInsertedText", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertDeletePreviousWord", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertEnter", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE), new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    });
    parser.registerAction(MappingMode.I, "VimInsertPreviousInsert", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertPreviousInsertExit", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.I, "VimInsertRegister", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)),
                          Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.I, "VimInsertReplaceToggle", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
    parser.registerAction(MappingMode.I, "VimInsertSingleCommand", Command.Type.INSERT,
                          EnumSet.of(CommandFlags.FLAG_CLEAR_STROKES, CommandFlags.FLAG_EXPECT_MORE),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimShiftLeftLines", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimShiftRightLines", Command.Type.INSERT, EnumSet.of(CommandFlags.FLAG_SAVE_STROKE),
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK)));

    // Digraph shortcuts are handled directly by KeyHandler#handleKey, so they don't have an action. But we still need to
    // register the shortcuts or the editor will swallow them. Technically, the shortcuts will be registered as part of
    // other commands, but it's best to be explicit
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK)));
    parser.registerShortcutWithoutAction(new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)));
  }
}
