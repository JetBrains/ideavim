/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType

class IjVimLocalOptions : VimLocalOptions {

  private val localValuesKey = Key<MutableMap<String, VimDataType>>("localOptions")

  override fun getOptions(editor: VimEditor): Map<String, VimDataType> {
    val ijEditor = (editor as IjVimEditor).editor

    return ijEditor.getUserData(localValuesKey) ?: emptyMap()
  }

  override fun setOption(editor: VimEditor, optionName: String, value: VimDataType) {
    val ijEditor = (editor as IjVimEditor).editor

    if (ijEditor.getUserData(localValuesKey) == null) {
      ijEditor.putUserData(localValuesKey, mutableMapOf(optionName to value))
    } else {
      ijEditor.getUserData(localValuesKey)!![optionName] = value
    }
  }

  override fun reset(editor: VimEditor) {
    val ijEditor = (editor as IjVimEditor).editor

    ijEditor.getUserData(localValuesKey)?.clear()
  }
}
