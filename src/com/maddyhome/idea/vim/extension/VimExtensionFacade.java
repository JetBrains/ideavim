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

package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.helper.EditorDataContext;
import com.maddyhome.idea.vim.helper.PlugInputModel;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.helper.TestInputModel;
import com.maddyhome.idea.vim.key.OperatorFunction;
import com.maddyhome.idea.vim.ui.ModalEntryDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Vim API facade that defines functions similar to the built-in functions and statements of the original Vim.
 *
 * See :help eval.
 *
 * @author vlan
 */
public class VimExtensionFacade {
  private VimExtensionFacade() {}

  /**
   * The 'map' command for mapping keys to handlers defined in extensions.
   */
  public static void putExtensionHandlerMapping(@NotNull Set<MappingMode> modes, @NotNull List<KeyStroke> fromKeys,
                                                @NotNull VimExtensionHandler extensionHandler, boolean recursive) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, null, extensionHandler, recursive);
  }

  /**
   * The 'map' command for mapping keys to other keys.
   */
  public static void putKeyMapping(@NotNull Set<MappingMode> modes, @NotNull List<KeyStroke> fromKeys,
                                   @NotNull List<KeyStroke> toKeys, boolean recursive) {
    VimPlugin.getKey().putKeyMapping(modes, fromKeys, toKeys, null, recursive);
  }

  /**
   * Sets the value of 'operatorfunc' to be used as the operator function in 'g@'.
   */
  public static void setOperatorFunction(@NotNull OperatorFunction function) {
    VimPlugin.getKey().setOperatorFunction(function);
  }

  /**
   * Runs normal mode commands similar to ':normal {commands}'.
   *
   * XXX: Currently it doesn't make the editor enter the normal mode, it doesn't recover from incomplete commands, it
   * leaves the editor in the insert mode if it's been activated.
   */
  public static void executeNormal(@NotNull List<KeyStroke> keys, @NotNull Editor editor) {
    final EditorDataContext context = new EditorDataContext(editor);
    for (KeyStroke key : keys) {
      KeyHandler.getInstance().handleKey(editor, key, context);
    }
  }

  /**
   * Returns a single key stroke from the user input similar to 'getchar()'.
   */
  @NotNull
  public static KeyStroke inputKeyStroke(@NotNull Editor editor) {
    final KeyStroke key;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      key = TestInputModel.getInstance(editor).nextKeyStroke();
    }
    else if (PlugInputModel.getInstance(editor).hasPendingKeyStroke()) {
      key = PlugInputModel.getInstance(editor).nextKeyStroke();
    } else {
      final Ref<KeyStroke> ref = Ref.create();
      final ModalEntryDialog dialog = new ModalEntryDialog(editor, "");
      dialog.setEntryKeyListener(new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
          ref.set(KeyStroke.getKeyStrokeForEvent(e));
          dialog.dispose();
        }
      });
      dialog.setVisible(true);
      key = ref.get();
    }
    return key != null ? key : KeyStroke.getKeyStroke((char)KeyEvent.VK_ESCAPE);
  }

  /**
   * Returns a string typed in the input box similar to 'input()'.
   */
  @NotNull
  public static String inputString(@NotNull Editor editor, @NotNull String prompt) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final StringBuilder builder = new StringBuilder();
      final TestInputModel inputModel = TestInputModel.getInstance(editor);
      for (KeyStroke key = inputModel.nextKeyStroke();
           key != null && !StringHelper.isCloseKeyStroke(key) && key.getKeyCode() != KeyEvent.VK_ENTER;
           key = inputModel.nextKeyStroke()) {
        final char c = key.getKeyChar();
        if (c != KeyEvent.CHAR_UNDEFINED) {
          builder.append(c);
        }
      }
      return builder.toString();
    }
    else {
      final StringBuilder builder = new StringBuilder();
      final PlugInputModel inputModel = PlugInputModel.getInstance(editor);
      while (inputModel.hasPendingKeyStroke()) {
        KeyStroke key = inputModel.nextKeyStroke();
        if (key == null || StringHelper.isCloseKeyStroke(key)) {
          return ""; // canceled
        }
        else if (key.getKeyCode() == KeyEvent.VK_ENTER) {
          return builder.toString();
        }
        else {
          final char c = key.getKeyChar();
          if (c != KeyEvent.CHAR_UNDEFINED) {
            builder.append(c);
          }
        }
      }

      final Ref<Boolean> canceled = Ref.create(false);
      final ModalEntryDialog dialog = new ModalEntryDialog(editor, prompt);
      dialog.setEntryKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          final KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
          if (StringHelper.isCloseKeyStroke(key)) {
            canceled.set(true);
            dialog.dispose();
          }
          else if (key.getKeyCode() == KeyEvent.VK_ENTER) {
            dialog.dispose();
          }
        }
      });
      dialog.setVisible(true);
      return canceled.get() ? "" : builder.toString() + dialog.getText();
    }
  }

  /**
   * Get the current contents of the given register similar to 'getreg()'.
   */
  @Nullable
  public static List<KeyStroke> getRegister(char register) {
    final Register reg = VimPlugin.getRegister().getRegister(register);
    if (reg == null) {
      return null;
    }
    return reg.getKeys();
  }

  /**
   * Set the current contents of the given register
   */
  public static void setRegister(char register, @Nullable List<KeyStroke> keys) {
    VimPlugin.getRegister().setKeys(register, keys != null ? keys : Collections.<KeyStroke>emptyList());
  }
}
