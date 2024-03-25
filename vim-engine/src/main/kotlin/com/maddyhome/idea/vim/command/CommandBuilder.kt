/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.common.CurrentCommandState
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RootNode
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

public class CommandBuilder(private var currentCommandPartNode: CommandPartNode<LazyVimCommand>): Cloneable {
  private var commandParts = ArrayDeque<Command>()
  private var keyList = mutableListOf<KeyStroke>()

  public var commandState: CurrentCommandState = CurrentCommandState.NEW_COMMAND
  public var count: Int = 0
    private set
  public val keys: Iterable<KeyStroke> get() = keyList
  public val register: Char?
    get() = commandParts.lastOrNull()?.register

  // The argument type for the current command part's action. Kept separate to handle digraphs and characters. We first
  // try to accept a digraph. If we get it, set expected argument type to character and handle the converted key. If we
  // fail to convert to a digraph, set expected argument type to character and try to handle the key again.
  public var expectedArgumentType: Argument.Type? = null
    private set

  private var prevExpectedArgumentType: Argument.Type? = null

  public val isReady: Boolean get() = commandState == CurrentCommandState.READY
  public val isBad: Boolean get() = commandState == CurrentCommandState.BAD_COMMAND
  public val isEmpty: Boolean get() = commandParts.isEmpty()
  public val isAtDefaultState: Boolean get() = isEmpty && count == 0 && expectedArgumentType == null

  public val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  public fun pushCommandPart(action: EditorActionHandlerBase) {
    commandParts.add(Command(count, action, action.type, action.flags))
    prevExpectedArgumentType = expectedArgumentType
    expectedArgumentType = action.argumentType
    count = 0
  }

  public fun pushCommandPart(register: Char) {
    // We will never execute this command, but we need to push something to correctly handle counts on either side of a
    // select register command part. e.g. 2"a2d2w or even crazier 2"a2"a2"a2"a2"a2d2w
    commandParts.add(Command(count, register))
    expectedArgumentType = null
    count = 0
  }

  public fun popCommandPart(): Command {
    val command = commandParts.removeLast()
    expectedArgumentType = if (commandParts.size > 0) commandParts.last().action.argumentType else null
    return command
  }

  public fun fallbackToCharacterArgument() {
    // Finished handling DIGRAPH. We either succeeded, in which case handle the converted character, or failed to parse,
    // in which case try to handle input as a character argument.
    assert(expectedArgumentType == Argument.Type.DIGRAPH) { "Cannot move state from $expectedArgumentType to CHARACTER" }
    expectedArgumentType = Argument.Type.CHARACTER
  }

  public fun addKey(key: KeyStroke) {
    keyList.add(key)
  }

  public fun addCountCharacter(key: KeyStroke) {
    count = (count * 10) + (key.keyChar - '0')
    // If count overflows and flips negative, reset to 999999999L. In Vim, count is a long, which is *usually* 32 bits,
    // so will flip at 2147483648. We store count as an Int, which is also 32 bit.
    // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
    if (count < 0) {
      count = 999999999
    }
    addKey(key)
  }

  public fun deleteCountCharacter() {
    count /= 10
    keyList.removeAt(keyList.size - 1)
  }

  public fun setCurrentCommandPartNode(newNode: CommandPartNode<LazyVimCommand>) {
    currentCommandPartNode = newNode
  }

  public fun getChildNode(key: KeyStroke): Node<LazyVimCommand>? {
    return currentCommandPartNode[key]
  }

  public fun isAwaitingCharOrDigraphArgument(): Boolean {
    if (commandParts.size == 0) return false
    val argumentType = commandParts.last().action.argumentType
    val awaiting = argumentType == Argument.Type.CHARACTER || argumentType == Argument.Type.DIGRAPH
    LOG.debug { "Awaiting char of digraph: $awaiting" }
    return awaiting
  }

  public fun isBuildingMultiKeyCommand(): Boolean {
    // Don't apply mapping if we're in the middle of building a multi-key command.
    // E.g. given nmap s v, don't try to map <C-W>s to <C-W>v
    //   Similarly, nmap <C-W>a <C-W>s should not try to map the second <C-W> in <C-W><C-W>
    // Note that we might still be at RootNode if we're handling a prefix, because we might be buffering keys until we
    // get a match. This means we'll still process the rest of the keys of the prefix.
    val isMultikey = currentCommandPartNode !is RootNode
    LOG.debug { "Building multikey command: $isMultikey" }
    return isMultikey
  }

  public fun isPuttingLiteral(): Boolean {
    return !commandParts.isEmpty() && commandParts.last().action.id == "VimInsertCompletedLiteralAction"
  }

  public fun isDone(): Boolean {
    return commandParts.isEmpty()
  }

  public fun completeCommandPart(argument: Argument) {
    commandParts.last().argument = argument
    commandState = CurrentCommandState.READY
  }

  public fun isDuplicateOperatorKeyStroke(key: KeyStroke): Boolean {
    val action = commandParts.last().action as? DuplicableOperatorAction
    return action?.duplicateWith == key.keyChar
  }

  public fun hasCurrentCommandPartArgument(): Boolean {
    return commandParts.firstOrNull()?.argument != null
  }

  public fun buildCommand(): Command {
    if (commandParts.last().action.id == "VimInsertCompletedDigraphAction" || commandParts.last().action.id == "VimResetModeAction") {
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
      } else {
        command.argument = Argument(next)
        assert(commandParts.size == 0)
      }
    }
    expectedArgumentType = null
    return command
  }

  public fun resetAll(commandPartNode: CommandPartNode<LazyVimCommand>) {
    resetInProgressCommandPart(commandPartNode)
    commandState = CurrentCommandState.NEW_COMMAND
    commandParts.clear()
    keyList.clear()
    expectedArgumentType = null
    prevExpectedArgumentType = null
  }

  public fun resetCount() {
    count = 0
  }

  public fun resetInProgressCommandPart(commandPartNode: CommandPartNode<LazyVimCommand>) {
    count = 0
    setCurrentCommandPartNode(commandPartNode)
  }

  @TestOnly
  public fun getCurrentTrie(): CommandPartNode<LazyVimCommand> = currentCommandPartNode
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CommandBuilder

    if (currentCommandPartNode != other.currentCommandPartNode) return false
    if (commandParts != other.commandParts) return false
    if (keyList != other.keyList) return false
    if (commandState != other.commandState) return false
    if (count != other.count) return false
    if (expectedArgumentType != other.expectedArgumentType) return false
    if (prevExpectedArgumentType != other.prevExpectedArgumentType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = currentCommandPartNode.hashCode()
    result = 31 * result + commandParts.hashCode()
    result = 31 * result + keyList.hashCode()
    result = 31 * result + commandState.hashCode()
    result = 31 * result + count
    result = 31 * result + (expectedArgumentType?.hashCode() ?: 0)
    result = 31 * result + (prevExpectedArgumentType?.hashCode() ?: 0)
    return result
  }

  public override fun clone(): CommandBuilder {
    val result = CommandBuilder(currentCommandPartNode)
    result.commandParts = ArrayDeque(commandParts)
    result.keyList = keyList.toMutableList()
    result.commandState = commandState
    result.count = count
    result.expectedArgumentType = expectedArgumentType
    result.prevExpectedArgumentType = prevExpectedArgumentType

    return result
  }

  public companion object {
    private val LOG = vimLogger<CommandBuilder>()
  }
}
