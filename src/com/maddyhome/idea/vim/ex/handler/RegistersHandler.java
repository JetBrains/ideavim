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
package com.maddyhome.idea.vim.ex.handler;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.CommandName;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.ui.MorePanel;
import java.util.Iterator;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 *
 */
public class RegistersHandler extends CommandHandler
{
    public RegistersHandler()
    {
        super(new CommandName[] {
            new CommandName("di", "splay"),
            new CommandName("reg", "isters")
        }, ARGUMENT_OPTIONAL | KEEP_FOCUS);
    }

    public boolean execute(final Editor editor, DataContext context, ExCommand cmd) throws ExException
    {
        List registers = CommandGroups.getInstance().getRegister().getRegisters();

        StringBuffer text = new StringBuffer();
        text.append("--- Registers ---\n");
        for (Iterator iterator = registers.iterator(); iterator.hasNext();)
        {
            Register reg = (Register)iterator.next();
            text.append("\"");
            text.append(reg.getName());

            text.append("   ");
            text.append(StringHelper.escape(reg.getText()).trim());
            text.append("\n");
        }

        MorePanel panel = MorePanel.getInstance(editor);
        panel.setText(text.toString());

        //panel.setVisible(true);

        return true;
    }
}
