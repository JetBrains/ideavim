/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

fun getNormalizedScrollOffset(editor: Editor): Int {
  val scrollOffset = (
    VimPlugin.getOptionService().getOptionValue(
      OptionScope.LOCAL(editor.vim),
      OptionConstants.scrolloffName,
      OptionConstants.scrolloffName
    ) as VimInt
    ).value
  return EditorHelper.normalizeScrollOffset(editor, scrollOffset)
}

fun getNormalizedSideScrollOffset(editor: Editor): Int {
  val sideScrollOffset = (
    VimPlugin.getOptionService().getOptionValue(
      OptionScope.LOCAL(editor.vim),
      OptionConstants.sidescrolloffName,
      OptionConstants.sidescrolloffName
    ) as VimInt
    ).value
  return EditorHelper.normalizeSideScrollOffset(editor, sideScrollOffset)
}
