package com.maddyhome.idea.vim.ex;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.handler.CopyTextHandler;
import com.maddyhome.idea.vim.ex.handler.DeleteLinesHandler;
import com.maddyhome.idea.vim.ex.handler.EditFileHandler;
import com.maddyhome.idea.vim.ex.handler.FindFileHandler;
import com.maddyhome.idea.vim.ex.handler.GotoCharacterHandler;
import com.maddyhome.idea.vim.ex.handler.HelpHandler;
import com.maddyhome.idea.vim.ex.handler.JoinLinesHandler;
import com.maddyhome.idea.vim.ex.handler.MarkHandler;
import com.maddyhome.idea.vim.ex.handler.MoveTextHandler;
import com.maddyhome.idea.vim.ex.handler.NextFileHandler;
import com.maddyhome.idea.vim.ex.handler.PreviousFileHandler;
import com.maddyhome.idea.vim.ex.handler.PutLinesHandler;
import com.maddyhome.idea.vim.ex.handler.QuitHandler;
import com.maddyhome.idea.vim.ex.handler.SelectFileHandler;
import com.maddyhome.idea.vim.ex.handler.SelectFirstFileHandler;
import com.maddyhome.idea.vim.ex.handler.SelectLastFileHandler;
import com.maddyhome.idea.vim.ex.handler.ShiftLeftHandler;
import com.maddyhome.idea.vim.ex.handler.ShiftRightHandler;
import com.maddyhome.idea.vim.ex.handler.SubstituteHandler;
import com.maddyhome.idea.vim.ex.handler.WriteHandler;
import com.maddyhome.idea.vim.ex.handler.WriteQuitHandler;
import com.maddyhome.idea.vim.ex.handler.YankLinesHandler;
import com.maddyhome.idea.vim.ex.range.AbstractRange;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.MotionGroup;

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


/**
 *
 */
public class CommandParser
{
    public synchronized static CommandParser getInstance()
    {
        if (ourInstance == null)
        {
            ourInstance = new CommandParser();
        }
        return ourInstance;
    }

    private CommandParser()
    {
    }

    public void registerHandlers()
    {
        new CopyTextHandler();
        new DeleteLinesHandler();
        new EditFileHandler();
        new FindFileHandler();
        new GotoCharacterHandler();
        new HelpHandler();
        new JoinLinesHandler();
        new MarkHandler();
        new MoveTextHandler();
        new NextFileHandler();
        new PreviousFileHandler();
        new PutLinesHandler();
        new QuitHandler();
        new SelectFileHandler();
        new SelectFirstFileHandler();
        new SelectLastFileHandler();
        new ShiftLeftHandler();
        new ShiftRightHandler();
        new SubstituteHandler();
        new WriteHandler();
        new WriteQuitHandler();
        new YankLinesHandler();
    }

    public void processCommand(Editor editor, DataContext context, String cmd) throws ExException
    {
        if (cmd.length() == 0)
        {
            return;
        }

        ParseResult res = parse(cmd);
        String command = res.getCommand();

        if (command.length() == 0)
        {
            MotionGroup.moveCaret(editor, context,
                CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor,
                res.getRanges().getLine(editor, context)));

            return;
        }

        CommandNode node = root;
        for (int i = 0; i < command.length(); i++)
        {
            node = node.getChild(command.charAt(i));
            if (node == null)
            {
                throw new InvalidCommandException(cmd);
            }
        }

        CommandHandler handler = node.getCommandHandler();
        if (handler == null)
        {
            throw new InvalidCommandException(cmd);
        }

