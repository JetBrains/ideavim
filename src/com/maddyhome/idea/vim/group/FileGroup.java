package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.EditorData;
import java.util.HashMap;

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

    public boolean openFile(String filename, DataContext context)
    {
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        ProjectRootManager prm = ProjectRootManager.getInstance(proj);
        VirtualFile[] roots = prm.getContentRoots();
        for (int i = 0; i < roots.length; i++)
        {
            logger.debug("root[" + i + "] = " + roots[i].getPath());
            VirtualFile vf = findFile(roots[i], filename);
            if (vf != null)
            {
                FileEditorManager fem = FileEditorManager.getInstance(proj);
                fem.openTextEditor(new OpenFileDescriptor(vf), true);

                return true;
            }
        }

        VimPlugin.showMessage("Unable to find " + filename);

        return false;
    }

    private VirtualFile findFile(VirtualFile root, String filename)
    {
        VirtualFile res = root.findFileByRelativePath(filename);
        if (res != null)
        {
            return res;
        }

        VirtualFile[] children = root.getChildren();
        for (int i = 0; i < children.length; i++)
        {
            if (children[i].isDirectory())
            {
                res = findFile(children[i], filename);
                if (res != null)
                {
                    return res;
                }
            }
            else if (children[i].getName().equals(filename))
            {
                return children[i];
            }
        }

        return null;
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
     * Saves specific file in the project
     * @param context The data context
     */
    public void saveFile(Editor editor, DataContext context)
    {
        FileDocumentManager.getInstance().saveDocument(editor.getDocument());
    }

    /**
     * Saves all files in the project
     * @param context The data context
     */
    public void saveFiles(DataContext context)
    {
        FileDocumentManager.getInstance().saveAllDocuments();
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

    /**
     * Selects previous editor tab
     */
    public void selectPreviousTab(DataContext context)
    {
        Project proj = (Project)context.getData(DataConstants.PROJECT);
        FileEditorManager fem = FileEditorManager.getInstance(proj);
        VirtualFile vf = (VirtualFile)lastSelections.get(fem);
        if (vf != null)
        {
            fem.openTextEditor(new OpenFileDescriptor(vf), true);
        }
        else
        {
            VimPlugin.indicateError();
        }
    }

    /**
     * This class listens for editor tab changes so any insert/replace modes that need to be reset can be
     */
    public static class SelectionCheck extends FileEditorManagerAdapter
    {
        /**
         * The user has changed the editor they are working with - exit insert/replace mode, and complete any
         * appropriate repeat.
         * @param event
         */
        public void selectionChanged(FileEditorManagerEvent event)
        {
            lastSelections.put(event.getManager(), event.getOldFile());
        }
    }

    private static HashMap lastSelections = new HashMap();

    private static Logger logger = Logger.getInstance(FileGroup.class.getName());
}
