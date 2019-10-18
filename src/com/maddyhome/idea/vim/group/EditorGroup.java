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

package com.maddyhome.idea.vim.group;

import com.intellij.find.EditorSearchSession;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import gnu.trove.TIntFunction;
import kotlin.text.StringsKt;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author vlan
 */
public class EditorGroup {
  private static final boolean ANIMATED_SCROLLING_VIM_VALUE = false;
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;

  private boolean isBlockCursor = false;
  private boolean isAnimatedScrolling = false;
  private boolean isRefrainFromScrolling = false;
  private Boolean isKeyRepeat = null;

  private final CaretListener myLineNumbersCaretListener = new CaretListener() {
    @Override
    public void caretPositionChanged(@NotNull CaretEvent e) {
      final boolean requiresRepaint = e.getNewPosition().line != e.getOldPosition().line;
      updateLineNumbers(e.getEditor(), requiresRepaint);
    }
  };

  public void turnOn() {
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      VimPlugin.getEditor().editorCreated(editor);
    }
  }

  public void turnOff() {
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      VimPlugin.getEditor().editorDeinit(editor, false);
    }
  }

  private void initLineNumbers(@NotNull final Editor editor) {
    if (!supportsVimLineNumbers(editor) || UserDataManager.getVimEditorGroup(editor)) {
      return;
    }

    editor.getCaretModel().addCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, true);

    UserDataManager.setVimLineNumbersInitialState(editor, editor.getSettings().isLineNumbersShown());
    updateLineNumbers(editor, true);
  }

  private void deinitLineNumbers(@NotNull Editor editor, boolean isReleasing) {
    if (!supportsVimLineNumbers(editor) || !UserDataManager.getVimEditorGroup(editor)) {
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

  private static boolean supportsVimLineNumbers(@NotNull final Editor editor) {
    // We only support line numbers in editors that are file based, and that aren't for diffs, which control their
    // own line numbers, often using EditorGutterComponentEx#setLineNumberConvertor
    return EditorHelper.isFileEditor(editor) && !EditorHelper.isDiffEditor(editor);
  }

  private static void updateLineNumbers(@NotNull final Editor editor, final boolean requiresRepaint) {
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
      else if (requiresRepaint) {
        repaintRelativeLineNumbers(editor);
      }
    }
    else if (hasRelativeLineNumbersInstalled(editor)) {
      removeRelativeLineNumbers(editor);
    }
  }

  private static boolean shouldShowBuiltinLineNumbers(@NotNull final Editor editor, boolean number, boolean relativeNumber) {
    final boolean initialState = UserDataManager.getVimLineNumbersInitialState(editor);

    // Builtin relative line numbers requires EditorGutterComponentEx#setLineNumberConvertor. If we don't have that,
    // fall back to the text annotation provider, which replaces the builtin line numbers
    // AFAICT, this will always be true, but I can't guarantee it
    if (editor.getGutter() instanceof EditorGutterComponentEx) {
      return initialState || number || relativeNumber;
    }

    return (initialState || number) && !relativeNumber;
  }

  private static void setBuiltinLineNumbers(@NotNull final Editor editor, boolean show) {
    editor.getSettings().setLineNumbersShown(show);
  }

  private static boolean hasRelativeLineNumbersInstalled(@NotNull final Editor editor) {
    return UserDataManager.getVimHasRelativeLineNumbersInstalled(editor);
  }

  private static void installRelativeLineNumbers(@NotNull final Editor editor) {
    if (!hasRelativeLineNumbersInstalled(editor)) {
      final EditorGutter gutter = editor.getGutter();
      if (gutter instanceof EditorGutterComponentEx) {
        ((EditorGutterComponentEx) gutter).setLineNumberConvertor(new RelativeLineNumberConverter(editor));
      }
      else {
        gutter.registerTextAnnotation(new RelativeLineNumberGutterProvider(editor));
      }
      UserDataManager.setVimHasRelativeLineNumbersInstalled(editor, true);
    }
  }

  private static void removeRelativeLineNumbers(@NotNull final Editor editor) {
    if (hasRelativeLineNumbersInstalled(editor)) {
      final EditorGutter gutter = editor.getGutter();
      if (gutter instanceof EditorGutterComponentEx) {
        ((EditorGutterComponentEx) gutter).setLineNumberConvertor(null);
      }
      else {
        // TODO:[VERSION UPDATE] 192 gives us an API to close just one annotation provider
        gutter.closeAllAnnotations();
      }
      UserDataManager.setVimHasRelativeLineNumbersInstalled(editor, false);
    }
  }

  private static void repaintRelativeLineNumbers(@NotNull final Editor editor) {
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
    final Element editor = element.getChild("editor");
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

  @Nullable
  public Boolean isKeyRepeat() {
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
    isBlockCursor = editor.getSettings().isBlockCursor();
    isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
    isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
    DocumentManager.INSTANCE.addListeners(editor.getDocument());
    VimPlugin.getKey().registerRequiredShortcutKeys(editor);

    initLineNumbers(editor);
    // Turn on insert mode if editor doesn't have any file
    if (!EditorHelper.isFileEditor(editor) &&
        editor.getDocument().isWritable() &&
        !CommandStateHelper.inInsertMode(editor)) {
      VimPlugin.getChange().insertBeforeCursor(editor, new EditorDataContext(editor));
      KeyHandler.getInstance().reset(editor);
    }
    editor.getSettings().setBlockCursor(!CommandStateHelper.inInsertMode(editor));
    editor.getSettings().setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
  }

  public void editorDeinit(@NotNull Editor editor, boolean isReleased) {
    deinitLineNumbers(editor, isReleased);
    UserDataManager.unInitializeEditor(editor);
    VimPlugin.getKey().unregisterShortcutKeys(editor);
    editor.getSettings().setBlockCursor(isBlockCursor);
    editor.getSettings().setAnimatedScrolling(isAnimatedScrolling);
    editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
    DocumentManager.INSTANCE.removeListeners(editor.getDocument());
  }

  public void notifyIdeaJoin(@Nullable Project project) {
    if (VimPlugin.getVimState().isIdeaJoinNotified() || OptionsManager.INSTANCE.getIdeajoin().isSet()) return;

    VimPlugin.getVimState().setIdeaJoinNotified(true);
    VimPlugin.getNotifications(project).notifyAboutIdeaJoin();
  }

  public static class NumberChangeListener implements OptionChangeListener {
    public static NumberChangeListener INSTANCE = new NumberChangeListener();

    @Contract(pure = true)
    private NumberChangeListener() {
    }

    @Override
    public void valueChange(OptionChangeEvent event) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        if (UserDataManager.getVimEditorGroup(editor) && supportsVimLineNumbers(editor)) {
          updateLineNumbers(editor, true);
        }
      }
    }
  }

  private static class RelativeLineNumberConverter implements TIntFunction {
    @NotNull
    private final Editor editor;

    @Contract(pure = true)
    RelativeLineNumberConverter(@NotNull final Editor editor) {
      this.editor = editor;
    }

    @Override
    public int execute(int line) {
      final boolean number = OptionsManager.INSTANCE.getNumber().isSet();
      final int caretLine = editor.getCaretModel().getLogicalPosition().line;

      if (number && line == caretLine) {
        return line;
      }
      else {
        return getRelativeLineNumber(line, editor, caretLine);
      }
    }

    private int getRelativeLineNumber(int line, @NotNull Editor editor, int caretLine) {
      final int visualLine = EditorHelper.logicalLineToVisualLine(editor, line);
      final int currentVisualLine = EditorHelper.logicalLineToVisualLine(editor, caretLine);
      return Math.abs(currentVisualLine - visualLine) - 1;
    }
  }

  private static class RelativeLineNumberGutterProvider implements TextAnnotationGutterProvider {
    @NotNull
    private final Editor editor;

    @Contract(pure = true)
    RelativeLineNumberGutterProvider(@NotNull final Editor editor) {
      this.editor = editor;
    }

    @Nullable
    @Override
    public String getLineText(int line, @NotNull Editor editor) {
      final boolean number = OptionsManager.INSTANCE.getNumber().isSet();
      if (number && isCaretLine(line, editor)) {
        return lineNumberToString(line + 1, editor, true);
      } else {
        return lineNumberToString(getRelativeLineNumber(line, editor), editor, false);
      }
    }

    private boolean isCaretLine(int line, @NotNull Editor editor) {
      return line == editor.getCaretModel().getLogicalPosition().line;
    }

    private int getRelativeLineNumber(int line, @NotNull Editor editor) {
      final int visualLine = EditorHelper.logicalLineToVisualLine(editor, line);
      final int currentLine = editor.getCaretModel().getLogicalPosition().line;
      final int currentVisualLine = EditorHelper.logicalLineToVisualLine(editor, currentLine);
      return Math.abs(currentVisualLine - visualLine);
    }

    private String lineNumberToString(int lineNumber, @NotNull Editor editor, boolean leftJustify) {
      final int lineCount = editor.getDocument().getLineCount();
      final int digitsCount = lineCount == 0 ? 1 : (int)Math.ceil(Math.log10(lineCount));
      return leftJustify
        ? StringsKt.padEnd(Integer.toString(lineNumber), digitsCount, ' ')
        : StringsKt.padStart(Integer.toString(lineNumber), digitsCount, ' ');
    }

    @Nullable
    @Override
    public String getToolTip(int line, Editor editor) {
      return null;
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
      return isCaretLine(line, editor) ? EditorFontType.BOLD: null;
    }

    @Nullable
    @Override
    public ColorKey getColor(int line, Editor editor) {
      return isCaretLine(line, editor) ? EditorColors.LINE_NUMBER_ON_CARET_ROW_COLOR : EditorColors.LINE_NUMBERS_COLOR;
    }

    @Nullable
    @Override
    public Color getBgColor(int line, Editor editor) {
      return null;
    }

    @Override
    public List<AnAction> getPopupActions(int line, Editor editor) {
      return null;
    }

    @Override
    public void gutterClosed() {
      UserDataManager.setVimHasRelativeLineNumbersInstalled(this.editor, false);
    }
  }
}