        handler.process(editor, context, new ExCommand(res.getRanges(), command, res.getArgument()));
    }

    public ParseResult parse(String cmd) throws ExException
    {
        logger.debug("processing `" + cmd + "'");
        int state = STATE_START;
        Ranges ranges = new Ranges();
        StringBuffer command = new StringBuffer();
        StringBuffer argument = new StringBuffer();
        StringBuffer location = null;
        int offsetSign = 1;
        int offsetNumber = 0;
        int offsetTotal = 0;
        boolean move = false;
        for (int i = 0; i <= cmd.length(); i++)
        {
            boolean reprocess = true;
            char ch = (i == cmd.length() ? '\n' : cmd.charAt(i));
            while (reprocess)
            {
                switch (state)
                {
                    case STATE_START:
                        if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0)
                        {
                            state = STATE_COMMAND;
                        }
                        else
                        {
                            state = STATE_RANGE;
                        }
                        break;
                    case STATE_COMMAND:
                        if (Character.isLetter(ch) ||
                            (command.length() == 0 && "~<>@=#*&!".indexOf(ch) >= 0) ||
                            (command.length() > 0 && ch == command.charAt(command.length() - 1) && "!@<>".indexOf(ch) >= 0))
                        {
                            command.append(ch);
                            reprocess = false;
                        }
                        else
                        {
                            state = STATE_CMD_ARG;
                        }
                        break;
                    case STATE_CMD_ARG:
                        argument.append(ch);
                        reprocess = false;
                        break;
                    case STATE_RANGE:
                        location = new StringBuffer();
                        offsetTotal = 0;
                        offsetNumber = 0;
                        move = false;
                        if (ch >= '0' && ch <= '9')
                        {
                            state = STATE_RANGE_LINE;
                        }
                        else if (ch == '.')
                        {
                            state = STATE_RANGE_CURRENT;
                        }
                        else if (ch == '$')
                        {
                            state = STATE_RANGE_LAST;
                        }
                        else if (ch == '%')
                        {
                            state = STATE_RANGE_ALL;
                        }
                        else if (ch == '\'')
                        {
                            state = STATE_RANGE_MARK;
                        }
                        else if (ch == '+' || ch == '-')
                        {
                            state = STATE_RANGE_OFFSET;
                        }
                        else
                        {
                            state = STATE_ERROR;
                            reprocess = false;
                        }
                        break;
                    case STATE_RANGE_LINE:
                        if (ch >= '0' && ch <= '9')
                        {
                            location.append(ch);
                            state = STATE_RANGE_MAYBE_DONE;
                            reprocess = false;
                        }
                        else
                        {
                            state = STATE_RANGE_MAYBE_DONE;
                        }
                        break;
                    case STATE_RANGE_CURRENT:
                        location.append(ch);
                        state = STATE_RANGE_MAYBE_DONE;
                        reprocess = false;
                        break;
                    case STATE_RANGE_LAST:
                        location.append(ch);
                        state = STATE_RANGE_MAYBE_DONE;
                        reprocess = false;
                        break;
                    case STATE_RANGE_ALL:
                        location.append(ch);
                        state = STATE_RANGE_MAYBE_DONE;
                        reprocess = false;
                        break;
                    case STATE_RANGE_MARK:
                        location.append(ch);
                        state = STATE_RANGE_MARK_CHAR;
                        reprocess = false;
                        break;
                    case STATE_RANGE_MARK_CHAR:
                        location.append(ch);
                        state = STATE_RANGE_MAYBE_DONE;
                        reprocess = false;
                        break;
                    case STATE_RANGE_DONE:
                        Range[] range = AbstractRange.createRange(location.toString(), offsetTotal, move);
                        ranges.addRange(range);
                        if (ch == ':' || ch == '\n')
                        {
                            state = STATE_COMMAND;
                            reprocess = false;
                        }
                        else if (Character.isLetter(ch) || "~<>@=#*&!".indexOf(ch) >= 0)
                        {
                            state = STATE_COMMAND;
                        }
                        else
                        {
                            state = STATE_RANGE;
                        }
                        break;
                    case STATE_RANGE_MAYBE_DONE:
                        if (ch == '+' || ch == '-')
                        {
                            state = STATE_RANGE_OFFSET;
                        }
                        else if (ch == ',' || ch == ';')
                        {
                            state = STATE_RANGE_SEPARATOR;
                        }
                        else if (ch >= '0' && ch <= '9')
                        {
                            state = STATE_RANGE_LINE;
                        }
                        else
                        {
                            state = STATE_RANGE_DONE;
                        }
                        break;
                    case STATE_RANGE_OFFSET:
                        offsetNumber = 0;
                        if (ch == '+')
                        {
                            offsetSign = 1;
                        }
                        else if (ch == '-')
                        {
                            offsetSign = -1;
                        }
                        state = STATE_RANGE_OFFSET_MAYBE_DONE;
                        reprocess = false;
                        break;
                    case STATE_RANGE_OFFSET_MAYBE_DONE:
                        if (ch >= '0' && ch <= '9')
                        {
                            state = STATE_RANGE_OFFSET_NUM;
                        }
                        else
                        {
                            state = STATE_RANGE_OFFSET_DONE;
                        }
                        break;
                    case STATE_RANGE_OFFSET_DONE:
                        if (offsetNumber == 0)
                        {
                            offsetNumber = 1;
                        }
                        offsetTotal += offsetNumber * offsetSign;

                        if (ch == '+' || ch == '-')
                        {
                            state = STATE_RANGE_OFFSET;
                        }
                        else
                        {
                            state = STATE_RANGE_MAYBE_DONE;
                        }
                        break;
                    case STATE_RANGE_OFFSET_NUM:
                        if (ch >= '0' && ch <= '9')
                        {
                            offsetNumber = offsetNumber * 10 + (ch - '0');
                            state = STATE_RANGE_OFFSET_MAYBE_DONE;
                            reprocess = false;
                        }
                        else if (ch == '+' || ch == '-')
                        {
                            state = STATE_RANGE_OFFSET_DONE;
                        }
                        else
                        {
                            state = STATE_RANGE_OFFSET_MAYBE_DONE;
                        }
                        break;
                    case STATE_RANGE_SEPARATOR:
                        if (ch == ',')
                        {
                            move = false;
                        }
                        else if (ch == ';')
                        {
                            move = true;
                        }
                        state = STATE_RANGE_DONE;
                        reprocess = false;
                        break;
                }
            }

            if (state == STATE_ERROR)
            {
                throw new InvalidCommandException(cmd);
            }
        }

        logger.debug("ranges = " + ranges);
        logger.debug("command = " + command);
        logger.debug("argument = " + argument);

        return new ParseResult(ranges, command.toString(), argument.toString().trim());
    }

    public void addHandler(CommandHandler handler)
    {
        CommandName[] names = handler.getNames();
        for (int c = 0; c < names.length; c++)
        {
            CommandNode node = root;
            String text = names[c].getRequired();
            for (int i = 0; i < text.length() - 1; i++)
            {
                CommandNode cn = node.getChild(text.charAt(i));
                if (cn == null)
                {
                    cn = node.addChild(text.charAt(i), null);
                }

                node = cn;
            }

            CommandNode cn = node.getChild(text.charAt(text.length() - 1));
            if (cn == null)
            {
                cn = node.addChild(text.charAt(text.length() - 1), handler);
            }
            else
            {
                cn.setCommandHandler(handler);
            }
            node = cn;

            text = names[c].getOptional();
            for (int i = 0; i < text.length(); i++)
            {
                cn = node.getChild(text.charAt(i));
                if (cn == null)
                {
                    cn = node.addChild(text.charAt(i), handler);
                }

                node = cn;
            }
        }
    }

    private CommandNode root = new CommandNode();

    private static CommandParser ourInstance;

    private static final int STATE_START = 1;
    private static final int STATE_COMMAND = 10;
    private static final int STATE_CMD_ARG = 11;
    private static final int STATE_RANGE = 20;
    private static final int STATE_RANGE_LINE = 21;
    private static final int STATE_RANGE_CURRENT = 22;
    private static final int STATE_RANGE_LAST = 23;
    private static final int STATE_RANGE_MARK = 24;
    private static final int STATE_RANGE_MARK_CHAR = 25;
    private static final int STATE_RANGE_ALL = 26;
    private static final int STATE_RANGE_OFFSET = 30;
    private static final int STATE_RANGE_OFFSET_NUM = 31;
    private static final int STATE_RANGE_OFFSET_DONE = 32;
    private static final int STATE_RANGE_OFFSET_MAYBE_DONE = 33;
    private static final int STATE_RANGE_SEPARATOR = 40;
    private static final int STATE_RANGE_MAYBE_DONE = 50;
    private static final int STATE_RANGE_DONE = 51;
    private static final int STATE_ERROR = 99;

    private static Logger logger = Logger.getInstance(CommandParser.class.getName());
}

