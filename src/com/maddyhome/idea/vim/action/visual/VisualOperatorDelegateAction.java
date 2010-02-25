package com.maddyhome.idea.vim.action.visual;

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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.action.AbstractDelegateEditorAction;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.handler.DelegateActionHandler;
import com.maddyhome.idea.vim.handler.VisualOperatorActionHandler;
import com.intellij.openapi.actionSystem.DataContext;

public class VisualOperatorDelegateAction extends AbstractDelegateEditorAction
{
    public VisualOperatorDelegateAction()
    {
        super(new Handler());
    }

    public void setOrigAction(AnAction origAction)
    {
        super.setOrigAction(origAction);
        ((Handler)getHandler()).setOrigAction(origAction);
    }

    private static class Handler extends VisualOperatorActionHandler implements DelegateActionHandler
    {
        protected boolean execute(Editor editor, DataContext context, Command cmd, TextRange range)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("execute, cmd=" + cmd + ", range=" + range);
                logger.debug("origAction=" + origAction);
            }
            KeyHandler.executeAction(origAction, context);

            return true;
        }

        public void setOrigAction(AnAction origAction)
        {
            if (logger.isDebugEnabled()) logger.debug("setOrigHander to " + origAction);
            this.origAction = origAction;
        }

        public AnAction getOrigAction()
        {
            return origAction;
        }

        private AnAction origAction;

        private static Logger logger = Logger.getInstance(Handler.class.getName());
    }
}