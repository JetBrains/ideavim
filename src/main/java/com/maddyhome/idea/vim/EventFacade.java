/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ShortcutSet;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.util.Disposer;
import com.maddyhome.idea.vim.helper.HandlerInjector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;

/**
 * @author vlan
 */
public class EventFacade {
  private static final @NotNull
  EventFacade ourInstance = new EventFacade();

  private @Nullable
  TypedActionHandler myOriginalTypedActionHandler;

  private EventFacade() {
  }

  public static @NotNull
  EventFacade getInstance() {
    return ourInstance;
  }

  public void setupTypedActionHandler(@NotNull VimTypedActionHandler handler) {
    final TypedAction typedAction = getTypedAction();

    if (HandlerInjector.notebookCommandMode(null)) {
      TypedActionHandler result = HandlerInjector.inject();
      if (result != null) {
        myOriginalTypedActionHandler = result;
        return;
      }
    }

    myOriginalTypedActionHandler = typedAction.getRawHandler();

    typedAction.setupRawHandler(handler);
  }

  public void restoreTypedActionHandler() {
    if (myOriginalTypedActionHandler != null) {
      getTypedAction().setupRawHandler(myOriginalTypedActionHandler);
    }
  }

  public void registerCustomShortcutSet(@NotNull AnAction action,
                                        @NotNull ShortcutSet shortcutSet,
                                        @Nullable JComponent component) {
    action.registerCustomShortcutSet(shortcutSet, component);
  }

  public void registerCustomShortcutSet(@NotNull AnAction action,
                                        @NotNull ShortcutSet shortcutSet,
                                        @Nullable JComponent component,
                                        @NotNull Disposable disposable) {
    action.registerCustomShortcutSet(shortcutSet, component, disposable);
  }

  public void unregisterCustomShortcutSet(@NotNull AnAction action, @NotNull JComponent component) {
    action.unregisterCustomShortcutSet(component);
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

  public void addCaretListener(@NotNull Editor editor,
                               @NotNull CaretListener listener,
                               @NotNull Disposable disposable) {
    editor.getCaretModel().addCaretListener(listener, disposable);
  }

  public void removeCaretListener(@NotNull Editor editor, @NotNull CaretListener listener) {
    editor.getCaretModel().removeCaretListener(listener);
  }

  public void addEditorMouseListener(@NotNull Editor editor,
                                     @NotNull EditorMouseListener listener,
                                     @NotNull Disposable disposable) {
    editor.addEditorMouseListener(listener, disposable);
  }

  public void removeEditorMouseListener(@NotNull Editor editor, @NotNull EditorMouseListener listener) {
    editor.removeEditorMouseListener(listener);
  }

  public void addComponentMouseListener(@NotNull Component component,
                                        @NotNull MouseListener mouseListener,
                                        @NotNull Disposable disposable) {
    component.addMouseListener(mouseListener);
    Disposer.register(disposable, () -> component.removeMouseListener(mouseListener));
  }

  public void removeComponentMouseListener(@NotNull Component component, @NotNull MouseListener mouseListener) {
    component.removeMouseListener(mouseListener);
  }

  public void addEditorMouseMotionListener(@NotNull Editor editor,
                                           @NotNull EditorMouseMotionListener listener,
                                           @NotNull Disposable disposable) {
    editor.addEditorMouseMotionListener(listener, disposable);
  }

  public void removeEditorMouseMotionListener(@NotNull Editor editor, @NotNull EditorMouseMotionListener listener) {
    editor.removeEditorMouseMotionListener(listener);
  }

  public void addEditorSelectionListener(@NotNull Editor editor,
                                         @NotNull SelectionListener listener,
                                         @NotNull Disposable disposable) {
    editor.getSelectionModel().addSelectionListener(listener, disposable);
  }

  public void removeEditorSelectionListener(@NotNull Editor editor, @NotNull SelectionListener listener) {
    editor.getSelectionModel().removeSelectionListener(listener);
  }

  private @NotNull
  TypedAction getTypedAction() {
    return TypedAction.getInstance();
  }
}
