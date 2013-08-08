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
package com.maddyhome.idea.vim;

import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.key.Shortcut;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * This registers all the key/action mappings for the plugin
 */
public class RegisterActions {
  public static RegisterActions getInstance() {
    if (instance == null) {
      instance = new RegisterActions();
    }

    return instance;
  }

  private RegisterActions() {
    KeyParser parser = KeyParser.getInstance();

    // ******************* Insert Mode Actions **********************
    // Delegation actions
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimClassNameCompletion", Command.Type.COMPLETION,
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimCodeCompletion", Command.Type.COMPLETION,
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimSmartTypeCompletion", Command.Type.COMPLETION,
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertLiveTemplate", Command.Type.COMPLETION,
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)));

    // Other insert actions
    parser
      .registerAction(KeyParser.MAPPING_INSERT, "VimEditorBackSpace", Command.Type.INSERT, Command.FLAG_IS_BACKSPACE,
                      new Shortcut[]{
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
                      });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorDelete", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorTab", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertCharacterAboveCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertCharacterBelowCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertDeleteInsertedText", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertDeletePreviousWord", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertEnter", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertExitMode", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertHelp", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertPreviousInsert", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertPreviousInsertExit", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertRegister", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)),
                          Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertReplaceToggle", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertSingleCommand", Command.Type.INSERT,
                          Command.FLAG_CLEAR_STROKES | Command.FLAG_EXPECT_MORE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionFirstColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionGotoLineFirst", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionGotoLineLastEnd", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionLastColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionLeft", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionRight", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionScrollPageUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionScrollPageDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionWordLeft", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionWordRight", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimShiftLeftLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_INSERT, "VimShiftRightLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK)));

    // ************************* Visual Mode Actions **********************
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimAutoIndentVisual", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE,
                          new Shortcut('='));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimReformatVisual", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE,
                          new Shortcut("gq"));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseLowerVisual", Command.Type.CHANGE,
                          new Shortcut('u'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseToggleVisual", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseUpperVisual", Command.Type.CHANGE,
                          new Shortcut('U'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisual", Command.Type.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
      new Shortcut('c'),
      new Shortcut('s')
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisualCharacter", Command.Type.CHANGE, Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisualLines", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut('R'),
        new Shortcut('S')
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisualLinesEnd", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut('C')
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimCopyYankVisual", Command.Type.COPY,
                          new Shortcut('y'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimCopyYankVisualLines", Command.Type.COPY, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('Y'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteJoinVisualLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteJoinVisualLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteVisual", Command.Type.DELETE, new Shortcut[]{
      new Shortcut('d'),
      new Shortcut('x'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteVisualLinesEnd", Command.Type.DELETE, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('D')
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteVisualLines", Command.Type.DELETE, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('X')
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimFilterVisualLines", Command.Type.CHANGE, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('!'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimShiftLeftVisual", Command.Type.CHANGE,
                          new Shortcut('<'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimShiftRightVisual", Command.Type.CHANGE,
                          new Shortcut('>'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualExitMode", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualPutText", Command.Type.PASTE, new Shortcut[]{
      new Shortcut('P'),
      new Shortcut('p')
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualPutTextMoveCursor", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("gp"),
      new Shortcut("gP")
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualPutTextNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[p"),
      new Shortcut("]p"),
      new Shortcut("[P"),
      new Shortcut("]P")
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualBlockInsert", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('I'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualBlockAppend", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('A'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualSwapEnds", Command.Type.OTHER_READONLY,
                          new Shortcut('o'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualSwapEndsBlock", Command.Type.OTHER_READONLY,
                          new Shortcut('O'));
    parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualSwapSelections", Command.Type.OTHER_READONLY,
                          new Shortcut("gv"));

    // ************************* Normal Mode Actions *************************
    // Copy/Paste Actions
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextBeforeCursor", Command.Type.PASTE,
                          new Shortcut('P'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextAfterCursor", Command.Type.PASTE,
                          new Shortcut('p'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextBeforeCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gP"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextAfterCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gp"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextBeforeCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[P"),
      new Shortcut("]P")
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextAfterCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[p"),
      new Shortcut("]p")
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankLine", Command.Type.COPY,
                          new Shortcut('Y'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankLine", Command.Type.COPY, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("yy"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankMotion", Command.Type.COPY, Command.FLAG_OP_PEND,
                          new Shortcut('y'), Argument.Type.MOTION);

    // Insert/Replace/Change Actions
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseLowerMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gu"), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseToggleCharacter", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseToggleMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("g~"), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseUpperMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gU"), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCharacter", Command.Type.CHANGE, Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser
      .registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCharacters", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('s'));
    parser
      .registerAction(KeyParser.MAPPING_NORMAL, "VimChangeEndOfLine", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('C'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeLine", Command.Type.CHANGE,
                          Command.FLAG_NO_REPEAT | Command.FLAG_ALLOW_MID_COUNT | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut("cc"),
        new Shortcut('S')
      });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeNumberInc", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeNumberDec", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND | Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('c'), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeReplace", Command.Type.CHANGE, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('R'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacter", Command.Type.DELETE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacterLeft", Command.Type.DELETE,
                          new Shortcut('X'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacterRight", Command.Type.DELETE,
                          new Shortcut('x'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteEndOfLine", Command.Type.DELETE,
                          new Shortcut('D'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteJoinLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteJoinLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteLine", Command.Type.DELETE, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("dd"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteMotion", Command.Type.DELETE, Command.FLAG_OP_PEND,
                          new Shortcut('d'), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFilterCountLines", Command.Type.CHANGE,
                          new Shortcut("!!"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFilterMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('!'), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAfterCursor", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('a'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAfterLineEnd", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('A'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAtPreviousInsert", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gi"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertBeforeCursor", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
      new Shortcut('i'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertBeforeFirstNonBlank", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('I'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertLineStart", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gI"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertNewLineAbove", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('O'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertNewLineBelow", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('o'));
    // Motion Actions
    parser
      .registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('`'), Argument.Type.CHARACTER);
    parser
      .registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('\''), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    // Misc Actions
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimLastSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut('&'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimLastGlobalSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut("g&"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimVisualSelectPrevious", Command.Type.OTHER_READONLY,
                          new Shortcut("gv"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRepeatChange", Command.Type.OTHER_WRITABLE,
                          new Shortcut('.'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRepeatExCommand", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@:"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "QuickJavaDoc", Command.Type.OTHER_READONLY,
                          new Shortcut('K'));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRedo", Command.Type.OTHER_WRITABLE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimUndo", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut('u'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
    });

    // File Actions
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFileSaveClose", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut("ZQ"),
      new Shortcut("ZZ")
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFilePrevious", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CIRCUMFLEX, KeyEvent.CTRL_MASK))
    });

    // Shift Actions
    // TODO - add =
    // TODO - == will ignore count and only autoindent 1 lines
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimAutoIndentLines", Command.Type.CHANGE,
                          new Shortcut("=="));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftLeftLines", Command.Type.CHANGE,
                          new Shortcut("<<"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftLeftMotion", Command.Type.CHANGE,
                          new Shortcut('<'), Argument.Type.MOTION);
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftRightLines", Command.Type.CHANGE,
                          new Shortcut(">>"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftRightMotion", Command.Type.CHANGE,
                          new Shortcut('>'), Argument.Type.MOTION);

    // Jump Actions

    // Do not override default Forward/Back actions!
    //parser.registerAction(KeyParser.MAPPING_NORMAL, "VimForward", Command.OTHER_READONLY);
    //parser.registerAction(KeyParser.MAPPING_NORMAL, "VimBack", Command.OTHER_READONLY);

    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionJumpNext", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionJumpPrevious", Command.Type.OTHER_READONLY,
                          new Shortcut[] {
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)),
                            // TODO: <C-T> is a tag command similar to <C-O>, the tag stack is not implemented
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK))
                          });

    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFileGetAscii", Command.Type.OTHER_READONLY,
                          new Shortcut("ga"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFileGetHex", Command.Type.OTHER_READONLY,
                          new Shortcut("g8"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFileGetFileInfo", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)));

    // Window Actions
    // TODO - CTRL-W commands: +, -, =, S, s, _, b, c, n, o, q, s, t, <up>, <down>

    // Macro Actions
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimPlaybackLastRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@@"));
    parser.registerAction(KeyParser.MAPPING_NORMAL, "VimPlaybackRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut('@'), Argument.Type.CHARACTER);
    // TODO - support for :map macros

    // ************************* Normal, Operator Pending, Visual Mode Actions *************************
    parser.registerAction(KeyParser.MAPPING_NVO, "VimCopySelectRegister", Command.Type.SELECT_REGISTER, Command.FLAG_EXPECT_MORE,
                          new Shortcut('"'), Argument.Type.CHARACTER);

    // Motion Actions
    // TODO - add ['
    // TODO - add [`
    // TODO - add ]'
    // TODO - add ]`
    // TODO - add zj
    // TODO - add zk

    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionNextTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gt"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionPreviousTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gT"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]b"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]w"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[b"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[w"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut('|'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('j'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)),
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gj"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)})
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDownFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('+'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDownLess1FirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('_'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('0'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g0"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)})
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('^')
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstScreenNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g^")
    });
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut('H'),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK))
                      });
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineFirst", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut("gg"),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK))
                      });
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineLast", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('G'));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineLastEnd", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut('$'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut("g$"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)})
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastMatchChar", Command.Type.MOTION,
                          new Shortcut(';'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastMatchCharReverse", Command.Type.MOTION,
                          new Shortcut(','));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastNonSpace", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("g_"));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionLastScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('L'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastScreenLineEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('h'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('F'), Argument.Type.DIGRAPH);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('T'), Argument.Type.DIGRAPH);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMiddleColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gm"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMiddleScreenLine", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('M'));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionNthCharacter", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("go"));
    // This represents two commands and one is linewise and the other is inclusive - the handler will fix it
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionPercentOrMatch", Command.Type.MOTION, Command.FLAG_SAVE_JUMP,
                          new Shortcut('%'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('l'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('f'), Argument.Type.DIGRAPH);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('t'), Argument.Type.DIGRAPH);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut(' '));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollFirstScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zt")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollFirstScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zs")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollFirstScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)})
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollFirstScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z+")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollHalfPageDown", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollHalfPageUp", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLastScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zb")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLastScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("ze")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLastScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z-")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLastScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z^")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLineDown", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLineUp", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollMiddleScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zz")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollMiddleScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z.")
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollColumnLeft", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zl"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)})
                          });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollColumnRight", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zh"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)})
                          });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollPageDown", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollPageUp", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('k'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gk"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)})
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUpFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('-'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("ge"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('e'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('b'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('w'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionBigWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gE"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionBigWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('E'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionBigWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('B'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionBigWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('W'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSentenceStartPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('('));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSentenceStartNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut(')'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSentenceEndPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g("));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSentenceEndNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g)"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionParagraphPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('{'));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimMotionParagraphNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('}'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUnmatchedBraceOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[{"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUnmatchedBraceClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]}"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUnmatchedParenOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[("));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUnmatchedParenClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("])"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSectionBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[]"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSectionBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[["));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSectionForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]]"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionSectionForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]["));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMethodBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[M"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMethodBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[m"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMethodForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]M"));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMethodForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]m"));

    // Misc Actions
    parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchFwdEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_FWD | Command.FLAG_SAVE_JUMP,
                          new Shortcut('/'), Argument.Type.EX_STRING);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchRevEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_REV | Command.FLAG_SAVE_JUMP,
                          new Shortcut('?'), Argument.Type.EX_STRING);
    parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchAgainNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('n'));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimSearchAgainPrevious", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('N'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimExEntry", Command.Type.OTHER_READ_WRITE,
                          new Shortcut(':'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWholeWordForward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('*'));
    parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWholeWordBackward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('#'));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimSearchWordForward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g*"));
    parser
      .registerAction(KeyParser.MAPPING_NVO, "VimSearchWordBackward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g#"));

    // ********************** Command Line Actions ************************
    parser
      .registerAction(KeyParser.MAPPING_CMD_LINE, "VimProcessExEntry", Command.Type.OTHER_READ_WRITE, Command.FLAG_COMPLETE_EX, new Shortcut[]{
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)),
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
        new Shortcut(KeyStroke.getKeyStroke((char)0x0a)),
        new Shortcut(KeyStroke.getKeyStroke((char)0x0d))
      });

    // ********************** Various Mode Actions ************************
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_INSERT | KeyParser.MAPPING_VISUAL,
                          "VimCommentByLineComment", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_KEEP_VISUAL);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimCommentByBlockComment",
                          Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimSurroundWith", Command.Type.CHANGE,
                          Command.FLAG_DELEGATE | Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE | Command.FLAG_FORCE_VISUAL);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimSurroundWithLiveTemplate",
                          Command.Type.CHANGE,
                          Command.FLAG_DELEGATE | Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE | Command.FLAG_FORCE_VISUAL);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimMoveStatementDown",
                          Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimMoveStatementUp", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE);

    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimVisualToggleCharacterMode",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_CHARACTERWISE,
                          new Shortcut('v'));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimVisualToggleLineMode",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_LINEWISE,
                          new Shortcut('V'));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimVisualToggleBlockMode",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_BLOCKWISE, new Shortcut[]{
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)),
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK))
      });
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimMotionMark",
                          Command.Type.OTHER_READONLY,
                          new Shortcut('m'), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimGotoDeclaration",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_SAVE_JUMP, new Shortcut[]{
        new Shortcut("gD"),
        new Shortcut("gd"),
        // TODO: <C-]> is a tag command similar to gD, the tag stack is not implemented
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, KeyEvent.CTRL_MASK)),
      });
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimFileGetLocationInfo",
                          Command.Type.OTHER_READONLY,
                          new Shortcut(
                            new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)}));
    // TODO - add zC
    // TODO - add zO
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "CollapseAllRegions",
                          Command.Type.OTHER_READONLY,
                          new Shortcut("zM"));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "CollapseRegion",
                          Command.Type.OTHER_READONLY,
                          new Shortcut("zc"));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "ExpandAllRegions",
                          Command.Type.OTHER_READONLY,
                          new Shortcut("zR"));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "ExpandRegion",
                          Command.Type.OTHER_READONLY,
                          new Shortcut("zo"));
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimToggleRecording",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_NO_ARG_RECORDING,
                          new Shortcut('q'), Argument.Type.CHARACTER);

    // Text Object Actions for Visual and Operator Pending Modes
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMark",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('`'), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMarkLine",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('\''), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMark",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMarkLine",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionTextOuterWord",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("aw"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionTextOuterBigWord",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("aW"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionTextInnerWord",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("iw"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionTextInnerBigWord",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("iW"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerParagraph",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("ip"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterParagraph",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("ap"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerSentence",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("is"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterSentence",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("as"));
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockAngle",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("i<"),
        new Shortcut("i>")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockBrace",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("iB"),
        new Shortcut("i{"),
        new Shortcut("i}")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockBracket",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("i["),
        new Shortcut("i]")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockParen",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("ib"),
        new Shortcut("i("),
        new Shortcut("i)")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("i\""),
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("i'"),
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionInnerBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("i`"),
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockAngle",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("a<"),
        new Shortcut("a>")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockBrace",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("aB"),
        new Shortcut("a{"),
        new Shortcut("a}")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockBracket",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("a["),
        new Shortcut("a]")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockParen",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("ab"),
        new Shortcut("a("),
        new Shortcut("a)")
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("a\""),
      });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("a'"),
    });
    parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionOuterBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("a`"),
    });
    parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_OP_PEND, "VimResetMode", Command.Type.RESET, new Shortcut(new KeyStroke[]{
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)
    }));

    // "Reserve" these keys so they don't work in IDEA. Eventually these may be valid plugin commands.
    parser.registerAction(KeyParser.MAPPING_ALL, "VimNotImplementedHandler", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK))
    });

    // Update many of the built-in IDEA actions with our key handlers.

    // Update completion actions
    parser.setupActionHandler("ClassNameCompletion", "VimClassNameCompletion");
    parser.setupActionHandler("CodeCompletion", "VimCodeCompletion");
    parser.setupActionHandler("SmartTypeCompletion", "VimSmartTypeCompletion");
    parser.setupActionHandler("InsertLiveTemplate", "VimInsertLiveTemplate");

    // Update generate actions
    parser.setupActionHandler("GenerateConstructor", "VimGenerateConstructor");
    parser.setupActionHandler("GenerateGetter", "VimGenerateGetter");
    parser.setupActionHandler("GenerateSetter", "VimGenerateSetter");
    parser.setupActionHandler("GenerateGetterAndSetter", "VimGenerateGetterAndSetter");
    parser.setupActionHandler("GenerateEquals", "VimGenerateEquals");

    parser.setupActionHandler("AutoIndentLines", "VimAutoIndentVisual");
    parser.setupActionHandler("ReformatCode", "VimReformatVisual");

    // This group allows us to propagate the keystroke if action acts on something other than an editor
    parser.setupActionHandler("EditorBackSpace", "VimEditorBackSpace", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
    parser.setupActionHandler("EditorDelete", "VimEditorDelete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    parser.setupActionHandler("EditorDown", "VimEditorDown", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
    parser.setupActionHandler("EditorEnter", "VimEditorEnter", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), true);
    parser.setupActionHandler("EditorEscape", "VimEditorEscape", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), true);
    parser.setupActionHandler("EditorLeft", "VimEditorLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
    parser.setupActionHandler("EditorLineStart", "VimEditorLineStart", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
    parser.setupActionHandler("EditorLineEnd", "VimEditorLineEnd", KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
    parser.setupActionHandler("EditorPageDown", "VimEditorPageDown", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
    parser.setupActionHandler("EditorPageUp", "VimEditorPageUp", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
    parser.setupActionHandler("EditorRight", "VimEditorRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
    parser.setupActionHandler("EditorTab", "VimEditorTab", KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), true);
    parser.setupActionHandler("EditorToggleInsertState", "VimEditorToggleInsertState", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
    parser.setupActionHandler("EditorUp", "VimEditorUp", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));

    // All the Alt keys

    // All the Ctrl keys
    parser.setupActionHandler("EditorDeleteToWordEnd", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK));
    parser
      .setupActionHandler("EditorDeleteToWordStart", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorScrollDown", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorPreviousWord", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorNextWord", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorScrollUp", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorTextStart", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorTextEnd", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorMoveToPageTop", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK));
    parser
      .setupActionHandler("EditorMoveToPageBottom", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK));

    // All the Shift keys
    parser.setupActionHandler("EditorDownWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorLeftWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorRightWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorUpWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorLineStartWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorLineEndWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorPageUpWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorPageDownWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK));

    // All the Ctrl-Shift keys
    parser.setupActionHandler("EditorPreviousWordWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorNextWordWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorTextStartWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorTextEndWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorMoveToPageTopWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorMoveToPageBottomWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
  }

  private static RegisterActions instance;
}
