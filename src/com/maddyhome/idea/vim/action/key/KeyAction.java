package com.maddyhome.idea.vim.action.key;

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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.ui.CommandEntryPanel;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 */
public class KeyAction extends AnAction
{
    public void actionPerformed(AnActionEvent event)
    {
        if (!VimPlugin.isEnabled())
        {
            // TODO - popup dialog telling user to switch to other keymapping
            return;
        }

        if (event.getInputEvent() instanceof KeyEvent)
        {
            if (CommandEntryPanel.getInstance().isActive())
            {
                CommandEntryPanel.getInstance().dispatchEvent(event.getInputEvent());
            }
            else
            {
                KeyStroke key = KeyStroke.getKeyStrokeForEvent((KeyEvent)event.getInputEvent());
                Editor editor = (Editor)event.getDataContext().getData(DataConstants.EDITOR);
                if (editor != null)
                {
                    KeyHandler.getInstance().handleKey(editor, key, event.getDataContext());
                }
            }
        }
    }

    private static Logger logger = Logger.getInstance(KeyAction.class.getName());
}
