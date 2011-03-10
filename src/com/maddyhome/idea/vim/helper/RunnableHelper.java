package com.maddyhome.idea.vim.helper;

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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;

/**
 * This provides some helper methods to run code as a command and an application write action
 */
public class RunnableHelper
{
    public static void runReadCommand(Project project, Runnable cmd, String name, Object groupId)
    {
        if (logger.isDebugEnabled()) logger.debug("read command " + cmd);
        CommandProcessor.getInstance().executeCommand(project, new ReadAction(cmd), name, groupId);
    }

    public static void runWriteCommand(Project project, Runnable cmd, String name, Object groupId)
    {
        if (logger.isDebugEnabled()) logger.debug("write command " + cmd);
        CommandProcessor.getInstance().executeCommand(project, new WriteAction(cmd), name, groupId);
    }

    static class ReadAction implements Runnable
    {
        ReadAction(Runnable cmd)
        {
            this.cmd = cmd;
        }

        public void run()
        {
            ApplicationManager.getApplication().runReadAction(cmd);
        }

        Runnable cmd;
    }

    static class WriteAction implements Runnable
    {
        WriteAction(Runnable cmd)
        {
            this.cmd = cmd;
        }

        public void run()
        {
            ApplicationManager.getApplication().runWriteAction(cmd);
        }

        Runnable cmd;
    }

    private RunnableHelper() {}

    private static Logger logger = Logger.getInstance(KeyHandler.class.getName());
}
