/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class DocumentManager {
  @NotNull
  public static DocumentManager getInstance() {
    return instance;
  }

  public void init() {
    logger.debug("opening project");
  }

  public void addDocumentListener(final DocumentListener listener) {
    docListeners.add(listener);
  }

  public void reloadDocument(@NotNull Document doc, Project p) {
    logger.debug("marking as up-to-date");
    VirtualFile vf = FileDocumentManager.getInstance().getFile(doc);
    if (logger.isDebugEnabled()) logger.debug("file=" + vf);
    if (vf != null) {
      removeListeners(doc);
      FileDocumentManager.getInstance().reloadFromDisk(doc);
      FileStatusManager.getInstance(p).fileStatusChanged(vf);
      addListeners(doc);
    }
  }

  public void addListeners(@NotNull Document doc) {
    Object marker = doc.getUserData(LISTENER_MARKER);
    if (marker != null) {
      return;
    }

    doc.putUserData(LISTENER_MARKER, "foo");
    for (DocumentListener docListener : docListeners) {
      doc.addDocumentListener(docListener);
    }
  }

  public void removeListeners(@NotNull Document doc) {
    Object marker = doc.getUserData(LISTENER_MARKER);
    if (marker == null) {
      return;
    }

    doc.putUserData(LISTENER_MARKER, null);
    for (DocumentListener docListener : docListeners) {
      doc.removeDocumentListener(docListener);
    }
  }


  @NotNull private HashSet<DocumentListener> docListeners = new HashSet<DocumentListener>();

  private static final Key<String> LISTENER_MARKER = new Key<String>("listenerMarker");
  @NotNull private static DocumentManager instance = new DocumentManager();
  private static Logger logger = Logger.getInstance(DocumentManager.class.getName());
}