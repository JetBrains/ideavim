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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseEventArea;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.event.EditorMouseMotionListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.motion.MotionEditorAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualRange;
import com.maddyhome.idea.vim.common.Mark;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.option.BoundStringOption;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import java.awt.event.MouseEvent;

/**
 * This handles all motion related commands and marks
 * TODO:
 *    Jumplists
 */
public class MotionGroup extends AbstractActionGroup
{
    public static final int LAST_F = 1;
    public static final int LAST_f = 2;
    public static final int LAST_T = 3;
    public static final int LAST_t = 4;

    /**
     * Create the group
     */
    public MotionGroup()
    {
        // TODO - need to listen to document selection changes to turn off visual mode after an IDEA command is
        // run on the selection.
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            public void editorCreated(EditorFactoryEvent event)
            {
                Editor editor = event.getEditor();
                EditorMouseHandler handler = new EditorMouseHandler();
                editor.addEditorMouseListener(handler);
                editor.addEditorMouseMotionListener(handler);
            }
        });
    }

    /**
     * Handles mouse drags by properly setting up visual mode based on the new selection
     * @param editor The editor the mouse drag occured in
     * @param event The mouse event
     */
    private void processMouseDrag(Editor editor, MouseEvent event)
    {
        // Mouse drags can only result in CHARWISE selection
        // We want to move the mouse back one character to be consistence with how regular motion highlights text.
        // Don't move the cursor if the user ended up selecting no characters.
        // Once the cursor is set, save the current column.
        setVisualMode(editor, null, Command.FLAG_MOT_CHARACTERWISE);
        int offset = editor.getCaretModel().getOffset();
        int start = editor.getSelectionModel().getSelectionStart();
        if (offset > start)
        {
            BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
            if (!opt.getValue().equals("exclusive"))
            {
                editor.getCaretModel().moveToOffset(offset - 1);
            }
        }
        EditorData.setLastColumn(editor, EditorHelper.getCurrentVisualColumn(editor));
    }

    /**
     * Process mouse clicks by setting/resetting visual mode. There are some strange scenerios to handle.
     * @param editor
     * @param event
     */
    private void processMouseClick(Editor editor, MouseEvent event)
    {
        // FIX - all this works except 3 click/triple click. Because we move the cursor after a two/double click,
        // we can't get the three/triple click so there is no easy way to enter FLAG_MOT_LINEWISE visual mode using the mouse.
        // If we don't move the mouse after the two/double click, then we can get FLAG_MOT_LINEWISE visual mode but then
        // FLAG_MOT_CHARACTERWISE mode is wrong because the cursor is in the wrong place and the first cursor move after
        // results in unexpected highlight behaviour.
        int visualMode = 0;
        switch (event.getClickCount() % 3)
        {
            case 1: // Single click or quad click
                visualMode = 0;
                break;
            case 2: // Double click
                visualMode = Command.FLAG_MOT_CHARACTERWISE;
                break;
            case 0: // Triple click
                visualMode = Command.FLAG_MOT_LINEWISE;
                break;
        }

        setVisualMode(editor, null, visualMode);

        switch (CommandState.getInstance().getVisualType())
        {
            case 0:
                int offset = editor.getCaretModel().getOffset();
                int clean = EditorHelper.normalizeOffset(editor, offset,
                    CommandState.getInstance().getMode() == CommandState.MODE_INSERT ||
                    CommandState.getInstance().getMode() == CommandState.MODE_REPLACE);
                if (offset != clean)
                {
                    editor.getCaretModel().moveToOffset(clean);
                }
                break;
            case Command.FLAG_MOT_CHARACTERWISE:
                BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
                int adj = 1;
                if (opt.getValue().equals("exclusive"))
                {
                    adj = 0;
                }
                editor.getCaretModel().moveToOffset(visualEnd - adj);
                break;
            case Command.FLAG_MOT_LINEWISE:
                editor.getCaretModel().moveToLogicalPosition(editor.xyToLogicalPosition(event.getPoint()));
                break;
        }

        EditorData.setLastColumn(editor, EditorHelper.getCurrentVisualColumn(editor));
        logger.debug("Mouse click: vp=" + editor.getCaretModel().getVisualPosition() +
            "lp=" + editor.getCaretModel().getLogicalPosition() +
            "offset=" + editor.getCaretModel().getOffset());
    }

    public static int moveCaretToMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument)
    {
        Command cmd = argument.getMotion();
        // Normalize the counts between the command and the motion argument
        int cnt = cmd.getCount() * count;
        int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
        MotionEditorAction action = (MotionEditorAction)cmd.getAction();

        // Execute the motion (without moving the cursor) and get where we end
        int offset = action.getOffset(editor, context, cnt, raw, cmd.getArgument());

        moveCaret(editor, context, offset);

        return offset;
    }

    /**
     * This helper method calculates the complete range a motion will move over taking into account whether
     * the motion is FLAG_MOT_LINEWISE or FLAG_MOT_CHARACTERWISE (FLAG_MOT_INCLUSIVE or FLAG_MOT_EXCLUSIVE).
     * @param editor The editor the motion takes place in
     * @param context The data context
     * @param count The count applied to the motion
     * @param rawCount The actual count entered by the user
     * @param argument Any argument needed by the motion
     * @param moveCursor True if cursor should be moved just as if motion command were executed by user, false if not
     * @return The motion's range
     */
    public static TextRange getMotionRange(Editor editor, DataContext context, int count, int rawCount,
        Argument argument, boolean moveCursor)
    {
        Command cmd = argument.getMotion();
        // Normalize the counts between the command and the motion argument
        int cnt = cmd.getCount() * count;
        int raw = rawCount == 0 && cmd.getRawCount() == 0 ? 0 : cnt;
        MotionEditorAction action = (MotionEditorAction)cmd.getAction();

        // This is where we are now
        int start = editor.getCaretModel().getOffset();

        // Execute the motion (without moving the cursor) and get where we end
        int end = action.getOffset(editor, context, cnt, raw, cmd.getArgument());

        // Invalid motion
        if (end == -1)
        {
            return null;
        }

        if (moveCursor)
        {
            moveCaret(editor, context, end);
        }

        // If we are a linewise motion we need to normalize the start and stop then move the start to the beginning
        // of the line and move the end to the end of the line.
        int flags = cmd.getFlags();
        if ((flags & Command.FLAG_MOT_LINEWISE) != 0)
        {
            if (start > end)
            {
                int t = start;
                start = end;
                end = t;
            }

            start = EditorHelper.getLineStartForOffset(editor, start);
            end = Math.min(EditorHelper.getLineEndForOffset(editor, end) + 1, EditorHelper.getFileSize(editor));
        }
        // If characterwise and inclusive, add the last character to the range
        else if ((flags & Command.FLAG_MOT_INCLUSIVE) != 0)
        {
            end = end + (start <= end ? 1 : -1);
        }

        // Normalize the range
        if (start > end)
        {
            int t = start;
            start = end;
            end = t;
        }

        return new TextRange(start, end);
    }

    public int moveCaretToNthCharacter(Editor editor, int count)
    {
        return Math.max(0, Math.min(count, EditorHelper.getFileSize(editor) - 1));
    }

    public int moveCaretToMarkLine(Editor editor, DataContext context, char ch)
    {
        Mark mark = CommandGroups.getInstance().getMark().getMark(editor, ch);
        if (mark != null)
        {
            VirtualFile vf = EditorData.getVirtualFile(editor);
            if (vf == null) return -1;

            int line = mark.getLogicalLine();
            if (!mark.getFile().equals(vf))
            {
                editor = selectEditor(editor, mark.getFile());
                moveCaret(editor, context, moveCaretToLineStartSkipLeading(editor, line));

                return -2;
            }
            else
            {
                //saveJumpLocation(editor, context);
                return moveCaretToLineStartSkipLeading(editor, line);
            }
        }
        else
        {
            return -1;
        }
    }

    public int moveCaretToFileMarkLine(Editor editor, DataContext context, char ch)
    {
        Mark mark = CommandGroups.getInstance().getMark().getFileMark(editor, ch);
        if (mark != null)
        {
            int line = mark.getLogicalLine();
            //saveJumpLocation(editor, context);
            return moveCaretToLineStartSkipLeading(editor, line);
        }
        else
        {
            return -1;
        }
    }

    public int moveCaretToMark(Editor editor, DataContext context, char ch)
    {
        Mark mark = CommandGroups.getInstance().getMark().getMark(editor, ch);
        if (mark != null)
        {
            VirtualFile vf = EditorData.getVirtualFile(editor);
            if (vf == null) return -1;

            LogicalPosition lp = new LogicalPosition(mark.getLogicalLine(), mark.getCol());
            if (!vf.equals(mark.getFile()))
            {
                editor = selectEditor(editor, mark.getFile());
                moveCaret(editor, context, editor.logicalPositionToOffset(lp));

                return -2;
            }
            else
            {
                //saveJumpLocation(editor, context);

                return editor.logicalPositionToOffset(lp);
            }
        }
        else
        {
            return -1;
        }
    }

    public int moveCaretToFileMark(Editor editor, DataContext context, char ch)
    {
        Mark mark = CommandGroups.getInstance().getMark().getFileMark(editor, ch);
        if (mark != null)
        {
            LogicalPosition lp = new LogicalPosition(mark.getLogicalLine(), mark.getCol());
            //saveJumpLocation(editor, context);

            return editor.logicalPositionToOffset(lp);
        }
        else
        {
            return -1;
        }
    }

    private Editor selectEditor(Editor editor, VirtualFile file)
    {
        Project proj = EditorData.getProject(editor);
        FileEditorManager fMgr = FileEditorManager.getInstance(proj);
        return fMgr.openFile(new OpenFileDescriptor(file), ScrollType.RELATIVE, true);
    }

    public int moveCaretToMatchingPair(Editor editor, DataContext context)
    {
        int pos = SearchHelper.findMatchingPairOnCurrentLine(editor);
        if (pos >= 0)
        {
            //saveJumpLocation(editor, context);

            return pos;
        }
        else
        {
            return -1;
        }
    }

    /**
     * This moves the caret to the start of the next/previous word/WORD.
     * @param count The number of words to skip
     * @param skipPunc If true then find WORD, if false then find word
     * @param editor The editor to move in
     */
    public int moveCaretToNextWord(Editor editor, int count, boolean skipPunc)
    {
        if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
            (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0))
        {
            return -1;
        }
        else
        {
            return SearchHelper.findNextWord(editor, count, skipPunc);
        }
    }

    /**
     * This moves the caret to the end of the next/previous word/WORD.
     * @param count The number of words to skip
     * @param skipPunc If true then find WORD, if false then find word
     * @param editor The editor to move in
     */
    public int moveCaretToNextWordEnd(Editor editor, int count, boolean skipPunc)
    {
        if ((editor.getCaretModel().getOffset() == 0 && count < 0) ||
            (editor.getCaretModel().getOffset() >= EditorHelper.getFileSize(editor) - 1 && count > 0))
        {
            return -1;
        }

        int pos = SearchHelper.findNextWordEnd(editor, count, skipPunc);
        if (pos == -1)
        {
            if (count < 0)
            {
                return moveCaretToLineStart(editor, 0);
            }
            else
            {
                return moveCaretToLineEnd(editor, EditorHelper.getLineCount(editor) - 1, false);
            }
        }
        else
        {
            return pos;
        }
    }

    public void setLastFTCmd(int lastFTCmd, char lastChar)
    {
        this.lastFTCmd = lastFTCmd;
        this.lastFTChar = lastChar;
    }

    public int repeatLastMatchChar(Editor editor, int count)
    {
        int res = -1;
        switch (lastFTCmd)
        {
            case LAST_F:
                res = moveCaretToNextCharacterOnLine(editor, -count, lastFTChar);
                break;
            case LAST_f:
                res = moveCaretToNextCharacterOnLine(editor, count, lastFTChar);
                break;
            case LAST_T:
                res = moveCaretToBeforeNextCharacterOnLine(editor, -count, lastFTChar);
                break;
            case LAST_t:
                res = moveCaretToBeforeNextCharacterOnLine(editor, count, lastFTChar);
                break;
        }

        return res;
    }

    /**
     * This moves the caret to the next/previous matching character on the current line
     * @param count The number of occurences to move to
     * @param ch The character to search for
     * @param editor The editor to search in
     * @return True if [count] character matches were found, false if not
     */
    public int moveCaretToNextCharacterOnLine(Editor editor, int count, char ch)
    {
        int pos = SearchHelper.findNextCharacterOnLine(editor, count, ch);

        if (pos >= 0)
        {
            return pos;
        }
        else
        {
            return -1;
        }
    }

    /**
     * This moves the caret next to the next/previous matching character on the current line
     * @param count The number of occurences to move to
     * @param ch The character to search for
     * @param editor The editor to search in
     * @return True if [count] character matches were found, false if not
     */
    public int moveCaretToBeforeNextCharacterOnLine(Editor editor, int count, char ch)
    {
        int pos = SearchHelper.findNextCharacterOnLine(editor, count, ch);

        if (pos >= 0)
        {
            int step = count >= 0 ? 1 : -1;
            return pos - step;
        }
        else
        {
            return -1;
        }
    }

    public boolean scrollLineToFirstScreenLine(Editor editor, DataContext context, int rawCount, int count,
        boolean start)
    {
        scrollLineToScreenLine(editor, context, 1, rawCount, count, start);

        return true;
    }

    public boolean scrollLineToMiddleScreenLine(Editor editor, DataContext context, int rawCount, int count,
        boolean start)
    {
        scrollLineToScreenLine(editor, context, EditorHelper.getScreenHeight(editor) / 2, rawCount, count, start);

        return true;
    }

    public boolean scrollLineToLastScreenLine(Editor editor, DataContext context, int rawCount, int count,
        boolean start)
    {
        scrollLineToScreenLine(editor, context, EditorHelper.getScreenHeight(editor), rawCount, count, start);

        return true;
    }

    private void scrollLineToScreenLine(Editor editor, DataContext context, int sline, int rawCount, int count,
        boolean start)
    {
        int vline = rawCount == 0 ?
            EditorHelper.getCurrentVisualLine(editor) : EditorHelper.logicalLineToVisualLine(editor, count - 1);
        scrollLineToTopOfScreen(editor, EditorHelper.normalizeVisualLine(editor, vline - sline + 1));
        if (vline != EditorHelper.getCurrentVisualLine(editor) || start)
        {
            int offset;
            if (start)
            {
                offset = moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, vline));
            }
            else
            {
                offset = moveCaretVertical(editor,
                    EditorHelper.visualLineToLogicalLine(editor, vline) - EditorHelper.getCurrentLogicalLine(editor));
            }

            moveCaret(editor, context, offset);
        }
    }

    public int moveCaretToFirstScreenLine(Editor editor, DataContext context, int count)
    {
        return moveCaretToScreenLine(editor, context, count);
    }

    public int moveCaretToLastScreenLine(Editor editor, DataContext context, int count)
    {
        return moveCaretToScreenLine(editor, context, EditorHelper.getScreenHeight(editor) - count);
    }

    public int moveCaretToLastScreenLineEnd(Editor editor, DataContext context, int count)
    {
        int offset = moveCaretToScreenLine(editor, context, EditorHelper.getScreenHeight(editor) - count);
        LogicalPosition lline = editor.offsetToLogicalPosition(offset);

        return moveCaretToLineEnd(editor, lline.line, false);
    }

    public int moveCaretToMiddleScreenLine(Editor editor, DataContext context)
    {
        return moveCaretToScreenLine(editor, context, EditorHelper.getScreenHeight(editor) / 2);
    }

    private int moveCaretToScreenLine(Editor editor, DataContext context, int line)
    {
        //saveJumpLocation(editor, context);
        int height = EditorHelper.getScreenHeight(editor);
        if (line > height)
        {
            line = height;
        }
        else if (line < 1)
        {
            line = 1;
        }

        int top = getVisualLineAtTopOfScreen(editor);

        return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, top + line - 1));
    }

    public boolean scrollHalfPageDown(Editor editor, DataContext context, int count)
    {
        NumberOption scroll = (NumberOption)Options.getInstance().getOption("scroll");
        if (count == 0)
        {
            count = scroll.value();
            if (count == 0)
            {
                count = EditorHelper.getScreenHeight(editor) / 2;
            }
        }
        else
        {
            scroll.set(count);
        }

        if (EditorHelper.getCurrentVisualLine(editor) == EditorHelper.getVisualLineCount(editor) - 1)
        {
            return false;
        }
        else
        {
            int tline = getVisualLineAtTopOfScreen(editor);
            moveCaret(editor, context, moveCaretToLineStartSkipLeadingOffset(editor, count));
            scrollLineToTopOfScreen(editor, EditorHelper.normalizeVisualLine(editor, tline + count));

            return true;
        }
    }

    public boolean scrollHalfPageUp(Editor editor, DataContext context, int count)
    {
        NumberOption scroll = (NumberOption)Options.getInstance().getOption("scroll");
        if (count == 0)
        {
            count = scroll.value();
            if (count == 0)
            {
                count = EditorHelper.getScreenHeight(editor) / 2;
            }
        }
        else
        {
            scroll.set(count);
        }

        int tline = getVisualLineAtTopOfScreen(editor);
        if (getVisualLineAtTopOfScreen(editor) == 0)
        {
            return false;
        }
        else
        {
            moveCaret(editor, context, moveCaretToLineStartSkipLeadingOffset(editor, -count));
            scrollLineToTopOfScreen(editor, EditorHelper.normalizeVisualLine(editor, tline - count));

            return true;
        }
    }

    public boolean scrollLine(Editor editor, DataContext context, int lines)
    {
        logger.debug("lines="+lines);
        int vline = getVisualLineAtTopOfScreen(editor);
        int cline = EditorHelper.getCurrentVisualLine(editor);
        vline = EditorHelper.normalizeVisualLine(editor, vline + lines);
        logger.debug("vline=" + vline + ", cline=" + cline);
        scrollLineToTopOfScreen(editor, vline);
        if (cline < vline)
        {
            moveCaret(editor, context, moveCaretVertical(editor, vline - cline));
        }
        else if (cline > vline + EditorHelper.getScreenHeight(editor))
        {
            moveCaret(editor, context, moveCaretVertical(editor, vline + EditorHelper.getScreenHeight(editor) - cline));
        }

        return true;
    }

    public boolean scrollPage(Editor editor, DataContext context, int pages)
    {
        logger.debug("scrollPage(" + pages + ")");
        int tline = getVisualLineAtTopOfScreen(editor);
        int height = EditorHelper.getScreenHeight(editor) - 2;
        if ((tline == 0 && pages < 0) || (tline == EditorHelper.getVisualLineCount(editor) - 1 && pages > 0))
        {
            return false;
        }

        int cline = tline;
        if (pages > 0)
        {
            cline += pages * height;
        }
        else
        {
            cline += ((pages + 1) * height);
        }

        tline = EditorHelper.normalizeVisualLine(editor, tline + pages * height);
        cline = EditorHelper.normalizeVisualLine(editor, cline);

        logger.debug("cline = " + cline + ", height = " + height);

        moveCaret(editor, context, moveCaretToLineStartSkipLeading(editor,
            EditorHelper.visualLineToLogicalLine(editor, cline)));

        scrollLineToTopOfScreen(editor, tline);

        return true;
    }

    private int getVisualLineAtTopOfScreen(Editor editor)
    {
        int vline = editor.getScrollingModel().getVerticalScrollOffset() / editor.getLineHeight();
        logger.debug("top = " + vline);
        return vline;
    }

    private void scrollLineToTopOfScreen(Editor editor, int vline)
    {
        editor.getScrollingModel().scrollVertically(vline * editor.getLineHeight());
    }

    public int moveCaretToMiddleColumn(Editor editor)
    {
        int width = EditorHelper.getScreenWidth(editor) / 2;
        int len = EditorHelper.getLineLength(editor);

        return moveCaretToColumn(editor, Math.max(0, Math.min(len - 1, width)));
    }

    public int moveCaretToColumn(Editor editor, int count)
    {
        int line = EditorHelper.getCurrentLogicalLine(editor);
        int pos = EditorHelper.normalizeColumn(editor, line, count);

        return editor.logicalPositionToOffset(new LogicalPosition(line, pos));
    }

    public int moveCaretToLineStartSkipLeading(Editor editor)
    {
        int lline = EditorHelper.getCurrentLogicalLine(editor);
        return moveCaretToLineStartSkipLeading(editor, lline);
    }

    public int moveCaretToLineStartSkipLeadingOffset(Editor editor, int offset)
    {
        int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
        return moveCaretToLineStartSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, line));
    }

    public int moveCaretToLineStartSkipLeading(Editor editor, int lline)
    {
        int start = EditorHelper.getLineStartOffset(editor, lline);
        int end = EditorHelper.getLineEndOffset(editor, lline, true);
        char[] chars = editor.getDocument().getChars();
        int pos = end;
        for (int offset = start; offset < end; offset++)
        {
            if (offset >= chars.length)
            {
                break;
            }

            if (!Character.isWhitespace(chars[offset]))
            {
                pos = offset;
                break;
            }
        }

        return pos;
    }

    public int moveCaretToLineEndSkipLeadingOffset(Editor editor, int offset)
    {
        int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
        return moveCaretToLineEndSkipLeading(editor, EditorHelper.visualLineToLogicalLine(editor, line));
    }

    public int moveCaretToLineEndSkipLeading(Editor editor, int lline)
    {
        int start = EditorHelper.getLineStartOffset(editor, lline);
        int end = EditorHelper.getLineEndOffset(editor, lline, true);
        char[] chars = editor.getDocument().getChars();
        int pos = start;
        for (int offset = end; offset > start; offset--)
        {
            if (offset >= chars.length)
            {
                break;
            }

            if (!Character.isWhitespace(chars[offset]))
            {
                pos = offset;
                break;
            }
        }

        return pos;
    }

    public int moveCaretToLineEnd(Editor editor, boolean allowPastEnd)
    {
        return moveCaretToLineEnd(editor, EditorHelper.getCurrentLogicalLine(editor), allowPastEnd);
    }

    public int moveCaretToLineEnd(Editor editor, int lline, boolean allowPastEnd)
    {
        int offset = EditorHelper.normalizeOffset(editor, lline, EditorHelper.getLineEndOffset(editor, lline, allowPastEnd),
            allowPastEnd);

        return offset;
    }

    public int moveCaretToLineEndOffset(Editor editor, int cntForward, boolean allowPastEnd)
    {
        int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + cntForward);

        return moveCaretToLineEnd(editor, EditorHelper.visualLineToLogicalLine(editor, line), allowPastEnd);
    }

    public int moveCaretToLineStart(Editor editor)
    {
        int lline = EditorHelper.getCurrentLogicalLine(editor);
        return moveCaretToLineStart(editor, lline);
    }

    public int moveCaretToLineStart(Editor editor, int lline)
    {
        if (lline >= EditorHelper.getLineCount(editor))
        {
            return EditorHelper.getFileSize(editor);
        }

        int start = EditorHelper.getLineStartOffset(editor, lline);
        return start;
    }

    public int moveCaretToLineStartOffset(Editor editor, int offset)
    {
        int line = EditorHelper.normalizeVisualLine(editor, EditorHelper.getCurrentVisualLine(editor) + offset);
        return moveCaretToLineStart(editor, EditorHelper.visualLineToLogicalLine(editor, line));
    }

    public int moveCaretHorizontalWrap(Editor editor, int count)
    {
        // FIX - allows cursor over newlines
        int oldoffset = editor.getCaretModel().getOffset();
        int offset = Math.min(Math.max(0, editor.getCaretModel().getOffset() + count), EditorHelper.getFileSize(editor));
        if (offset == oldoffset)
        {
            return -1;
        }
        else
        {
            return offset;
        }
    }

    public int moveCaretHorizontal(Editor editor, int count, boolean allowPastEnd)
    {
        int oldoffset = editor.getCaretModel().getOffset();
        int offset = EditorHelper.normalizeOffset(editor, EditorHelper.getCurrentLogicalLine(editor), oldoffset + count,
            allowPastEnd);
        if (offset == oldoffset)
        {
            return -1;
        }
        else
        {
            return offset;
        }
    }

    public int moveCaretVertical(Editor editor, int count)
    {
        VisualPosition pos = editor.getCaretModel().getVisualPosition();
        if ((pos.line == 0 && count < 0) || (pos.line >= EditorHelper.getVisualLineCount(editor) - 1 && count > 0))
        {
            return -1;
        }
        else
        {
            int col = EditorData.getLastColumn(editor);
            int line = EditorHelper.normalizeVisualLine(editor, pos.line + count);
            VisualPosition newPos = new VisualPosition(line, EditorHelper.normalizeVisualColumn(editor, line, col));

            return EditorHelper.visualPostionToOffset(editor, newPos);
        }
    }

    public int moveCaretToLine(Editor editor, DataContext context, int lline)
    {
        int col = EditorData.getLastColumn(editor);
        int line = lline;
        if (lline < 0)
        {
            line = 0;
            col = 0;
        }
        else if (lline >= EditorHelper.getLineCount(editor))
        {
            line = EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1);
            col = EditorHelper.getLineLength(editor, line);
        }

        LogicalPosition newPos = new LogicalPosition(line, EditorHelper.normalizeColumn(editor, line, col));

        return editor.logicalPositionToOffset(newPos);
    }

    public int moveCaretToLinePercent(Editor editor, DataContext context, int count)
    {
        if (count > 100) count = 100;

        //saveJumpLocation(editor, context);
        return moveCaretToLineStartSkipLeading(editor, EditorHelper.normalizeLine(
            editor, (EditorHelper.getLineCount(editor) * count + 99) / 100 - 1));
    }

    public int moveCaretGotoLineLast(Editor editor, DataContext context, int rawCount, int lline)
    {
        //saveJumpLocation(editor, context);
        return moveCaretToLineStartSkipLeading(editor, rawCount == 0 ?
            EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) : lline);
    }

    public int moveCaretGotoLineLastEnd(Editor editor, DataContext context, int rawCount, int lline, boolean pastEnd)
    {
        //saveJumpLocation(editor, context);
        return moveCaretToLineEnd(editor, rawCount == 0 ?
            EditorHelper.normalizeLine(editor, EditorHelper.getLineCount(editor) - 1) : lline, pastEnd);
    }

    public int moveCaretGotoLineFirst(Editor editor, DataContext context, int lline)
    {
        //saveJumpLocation(editor, context);
        return moveCaretToLineStartSkipLeading(editor, lline);
    }

    public static void moveCaret(Editor editor, DataContext context, int offset)
    {
        if (offset >= 0 && offset <= editor.getDocument().getTextLength())
        {
            editor.getCaretModel().moveToOffset(offset);
            EditorData.setLastColumn(editor, editor.getCaretModel().getVisualPosition().column);
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);

            if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
            {
                CommandGroups.getInstance().getMotion().updateSelection(editor, context, offset);
            }
            else
            {
                editor.getSelectionModel().removeSelection();
            }
        }
    }

    public boolean selectPreviousVisualMode(Editor editor, DataContext context)
    {
        VisualRange vr = EditorData.getLastVisualRange(editor);
        if (vr == null)
        {
            return false;
        }

        CommandState.getInstance().setMode(CommandState.MODE_VISUAL);
        CommandState.getInstance().setVisualType(vr.getType());
        CommandState.getInstance().setMappingMode(KeyParser.MAPPING_VISUAL);

        visualStart = vr.getStart();
        visualEnd = vr.getEnd();

        updateSelection(editor, context, visualEnd);

        MotionGroup.moveCaret(editor, context, visualEnd);

        return true;
    }

    public boolean swapVisualSelections(Editor editor, DataContext context)
    {
        VisualRange vr = EditorData.getLastVisualRange(editor);
        if (vr == null)
        {
            return false;
        }

        EditorData.setLastVisualRange(editor, new VisualRange(visualStart, visualEnd,
            CommandState.getInstance().getVisualType()));

        visualStart = vr.getStart();
        visualEnd = vr.getEnd();
        CommandState.getInstance().setVisualType(vr.getType());

        updateSelection(editor, context, vr.getEnd());

        MotionGroup.moveCaret(editor, context, visualEnd);

        return true;
    }

    public void setVisualMode(Editor editor, DataContext context, int mode)
    {
        int oldMode = CommandState.getInstance().getVisualType();
        if (mode == 0)
        {
            int start = editor.getSelectionModel().getSelectionStart();
            int end = editor.getSelectionModel().getSelectionEnd();
            if (start != end)
            {
                int line = editor.offsetToLogicalPosition(start).line;
                int lstart = EditorHelper.getLineStartOffset(editor, line);
                int lend = EditorHelper.getLineEndOffset(editor, line, true);
                logger.debug("start=" + start + ", end=" + end + ", lstart=" + lstart + ", lend=" + lend);
                if (lstart == start && lend + 1 == end)
                {
                    mode = Command.FLAG_MOT_LINEWISE;
                }
                else
                {
                    mode = Command.FLAG_MOT_CHARACTERWISE;
                }
            }
        }

        if (oldMode == 0 && mode == 0)
        {
            editor.getSelectionModel().removeSelection();
            return;
        }

        CommandState.getInstance().setVisualType(mode);
        if (mode == 0)
        {
            resetVisual(editor);
        }
        else
        {
            CommandState.getInstance().setMode(CommandState.MODE_VISUAL);
            CommandState.getInstance().setMappingMode(KeyParser.MAPPING_VISUAL);
        }

        KeyHandler.getInstance().reset();

        visualStart = editor.getSelectionModel().getSelectionStart();
        visualEnd = editor.getSelectionModel().getSelectionEnd();

        CommandGroups.getInstance().getMark().setMark(editor, context, '<', visualStart);
        CommandGroups.getInstance().getMark().setMark(editor, context, '>', visualEnd);
    }

    public boolean toggleVisual(Editor editor, DataContext context, int mode)
    {
        // TODO - support count before v and V commands
        // TODO - support pending insert mode
        int currentMode = CommandState.getInstance().getVisualType();
        if (currentMode == 0)
        {
            CommandState.getInstance().setMode(CommandState.MODE_VISUAL);
            CommandState.getInstance().setVisualType(mode);
            CommandState.getInstance().setMappingMode(KeyParser.MAPPING_VISUAL);
            visualStart = editor.getSelectionModel().getSelectionStart();
            updateSelection(editor, context, visualStart);
        }
        else if (mode == currentMode)
        {
            resetVisual(editor);
        }
        else
        {
            CommandState.getInstance().setVisualType(mode);
            updateSelection(editor, context, visualEnd);
        }

        return true;
    }

    public void resetVisual(Editor editor)
    {
        EditorData.setLastVisualRange(editor, new VisualRange(visualStart,
            visualEnd, CommandState.getInstance().getVisualType()));

        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
        {
            CommandState.getInstance().reset();
        }
        editor.getSelectionModel().removeSelection();
    }

    public TextRange getVisualRange(Editor editor)
    {
        return new TextRange(editor.getSelectionModel().getSelectionStart(),
            editor.getSelectionModel().getSelectionEnd());
    }

    private void updateSelection(Editor editor, DataContext context, int offset)
    {
        visualEnd = offset;
        int start = visualStart;
        int end = visualEnd;
        if (start > end)
        {
            int t = start;
            start = end;
            end = t;
        }

        if (CommandState.getInstance().getVisualType() == Command.FLAG_MOT_CHARACTERWISE)
        {
            BoundStringOption opt = (BoundStringOption)Options.getInstance().getOption("selection");
            int adj = 1;
            if (opt.getValue().equals("exclusive"))
            {
                adj = 0;
            }
            end = Math.min(EditorHelper.getFileSize(editor), end + adj);
            editor.getSelectionModel().setSelection(start, end);
        }
        else
        {
            start = EditorHelper.getLineStartForOffset(editor, start);
            end = EditorHelper.getLineEndForOffset(editor, end);
            editor.getSelectionModel().setSelection(start, end);
        }

        CommandGroups.getInstance().getMark().setMark(editor, context, '<', start);
        CommandGroups.getInstance().getMark().setMark(editor, context, '>', end);
    }

    public boolean swapVisualEnds(Editor editor, DataContext context)
    {
        int t = visualEnd;
        visualEnd = visualStart;
        visualStart = t;

        moveCaret(editor, context, visualEnd);

        return true;
    }

    public void processEscape(Editor editor, DataContext context)
    {
        CommandState.getInstance().reset();
        editor.getSelectionModel().removeSelection();
    }

    public static class MotionEditorChange implements FileEditorManagerListener
    {
        public void selectedFileChanged(FileEditorManagerEvent event)
        {
            if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
            {
                CommandState.getInstance().reset();
                EditorHelper.getEditor(event.getManager(), event.getOldFile()).getSelectionModel().removeSelection();
            }
        }
    }

    private static class EditorMouseHandler implements EditorMouseListener, EditorMouseMotionListener
    {
        public void mouseMoved(EditorMouseEvent event)
        {
            // no-op
        }

        public void mouseDragged(EditorMouseEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            if (event.getArea() == EditorMouseEventArea.EDITING_AREA)
            {
                dragEditor = event.getEditor();
            }
        }

        public void mousePressed(EditorMouseEvent event)
        {
            // no-op
        }

        public void mouseClicked(EditorMouseEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            if (event.getArea() == EditorMouseEventArea.EDITING_AREA)
            {
                CommandGroups.getInstance().getMotion().processMouseClick(event.getEditor(), event.getMouseEvent());
                event.consume();
            }
        }

        public void mouseReleased(EditorMouseEvent event)
        {
            if (!VimPlugin.isEnabled()) return;

            if (event.getArea() == EditorMouseEventArea.EDITING_AREA)
            {
                if (event.getEditor().equals(dragEditor))
                {
                    CommandGroups.getInstance().getMotion().processMouseDrag(event.getEditor(), event.getMouseEvent());
                    event.consume();
                }

                dragEditor = null;
            }
        }

        public void mouseEntered(EditorMouseEvent event)
        {
            // no-op
        }

        public void mouseExited(EditorMouseEvent event)
        {
            // no-op
        }

        private Editor dragEditor = null;
    }

    private int lastFTCmd = 0;
    private char lastFTChar;
    private int visualStart;
    private int visualEnd;

    private static Logger logger = Logger.getInstance(MotionGroup.class.getName());
}
