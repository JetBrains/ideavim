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

package com.maddyhome.idea.vim.action;

import com.google.common.collect.ImmutableSet;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.change.insert.InsertExitModeAction;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.awt.event.KeyEvent.*;

/**
 * Handles Vim keys that are treated as action shortcuts by the IDE.
 *
 * These keys are not passed to {@link com.maddyhome.idea.vim.VimTypedActionHandler} and should be handled by actions.
 */
public class VimShortcutKeyAction extends AnAction implements DumbAware {
  private static final String ACTION_ID = "VimShortcutKeyAction";

  @NotNull public static final Set<KeyStroke> VIM_ONLY_EDITOR_KEYS = ImmutableSet.<KeyStroke>builder()
    .addAll(getKeyStrokes(VK_ENTER, 0))
    .addAll(getKeyStrokes(VK_ESCAPE, 0))
    .addAll(getKeyStrokes(VK_TAB, 0))
    .addAll(getKeyStrokes(VK_BACK_SPACE, 0, CTRL_MASK))
    .addAll(getKeyStrokes(VK_INSERT, 0))
    .addAll(getKeyStrokes(VK_DELETE, 0, CTRL_MASK))
    .addAll(getKeyStrokes(VK_UP, 0, CTRL_MASK, SHIFT_MASK))
    .addAll(getKeyStrokes(VK_DOWN, 0, CTRL_MASK, SHIFT_MASK))
    .addAll(getKeyStrokes(VK_LEFT, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_RIGHT, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_HOME, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_END, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_PAGE_UP, 0, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_PAGE_DOWN, 0, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .build();

  @NotNull private static final Set<KeyStroke> NON_FILE_EDITOR_KEYS = ImmutableSet.<KeyStroke>builder()
    .addAll(getKeyStrokes(VK_ENTER, 0))
    .addAll(getKeyStrokes(VK_ESCAPE, 0))
    .addAll(getKeyStrokes(VK_TAB, 0))
    .addAll(getKeyStrokes(VK_UP, 0))
    .addAll(getKeyStrokes(VK_DOWN, 0))
    .build();

  private static final Logger ourLogger = Logger.getInstance(VimShortcutKeyAction.class.getName());
  private static AnAction ourInstance = null;

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = getEditor(e);
    final KeyStroke keyStroke = getKeyStroke(e);
    if (editor != null && keyStroke != null) {
      final ShortcutOwner owner = VimPlugin.getKey().getSavedShortcutConflicts().get(keyStroke);
      if (owner == ShortcutOwner.UNDEFINED) {
        notifyAboutShortcutConflict(keyStroke);
      }
      // Should we use InjectedLanguageUtil.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      try {
        KeyHandler.getInstance().handleKey(editor, keyStroke, new EditorDataContext(editor));
      }
      catch (Throwable throwable) {
        ourLogger.error(throwable);
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  @NotNull
  public static AnAction getInstance() {
    if (ourInstance == null) {
      final AnAction originalAction = ActionManager.getInstance().getAction(ACTION_ID);
      ourInstance = EmptyAction.wrap(originalAction);
    }
    return ourInstance;
  }

  private void notifyAboutShortcutConflict(@NotNull final KeyStroke keyStroke) {
    VimPlugin.getKey().getSavedShortcutConflicts().put(keyStroke, ShortcutOwner.VIM);
    final String message = String.format(
      "Using the <b>%s</b> shortcut for Vim emulation.<br/>" +
      "You can redefine it as an <a href='#ide'>IDE shortcut</a> or " +
      "configure its handler in <a href='#settings'>Vim Emulation</a> settings.",
      KeymapUtil.getShortcutText(new KeyboardShortcut(keyStroke, null)));
    final NotificationListener listener = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        final String description = e.getDescription();
        if ("#ide".equals(description)) {
          VimPlugin.getKey().getSavedShortcutConflicts().put(keyStroke, ShortcutOwner.IDE);
          notification.expire();
        }
        else if ("#settings".equals(description)) {
          ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
        }
      }
    };
    final Notification notification = new Notification(VimPlugin.IDEAVIM_NOTIFICATION_ID,
                                                       VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
                                                       message,
                                                       NotificationType.INFORMATION,
                                                       listener);
    notification.notify(null);
  }

  private boolean isEnabled(@NotNull AnActionEvent e) {
    if (VimPlugin.isEnabled()) {
      final Editor editor = getEditor(e);
      final KeyStroke keyStroke = getKeyStroke(e);
      if (editor != null && keyStroke != null) {
        final int keyCode = keyStroke.getKeyCode();
        if (LookupManager.getActiveLookup(editor) != null) {
          return isExitInsertMode(keyStroke);
        }
        if (keyCode == VK_ESCAPE) {
          return isEnabledForEscape(editor);
        }
        if (CommandState.inInsertMode(editor)) {
          // XXX: <Tab> won't be recorded in macros
          if (keyCode == VK_TAB) {
            return false;
          }
          // Debug watch, Python console, etc.
          if (NON_FILE_EDITOR_KEYS.contains(keyStroke) && !EditorData.isFileEditor(editor)) {
            return false;
          }
        }
        if (VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
          return true;
        }
        final Map<KeyStroke, ShortcutOwner> savedShortcutConflicts = VimPlugin.getKey().getSavedShortcutConflicts();
        final ShortcutOwner owner = savedShortcutConflicts.get(keyStroke);
        if (owner == ShortcutOwner.VIM) {
          return true;
        }
        else if (owner == ShortcutOwner.IDE) {
          return !isShortcutConflict(keyStroke);
        }
        else {
          if (isShortcutConflict(keyStroke)) {
            savedShortcutConflicts.put(keyStroke, ShortcutOwner.UNDEFINED);
          }
          return true;
        }
      }
    }
    return false;
  }

  private boolean isEnabledForEscape(@NotNull Editor editor) {
    final CommandState.Mode mode = CommandState.getInstance(editor).getMode();
    return isPrimaryEditor(editor) || (EditorData.isFileEditor(editor) && mode != CommandState.Mode.COMMAND);
  }

  /**
   * Checks if the editor is a primary editor in the main editing area.
   */
  private boolean isPrimaryEditor(@NotNull Editor editor) {
    final Project project = editor.getProject();
    if (project == null) return false;
    final FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
    return StreamEx.of(fileEditorManager.getAllEditors())
      .anyMatch(fileEditor -> editor.equals(EditorUtil.getEditorEx(fileEditor)));
  }

  private boolean isExitInsertMode(@NotNull KeyStroke keyStroke) {
    for (List<KeyStroke> keys : InsertExitModeAction.getInstance().getKeyStrokesSet()) {
      // XXX: Currently we cannot handle <C-\><C-N> because of the importance of <C-N> for the IDE on Linux
      if (keys.size() == 1 && keyStroke.equals(keys.get(0))) {
        return true;
      }
    }
    return false;
  }

  private boolean isShortcutConflict(@NotNull KeyStroke keyStroke) {
    return !VimPlugin.getKey().getKeymapConflicts(keyStroke).isEmpty();
  }

  @NotNull
  private static List<KeyStroke> getKeyStrokes(int keyCode, @NotNull int... modifiers) {
    final List<KeyStroke> keyStrokes = new ArrayList<KeyStroke>();
    for (int modifier : modifiers) {
      keyStrokes.add(KeyStroke.getKeyStroke(keyCode, modifier));
    }
    return keyStrokes;
  }

  @Nullable
  private KeyStroke getKeyStroke(@NotNull AnActionEvent e) {
    final InputEvent inputEvent = e.getInputEvent();
    if (inputEvent instanceof KeyEvent) {
      final KeyEvent keyEvent = (KeyEvent)inputEvent;
      return KeyStroke.getKeyStrokeForEvent(keyEvent);
    }
    return null;
  }

  @Nullable
  private Editor getEditor(@NotNull AnActionEvent e) {
    return e.getData(PlatformDataKeys.EDITOR);
  }
}
