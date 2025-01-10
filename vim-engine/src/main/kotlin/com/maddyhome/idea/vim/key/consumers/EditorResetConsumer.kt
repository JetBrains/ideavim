/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key.consumers

import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.helper.isCloseKeyStroke
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.state.KeyHandlerState
import com.maddyhome.idea.vim.state.mode.Mode
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class EditorResetConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<EditorResetConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered EditorResetConsumer" }
    if (!isEditorReset(key, editor)) return false
    keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
      handleEditorReset(
        lambdaEditor,
        key,
        lambdaKeyState,
        lambdaContext
      )
    }
    return true
  }

  private fun isEditorReset(key: KeyStroke, editor: VimEditor): Boolean {
    val editorReset = editor.mode is Mode.NORMAL && key.isCloseKeyStroke()
    logger.debug { "This is editor reset: $editorReset" }
    return editorReset
  }

  private fun handleEditorReset(
    editor: VimEditor,
    key: KeyStroke,
    keyState: KeyHandlerState,
    context: ExecutionContext,
  ) {
    val commandBuilder = keyState.commandBuilder
    if (commandBuilder.isAwaitingCharOrDigraphArgument()) {
      editor.isReplaceCharacter = false
    }
    if (commandBuilder.isEmpty) {
      val register = injector.registerGroup
      if (register.currentRegister == register.defaultRegister) {
        // Escape should exit "Insert Normal" mode. We don't have a handler for <Esc> in Normal mode, so we do it here
        val mode = editor.mode
        if (mode is Mode.NORMAL && (mode.isInsertPending || mode.isReplacePending)) {
          editor.mode = mode.returnTo
        } else {
          var indicateError = true
          if (key.keyCode == KeyEvent.VK_ESCAPE) {
            val executed = arrayOf<Boolean?>(null)
            injector.actionExecutor.executeCommand(
              editor,
              { executed[0] = injector.actionExecutor.executeEsc(editor, context) },
              "",
              null,
            )
            indicateError = !executed[0]!!
          }
          if (indicateError) {
            injector.messages.indicateError()
          }
        }
      }
    }
    KeyHandler.getInstance().reset(keyState, editor.mode)
  }
}
