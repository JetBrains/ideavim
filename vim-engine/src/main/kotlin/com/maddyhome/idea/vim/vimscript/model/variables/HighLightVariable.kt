/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.variables

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

/**
 * Represents the `v:hlsearch` variable, which indicates whether search highlighting is currently active
 *
 * Returns 1 if search highlighting is active (text is highlighted), 0 otherwise.
 * This is controlled by the `:set hlsearch` option and can be temporarily disabled
 * with `:nohlsearch`.
 *
 * See `:help v:hlsearch`
 */
internal object HighLightVariable : Variable {

  override fun evaluate(
    name: String,
    editor: VimEditor,
    context: ExecutionContext,
    vimContext: VimLContext,
  ): VimDataType {
    return injector.searchGroup.isSomeTextHighlighted().asVimInt()
  }

}