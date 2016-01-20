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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author vlan
 */
public class UiHelper {
  private UiHelper() {
  }

  @NotNull
  public static Font getEditorFont() {
    final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    return new Font(scheme.getEditorFontName(), Font.PLAIN, scheme.getEditorFontSize());
  }

  /**
   * Get focus reliably.
   */
  public static void requestFocus(@NotNull final JComponent component) {
    final Application application = ApplicationManager.getApplication();
    // XXX: This workaround is required at least for Oracle Java 6
    application.invokeLater(new Runnable() {
      @Override
      public void run() {
        application.invokeLater(new Runnable() {
          @Override
          public void run() {
            component.requestFocus();
          }
        });
      }
    });
  }

  /**
   * Run code after getting focus on request.
   *
   * @see #requestFocus
   */
  public static void runAfterGotFocus(@NotNull final Runnable runnable) {
    final Application application = ApplicationManager.getApplication();
    // XXX: One more invokeLater than in requestFocus()
    application.invokeLater(new Runnable() {
      @Override
      public void run() {
        application.invokeLater(new Runnable() {
          @Override
          public void run() {
            application.invokeLater(runnable);
          }
        });
      }
    });
  }
}
