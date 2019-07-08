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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.common.Register;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Used to handle playback of macros
 */
public class MacroGroup {
  /**
   * This method is used to play the macro of keystrokes stored in the specified registers.
   *
   * @param editor  The editor to play the macro in
   * @param context The data context
   * @param project The project
   * @param reg     The register to get the macro from
   * @param count   The number of times to execute the macro
   * @return true if able to play the macro, false if invalid or empty register
   */
  public boolean playbackRegister(@NotNull Editor editor, @NotNull DataContext context, @Nullable Project project, char reg, int count) {
    if (logger.isDebugEnabled()) {
      logger.debug("play bakc register " + reg + " " + count + " times");
    }
    Register register = VimPlugin.getRegister().getPlaybackRegister(reg);
    if (register == null) {
      return false;
    }

    List<KeyStroke> keys = register.getKeys();
    playbackKeys(editor, context, project, keys, 0, 0, count);

    lastRegister = reg;

    return true;
  }

  /**
   * This plays back the last register that was executed, if any.
   *
   * @param editor  The editr to play the macro in
   * @param context The data context
   * @param project The project
   * @param count   The number of times to execute the macro
   * @return true if able to play the macro, false in no previous playback
   */
  public boolean playbackLastRegister(@NotNull Editor editor, @NotNull DataContext context, @Nullable Project project, int count) {
    return lastRegister != 0 && playbackRegister(editor, context, project, lastRegister, count);
  }

  /**
   * This puts a single keystroke at the end of the event queue for playback
   *
   * @param editor  The editor to play the key in
   * @param context The data context
   * @param project The project
   * @param keys    The list of keys to playback
   * @param pos     The position within the list for the specific key to queue
   * @param cnt     count
   * @param total   total
   */
  public void playbackKeys(@NotNull final Editor editor, @NotNull final DataContext context, @Nullable final Project project,
                           @NotNull final List<KeyStroke> keys, final int pos, final int cnt, final int total) {
    if (logger.isDebugEnabled()) {
      logger.debug("playbackKeys " + pos);
    }
    if (pos >= keys.size() || cnt >= total) {
      logger.debug("done");

      return;
    }

    // This took a while to get just right. The original approach has a loop that made a runnable for each
    // character. It worked except for one case - if the macro had a complete ex command, the editor did not
    // end up with the focus and I couldn't find anyway to get it to have focus. This approach was the only
    // solution. This makes the most sense now (of course it took hours of trial and error to come up with
    // this one). Each key gets added, one at a time, to the event queue. If a given key results in other
    // events getting queued, they get queued before the next key, just what would happen if the user was typing
    // the keys one at a time. With the old loop approach, all the keys got queued, then any events they caused
    // were queued - after the keys. This is what caused the problem.
    final Runnable run = () -> {
      if (logger.isDebugEnabled()) {
        logger.debug("processing key " + pos);
      }
      // Handle one keystroke then queue up the next key
      KeyHandler.getInstance().handleKey(editor, keys.get(pos), context);
      if (pos < keys.size() - 1) {
        playbackKeys(editor, context, project, keys, pos + 1, cnt, total);
      }
      else {
        playbackKeys(editor, context, project, keys, 0, cnt + 1, total);
      }
    };

    ApplicationManager.getApplication().invokeLater(
        () -> CommandProcessor.getInstance().executeCommand(project, run, "Vim Macro Playback", keys.get(pos)));
  }

  public void postKey(@NotNull KeyStroke stroke, @NotNull Editor editor) {
    final Component component = SwingUtilities.getAncestorOfClass(Window.class, editor.getComponent());
    final KeyEvent event = createKeyEvent(stroke, component);
    ApplicationManager.getApplication().invokeLater(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("posting " + event);
      }
      Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(event);
    });
  }

  @NotNull
  private KeyEvent createKeyEvent(@NotNull KeyStroke stroke, Component component) {
    return new KeyEvent(component,
                        stroke.getKeyChar() == KeyEvent.CHAR_UNDEFINED ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_TYPED,
                        System.currentTimeMillis(), stroke.getModifiers(), stroke.getKeyCode(), stroke.getKeyChar());
  }

  private char lastRegister = 0;
  private static final Logger logger = Logger.getInstance(MacroGroup.class.getName());
}
