/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.Options;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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

  private final CaretListener myLineNumbersCaretListener = new CaretAdapter() {
    @Override
    public void caretPositionChanged(CaretEvent e) {
      updateLineNumbers(e.getEditor());
    }
  };

  private final LineNumbersGutterProvider myLineNumbersGutterProvider = new LineNumbersGutterProvider();

  public EditorGroup() {
    final Options options = Options.getInstance();
    final OptionChangeListener numbersChangeListener = new OptionChangeListener() {
      @Override
      public void valueChange(OptionChangeEvent event) {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
          updateLineNumbers(editor);
        }
      }
    };
    options.getOption(Options.NUMBER).addOptionChangeListener(numbersChangeListener);
    options.getOption(Options.RELATIVE_NUMBER).addOptionChangeListener(numbersChangeListener);

    EventFacade.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        isBlockCursor = editor.getSettings().isBlockCursor();
        isAnimatedScrolling = editor.getSettings().isAnimatedScrolling();
        isRefrainFromScrolling = editor.getSettings().isRefrainFromScrolling();
        EditorData.initializeEditor(editor);
        DocumentManager.getInstance().addListeners(editor.getDocument());
        VimPlugin.getKey().registerRequiredShortcutKeys(editor);

        if (VimPlugin.isEnabled()) {
          initLineNumbers(editor);
          // Turn on insert mode if editor doesn't have any file
          if (!EditorData.isFileEditor(editor) && editor.getDocument().isWritable() &&
              !CommandState.inInsertMode(editor)) {
            KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStroke('i'), new EditorDataContext(editor));
          }
          editor.getSettings().setBlockCursor(!CommandState.inInsertMode(editor));
          editor.getSettings().setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
          editor.getSettings().setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);
        }
      }

      @Override
      public void editorReleased(@NotNull EditorFactoryEvent event) {
        final Editor editor = event.getEditor();
        deinitLineNumbers(editor);
        EditorData.unInitializeEditor(editor);
        VimPlugin.getKey().unregisterShortcutKeys(editor);
        editor.getSettings().setAnimatedScrolling(isAnimatedScrolling);
        editor.getSettings().setRefrainFromScrolling(isRefrainFromScrolling);
        DocumentManager.getInstance().removeListeners(editor.getDocument());
      }
    }, ApplicationManager.getApplication());
  }

  public void turnOn() {
    setCursors(BLOCK_CURSOR_VIM_VALUE);
    setAnimatedScrolling(ANIMATED_SCROLLING_VIM_VALUE);
    setRefrainFromScrolling(REFRAIN_FROM_SCROLLING_VIM_VALUE);

    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      if (!EditorData.getEditorGroup(editor)) {
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
    EditorData.setEditorGroup(editor, true);

    final EditorSettings settings = editor.getSettings();
    EditorData.setLineNumbersShown(editor, settings.isLineNumbersShown());
    updateLineNumbers(editor);
  }

  private void deinitLineNumbers(@NotNull Editor editor) {
    editor.getCaretModel().removeCaretListener(myLineNumbersCaretListener);
    EditorData.setEditorGroup(editor, false);

    editor.getGutter().closeAllAnnotations();

    final Project project = editor.getProject();
    if (project == null || project.isDisposed()) return;

    editor.getSettings().setLineNumbersShown(EditorData.isLineNumbersShown(editor));
  }

  private void updateLineNumbers(@NotNull Editor editor) {
    if (!EditorData.isFileEditor(editor)) {
      return;
    }

    final Options options = Options.getInstance();
    final boolean relativeLineNumber = options.isSet(Options.RELATIVE_NUMBER);
    final boolean lineNumber = options.isSet(Options.NUMBER);

    final EditorSettings settings = editor.getSettings();
    final boolean showEditorLineNumbers = (EditorData.isLineNumbersShown(editor) || lineNumber) && !relativeLineNumber;

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
      gutter.registerTextAnnotation(myLineNumbersGutterProvider);
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
    if (isKeyRepeat != null) {
      final Element editor = new Element("editor");
      element.addContent(editor);
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

  private static class LineNumbersGutterProvider implements TextAnnotationGutterProvider {
    @Nullable
    @Override
    public String getLineText(int line, @NotNull Editor editor) {
      if (VimPlugin.isEnabled() && EditorData.isFileEditor(editor)) {
        final Options options = Options.getInstance();
        final boolean relativeLineNumber = options.isSet(Options.RELATIVE_NUMBER);
        final boolean lineNumber = options.isSet(Options.NUMBER);
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
