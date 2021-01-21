/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.action.DuplicableOperatorAction
import com.maddyhome.idea.vim.action.ResetModeAction
import com.maddyhome.idea.vim.action.change.insert.InsertCompletedDigraphAction
import com.maddyhome.idea.vim.action.change.insert.InsertCompletedLiteralAction
import com.maddyhome.idea.vim.handler.ActionBeanClass
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RootNode
import org.jetbrains.annotations.TestOnly
import java.util.*
import javax.swing.KeyStroke

class CommandBuilder(private var currentCommandPartNode: CommandPartNode<ActionBeanClass>) {
  private val commandParts = ArrayDeque<Command>()
  private var keyList = mutableListOf<KeyStroke>()

  var commandState = CurrentCommandState.NEW_COMMAND
  var count = 0
    private set
  val keys: Iterable<KeyStroke> get() = keyList

  // The argument type for the current command part's action. Kept separate to handle digraphs and characters. We first
  // try to accept a digraph. If we get it, set expected argument type to character and handle the converted key. If we
  // fail to convert to a digraph, set expected argument type to character and try to handle the key again.
  var expectedArgumentType: Argument.Type? = null
    private set

  var prevExpectedArgumentType: Argument.Type? = null
    private set

  val isReady get() = commandState == CurrentCommandState.READY
  val isBad get() = commandState == CurrentCommandState.BAD_COMMAND
  val isEmpty get() = commandParts.isEmpty()
  val isAtDefaultState get() = isEmpty && count == 0 && expectedArgumentType == null

  val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  fun pushCommandPart(action: EditorActionHandlerBase) {
    commandParts.add(Command(count, action, action.type, action.flags))
    prevExpectedArgumentType = expectedArgumentType
    expectedArgumentType = action.argumentType
    count = 0
  }

  fun pushCommandPart(register: Char) {
    // We will never execute this command, but we need to push something to correctly handle counts on either side of a
    // select register command part. e.g. 2"a2d2w or even crazier 2"a2"a2"a2"a2"a2d2w
    commandParts.add(Command(count, register))
    expectedArgumentType = null
    count = 0
  }

  fun popCommandPart(): Command {
    val command = commandParts.removeLast()
    expectedArgumentType = if (commandParts.size > 0) commandParts.peekLast().action.argumentType else null
    return command
  }

  fun fallbackToCharacterArgument() {
    // Finished handling DIGRAPH. We either succeeded, in which case handle the converted character, or failed to parse,
    // in which case try to handle input as a character argument.
    assert(expectedArgumentType == Argument.Type.DIGRAPH) { "Cannot move state from $expectedArgumentType to CHARACTER"}
    expectedArgumentType = Argument.Type.CHARACTER
  }

  fun addKey(key: KeyStroke) {
    keyList.add(key)
  }

  fun addCountCharacter(key: KeyStroke) {
    count = (count * 10) + (key.keyChar - '0')
    // If count overflows and flips negative, reset to 999999999L. In Vim, count is a long, which is *usually* 32 bits,
    // so will flip at 2147483648. We store count as an Int, which is also 32 bit.
    // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
    if (count < 0) {
      count = 999999999
    }
    addKey(key)
  }

  fun deleteCountCharacter() {
    count /= 10
    keyList.removeAt(keyList.size - 1)
  }

  fun setCurrentCommandPartNode(newNode: CommandPartNode<ActionBeanClass>) {
    currentCommandPartNode = newNode
  }

  fun getChildNode(key: KeyStroke): Node<ActionBeanClass>? {
    return currentCommandPartNode[key]
  }

  fun isAwaitingCharOrDigraphArgument(): Boolean {
    if (commandParts.size == 0) return false
    val argumentType = commandParts.peekLast().action.argumentType
    return argumentType == Argument.Type.CHARACTER || argumentType == Argument.Type.DIGRAPH
  }

  fun isBuildingMultiKeyCommand(): Boolean {
    // Don't apply mapping if we're in the middle of building a multi-key command.
    // E.g. given nmap s v, don't try to map <C-W>s to <C-W>v
    //   Similarly, nmap <C-W>a <C-W>s should not try to map the second <C-W> in <C-W><C-W>
    // Note that we might still be at RootNode if we're handling a prefix, because we might be buffering keys until we
    // get a match. This means we'll still process the rest of the keys of the prefix.
    return currentCommandPartNode !is RootNode
  }

  fun isPuttingLiteral(): Boolean {
    return !commandParts.isEmpty() && commandParts.last.action is InsertCompletedLiteralAction
  }

  fun isDone(): Boolean {
    return commandParts.isEmpty()
  }

  fun completeCommandPart(argument: Argument) {
    commandParts.peekLast().argument = argument
    commandState = CurrentCommandState.READY
  }

  fun isDuplicateOperatorKeyStroke(key: KeyStroke): Boolean {
    val action = commandParts.peekLast().action as? DuplicableOperatorAction
    return action?.duplicateWith == key.keyChar
  }

  fun hasCurrentCommandPartArgument(): Boolean {
    return commandParts.peek()?.argument != null
  }

  fun buildCommand(): Command {
    if (commandParts.last.action is InsertCompletedDigraphAction || commandParts.last.action is ResetModeAction) {
      expectedArgumentType = prevExpectedArgumentType
      prevExpectedArgumentType = null
      return commandParts.removeLast()
    }

    var command: Command = commandParts.removeFirst()
    while (commandParts.size > 0) {
      val next = commandParts.removeFirst()
      next.count = if (command.rawCount == 0 && next.rawCount == 0) 0 else command.count * next.count
      command.count = 0
      if (command.type == Command.Type.SELECT_REGISTER) {
        next.register = command.register
        command.register = null
        command = next
      }
      else {
        command.argument = Argument(next)
        assert(commandParts.size == 0)
      }
    }
    expectedArgumentType = null
    return command
  }

  fun resetAll(commandPartNode: CommandPartNode<ActionBeanClass>) {
    resetInProgressCommandPart(commandPartNode)
    commandState = CurrentCommandState.NEW_COMMAND
    commandParts.clear()
    keyList.clear()
    expectedArgumentType = null
    prevExpectedArgumentType = null
  }

  fun resetInProgressCommandPart(commandPartNode: CommandPartNode<ActionBeanClass>) {
    count = 0
    setCurrentCommandPartNode(commandPartNode)
  }

  @TestOnly
  fun getCurrentTrie(): CommandPartNode<ActionBeanClass> = currentCommandPartNode
}
