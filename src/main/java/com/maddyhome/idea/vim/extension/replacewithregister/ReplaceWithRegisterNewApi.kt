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
import com.intellij.vim.api.scopes.getVariable
import com.maddyhome.idea.vim.extension.thin.api.VimPluginBase

class ReplaceWithRegisterNewApi : VimPluginBase() {
  override fun getName(): String = "ReplaceWithRegister"

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
    fun CaretTransaction.getSelection(): Array<Range>? {
      return when (this@operatorFunction.mode) {
        is Mode.NORMAL -> changeMarks?.let { arrayOf(it) }
        is Mode.VISUAL -> visualSelectionMarks
        else -> null
      }
    }

    editor {
      change {
        forEachCaret {
          val selectionRange = getSelection() ?: return@forEachCaret
          val registerData = prepareRegisterData() ?: return@forEachCaret
          replaceTextAndUpdateCaret(selectionRange, registerData)
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
        forEachCaretSorted {
          val lineRange = Range(getLineStartOffset(line), getLineEndOffset(line + count1 - 1, true))
          val registerData = prepareRegisterData() ?: return@forEachCaretSorted
          replaceText(lineRange.start, lineRange.end, registerData.first)
          updateCaret(offset = lineRange.start)
        }
      }
    }
  }

  private fun VimScope.rewriteVisual() {
    editor {
      change {
        forEachCaretSorted {
          val selectionRange = visualSelectionMarks ?: return@forEachCaretSorted
          val registerData = prepareRegisterData() ?: return@forEachCaretSorted
          replaceTextAndUpdateCaret(selectionRange, registerData)
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
    selectionRange: Array<Range>,
    registerData: Pair<String, TextType>,
  ) {
    val text: String = registerData.first
    val registerType: TextType = registerData.second
    if (registerType == TextType.BLOCK_WISE) {
      val lines = text.lines()
      val startOffset = selectionRange.first().start
      val endOffset = selectionRange.last().end
      val startLine = getLineNumber(startOffset)
      val diff = startOffset - getLineStartOffset(startLine)

      lines.forEachIndexed { index, lineText ->
        val offset = getLineStartOffset(startLine + index) + diff
        if (index == 0) {
          replaceText(offset, endOffset, lineText)
        } else {
          insertText(offset, lineText)
        }
      }

      updateCaret(offset = startOffset)
    } else {
      val range = selectionRange.first()
      replaceText(range.start, range.end, text)
    }
  }

  companion object {
    private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
    private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
    private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
    private const val OPERATOR_FUNC_NAME = "ReplaceWithRegisterOperatorFunc"
  }
}