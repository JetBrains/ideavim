package com.maddyhome.idea.vim.handler;

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
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.ui.CommandEntryPanel;
import com.maddyhome.idea.vim.VimPlugin;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

/**
 *
 */
public class ExEntryHandler extends AbstractEditorActionHandler
{
    protected boolean execute(Editor editor, DataContext context, Command cmd)
    {
        CommandEntryPanel panel = CommandEntryPanel.getInstance();

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

        listener.setState(editor, context);

        panel.activate(((Editor)context.getData(DataConstants.EDITOR)).getContentComponent(), ":", initText);

        return true;
    }

    static class ExEntryListener implements ActionListener
    {
        public void actionPerformed(final ActionEvent e)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        logger.debug("processing command");
                        CommandEntryPanel.getInstance().deactivate(true);
                        CommandParser.getInstance().processCommand(editor, context, e.getActionCommand(), 1);
                        if (CommandState.getInstance().getMode() == CommandState.MODE_VISUAL)
                        {
                            CommandGroups.getInstance().getMotion().resetVisual(editor);
                        }
                    }
                    catch (ExException ex)
                    {
                        // TODO - display error
                        logger.info(ex.getMessage());
                        VimPlugin.indicateError();
                    }
                    catch (Exception bad)
                    {
                        logger.error(bad);
                        VimPlugin.indicateError();
                    }
                    finally
                    {
                    }
                }
            });
        }

        public void setState(Editor editor, DataContext context)
        {
            this.editor = editor;
            this.context = context;
        }

        private Editor editor;
        private DataContext context;
    }

    private static ExEntryListener listener = new ExEntryListener();

    static {
        CommandEntryPanel.getInstance().addActionListener(listener);
    }

    private static Logger logger = Logger.getInstance(ExEntryHandler.class.getName());
}
