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

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.ui.ExOutputPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This class is used to manipulate editor specific data. Each editor has a user defined map associated with it.
 * These methods provide convenient methods for working with that Vim Plugin specific data.
 */
public class EditorData {

  /**
   * This is used to initialize each new editor that gets created.
   *
   * @param editor The editor to initialize
   */
  public static void initializeEditor(Editor editor) {
    if (logger.isDebugEnabled()) logger.debug("editor created: " + editor);
  }

  /**
   * This is used to clean up editors whenever they are closed.
   *
   * @param editor The editor to cleanup
   */
  public static void unInitializeEditor(@NotNull Editor editor) {
    if (logger.isDebugEnabled()) logger.debug("editor closed: " + editor);
    editor.putUserData(COMMAND_STATE, null);
    editor.putUserData(LAST_HIGHLIGHTS, null);
    editor.putUserData(LAST_SELECTION_TYPE, null);
    editor.putUserData(LAST_VISUAL_RANGE, null);
    editor.putUserData(MORE_PANEL, null);
    editor.putUserData(EX_OUTPUT_MODEL, null);
  }

  /**
   * This gets the last column the primary cursor was in for the editor.
   *
   * @param editor The editor to get the last column from
   * @return Returns the last column as set by {@link #setLastColumn} or the current primary cursor column
   * @deprecated Use {@link CaretData#getLastColumn(Caret)} to get the primary cursor last column
   */
  public static int getLastColumn(@NotNull Editor editor) {
    return CaretData.getLastColumn(editor.getCaretModel().getPrimaryCaret());
  }

  /**
   * Sets the last column for the primary caret of this editor
   *
   * @param col    The column
   * @param editor The editor
   * @deprecated Use {@link CaretData#setLastColumn(Editor, Caret, int)} to set the caret last column
   */
  public static void setLastColumn(@NotNull Editor editor, int col) {
    CaretData.setLastColumn(editor, editor.getCaretModel().getPrimaryCaret(), col);
  }

  @Nullable
  public static String getLastSearch(@NotNull Editor editor) {
    return editor.getUserData(LAST_SEARCH);
  }

  public static void setLastSearch(@NotNull Editor editor, String search) {
    editor.putUserData(LAST_SEARCH, search);
  }

  @Nullable
  public static Collection<RangeHighlighter> getLastHighlights(@NotNull Editor editor) {
    return editor.getUserData(LAST_HIGHLIGHTS);
  }

  public static void setLastHighlights(@NotNull Editor editor, Collection<RangeHighlighter> highlights) {
    editor.putUserData(LAST_HIGHLIGHTS, highlights);
  }

  /***
   * @see :help visualmode()
   */
  @Nullable
  public static SelectionType getLastSelectionType(@NotNull Editor editor) {
    return editor.getDocument().getUserData(LAST_SELECTION_TYPE);
  }

  public static void setLastSelectionType(@NotNull Editor editor, @NotNull SelectionType selectionType) {
    editor.getDocument().putUserData(LAST_SELECTION_TYPE, selectionType);
  }

  @Nullable
  public static TextRange getLastVisualRange(@NotNull Editor editor) {
    return editor.getDocument().getUserData(LAST_VISUAL_RANGE);
  }

  public static void setLastVisualRange(@NotNull Editor editor, @NotNull TextRange textRange) {
    editor.getDocument().putUserData(LAST_VISUAL_RANGE, textRange);
  }

  @Nullable
  public static CommandState getCommandState(@NotNull Editor editor) {
    return editor.getUserData(COMMAND_STATE);
  }

  public static void setCommandState(@NotNull Editor editor, CommandState state) {
    editor.putUserData(COMMAND_STATE, state);
  }

  public static boolean getChangeGroup(@NotNull Editor editor) {
    Boolean res = editor.getUserData(CHANGE_GROUP);
    if (res != null) {
      return res;
    }
    else {
      return false;
    }
  }

  public static void setChangeGroup(@NotNull Editor editor, boolean adapter) {
    editor.putUserData(CHANGE_GROUP, adapter);
  }

  public static boolean getMotionGroup(@NotNull Editor editor) {
    return editor.getUserData(MOTION_GROUP) == Boolean.TRUE;
  }

  public static void setMotionGroup(@NotNull Editor editor, boolean adapter) {
    editor.putUserData(MOTION_GROUP, adapter);
  }

  public static boolean getEditorGroup(@NotNull Editor editor) {
    return editor.getUserData(EDITOR_GROUP) == Boolean.TRUE;
  }

  public static void setEditorGroup(@NotNull Editor editor, boolean value) {
    editor.putUserData(EDITOR_GROUP, value);
  }

