/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.group;

import com.intellij.find.EditorSearchSession;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.group.visual.VisualGroupKt;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vlan
 */
@State(name = "VimEditorSettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class EditorGroup implements PersistentStateComponent<Element> {
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;
  public static final @NonNls String EDITOR_STORE_ELEMENT = "editor";

  private boolean isBlockCursor = false;
  private boolean isRefrainFromScrolling = false;
  private Boolean isKeyRepeat = null;

  private final CaretListener myLineNumbersCaretListener = new CaretListener() {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      final boolean requiresRepaint = e.getNewPosition().line != e.getOldPosition().line;
      if (requiresRepaint && OptionsManager.INSTANCE.getRelativenumber().isSet()) {
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
    final boolean relativeNumber = OptionsManager.INSTANCE.getRelativenumber().isSet();
    final boolean number = OptionsManager.INSTANCE.getNumber().isSet();

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
    }
    else if (hasRelativeLineNumbersInstalled(editor)) {
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

  public boolean isBarCursorSettings() {
    return !EditorSettingsExternalizable.getInstance().isBlockCursor();
  }

  public void editorCreated(@NotNull Editor editor) {
    isBlockCursor = editor.getSettings().isBlockCursor();
    isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
    DocumentManager.INSTANCE.addListeners(editor.getDocument());
    VimPlugin.getKey().registerRequiredShortcutKeys(editor);

    initLineNumbers(editor);
    // Turn on insert mode if editor doesn't have any file
    if (!EditorHelper.isFileEditor(editor) &&
        editor.getDocument().isWritable() &&
        !CommandStateHelper.inInsertMode(editor)) {
      VimPlugin.getChange().insertBeforeCursor(editor, new EditorDataContext(editor, null));
      KeyHandler.getInstance().reset(editor);
    }
    VisualGroupKt.resetShape(CommandStateHelper.getMode(editor), editor);
    editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
  }

  public void editorDeinit(@NotNull Editor editor, boolean isReleased) {
    deinitLineNumbers(editor, isReleased);
    UserDataManager.unInitializeEditor(editor);
    VimPlugin.getKey().unregisterShortcutKeys(editor);
    editor.getSettings().setBlockCursor(isBlockCursor);
    editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
    DocumentManager.INSTANCE.removeListeners(editor.getDocument());
  }

  public void notifyIdeaJoin(@Nullable Project project) {
    if (VimPlugin.getVimState().isIdeaJoinNotified() || OptionsManager.INSTANCE.getIdeajoin().isSet()) return;

    VimPlugin.getVimState().setIdeaJoinNotified(true);
    VimPlugin.getNotifications(project).notifyAboutIdeaJoin();
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

  public static class NumberChangeListener implements OptionChangeListener<Boolean> {
    public static NumberChangeListener INSTANCE = new NumberChangeListener();

    @Contract(pure = true)
    private NumberChangeListener() {
    }

    @Override
    public void valueChange(Boolean oldValue, Boolean newValue) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        if (UserDataManager.getVimEditorGroup(editor) && supportsVimLineNumbers(editor)) {
          updateLineNumbers(editor);
        }
      }
    }
  }

  private static class RelativeLineNumberConverter implements LineNumberConverter {
    @Override
    public Integer convert(@NotNull Editor editor, int lineNumber) {
      final boolean number = OptionsManager.INSTANCE.getNumber().isSet();
      final int caretLine = editor.getCaretModel().getLogicalPosition().line;

      // lineNumber is 1 based
      if (number && (lineNumber - 1) == caretLine) {
        return lineNumber;
      }
      else {
        final int visualLine = EditorHelper.logicalLineToVisualLine(editor, lineNumber - 1);
        final int currentVisualLine = EditorHelper.logicalLineToVisualLine(editor, caretLine);
        return Math.abs(currentVisualLine - visualLine);
      }
    }

    @Override
    public Integer getMaxLineNumber(@NotNull Editor editor) {
      return editor.getDocument().getLineCount();
    }
  }
}
