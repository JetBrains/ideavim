/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.common.VimCopiedText
import com.maddyhome.idea.vim.group.visual.VimSelection
import com.maddyhome.idea.vim.register.Register
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * [putToLine] has affect only of [insertTextBeforeCaret] is false and [visualSelection] is null
 */
data class PutData(
  val textData: TextData?,
  val visualSelection: VisualSelection?,
  val count: Int,
  val insertTextBeforeCaret: Boolean,
  private val rawIndent: Boolean,
  val caretAfterInsertedText: Boolean,
  val putToLine: Int = -1,
) {
  val indent: Boolean =
    if (rawIndent && textData?.typeInRegister != SelectionType.LINE_WISE && visualSelection?.typeInEditor != SelectionType.LINE_WISE) false else rawIndent

  data class VisualSelection(
    val caretsAndSelections: Map<VimCaret, VimSelection>,
    val typeInEditor: SelectionType,
  )

  data class TextData(
    val registerChar: Char?,
    val copiedText: VimCopiedText,
    val typeInRegister: SelectionType,
  ) {
    constructor(register: Register): this(register.name, register.copiedText, register.type)

    val rawText = copiedText.text // TODO do not call it raw text...
  }
}
