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
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.newapi.IjVimDocument;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.api.VimInjectorKt.options;
import static com.maddyhome.idea.vim.helper.CaretVisualAttributesHelperKt.updateCaretsVisualAttributes;
import static com.maddyhome.idea.vim.newapi.IjVimInjectorKt.ijOptions;

/**
 * @author vlan
 */
@State(name = "VimEditorSettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class EditorGroup implements PersistentStateComponent<Element>, VimEditorGroup {
  public static final @NonNls
  String EDITOR_STORE_ELEMENT = "editor";

  private Boolean isKeyRepeat = null;

  private final CaretListener myLineNumbersCaretListener = new CaretListener() {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      final boolean requiresRepaint = e.getNewPosition().line != e.getOldPosition().line;
      if (requiresRepaint && options(injector, new IjVimEditor(e.getEditor())).getRelativenumber()) {
        repaintRelativeLineNumbers(e.getEditor());
      }
    }
  };

  private void initLineNumbers(final @NotNull Editor editor) {
    if (!supportsVimLineNumbers(editor) || UserDataManager.getVimEditorGroup(editor)) {
      return;
    }

    editor.getCaretModel().addCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, true);

    UserDataManager.setVimLineNumbersInitialState(editor, editor.getSettings().isLineNumbersShown());
    updateLineNumbers(editor);
  }

  private void deinitLineNumbers(@NotNull Editor editor, boolean isReleasing) {
    if (isProjectDisposed(editor) || !supportsVimLineNumbers(editor) || !UserDataManager.getVimEditorGroup(editor)) {
      return;
    }

    editor.getCaretModel().removeCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, false);

    removeRelativeLineNumbers(editor);

    // Don't reset the built in line numbers if we're releasing the editor. If we do, EditorSettings.setLineNumbersShown
    // can cause the editor to refresh settings and can call into FileManagerImpl.getCachedPsiFile AFTER FileManagerImpl
    // has been disposed (Closing the project with a Find Usages result showing a preview panel is a good repro case).
    // See IDEA-184351 and VIM-1671
    if (!isReleasing) {
      setBuiltinLineNumbers(editor, UserDataManager.getVimLineNumbersInitialState(editor));
    }
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
    final EffectiveOptions options = options(injector, new IjVimEditor(editor));
    final boolean relativeNumber = options.getRelativenumber();
    final boolean number = options.getNumber();

    final boolean showBuiltinEditorLineNumbers = shouldShowBuiltinLineNumbers(editor, number, relativeNumber);

    final EditorSettings settings = editor.getSettings();
    if (settings.isLineNumbersShown() ^ showBuiltinEditorLineNumbers) {
      // Update line numbers later since it may be called from a caret listener
      // on the caret move and it may move the caret internally
      ApplicationManager.getApplication().invokeLater(() -> {
        if (editor.isDisposed()) return;
        setBuiltinLineNumbers(editor, showBuiltinEditorLineNumbers);
      });
    }

    if (relativeNumber) {
      if (!hasRelativeLineNumbersInstalled(editor)) {
        installRelativeLineNumbers(editor);
      }
    } else if (hasRelativeLineNumbersInstalled(editor)) {
      removeRelativeLineNumbers(editor);
    }
  }

  private static boolean shouldShowBuiltinLineNumbers(final @NotNull Editor editor, boolean number, boolean relativeNumber) {
    final boolean initialState = UserDataManager.getVimLineNumbersInitialState(editor);
    return initialState || number || relativeNumber;
  }

  private static void setBuiltinLineNumbers(final @NotNull Editor editor, boolean show) {
    editor.getSettings().setLineNumbersShown(show);
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

  public @Nullable
  Boolean isKeyRepeat() {
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
    DocumentManager.INSTANCE.addListeners(editor.getDocument());
    VimPlugin.getKey().registerRequiredShortcutKeys(new IjVimEditor(editor));

    initLineNumbers(editor);

    // We add Vim bindings to all opened editors, even read-only editors. We also add bindings to editors that are used
    // elsewhere in the IDE, rather than just for editing project files. This includes editors used as part of the UI,
    // such as the VCS commit message, or used as read-only viewers for text output, such as log files in run
    // configurations or the Git Console tab. And editors are used for interactive stdin/stdout for console-based run
    // configurations.
    // We want to provide an intuitive experience for working with these additional editors, so we automatically switch
    // to INSERT mode for interactive editors. Recognising these can be a bit tricky.
    // These additional interactive editors are not file-based, but must have a writable document. However, log output
    // documents are also writable (the IDE is writing new content as it becomes available) just not user-editable. So
    // we must also check that the editor is not in read-only "viewer" mode (this includes "rendered" mode, which is
    // read-only and also hides the caret).
    // Furthermore, the interactive stdin/stdout console output is hosted in a read-only editor, but it can still be
    // edited. The `ConsoleViewImpl` class installs a typing handler that ignores the editor's `isViewer` property and
    // allows typing if the associated process (if any) is still running. We can get the editor's console view and check
    // this ourselves, but we have to wait until the editor has finished initialising before it's available in user
    // data.
    // Note that we need a similar check in `VimEditor.isWritable` to allow Escape to work to exit insert mode. We need
    // to know that a read-only editor that is hosting a console view with a running process can be treated as writable.
    Runnable switchToInsertMode = () -> {
      ExecutionContext.Editor context = injector.getExecutionContextManager().onEditor(new IjVimEditor(editor), null);
      VimPlugin.getChange().insertBeforeCursor(new IjVimEditor(editor), context);
      KeyHandler.getInstance().reset(new IjVimEditor(editor));
    };
    if (!editor.isViewer() &&
      !EditorHelper.isFileEditor(editor) &&
      editor.getDocument().isWritable() &&
      !CommandStateHelper.inInsertMode(editor)) {
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
    updateCaretsVisualAttributes(editor);
  }

  public void editorDeinit(@NotNull Editor editor, boolean isReleased) {
    deinitLineNumbers(editor, isReleased);
    UserDataManager.unInitializeEditor(editor);
    VimPlugin.getKey().unregisterShortcutKeys(new IjVimEditor(editor));
    DocumentManager.INSTANCE.removeListeners(editor.getDocument());
    CaretVisualAttributesHelperKt.removeCaretsVisualAttributes(editor);
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

  public static class NumberChangeListener implements EffectiveOptionValueChangeListener {
    public static NumberChangeListener INSTANCE = new NumberChangeListener();

    @Contract(pure = true)
    private NumberChangeListener() {
    }

    @Override
    public void onEffectiveValueChanged(@NotNull VimEditor editor) {
      Editor ijEditor = ((IjVimEditor) editor).getEditor();

      if (UserDataManager.getVimEditorGroup(ijEditor) && supportsVimLineNumbers(ijEditor)) {
        updateLineNumbers(ijEditor);
      }
    }
  }

  private static class RelativeLineNumberConverter implements LineNumberConverter {
    @Override
    public Integer convert(@NotNull Editor editor, int lineNumber) {
      final boolean number = options(injector, new IjVimEditor(editor)).getNumber();
      final int caretLine = editor.getCaretModel().getLogicalPosition().line;

      // lineNumber is 1 based
      if (number && (lineNumber - 1) == caretLine) {
        return lineNumber;
      } else {
        final int visualLine = new IjVimEditor(editor).bufferLineToVisualLine(lineNumber - 1);
        final int currentVisualLine = new IjVimEditor(editor).bufferLineToVisualLine(caretLine);
        return Math.abs(currentVisualLine - visualLine);
      }
    }

    @Override
    public Integer getMaxLineNumber(@NotNull Editor editor) {
      return editor.getDocument().getLineCount();
    }
  }

  @NotNull
  @Override
  public Collection<VimEditor> localEditors() {
    return HelperKt.localEditors().stream()
      .map(IjVimEditor::new)
      .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Collection<VimEditor> localEditors(@NotNull VimDocument buffer) {
    final Document document = ((IjVimDocument) buffer).getDocument();
    return HelperKt.localEditors(document).stream()
      .map(IjVimEditor::new)
      .collect(Collectors.toList());
  }
}
