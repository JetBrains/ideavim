package com.maddyhome.idea.vim.key;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * This registers all the key/action mappings for the plugin
 */
public class RegisterActions
{
    public static RegisterActions getInstance()
    {
        if (instance == null)
        {
            instance = new RegisterActions();
        }

        return instance;
    }

    private RegisterActions()
    {
        // Update many of the builting IDEA actions with our key handlers.
        // This group allows us to propagate the keystroke if action acts on something other than an editor
        KeyParser.setupActionHandler("EditorBackSpace", "VimEditorBackSpace", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        KeyParser.setupActionHandler("EditorDelete", "VimEditorDelete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        KeyParser.setupActionHandler("EditorDown", "VimEditorDown", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        KeyParser.setupActionHandler("EditorEnter", "VimEditorEnter", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        KeyParser.setupActionHandler("EditorEscape", "VimEditorEscape", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        KeyParser.setupActionHandler("EditorLeft", "VimEditorLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        KeyParser.setupActionHandler("EditorLineStart", "VimEditorLineStart", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
        KeyParser.setupActionHandler("EditorLineEnd", "VimEditorLineEnd", KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
        KeyParser.setupActionHandler("EditorPageUp", "VimEditorPageUp", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
        KeyParser.setupActionHandler("EditorRight", "VimEditorRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        KeyParser.setupActionHandler("EditorTab", "VimEditorTab", KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        KeyParser.setupActionHandler("EditorToggleInsertState", "VimEditorToggleInsertState", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
        KeyParser.setupActionHandler("EditorUp", "VimEditorUp", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));

        // All the Alt keys
        KeyParser.setupActionHandler("GenerateConstructor");
        KeyParser.setupActionHandler("GenerateGetter");
        KeyParser.setupActionHandler("GenerateSetter");
        KeyParser.setupActionHandler("GenerateGetterAndSetter");
        KeyParser.setupActionHandler("GenerateEquals");

        // All the Ctrl keys
        KeyParser.setupActionHandler("EditorDeleteToWordEnd", null, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorDeleteToWordStart", null, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorScrollDown", null, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorPreviousWord", null, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorNextWord", null, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorScrollUp", null, KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorTextStart", null, KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorTextEnd", null, KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorMoveToPageTop", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK));
        KeyParser.setupActionHandler("EditorMoveToPageBottom", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK));

        // All the Shift keys
        KeyParser.setupActionHandler("EditorDownWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorLeftWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorRightWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorUpWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorLineStartWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorLineEndWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorPageUpWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorPageDownWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK));

        // All the Ctrl-Shift keys
        KeyParser.setupActionHandler("EditorPreviousWordWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorNextWordWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorTextStartWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorTextEndWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorMoveToPageTopWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
        KeyParser.setupActionHandler("EditorMoveToPageBottomWithSelection", null, KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));

        KeyParser parser = KeyParser.getInstance();

        // ******************* Insert Mode Actions **********************
        parser.registerAction(KeyParser.MAPPING_INSERT, "ClassNameCompletion", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "CodeCompletion", Command.INSERT, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "InsertLiveTemplate", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(']', KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorBackSpace", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorDelete", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorDown", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorTab", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimEditorUp", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "SmartTypeCompletion", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertCharacterAboveCursor", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertCharacterBelowCursor", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertDeleteInsertedText", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertDeletePreviousWord", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertEnter", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertExitMode", Command.INSERT, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertHelp", Command.INSERT, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertPreviousInsert", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertPreviousInsertExit", Command.INSERT, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertRegister", Command.INSERT, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertReplaceToggle", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertSingleCommand", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionFirstColumn", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionGotoLineFirst", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionGotoLineLastEnd", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionLastColumn", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionLeft", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionRight", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionScrollPageUp", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionScrollPageDown", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionWordLeft", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimMotionWordRight", Command.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimShiftLeftLines", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_INSERT, "VimShiftRightLines", Command.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK)));

        // ************************* Visual Mode Actions **********************
        parser.registerAction(KeyParser.MAPPING_VISUAL, "AutoIndentLines", Command.CHANGE, new Shortcut('='));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "ReformatCode", Command.CHANGE, new Shortcut("gq"));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseLowerVisual", Command.CHANGE, new Shortcut('u'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseToggleVisual", Command.CHANGE, new Shortcut('~'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeCaseUpperVisual", Command.CHANGE, new Shortcut('U'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisual", Command.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut[] {
            new Shortcut('c'),
            new Shortcut('s')
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisualCharacter", Command.CHANGE, new Shortcut('r'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimChangeVisualLines", Command.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut[] {
            new Shortcut('C'),
            new Shortcut('R'),
            new Shortcut('S')
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimCopyYankVisual", Command.COPY, new Shortcut('y'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimCopyYankVisualLines", Command.COPY, new Shortcut('Y'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteJoinVisualLines", Command.DELETE, new Shortcut("gJ"));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteJoinVisualLinesSpaces", Command.DELETE, new Shortcut('J'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteVisual", Command.DELETE, new Shortcut[] {
            new Shortcut('d'),
            new Shortcut('x'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimDeleteVisualLines", Command.DELETE, new Shortcut[] {
            new Shortcut('D'),
            new Shortcut('X')
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimFilterVisualLines", Command.CHANGE, new Shortcut('!'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimShiftLeftVisual", Command.CHANGE, new Shortcut('<'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimShiftRightVisual", Command.CHANGE, new Shortcut('>'));
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualExitMode", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualPutText", Command.PASTE, new Shortcut[] {
            new Shortcut('P'),
            new Shortcut('p')
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualSwapEnds", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut('O'),
            new Shortcut('o')
        });
        parser.registerAction(KeyParser.MAPPING_VISUAL, "VimVisualSwapSelections", Command.OTHER_READONLY, new Shortcut("gv"));

        // ************************* Normal Mode Actions *************************
        // Copy/Paste Actions
        // TODO - add gP
        // TODO - add gp
        parser.registerAction(KeyParser.MAPPING_NORMAL, "HelpTopics", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0))
        });
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextBeforeCursor", Command.PASTE, new Shortcut('P'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyPutTextAfterCursor", Command.PASTE, new Shortcut('p'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankLine", Command.COPY, new Shortcut('Y'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankLine", Command.COPY, new Shortcut("yy"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimCopyYankMotion", Command.COPY, Command.FLAG_OP_PEND, new Shortcut('y'), Argument.MOTION);

        // Insert/Replace/Change Actions
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseLowerMotion", Command.CHANGE, Command.FLAG_OP_PEND, new Shortcut("gu"), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseToggleCharacter", Command.CHANGE, new Shortcut('~'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseToggleMotion", Command.CHANGE, Command.FLAG_OP_PEND, new Shortcut("g~"), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCaseUpperMotion", Command.CHANGE, Command.FLAG_OP_PEND, new Shortcut("gU"), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCharacter", Command.CHANGE, new Shortcut('r'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeCharacters", Command.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO, new Shortcut('s'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeEndOfLine", Command.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut('C'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeLine", Command.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut[] {
            new Shortcut("cc"),
            new Shortcut('S')
        });
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeMotion", Command.CHANGE, Command.FLAG_OP_PEND | Command.FLAG_MULTIKEY_UNDO, new Shortcut('c'), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimChangeReplace", Command.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut('R'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacter", Command.DELETE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacterLeft", Command.DELETE, new Shortcut('X'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteCharacterRight", Command.DELETE, new Shortcut('x'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteEndOfLine", Command.DELETE, new Shortcut('D'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteJoinLines", Command.DELETE, new Shortcut("gJ"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteJoinLinesSpaces", Command.DELETE, new Shortcut('J'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteLine", Command.DELETE, new Shortcut("dd"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimDeleteMotion", Command.DELETE, Command.FLAG_OP_PEND, new Shortcut('d'), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFilterCountLines", Command.CHANGE, new Shortcut("!!"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFilterMotion", Command.CHANGE, Command.FLAG_OP_PEND, new Shortcut('!'), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAfterCursor", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut('a'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAfterLineEnd", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut('A'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertAtPreviousInsert", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut("gi"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertBeforeCursor", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut[] {
            new Shortcut('i'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0))
        });
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertBeforeFirstNonBlank", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut('I'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertLineStart", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut("gI"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertNewLineAbove", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut('O'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimInsertNewLineBelow", Command.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut('o'));
        // Motion Actions
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMark", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('`'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimMotionGotoMarkLine", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('\''), Argument.CHARACTER);
        // Misc Actions
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimLastSearchReplace", Command.OTHER_WRITABLE, new Shortcut('&'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimLastGlobalSearchReplace", Command.OTHER_WRITABLE, new Shortcut("g&"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimVisualSelectPrevious", Command.OTHER_READONLY, new Shortcut("gv"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRepeatChange", Command.OTHER_WRITABLE, new Shortcut('.'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRepeatExCommand", Command.OTHER_WRITABLE, new Shortcut("@:"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "QuickJavaDoc", Command.OTHER_READONLY, new Shortcut('K'));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimRedo", Command.OTHER_WRITABLE, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimUndo", Command.OTHER_WRITABLE, new Shortcut[] {
            new Shortcut('u'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
        });

        // File Actions
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimFileSaveClose", Command.OTHER_WRITABLE, new Shortcut[] {
            new Shortcut("ZQ"),
            new Shortcut("ZZ")
        });

        // Shift Actions
        // TODO - add =
        // TODO - == will ignore count and only autoindent 1 lines
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimAutoIndentLines", Command.CHANGE, new Shortcut("=="));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftLeftLines", Command.CHANGE, new Shortcut("<<"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftLeftMotion", Command.CHANGE, new Shortcut('<'), Argument.MOTION);
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftRightLines", Command.CHANGE, new Shortcut(">>"));
        parser.registerAction(KeyParser.MAPPING_NORMAL, "VimShiftRightMotion", Command.CHANGE, new Shortcut('>'), Argument.MOTION);

        // Jump Actions
        // TODO - Tab, CTRL-I
        // TODO - CTRL-O
        // TODO - CTRL-T

        // Window Actions
        // TODO - CTRL-W commands: +, -, =, S, s, _, b, c, n, o, q, s, t, <up>, <down>

        // Macro Actions
        // TODO - add @
        // TODO - add @@
        // TODO - add q
        // TODO - support for :map macros

        // ************************* Normal, Operator Pending, Visual Mode Actions *************************
        parser.registerAction(KeyParser.MAPPING_NVO, "VimCopySelectRegister", Command.SELECT_REGISTER, Command.FLAG_EXPECT_MORE, new Shortcut('"'), Argument.CHARACTER);

        // Motion Actions
        // TODO - add ['
        // TODO - add [`
        // TODO - add ]'
        // TODO - add ]`
        // TODO - add g'
        // TODO - add g`
        // TODO - add z+
        // TODO - add z^
        // TODO - add ze
        // TODO - add zh, z<left>
        // TODO - add zj
        // TODO - add zk
        // TODO - add zl, z<right>
        // TODO - add zs

        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelEndLeft", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("]b"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelEndRight", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("]w"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelLeft", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut("[b"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionCamelRight", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut("[w"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionColumn", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut('|'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDown", Command.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[] {
            new Shortcut('j'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)),
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDown", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut("gj"),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0) })
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDownFirstNonSpace", Command.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[] {
            new Shortcut('+'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionDownLess1FirstNonSpace", Command.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut('_'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstColumn", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('0'),
            new Shortcut("g0"),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0) })
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstNonSpace", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('^'),
            new Shortcut("g^")
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionFirstScreenLine", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut[] {
            new Shortcut('H'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineFirst", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut[] {
            new Shortcut("gg"),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineLast", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('G'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionGotoLineLastEnd", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastColumn", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[] {
            new Shortcut('$'),
            new Shortcut("g$"),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0) })
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastMatchChar", Command.MOTION, new Shortcut(';'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastMatchCharReverse", Command.MOTION, new Shortcut(','));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastNonSpace", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("g_"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastScreenLine", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('L'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLastScreenLineEnd", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeft", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('h'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftMatchChar", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('F'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftTillMatchChar", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('T'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionLeftWrap", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMiddleColumn", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("gm"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionMiddleScreenLine", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('M'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionNthCharacter", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut("go"));
        // This represents two commands and one is linewise and the other is exclusive - the handler will fix it
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionPercentOrMatch", Command.MOTION, Command.FLAG_SAVE_JUMP, new Shortcut('%'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRight", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('l'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightMatchChar", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('f'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightTillMatchChar", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('t'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionRightWrap", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut(' '));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollFirstScreenLine", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut("zt"),
            // FIX - z<Enter> should move cursor to first non-blank on line
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0) })
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollHalfPageDown", Command.OTHER_READONLY, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollHalfPageUp", Command.OTHER_READONLY, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLastScreenLine", Command.OTHER_READONLY, new Shortcut[] {
            // FIX - z- should move cursor to first non-blank on line
            new Shortcut("z-"),
            new Shortcut("zb")
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLineDown", Command.OTHER_READONLY, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollLineUp", Command.OTHER_READONLY, new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollMiddleScreenLine", Command.OTHER_READONLY, new Shortcut[] {
            // FIX - z. should move cursor to first non-blank on line
            new Shortcut("z."),
            new Shortcut("zz")
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollPageDown", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionScrollPageUp", Command.OTHER_READONLY, new Shortcut[] {
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUp", Command.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[] {
            new Shortcut('k'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK)),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUp", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut("gk"),
            new Shortcut(new KeyStroke[] { KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0) })
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionUpFirstNonSpace", Command.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut('-'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordEndLeft", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("ge"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordEndRight", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('e'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordLeft", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('b'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWordRight", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('w'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWORDEndLeft", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut("gE"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWORDEndRight", Command.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut('E'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWORDLeft", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('B'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK))
        });
        parser.registerAction(KeyParser.MAPPING_NVO, "VimMotionWORDRight", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[] {
            new Shortcut('W'),
            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK))
        });

        // Misc Actions
        // TODO - finds should update visual mode selection
        // TODO - add #
        // TODO - add *
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchEntry", Command.OTHER_READONLY, Command.FLAG_SEARCH_FWD | Command.FLAG_SAVE_JUMP, new Shortcut('/'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchEntry", Command.OTHER_READONLY, Command.FLAG_SEARCH_REV | Command.FLAG_SAVE_JUMP, new Shortcut('?'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchAgainNext", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('n'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchAgainPrevious", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('N'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimExEntry", Command.OTHER_READONLY, new Shortcut(':'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWholeWordForward", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('*'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWholeWordBackward", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('#'));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWordForward", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut("g*"));
        parser.registerAction(KeyParser.MAPPING_NVO, "VimSearchWordBackward", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut("g#"));

        // ********************** Various Mode Actions ************************
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimVisualToggleCharacterMode", Command.OTHER_READONLY, Command.FLAG_MOT_CHARACTERWISE, new Shortcut('v'));
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimVisualToggleLineMode", Command.OTHER_READONLY, Command.FLAG_MOT_LINEWISE, new Shortcut('V'));
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "VimMotionMark", Command.OTHER_READONLY, new Shortcut('m'), Argument.CHARACTER);
        // FIX - make custom actions so can save jump
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "GotoDeclaration", Command.OTHER_READONLY | Command.FLAG_SAVE_JUMP, new Shortcut[] {
            new Shortcut("gD"),
            new Shortcut("gd")
        });
        // TODO - add zC
        // TODO - add zO
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "CollapseAllRegions", Command.OTHER_READONLY, new Shortcut("zM"));
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "CollapseRegion", Command.OTHER_READONLY, new Shortcut("zc"));
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "ExpandAllRegions", Command.OTHER_READONLY, new Shortcut("zR"));
        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_VISUAL, "ExpandRegion", Command.OTHER_READONLY, new Shortcut("zo"));

        // Text Object Actions for Visual and Operator Pending Modes
        // TODO - aW
        // TODO - a[
        // TODO - a]
        // TODO - aw
        // TODO - iW
        // TODO - i[
        // TODO - i]
        // TODO - iw
        parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMark", Command.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('`'), Argument.CHARACTER);
        parser.registerAction(KeyParser.MAPPING_VISUAL | KeyParser.MAPPING_OP_PEND, "VimMotionGotoFileMarkLine", Command.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('\''), Argument.CHARACTER);

        parser.registerAction(KeyParser.MAPPING_NORMAL | KeyParser.MAPPING_OP_PEND, "VimResetMode", Command.RESET, new Shortcut(new KeyStroke[] {
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
            KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)
        }));
    }

    private static RegisterActions instance;
}
