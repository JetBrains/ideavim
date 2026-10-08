/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.multiplecursors.leaveVisualMode
import com.maddyhome.idea.vim.newapi.ij

/**
 * Starts Insert mode with the [command] of Normal mode at each region: `i` before it, `a` after it, and `I` and `A`
 * at the start and the end of its line
 *
 * In Visual mode, `i` and `a` would start a text object. Inside the session, vim-visual-multi uses them for Insert
 * mode too.
 */
internal class InsertHandler(private val command: String) : ExtensionHandler {

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val insertOffsets = editor.nativeCarets().associateWith {
      if (command == "a") it.selectionEnd - 1 else it.selectionStart
    }
    leaveVisualMode(editor)
    insertOffsets.forEach { (caret, offset) -> caret.moveToOffset(offset) }
    executeNormalWithoutMapping(injector.parser.parseKeys(command), editor.ij)
  }
}
