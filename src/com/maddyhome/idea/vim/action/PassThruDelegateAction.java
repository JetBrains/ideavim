package com.maddyhome.idea.vim.action;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.intellij.openapi.actionSystem.DataContext;

import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class PassThruDelegateAction extends AbstractDelegateAction
{
    public PassThruDelegateAction(KeyStroke stroke)
    {
        this.stroke = stroke;
    }

    public void actionPerformed(AnActionEvent event)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("actionPerformed key=" + stroke);
        }
        final Editor editor = event.getData(PlatformDataKeys.EDITOR); // API change - don't merge
        if (editor == null || !VimPlugin.isEnabled())
        {
            getOrigAction().actionPerformed(event);
        }
        else if (event.getInputEvent() instanceof KeyEvent)
        {
            KeyStroke key = KeyStroke.getKeyStrokeForEvent((KeyEvent)event.getInputEvent());
            if (logger.isDebugEnabled())
            {
                logger.debug("event = KeyEvent: " + key);
            }
            KeyHandler.getInstance().handleKey(editor, key, event.getDataContext());
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("event is a " + event.getInputEvent().getClass().getName());
            }
            KeyHandler.getInstance().handleKey(editor, stroke, event.getDataContext());
        }
    }

    private KeyStroke stroke;

    private static Logger logger = Logger.getInstance(PassThruDelegateAction.class.getName());
}
