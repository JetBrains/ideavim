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

package com.maddyhome.idea.vim.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent.KeyBinding;
import java.awt.event.KeyEvent;

/**
 *
 */
public class ExKeyBindings {
  @NotNull
  public static KeyBinding[] getBindings() {
    return bindings;
  }

  // TODO - add the following keys:
  // Ctrl-\ Ctrl-N - abort
  static final KeyBinding[] bindings = new KeyBinding[]{
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ExEditorKit.EscapeChar),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, KeyEvent.CTRL_MASK), ExEditorKit.EscapeChar),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK), ExEditorKit.CancelEntry),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ExEditorKit.CompleteEntry),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_J, KeyEvent.CTRL_MASK), ExEditorKit.CompleteEntry),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK), ExEditorKit.CompleteEntry),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK), ExEditorKit.beginLineAction),
    new KeyBinding(KeyStroke.getKeyStroke((char)0x02, KeyEvent.CTRL_MASK), ExEditorKit.beginLineAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), ExEditorKit.beginLineAction),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK), ExEditorKit.endLineAction),
    new KeyBinding(KeyStroke.getKeyStroke((char)0x05, KeyEvent.CTRL_MASK), ExEditorKit.endLineAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), ExEditorKit.endLineAction),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), ExEditorKit.DeletePreviousChar),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK), ExEditorKit.DeletePreviousChar),
    new KeyBinding(KeyStroke.getKeyStroke((char)0x08, KeyEvent.CTRL_MASK), ExEditorKit.DeletePreviousChar),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), ExEditorKit.deleteNextCharAction),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK), ExEditorKit.DeletePreviousWord),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK), ExEditorKit.DeleteToCursor),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), ExEditorKit.HistoryUpFilter),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK), ExEditorKit.HistoryUp),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), ExEditorKit.HistoryUp),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), ExEditorKit.HistoryDownFilter),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK), ExEditorKit.HistoryDown),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), ExEditorKit.HistoryDown),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), ExEditorKit.ToggleInsertReplace),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), ExEditorKit.backwardAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK), ExEditorKit.previousWordAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK), ExEditorKit.previousWordAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), ExEditorKit.forwardAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK), ExEditorKit.nextWordAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK), ExEditorKit.nextWordAction),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK), ExEditorKit.StartDigraph),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), ExEditorKit.StartDigraph),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK), ExEditorKit.StartDigraph),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_MASK), ExEditorKit.pasteAction),
    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_MASK), ExEditorKit.pasteAction),

    new KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK), ExEditorKit.InsertRegister),
  };
}
