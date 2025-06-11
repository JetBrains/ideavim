/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Range
import com.intellij.vim.api.RegisterData
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.isLine
import com.intellij.vim.api.scopes.Transaction
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
    fun CaretTransaction.getSelection(): Range? {
      return when (this@operatorFunction.mode) {
        Mode.NORMAL -> getChangeMarks()
        Mode.VISUAL -> getVisualSelectionMarks()
        else -> null
      }
    }

    val selectionType: TextSelectionType = getSelectionTypeForCurrentMode() ?: TextSelectionType.CHARACTER_WISE
    editor {
      change {
        forEachCaret {
          val selectionRange = getSelection() ?: return@forEachCaret
          val registerData = prepareRegisterData() ?: return@forEachCaret
          replaceTextAndUpdateCaret(caretId, selectionRange, selectionType, registerData)
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
          val line = getCaretLine()
          val lineRange = Range(getLineStartOffset(line), getLineEndOffset(line + count1 - 1, true))
          val registerData = prepareRegisterData() ?: return@forEachCaretSorted
          val selectionType = TextSelectionType.LINE_WISE
          replaceTextAndUpdateCaret(caretId, lineRange, selectionType, registerData)
          updateCaret(caretInfo.copy(offset = lineRange.start))
        }
      }
    }
  }

  private fun VimScope.rewriteVisual() {
    val selectionType: TextSelectionType = getSelectionTypeForCurrentMode() ?: TextSelectionType.CHARACTER_WISE
    editor {
      change {
        forEachCaretSorted {
          val selectionRange = getVisualSelectionMarks() ?: return@forEachCaretSorted
          val registerData = prepareRegisterData() ?: return@forEachCaretSorted
          replaceTextAndUpdateCaret(caretId, selectionRange, selectionType, registerData)
        }
      }
    }
    exitVisualMode()
  }

  private fun CaretTransaction.prepareRegisterData(): RegisterData? {
    val lastRegisterName: Char = getCurrentRegisterName()
    var registerText: String = getRegisterContent(lastRegisterName) ?: return null
    var registerType: RegisterType = getRegisterType(lastRegisterName) ?: return null

    if (registerType.isLine && registerText.endsWith("\n")) {
      registerText = registerText.removeSuffix("\n")
      registerType = RegisterType.CHAR
    }

    return RegisterData(registerText, registerType)
  }

  private fun Transaction.replaceTextAndUpdateCaret(
    caretId: CaretId,
    selectionRange: Range,
    selectionType: TextSelectionType,
    registerData: RegisterData,
  ) {
    val text: String = registerData.text
    val registerType: RegisterType = registerData.type
    val (startOffset, endOffset) = selectionRange
    /**
     * This logic that includes updating caret position should probably be hidden from the user and handled inside
     * functions - this is a simplified version for the API draft.
     */
    var newCaretOffset: Int
    val replaceTextBlockWise: Boolean =
      registerType == RegisterType.BLOCK && selectionType == TextSelectionType.CHARACTER_WISE

    if (replaceTextBlockWise) {
      replaceTextBlockwise(caretId, startOffset, endOffset, text.split("\n"))
      newCaretOffset = startOffset
    } else {
      replaceText(caretId, startOffset, endOffset, text)
      newCaretOffset = when (selectionType) {
        TextSelectionType.CHARACTER_WISE -> startOffset + text.length - 1
        TextSelectionType.LINE_WISE -> startOffset
        TextSelectionType.BLOCK_WISE -> startOffset
      }
    }

    withCaret(caretId) {
      updateCaret(CaretInfo(newCaretOffset, null))
    }
  }

  companion object {
    private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
    private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
    private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
    private const val OPERATOR_FUNC_NAME = "ReplaceWithRegisterOperatorFunc"
  }
}