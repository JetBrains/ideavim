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

package com.maddyhome.idea.vim.ui;

import com.intellij.util.Processor;
import com.maddyhome.idea.vim.helper.StringHelper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * @author dhleong
 */
public final class ModalEntry {
  @Contract(pure = true)
  private ModalEntry() {}

  /**
   * Activates modal entry mode, passing all received key strokes to the processor until it returns false.
   */
  public static void activate(@NotNull final Processor<KeyStroke> processor) {
    final EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    final SecondaryLoop loop = systemQueue.createSecondaryLoop();

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent e) {
        final KeyStroke stroke;
        if (e.getID() == KeyEvent.KEY_RELEASED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e);
          if (!StringHelper.isCloseKeyStroke(stroke) && stroke.getKeyCode() != KeyEvent.VK_ENTER) {
            return true;
          }
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
          stroke = KeyStroke.getKeyStrokeForEvent(e);
        } else {
          return true;
        }
        if (!processor.process(stroke)) {
          KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
          loop.exit();
        }
        return true;
      }
    });

    loop.enter();
  }
}
