/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.plugin

import com.intellij.vim.api.Api
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.CaretRead
import com.intellij.vim.api.CaretTransaction
import com.intellij.vim.api.Mode
import com.intellij.vim.api.Scope
import com.intellij.vim.api.TextRange
import com.intellij.vim.api.getRegContent
import com.intellij.vim.api.group.*
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.state.mode.SelectionType

class ReplaceWithRegister : VimPlugin {
  private lateinit var api: Api
  
  override fun Scope.load() {
    nmap("<Plug>ReplaceWithRegisterOperator", RwrMotion())
    nmap("<Plug>ReplaceWithRegisterLine", RwrLine())
    vmap("<Plug>ReplaceWithRegisterVisual", RwrVisual())
    
    nmap("gr", "<Plug>ReplaceWithRegisterOperator")
    nmap("grr", "<Plug>ReplaceWithRegisterLine")
    vmap("gr", "<Plug>ReplaceWithRegisterVisual")

    exportOperatorFunction("ReplaceWithRegisterOperatorFunc") {
      read {
        forEachCaret {
          // TODO collect ranges and throw exception is range for caret is missing
        }
      }
      change {
        forEachCaret {
          val range = getRange() ?: throw ExException("Failed to get range for operator function")
          replaceWithRegister(range)
        }
      }
    }
  }

  private fun CaretRead.getRange(): TextRange? = when (mode) {
    Mode.NORMAL -> api.getChangeMarks(this)
    Mode.VISUAL -> api.getCaretInfo(this).selection?.run { TextRange.SimpleRange(first, second) }
    else -> null
  }
  
  private fun CaretTransaction.replaceWithRegister(range: TextRange) {
    val registerContent = getRegContent(caretId, register) ?: return
    var registerText = registerContent.text
    var registerType = registerContent.type
    val registerData = registerContent.transferableData
    if (registerType == SelectionType.LINE_WISE && registerText.endsWith('\n')) {
      // TODO it won't work properly with TransferableData. It should be readjusted to the new text length
      registerText = registerText.dropLast(1)
      registerType = SelectionType.CHARACTER_WISE
    }
    api.deleteText(this, range, register = '_')
    api.insert(this, registerText, registerType, registerData)
    api.updateCaret(this, CaretInfo(range.min + registerText.length - 1, null))
  }
}