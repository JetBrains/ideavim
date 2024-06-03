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
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.key.KeyConsumer
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public class DigraphConsumer : KeyConsumer {
  private companion object {
    private val logger = vimLogger<DigraphConsumer>()
  }

  override fun consumeKey(
    key: KeyStroke,
    editor: VimEditor,
    allowKeyMappings: Boolean,
    mappingCompleted: Boolean,
    keyProcessResultBuilder: KeyProcessResult.KeyProcessResultBuilder,
    shouldRecord: KeyHandler.MutableBoolean,
  ): Boolean {
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
        commandBuilder.addKey(key)
        return true
      }
      if (digraphSequence.isLiteralStart(key)) {
        digraphSequence.startLiteralSequence()
        commandBuilder.addKey(key)
        return true
      }
    }
    val res = digraphSequence.processKey(key, editor)
    val keyHandler = KeyHandler.getInstance()
    when (res.result) {
      DigraphResult.RES_HANDLED -> {
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, _, _ ->
          val commandLine = injector.commandLine.getActiveCommandLine()
          commandLine?.setPromptCharacter(if (lambdaKeyState.commandBuilder.isPuttingLiteral()) '^' else key.keyChar)
          lambdaKeyState.commandBuilder.addKey(key)
        }
        return true
      }
      DigraphResult.RES_DONE -> {
        val commandLine = injector.commandLine.getActiveCommandLine()
        if (commandLine != null) {
          if (key.keyCode == KeyEvent.VK_C && key.modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
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
        val stroke = res.stroke ?: return false
        keyProcessResultBuilder.addExecutionStep { lambdaKeyState, lambdaEditorState, lambdaContext ->
          lambdaKeyState.commandBuilder.addKey(key)
          keyHandler.handleKey(lambdaEditorState, stroke, lambdaContext, lambdaKeyState)
        }
        return true
      }
      DigraphResult.RES_BAD -> {
        val commandLine = injector.commandLine.getActiveCommandLine()
        if (commandLine != null) {
          if (key.keyCode == KeyEvent.VK_C && key.modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
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
      DigraphResult.RES_UNHANDLED -> {
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
    return false
  }
}