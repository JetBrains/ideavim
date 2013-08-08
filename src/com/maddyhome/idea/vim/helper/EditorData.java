/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.VisualChange;
import com.maddyhome.idea.vim.command.VisualRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
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
  public static void uninitializeEditor(@NotNull Editor editor) {
    if (logger.isDebugEnabled()) logger.debug("editor closed: " + editor);
    editor.putUserData(COMMAND_STATE, null);
    editor.putUserData(LAST_HIGHLIGHTS, null);
    editor.putUserData(VISUAL, null);
    editor.putUserData(VISUAL_OP, null);
  }

  /**
   * This gets the last column the cursor was in for the editor.
   *
   * @param editor The editr to get the last column from
   * @return Returns the last column as set by {@link #setLastColumn} or the current cursor column
   */
  public static int getLastColumn(@NotNull Editor editor) {
    Integer col = editor.getUserData(LAST_COLUMN);
    if (col == null) {
      return editor.getCaretModel().getVisualPosition().column;
    }
    else {
      return col;
    }
  }

  /**
   * Sets the last column for this editor
   *
   * @param col    The column
   * @param editor The editor
   */
  public static void setLastColumn(@NotNull Editor editor, int col) {
    editor.putUserData(LAST_COLUMN, col);
    int t = getLastColumn(editor);
    if (logger.isDebugEnabled()) logger.debug("setLastColumn(" + col + ") is now " + t);
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

  /**
   * Gets the previous visual range for the editor.
   *
   * @param editor The editor to get the range for
   * @return The last visual range, null if no previous range
   */
  @Nullable
  public static VisualRange getLastVisualRange(@NotNull Editor editor) {
    return editor.getDocument().getUserData(VISUAL);
  }

  /**
   * Sets the previous visual range for the editor.
   *
   * @param editor The editor to set the range for
   * @param range  The visual range
   */
  public static void setLastVisualRange(@NotNull Editor editor, VisualRange range) {
    editor.getDocument().putUserData(VISUAL, range);
  }

  /**
   * Gets the previous visual operator range for the editor.
   *
   * @param editor The editor to get the range for
   * @return The last visual range, null if no previous range
   */
  @Nullable
  public static VisualChange getLastVisualOperatorRange(@NotNull Editor editor) {
    return editor.getDocument().getUserData(VISUAL_OP);
  }

  /**
   * Sets the previous visual operator range for the editor.
   *
   * @param editor The editor to set the range for
   * @param range  The visual range
   */
  public static void setLastVisualOperatorRange(@NotNull Editor editor, VisualChange range) {
    editor.getDocument().putUserData(VISUAL_OP, range);
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

  public static boolean isConsoleOutput(@NotNull Editor editor) {
    Object res = editor.getUserData(CONSOLE_VIEW_IN_EDITOR_VIEW);
    logger.debug("isConsoleOutput for editor " + editor + " - " + res);
    return res != null;
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
   * This is a static helper - no instances needed
   */
  private EditorData() {
  }

  private static final Key<Integer> LAST_COLUMN = new Key<Integer>("lastColumn");
  private static final Key<VisualRange> VISUAL = new Key<VisualRange>("lastVisual");
  private static final Key<VisualChange> VISUAL_OP = new Key<VisualChange>("lastVisualOp");
  private static final Key<String> LAST_SEARCH = new Key<String>("lastSearch");
  private static final Key<Collection<RangeHighlighter>> LAST_HIGHLIGHTS = new Key<Collection<RangeHighlighter>>("lastHighlights");
  private static final Key<CommandState> COMMAND_STATE = new Key<CommandState>("commandState");
  private static final Key<Boolean> CHANGE_GROUP = new Key<Boolean>("changeGroup");
  private static final Key<Boolean> MOTION_GROUP = new Key<Boolean>("motionGroup");
  private static Key CONSOLE_VIEW_IN_EDITOR_VIEW = Key.create("CONSOLE_VIEW_IN_EDITOR_VIEW");

  private static Logger logger = Logger.getInstance(EditorData.class.getName());

  static {
    try {
      // Yikes! The console output pane is really an editor. I need to be able to differentiate this editor
      // from other editors. After an email with Jetbrains I learned that the class to look at was ConsoleViewImpl
      // This class creates the editor and adds itself as user data to the editor. Unfortunately the Key used
      // for the user data is a private static in the class. And the name is obfuscated (why should this be easy).
      // This code looks at all the fields in ConsoleViewImpl and looks for a field of type Key. It is assumed
      // the first Key is the one I need. The key is created as:
      // private static final Key c = Key.create("CONSOLE_VIEW_IN_EDITOR_VIEW");
      // I tried to create a Key with the same name but it doesn't work. Most likely the Key implementation is
      // coded such that two keys with the same name are treated as different keys - oh well.
      // This code will work as long as the key I need is the first one in the ConsoleViewImpl.
      Class cvi = Class.forName("com.intellij.execution.impl.ConsoleViewImpl");
      Field[] flds = cvi.getDeclaredFields();
      for (Field f : flds) {
        if (f.getType().equals(Key.class)) {
          f.setAccessible(true);
          Key key = (Key)f.get(null);
          CONSOLE_VIEW_IN_EDITOR_VIEW = key;
          break;
        }
      }
    }
    catch (ClassNotFoundException e) {
      logger.error("ConsoleViewImpl not found");
    }
    catch (IllegalAccessException e) {
      logger.error("Can't access field 'c'");
    }
  }

  /**
   * Checks if editor is file editor, also it takes into account that editor can be placed in editors hierarhy
   */
  public static boolean isFileEditor(@NotNull Editor editor){
    final VirtualFile virtualFile = EditorData.getVirtualFile(editor);
    return virtualFile != null && !(virtualFile instanceof LightVirtualFile);
  }
}
