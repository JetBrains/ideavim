package com.maddyhome.idea.vim.helper;

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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.command.VisualRange;
import com.maddyhome.idea.vim.group.MarkGroup;
import com.maddyhome.idea.vim.undo.UndoManager;

/**
 * This class is used to manipulate editor specific data. Each editor has a user defined map associated with it.
 * These methods provide convenient methods for working with that Vim Plugin specific data.
 */
public class EditorData
{
    /**
     * This is used to initialize each new editor that gets created.
     * @param editor The editor to initialize
     */
    public static void initializeEditor(Editor editor)
    {
        // Add a document listener so we can update the position of the editor specific marks each time the
        // contents change
        if (!editor.isViewer())
        {
            editor.getDocument().addDocumentListener(new MarkGroup.MarkUpdater(editor));
        }
        UndoManager.getInstance().editorOpened(editor);
    }

    /**
     * This is used to clean up editors whenever they are closed.
     * @param editor The editor to cleanup
     */
    public static void uninitializeEditor(Editor editor)
    {
        UndoManager.getInstance().editorClosed(editor);
    }

    /**
     * This gets the last column the cursor was in for the editor.
     * @param editor The editr to get the last column from
     * @return Returns the last column as set by {@link #setLastColumn} or the current cursor column
     */
    public static int getLastColumn(Editor editor)
    {
        Integer col = (Integer)editor.getUserData(LAST_COLUMN);
        if (col == null)
        {
            return EditorHelper.getCurrentVisualColumn(editor);
        }
        else
        {
            return col.intValue();
        }
    }

    /**
     * Sets the last column for this editor
     * @param col The column
     * @param editor The editor
     */
    public static void setLastColumn(Editor editor, int col)
    {
        editor.putUserData(LAST_COLUMN, new Integer(col));
        int t = getLastColumn(editor);
        logger.debug("setLastColumn(" + col + ") is now " + t);
    }

    /**
     * Gets the previous visual range for the editor.
     * @param editor The editor to get the range for
     * @return The last visual range, null if no previous range
     */
    public static VisualRange getLastVisualRange(Editor editor)
    {
        VisualRange res = (VisualRange)editor.getUserData(VISUAL);
        return res;
    }

    /**
     * Sets the previous visual range for the editor.
     * @param editor The editor to set the range for
     * @param range The visual range
     */
    public static void setLastVisualRange(Editor editor, VisualRange range)
    {
        editor.putUserData(VISUAL, range);
    }

    /**
     * Gets the previous visual operator range for the editor.
     * @param editor The editor to get the range for
     * @return The last visual range, null if no previous range
     */
    public static VisualChange getLastVisualOperatorRange(Editor editor)
    {
        VisualChange res = (VisualChange)editor.getUserData(VISUAL_OP);
        return res;
    }

    /**
     * Sets the previous visual operator range for the editor.
     * @param editor The editor to set the range for
     * @param range The visual range
     */
    public static void setLastVisualOperatorRange(Editor editor, VisualChange range)
    {
        editor.putUserData(VISUAL_OP, range);
    }

    /**
     * Gets the project associated with the editor.
     * @param editor The editor to get the project for
     * @return The editor's project
     */
    public static Project getProject(Editor editor)
    {
        Project proj = (Project)editor.getUserData(PROJECT);
        if (proj == null)
        {
            // If we don't have the project already we need to scan all open projects and check all their
            // open editors until there is a match
            Project[] projs = ProjectManager.getInstance().getOpenProjects();
            for (int p = 0; p < projs.length; p++)
            {
                FileEditorManager fMgr = FileEditorManager.getInstance(projs[p]);
                /*
                VirtualFile[] files = fMgr.getOpenFiles();
                for (int i = 0; i < files.length; i++)
                {
                    FileEditor[] editors = fMgr.getEditors(files[i]);
                    */
                    FileEditor[] editors = fMgr.getAllEditors();
                    for (int e = 0; e < editors.length; e++)
                    {
                        if (editors[e] instanceof TextEditor && ((TextEditor)editors[e]).getEditor().equals(editor))
                        {
                            proj = projs[p];
                            editor.putUserData(PROJECT, proj);
                            break;
                        }
                    }
                /*
                }
                */
            }
        }

        return proj;
    }

    /**
     * Gets the virtual file associated with this editor
     * @param editor The editor
     * @return The virtual file for the editor
     */
    public static VirtualFile getVirtualFile(Editor editor)
    {
        /*
        Key key = new Key(FILE);
        VirtualFile file = (VirtualFile)editor.getUserData(key);
        if (file == null)
        {
            Project[] projs = ProjectManager.getInstance().getOpenProjects();
            for (int p = 0; p < projs.length; p++)
            {
                FileEditorManager fMgr = FileEditorManager.getInstance(projs[p]);
                VirtualFile[] files = fMgr.getOpenFiles();
                for (int i = 0; i < files.length; i++)
                {
                    FileEditor[] editors = fMgr.getEditors(files[i]);
                    for (int e = 0; e < editors.length; e++)
                    {
                        if (editors[0] instanceof TextEditor && ((TextEditor)editors[0]).getEditor().equals(editor))
                        {
                            file = files[i];
                            editor.putUserData(key, file);
                            break;
                        }
                    }
                }
            }
        }

        return file;
        */

        return FileDocumentManager.getInstance().getFile(editor.getDocument());
    }

    /**
     * This is a static helper - no instances needed
     */
    private EditorData() {}

    private static final Key LAST_COLUMN = new Key("lastColumn");
    private static final Key PROJECT = new Key("project");
    private static final Key VISUAL = new Key("lastVisual");
    private static final Key VISUAL_OP = new Key("lastVisualOp");

    private static Logger logger = Logger.getInstance(EditorData.class.getName());
}
