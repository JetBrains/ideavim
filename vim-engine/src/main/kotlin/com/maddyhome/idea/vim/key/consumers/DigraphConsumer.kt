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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.common.DigraphResult
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CTRL_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_C

internal class DigraphConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<DigraphConsumer>()
  }

  override fun consumeKey(
    key: VimKeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
  ): Boolean {
    logger.trace { "Entered DigraphConsumer" }
    logger.debug("Handling digraph")
    // Support starting a digraph/literal sequence if the operator accepts one as an argument, e.g. 'r' or 'f'.
    // Normally, we start the sequence (in Insert or CmdLine mode) through a VimAction that can be mapped. Our
    // VimActions don't work as arguments for operators, so we have to special case here. Helpfully, Vim appears to
    // hardcode the shortcuts, and doesn't support mapping, so everything works nicely.
    val keyState = keyProcessResultBuilder.state
    val commandBuilder = keyState.commandBuilder
    val digraphSequence = keyState.digraphSequence

    if (commandBuilder.expectedArgumentType == Argument.Type.DIGRAPH) {
      logger.trace("Expected argument is digraph")
      if (digraphSequence.isDigraphStart(key)) {
        digraphSequence.startDigraphSequence()
        commandBuilder.addTypedKeyStroke(key)
        return true
      }
      if (digraphSequence.isLiteralStart(key)) {
        digraphSequence.startLiteralSequence()
        commandBuilder.addTypedKeyStroke(key)
        return true
      }
    }

    val res = digraphSequence.processKey(key, editor)
    val keyHandler = KeyHandler.getInstance()
    when (res) {
      is DigraphResult.Handled -> {
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, _, _ ->
          keyHandler.setPromptCharacterEx(res.promptCharacter)
          lambdaKeyState.commandBuilder.addTypedKeyStroke(key)
        }
        return true
      }

      is DigraphResult.Done -> {
        val commandLine = injector.commandLine.getActiveCommandLine()
        if (commandLine != null) {
          if (key.keyCode == VK_C && key.modifiers and CTRL_DOWN_MASK != 0) {
            return false
          } else {
            keyProcessResultBuilder.addExecutionStep { _, _, _ ->
              commandLine.clearCurrentAction()
            }
          }
        }

        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, _, _ ->
          if (lambdaKeyState.commandBuilder.expectedArgumentType === Argument.Type.DIGRAPH) {
            lambdaKeyState.commandBuilder.fallbackToCharacterArgument()
          }
        }
        val codepoint = res.codepoint ?: return false
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditorState, lambdaContext ->
          lambdaKeyState.commandBuilder.addTypedKeyStroke(key)
          if (Character.isSupplementaryCodePoint(codepoint)) {
            val charArray = Character.toChars(codepoint)
            val highSurrogate = charArray[0]
            val lowSurrogate = charArray[1]

            val keyStrokeHigh = VimKeyStroke.getKeyStroke(highSurrogate)
            keyHandler.handleKey(lambdaEditorState, keyStrokeHigh, lambdaContext, lambdaKeyState)

            val keyStrokeLow = VimKeyStroke.getKeyStroke(lowSurrogate)
            keyHandler.handleKey(lambdaEditorState, keyStrokeLow, lambdaContext, lambdaKeyState)
          }
          else {
            val stroke = VimKeyStroke.getKeyStroke(codepoint.toChar())
            keyHandler.handleKey(lambdaEditorState, stroke, lambdaContext, lambdaKeyState)
          }
        }
        return true
      }

      is DigraphResult.Bad -> {
        val commandLine = injector.commandLine.getActiveCommandLine()
        if (commandLine != null) {
          if (key.keyCode == VK_C && key.modifiers and CTRL_DOWN_MASK != 0) {
            return false
          } else {
            keyProcessResultBuilder.addExecutionStep { _, _, _ ->
              commandLine.clearCurrentAction()
            }
          }
        }
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, _ ->
          // BAD is an error. We were expecting a valid character, and we didn't get it.
          if (lambdaKeyState.commandBuilder.expectedArgumentType != null) {
            KeyHandler.getInstance().setBadCommand(lambdaEditor, lambdaKeyState)
          }
        }
        return true
      }

      is DigraphResult.Unhandled -> {
        // UNHANDLED means the keystroke made no sense in the context of a digraph, but isn't an error in the current
        // state. E.g. waiting for {char} <BS> {char}. Let the key handler have a go at it.
        if (commandBuilder.expectedArgumentType === Argument.Type.DIGRAPH) {
          commandBuilder.fallbackToCharacterArgument()
          keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditor, lambdaContext ->
            keyHandler.handleKey(lambdaEditor, key, lambdaContext, lambdaKeyState)
          }
          return true
        }
        return false
      }
    }
  }
}
