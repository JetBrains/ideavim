package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.helper.EditorData;

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
public class FileGroup extends AbstractActionGroup
{
    public FileGroup()
    {
    }

    /**
     * Close the current editor
     * @param context The data context
     */
    public void closeFile(Editor editor, DataContext context)
    {
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        FileEditorManager fem = FileEditorManager.getInstance(proj);
        //fem.closeFile(fem.getSelectedFile());
        fem.closeFile(EditorData.getVirtualFile(fem.getSelectedTextEditor()));

        /*
        if (fem.getOpenFiles().length == 0)
        {
            exitIdea();
        }
        */
    }

    /**
     * Close all editors except for the current editor
     * @param context The data context
     */
    public void closeAllButCurrent(DataContext context)
    {
        KeyHandler.executeAction("CloseAllEditorsButCurrent", context);
    }

    /**
     * Close all editors
     * @param context The data context
     */
    public void closeAllFiles(DataContext context)
    {
        KeyHandler.executeAction("CloseAllEditors", context);
    }

    /**
     * Saves all files in the project
     * @param context The data context
     */
    public void saveFiles(DataContext context)
    {
        // TODO - this needs to be fixed
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        proj.saveAllDocuments();
        proj.save();
    }

    public void closeProject(DataContext context)
    {
        KeyHandler.executeAction("CloseProject", context);
    }

    public void exitIdea()
    {
        ApplicationManager.getApplication().exit();
    }

    /**
     * Selects then next or previous editor
     * @param count
     * @param context
     */
    public boolean selectFile(int count, DataContext context)
    {
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        FileEditorManager fem = FileEditorManager.getInstance(proj);
        VirtualFile[] editors = fem.getOpenFiles();
        if (count == 99)
        {
            count = editors.length - 1;
        }
        if (count < 0 || count >= editors.length)
        {
            return false;
        }

        //fem.openFile(new OpenFileDescriptor(editors[count]), ScrollType.RELATIVE, true);
        fem.openTextEditor(new OpenFileDescriptor(editors[count]), true);

        return true;
    }

    /**
     * Selects then next or previous editor
     * @param count
     * @param context
     */
    public void selectNextFile(int count, DataContext context)
    {
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        FileEditorManager fem = FileEditorManager.getInstance(proj);
        VirtualFile[] editors = fem.getOpenFiles();
        VirtualFile current = fem.getSelectedFiles()[0];
        for (int i = 0; i < editors.length; i++)
        {
            if (editors[i].equals(current))
            {
                int pos = (i + (count % editors.length) + editors.length) % editors.length;

                //fem.openFile(new OpenFileDescriptor(editors[pos]), ScrollType.RELATIVE, true);
                fem.openTextEditor(new OpenFileDescriptor(editors[pos]), true);
            }
        }
    }
}
