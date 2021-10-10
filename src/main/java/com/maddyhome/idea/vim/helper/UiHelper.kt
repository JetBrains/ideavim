/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

@file:JvmName("UiHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.Font
import javax.swing.JComponent

/**
 * Get focus reliably.
 */
fun requestFocus(component: JComponent) {
  IdeFocusManager.findInstance().requestFocus(component, true)
}

/**
 * Run code after getting focus on request.
 *
 * @see [requestFocus]
 */
fun runAfterGotFocus(runnable: Runnable) {
  IdeFocusManager.findInstance().doWhenFocusSettlesDown(runnable, ModalityState.defaultModalityState())
}

fun selectFont(forStr: String): Font {
  val scheme = EditorColorsManager.getInstance().globalScheme

  val fontName = scheme.fontPreferences.realFontFamilies.firstOrNull {
    Font(it, Font.PLAIN, scheme.editorFontSize).canDisplayUpTo(forStr) == -1
  } ?: return Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
  return Font(fontName, Font.PLAIN, scheme.editorFontSize)
}
