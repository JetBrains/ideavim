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
import com.intellij.vim.api.RegisterType
import com.intellij.vim.api.TextSelectionType
import com.intellij.vim.api.isLine
import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.extension.thin.api.VimPluginBase

class ReplaceWithRegisterNewApi : VimPluginBase {
  override fun getName(): String = "ReplaceWithRegister"

  override fun VimScope.init() {
    nmap(fromKeys = RWR_OPERATOR, isRepeatable = true) {
      rewriteMotion()
    }
    nmap(fromKeys = RWR_LINE, isRepeatable = true) {
      rewriteLine()
    }
    vmap(fromKeys = RWR_VISUAL, isRepeatable = true) {
      rewriteVisual()
    }

    nmap(fromKeys = "gr", toKeys = RWR_OPERATOR)
    nmap(fromKeys = "grr", toKeys = RWR_LINE)
    vmap(fromKeys = "gr", toKeys = RWR_VISUAL)

    exportOperatorFunction(OPERATOR_FUNC) {
      forEachCaret { caretId ->
        val selectionRange: Pair<Int, Int>? = read {
          when (this@forEachCaret.mode) {
            Mode.NORMAL -> getChangeMarks(caretId)
            Mode.VISUAL -> getVisualSelectionMarks(caretId)
            else -> null
          }
        }

        if (selectionRange == null) {
          return@forEachCaret
        }

        val selectionType: TextSelectionType = getSelectionTypeForCurrentMode() ?: TextSelectionType.CHARACTER_WISE
        replaceTextForCaret(caretId, selectionRange, selectionType)
      }
      return@exportOperatorFunction true
    }
  }

  private fun VimScope.rewriteMotion() {
    setOperatorFunction(OPERATOR_FUNC)
    normal("g@")
  }

  private fun VimScope.rewriteLine() {
    val caretsSelectionsMap: MutableMap<CaretId, Pair<Int, Int>> = mutableMapOf()
    forEachCaret { caretId ->
      val caretLine: Int?
      val lineRange: Pair<Int, Int>?
      val count1 = getVariableInt("v:count1") ?: 1

      read {
        caretLine = getCaretLine(caretId)
        lineRange = caretLine?.let { line ->
          getLineStartOffset(line) to getLineEndOffset(line + count1 - 1, true)
        }
      }

      caretsSelectionsMap[caretId] = lineRange ?: return@forEachCaret
      replaceTextForCaret(caretId, lineRange, TextSelectionType.LINE_WISE)
    }

    forEachCaretSorted { caretId ->
      val (selectionStart, _) = caretsSelectionsMap.getValue(caretId)
      change {
        val caretInfo: CaretInfo = getCaretInfo(caretId) ?: return@change
        updateCaret(caretId, caretInfo.copy(offset = selectionStart))
      }
    }
  }

  private fun VimScope.rewriteVisual() {
    forEachCaretSorted { caretId ->
      val selectionType: TextSelectionType = getSelectionTypeForCurrentMode() ?: TextSelectionType.CHARACTER_WISE
      val selectionRange: Pair<Int, Int> = read { getVisualSelectionMarks(caretId) } ?: return@forEachCaretSorted
      replaceTextForCaret(caretId, selectionRange, selectionType)
    }
    exitVisualMode()
  }

  private fun VimScope.replaceTextForCaret(
    caretId: CaretId,
    selectionRange: Pair<Int, Int>,
    selectionType: TextSelectionType,
  ) {
    val (startOffset, endOffset) = selectionRange
    val lastRegisterName: Char
    var registerText: String?
    var registerType: RegisterType?

    read {
      lastRegisterName = getCurrentRegisterName(caretId)
      registerText = getRegisterContent(caretId, lastRegisterName)
      registerType = getRegisterType(caretId, lastRegisterName)
    }

    if (registerText == null || registerType == null) return

    if (registerType.isLine && registerText.endsWith("\n")) {
      registerText = registerText.removeSuffix("\n")
      registerType = RegisterType.CHAR
    }

    change {
      /**
       * This logic that includes updating caret position should probably be hidden from the user and handled inside
       * functions - this is a simplified version for the API draft.
       */
      var newCaretOffset: Int
      val replaceTextBlockWise: Boolean =
        registerType == RegisterType.BLOCK && selectionType == TextSelectionType.CHARACTER_WISE

      if (replaceTextBlockWise) {
        replaceTextBlockwise(caretId, startOffset, endOffset, registerText.split("\n"))
        newCaretOffset = startOffset
      } else {
        replaceText(caretId, startOffset, endOffset, registerText)
        newCaretOffset = when (selectionType) {
          TextSelectionType.CHARACTER_WISE -> startOffset + registerText.length - 1
          TextSelectionType.LINE_WISE -> startOffset
          TextSelectionType.BLOCK_WISE -> startOffset
        }
      }

      val caretInfo: CaretInfo = getCaretInfo(caretId) ?: return@change
      updateCaret(caretId, caretInfo.copy(newCaretOffset, null))
    }
  }

  companion object {
    private const val RWR_OPERATOR = "<Plug>ReplaceWithRegisterOperator"
    private const val RWR_LINE = "<Plug>ReplaceWithRegisterLine"
    private const val RWR_VISUAL = "<Plug>ReplaceWithRegisterVisual"
    private const val OPERATOR_FUNC = "ReplaceWithRegisterOperatorFunc"
  }
}