/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:JvmName("UiHelper")

package com.maddyhome.idea.vim.helper

import com.intellij.ide.ui.UISettings
import com.intellij.ide.ui.UISettingsUtils
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
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

internal fun selectEditorFont(editor: Editor?, forText: String): Font {
  val fontSize = when {
    editor is EditorImpl -> editor.fontSize2D
    UISettings.getInstance().presentationMode -> UISettingsUtils.getInstance().presentationModeFontSize
    editor?.editorKind == EditorKind.CONSOLE -> UISettingsUtils.getInstance().scaledConsoleFontSize
    else -> UISettingsUtils.getInstance().scaledEditorFontSize
  }

  val scheme = EditorColorsManager.getInstance().globalScheme
  scheme.fontPreferences.realFontFamilies.forEach { fontName ->
    val font = Font(fontName, Font.PLAIN, scheme.editorFontSize)
    if (font.canDisplayUpTo(forText) == -1) {
      return font.deriveFont(fontSize)
    }
  }

  return Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize).deriveFont(fontSize)
}
