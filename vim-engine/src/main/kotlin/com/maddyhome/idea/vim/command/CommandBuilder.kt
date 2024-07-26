/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.action.change.LazyVimCommand
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CurrentCommandState
import com.maddyhome.idea.vim.diagnostic.debug
import com.maddyhome.idea.vim.diagnostic.trace
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RootNode
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

class CommandBuilder(
  private var currentCommandPartNode: CommandPartNode<LazyVimCommand>,
  private val commandParts: ArrayDeque<Command>,
  private val keyList: MutableList<KeyStroke>,
  initialUncommittedRawCount: Int,
) : Cloneable {

  constructor(
    currentCommandPartNode: CommandPartNode<LazyVimCommand>,
    initialUncommittedRawCount: Int = 0
  ) : this(
    currentCommandPartNode,
    ArrayDeque(),
    mutableListOf(),
    initialUncommittedRawCount
  )

  var commandState: CurrentCommandState = CurrentCommandState.NEW_COMMAND

  /**
   * The current uncommitted count for the currently in-progress command part
   *
   * TODO: Investigate usages. This value cannot be trusted
   * TODO: Rename to uncommittedRawCount
   *
   * This value is not coerced, and can be 0.
   *
   * There are very few reasons for using this value. It is incomplete (the user could type another digit), and there
   * can be other committed command parts, such as operator and multiple register selections, each of which will can a
   * count (e.g., `2"a3"b4"c5d6` waiting for a motion). The count is only final after [buildCommand], and then only via
   * [Command.count] or [Command.rawCount].
   *
   * The [aggregatedUncommittedCount] property can be used to get the current total count across all command parts,
   * although this value is also not guaranteed to be final.
   */
  var count: Int = initialUncommittedRawCount
    private set

  /**
   * The current aggregated, but uncommitted count for all command parts in the command builder, coerced to 1
   *
   * This value multiplies together the count for command parts currently committed, such as operator and multiple
   * register selections, as well as the current uncommitted count for the next command part. E.g., `2"a3"b4"c5d6` will
   * multiply each count together to get what would be the final count. All counts are coerced to at least 1 before
   * multiplying, which means the result will also be at least 1.
   *
   * Note that there are very few uses for this value. The final value should be retrieved from [Command.count] or
   * [Command.rawCount] after a call to [buildCommand]. This value is expected to be used for `'incsearch'`
   * highlighting.
   */
  val aggregatedUncommittedCount: Int
    get() = (commandParts.map { it.count }.reduceOrNull { acc, i -> acc * i } ?: 1) * count.coerceAtLeast(1)

  val keys: Iterable<KeyStroke> get() = keyList

  // TODO: Try to remove this. We shouldn't be looking at the unbuilt command
  // This is used by the extension mapping handler, to select the current register before invoking the extension. We
  // need better handling of extensions so that they integrate better with half-built commands, either by finishing or
  // resetting the command.
  // If we keep this, consider renaming to something like `uncommittedRegister`, to reflect that the register could
  // still change, if more keys are processed. E.g., it's perfectly valid to select register multiple times `"a"b`.
  // This doesn't cause any issues with existing extensions
  val register: Char?
    get() = commandParts.lastOrNull { it.register != null }?.register

  /**
   * The argument type for the current in-progress command part's action
   *
   * For digraph arguments, this can fall back to [Argument.Type.CHARACTER] if there isn't a digraph match.
   */
  val expectedArgumentType: Argument.Type?
    get() = fallbackArgumentType ?: commandParts.lastOrNull()?.action?.argumentType

  private var fallbackArgumentType: Argument.Type? = null

  val isReady: Boolean get() = commandState == CurrentCommandState.READY
  val isEmpty: Boolean get() = commandParts.isEmpty()
  val isAtDefaultState: Boolean get() = isEmpty && count == 0 && expectedArgumentType == null

  val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  fun pushCommandPart(action: EditorActionHandlerBase) {
    logger.trace { "pushCommandPart is executed. action = $action" }
    commandParts.add(Command(count, action, action.type, action.flags))
    fallbackArgumentType = null
    count = 0
  }

  fun pushCommandPart(register: Char) {
    logger.trace { "pushCommandPart is executed. register = $register" }
    // We will never execute this command, but we need to push something to correctly handle counts on either side of a
    // select register command part. e.g. 2"a2d2w or even crazier 2"a2"a2"a2"a2"a2d2w
    commandParts.add(Command(count, register))
    fallbackArgumentType = null
    count = 0
  }

  fun fallbackToCharacterArgument() {
    logger.trace { "fallbackToCharacterArgument is executed" }
    // Finished handling DIGRAPH. We either succeeded, in which case handle the converted character, or failed to parse,
    // in which case try to handle input as a character argument.
    assert(expectedArgumentType == Argument.Type.DIGRAPH) { "Cannot move state from $expectedArgumentType to CHARACTER" }
    fallbackArgumentType = Argument.Type.CHARACTER
  }

  fun addKey(key: KeyStroke) {
    logger.trace { "added key to command builder" }
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

  fun setCurrentCommandPartNode(newNode: CommandPartNode<LazyVimCommand>) {
    logger.trace { "setCurrentCommandPartNode is executed" }
    currentCommandPartNode = newNode
  }

  fun getChildNode(key: KeyStroke): Node<LazyVimCommand>? {
    return currentCommandPartNode[key]
  }

  fun isAwaitingCharOrDigraphArgument(): Boolean {
    val awaiting = expectedArgumentType == Argument.Type.CHARACTER || expectedArgumentType == Argument.Type.DIGRAPH
    logger.debug { "Awaiting char or digraph: $awaiting" }
    return awaiting
  }

  fun isBuildingMultiKeyCommand(): Boolean {
    // Don't apply mapping if we're in the middle of building a multi-key command.
    // E.g. given nmap s v, don't try to map <C-W>s to <C-W>v
    //   Similarly, nmap <C-W>a <C-W>s should not try to map the second <C-W> in <C-W><C-W>
    // Note that we might still be at RootNode if we're handling a prefix, because we might be buffering keys until we
    // get a match. This means we'll still process the rest of the keys of the prefix.
    val isMultikey = currentCommandPartNode !is RootNode
    logger.debug { "Building multikey command: $isMultikey" }
    return isMultikey
  }

  fun isDone(): Boolean {
    return commandParts.isEmpty()
  }

  fun completeCommandPart(argument: Argument) {
    logger.trace { "completeCommandPart is executed" }
    commandParts.last().argument = argument
    commandState = CurrentCommandState.READY
  }

  fun isDuplicateOperatorKeyStroke(key: KeyStroke): Boolean {
    logger.trace { "entered isDuplicateOperatorKeyStroke" }
    val action = commandParts.last().action as? DuplicableOperatorAction
    logger.trace { "action = $action" }
    return action?.duplicateWith == key.keyChar
  }

  fun hasCurrentCommandPartArgument(): Boolean {
    return commandParts.lastOrNull()?.argument != null
  }

  fun buildCommand(): Command {
    var command: Command = commandParts.removeFirst()
    while (commandParts.size > 0) {
      val next = commandParts.removeFirst()
      next.rawCount = if (command.rawCount == 0 && next.rawCount == 0) 0 else command.count * next.count
      command.rawCount = 0
      if (command.type == Command.Type.SELECT_REGISTER) {
        next.register = command.register
        command.register = null
        command = next
      } else {
        command.argument = Argument.MotionAction(next)
        assert(commandParts.size == 0)
      }
    }
    fallbackArgumentType = null
    return command
  }

  fun resetAll(commandPartNode: CommandPartNode<LazyVimCommand>) {
    logger.trace { "resetAll is executed" }
    resetInProgressCommandPart(commandPartNode)
    commandState = CurrentCommandState.NEW_COMMAND
    commandParts.clear()
    keyList.clear()
    fallbackArgumentType = null
  }

  fun resetCount() {
    count = 0
  }

  fun resetInProgressCommandPart(commandPartNode: CommandPartNode<LazyVimCommand>) {
    logger.trace { "resetInProgressCommandPart is executed" }
    count = 0
    setCurrentCommandPartNode(commandPartNode)
  }

  @TestOnly
  fun getCurrentTrie(): CommandPartNode<LazyVimCommand> = currentCommandPartNode
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
    if (fallbackArgumentType != other.fallbackArgumentType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = currentCommandPartNode.hashCode()
    result = 31 * result + commandParts.hashCode()
    result = 31 * result + keyList.hashCode()
    result = 31 * result + commandState.hashCode()
    result = 31 * result + count
    result = 31 * result + (expectedArgumentType?.hashCode() ?: 0)
    result = 31 * result + (fallbackArgumentType?.hashCode() ?: 0)
    return result
  }

  public override fun clone(): CommandBuilder {
    val result = CommandBuilder(currentCommandPartNode, ArrayDeque(commandParts), keyList.toMutableList(), count)
    result.commandState = commandState
    result.fallbackArgumentType = fallbackArgumentType
    return result
  }

  override fun toString(): String {
    return "Command state = $commandState, key list = ${ injector.parser.toKeyNotation(keyList) }, command parts = ${ commandParts }, count = $count\n" +
      "command part node - $currentCommandPartNode"
  }

  companion object {
    private val logger = vimLogger<CommandBuilder>()
  }
}