  public static boolean isLineNumbersShown(@NotNull Editor editor) {
    return editor.getUserData(LINE_NUMBERS_SHOWN) == Boolean.TRUE;
  }

  public static void setLineNumbersShown(@NotNull Editor editor, boolean value) {
    editor.putUserData(LINE_NUMBERS_SHOWN, value);
  }

  @Nullable
  public static ExOutputPanel getMorePanel(@NotNull Editor editor) {
    return editor.getUserData(MORE_PANEL);
  }

  public static void setMorePanel(@NotNull Editor editor, @NotNull ExOutputPanel panel) {
    editor.putUserData(MORE_PANEL, panel);
  }

  @Nullable
  public static ExOutputModel getExOutputModel(@NotNull Editor editor) {
    return editor.getUserData(EX_OUTPUT_MODEL);
  }

  public static void setExOutputModel(@NotNull Editor editor, @NotNull ExOutputModel model) {
    editor.putUserData(EX_OUTPUT_MODEL, model);
  }

  /**
   * Sets the visual block start for the editor.
   *
   * @param editor The editor
   */
  public static void setVisualBlockStart(@NotNull Editor editor, int visualBlockStart) {
    editor.putUserData(VISUAL_BLOCK_START, visualBlockStart);
  }

  /**
   * Gets the visual block start for the editor.
   *
   * @param editor The editor
   */
  public static int getVisualBlockStart(@NotNull Editor editor) {
    Integer visualBlockStart = editor.getUserData(VISUAL_BLOCK_START);

    if (visualBlockStart == null) {
      return editor.getCaretModel().getPrimaryCaret().getOffset();
    }
    else {
      return visualBlockStart;
    }
  }

  /**
   * Sets the visual block end for the editor.
   *
   * @param editor The editor
   */
  public static void setVisualBlockEnd(@NotNull Editor editor, int visualBlockEnd) {
    editor.putUserData(VISUAL_BLOCK_END, visualBlockEnd);
  }

  /**
   * Gets the visual block offset for the editor.
   *
   * @param editor The editor
   */
  public static int getVisualBlockOffset(@NotNull Editor editor) {
    Integer visualBlockOffset = editor.getUserData(VISUAL_BLOCK_OFFSET);

    if (visualBlockOffset == null) {
      return editor.getCaretModel().getPrimaryCaret().getOffset();
    }
    else {
      return visualBlockOffset;
    }
  }

  /**
   * Sets the visual block offset for the editor.
   *
   * @param editor The editor
   */
  public static void setVisualBlockOffset(@NotNull Editor editor, int visualBlockOffset) {
    editor.putUserData(VISUAL_BLOCK_OFFSET, visualBlockOffset);
  }

  /**
   * Gets the visual block end for the editor.
   *
   * @param editor The editor
   */
  public static int getVisualBlockEnd(@NotNull Editor editor) {
    Integer visualBlockEnd = editor.getUserData(VISUAL_BLOCK_END);

    if (visualBlockEnd == null) {
      return editor.getCaretModel().getPrimaryCaret().getOffset();
    }
    else {
      return visualBlockEnd;
    }
  }

  /**
   * Gets the virtual file associated with this editor
   *
   * @param editor The editor
   * @return The virtual file for the editor
   */
  @Nullable
  public static VirtualFile getVirtualFile(@NotNull Editor editor) {
    return FileDocumentManager.getInstance().getFile(editor.getDocument());
  }

  /**
   * Asks whether next down move should be ignored.
   */
  public static boolean shouldIgnoreNextMove(@NotNull Editor editor) {
    Boolean ret = editor.getUserData(IGNORE_NEXT_MOVE);
    if (ret == null) {
      return false;
    }
    else {
      return ret;
    }
  }

  /**
   * Indicate that the next down move should be ignored.
   */
  public static void ignoreNextMove(@NotNull Editor editor) {
    editor.putUserData(IGNORE_NEXT_MOVE, true);
  }

  /**
   * Indicate that the next down move should not be ignored.
   */
  public static void dontIgnoreNextMove(@NotNull Editor editor) {
    editor.putUserData(IGNORE_NEXT_MOVE, false);
  }

  /**
   * Checks whether a keeping visual mode visual operator action is performed on editor.
   */
  public static boolean isKeepingVisualOperatorAction(@NotNull Editor editor) {
    Boolean res = editor.getUserData(IS_KEEPING_VISUAL_OPERATOR_ACTION);

    if (res == null) {
      return false;
    }
    else {
      return res;
    }
  }

  /**
   * Sets the keeping visual mode visual operator action flag for the editor.
   */
  public static void setKeepingVisualOperatorAction(@NotNull Editor editor, boolean value) {
    editor.putUserData(IS_KEEPING_VISUAL_OPERATOR_ACTION, value);
  }

