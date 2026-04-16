/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.find.EditorSearchSession;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.client.ClientAppSession;
import com.intellij.openapi.client.ClientKind;
import com.intellij.openapi.client.ClientSessionsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.helper.CaretVisualAttributesHelperKt;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.newapi.IjVimDocument;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.editor.EditorSettings.LineNumerationType;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.api.VimInjectorKt.options;
import static com.maddyhome.idea.vim.newapi.IjVimInjectorKt.ijOptions;

/**
 * @author vlan
 */
@State(name = "VimEditorSettings", storages = {@Storage(value = "vim_settings.xml")})
public class EditorGroup implements PersistentStateComponent<Element>, VimEditorGroup {
  public static final @NonNls String EDITOR_STORE_ELEMENT = "editor";
  private final CaretListener myLineNumbersCaretListener = new CaretListener() {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      // We don't get notified when the IDE's settings change, so make sure we're up-to-date when the caret moves
      final Editor editor = e.getEditor();
      boolean relativenumber = ijOptions(injector, new IjVimEditor(editor)).getRelativenumber();
      if (relativenumber) {
        if (!hasRelativeLineNumbersInstalled(editor)) {
          installRelativeLineNumbers(editor);
        }
        else {
          // We must repaint on each caret move, so we update when caret's visual line doesn't match logical line
          repaintRelativeLineNumbers(editor);
        }
      }
      else {
        if (hasRelativeLineNumbersInstalled(editor)) {
          removeRelativeLineNumbers(editor);
        }
      }
    }
  };

  // TODO: Get rid of this custom line converter once we support soft wraps properly
  // The builtin relative line converter looks like it's using Vim's logical lines for counting, where a Vim logical
  // line is a buffer line, or a single line representing a fold of several buffer lines. This converter is counting
  // screen lines (but badly - if you're on the second line of a wrapped line, it still counts like you're on the first.
  // We really want to use Vim logical lines, but we don't currently support them for movement - we move by screen line.
  private Boolean isKeyRepeat = null;

  private static boolean supportsVimLineNumbers(final @NotNull Editor editor) {
    // We only support line numbers in editors that are file based, and that aren't for diffs, which control their
    // own line numbers, often using EditorGutter#setLineNumberConverter
    return EditorHelper.isFileEditor(editor) && !EditorHelper.isDiffEditor(editor);
  }

  private static boolean isProjectDisposed(final @NotNull Editor editor) {
    return editor.getProject() == null || editor.getProject().isDisposed();
  }

  private static void updateLineNumbers(final @NotNull Editor editor) {
    final boolean isLineNumbersShown = editor.getSettings().isLineNumbersShown();
    if (!isLineNumbersShown) {
      return;
    }

    final LineNumerationType lineNumerationType = editor.getSettings().getLineNumerationType();
    if (lineNumerationType == LineNumerationType.RELATIVE || lineNumerationType == LineNumerationType.HYBRID) {
      if (!hasRelativeLineNumbersInstalled(editor)) {
        installRelativeLineNumbers(editor);
      }
    }
    else {
      removeRelativeLineNumbers(editor);
    }
  }

  private static boolean hasRelativeLineNumbersInstalled(final @NotNull Editor editor) {
    return UserDataManager.getVimHasRelativeLineNumbersInstalled(editor);
  }

  private static void installRelativeLineNumbers(final @NotNull Editor editor) {
    if (!hasRelativeLineNumbersInstalled(editor)) {
      final EditorGutter gutter = editor.getGutter();
      gutter.setLineNumberConverter(new RelativeLineNumberConverter());
      UserDataManager.setVimHasRelativeLineNumbersInstalled(editor, true);
    }
  }

  private static void removeRelativeLineNumbers(final @NotNull Editor editor) {
    if (hasRelativeLineNumbersInstalled(editor)) {
      final EditorGutter gutter = editor.getGutter();
      gutter.setLineNumberConverter(LineNumberConverter.DEFAULT);
      UserDataManager.setVimHasRelativeLineNumbersInstalled(editor, false);
    }
  }

  private static void repaintRelativeLineNumbers(final @NotNull Editor editor) {
    final EditorGutter gutter = editor.getGutter();
    final EditorGutterComponentEx gutterComponent =
      gutter instanceof EditorGutterComponentEx ? (EditorGutterComponentEx)gutter : null;
    if (gutterComponent != null) {
      gutterComponent.repaint();
    }
  }

  private void initLineNumbers(final @NotNull Editor editor) {
    if (!supportsVimLineNumbers(editor) || UserDataManager.getVimEditorGroup(editor)) {
      return;
    }

    editor.getCaretModel().addCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, true);

    updateLineNumbers(editor);
  }

  private void deinitLineNumbers(@NotNull Editor editor) {
    if (isProjectDisposed(editor) || !supportsVimLineNumbers(editor) || !UserDataManager.getVimEditorGroup(editor)) {
      return;
    }

    editor.getCaretModel().removeCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, false);

    removeRelativeLineNumbers(editor);
  }

  public void saveData(@NotNull Element element) {
    final Element editor = new Element("editor");
    element.addContent(editor);

    if (isKeyRepeat != null) {
      final Element keyRepeat = new Element("key-repeat");
      keyRepeat.setAttribute("enabled", Boolean.toString(isKeyRepeat));
      editor.addContent(keyRepeat);
    }
  }

  public void readData(@NotNull Element element) {
    final Element editor = element.getChild(EDITOR_STORE_ELEMENT);
    if (editor != null) {
      final Element keyRepeat = editor.getChild("key-repeat");
      if (keyRepeat != null) {
        final String enabled = keyRepeat.getAttributeValue("enabled");
        if (enabled != null) {
          isKeyRepeat = Boolean.valueOf(enabled);
        }
      }
    }
  }

  @Override
  public @Nullable Boolean isKeyRepeat() {
    return isKeyRepeat;
  }

  @Override
  public void setKeyRepeat(@Nullable Boolean value) {
    this.isKeyRepeat = value;
  }

  @Override
  public void closeEditorSearchSession(@NotNull VimEditor editor) {
    final Editor ijEditor = ((IjVimEditor)editor).getEditor();
    final EditorSearchSession editorSearchSession = EditorSearchSession.get(ijEditor);
    if (editorSearchSession != null) {
      editorSearchSession.close();
    }
  }

  @Override
  public void editorCreated(@NotNull VimEditor editor) {
    final Editor ijEditor = ((IjVimEditor)editor).getEditor();
    UserDataManager.setVimInitialised(ijEditor, true);

    ((VimKeyGroupBase)VimPlugin.getKey()).registerRequiredShortcutKeys(editor);

    initLineNumbers(ijEditor);

    // Listen for changes to the font size, so we can hide the ex text field/output panel
    if (ijEditor instanceof EditorEx editorEx) {
      editorEx.addPropertyChangeListener(FontSizeChangeListener.INSTANCE);
    }

    if (injector.getApplication().isUnitTest()) {
      updateCaretsVisualAttributes(editor);
    }
  }

  @Override
  public void editorDeinit(@NotNull VimEditor editor) {
    final Editor ijEditor = ((IjVimEditor)editor).getEditor();
    deinitLineNumbers(ijEditor);
    UserDataManager.unInitializeEditor(ijEditor);
    ((VimKeyGroupBase)VimPlugin.getKey()).unregisterShortcutKeys(editor);
    CaretVisualAttributesHelperKt.removeCaretsVisualAttributes(ijEditor);
    if (ijEditor instanceof EditorEx editorEx) {
      editorEx.removePropertyChangeListener(FontSizeChangeListener.INSTANCE);
    }
  }

  @Override
  public void onNumberOptionChanged(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    if (UserDataManager.getVimEditorGroup(ijEditor) && supportsVimLineNumbers(ijEditor)) {
      updateLineNumbers(ijEditor);
    }
  }

  @Override
  public void loadEditorStateData(@NotNull Object element) {
    readData((Element)element);
  }

  public void notifyIdeaJoin(@Nullable Project project, @NotNull VimEditor editor) {
    if (VimPlugin.getVimState().isIdeaJoinNotified() || ijOptions(injector, editor).getIdeajoin()) {
      return;
    }

    VimPlugin.getVimState().setIdeaJoinNotified(true);
    VimPlugin.getNotifications(project).notifyAboutIdeaJoin(editor);
  }

  @Override
  public @Nullable Element getState() {
    Element element = new Element("editor");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }

  @Override
  public void notifyIdeaJoin(@NotNull VimEditor editor) {
    notifyIdeaJoin(((IjVimEditor)editor).getEditor().getProject(), editor);
  }

  @Override
  public void updateCaretsVisualAttributes(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    CaretVisualAttributesHelperKt.updateCaretsVisualAttributes(ijEditor);
  }

  @Override
  public void updateCaretsVisualPosition(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor)editor).getEditor();
    CaretVisualAttributesHelperKt.updateCaretsVisualAttributes(ijEditor);
  }

  @Override
  public @Nullable VimEditor getFocusedEditor() {
    try {
      DataContext dataContext = DataManager.getInstance().getDataContextFromFocusAsync().blockingGet(1000);
      if (dataContext != null) {
        Editor focusedEditor = CommonDataKeys.EDITOR.getData(dataContext);
        if (focusedEditor != null) {
          return new IjVimEditor(focusedEditor);
        }
      }
    }
    catch (TimeoutException | ExecutionException e) {
      return null;
    }
    return null;
  }

  @Override
  public @Nullable VimEditor getSelectedEditor(@NotNull String projectId) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (project.isDisposed()) continue;
      if (!injector.getFile().getProjectId(project).equals(projectId)) continue;
      Editor selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
      if (selectedEditor != null) {
        return new IjVimEditor(selectedEditor);
      }
    }
    return null;
  }

  @Override
  public @NotNull Collection<VimEditor> getEditorsRaw() {
    return getLocalEditors().map(IjVimEditor::new).collect(Collectors.toList());
  }

  @Override
  public @NotNull Collection<VimEditor> getEditors() {
    return getLocalEditors().filter(UserDataManager::getVimInitialised).map(IjVimEditor::new)
      .collect(Collectors.toList());
  }

  @Override
  public @NotNull Collection<VimEditor> getEditors(@NotNull VimDocument buffer) {
    final Document document = ((IjVimDocument)buffer).getDocument();
    return getLocalEditors().filter(
        editor -> UserDataManager.getVimInitialised(editor) && editor.getDocument().equals(document))
      .map(IjVimEditor::new).collect(Collectors.toList());
  }

  private Stream<Editor> getLocalEditors() {
    // Always fetch local editors. If we're hosting a Code With Me session, any connected guests will create hidden
    // editors to handle syntax highlighting, completion requests, etc. We need to make sure that IdeaVim only makes
    // changes (e.g., adding search highlights) to local editors so things don't incorrectly flow through to any Clients.
    // In non-CWM scenarios, or if IdeaVim is installed on the Client, there are only ever local editors, so this will
    // also work there. In Gateway remote development scenarios, IdeaVim should not be installed on the host, only the
    // Client, so all should work there too.
    // Note that most IdeaVim operations are in response to interactive keystrokes, which would mean that
    // ClientEditorManager.getCurrentInstance would return local editors. However, some operations are in response to
    // events such as document change (to update search highlights), and these can come from CWM guests, and we'd get
    // the remote editors.
    // This invocation will always get local editors, regardless of the current context.
    List<ClientAppSession> appSessions = ClientSessionsManager.getAppSessions(ClientKind.LOCAL);
    if (!appSessions.isEmpty()) {
      ClientAppSession localSession = appSessions.get(0);
      return localSession.getService(ClientEditorManager.class).editors();
    }
    else {
      return Stream.empty();
    }
  }

  private static class RelativeLineNumberConverter implements LineNumberConverter {
    @Override
    public Integer convert(@NotNull Editor editor, int lineNumber) {
      final IjVimEditor ijVimEditor = new IjVimEditor(editor);
      final boolean number = options(injector, ijVimEditor).getNumber();
      final int caretLine = editor.getCaretModel().getLogicalPosition().line;

      // lineNumber is 1 based
      if ((lineNumber - 1) == caretLine) {
        return number ? lineNumber : 0;
      }
      else {
        final int visualLine = ijVimEditor.bufferLineToVisualLine(lineNumber - 1);
        final int caretVisualLine = editor.getCaretModel().getVisualPosition().line;
        return Math.abs(caretVisualLine - visualLine);
      }
    }

    @Override
    public Integer getMaxLineNumber(@NotNull Editor editor) {
      return editor.getDocument().getLineCount();
    }
  }

  /**
   * Listens to property changes from the editor to hide ex text field/output panel when the editor's font is zoomed
   */
  private static class FontSizeChangeListener implements PropertyChangeListener {
    public static FontSizeChangeListener INSTANCE = new FontSizeChangeListener();

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if (VimPlugin.isNotEnabled()) return;
      if (evt.getPropertyName().equals(EditorEx.PROP_FONT_SIZE)) {
        Object source = evt.getSource();
        if (source instanceof Editor editor) {
          // The editor is being zoomed, so hide the command line or output panel, if they're being shown. On the one
          // hand, it's a little rude to cancel a command line for the user, but on the other, the panels obscure the
          // zoom indicator, so it looks nicer if we hide them.
          // Note that IDE scale is handled by LafManager.lookAndFeelChanged
          VimCommandLine activeCommandLine = injector.getCommandLine().getActiveCommandLine();
          if (activeCommandLine != null) {
            activeCommandLine.close(true, false);
          }
          VimOutputPanel outputPanel = injector.getOutputPanel().getCurrentOutputPanel();
          if (outputPanel != null) {
            outputPanel.close();
          }
          VimModalInput modalInput = injector.getModalInput().getCurrentModalInput();
          if (modalInput != null) {
            modalInput.deactivate(true, false);
          }
        }
      }
    }
  }
}
