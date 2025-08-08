/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.VimPlugin
import com.intellij.vim.api.getVariable
import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.models.TextType
import com.intellij.vim.api.scopes.editor.caret.CaretTransaction

private const val PLUGIN_NAME: String = "ReplaceWithRegister"

@VimPlugin(name = PLUGIN_NAME)
fun VimApi.init() {
  mappings {
    nmap(keys = "gr", actionName = RWR_OPERATOR) {
      rewriteMotion()
    }
    nmap(keys = "grr", actionName = RWR_LINE) {
      rewriteLine()
    }
    vmap(keys = "gr", actionName = RWR_VISUAL) {
      rewriteVisual()
    }
  }

  exportOperatorFunction(OPERATOR_FUNC_NAME) {
    operatorFunction()
  }
}

private fun VimApi.operatorFunction(): Boolean {
  fun CaretTransaction.getSelection(): Range? {
    return when {
      this@operatorFunction.mode == Mode.NORMAL -> changeMarks
      this@operatorFunction.mode.isVisual -> selection
      else -> null
    }
  }

  editor {
    change {
      forEachCaret {
        val selectionRange = getSelection() ?: return@forEachCaret
        val registerData = prepareRegisterData() ?: return@forEachCaret
        replaceTextAndUpdateCaret(this@operatorFunction, selectionRange, registerData)
      }
    }
  }
  return true
}

private fun VimApi.rewriteMotion() {
  setOperatorFunction(OPERATOR_FUNC_NAME)
  normal("g@")
}

private fun VimApi.rewriteLine() {
  val count1 = getVariable<Int>("v:count1") ?: 1
  editor {
    change {
      forEachCaret {
        val endOffset = getLineEndOffset(line.number + count1 - 1, true)
        val lineStartOffset = line.start
        val registerData = prepareRegisterData() ?: return@forEachCaret
        replaceText(lineStartOffset, endOffset, registerData.first)
        updateCaret(offset = lineStartOffset)
      }
    }
  }
}

private fun VimApi.rewriteVisual() {
  editor {
    change {
      forEachCaret {
        val selectionRange = selection
        val registerData = prepareRegisterData() ?: return@forEachCaret
        replaceTextAndUpdateCaret(this@rewriteVisual, selectionRange, registerData)
      }
    }
  }
  mode = Mode.NORMAL
}

private fun CaretTransaction.prepareRegisterData(): Pair<String, TextType>? {
  val lastRegisterName: Char = lastSelectedReg
  var registerText: String = getReg(lastRegisterName) ?: return null
  var registerType: TextType = getRegType(lastRegisterName) ?: return null

  if (registerType == TextType.LINE_WISE && registerText.endsWith("\n")) {
    registerText = registerText.removeSuffix("\n")
    registerType = TextType.CHARACTER_WISE
  }

  return registerText to registerType
}

private fun CaretTransaction.replaceTextAndUpdateCaret(
  vimApi: VimApi,
  selectionRange: Range,
  registerData: Pair<String, TextType>,
) {
  val (text, registerType) = registerData

  if (registerType == TextType.BLOCK_WISE) {
    val lines = text.lines()

    if (selectionRange is Range.Simple) {
      val startOffset = selectionRange.start
      val endOffset = selectionRange.end
      val startLine = getLine(startOffset)
      val diff = startOffset - startLine.start

      lines.forEachIndexed { index, lineText ->
        val offset = getLineStartOffset(startLine.number + index) + diff
        if (index == 0) {
          replaceText(offset, endOffset, lineText)
        } else {
          insertText(offset, lineText, insertBeforeCaret = true)
        }
      }

      updateCaret(offset = startOffset)
    } else if (selectionRange is Range.Block) {
      val selections: Array<Range.Simple> = selectionRange.ranges

      selections.zip(lines).forEach { (range, lineText) ->
        replaceText(range.start, range.end, lineText)
      }
    }
  } else {
    if (selectionRange is Range.Simple) {
      val textLength = this.text.length
      if (textLength == 0) {
        insertText(0, text)
      } else {
        replaceText(selectionRange.start, selectionRange.end, text)
      }
    } else if (selectionRange is Range.Block) {
      val selections: Array<Range.Simple> = selectionRange.ranges.sortedByDescending { it.start }.toTypedArray()
      val lines = List(selections.size) { text }

      replaceTextBlockwise(selectionRange, lines)

      vimApi.mode = Mode.NORMAL
      updateCaret(offset = selections.last().start)
    }
  }
}

private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
private const val OPERATOR_FUNC_NAME = "ReplaceWithRegisterOperatorFunc"