  /**
   * Gets the mode to which the editor should switch after a change/visual action.
   */
  @Nullable
  public static CommandState.Mode getChangeSwitchMode(@NotNull Editor editor) {
    return editor.getUserData(CHANGE_ACTION_SWITCH_MODE);
  }

  /**
   * Sets the mode to which the editor should switch after a change/visual action.
   */
  public static void setChangeSwitchMode(@NotNull Editor editor, @Nullable CommandState.Mode mode) {
    editor.putUserData(CHANGE_ACTION_SWITCH_MODE, mode);
  }

  /**
   * Sets the visual block mode flag in the beginning of handling visual operator actions
   */
  public static boolean wasVisualBlockMode(@NotNull Editor editor) {
    Boolean res = editor.getUserData(WAS_VISUAL_BLOCK_MODE);
    return res != null && res;
  }

  /**
   * Sets the visual block mode flag in the beginning of handling visual operator actions
   */
  public static void setWasVisualBlockMode(@NotNull Editor editor, boolean value) {
    editor.putUserData(WAS_VISUAL_BLOCK_MODE, value);
  }

  /**
   * Gets the last caret used in down movement.
   */
  @Nullable
  public static Caret getLastDownCaret(@NotNull Editor editor) {
    return editor.getUserData(LAST_DOWN_CARET);
  }

  /**
   * Sets the last caret used in down movement.
   */
  public static void setLastDownCaret(@NotNull Editor editor, @NotNull Caret caret) {
    editor.putUserData(LAST_DOWN_CARET, caret);
  }

  /**
   * This is a static helper - no instances needed
   */
  private EditorData() {
  }

  private static final Key<SelectionType> LAST_SELECTION_TYPE = new Key<SelectionType>("lastSelectionType");
  public static final Key<TextRange> LAST_VISUAL_RANGE = new Key<TextRange>("lastVisualRange");
  private static final Key<String> LAST_SEARCH = new Key<String>("lastSearch");
  private static final Key<Collection<RangeHighlighter>> LAST_HIGHLIGHTS =
    new Key<Collection<RangeHighlighter>>("lastHighlights");
  private static final Key<CommandState> COMMAND_STATE = new Key<CommandState>("commandState");
  private static final Key<Boolean> CHANGE_GROUP = new Key<Boolean>("changeGroup");
  private static final Key<Boolean> MOTION_GROUP = new Key<Boolean>("motionGroup");
  public static final Key<Boolean> EDITOR_GROUP = new Key<Boolean>("editorGroup");
  public static final Key<Boolean> LINE_NUMBERS_SHOWN = new Key<Boolean>("lineNumbersShown");
  private static final Key<ExOutputPanel> MORE_PANEL = new Key<ExOutputPanel>("IdeaVim.morePanel");
  private static final Key<ExOutputModel> EX_OUTPUT_MODEL = new Key<ExOutputModel>("IdeaVim.exOutputModel");
  private static final Key<TestInputModel> TEST_INPUT_MODEL = new Key<TestInputModel>("IdeaVim.testInputModel");
  private static final Key<Integer> VISUAL_BLOCK_START = new Key<>("visuaBlockStart");
  private static final Key<Integer> VISUAL_BLOCK_END = new Key<>("visualBlockEnd");
  private static final Key<Integer> VISUAL_BLOCK_OFFSET = new Key<>("visualBlockOffset");
  private static final Key<Boolean> IGNORE_NEXT_MOVE = new Key<>("shouldIgnoreNextMove");
  private static final Key<Boolean> IS_KEEPING_VISUAL_OPERATOR_ACTION = new Key<>("isKeepingVisualOperatorAction");
  private static final Key<CommandState.Mode> CHANGE_ACTION_SWITCH_MODE = new Key<>("changeActionSwitchMode");
  private static final Key<Boolean> WAS_VISUAL_BLOCK_MODE = new Key<>("wasVisualBlockMode");
  private static final  Key<Caret> LAST_DOWN_CARET = new Key<>("lastDownCaret");

  private static final Logger logger = Logger.getInstance(EditorData.class.getName());

  /**
   * Checks if editor is file editor, also it takes into account that editor can be placed in editors hierarchy
   */
  public static boolean isFileEditor(@NotNull Editor editor) {
    final VirtualFile virtualFile = EditorData.getVirtualFile(editor);
    return virtualFile != null && !(virtualFile instanceof LightVirtualFile);
  }

  @Nullable
  public static TestInputModel getTestInputModel(@NotNull Editor editor) {
    return editor.getUserData(TEST_INPUT_MODEL);
  }

  public static void setTestInputModel(@NotNull Editor editor, @NotNull TestInputModel model) {
    editor.putUserData(TEST_INPUT_MODEL, model);
  }
}
