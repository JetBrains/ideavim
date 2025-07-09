/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType
import com.intellij.vim.api.isLine
import com.intellij.vim.api.scopes.VimScope
import com.intellij.vim.api.scopes.caret.CaretTransaction
import com.maddyhome.idea.vim.extension.thin.api.VimPluginBase

class ReplaceWithRegisterNewApi : VimPluginBase() {
  override val name: String = "ReplaceWithRegister"

  override fun VimScope.init() {
    mappings {
      nmap(keys = "gr", label = RWR_OPERATOR, isRepeatable = true) {
        rewriteMotion()
      }
      nmap(keys = "grr", label = RWR_LINE, isRepeatable = true) {
        rewriteLine()
      }
      vmap(keys = "gr", label = RWR_VISUAL, isRepeatable = true) {
        rewriteVisual()
      }
    }

    exportOperatorFunction(OPERATOR_FUNC_NAME) {
      operatorFunction()
    }
  }

  private fun VimScope.operatorFunction(): Boolean {
    fun CaretTransaction.getSelection(): Range? {
      return when (this@operatorFunction.mode) {
        is Mode.NORMAL -> changeMarks
        is Mode.VISUAL -> selection
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

  private fun VimScope.rewriteMotion() {
    setOperatorFunction(OPERATOR_FUNC_NAME)
    normal("g@")
  }

  private fun VimScope.rewriteLine() {
    val count1 = getVariable<Int>("v:count1") ?: 1
    editor {
      change {
        forEachCaret {
          val endOffset = getLineEndOffset(line.number + count1 - 1, true)
          val registerData = prepareRegisterData() ?: return@forEachCaret
          replaceText(line.start, endOffset, registerData.first)
          updateCaret(offset = line.start)
        }
      }
    }
  }

  private fun VimScope.rewriteVisual() {
    editor {
      change {
        forEachCaret {
          val selectionRange = selection
          val registerData = prepareRegisterData() ?: return@forEachCaret
          replaceTextAndUpdateCaret(this@rewriteVisual, selectionRange, registerData)
        }
      }
    }
    mode = Mode.NORMAL()
  }

  private fun CaretTransaction.prepareRegisterData(): Pair<String, TextType>? {
    val lastRegisterName: Char = lastSelectedReg
    var registerText: String = getReg(lastRegisterName) ?: return null
    var registerType: TextType = getRegType(lastRegisterName) ?: return null

    if (registerType.isLine && registerText.endsWith("\n")) {
      registerText = registerText.removeSuffix("\n")
      registerType = TextType.CHARACTER_WISE
    }

    return registerText to registerType
  }

  private fun CaretTransaction.replaceTextAndUpdateCaret(
    vimScope: VimScope,
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
            insertText(offset, lineText)
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
        replaceText(selectionRange.start, selectionRange.end, text)
      } else if (selectionRange is Range.Block) {
        val selections: Array<Range.Simple> = selectionRange.ranges.sortedByDescending { it.start }.toTypedArray()

        selections.forEach { range ->
          replaceTextForRange(range.start, range.end, text)
        }

        vimScope.mode = Mode.NORMAL()
        updateCaret(offset = selections.last().start)
      }
    }
  }

  companion object {
    private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
    private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
    private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
    private const val OPERATOR_FUNC_NAME = "ReplaceWithRegisterOperatorFunc"
  }
}
