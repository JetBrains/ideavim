package com.maddyhome.idea.vim.handler.motion;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2004 Rick Maddy
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
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.common.TextRange;

/**
 */
public abstract class TextObjectActionHandler extends AbstractEditorActionHandler
{
    protected final boolean execute(Editor editor, DataContext context, Command cmd)
    {
        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
        {
            TextRange range = getRange(editor, context, cmd.getCount(), cmd.getRawCount(), cmd.getArgument());
            TextRange vr = CommandGroups.getInstance().getMotion().getRawVisualRange();

            int newstart = vr.getEndOffset() >= vr.getStartOffset() ? range.getStartOffset() : range.getEndOffset();
            int newend = vr.getEndOffset() >= vr.getStartOffset() ? range.getEndOffset() : range.getStartOffset();

            if (vr.getStartOffset() == vr.getEndOffset())
            {
                CommandGroups.getInstance().getMotion().moveVisualStart(editor, newstart);
            }

            if ((cmd.getFlags() & Command.FLAG_MOT_LINEWISE) != 0 &&
                CommandState.getInstance().getSubMode() != Command.FLAG_MOT_LINEWISE)
            {
                CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 0, Command.FLAG_MOT_LINEWISE);
            }
            else if ((cmd.getFlags() & Command.FLAG_MOT_CHARACTERWISE) != 0 &&
                CommandState.getInstance().getSubMode() != Command.FLAG_MOT_CHARACTERWISE)
            {
                CommandGroups.getInstance().getMotion().toggleVisual(editor, context, 1, 0, Command.FLAG_MOT_CHARACTERWISE);
            }

            MotionGroup.moveCaret(editor, context, newend);
        }

        return true;
    }

    public abstract TextRange getRange(Editor editor, DataContext context, int count, int rawCount, Argument argument);
}
