/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.scroll.VerticalScroll

/**
 * Moves [visualLine] into the part of the screen that is at least 'scrolloff' rows from the top and the bottom, see
 * [VerticalScroll.cursorCorrect]
 */
fun keepVisualLineInScrollOffset(
  editor: Editor,
  visualLine: Int,
  scrollOffset: Int = injector.options(editor.vim).scrolloff,
): Int {
  val layout = EditorWindowLayout(editor)
  return VerticalScroll.cursorCorrect(layout, layout.view, visualLine, scrollOffset)
}

fun getNormalizedSideScrollOffset(editor: Editor): Int {
  val sideScrollOffset = injector.options(editor.vim).sidescrolloff
  return EditorHelper.normalizeSideScrollOffset(editor, sideScrollOffset)
}
