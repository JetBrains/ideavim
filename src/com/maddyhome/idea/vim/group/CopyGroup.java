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
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.EditorHelper;

/**
 * This group works with command associated with copying and pasting text
 */
public class CopyGroup extends AbstractActionGroup
{
    /**
     * Creates the group
     */
    public CopyGroup()
    {
    }

    /**
     * This yanks the text moved over by the motion command argument.
     * @param editor The editor to yank from
     * @param context The data context
     * @param count The number of times to yank
     * @param rawCount The actual count entered by the user
     * @param argument The motion command argument
     * @return true if able to yank the text, false if not
     */
    public boolean yankMotion(Editor editor, DataContext context, int count, int rawCount, Argument argument)
    {
        TextRange range = MotionGroup.getMotionRange(editor, context, count, rawCount, argument, true);

        return yankRange(editor, context, range, argument.getMotion().getFlags());
    }

    /**
     * This yanks count lines of text
     * @param editor The editor to yank from
     * @param context The data context
     * @param count The number of lines to yank
     * @return true if able to yank the lines, false if not
     */
    public boolean yankLine(Editor editor, DataContext context, int count)
    {
        int start = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
        int offset = Math.min(CommandGroups.getInstance().getMotion().moveCaretToLineEndOffset(
            editor, count - 1, true) + 1, EditorHelper.getFileSize(editor));
        if (offset != -1)
        {
            return yankRange(editor, context, new TextRange(start, offset), Command.FLAG_MOT_LINEWISE);
        }

        return false;
    }

    /**
     * This yanks a range of text
     * @param editor The editor to yank from
     * @param context The data context
     * @param range The range of text to yank
     * @param type The type of yank - characterwise or linewise
     * @return true if able to yank the range, false if not
     */
    public boolean yankRange(Editor editor, DataContext context, TextRange range, int type)
    {
        if (range != null)
        {
            logger.debug("yanking range");
            return CommandGroups.getInstance().getRegister().storeText(editor, context, range.getStartOffset(),
                range.getEndOffset(), type, false, true);
        }

        return false;
    }

    /**
     * Pastes text from the last register into the editor before the current cursor location.
     * @param editor The editor to paste into
     * @param context The data context
     * @param count The number of times to perform the paste
     * @return true if able to paste, false if not
     */
    public boolean putTextBeforeCursor(Editor editor, DataContext context, int count)
    {
        // What register are we getting the text from?
        Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
        if (reg != null)
        {
            int pos = 0;
            // If a linewise put the text is inserted before the current line.
            if ((reg.getType() & Command.FLAG_MOT_LINEWISE) != 0)
            {
                pos = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor);
            }
            else
            {
                pos = editor.getCaretModel().getOffset();
            }

            putText(editor, context, pos, reg.getText(), reg.getType(), count);

            return true;
        }

        return false;
    }

    /**
     * Pastes text from the last register into the editor after the current cursor location.
     * @param editor The editor to paste into
     * @param context The data context
     * @param count The number of times to perform the paste
     * @return true if able to paste, false if not
     */
    public boolean putTextAfterCursor(Editor editor, DataContext context, int count)
    {
        Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
        if (reg != null)
        {
            int pos = 0;
            // If a linewise paste, the text is inserted after the current line.
            if ((reg.getType() & Command.FLAG_MOT_LINEWISE) != 0)
            {
                pos = CommandGroups.getInstance().getMotion().moveCaretToLineStartOffset(editor, 1);
            }
            else
            {
                pos = editor.getCaretModel().getOffset() + 1;
            }

            putText(editor, context, pos, reg.getText(), reg.getType(), count);

            return true;
        }

        return false;
    }

    public boolean putVisualRange(Editor editor, DataContext context, TextRange range, int count)
    {
        Register reg = CommandGroups.getInstance().getRegister().getLastRegister();
        if (reg != null)
        {
            int start = editor.getSelectionModel().getSelectionStart();
            int end = editor.getSelectionModel().getSelectionEnd();
            int pos = 0;
            // If a linewise paste, the text is inserted after the current line.
            if ((reg.getType() & Command.FLAG_MOT_LINEWISE) != 0)
            {
                MotionGroup.moveCaret(editor, context, end);
                pos = CommandGroups.getInstance().getMotion().moveCaretToLineStartOffset(editor, 1);
            }
            else
            {
                pos = end;
            }

            putText(editor, context, pos, reg.getText(), reg.getType(), count);

            MotionGroup.moveCaret(editor, context, start);

            CommandGroups.getInstance().getChange().deleteRange(editor, context, range,
                CommandState.getInstance().getVisualType());

            return true;
        }

        return false;
    }

    /**
     * This performs the actual insert of the paste
     * @param editor The editor to paste into
     * @param context The data context
     * @param offset The location within the file to paste the text
     * @param text The text to paste
     * @param type The type of paste (linewise or characterwise)
     * @param count The number of times to paste the text
     */
    public void putText(Editor editor, DataContext context, int offset, String text, int type, int count)
    {
        // TODO - What about auto imports?
        for (int i = 0; i < count; i++)
        {
            CommandGroups.getInstance().getChange().insertText(editor, context, offset, text);
        }

        LogicalPosition slp = editor.offsetToLogicalPosition(offset);
        int adjust = 0;
        if ((type & Command.FLAG_MOT_LINEWISE) != 0)
        {
            adjust = -1;
        }
        LogicalPosition elp = editor.offsetToLogicalPosition(offset + count * text.length() + adjust);
        for (int i = slp.line; i <= elp.line; i++)
        {
            MotionGroup.moveCaret(editor, context, editor.logicalPositionToOffset(new LogicalPosition(i, 0)));
            KeyHandler.executeAction("AutoIndentLines", context);
        }

        // Adjust the cursor position after the paste
        if ((type & Command.FLAG_MOT_LINEWISE) != 0)
        {
            MotionGroup.moveCaret(editor, context, offset);
            MotionGroup.moveCaret(editor, context,
                CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor));
        }
        else
        {
            MotionGroup.moveCaret(editor, context, offset + count * text.length() - 1);
        }
    }

    private static Logger logger = Logger.getInstance(CopyGroup.class.getName());
}
