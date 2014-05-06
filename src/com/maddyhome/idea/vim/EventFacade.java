package com.maddyhome.idea.vim;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author vlan
 */
public class EventFacade {
  @NotNull private static final EventFacade ourInstance = new EventFacade();

  @Nullable private TypedActionHandler myOriginalTypedActionHandler;

  private EventFacade() {
  }

  @NotNull
  public static EventFacade getInstance() {
    return ourInstance;
  }

  public void addProjectManagerListener(@NotNull ProjectManagerListener listener) {
    ProjectManager.getInstance().addProjectManagerListener(listener);
  }

  public void setupTypedActionHandler(@NotNull TypedActionHandler handler) {
    final TypedAction typedAction = getTypedAction();
    myOriginalTypedActionHandler = typedAction.getHandler();
    typedAction.setupHandler(handler);
  }

  public void restoreTypedActionHandler() {
    if (myOriginalTypedActionHandler != null) {
      getTypedAction().setupHandler(myOriginalTypedActionHandler);
    }
  }

  public void registerCustomShortcutSet(@NotNull AnAction action, @NotNull ShortcutSet shortcutSet,
                                        @Nullable JComponent component) {
    action.registerCustomShortcutSet(shortcutSet, component);
  }

  public void unregisterCustomShortcutSet(@NotNull AnAction action, @Nullable JComponent component) {
    action.unregisterCustomShortcutSet(component);
  }

  public void addFileEditorManagerListener(@NotNull Project project, @NotNull FileEditorManagerListener listener) {
    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
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

  public void addEditorMouseListener(@NotNull Editor editor, @NotNull EditorMouseListener listener) {
    editor.addEditorMouseListener(listener);
  }

  public void removeEditorMouseListener(@NotNull Editor editor, @NotNull EditorMouseListener listener) {
    editor.removeEditorMouseListener(listener);
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

  @NotNull
  private TypedAction getTypedAction() {
    return EditorActionManager.getInstance().getTypedAction();
  }
}
