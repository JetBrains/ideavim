/*
 * Copyright 2003-2023 The IdeaVim authors
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

internal fun getNormalizedScrollOffset(editor: Editor): Int {
  val scrollOffset = injector.options(editor.vim).scrolloff
  return EditorHelper.normalizeScrollOffset(editor, scrollOffset)
}

internal fun getNormalizedSideScrollOffset(editor: Editor): Int {
  val sideScrollOffset = injector.options(editor.vim).sidescrolloff
  return EditorHelper.normalizeSideScrollOffset(editor, sideScrollOffset)
}
