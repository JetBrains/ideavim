/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
internal fun requestFocus(component: JComponent) {
  IdeFocusManager.findInstance().requestFocus(component, true)
}

/**
 * Run code after getting focus on request.
 *
 * @see [requestFocus]
 */
internal fun runAfterGotFocus(runnable: Runnable) {
  IdeFocusManager.findInstance().doWhenFocusSettlesDown(runnable, ModalityState.defaultModalityState())
}

internal fun selectFont(forStr: String): Font {
  val scheme = EditorColorsManager.getInstance().globalScheme

  val fontName = scheme.fontPreferences.realFontFamilies.firstOrNull {
    Font(it, Font.PLAIN, scheme.editorFontSize).canDisplayUpTo(forStr) == -1
  } ?: return Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
  return Font(fontName, Font.PLAIN, scheme.editorFontSize)
}
