/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.replacewithregister

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.CaretInfo
import com.intellij.vim.api.Transaction
import com.intellij.vim.api.apiContext
import com.intellij.vim.api.apiEditor
import com.intellij.vim.api.change
import com.intellij.vim.api.forEachCaretSorted
import com.intellij.vim.api.getReg
import com.intellij.vim.api.getRegType
import com.intellij.vim.api.register
import com.intellij.vim.api.replaceText
import com.intellij.vim.api.setMap
import com.intellij.vim.api.updateCaret
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.isLine

internal class ReplaceWithRegister : VimExtension {

  override fun getName(): String = "ReplaceWithRegister"

  override fun init() {
    /**
     *
     * writeLock {
     *   val map = getMap()
     *   < ----- Other thread removeMap(--map)
     *   removeMap(map)
     * }
     *
     *
     */
    change {
      setMap("X", "p", "<Plug>ReplaceWithRegisterVisual")
      setMap("X", "<Plug>ReplaceWithRegisterVisual", RwrVisual())
    }
  }

  private class RwrVisual : ExtensionHandler {
    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      apiEditor = editor
      apiContext = context

      forEachCaretSorted { caretId, caretInfo ->
        doReplace(caretId, caretInfo)
      }
      editor.exitVisualMode()
    }
  }
}

private fun Transaction.doReplace(caret: CaretId, caretInfo: CaretInfo) {
  val selection = caretInfo.selection ?: return

  var usedText = getReg(register) ?: return
  var usedType = getRegType(register) ?: return

  if (usedType.isLine && usedText.endsWith('\n') == true) {
    // Code from original plugin implementation. Correct text for linewise selected text
    usedText = usedText.dropLast(1)
    usedType = SelectionType.CHARACTER_WISE
  }

  replaceText(
    selection.first,
    selection.second,
    usedText,
  )

  updateCaret(caret, CaretInfo(selection.first + usedText.length - 1, null))
}
