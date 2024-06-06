/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.find.EditorSearchSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.client.ClientAppSession;
import com.intellij.openapi.client.ClientKind;
import com.intellij.openapi.client.ClientSessionsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.helper.CaretVisualAttributesHelperKt;
import com.maddyhome.idea.vim.helper.CommandStateHelper;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.UserDataManager;
import com.maddyhome.idea.vim.newapi.IjVimDocument;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.editor.EditorSettings.*;
import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.newapi.IjVimInjectorKt.ijOptions;

/**
 * @author vlan
 */
@State(name = "VimEditorSettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class EditorGroup implements PersistentStateComponent<Element>, VimEditorGroup {
  public static final @NonNls String EDITOR_STORE_ELEMENT = "editor";

  private Boolean isKeyRepeat = null;

  // TODO: Get rid of this custom line converter once we support soft wraps properly
  // The builtin relative line converter looks like it's using Vim's logical lines for counting, where a Vim logical
  // line is a buffer line, or a single line representing a fold of several buffer lines. This converter is counting
  // screen lines (but badly - if you're on the second line of a wrapped line, it still counts like you're on the first.
  // We really want to use Vim logical lines, but we don't currently support them for movement - we move by screen line.

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
    final EditorGutterComponentEx gutterComponent = gutter instanceof EditorGutterComponentEx ? (EditorGutterComponentEx) gutter : null;
    if (gutterComponent != null) {
      gutterComponent.repaint();
    }
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

  public @Nullable Boolean isKeyRepeat() {
    return isKeyRepeat;
  }

  public void setKeyRepeat(@Nullable Boolean value) {
    this.isKeyRepeat = value;
  }

  public void closeEditorSearchSession(@NotNull Editor editor) {
    final EditorSearchSession editorSearchSession = EditorSearchSession.get(editor);
    if (editorSearchSession != null) {
      editorSearchSession.close();
    }
  }

  public void editorCreated(@NotNull Editor editor) {
    UserDataManager.setVimInitialised(editor, true);

    VimPlugin.getKey().registerRequiredShortcutKeys(new IjVimEditor(editor));

    initLineNumbers(editor);

    // Listen for changes to the font size, so we can hide the ex text field/output panel
    if (editor instanceof EditorEx editorEx) {
      editorEx.addPropertyChangeListener(FontSizeChangeListener.INSTANCE);
    }

    // We add Vim bindings to all opened editors, including editors used as UI controls rather than just project file
    // editors. This includes editors used as part of the UI, such as the VCS commit message, or used as read-only
    // viewers for text output, such as log files in run configurations or the Git Console tab. And editors are used for
    // interactive stdin/stdout for console-based run configurations.
    // We want to provide an intuitive experience for working with these additional editors, so we automatically switch
    // to INSERT mode if they are interactive editors. Recognising these can be a bit tricky.
    // These additional interactive editors are not file-based, but must have a writable document. However, log output
    // documents are also writable (the IDE is writing new content as it becomes available) just not user-editable. So
    // we must also check that the editor is not in read-only "viewer" mode (this includes "rendered" mode, which is
    // read-only and also hides the caret).
    // Furthermore, interactive stdin/stdout console output in run configurations is hosted in a read-only editor, but
    // it can still be edited. The `ConsoleViewImpl` class installs a typing handler that ignores the editor's
    // `isViewer` property and allows typing if the associated process (if any) is still running. We can get the
    // editor's console view and check this ourselves, but we have to wait until the editor has finished initialising
    // before it's available in user data.
    // Finally, we have a special check for diff windows. If we compare against clipboard, we get a diff editor that is
    // not file based, is writable, and not a viewer, but we don't want to treat this as an interactive editor.
    // Note that we need a similar check in `VimEditor.isWritable` to allow Escape to work to exit insert mode. We need
    // to know that a read-only editor that is hosting a console view with a running process can be treated as writable.
    Runnable switchToInsertMode = () -> {
      ExecutionContext context = injector.getExecutionContextManager().getEditorExecutionContext(new IjVimEditor(editor));
      VimPlugin.getChange().insertBeforeCursor(new IjVimEditor(editor), context);
      KeyHandler.getInstance().reset(new IjVimEditor(editor));
    };
    if (!editor.isViewer() &&
        !EditorHelper.isFileEditor(editor) &&
        editor.getDocument().isWritable() &&
        !CommandStateHelper.inInsertMode(editor) &&
        editor.getEditorKind() != EditorKind.DIFF) {
      switchToInsertMode.run();
    }
    ApplicationManager.getApplication().invokeLater(
      () -> {
        if (editor.isDisposed()) return;
        ConsoleViewImpl consoleView = editor.getUserData(ConsoleViewImpl.CONSOLE_VIEW_IN_EDITOR_VIEW);
        if (consoleView != null && consoleView.isRunning() && !CommandStateHelper.inInsertMode(editor)) {
          switchToInsertMode.run();
        }
      });
    updateCaretsVisualAttributes(new IjVimEditor(editor));
  }

  public void editorDeinit(@NotNull Editor editor) {
    deinitLineNumbers(editor);
    UserDataManager.unInitializeEditor(editor);
    VimPlugin.getKey().unregisterShortcutKeys(new IjVimEditor(editor));
    CaretVisualAttributesHelperKt.removeCaretsVisualAttributes(editor);
    if (editor instanceof EditorEx editorEx) {
      editorEx.removePropertyChangeListener(FontSizeChangeListener.INSTANCE);
    }
  }

  public void notifyIdeaJoin(@Nullable Project project, @NotNull VimEditor editor) {
    if (VimPlugin.getVimState().isIdeaJoinNotified() || ijOptions(injector, editor).getIdeajoin()) {
      return;
    }

    VimPlugin.getVimState().setIdeaJoinNotified(true);
    VimPlugin.getNotifications(project).notifyAboutIdeaJoin(editor);
  }

  @Nullable
  @Override
  public Element getState() {
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
    notifyIdeaJoin(((IjVimEditor) editor).getEditor().getProject(), editor);
  }

  @Override
  public void updateCaretsVisualAttributes(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor) editor).getEditor();
    CaretVisualAttributesHelperKt.updateCaretsVisualAttributes(ijEditor);
  }

  @Override
  public void updateCaretsVisualPosition(@NotNull VimEditor editor) {
    Editor ijEditor = ((IjVimEditor) editor).getEditor();
    CaretVisualAttributesHelperKt.updateCaretsVisualAttributes(ijEditor);
  }

  public static class NumberChangeListener implements EffectiveOptionValueChangeListener {
    public static NumberChangeListener INSTANCE = new NumberChangeListener();

    @Contract(pure = true)
    private NumberChangeListener() {
    }

    @Override
    public void onEffectiveValueChanged(@NotNull VimEditor editor) {
      Editor ijEditor = ((IjVimEditor)editor).getEditor();

      if (UserDataManager.getVimEditorGroup(ijEditor) && supportsVimLineNumbers(ijEditor)) {
        updateLineNumbers(ijEditor);
      }
    }
  }

  private static class RelativeLineNumberConverter implements LineNumberConverter {
    @Override
    public Integer convert(@NotNull Editor editor, int lineNumber) {
      final IjVimEditor ijVimEditor = new IjVimEditor(editor);
      final boolean number = ijOptions(injector, ijVimEditor).getNumber();
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

  @Override
  public @NotNull Collection<VimEditor> getEditorsRaw() {
    return getLocalEditors()
      .map(IjVimEditor::new)
      .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Collection<VimEditor> getEditors() {
    return getLocalEditors()
      .filter(UserDataManager::getVimInitialised)
      .map(IjVimEditor::new)
      .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Collection<VimEditor> getEditors(@NotNull VimDocument buffer) {
    final Document document = ((IjVimDocument)buffer).getDocument();
    return getLocalEditors()
      .filter(editor -> UserDataManager.getVimInitialised(editor) && editor.getDocument().equals(document))
      .map(IjVimEditor::new)
      .collect(Collectors.toList());
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
    List<ClientAppSession> appSessions = ClientSessionsManager.getAppSessions(ClientKind.ALL);
    if (!appSessions.isEmpty()) {
      ClientAppSession localSession = appSessions.get(0);
      return localSession.getService(ClientEditorManager.class).editors();
    }
    else {
      return Stream.empty();
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
            injector.getProcessGroup().cancelExEntry(new IjVimEditor(editor), false);
          }
          ExOutputModel.getInstance(editor).close();
        }
      }
    }
  }
}
