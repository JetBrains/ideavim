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
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.MissingRangeException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.helper.StrictMode
import com.maddyhome.idea.vim.state.mode.inNormalMode
import com.maddyhome.idea.vim.state.mode.isBlock
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import org.jetbrains.annotations.ApiStatus

enum class CommandModifier {
  NONE,
  BANG
}

sealed class Command(
  private val commandRange: Range,
  protected val commandModifier: CommandModifier,
  val commandArgument: String,
) : Executable {
  override lateinit var vimContext: VimLContext
  override lateinit var rangeInScript: TextRange

  protected abstract val argFlags: CommandHandlerFlags

  protected var defaultRange: String = "."

  private var nextArgumentTokenOffset = 0
  private val logger = vimLogger<Command>()

  abstract class ForEachCaret(range: Range, modifier: CommandModifier, argument: String = "") :
    Command(range, modifier, argument) {
    abstract fun processCommand(
      editor: VimEditor,
      caret: VimCaret,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ): ExecutionResult
  }

  abstract class SingleExecution(range: Range, modifier: CommandModifier, argument: String = "") :
    Command(range, modifier, argument) {
    abstract fun processCommand(
      editor: VimEditor,
      context: ExecutionContext,
      operatorArguments: OperatorArguments,
    ): ExecutionResult
  }

  @Throws(ExException::class)
  override fun execute(editor: VimEditor, context: ExecutionContext): ExecutionResult {
    validate(editor)

    StrictMode.assert(editor.inNormalMode, "Command execution should only occur in normal mode")

    // We are currently in Normal mode, but might still have a visual or visual block selection and/or multiple carets.
    // Vim clears Visual mode before entering Command-line, but we can't do that because some of our commands can handle
    // selection and multiple carets, and we have to wait until now before we can handle it.
    // See ProcessGroup.startExEntry and ProcessExCommandEntryAction
    // Unless the command needs us to keep visual (e.g. :action), remove the secondary carets that are an implementation
    // detail for block selection, but leave all other carets. If any other caret has a selection, move the caret to the
    // start offset of the selection, copying Vim's behaviour (with its only caret)
    if (Flag.SAVE_SELECTION !in argFlags.flags) {
      // Editor.inBlockSelection is not available, because we're not in Visual mode anymore. Check if the primary caret
      // currently has a selection and if (when we still in Visual) it was a block selection.
      injector.application.runReadAction {
        if (editor.primaryCaret().hasSelection() && editor.primaryCaret().lastSelectionInfo.selectionType.isBlock) {
          editor.removeSecondaryCarets()
        }
        editor.nativeCarets().forEach {
          if (it.hasSelection()) {
            val offset = it.selectionStart
            it.removeSelection()
            it.moveToOffset(offset)
          }
        }
      }
    }

    if (argFlags.access == Access.WRITABLE && !editor.isDocumentWritable()) {
      logger.info("Trying to modify readonly document")
      return ExecutionResult.Error
    }

    val operatorArguments = OperatorArguments(0, editor.mode)

    return runCommand(editor, context, operatorArguments)
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

  private fun validate(editor: VimEditor) {
    checkRanges(editor)
    // TODO: Consider adding something like ModifiersFlag.BANG_ALLOWED with validation
    checkArgument()
  }

  private fun checkRanges(editor: VimEditor) {
    if (RangeFlag.RANGE_FORBIDDEN == argFlags.rangeFlag && commandRange.size() != 0) {
      // Some commands (e.g. `:file`) throw "E474: Invalid argument" instead, while e.g. `:3ascii` throws E481
      throw exExceptionMessage("E481")  // E481: No range allowed
    }

    if (RangeFlag.RANGE_REQUIRED == argFlags.rangeFlag && commandRange.size() == 0) {
      // This will never be hit. The flag is used by `:[range]` and this only parses if there's an actual range
      injector.messages.showStatusBarMessage(editor, injector.messages.message("e_rangereq"))
      throw MissingRangeException()
    }

    // If a range isn't specified, the default range for most commands is the current line ("."). If the command is
    // expecting a count instead of a range, the default would be a count of 1, represented as the range "1".
    // GlobalCommand is the only other command that has a different default. We could introduce another RangeFlag
    // (and maybe make them an enum set so it can be optional and whole-file-range), or set it
    // TODO: This is initialisation, not validation
    // It would be nice to do this in the constructor, but argFlags is abstract, so we can't access it
    if (RangeFlag.RANGE_IS_COUNT == argFlags.rangeFlag) {
      commandRange.defaultRange = "1"
    } else {
      commandRange.defaultRange = defaultRange
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

  enum class RangeFlag {
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
     * Indicates that the command takes a count, not a range - affects the default value if range isn't specified
     *
     * Implies range is optional
     */
    RANGE_IS_COUNT,
  }

  enum class ArgumentFlag {
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

  enum class Access {
    /**
     * Indicates that this is a command that modifies the editor
     *
     * Obsolete: It used to start a write action automatically, but now all actions are supposed to take care of locks
     * individually.
     */
    @ApiStatus.Obsolete
    WRITABLE,

    /**
     * Indicates that this command does not modify the editor
     *
     * Obsolete: It used to start a read action automatically, but now all actions are supposed to take care of locks
     * individually.
     */
    @ApiStatus.Obsolete
    READ_ONLY,

    /**
     * Indicates that this command handles writability by itself
     */
    SELF_SYNCHRONIZED,
  }

  enum class Flag {
    /**
     * The current selection should not be reset before executing this command
     *
     * Vim and IdeaVim always exit Visual mode, removing selection, and return to Normal before a command is executed.
     * However, IdeaVim has some commands that require the current selection, especially commands like `:action` that
     * interact with IDE functions. If this flag is specified, IdeaVim will still leave Visual and return to Normal, but
     * the current selection is not removed. It is up to the command to handle and either remove/update the selection.
     */
    SAVE_SELECTION,
  }

  data class CommandHandlerFlags(
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

  @Suppress("SameParameterValue")
  private fun setNextArgumentTokenOffset(nextArgumentTokenOffset: Int) {
    this.nextArgumentTokenOffset = nextArgumentTokenOffset
  }

  private fun getNextArgumentToken() = commandArgument.substring(nextArgumentTokenOffset).trimStart()

  protected fun isRangeSpecified(): Boolean = commandRange.size() > 0

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
      ?: throw exExceptionMessage("e_invrange") // E16: Invalid range
  }

  protected fun getLine(editor: VimEditor): Int = getLine(editor, editor.currentCaret())
  protected fun getLine(editor: VimEditor, caret: VimCaret): Int = commandRange.getLine(editor, caret)

  protected fun getLineRange(editor: VimEditor): LineRange = getLineRange(editor, editor.currentCaret())
  protected fun getLineRange(editor: VimEditor, caret: VimCaret): LineRange = commandRange.getLineRange(editor, caret)

  /**
   * Accessor method purely for incsearch
   *
   * Ensures that the range and argument have been correctly initialised and validated, specifically that the default
   * range has been set. Any validation errors are swallowed and ignored.
   *
   * It would be cleaner to move incsearch handling into the search Command instances, which could access this data
   * safely.
   */
  fun getLineRangeSafe(editor: VimEditor): LineRange? {
    try {
      validate(editor)
    } catch (_: Throwable) {
      return null
    }
    return getLineRange(editor)
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
  protected fun getLineRangeWithCount(editor: VimEditor, caret: VimCaret): LineRange {
    val lineRange = getLineRange(editor, caret)
    return getCountFromArgument()?.let { count ->
      LineRange(lineRange.endLine, lineRange.endLine + count - 1)
    } ?: lineRange
  }
}
