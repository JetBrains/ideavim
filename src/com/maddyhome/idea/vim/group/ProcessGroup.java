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

import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ui.CommandEntryPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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

    public void startExCommand(Editor editor, DataContext context, Command cmd)
    {
        String initText = getRange(cmd);
        CommandEntryPanel panel = CommandEntryPanel.getInstance();
        panel.addActionListener(new ExEntryListener(editor, context));

        panel.activate(((Editor)context.getData(DataConstants.EDITOR)).getContentComponent(), ":", initText);
    }

    public void startFilterCommand(Editor editor, DataContext context, Command cmd)
    {
        String initText = getRange(cmd) + "!";
        CommandEntryPanel panel = CommandEntryPanel.getInstance();
        panel.addActionListener(new ExEntryListener(editor, context));

        panel.activate(((Editor)context.getData(DataConstants.EDITOR)).getContentComponent(), ":", initText);
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

    public boolean executeFilter(Editor editor, DataContext context, TextRange range, String command)
    {
        logger.debug("command=" + command);
        char[] chars = editor.getDocument().getChars();
        CharArrayReader car = new CharArrayReader(chars, range.getStartOffset(),
            range.getEndOffset() - range.getStartOffset());
        StringWriter sw = new StringWriter();

        try
        {
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
        catch (IOException e)
        {
            // TODO
            return false;
        }
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

    private static class ExEntryListener implements ActionListener
    {
        public ExEntryListener(Editor editor, DataContext context)
        {
            this.editor = editor;
            this.context = context;
        }

        public void actionPerformed(final ActionEvent e)
        {
            CommandEntryPanel.getInstance().removeActionListener(this);

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        ProcessGroup.logger.debug("processing command");
                        CommandEntryPanel.getInstance().deactivate(true);
                        CommandParser.getInstance().processCommand(editor, context, e.getActionCommand(), 1);
                        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
                        {
                            CommandGroups.getInstance().getMotion().exitVisual(editor);
                        }
                    }
                    catch (ExException ex)
                    {
                        // TODO - display error
                        ProcessGroup.logger.info(ex.getMessage());
                        VimPlugin.indicateError();
                    }
                    catch (Exception bad)
                    {
                        ProcessGroup.logger.error(bad);
                        VimPlugin.indicateError();
                    }
                    finally
                    {
                    }
                }
            });
        }

        private Editor editor;
        private DataContext context;
    }

    private String lastCommand;

    private static Logger logger = Logger.getInstance(ProcessGroup.class.getName());
}
