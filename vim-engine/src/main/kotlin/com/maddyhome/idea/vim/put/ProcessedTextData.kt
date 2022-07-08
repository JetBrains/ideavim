package com.maddyhome.idea.vim.put

import com.maddyhome.idea.vim.command.SelectionType

data class ProcessedTextData(
  val text: String,
  val typeInRegister: SelectionType,
  val transferableData: List<Any>,
)
