package com.maddyhome.idea.vim.group;

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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorMouseAdapter;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.CharacterHelper;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.ui.CommandEntryPanel;
import com.maddyhome.idea.vim.undo.UndoManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.KeyStroke;

/**
 * Provides all the insert/replace related functionality
 */
public class ChangeGroup extends AbstractActionGroup
{
    /** Special command flag that indicates it is not to be repeated */
    public static final int NO_REPEAT = 1;
    public static final int CLEAR_STROKES = 2;
    public static final int SAVE_STROKE = 4;

    /**
     * Creates the group
     */
    public ChangeGroup()
    {
        // We want to know when a user clicks the mouse somewhere in the editor so we can clear any
        // saved text for the current insert mode.
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            public void editorCreated(EditorFactoryEvent event)
            {
                Editor editor = event.getEditor();
                editor.addEditorMouseListener(new EditorMouseAdapter() {
                    public void mouseClicked(EditorMouseEvent event)
                    {
                        if (!VimPlugin.isEnabled()) return;

                        if (CommandState.getInstance().getMode() == CommandState.MODE_INSERT ||
                            CommandState.getInstance().getMode() == CommandState.MODE_REPLACE)
                        {
                            clearStrokes(event.getEditor());
                        }
                    }
                });
            }

       });
    }

    /**
     * Begin insert before the cursor position
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertBeforeCursor(Editor editor, DataContext context)
    {
        initInsert(editor, context, CommandState.MODE_INSERT);
    }

    /**
     * Begin insert before the first non-blank on the current line
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertBeforeFirstNonBlank(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
    }

    /**
     * Begin insert before the start of the current line
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertLineStart(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
    }

    /**
     * Begin insert after the cursor position
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertAfterCursor(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretHorizontalAppend(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
    }

    /**
     * Begin insert after the end of the current line
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertAfterLineEnd(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEndAppend(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
    }

    /**
     * Begin insert before the current line by creating a new blank line above the current line
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertNewLineAbove(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
        KeyHandler.executeAction("VimEditorEnter", context);
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretVertical(editor, -1));
    }

    /**
     * Begin insert after the current line by creating a new blank line below the current line
     * @param editor The editor to insert into
     * @param context The data context
     */
    public void insertNewLineBelow(Editor editor, DataContext context)
    {
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEndAppend(editor));
        initInsert(editor, context, CommandState.MODE_INSERT);
        KeyHandler.executeAction("VimEditorEnter", context);
    }

    public void insertAtPreviousInsert(Editor editor, DataContext context)
    {
        int offset = CommandGroups.getInstance().getMotion().moveCaretToFileMarkLine(editor, context, '^');
        if (offset != -1)
        {
            MotionGroup.moveCaret(editor, context, offset);
        }

        insertBeforeCursor(editor, context);
    }

    public void insertPreviousInsert(Editor editor, DataContext context, boolean exit)
    {
        repeatInsertText(editor, context, 1);
        //strokes.addAll(lastStrokes);
        if (exit)
        {
            processEscape(editor, context);
        }
    }

    public boolean insertRegister(Editor editor, DataContext context, char key)
    {
        Register register = CommandGroups.getInstance().getRegister().getRegister(key);
        if (register != null)
        {
            String text = register.getText();
            for (int i = 0; i < text.length(); i++)
            {
                processKey(editor, context, KeyStroke.getKeyStroke(text.charAt(i)));
            }

            return true;
        }

        return false;
    }

    public boolean insertCharacterAroundCursor(Editor editor, DataContext context, int dir)
    {
        boolean res = false;

        VisualPosition vp = editor.getCaretModel().getVisualPosition();
        vp = new VisualPosition(vp.line + dir, vp.column);
        int len = EditorHelper.getLineLength(editor, EditorHelper.visualLineToLogicalLine(editor, vp.line));
        if (vp.column < len)
        {
            int offset = EditorHelper.visualPostionToOffset(editor, vp);
            char ch = editor.getDocument().getChars()[offset];
            processKey(editor, context, KeyStroke.getKeyStroke(ch));
            res = true;
        }

        return res;
    }

    public boolean insertDeletePreviousWord(Editor editor, DataContext context)
    {
        int deleteTo = insertStart;
        int offset = editor.getCaretModel().getOffset();
        if (offset == insertStart)
        {
            deleteTo = CommandGroups.getInstance().getMotion().moveCaretToNextWord(editor, -1, false);
        }

        if (deleteTo != -1)
        {
            deleteRange(editor, context, new TextRange(deleteTo, offset), MotionGroup.EXCLUSIVE);
            
            return true;
        }

        return false;
    }

    /**
     * Begin insert/replace mode
     * @param editor The editor to insert into
     * @param context The data context
     * @param mode The mode - inidicate insert or replace
     */
    private void initInsert(Editor editor, DataContext context, int mode)
    {
        CommandState state = CommandState.getInstance();

        insertStart = editor.getCaretModel().getOffset();
        CommandGroups.getInstance().getMark().setMark(editor, context, '[', insertStart);

        // If we are repeating the last insert/replace
        if (state.getMode() == CommandState.MODE_REPEAT)
        {
            // If this command doesn't allow repeating, set the count to 1
            if ((state.getCommand().getFlags() & NO_REPEAT) != 0)
            {
                repeatInsert(editor, context, 1);
            }
            else
            {
                repeatInsert(editor, context, state.getCommand().getCount());
            }
        }
        // Here we begin insert/replace mode
        else
        {
            lastInsert = state.getCommand();
            strokes.clear();
            if (mode == CommandState.MODE_REPLACE)
            {
                processInsert(editor, context);
            }
            state.setMode(mode);
            state.setMappingMode(KeyParser.MAPPING_INSERT);
        }
    }

    /**
     * This repeats the previous insert count times
     * @param editor The editor to insert into
     * @param context The data context
     * @param count The number of times to repeat the previous insert
     */
    private void repeatInsert(Editor editor, DataContext context, int count)
    {
        repeatInsertText(editor, context, count);

        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1));
    }

    /**
     * This repeats the previous insert count times
     * @param editor The editor to insert into
     * @param context The data context
     * @param count The number of times to repeat the previous insert
     */
    private void repeatInsertText(Editor editor, DataContext context, int count)
    {
        for (int i = 0; i < count; i++)
        {
            // Treat other keys special by performing the appropriate action they represent in insert/replace mode
            for (int k = 0; k < lastStrokes.size(); k++)
            {
                Object obj = lastStrokes.get(k);
                if (obj instanceof AnAction)
                {
                    KeyHandler.executeAction((AnAction)obj, context);
                    strokes.add(obj);
                }
                else if (obj instanceof Character)
                {
                    processKey(editor, context, KeyStroke.getKeyStroke(((Character)obj).charValue()));
                }
            }
        }
    }

    /**
     * Terminate insert/replace mode after the user presses Escape or Ctrl-C
     * @param editor The editor that was being edited
     * @param context The data context
     */
    public void processEscape(Editor editor, DataContext context)
    {
        // TODO - register '.' needs to get text of insert
        logger.debug("processing escape");
        int cnt = lastInsert.getCount();
        // Turn off overwrite mode if we were in replace mode
        if (CommandState.getInstance().getMode() == CommandState.MODE_REPLACE)
        {
            KeyHandler.executeAction("VimInsertReplaceToggle", context);
        }
        // If this command doesn't allow repeats, set count to 1
        if ((lastInsert.getFlags() & NO_REPEAT) != 0)
        {
            cnt = 1;
        }

        // Save off current list of keystrokes
        lastStrokes = new ArrayList(strokes);

        // If the insert/replace command was preceded by a count, repeat again N - 1 times
        repeatInsert(editor, context, cnt - 1);
        CommandGroups.getInstance().getMark().setMark(editor, context, '^', editor.getCaretModel().getOffset());
        CommandGroups.getInstance().getMark().setMark(editor, context, ']', editor.getCaretModel().getOffset());
        CommandState.getInstance().reset();
        UndoManager.getInstance().endCommand(editor);
    }

    /**
     * Processes the user pressing the Enter key. If this is REPLACE mode we need to turn off OVERWRITE before and
     * then turn OVERWRITE back on after sending the "Enter" key.
     * @param editor The editor to press "Enter" in
     * @param context The data context
     */
    public void processEnter(Editor editor, DataContext context)
    {
        if (CommandState.getInstance().getMode() == CommandState.MODE_REPLACE)
        {
            KeyHandler.executeAction("VimEditorToggleInsertState", context);
        }
        KeyHandler.executeAction("VimEditorEnter", context);
        if (CommandState.getInstance().getMode() == CommandState.MODE_REPLACE)
        {
            KeyHandler.executeAction("VimEditorToggleInsertState", context);
        }
    }

    /**
     * Processes the user pressing the Insert key while in INSERT or REPLACE mode. This simply toggles the
     * Insert/Overwrite state which updates the status bar.
     * @param editor The editor to toggle the state in
     * @param context The data context
     */
    public void processInsert(Editor editor, DataContext context)
    {
        KeyHandler.executeAction("VimEditorToggleInsertState", context);
        CommandState.getInstance().toggleInsertOverwrite();
    }

    /**
     * While in INSERT or REPLACE mode the user can enter a single NORMAL mode command and then automatically
     * return to INSERT or REPLACE mode.
     * @param editor The editor to put into NORMAL mode for one command
     * @param context The data context
     */
    public void processSingleCommand(Editor editor, DataContext context)
    {
        CommandState.getInstance().saveMode();
        clearStrokes(editor);
    }

    /**
     * This processes all "regular" keystrokes entered while in insert/replace mode
     * @param editor The editor the character was typed into
     * @param context The data context
     * @param key The user entered keystroke
     * @return true if this was a regular character, false if not
     */
    public boolean processKey(Editor editor, DataContext context, KeyStroke key)
    {
        logger.debug("processKey(" + key + ")");

        if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
        {
            // Regular characters are not handled by us, pass them back to Idea. We just keep track of the keystroke
            // for repeating later.
            strokes.add(new Character(key.getKeyChar()));

            KeyHandler.getInstance().getOriginalHandler().execute(editor, key.getKeyChar(), context);

            return true;
        }

        return false;
    }

    /**
     * This processes all keystrokes in Insert/Replace mode that were converted into Commands. Some of these
     * commands need to be saved off so the inserted/replaced text can be repeated properly later if needed.
     * @param editor The editor the command was executed in
     * @param context The data context
     * @param cmd The command that was executed
     * @return true if the command was stored for later repeat, false if not
     */
    public boolean processCommand(Editor editor, DataContext context, Command cmd)
    {
        if ((cmd.getFlags() & SAVE_STROKE) != 0)
        {
            strokes.add(cmd.getAction());

            return true;
        }
        else if ((cmd.getFlags() & CLEAR_STROKES) != 0)
        {
            clearStrokes(editor);
            return false;
        }
        else
        {
            return false;
        }
    }

    private void clearStrokes(Editor editor)
    {
        strokes.clear();
        insertStart = editor.getCaretModel().getOffset();
    }

    /**
     * Deletes count characters from the editor
     * @param editor The editor to remove the characters from
     * @param context The data context
     * @param count The number of characters to delete
     * @return true if able to delete, false if not
     */
    public boolean deleteCharacter(Editor editor, DataContext context, int count)
    {
        int offset = CommandGroups.getInstance().getMotion().moveCaretHorizontalAppend(editor, count);
        if (offset != -1)
        {
            boolean res = deleteText(editor, context, editor.getCaretModel().getOffset(), offset, MotionGroup.INCLUSIVE);
            int pos = editor.getCaretModel().getOffset();
            int norm = EditorHelper.normalizeOffset(editor, EditorHelper.getCurrentLogicalLine(editor), pos, false);
            if (norm != pos)
            {
                MotionGroup.moveCaret(editor, context, norm);
            }

            return res;
        }

        return false;
    }

    /**
     * Deletes count lines including the current line
     * @param editor The editor to remove the lines from
     * @param context The data context
     * @param count The number of lines to delete
     * @return true if able to delete the lines, false if not
     */
    public boolean deleteLine(Editor editor, DataContext context, int count)
    {
        int start = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
        int offset = Math.min(CommandGroups.getInstance().getMotion().moveCaretToLineEndAppendOffset(editor,
            count - 1) + 1, EditorHelper.getFileSize(editor));
        if (offset != -1)
        {
            boolean res = deleteText(editor, context, start, offset, MotionGroup.LINEWISE);
            if (res && editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) &&
                editor.getCaretModel().getOffset() != 0)
            {
                MotionGroup.moveCaret(editor, context,
                    CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, -1));
            }

            return res;
        }

        return false;
    }

    /**
     * Delete from the cursor to the end of count - 1 lines down
     * @param editor The editor to delete from
     * @param context The data context
     * @param count The number of lines affected
     * @return true if able to delete the text, false if not
     */
    public boolean deleteEndOfLine(Editor editor, DataContext context, int count)
    {
        int offset = CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(editor, count - 1) + 1;
        if (offset != -1)
        {
            boolean res = deleteText(editor, context, editor.getCaretModel().getOffset(), offset, MotionGroup.INCLUSIVE);
            int pos = CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1);
            if (pos != -1)
            {
                MotionGroup.moveCaret(editor, context, pos);
            }

            return res;
        }

        return false;
    }

    /**
     * Joins count lines togetheri starting at the cursor. No count or a count of one still joins two lines.
     * @param editor The editor to join the lines in
     * @param context The data context
     * @param count The number of lines to join
     * @param spaces If true the joined lines will have one space between them and any leading space on the second line
     *        will be removed. If false, only the newline is removed to join the lines.
     * @return true if able to join the lines, false if not
     */
    public boolean deleteJoinLines(Editor editor, DataContext context, int count, boolean spaces)
    {
        if (count < 2) count = 2;
        int lline = EditorHelper.getCurrentLogicalLine(editor);
        int total = EditorHelper.getLineCount(editor);
        if (lline + count > total)
        {
            return false;
        }

        return deleteJoinNLines(editor, context, lline, count, spaces);
    }

    /**
     * Joins all the lines selected by the current visual selection.
     * @param editor The editor to join the lines in
     * @param context The data context
     * @param range The range of the visual selection
     * @param spaces If true the joined lines will have one space between them and any leading space on the second line
     *        will be removed. If false, only the newline is removed to join the lines.
     * @return true if able to join the lines, false if not
     */
    public boolean deleteJoinRange(Editor editor, DataContext context, TextRange range, boolean spaces)
    {
        int startLine = editor.offsetToLogicalPosition(range.getStartOffset()).line;
        int endLine = editor.offsetToLogicalPosition(range.getEndOffset()).line;
        int count = endLine - startLine + 1;
        if (count < 2) count = 2;

        return deleteJoinNLines(editor, context, startLine, count, spaces);
    }

    /**
     * This does the actual joining of the lines
     * @param editor The editor to join the lines in
     * @param context The data context
     * @param startLine The starting logical line
     * @param count The number of lines to join including startLine
     * @param spaces If true the joined lines will have one space between them and any leading space on the second line
     *        will be removed. If false, only the newline is removed to join the lines.
     * @return true if able to join the lines, false if not
     */
    private boolean deleteJoinNLines(Editor editor, DataContext context, int startLine, int count, boolean spaces)
    {
        // start my moving the cursor to the very end of the first line
        MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretToLineEndAppend(editor, startLine));
        for (int i = 1; i < count; i++)
        {
            int start = CommandGroups.getInstance().getMotion().moveCaretToLineEndAppend(editor);
            MotionGroup.moveCaret(editor, context, start);
            int offset;
            if (spaces)
            {
                offset = CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, 1);
            }
            else
            {
                offset = CommandGroups.getInstance().getMotion().moveCaretToLineStartOffset(editor, 1);
            }
            deleteText(editor, context, editor.getCaretModel().getOffset(), offset, MotionGroup.INCLUSIVE);
            if (spaces)
            {
                insertText(editor, context, start, " ");
                MotionGroup.moveCaret(editor, context, CommandGroups.getInstance().getMotion().moveCaretHorizontal(editor, -1));
            }
        }

        return true;
    }

    /**
     * Delete all text moved over by the supplied motion command argument.
     * @param editor The editor to delete the text from
     * @param context The data context
     * @param count The number of times to repear the deletion
     * @param rawCount The actual count entered by the user
     * @param argument The motion command
     * @return true if able to delete the text, false if not
     */
    public boolean deleteMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument)
    {
        TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, false);

        return deleteRange(editor, context, range, argument.getMotion().getFlags());
    }

    /**
     * Delete the range of text.
     * @param editor The editor to delete the text from
     * @param context The data context
     * @param range The range to delete
     * @param type The type of deletion (LINEWISE, EXCLUSIVE, or INCLUSIVE)
     * @return true if able to delete the text, false if not
     */
    public boolean deleteRange(Editor editor, DataContext context, TextRange range, int type)
    {
        if (range == null)
        {
            return false;
        }
        else
        {
            boolean res = deleteText(editor, context, range.getStartOffset(), range.getEndOffset(), type);
            if (res && editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) &&
                editor.getCaretModel().getOffset() != 0)
            {
                MotionGroup.moveCaret(editor, context,
                    CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeadingOffset(editor, -1));
            }

            return res;
        }
    }

    /**
     * Begin Replace mode
     * @param editor The editor to replace in
     * @param context The data context
     * @return true
     */
    public boolean changeReplace(Editor editor, DataContext context)
    {
        initInsert(editor, context, CommandState.MODE_REPLACE);

        return true;
    }

    /**
     * Replace each of the next count characters with the charcter ch
     * @param editor The editor to chage
     * @param context The data context
     * @param count The number of characters to change
     * @param ch The character to change to
     * @return true if able to change count characters, false if not
     */
    public boolean changeCharacter(Editor editor, DataContext context, int count, char ch)
    {
        int col = EditorHelper.getCurrentLogicalColumn(editor);
        int len = EditorHelper.getLineLength(editor);
        int offset = editor.getCaretModel().getOffset();
        if (len - col < count)
        {
            return false;
        }

        StringBuffer repl = new StringBuffer(count);
        for (int i = 0; i < count; i++)
        {
            repl.append(ch);
        }

        replaceText(editor, context, offset, offset + count, repl.toString());

        return true;
    }

    /**
     * Each character in the supplied range gets replaced with the character ch
     * @param editor The editor to change
     * @param context The data context
     * @param range The range to change
     * @param ch The replacing character
     * @return true if able to change the range, false if not
     */
    public boolean changeCharacterRange(Editor editor, DataContext context, TextRange range, char ch)
    {
        char[] chars = editor.getDocument().getChars();
        for (int i = range.getStartOffset(); i < range.getEndOffset(); i++)
        {
            if ('\n' != chars[i])
            {
                replaceText(editor, context, i, i + 1, Character.toString(ch));
            }
        }
        return true;
    }

    /**
     * Delete count characters and then enter insert mode
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of characters to change
     * @return true if able to delete count characters, false if not
     */
    public boolean changeCharacters(Editor editor, DataContext context, int count)
    {
        boolean res = deleteCharacter(editor, context, count);
        if (res)
        {
            initInsert(editor, context, CommandState.MODE_INSERT);
        }

        return res;
    }

    /**
     * Delete count lines and then enter insert mode
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of lines to change
     * @return true if able to delete count lines, false if not
     */
    public boolean changeLine(Editor editor, DataContext context, int count)
    {
        boolean res = deleteLine(editor, context, count);
        if (res)
        {
            insertNewLineAbove(editor, context);
        }

        return res;
    }

    /**
     * Delete from the cursor to the end of count - 1 lines down and enter insert mode
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of lines to change
     * @return true if able to delete count lines, false if not
     */
    public boolean changeEndOfLine(Editor editor, DataContext context, int count)
    {
        boolean res = deleteEndOfLine(editor, context, count);
        if (res)
        {
            insertAfterLineEnd(editor, context);
        }

        return res;
    }

    /**
     * Delete the text covered by the motion command argument and enter insert mode
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of time to repeat the change
     * @param rawCount The actual count entered by the user
     * @param argument The motion command
     * @return true if able to delete the text, false if not
     */
    public boolean changeMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument)
    {
        // TODO: Hack - find better way to do this exceptional case - at least make constants out of these strings

        // Vim treats cw as ce and cW as cE if cursor is on a non-blank character
        String id = ActionManager.getInstance().getId(argument.getMotion().getAction());
        if (id.equals("VimMotionWordRight"))
        {
            if (!Character.isWhitespace(editor.getDocument().getChars()[editor.getCaretModel().getOffset()]))
            {
                argument.getMotion().setAction(ActionManager.getInstance().getAction("VimMotionWordEndRight"));
                argument.getMotion().setFlags(MotionGroup.INCLUSIVE);
            }
        }
        else if (id.equals("VimMotionWORDRight"))
        {
            if (!Character.isWhitespace(editor.getDocument().getChars()[editor.getCaretModel().getOffset()]))
            {
                argument.getMotion().setAction(ActionManager.getInstance().getAction("VimMotionWORDEndRight"));
                argument.getMotion().setFlags(MotionGroup.INCLUSIVE);
            }
        }

        boolean res = deleteMotion(editor, context, count, rawCount, argument);
        if (res)
        {
            insertBeforeCursor(editor, context);
        }

        return res;
    }

    /**
     * Deletes the range of text and enters insert mode
     * @param editor The editor to change
     * @param context The data context
     * @param range The range to change
     * @param type The type of the range (LINEWISE, CHARACTERWISE)
     * @return true if able to delete the range, false if not
     */
    public boolean changeRange(Editor editor, DataContext context, TextRange range, int type)
    {
        boolean after = range.getEndOffset() >= EditorHelper.getFileSize(editor);
        boolean res = deleteRange(editor, context, range, type);
        if (res)
        {
            if (type == MotionGroup.LINEWISE)
            {
                if (after)
                {
                    insertNewLineBelow(editor, context);
                }
                else
                {
                    insertNewLineAbove(editor, context);
                }
            }
            else
            {
                insertBeforeCursor(editor, context);
            }
        }

        return res;
    }

    /**
     * Toggles the case of count characters
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of characters to change
     * @return true if able to change count characters
     */
    public boolean changeCaseToggleCharacter(Editor editor, DataContext context, int count)
    {
        int offset = CommandGroups.getInstance().getMotion().moveCaretHorizontalAppend(editor, count);
        if (offset == -1)
        {
            return false;
        }
        else
        {
            changeCase(editor, context, editor.getCaretModel().getOffset(), offset, CharacterHelper.CASE_TOGGLE);

            offset = EditorHelper.normalizeOffset(editor, offset, false);
            MotionGroup.moveCaret(editor, context, offset);

            return true;
        }
    }

    /**
     * Changes the case of all the character moved over by the motion argument.
     * @param editor The editor to change
     * @param context The data context
     * @param count The number of times to repeat the change
     * @param rawCount The actual count entered by the user
     * @param type The case change type (TOGGLE, UPPER, LOWER)
     * @param argument The motion command
     * @return true if able to delete the text, false if not
     */
    public boolean changeCaseMotion(Editor editor, DataContext context, int count, int rawCount, char type, Argument argument)
    {
        TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, false);

        return changeCaseRange(editor, context, range, type);
    }

    /**
     * Changes the case of all the characters in the range
     * @param editor The editor to change
     * @param context The data context
     * @param range The range to change
     * @param type The case change type (TOGGLE, UPPER, LOWER)
     * @return true if able to delete the text, false if not
     */
    public boolean changeCaseRange(Editor editor, DataContext context, TextRange range, char type)
    {
        if (range == null)
        {
            return false;
        }
        else
        {
            changeCase(editor, context, range.getStartOffset(), range.getEndOffset(), type);
            MotionGroup.moveCaret(editor, context, range.getStartOffset());

            return true;
        }
    }

    /**
     * This performs the actual case change.
     * @param editor The editor to change
     * @param context The data context
     * @param start The start offset to change
     * @param end The end offset to change
     * @param type The type of change (TOGGLE, UPPER, LOWER)
     */
    private void changeCase(Editor editor, DataContext context, int start, int end, char type)
    {
        if (start > end)
        {
            int t = end;
            end = start;
            start = t;
        }

        char[] chars = editor.getDocument().getChars();
        for (int i = start; i < end; i++)
        {
            char ch = CharacterHelper.changeCase(chars[i], type);
            if (ch != chars[i])
            {
                replaceText(editor, context, i, i + 1, Character.toString(ch));
            }
        }
    }

    public void indentLines(Editor editor, DataContext context, int lines, int dir)
    {
        int start = editor.getCaretModel().getOffset();
        int end = CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(editor, lines - 1);

        indentRange(editor, context, new TextRange(start, end), 1, dir);
    }

    public void indentMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument, int dir)
    {
        TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, false);

        indentRange(editor, context, range, 1, dir);
    }

    public void indentRange(Editor editor, DataContext context, TextRange range, int count, int dir)
    {
        if (range == null) return;

        Project proj = (Project)context.getData(DataConstants.PROJECT);
        int tabSize = editor.getSettings().getTabSize(proj);
        boolean useTabs = editor.getSettings().isUseTabCharacter(proj);

        int sline = editor.offsetToLogicalPosition(range.getStartOffset()).line;
        int eline = editor.offsetToLogicalPosition(range.getEndOffset()).line;
        int eoff = EditorHelper.getLineStartForOffset(editor, range.getEndOffset());
        if (eoff == range.getEndOffset())
        {
            eline--;
        }

        for (int l = sline; l <= eline; l++)
        {
            int soff = editor.getDocument().getLineStartOffset(l);
            int woff = CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor, l);
            int col = editor.offsetToVisualPosition(woff).column;
            int newCol = Math.max(0, col + dir * tabSize * count);
            if (dir == 1 || col > 0)
            {
                StringBuffer space = new StringBuffer();
                int tabCnt = 0;
                int spcCnt = 0;
                if (useTabs)
                {
                    tabCnt = newCol / tabSize;
                    spcCnt = newCol % tabSize;
                }
                else
                {
                    spcCnt = newCol;
                }

                for (int i = 0; i < tabCnt; i++)
                {
                    space.append('\t');
                }
                for (int i = 0; i < spcCnt; i++)
                {
                    space.append(' ');
                }

                replaceText(editor, context, soff, woff, space.toString());
            }
        }

        if (CommandState.getInstance().getMode() != CommandState.MODE_INSERT &&
            CommandState.getInstance().getMode() != CommandState.MODE_REPLACE)
        {
            MotionGroup.moveCaret(editor, context,
                CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor, sline));
        }

        EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
    }

    /**
     * Insert text into the document
     * @param editor The editor to insert into
     * @param context The data context
     * @param start The starting offset to insert at
     * @param str The text to insert
     */
    public void insertText(Editor editor, DataContext context, int start, String str)
    {
        editor.getDocument().insertString(start, str);
        editor.getCaretModel().moveToOffset(start + str.length());

        CommandGroups.getInstance().getMark().setMark(editor, context, '.', start);
        //CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
        //CommandGroups.getInstance().getMark().setMark(editor, context, ']', start + str.length());

        //CommandGroups.getInstance().getRegister().storeTextInternal(editor, context, start, start + str.length(), str, MotionGroup.CHARACTERWISE, '.', false, false);
        //runCommand(new InsertText(editor, context, start, str));
    }

    /**
     * Delete text from the document. This will fail if being asked to store the deleted text into a read-only
     * register.
     * @param editor The editor to delete from
     * @param context The data context
     * @param start The start offset to delete
     * @param end The end offset to delete
     * @param type The type of deletion (LINEWISE, CHARACTERWISE)
     * @return true if able to delete the text, false if not
     */
    private boolean deleteText(Editor editor, DataContext context, int start, int end, int type)
    {
        if (start > end)
        {
            int t = start;
            start = end;
            end = t;
        }

        start = Math.max(0, Math.min(start, EditorHelper.getFileSize(editor)));
        end = Math.max(0, Math.min(end, EditorHelper.getFileSize(editor)));

        if (CommandGroups.getInstance().getRegister().storeText(editor, context, start, end, type, true, false))
        {
            editor.getDocument().deleteString(start, end);

            CommandGroups.getInstance().getMark().setMark(editor, context, '.', start);
            CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
            CommandGroups.getInstance().getMark().setMark(editor, context, ']', start);
            //runCommand(new DeleteText(editor, context, start, end));

            return true;
        }

        return false;
    }

    /**
     * Replace text in the editor
     * @param editor The editor to replace text in
     * @param context The data context
     * @param start The start offset to change
     * @param end The end offset to change
     * @param str The new text
     */
    private void replaceText(Editor editor, DataContext context, int start, int end, String str)
    {
        editor.getDocument().replaceString(start, end, str);

        CommandGroups.getInstance().getMark().setMark(editor, context, '[', start);
        CommandGroups.getInstance().getMark().setMark(editor, context, ']', start + str.length());
        CommandGroups.getInstance().getMark().setMark(editor, context, '.', start + str.length());
        //runCommand(new ReplaceText(editor, context, start, end, str));
    }

    /*
    public static void runCommand(Runnable cmd)
    {
        CommandProcessor.getInstance().executeCommand(new WriteAction(cmd), "Foo", "Bar");
    }

    static class WriteAction implements Runnable
    {
        WriteAction(Runnable cmd)
        {
            this.cmd = cmd;
        }

        public void run()
        {
            ApplicationManager.getApplication().runWriteAction(cmd);
        }

        Runnable cmd;
    }

    static class InsertText implements Runnable
    {
        InsertText(Editor editor, DataContext context, int start, String str)
        {
            this.editor = editor;
            this.context = context;
            this.start = start;
            this.str = str;
        }

        public void run()
        {
            editor.getDocument().insertString(start, str);
            editor.getCaretModel().moveToOffset(start + str.length());
        }

        Editor editor;
        DataContext context;
        int start;
        String str;
    }

    static class DeleteText implements Runnable
    {
        DeleteText(Editor editor, DataContext context, int start, int end)
        {
            this.editor = editor;
            this.context = context;
            this.start = start;
            this.end = end;
        }

        public void run()
        {
            editor.getDocument().deleteString(start, end);
        }

        Editor editor;
        DataContext context;
        int start;
        int end;
    }

    static class ReplaceText implements Runnable
    {
        ReplaceText(Editor editor, DataContext context, int start, int end, String str)
        {
            this.editor = editor;
            this.context = context;
            this.start = start;
            this.end = end;
            this.str = str;
        }

        public void run()
        {
            editor.getDocument().replaceString(start, end, str);
        }

        Editor editor;
        DataContext context;
        int start;
        int end;
        String str;
    }
    */

    /**
     * This class listens for editor tab changes so any insert/replace modes that need to be reset can be
     */
    public static class InsertCheck implements FileEditorManagerListener
    {
        /**
         * The user has changed the editor they are working with - exit insert/replace mode, and complete any
         * appropriate repeat.
         * @param event The file selection event
         */
        public void selectedFileChanged(FileEditorManagerEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            logger.debug("selected file changed");

            if (CommandState.getInstance().getMode() == CommandState.MODE_INSERT ||
                CommandState.getInstance().getMode() == CommandState.MODE_REPLACE)
            {
                // NOTE - is there a way to get the DataContext at this point?
                CommandGroups.getInstance().getChange().processEscape(EditorHelper.getEditor(event.getManager(), event.getOldFile()), null);
            }

            if (CommandEntryPanel.getInstance().isActive())
            {
                CommandEntryPanel.getInstance().deactivate();
            }
        }
    }

    private ArrayList strokes = new ArrayList();
    private ArrayList lastStrokes;
    private int insertStart;
    private Command lastInsert;

    private static Logger logger = Logger.getInstance(ChangeGroup.class.getName());
}
