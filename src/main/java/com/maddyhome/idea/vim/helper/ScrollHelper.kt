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
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants

fun getNormalizedScrollOffset(editor: Editor): Int {
  val scrollOffset = injector.options(editor.vim).getIntValue(OptionConstants.scrolloff)
  return EditorHelper.normalizeScrollOffset(editor, scrollOffset)
}

fun getNormalizedSideScrollOffset(editor: Editor): Int {
  val sideScrollOffset = injector.options(editor.vim).getIntValue(OptionConstants.sidescrolloff)
  return EditorHelper.normalizeSideScrollOffset(editor, sideScrollOffset)
}
