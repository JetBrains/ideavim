package com.maddyhome.idea.vim.undo;

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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.fileEditor.VetoDocumentSavingException;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import java.util.HashMap;
import java.util.HashSet;

/**
 */
public class UndoManager
{
    public static UndoManager getInstance()
    {
        if (instance == null)
        {
            instance = new UndoManager();
        }

        return instance;
    }

    public UndoManager()
    {
        //listener = new DocumentChangeListener();
        editors = new HashMap();
        documents = new HashMap();

        EditorFactory.getInstance().addEditorFactoryListener(new UndoEditorCloseListener());
        FileDocumentManager.getInstance().addFileDocumentManagerListener(new FileDocumentListener());
    }

    public boolean inCommand(Editor editor)
    {
        EditorUndoList list = getEditorUndoList(editor);

        return list.inCommand();
    }

    public void beginCommand(Editor editor)
    {
        EditorUndoList list = getEditorUndoList(editor);
        list.beginCommand(editor);
    }

    public void abortCommand(Editor editor)
    {
        EditorUndoList list = getEditorUndoList(editor);
        list.abortCommand();
    }

    public void endCommand(Editor editor)
    {
        EditorUndoList list = getEditorUndoList(editor);
        list.endCommand();
        logger.debug("endCommand: list=" + list);
    }

    public boolean undo(Editor editor, DataContext context)
    {
        EditorUndoList list = getEditorUndoList(editor);
        boolean res = list.undo(editor, context);
        logger.debug("undo: list=" + list);
        return res;
    }

    public boolean redo(Editor editor, DataContext context)
    {
        EditorUndoList list = getEditorUndoList(editor);
        boolean res = list.redo(editor, context);
        logger.debug("redo: list=" + list);
        return res;
    }

    public void editorOpened(Editor editor)
    {
        logger.info("editorOpened");
        if (!editor.isViewer())
        {
            addEditorUndoList(editor);
        }
    }

    public void editorClosed(Editor editor)
    {
        logger.info("editorClosed");
        if (!editor.isViewer())
        {
            removeEditorUndoList(editor);
        }
    }

    public HashMap getEditors()
    {
        return editors;
    }

    private EditorUndoList addEditorUndoList(Editor editor)
    {
        EditorUndoList res = null;
        HashSet tors = (HashSet)documents.get(editor.getDocument());
        if (tors == null)
        {
            tors = new HashSet();
            documents.put(editor.getDocument(), tors);

            res = new EditorUndoList(editor);
            editors.put(editor.getDocument(), res);
        }
        else
        {
            res = (EditorUndoList)editors.get(editor.getDocument());
        }

        tors.add(editor);

        return res;
    }

    private void removeEditorUndoList(Editor editor)
    {
        HashSet tors = (HashSet)documents.get(editor.getDocument());
        if (tors != null)
        {
            tors.remove(editor);

            if (tors.isEmpty())
            {
                documents.remove(editor.getDocument());
                editors.remove(editor.getDocument());
            }
        }
    }

    private EditorUndoList getEditorUndoList(Editor editor)
    {
        EditorUndoList res = (EditorUndoList)editors.get(editor.getDocument());
        if (res == null)
        {
            res = addEditorUndoList(editor);
        }

        return res;
    }

    public static class DocumentChangeListener extends DocumentAdapter
    {
        public void documentChanged(DocumentEvent event)
        {
            EditorUndoList list = (EditorUndoList)UndoManager.getInstance().getEditors().get(event.getDocument());
            if (list != null)
            {
                list.addChange(new DocumentChange(event.getOffset(), event.getOldFragment(), event.getNewFragment()));
            }
        }
    }

    public static class UndoEditorCloseListener extends EditorFactoryAdapter
    {
        public void editorReleased(EditorFactoryEvent event)
        {
            UndoManager.getInstance().removeEditorUndoList(event.getEditor());
        }
    }

    private class FileDocumentListener extends FileDocumentManagerAdapter
    {
        public void beforeDocumentSaving(Document document) throws VetoDocumentSavingException
        {
            EditorUndoList list = (EditorUndoList)UndoManager.getInstance().getEditors().get(document);
            if (list != null)
            {
                list.documentSaved();
            }
        }
    }

    //private DocumentListener listener;
    private HashMap editors;
    private HashMap documents;

    private static UndoManager instance;
    private static Logger logger = Logger.getInstance(UndoManager.class.getName());
}
