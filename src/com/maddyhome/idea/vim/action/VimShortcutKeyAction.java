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

package com.maddyhome.idea.vim.action;

import com.google.common.collect.ImmutableSet;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.DumbAware;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
  @NotNull public static Set<KeyStroke> VIM_ONLY_EDITOR_KEYS = ImmutableSet.<KeyStroke>builder()
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
    .addAll(getKeyStrokes(VK_PAGE_UP, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .addAll(getKeyStrokes(VK_PAGE_DOWN, 0, CTRL_MASK, SHIFT_MASK, CTRL_MASK | SHIFT_MASK))
    .build();

  private static List<KeyStroke> getKeyStrokes(int keyCode, int... modifiers) {
    final List<KeyStroke> keyStrokes = new ArrayList<KeyStroke>();
    for (int modifier : modifiers) {
      keyStrokes.add(KeyStroke.getKeyStroke(keyCode, modifier));
    }
    return keyStrokes;
  }

  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = getEditor(e);
    final KeyStroke keyStroke = getKeyStroke(e);
    if (editor != null && keyStroke != null) {
      // Should we use InjectedLanguageUtil.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      KeyHandler.getInstance().handleKey(editor, keyStroke, e.getDataContext());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  private boolean isEnabled(@NotNull AnActionEvent e) {
    if (VimPlugin.isEnabled()) {
      final Editor editor = getEditor(e);
      final KeyStroke keyStroke = getKeyStroke(e);
      if (editor != null && keyStroke != null) {
        final int keyCode = keyStroke.getKeyCode();
        final Map<KeyStroke, ShortcutOwner> shortcutConflicts = VimPlugin.getShortcutConflicts();
        if (LookupManager.getActiveLookup(editor) != null) {
          return keyCode == VK_ESCAPE;
        }
        // Debug watch, Python console, etc.
        else if (!EditorData.isFileEditor(editor) && CommandState.inInsertMode(editor)) {
          return keyCode != VK_ENTER && keyCode != VK_ESCAPE && keyCode != VK_TAB;
        }
        else if (VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
          return true;
        }
        else if (shortcutConflicts.containsKey(keyStroke)) {
          return shortcutConflicts.get(keyStroke) == ShortcutOwner.VIM;
        }
        else {
          final List<AnAction> actions = getKeymapConflicts(keyStroke);
          if (!actions.isEmpty()) {
            // TODO: Show a balloon that allows binding the shortcut to Vim or IDE
            shortcutConflicts.put(keyStroke, ShortcutOwner.IDE);
            return false;
          }
          return true;
        }
      }
    }
    return false;
  }

  @NotNull
  private List<AnAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    final Keymap keymap = keymapManager.getActiveKeymap();
    final KeyboardShortcut shortcut = new KeyboardShortcut(keyStroke, null);
    final Map<String, ArrayList<KeyboardShortcut>> conflicts = keymap.getConflicts("", shortcut);
    final List<AnAction> actions = new ArrayList<AnAction>();
    for (String actionId : conflicts.keySet()) {
      final AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
      if (action != null) {
        actions.add(action);
      }
    }
    return actions;
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
