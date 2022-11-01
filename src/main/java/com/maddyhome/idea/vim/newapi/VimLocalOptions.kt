/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

interface VimLocalOptions {
  fun getOptions(editor: VimEditor): Map<String, VimDataType>
  fun setOption(editor: VimEditor, optionName: String, value: VimDataType)
  fun reset(editor: VimEditor)
}
