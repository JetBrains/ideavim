/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change

import com.intellij.openapi.command.CommandProcessor
import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.group.withVimUndoGroup
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.key.KeySource
import com.maddyhome.idea.vim.newapi.ij

/**
 * `.` - repeat the last change.
 *
 * The last change is remembered as the keys typed for it (see [VimRedoBuffer]), so repeating it means feeding those
 * keys back through the key handler, the way `@` replays a macro. Extensions that register a repeat handler are the
 * exception - they are Kotlin callbacks rather than keys, and are still invoked directly.
 */
@CommandOrMotion(keys = ["."], modes = [Mode.NORMAL])
internal class RepeatChangeAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_WRITABLE

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val extensionHandler = Extension.lastExtensionHandler.takeIf { VimRepeater.repeatHandler }
    if (extensionHandler == null && VimRedoBuffer.isEmpty) return false

    preservingRepeatState {
      // One undo step for the whole repeat. The JBC backend otherwise records each atomic edit of a `s`/`c`-style
      // command separately, because speculative undo disables the platform's command grouping
      withVimUndoGroup(editor, "Vim Dot Repeat") {
        if (extensionHandler != null) {
          runExtensionHandler(extensionHandler, editor, context, operatorArguments)
        } else {
          replayKeys(editor, context, cmd.rawCount)
        }
      }
    }
    return true
  }

  /**
   * Feeds the keys of the last change back through the key handler.
   *
   * They are pulled off the key stack one at a time, as [com.maddyhome.idea.vim.key.MappingInfo] does for the
   * right-hand side of a mapping, and fed as typed keys: what was recorded is what the user typed, so a change made
   * through a mapping runs the mapping again.
   */
  private fun replayKeys(editor: VimEditor, context: ExecutionContext, rawCount: Int) {
    val keys = VimRedoBuffer.keysForReplay(rawCount)
    VimRedoBuffer.replaying {
      val keyHandler = KeyHandler.getInstance()
      keyHandler.keyStack.addKeys(keys)
      try {
        while (keyHandler.keyStack.hasStroke()) {
          val key = keyHandler.keyStack.feedStroke()
          keyHandler.handleKey(editor, key, KeySource.TYPED, context, keyHandler.keyHandlerState)
        }
      } finally {
        keyHandler.keyStack.removeFirst()
      }
    }
  }

  private fun runExtensionHandler(
    handler: ExtensionHandler,
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ) {
    val state = injector.vimState
    state.isDotRepeatInProgress = true
    try {
      CommandProcessor.getInstance().executeCommand(
        editor.ij.project,
        { handler.execute(editor, context, operatorArguments) },
        "Vim " + handler.javaClass.simpleName,
        null,
      )
    } finally {
      state.isDotRepeatInProgress = false
    }
  }

  /**
   * Runs [block] and puts back the state the replay would otherwise leave changed: `.` does not become the target of
   * `;`, does not change the register the next command uses, and stays repeatable itself.
   */
  private inline fun preservingRepeatState(block: () -> Unit) {
    val state = injector.vimState
    val executingCommand = state.executingCommand
    val lastFTCmd = injector.motion.lastFTCmd
    val lastFTChar = injector.motion.lastFTChar
    val register = injector.registerGroup.currentRegister
    val extensionHandler = Extension.lastExtensionHandler
    val repeatHandler = VimRepeater.repeatHandler

    block()

    if (executingCommand != null) state.executingCommand = executingCommand
    VimPlugin.getMotion().setLastFTCmd(lastFTCmd, lastFTChar)
    if (extensionHandler != null) Extension.lastExtensionHandler = extensionHandler
    VimRepeater.repeatHandler = repeatHandler
    Extension.reset()
    VimPlugin.getRegister().selectRegister(register)
  }
}
