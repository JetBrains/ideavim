package com.maddyhome.idea.vim.handler.search;

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
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.handler.AbstractEditorActionHandler;
import com.maddyhome.idea.vim.ui.CommandEntryPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;

/**
 *
 */
public class SearchEntryHandler extends AbstractEditorActionHandler
{
    protected boolean execute(Editor editor, DataContext context, Command cmd)
    {
        CommandEntryPanel panel = CommandEntryPanel.getInstance();

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

        CommandEntryPanel.getInstance().addActionListener(new ExEntryListener(editor, context, cmd.getCount(), flags));

        panel.activate(((Editor)context.getData(DataConstants.EDITOR)).getContentComponent(), label, initText);

        return true;
    }

    private static class ExEntryListener implements ActionListener
    {
        public ExEntryListener(Editor editor, DataContext context, int count, int flags)
        {
            this.editor = editor;
            this.context = context;
            this.count = count;
            this.flags = flags;
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
                        logger.debug("processing search");
                        CommandEntryPanel.getInstance().deactivate(true);
                        CommandGroups.getInstance().getSearch().search(editor, context, e.getActionCommand(), count, flags, true);
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

        private int count;
        private int flags;
        private Editor editor;
        private DataContext context;
    }

    private static Logger logger = Logger.getInstance(SearchEntryHandler.class.getName());
}
