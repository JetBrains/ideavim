package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.OperatedRange

abstract class VimChangeGroupBase : VimChangeGroup {


}

fun OperatedRange.toType() = when (this) {
  is OperatedRange.Characters -> SelectionType.CHARACTER_WISE
  is OperatedRange.Lines -> SelectionType.LINE_WISE
  is OperatedRange.Block -> SelectionType.BLOCK_WISE
}
