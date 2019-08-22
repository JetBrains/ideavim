/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.TemplateManagerListener;
import com.intellij.find.FindManager;
import com.intellij.find.FindModelListener;
import com.intellij.ide.bookmarks.BookmarksListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vlan
 */
public class EventFacade {
  @NotNull private static final EventFacade ourInstance = new EventFacade();

  @Nullable private TypedActionHandler myOriginalTypedActionHandler;
  private Map<Project, MessageBusConnection> connections = new HashMap<>();

  private EventFacade() {
  }

  @NotNull
  public static EventFacade getInstance() {
    return ourInstance;
  }

  public void setupTypedActionHandler(@NotNull VimTypedActionHandler handler) {
    final TypedAction typedAction = getTypedAction();
    myOriginalTypedActionHandler = typedAction.getRawHandler();

    typedAction.setupRawHandler(handler);
  }

  public void restoreTypedActionHandler() {
    if (myOriginalTypedActionHandler != null) {
      getTypedAction().setupRawHandler(myOriginalTypedActionHandler);
    }
  }

  public void registerCustomShortcutSet(@NotNull AnAction action, @NotNull ShortcutSet shortcutSet,
                                        @Nullable JComponent component) {
    action.registerCustomShortcutSet(shortcutSet, component);
  }

  public void registerCustomShortcutSet(@NotNull AnAction action, @NotNull ShortcutSet shortcutSet,
                                        @Nullable JComponent component, @NotNull Disposable disposable) {
    action.registerCustomShortcutSet(shortcutSet, component, disposable);
  }

  public void unregisterCustomShortcutSet(@NotNull AnAction action, @Nullable JComponent component) {
    action.unregisterCustomShortcutSet(component);
  }

  public void connectFileEditorManagerListener(@NotNull Project project, @NotNull FileEditorManagerListener listener) {
    final MessageBusConnection connection = getConnection(project);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
  }

  public void connectAnActionListener(@NotNull Project project, @NotNull AnActionListener listener) {
    final MessageBusConnection connection = getConnection(project);
    connection.subscribe(AnActionListener.TOPIC, listener);
  }

  public void connectTemplateStartedListener(@NotNull Project project, @NotNull TemplateManagerListener listener) {
    final MessageBusConnection connection = getConnection(project);
    connection.subscribe(TemplateManager.TEMPLATE_STARTED_TOPIC, listener);
  }

  public void connectBookmarkListener(@NotNull Project project, @NotNull BookmarksListener bookmarksListener) {
    final MessageBusConnection connection = getConnection(project);
    connection.subscribe(BookmarksListener.TOPIC, bookmarksListener);
  }

  public void connectFindModelListener(@NotNull Project project, @NotNull FindModelListener findModelListener) {
    final MessageBusConnection connection = getConnection(project);
    connection.subscribe(FindManager.FIND_MODEL_TOPIC, findModelListener);
  }

  public void addDocumentListener(@NotNull Document document, @NotNull DocumentListener listener) {
    document.addDocumentListener(listener);
  }

  public void removeDocumentListener(@NotNull Document document, @NotNull DocumentListener listener) {
    document.removeDocumentListener(listener);
  }

  public void addEditorFactoryListener(@NotNull EditorFactoryListener listener, @NotNull Disposable parentDisposable) {
    EditorFactory.getInstance().addEditorFactoryListener(listener, parentDisposable);
  }

  public void removeEditorFactoryListener(@NotNull EditorFactoryListener listener) {
    // Listener is removed not only if application is disposed
    //noinspection deprecation
    EditorFactory.getInstance().removeEditorFactoryListener(listener);
  }

  public void addEditorMouseListener(@NotNull Editor editor, @NotNull EditorMouseListener listener) {
    editor.addEditorMouseListener(listener);
  }

  public void removeEditorMouseListener(@NotNull Editor editor, @NotNull EditorMouseListener listener) {
    editor.removeEditorMouseListener(listener);
  }

  public void addComponentMouseListener(@NotNull Component component, @NotNull MouseListener mouseListener) {
    component.addMouseListener(mouseListener);
  }

  public void removeComponentMouseListener(@NotNull Component component, @NotNull MouseListener mouseListener) {
    component.removeMouseListener(mouseListener);
  }

  public void addEditorMouseMotionListener(@NotNull Editor editor, @NotNull EditorMouseMotionListener listener) {
    editor.addEditorMouseMotionListener(listener);
  }

  public void removeEditorMouseMotionListener(@NotNull Editor editor, @NotNull EditorMouseMotionListener listener) {
    editor.removeEditorMouseMotionListener(listener);
  }

  public void addEditorSelectionListener(@NotNull Editor editor, @NotNull SelectionListener listener) {
    editor.getSelectionModel().addSelectionListener(listener);
  }

  public void removeEditorSelectionListener(@NotNull Editor editor, @NotNull SelectionListener listener) {
    editor.getSelectionModel().removeSelectionListener(listener);
  }

  public void registerLookupListener(@NotNull Project project, @NotNull PropertyChangeListener propertyChangeListener) {
    LookupManager.getInstance(project).addPropertyChangeListener(propertyChangeListener, project);
  }

  public void removeLookupListener(@NotNull Project project, @NotNull PropertyChangeListener propertyChangeListener) {
    LookupManager.getInstance(project).removePropertyChangeListener(propertyChangeListener);
  }

  public void disableBusConnection() {
    connections.values().forEach(MessageBusConnection::disconnect);
    connections.clear();
  }

  private MessageBusConnection getConnection(Project project) {
    if (!connections.containsKey(project)) {
      final MessageBusConnection connection = project.getMessageBus().connect();
      connections.put(project, connection);
      Disposer.register(project, () -> {
        connection.disconnect();
        connections.remove(project);
      });
    }
    return connections.get(project);
  }

  @NotNull
  private TypedAction getTypedAction() {
    return EditorActionManager.getInstance().getTypedAction();
  }
}
