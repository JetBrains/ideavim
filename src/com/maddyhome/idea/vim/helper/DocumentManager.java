package com.maddyhome.idea.vim.helper;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashSet;

public class DocumentManager {
  public static DocumentManager getInstance() {
    return instance;
  }

  public void init() {
    logger.debug("opening project");
    //FileDocumentManager.getInstance().addFileDocumentManagerListener(listener);
  }

  /*
  public void closeProject(Project project)
  {
      logger.debug("closing project");

      // This bit of code is here because FileEditorManager.fileClosed is not getting called
      // for each open file when a project is closed. See IDEA bug 29727.
      VirtualFile[] files = FileEditorManager.getInstance(project).getOpenFiles();
      logger.debug("there are " + files.length + " open files");
      for (int i = 0; i < files.length; i++)
      {
          removeListeners(FileDocumentManager.getInstance().getDocument(files[i]));
      }

      FileDocumentManager.getInstance().removeFileDocumentManagerListener(listener);
  }
  */

  public void addDocumentListener(DocumentListener listener) {
    docListeners.add(listener);
  }

  public void reloadDocument(Document doc, Project p) {
    logger.debug("marking as up-to-date");
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (logger.isDebugEnabled()) logger.debug("file=" + vf);
    if (vf != null) {
      removeListeners(doc);
      FileDocumentManager.getInstance().reloadFromDisk(doc);
      //AbstractVcsHelper.getInstance(p).markFileAsUpToDate(vf);
      FileStatusManager.getInstance(p).fileStatusChanged(vf);
      addListeners(doc);
    }
  }

  public void addListeners(Document doc) {
    Object marker = doc.getUserData(LISTENER_MARKER);
    if (marker != null) {
      return;
    }

    doc.putUserData(LISTENER_MARKER, "foo");
    for (DocumentListener docListener : docListeners) {
      //try
      //{
      doc.addDocumentListener(docListener);
      //}
      /*
      catch (AssertionError e)
      {
          // Ignore - I have no way to avoid adding a listenter twice.
      }
      catch (Throwable e)
      {
          // Ignore - I have no way to avoid adding a listenter twice.
      }
      */
    }
  }

  public void removeListeners(Document doc) {
    Object marker = doc.getUserData(LISTENER_MARKER);
    if (marker == null) {
      return;
    }

    doc.putUserData(LISTENER_MARKER, null);
    for (DocumentListener docListener : docListeners) {
      //try
      //{
      doc.removeDocumentListener(docListener);
      //}
      /*
      catch (AssertionError e)
      {
          // Ignore - I have no way to avoid removing a listenter twice.
      }
      catch (Throwable e)
      {
          // Ignore - I have no way to avoid removing a listenter twice.
      }
      */
    }
  }

  private DocumentManager() {
  }

  private class FileDocumentListener extends FileDocumentManagerAdapter {
    /* This doesn't seem to get called at all
    public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile)
    {
        logger.debug("opened vf=" + virtualFile.getPresentableName());
        Document doc = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (doc != null)
        {
            addListeners(doc);
        }
    }

    public void fileClosed(FileEditorManager fileEditorManager, VirtualFile virtualFile)
    {
        logger.debug("closed vf=" + virtualFile.getName());
        Document doc = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (doc != null)
        {
            removeListeners(doc);
        }
    }
    */

    public void fileContentLoaded(VirtualFile file, Document document) {
      if (logger.isDebugEnabled()) logger.debug("loaded vf=" + file.getName());
      //addListeners(document);
    }
  }

  //private FileDocumentListener listener = new FileDocumentListener();
  private HashSet<DocumentListener> docListeners = new HashSet<DocumentListener>();

  private static final Key<String> LISTENER_MARKER = new Key<String>("listenerMarker");
  private static DocumentManager instance = new DocumentManager();
  private static Logger logger = Logger.getInstance(DocumentManager.class.getName());
}