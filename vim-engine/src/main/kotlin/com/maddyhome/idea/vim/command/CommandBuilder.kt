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
import com.maddyhome.idea.vim.handler.ExternalActionHandler
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RootNode
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

class CommandBuilder private constructor(
  private var currentCommandPartNode: CommandPartNode<LazyVimCommand>,
  private val counts: MutableList<Int>,
  private val keyList: MutableList<KeyStroke>,
) : Cloneable {

  constructor(
    currentCommandPartNode: CommandPartNode<LazyVimCommand>,
    initialUncommittedRawCount: Int = 0
  ) : this(
    currentCommandPartNode,
    mutableListOf(initialUncommittedRawCount),
    mutableListOf(),
  )

  private var selectedRegister: Char? = null
  private var action: EditorActionHandlerBase? = null
  private var argument: Argument? = null
  private var fallbackArgumentType: Argument.Type? = null

  private var currentCount: Int
    get() = counts.last()
    set(value) {
      counts[counts.size - 1] = value
    }

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
  val count: Int
    get() = counts.last()

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
    get() = counts.map { it.coerceAtLeast(1) }.reduce { acc, i -> acc * i }

  val keys: Iterable<KeyStroke> get() = keyList

  // TODO: Try to remove this. We shouldn't be looking at the unbuilt command
  // This is used by the extension mapping handler, to select the current register before invoking the extension. We
  // need better handling of extensions so that they integrate better with half-built commands, either by finishing or
  // resetting the command.
  // If we keep this, consider renaming to something like `uncommittedRegister`, to reflect that the register could
  // still change, if more keys are processed. E.g., it's perfectly valid to select register multiple times `"a"b`.
  // This doesn't cause any issues with existing extensions
  val register: Char?
    get() = selectedRegister

  /**
   * The argument type for the current in-progress command part's action
   *
   * For digraph arguments, this can fall back to [Argument.Type.CHARACTER] if there isn't a digraph match.
   */
  val expectedArgumentType: Argument.Type?
    get() = fallbackArgumentType
      ?: (argument as? Argument.Motion)?.let { return it.motion.argumentType }
      ?: action?.argumentType

  // TODO: Review all of these
  val isReady: Boolean get() = commandState == CurrentCommandState.READY
  val isEmpty: Boolean get() = selectedRegister == null && counts.size == 1 && action == null && argument == null
  val isAtDefaultState: Boolean get() = isEmpty && count == 0 && expectedArgumentType == null
  fun isDone() = isEmpty

  val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  fun pushCommandPart(action: EditorActionHandlerBase) {
    logger.trace { "pushCommandPart is executed. action = $action" }
    if (this.action == null) {
      this.action = action
    }
    else {
      StrictMode.assert(argument == null, "Command builder already has an action and a fully populated argument")
      argument = when (action) {
        is MotionActionHandler -> Argument.Motion(action, null)
        is TextObjectActionHandler -> Argument.Motion(action)
        is ExternalActionHandler -> Argument.Motion(action)
        else -> throw RuntimeException("Unexpected action type: $action")
      }
    }
    counts.add(0)
    fallbackArgumentType = null
  }

  fun pushCommandPart(register: Char) {
    logger.trace { "pushCommandPart is executed. register = $register" }
    selectedRegister = register
    fallbackArgumentType = null
    counts.add(0)
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
    currentCount = (currentCount * 10) + (key.keyChar - '0')
    // If count overflows and flips negative, reset to 999999999L. In Vim, count is a long, which is *usually* 32 bits,
    // so will flip at 2147483648. We store count as an Int, which is also 32 bit.
    // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
    if (currentCount < 0) {
      currentCount = 999999999
    }
    addKey(key)
  }

  fun deleteCountCharacter() {
    currentCount /= 10
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

  fun completeCommandPart(argument: Argument) {
    logger.trace { "completeCommandPart is executed" }
    this.argument = (this.argument as? Argument.Motion)?.withArgument(argument) ?: argument
    commandState = CurrentCommandState.READY
  }

  fun isDuplicateOperatorKeyStroke(key: KeyStroke): Boolean {
    logger.trace { "entered isDuplicateOperatorKeyStroke" }
    val action = (argument as? Argument.Motion)?.motion as? DuplicableOperatorAction
      ?: action as? DuplicableOperatorAction
    logger.trace { "action = $action" }
    return action?.duplicateWith == key.keyChar
  }

  fun hasCurrentCommandPartArgument() = argument != null

  fun buildCommand(): Command {
    val rawCount = if (counts.all { it == 0 }) 0 else counts.map { it.coerceAtLeast(1) }.reduce { acc, i -> acc * i }
    val command = Command(selectedRegister, rawCount, action!!, argument, action!!.type, action?.flags ?: noneOfEnum())
    resetAll(currentCommandPartNode)
    return command
  }

  fun resetAll(commandPartNode: CommandPartNode<LazyVimCommand>) {
    logger.trace { "resetAll is executed" }
    resetInProgressCommandPart(commandPartNode)
    commandState = CurrentCommandState.NEW_COMMAND
    counts.clear()
    counts.add(0)
    selectedRegister = null
    action = null
    argument = null
    keyList.clear()
    fallbackArgumentType = null
  }

  // TODO: Get rid of this
  // It's used by the Matchit extension to incorrectly reset the command builder. Extensions need a way to properly
  // handle the command builder. I.e., they should act like expression mappings, which return keys to evaluate, or an
  // empty string to leave state as it is - either way, it's an explicit choice. Currently, extensions mostly ignore it
  fun resetCount() {
    counts[counts.size - 1] = 0
  }

  fun resetInProgressCommandPart(commandPartNode: CommandPartNode<LazyVimCommand>) {
    logger.trace { "resetInProgressCommandPart is executed" }
    counts[counts.size - 1] = 0
    setCurrentCommandPartNode(commandPartNode)
  }

  @TestOnly
  fun getCurrentTrie(): CommandPartNode<LazyVimCommand> = currentCommandPartNode

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CommandBuilder

    if (currentCommandPartNode != other.currentCommandPartNode) return false
    if (counts != other.counts) return false
    if (selectedRegister != other.selectedRegister) return false
    if (action != other.action) return false
    if (argument != other.argument) return false
    if (keyList != other.keyList) return false
    if (commandState != other.commandState) return false
    if (expectedArgumentType != other.expectedArgumentType) return false
    if (fallbackArgumentType != other.fallbackArgumentType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = currentCommandPartNode.hashCode()
    result = 31 * result + counts.hashCode()
    result = 31 * result + selectedRegister.hashCode()
    result = 31 * result + action.hashCode()
    result = 31 * result + argument.hashCode()
    result = 31 * result + keyList.hashCode()
    result = 31 * result + commandState.hashCode()
    result = 31 * result + expectedArgumentType.hashCode()
    result = 31 * result + fallbackArgumentType.hashCode()
    return result
  }

  public override fun clone(): CommandBuilder {
    val result = CommandBuilder(
      currentCommandPartNode,
      counts.toMutableList(),
      keyList.toMutableList()
    )
    result.selectedRegister = selectedRegister
    result.action = action
    result.argument = argument
    result.commandState = commandState
    result.fallbackArgumentType = fallbackArgumentType
    return result
  }

  override fun toString(): String {
    return "Command state = $commandState, " +
      "key list = ${ injector.parser.toKeyNotation(keyList) }, " +
      "selected register = $selectedRegister, " +
      "counts = $counts, " +
      "action = $action, " +
      "argument = $argument, " +
      "command part node - $currentCommandPartNode"
  }

  companion object {
    private val logger = vimLogger<CommandBuilder>()
  }
}
