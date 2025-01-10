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
import com.maddyhome.idea.vim.key.KeyStrokeTrie
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

class CommandBuilder private constructor(
  private var keyStrokeTrie: KeyStrokeTrie<LazyVimCommand>,
  private val counts: MutableList<Int>,
  private val typedKeyStrokes: MutableList<KeyStroke>,
  private val commandKeyStrokes: MutableList<KeyStroke>,
) : Cloneable {

  constructor(keyStrokeTrie: KeyStrokeTrie<LazyVimCommand>, initialUncommittedRawCount: Int = 0)
    : this(keyStrokeTrie, mutableListOf(initialUncommittedRawCount), mutableListOf(), mutableListOf())

  private var commandState: CurrentCommandState = CurrentCommandState.NEW_COMMAND
  private var selectedRegister: Char? = null
  private var action: EditorActionHandlerBase? = null
  private var argument: Argument? = null
  private var fallbackArgumentType: Argument.Type? = null

  private val motionArgument
    get() = argument as? Argument.Motion

  private var currentCount: Int
    get() = counts.last()
    set(value) {
      counts[counts.size - 1] = value
    }

  /** Provide the typed keys for `'showcmd'` */
  val keys: Iterable<KeyStroke> get() = typedKeyStrokes

  /** Returns true if the command builder is clean and ready to start building */
  val isEmpty
    get() = commandState == CurrentCommandState.NEW_COMMAND
      && selectedRegister == null
      && counts.size == 1
      && action == null
      && argument == null
      && fallbackArgumentType == null

  /** Returns true if the command is ready to be built and executed */
  val isReady
    get() = commandState == CurrentCommandState.READY

  /**
   * Returns the current total count, as the product of all entered count components. The value is not coerced.
   *
   * This value is not reliable! Please use [Command.rawCount] or [Command.count] instead of this function.
   *
   * This value is a snapshot of the count for a currently in-progress command, and should not be used for anything
   * other than reporting on the state of the command. This value is likely to change as the user continues entering the
   * command. There are very few expected uses of this value. Examples include calculating `'incsearch'` highlighting
   * for an in-progress search command, or the `v:count` and `v:count1` variables used during an expression mapping.
   *
   * The returned value is the product of all count components. In other words, given a command that is an
   * operator+motion, both the operator and motion can have a count, such as `2d3w`, which means delete the next six
   * words. Furthermore, Vim allows a count when selecting register, and it is valid to select register multiple times.
   * E.g., `2"a3"b4"c5d6w` will delete the next 720 words and save the text to the register `c`.
   *
   * The returned value is not coerced. If no count components are specified, the returned value is 0. If any components
   * are specified, the value will naturally be greater than 0.
   */
  fun calculateCount0Snapshot(): Int {
    return if (counts.all { it == 0 }) 0 else counts.map { it.coerceAtLeast(1) }.reduce { acc, i -> acc * i }
  }

  // This is used by the extension mapping handler, to select the current register before invoking the extension. We
  // need better handling of extensions so that they integrate better with half-built commands, either by finishing or
  // resetting the command.
  // This is also used by the `v:register` variable.
  val registerSnapshot: Char?
    get() = selectedRegister

  // TODO: Try to remove this too. Also used by extension handling
  fun hasCurrentCommandPartArgument() = motionArgument != null || argument != null

  // TODO: And remove this too. More extension special case code
  // It's used by the Matchit extension to incorrectly reset the command builder. Extensions need a way to properly
  // handle the command builder. I.e., they should act like expression mappings, which return keys to evaluate, or an
  // empty string to leave state as it is - either way, it's an explicit choice. Currently, extensions mostly ignore it
  fun resetCount() {
    counts[counts.size - 1] = 0
  }

  /**
   * The argument type for the current in-progress command part's action
   *
   * For digraph arguments, this can fall back to [Argument.Type.CHARACTER] if there isn't a digraph match.
   */
  val expectedArgumentType: Argument.Type?
    get() = fallbackArgumentType
      ?: motionArgument?.let { return it.motion.argumentType }
      ?: action?.argumentType

  /**
   * Returns true if the command builder is waiting for an argument
   *
   * The command builder might be waiting for the argument to a simple motion action such as `f`, waiting for a
   * character to move to, or it might be waiting for the argument to a motion that is itself an argument to an operator
   * argument. For example, the character argument to `f` in `df{character}`.
   */
  val isAwaitingArgument: Boolean
    get() = expectedArgumentType != null && (motionArgument?.let { it.argument == null } ?: (argument == null))

  fun fallbackToCharacterArgument() {
    logger.trace("fallbackToCharacterArgument is executed")
    // Finished handling DIGRAPH. We either succeeded, in which case handle the converted character, or failed to parse,
    // in which case try to handle input as a character argument.
    assert(expectedArgumentType == Argument.Type.DIGRAPH) { "Cannot move state from $expectedArgumentType to CHARACTER" }
    fallbackArgumentType = Argument.Type.CHARACTER
  }

  fun isAwaitingCharOrDigraphArgument(): Boolean {
    val awaiting = expectedArgumentType == Argument.Type.CHARACTER || expectedArgumentType == Argument.Type.DIGRAPH
    logger.debug { "Awaiting char or digraph: $awaiting" }
    return awaiting
  }

  val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        !isRegisterPending &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  /**
   * Returns true if the user has typed some count characters
   *
   * Used to know if `0` should be mapped or not. Vim allows "0" to be mapped, but not while entering a count. Also used
   * to know if there are count characters available to delete.
   */
  fun hasCountCharacters() = currentCount > 0

  fun addCountCharacter(key: KeyStroke) {
    currentCount = (currentCount * 10) + (key.keyChar - '0')
    // If count overflows and flips negative, reset to 999999999L. In Vim, count is a long, which is *usually* 32 bits,
    // so will flip at 2147483648. We store count as an Int, which is also 32 bit.
    // See https://github.com/vim/vim/blob/b376ace1aeaa7614debc725487d75c8f756dd773/src/normal.c#L631
    if (currentCount < 0) {
      currentCount = 999999999
    }
    addTypedKeyStroke(key)
  }

  fun deleteCountCharacter() {
    currentCount /= 10
    typedKeyStrokes.removeLast()
  }

  var isRegisterPending: Boolean = false
    private set

  fun startWaitingForRegister(key: KeyStroke) {
    isRegisterPending = true
    addTypedKeyStroke(key)
  }

  fun selectRegister(register: Char) {
    logger.trace { "Selected register '$register'" }
    selectedRegister = register
    isRegisterPending = false
    fallbackArgumentType = null
    counts.add(0)
  }

  /**
   * Adds a keystroke to the command builder
   *
   * Only public use is when entering a digraph/literal, where each key isn't handled by [CommandBuilder], but should
   * be added to the `'showcmd'` output.
   */
  fun addTypedKeyStroke(key: KeyStroke) {
    logger.trace { "added key to command builder: $key" }
    typedKeyStrokes.add(key)
  }

  /**
   * Add an action to the command
   *
   * This can be an action such as delete the current character - `x`, a motion like `w`, an operator like `d` or a
   * motion that will be used as the argument of an operator - the `w` in `dw`.
   */
  fun addAction(action: EditorActionHandlerBase) {
    logger.trace { "addAction is executed. action = $action" }

    if (this.action == null) {
      this.action = action
    } else {
      StrictMode.assert(argument == null, "Command builder already has an action and a fully populated argument")
      argument = when (action) {
        is MotionActionHandler -> Argument.Motion(action, null)
        is TextObjectActionHandler -> Argument.Motion(action)
        is ExternalActionHandler -> Argument.Motion(action)
        else -> throw RuntimeException("Unexpected action type: $action")
      }
    }

    // Push a new count component, so we get an extra count for e.g. an operator's motion
    counts.add(0)
    fallbackArgumentType = null

    if (!isAwaitingArgument) {
      logger.trace("Action does not require an argument. Setting command state to READY")
      commandState = CurrentCommandState.READY
    }
  }

  /**
   * Add an argument to the command
   *
   * This might be a simple character argument, such as `x` in `fx`, or an ex-string argument to a search motion, like
   * `d/foo`. If the command is an operator+motion, the motion is both an action and an argument. While it is simpler
   * to use [addAction], it will still work if the motion action can also be wrapped in an [Argument.Motion] and passed
   * to [addArgument].
   */
  fun addArgument(argument: Argument) {
    logger.trace("addArgument is executed")

    // If the command's action is an operator, the argument will be a motion, which might be waiting for its argument.
    // If so, update the motion argument to include the given argument
    this.argument = motionArgument?.withArgument(argument) ?: argument

    fallbackArgumentType = null

    if (!isAwaitingArgument) {
      logger.trace("Argument is simple type, or motion with own argument. No further argument required. Setting command state to READY")
      commandState = CurrentCommandState.READY
    }
  }

  /**
   * Process a keystroke, matching an action if available
   *
   * If the given keystroke matches an action, the [processor] is invoked with the action instance. Typically, the
   * caller will end up passing the action back to [addAction], but there are more housekeeping steps that stop us
   * encapsulating it completely.
   *
   * If the given keystroke does not yet match an action, the internal state is updated to track the current command
   * part node.
   */
  fun processKey(key: KeyStroke, processor: (EditorActionHandlerBase) -> Unit): Boolean {
    commandKeyStrokes.add(key)
    val node = keyStrokeTrie.getTrieNode(commandKeyStrokes)
    if (node == null) {
      logger.trace { "No command or part command for key sequence: ${injector.parser.toPrintableString(commandKeyStrokes)}" }
      commandKeyStrokes.clear()
      return false
    }

    addTypedKeyStroke(key)

    val command = node.data
    if (command == null) {
      logger.trace { "Found unfinished key sequence for ${injector.parser.toPrintableString(commandKeyStrokes)} - ${node.debugString}" }
      return true
    }

    logger.trace { "Found command for ${injector.parser.toPrintableString(commandKeyStrokes)} - ${node.debugString}" }
    commandKeyStrokes.clear()
    processor(command.instance)
    return true
  }

  /**
   * Map a keystroke that duplicates an operator into the `_` "current line" motion
   *
   * Some commands like `dd` or `yy` or `cc` are treated as special cases by Vim. There is no `d`, `y` or `c` motion,
   * so for convenience, Vim maps the repeated operator keystroke as meaning "operate on the current line", and replaces
   * the second keystroke with the `_` motion. I.e. `dd` becomes `d_`, `yy` becomes `y_`, `cc` becomes `c_`, etc.
   *
   * @see DuplicableOperatorAction
   */
  fun convertDuplicateOperatorKeyStrokeToMotion(key: KeyStroke): KeyStroke {
    logger.trace { "convertDuplicateOperatorKeyStrokeToMotion is executed. key = $key" }

    // Simple check to ensure that we're in OP_PENDING. If we don't have an action, we don't have an operator. If we
    // have an argument, we can't be in OP_PENDING
    if (action != null && argument == null) {
      (action as? DuplicableOperatorAction)?.let {
        logger.trace { "action = $action" }
        if (it.duplicateWith == key.keyChar) {
          return KeyStroke.getKeyStroke('_')
        }
      }
    }
    return key
  }

  fun isBuildingMultiKeyCommand(): Boolean {
    // Don't apply mapping if we're in the middle of building a multi-key command.
    // E.g. given nmap s v, don't try to map <C-W>s to <C-W>v
    //   Similarly, nmap <C-W>a <C-W>s should not try to map the second <C-W> in <C-W><C-W>
    // Note that we might still be at RootNode if we're handling a prefix, because we might be buffering keys until we
    // get a match. This means we'll still process the rest of the keys of the prefix.
    val isMultikey = commandKeyStrokes.isNotEmpty()
    logger.debug { "Building multikey command: $commandKeyStrokes" }
    return isMultikey
  }

  /**
   * Build the command with the current counts, register, actions and arguments
   *
   * The command builder is reset after the command is built.
   */
  fun buildCommand(): Command {
    val rawCount = calculateCount0Snapshot()
    val command = Command(selectedRegister, rawCount, action!!, argument, action!!.type, action?.flags ?: noneOfEnum())
    resetAll(keyStrokeTrie)
    return command
  }

  fun resetAll(keyStrokeTrie: KeyStrokeTrie<LazyVimCommand>) {
    logger.trace("resetAll is executed")
    this.keyStrokeTrie = keyStrokeTrie
    commandState = CurrentCommandState.NEW_COMMAND
    commandKeyStrokes.clear()
    counts.clear()
    counts.add(0)
    isRegisterPending = false
    selectedRegister = null
    action = null
    argument = null
    typedKeyStrokes.clear()
    fallbackArgumentType = null
  }

  /**
   * Change the command trie root node used to find commands for the current mode
   *
   * Typically, we reset the command trie root node after a command is executed, using the root node of the current
   * mode - this is handled by [resetAll]. This function allows us to change the root node without executing a command
   * or fully resetting the command builder, such as when switching to Op-pending while entering an operator+motion.
   */
  fun resetCommandTrie(keyStrokeTrie: KeyStrokeTrie<LazyVimCommand>) {
    logger.trace("resetCommandTrieRootNode is executed")
    this.keyStrokeTrie = keyStrokeTrie
  }

  @TestOnly
  fun getCurrentTrie(): KeyStrokeTrie<LazyVimCommand> = keyStrokeTrie

  @TestOnly
  fun getCurrentCommandKeys(): List<KeyStroke> = commandKeyStrokes

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CommandBuilder

    if (keyStrokeTrie != other.keyStrokeTrie) return false
    if (counts != other.counts) return false
    if (selectedRegister != other.selectedRegister) return false
    if (action != other.action) return false
    if (argument != other.argument) return false
    if (typedKeyStrokes != other.typedKeyStrokes) return false
    if (commandState != other.commandState) return false
    if (expectedArgumentType != other.expectedArgumentType) return false
    if (fallbackArgumentType != other.fallbackArgumentType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = keyStrokeTrie.hashCode()
    result = 31 * result + counts.hashCode()
    result = 31 * result + selectedRegister.hashCode()
    result = 31 * result + action.hashCode()
    result = 31 * result + argument.hashCode()
    result = 31 * result + typedKeyStrokes.hashCode()
    result = 31 * result + commandState.hashCode()
    result = 31 * result + expectedArgumentType.hashCode()
    result = 31 * result + fallbackArgumentType.hashCode()
    return result
  }

  public override fun clone(): CommandBuilder {
    val result = CommandBuilder(
      keyStrokeTrie,
      counts.toMutableList(),
      typedKeyStrokes.toMutableList(),
      commandKeyStrokes.toMutableList()
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
      "key list = ${injector.parser.toKeyNotation(typedKeyStrokes)}, " +
      "selected register = $selectedRegister, " +
      "counts = $counts, " +
      "action = $action, " +
      "argument = $argument, " +
      "command part node - $keyStrokeTrie"
  }

  companion object {
    private val logger = vimLogger<CommandBuilder>()
  }
}
