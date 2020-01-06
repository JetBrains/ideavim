package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.action.DuplicableOperatorAction
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase
import com.maddyhome.idea.vim.key.CommandPartNode
import com.maddyhome.idea.vim.key.Node
import com.maddyhome.idea.vim.key.RootNode
import java.util.*
import javax.swing.KeyStroke

class CommandBuilder(private var currentCommandPartNode: CommandPartNode) {
  private val commandParts = Stack<Command>()
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

  val isReady get() = commandState == CurrentCommandState.READY
  val isBad get() = commandState == CurrentCommandState.BAD_COMMAND
  val isEmpty get() = commandParts.empty()
  val isAtDefaultState get() = isEmpty && count == 0 && expectedArgumentType == null

  val isExpectingCount: Boolean
    get() {
      return commandState == CurrentCommandState.NEW_COMMAND &&
        expectedArgumentType != Argument.Type.CHARACTER &&
        expectedArgumentType != Argument.Type.DIGRAPH
    }

  fun pushCommandPart(action: EditorActionHandlerBase) {
    commandParts.push(Command(count, action, action.type, action.flags))
    expectedArgumentType = action.argumentType
  }

  fun popCommandPart() {
    commandParts.pop()
    expectedArgumentType = if (commandParts.size > 0) commandParts.peek().action.argumentType else null
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

  fun setCurrentCommandPartNode(newNode: CommandPartNode) {
    currentCommandPartNode = newNode
  }

  fun getChildNode(key: KeyStroke): Node? {
    return currentCommandPartNode[key]
  }

  fun isAwaitingCharOrDigraphArgument(): Boolean {
    if (commandParts.size == 0) return false
    val argumentType = commandParts.peek().action.argumentType
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

  fun completeCommandPart(argument: Argument) {
    commandParts.peek().argument = argument
    commandState = CurrentCommandState.READY
  }

  fun isDuplicateOperatorKeyStroke(key: KeyStroke): Boolean {
    val action = commandParts.peek()?.action as? DuplicableOperatorAction
    return action?.duplicateWith == key.keyChar
  }

  fun hasCurrentCommandPartArgument(): Boolean {
    return commandParts.peek()?.argument != null
  }

  fun buildCommand(): Command {
    /* Let's go through the command stack and merge it all into one command. At this time there should never
       be more than two commands on the stack - one is the actual command, and the other would be a motion
       command argument needed by the first command */
    var command: Command = commandParts.pop()
    while (commandParts.size > 0) {
      val top: Command = commandParts.pop()
      top.argument = Argument(command)
      command = top
    }
    return fixCommandCounts(command)
  }

  private fun fixCommandCounts(command: Command): Command {
    // If we have a command with a motion command argument, both could have their own counts. We need to adjust the
    // counts, so the motion gets the product of both counts, and the count associated with the command gets reset.
    // E.g. 3c2w (change 2 words, three times) becomes c6w (change 6 words)
    if (command.argument?.type === Argument.Type.MOTION) {
      val motion = command.argument!!.motion
      motion.count = if (command.rawCount == 0 && motion.rawCount == 0) 0 else command.count * motion.count
      command.count = 0
    }
    return command
  }

  fun resetAll(commandPartNode: CommandPartNode) {
    resetInProgressCommandPart(commandPartNode)
    commandState = CurrentCommandState.NEW_COMMAND
    commandParts.clear()
    keyList.clear()
    expectedArgumentType = null
  }

  fun resetInProgressCommandPart(commandPartNode: CommandPartNode) {
    count = 0
    setCurrentCommandPartNode(commandPartNode)
  }
}