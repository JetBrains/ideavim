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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.find.EditorSearchSession;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.CaretAdapter;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

/**
 * @author vlan
 */
public class EditorGroup {
  private static final boolean BLOCK_CURSOR_VIM_VALUE = true;
  private static final boolean ANIMATED_SCROLLING_VIM_VALUE = false;
  private static final boolean REFRAIN_FROM_SCROLLING_VIM_VALUE = true;

  private boolean isBlockCursor = false;
  private boolean isAnimatedScrolling = false;
  private boolean isRefrainFromScrolling = false;
  private Boolean isKeyRepeat = null;

  private boolean isSmartJoinNotified = false;

  private final CaretListener myLineNumbersCaretListener = new CaretAdapter() {
    @Override
    public void caretPositionChanged(CaretEvent e) {
      updateLineNumbers(e.getEditor());
    }
  };

  public void turnOn() {
    setCursors(BLOCK_CURSOR_VIM_VALUE);
    setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);

    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      if (!UserDataManager.getVimEditorGroup(editor)) {
        initLineNumbers(editor);
      }
    }
  }

  public void turnOff() {
    setCursors(isBlockCursor);
    setAnimatedScrolling(isAnimatedScrolling);
    setRefrainFromScrolling(isRefrainFromScrolling);

    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      deinitLineNumbers(editor);
    }
  }

  private void initLineNumbers(@NotNull final Editor editor) {
    editor.getCaretModel().addCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, true);

    final EditorSettings settings = editor.getSettings();
    UserDataManager.setVimLineNumbersShown(editor, settings.isLineNumbersShown());
    updateLineNumbers(editor);
  }

  private void deinitLineNumbers(@NotNull Editor editor) {
    editor.getCaretModel().removeCaretListener(myLineNumbersCaretListener);
    UserDataManager.setVimEditorGroup(editor, false);

    editor.getGutter().closeAllAnnotations();

    final Project project = editor.getProject();
    if (project == null || project.isDisposed()) return;

    editor.getSettings().setLineNumbersShown(UserDataManager.getVimLineNumbersShown(editor));
  }

  private static void updateLineNumbers(@NotNull Editor editor) {
    if (!EditorHelper.isFileEditor(editor)) {
      return;
    }

    final boolean relativeLineNumber = OptionsManager.INSTANCE.getRelativenumber().isSet();
    final boolean lineNumber = OptionsManager.INSTANCE.getNumber().isSet();

    final EditorSettings settings = editor.getSettings();
    final boolean showEditorLineNumbers = (UserDataManager.getVimLineNumbersShown(editor) || lineNumber) && !relativeLineNumber;

    if (settings.isLineNumbersShown() ^ showEditorLineNumbers) {
      // Update line numbers later since it may be called from a caret listener
      // on the caret move and it may move the caret internally
      ApplicationManager.getApplication().invokeLater(() -> {
        if (editor.isDisposed()) return;
        settings.setLineNumbersShown(showEditorLineNumbers);
      });
    }

    if (relativeLineNumber) {
      final EditorGutter gutter = editor.getGutter();
      gutter.closeAllAnnotations();
      gutter.registerTextAnnotation(LineNumbersGutterProvider.INSTANCE);
    }
  }

  private void setCursors(boolean isBlock) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      // Vim plugin should be turned on in insert mode
      ((EditorEx)editor).setInsertMode(true);
      editor.getSettings().setBlockCursor(isBlock);
    }
  }

  private void setAnimatedScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setAnimatedScrolling(isOn);
    }
  }

  private void setRefrainFromScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setRefrainFromScrolling(isOn);
    }
  }

  public void saveData(@NotNull Element element) {
    final Element editor = new Element("editor");
    element.addContent(editor);

    final Element smartJoin = new Element("smart-join");
    smartJoin.setAttribute("enabled", Boolean.toString(isSmartJoinNotified));
    editor.addContent(smartJoin);
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
      final Element smartJoin = editor.getChild("smart-join");
      if (smartJoin != null) {
        final String enabled = smartJoin.getAttributeValue("enabled");
        if (enabled != null) {
          isSmartJoinNotified = Boolean.parseBoolean(enabled);
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

  public void notifyAboutSmartJoin() {
    if (isSmartJoinNotified || OptionsManager.INSTANCE.getSmartjoin().isSet()) return;

    isSmartJoinNotified = true;

    new Notification(VimPlugin.IDEAVIM_STICKY_NOTIFICATION_ID, VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                     "Put \"<code>set smartjoin</code>\" into your <code>.ideavimrc</code> to perform a join via the IDE",
                     NotificationType.INFORMATION).notify(null);
  }

  public void closeEditorSearchSession(@NotNull Editor editor) {
    final EditorSearchSession editorSearchSession = EditorSearchSession.get(editor);
    if (editorSearchSession != null) {
      editorSearchSession.close();
    }
  }

  public void editorCreated(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    isBlockCursor = editor.getSettings().isBlockCursor();
    isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
    isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
    DocumentManager.getInstance().addListeners(editor.getDocument());
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

  public void editorReleased(@NotNull EditorFactoryEvent event) {
    final Editor editor = event.getEditor();
    deinitLineNumbers(editor);
    UserDataManager.unInitializeEditor(editor);
    VimPlugin.getKey().unregisterShortcutKeys(editor);
    editor.getSettings().setAnimatedScrolling(isAnimatedScrolling);
    editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
    DocumentManager.getInstance().removeListeners(editor.getDocument());
  }

  public static class NumberChangeListener implements OptionChangeListener {
    public static NumberChangeListener INSTANCE = new NumberChangeListener();
    private NumberChangeListener() {
    }
    @Override
    public void valueChange(OptionChangeEvent event) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        updateLineNumbers(editor);
      }
    }
  }

  private static class LineNumbersGutterProvider implements TextAnnotationGutterProvider {

    public static LineNumbersGutterProvider INSTANCE = new LineNumbersGutterProvider();

    private LineNumbersGutterProvider() {
    }

    @Nullable
    @Override
    public String getLineText(int line, @NotNull Editor editor) {
      if (VimPlugin.isEnabled() && EditorHelper.isFileEditor(editor)) {
        final boolean relativeLineNumber = OptionsManager.INSTANCE.getRelativenumber().isSet();
        final boolean lineNumber = OptionsManager.INSTANCE.getNumber().isSet();
        if (relativeLineNumber && lineNumber && isCaretLine(line, editor)) {
          return lineNumberToString(getLineNumber(line), editor);
        }
        else if (relativeLineNumber) {
          return lineNumberToString(getRelativeLineNumber(line, editor), editor);
        }
      }
      return null;
    }

    private boolean isCaretLine(int line, @NotNull Editor editor) {
      return line == editor.getCaretModel().getLogicalPosition().line;
    }

    private int getLineNumber(int line) {
      return line + 1;
    }

    private int getRelativeLineNumber(int line, @NotNull Editor editor) {
      final int visualLine = EditorHelper.logicalLineToVisualLine(editor, line);
      final int currentLine = editor.getCaretModel().getLogicalPosition().line;
      final int currentVisualLine = EditorHelper.logicalLineToVisualLine(editor, currentLine);
      return Math.abs(currentVisualLine - visualLine);
    }

    private String lineNumberToString(int lineNumber, @NotNull Editor editor) {
      final int lineCount = editor.getDocument().getLineCount();
      final int digitsCount = (int)Math.ceil(Math.log10(lineCount));
      return StringHelper.leftJustify("" + lineNumber, digitsCount, ' ');
    }

    @Nullable
    @Override
    public String getToolTip(int line, Editor editor) {
      return null;
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
      return null;
    }

    @Nullable
    @Override
    public ColorKey getColor(int line, Editor editor) {
      return EditorColors.LINE_NUMBERS_COLOR;
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
    }
  }
}
