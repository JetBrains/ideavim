package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.group.visual.VimSelection

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
    val rawText: String?,
    val typeInRegister: SelectionType,
    val transferableData: List<Any>,
  )
}
