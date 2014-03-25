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
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable;
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

  public void actionPerformed(@NotNull AnActionEvent e) {
    final Editor editor = getEditor(e);
    final KeyStroke keyStroke = getKeyStroke(e);
    if (editor != null && keyStroke != null) {
      final ShortcutOwner owner = VimPlugin.getSavedShortcutConflicts().get(keyStroke);
      if (owner == ShortcutOwner.UNDEFINED) {
        notifyAboutShortcutConflict(keyStroke);
      }
      // Should we use InjectedLanguageUtil.getTopLevelEditor(editor) here, as we did in former EditorKeyHandler?
      KeyHandler.getInstance().handleKey(editor, keyStroke, e.getDataContext());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  private void notifyAboutShortcutConflict(@NotNull final KeyStroke keyStroke) {
    VimPlugin.getSavedShortcutConflicts().put(keyStroke, ShortcutOwner.VIM);
    final String message = String.format(
      "Using the <b>%s</b> shortcut for Vim emulation.<br/>" +
      "You can change its handler in <a href='#settings'>Vim Emulation</a> settings.",
      KeymapUtil.getShortcutText(new KeyboardShortcut(keyStroke, null)));
    final NotificationListener listener = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
        ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
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
          return keyCode == VK_ESCAPE;
        }
        // Debug watch, Python console, etc.
        else if (!EditorData.isFileEditor(editor) && CommandState.inInsertMode(editor)) {
          return keyCode != VK_ENTER && keyCode != VK_ESCAPE && keyCode != VK_TAB;
        }
        else if (VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
          return true;
        }
        final Map<KeyStroke, ShortcutOwner> savedShortcutConflicts = VimPlugin.getSavedShortcutConflicts();
        final ShortcutOwner owner = savedShortcutConflicts.get(keyStroke);
        if (owner == ShortcutOwner.VIM) {
          return true;
        }
        else if (owner == ShortcutOwner.IDE) {
          return false;
        }
        else {
          final List<AnAction> actions = KeyParser.getKeymapConflicts(keyStroke);
          if (!actions.isEmpty()) {
            savedShortcutConflicts.put(keyStroke, ShortcutOwner.UNDEFINED);
          }
          return true;
        }
      }
    }
    return false;
  }

  private static List<KeyStroke> getKeyStrokes(int keyCode, int... modifiers) {
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
