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
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 */
public class ProcessGroup extends AbstractActionGroup
{
    public ProcessGroup()
    {
    }

    public String getLastCommand()
    {
        return lastCommand;
    }

    public void startSearchCommand(Editor editor, DataContext context, Command cmd)
    {
        String initText = "";
        int flags = cmd.getFlags();
        String label = "";
        if ((flags & Command.FLAG_SEARCH_FWD) != 0)
        {
            label = "/";
        }
        else if ((flags & Command.FLAG_SEARCH_REV) != 0)
        {
            label = "?";
        }

        CommandState.getInstance().pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
        ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.activate(editor, context, label, initText, cmd.getCount());
    }

    public void startExCommand(Editor editor, DataContext context, Command cmd)
    {
        String initText = getRange(cmd);
        CommandState.getInstance().pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
        ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.activate(editor, context, ":", initText, 1);
    }

    public boolean processExKey(Editor editor, DataContext context, KeyStroke stroke, boolean charOnly)
    {
        if (!charOnly || stroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
        {
            ExEntryPanel panel = ExEntryPanel.getInstance();
            panel.handleKey(stroke);

            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean processExEntry(final Editor editor, final DataContext context)
    {
        final ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.deactivate(true);
        boolean res = true;
        try
        {
            CommandState.getInstance().popState();
            logger.debug("processing command");
            final String text = panel.getText();
            logger.debug("swing=" + SwingUtilities.isEventDispatchThread());
            if (panel.getLabel().equals(":"))
            {
                CommandParser.getInstance().processCommand(editor, context, text, 1);
                if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
                {
                    CommandGroups.getInstance().getMotion().exitVisual(editor);
                }
            }
            else
            {
                int pos = CommandGroups.getInstance().getSearch().search(editor, context, text, panel.getCount(),
                    panel.getLabel().equals("/") ? Command.FLAG_SEARCH_FWD : Command.FLAG_SEARCH_REV, true);
                if (pos == -1)
                {
                    res = false;
                }
            }
        }
        catch (ExException ex)
        {
            //VimPlugin.showMessage(ex.getMessage());
            ProcessGroup.logger.info(ex.getMessage());
            VimPlugin.indicateError();
            res = false;
        }
        catch (Exception bad)
        {
            ProcessGroup.logger.error(bad);
            VimPlugin.indicateError();
            res = false;
        }
        finally
        {
            /*
            SwingUtilities.invokeLater(new Runnable() {
                public void run()
                {
                    editor.getContentComponent().requestFocus();
                }
            });
            */
            return res;
        }
    }

    public boolean cancelExEntry(final Editor editor, DataContext context)
    {
        CommandState.getInstance().popState();
        ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.clear();
        panel.deactivate(true);
        /*
        SwingUtilities.invokeLater(new Runnable() {
            public void run()
            {
                editor.getContentComponent().requestFocus();
            }
        });
        */

        return true;
    }

    public void startFilterCommand(Editor editor, DataContext context, Command cmd)
    {
        String initText = getRange(cmd) + "!";
        CommandState.getInstance().pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
        ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.activate(editor, context, ":", initText, 1);
    }

    private String getRange(Command cmd)
    {
        String initText = "";
        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
        {
            initText = "'<,'>";
        }
        else if (cmd.getRawCount() > 0)
        {
            if (cmd.getCount() == 1)
            {
                initText = ".";
            }
            else
            {
                initText = ".,.+" + (cmd.getCount() - 1);
            }
        }

        return initText;
    }

    public boolean executeFilter(Editor editor, DataContext context, TextRange range, String command) throws IOException
    {
        logger.debug("command=" + command);
        char[] chars = editor.getDocument().getChars();
        CharArrayReader car = new CharArrayReader(chars, range.getStartOffset(),
            range.getEndOffset() - range.getStartOffset());
        StringWriter sw = new StringWriter();

        logger.debug("about to create filter");
        Process filter = Runtime.getRuntime().exec(command);
        logger.debug("filter created");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(filter.getOutputStream()));
        logger.debug("sending text");
        copy(car, writer);
        writer.close();
        logger.debug("sent");

        BufferedReader reader = new BufferedReader(new InputStreamReader(filter.getInputStream()));
        logger.debug("getting result");
        copy(reader, sw);
        sw.close();
        logger.debug("received");

        editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), sw.toString());

        lastCommand = command;

        return true;
    }

    private void copy(Reader from, Writer to) throws IOException
    {
        char[] buf = new char[2048];
        int cnt;
        while ((cnt = from.read(buf)) != -1)
        {
            logger.debug("buf="+buf);
            to.write(buf, 0, cnt);
        }
    }

    private String lastCommand;

    private static Logger logger = Logger.getInstance(ProcessGroup.class.getName());
}
