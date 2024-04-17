/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.MissingRangeException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import java.util.*

public sealed class Command(protected var commandRange: Range, public val commandArgument: String) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  protected abstract val argFlags: CommandHandlerFlags
  protected open val optFlags: EnumSet<CommandFlags> = noneOfEnum()
  private var nextArgumentTokenOffset = 0
  private val logger = vimLogger<Command>()

  public abstract class ForEachCaret(range: Range, argument: String = "") : Command(range, argument) {
    public abstract fun processCommand(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ): ExecutionResult
  }

  public abstract class SingleExecution(range: Range, argument: String = "") : Command(range, argument) {
    public abstract fun processCommand(
      editor: VimEditor,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ): ExecutionResult
  }

  @Throws(ExException::class)
  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    checkRanges(editor)
    checkArgument()
    if (editor.nativeCarets().any { it.hasSelection() } && Flag.SAVE_VISUAL !in argFlags.flags) {
      editor.removeSelection()
      editor.removeSecondaryCarets()
    }
    if (argFlags.access == Access.WRITABLE && !editor.isDocumentWritable()) {
      logger.info("Trying to modify readonly document")
      return ExecutionResult.Error
    }

    val operatorArguments = OperatorArguments(
      editor.vimStateMachine.isOperatorPending(editor.mode),
      0,
      editor.mode,
    )

    val runCommand = { runCommand(editor, context, operatorArguments) }
    return when (argFlags.access) {
      Access.WRITABLE -> injector.application.runWriteAction(runCommand)
      Access.READ_ONLY -> injector.application.runReadAction(runCommand)
      Access.SELF_SYNCHRONIZED -> runCommand.invoke()
    }
  }

  private fun runCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    when (this) {
      is ForEachCaret -> {
        editor.forEachNativeCaret(
          { caret ->
            if (result is ExecutionResult.Success) {
              result = processCommand(editor, caret, context, operatorArguments)
            }
          },
          true,
        )
      }
      is SingleExecution -> result = processCommand(editor, context, operatorArguments)
    }
    return result
  }

  private fun checkRanges(editor: VimEditor) {
    if (RangeFlag.RANGE_FORBIDDEN == argFlags.rangeFlag && commandRange.size() != 0) {
      // Some commands (e.g. `:file`) throw "E474: Invalid argument" instead, while e.g. `:3ascii` throws E481
      throw exExceptionMessage("E481")  // E481: No range allowed
    }

    if (RangeFlag.RANGE_REQUIRED == argFlags.rangeFlag && commandRange.size() == 0) {
      // This will never be hit. The flag is used by `:[range]` and this only parses if there's an actual range
      injector.messages.showStatusBarMessage(editor, injector.messages.message(Msg.e_rangereq))
      throw MissingRangeException()
    }

    if (RangeFlag.RANGE_IS_COUNT == argFlags.rangeFlag) {
      commandRange.setDefaultLine(1)
    }
  }

  private fun checkArgument() {
    if (ArgumentFlag.ARGUMENT_FORBIDDEN == argFlags.argumentFlag && commandArgument.isNotBlank()) {
      throw exExceptionMessage("E488", commandArgument) // E488: Trailing characters: {0}
    }

    if (ArgumentFlag.ARGUMENT_REQUIRED == argFlags.argumentFlag && commandArgument.isBlank()) {
      throw exExceptionMessage("E471")  // E471: Argument required
    }
  }

  public enum class RangeFlag {
    /**
     * Indicates that a range must be specified with this command
     */
    RANGE_REQUIRED,

    /**
     * Indicates that a range is optional for this command
     */
    RANGE_OPTIONAL,

    /**
     * Indicates that a range can't be specified for this command
     */
    RANGE_FORBIDDEN,

    /**
     * Indicates that the command takes a count, not a range - effects default
     * Works like RANGE_OPTIONAL
     */
    RANGE_IS_COUNT,
  }

  public enum class ArgumentFlag {
    /**
     * Indicates that an argument must be specified with this command
     */
    ARGUMENT_REQUIRED,

    /**
     * Indicates that an argument is optional for this command
     */
    ARGUMENT_OPTIONAL,

    /**
     * Indicates that an argument can't be specified for this command
     */
    ARGUMENT_FORBIDDEN,
  }

  public enum class Access {
    /**
     * Indicates that this is a command that modifies the editor
     */
    WRITABLE,

    /**
     * Indicates that this command does not modify the editor
     */
    READ_ONLY,

    /**
     * Indicates that this command handles writability by itself
     */
    SELF_SYNCHRONIZED,
  }

  public enum class Flag {
    /**
     * This command should not exit visual mode.
     *
     * Vim exits visual mode before command execution, but in this case :action will work incorrect.
     *   With this flag visual mode will not be exited while command execution.
     */
    SAVE_VISUAL,
  }

  public data class CommandHandlerFlags(
    val rangeFlag: RangeFlag,
    val argumentFlag: ArgumentFlag,
    val access: Access,
    val flags: Set<Flag>,
  )

  protected fun flags(
    rangeFlag: RangeFlag,
    argumentFlag: ArgumentFlag,
    access: Access,
    vararg flags: Flag,
  ): CommandHandlerFlags = CommandHandlerFlags(rangeFlag, argumentFlag, access, flags.toSet())

  protected fun setNextArgumentTokenOffset(nextArgumentTokenOffset: Int) {
    this.nextArgumentTokenOffset = nextArgumentTokenOffset
  }

  private fun getNextArgumentToken() = commandArgument.substring(nextArgumentTokenOffset).trimStart()

  /**
   * Return the last line of the range as a count, one-based
   */
  protected fun getCountFromRange(editor: VimEditor, caret: VimCaret): Int {
    return commandRange.getCount(editor, caret)
  }

  /**
   * Return the argument as a count, throwing E488 if it's invalid or there are trailing characters
   */
  protected fun getCountFromArgument(): Int? {
    return Regex("""(?<count>\d+)\s*(?<trailing>.*)?(".*)?""").matchEntire(getNextArgumentToken())?.let { match ->
      match.groups["trailing"]?.let { trailing ->
        if (trailing.value.isNotEmpty()) throw exExceptionMessage("E488", trailing.value)
      }
      match.groups["count"]?.value?.toInt()
    }
  }

  /**
   * Consume the register from the argument, moving the current position of the argument.
   *
   * This will try to find a register at the start of the argument, if available. It will only consume valid, writable
   * registers, and will throw "E488: Trailing characters: {0}" for invalid or readonly registers. It will not consume
   * the digit registers - these will be available as a count.
   *
   * When the register is consumed, the end position is remembered so that subsequent calls to [getCountFromArgument]
   * will read the correct value. This call is obviously not idempotent.
   */
  protected fun consumeRegisterFromArgument(): Char {
    val argument = getNextArgumentToken()
    return if (argument.isNotEmpty() && !argument[0].isDigit()) {
      if (!injector.registerGroup.isValid(argument[0]) || !injector.registerGroup.isRegisterWritable(argument[0])) {
        throw exExceptionMessage("E488", argument)  // E488: Trailing characters: {0}
      }
      setNextArgumentTokenOffset(1) // Skip the register
      argument[0]
    } else {
      injector.registerGroup.defaultRegister
    }
  }

  /**
   * Return the first address, as a one-based line number, from the argument. Throws E16 for invalid range
   *
   * Given a command in the format `:[range]command {address}`, this function will return the line number for the
   * `{address}`. If no address is specified, or is invalid, it will throw "E16: Invalid range".
   *
   * Note that address can be `0`, which can mean the line _before_ the first line. This is useful for `:[range]move 0`,
   * to move a range to the very top of the file.
   */
  protected fun getAddressFromArgument(editor: VimEditor): Int {
    // The simplest way to parse a range is to parse it as a command (it will default to GoToLineCommand) and ask for
    // its line range. We should perhaps improve this in the future
    return injector.vimscriptParser.parseCommand(getNextArgumentToken())?.getLineRange(editor)?.startLine1
      ?: throw exExceptionMessage(Msg.e_invrange) // E16: Invalid range
  }

  public fun getLine(editor: VimEditor): Int = getLine(editor, editor.currentCaret())
  public fun getLine(editor: VimEditor, caret: VimCaret): Int = commandRange.getLine(editor, caret)

  public fun getLineRange(editor: VimEditor): LineRange = getLineRange(editor, editor.currentCaret())
  public fun getLineRange(editor: VimEditor, caret: VimCaret): LineRange = commandRange.getLineRange(editor, caret)

  // TODO: Get rid of checkCount here. Used by getTextRange
  public fun getLineRange(editor: VimEditor, caret: VimCaret, checkCount: Boolean): LineRange {
    val lineRange = commandRange.getLineRange(editor, caret)
    val count = if (checkCount) countArgument else null
    return if (checkCount && count != null) {
      // If the argument has a count, the returned range is count lines from the end of the command's range
      LineRange(lineRange.endLine, lineRange.endLine + count - 1)
    } else {
      lineRange
    }
  }

  /**
   * Get the line range using the optional count argument
   *
   * The command is in the format `:[range]command {count}`. If `{count}` is not specified, the range is returned as-is.
   * If `{count}` is specified, then the returned range is `count` lines from the last line of the range.
   *
   * The `{count}` argument must be a simple integer, with no trailing characters. This function will fail with "E488:
   * Trailing characters" otherwise.
   */
  public fun getLineRangeWithCount(editor: VimEditor, caret: VimCaret): LineRange {
    val lineRange = commandRange.getLineRange(editor, caret)
    return getCountFromArgument()?.let { count ->
      LineRange(lineRange.endLine, lineRange.endLine + count - 1)
    } ?: lineRange
  }

  public fun getTextRange(editor: VimEditor): TextRange = getTextRange(editor, editor.currentCaret())

  public fun getTextRange(editor: VimEditor, caret: VimCaret): TextRange {
    return getLineRange(editor, caret).toTextRange(editor)
  }

  public fun getTextRange(editor: VimEditor, caret: VimCaret, checkCount: Boolean): TextRange {
    return getLineRange(editor, caret, checkCount).toTextRange(editor)
  }

  private val countArgument: Int?
    get() = getNextArgumentToken().toIntOrNull()
}
