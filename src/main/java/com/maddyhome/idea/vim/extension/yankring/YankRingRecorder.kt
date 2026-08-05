/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.yankring

import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.common.VimRegisterListener
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.thinapi.toTextSelectionType

/**
 * Feeds every yank, delete and change into the ring.
 */
internal object YankRingRecorder : VimRegisterListener {
  override val owner: ListenerOwner = ListenerOwner.Plugin.get(PLUGIN_NAME)

  override fun registerStored(
    register: Char,
    copiedText: VimCopiedText,
    type: SelectionType,
    isDelete: Boolean,
  ) {
    YankRing.record(copiedText.text, type.toTextSelectionType())
  }
}
