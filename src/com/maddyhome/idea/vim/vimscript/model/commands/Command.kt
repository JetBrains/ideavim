package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.ThrowableComputable
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.MissingArgumentException
import com.maddyhome.idea.vim.ex.MissingRangeException
import com.maddyhome.idea.vim.ex.NoArgumentAllowedException
import com.maddyhome.idea.vim.ex.NoRangeAllowedException
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import java.util.*

sealed class Command(var commandRanges: Ranges, val commandArgument: String) : Executable {

  abstract val argFlags: CommandHandlerFlags
  protected open val optFlags: EnumSet<CommandFlags> = noneOfEnum()
  private val logger = logger<Command>()

  abstract class ForEachCaret(ranges: Ranges, argument: String = "") : Command(ranges, argument) {
    abstract fun processCommand(editor: Editor, caret: Caret, context: DataContext, vimContext: VimContext): ExecutionResult
  }

  abstract class SingleExecution(ranges: Ranges, argument: String = "") : Command(ranges, argument) {
    abstract fun processCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult
  }

  @Throws(ExException::class)
  override fun execute(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    checkRanges()
    checkArgument()
    if (editor.inVisualMode && Flag.SAVE_VISUAL !in argFlags.flags) {
      editor.exitVisualMode()
    }
    if (argFlags.access == Access.WRITABLE && !editor.document.isWritable) {
      logger.info("Trying to modify readonly document")
      return ExecutionResult.Error
    }

    val runCommand = ThrowableComputable<ExecutionResult, ExException> { runCommand(editor, context, vimContext) }
    return when (argFlags.access) {
      Access.WRITABLE -> ApplicationManager.getApplication().runWriteAction(runCommand)
      Access.READ_ONLY -> ApplicationManager.getApplication().runReadAction(runCommand)
      Access.SELF_SYNCHRONIZED -> runCommand.compute()
    }
  }

  private fun runCommand(editor: Editor, context: DataContext, vimContext: VimContext): ExecutionResult {
    var result: ExecutionResult = ExecutionResult.Success
    when (this) {
      is ForEachCaret -> {
        editor.caretModel.runForEachCaret(
          { caret ->
            if (result is ExecutionResult.Success) {
              result = processCommand(editor, caret, context, vimContext)
            }
          },
          true
        )
      }
      is SingleExecution -> result = processCommand(editor, context, vimContext)
    }
    return result
  }

  private fun checkRanges() {
    if (RangeFlag.RANGE_FORBIDDEN == argFlags.rangeFlag && commandRanges.size() != 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_norange))
      throw NoRangeAllowedException()
    }

    if (RangeFlag.RANGE_REQUIRED == argFlags.rangeFlag && commandRanges.size() == 0) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_rangereq))
      throw MissingRangeException()
    }

    if (RangeFlag.RANGE_IS_COUNT == argFlags.rangeFlag) {
      commandRanges.setDefaultLine(1)
    }
  }

  private fun checkArgument() {
    if (ArgumentFlag.ARGUMENT_FORBIDDEN == argFlags.argumentFlag && commandArgument.isNotBlank()) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argforb))
      throw NoArgumentAllowedException()
    }

    if (ArgumentFlag.ARGUMENT_REQUIRED == argFlags.argumentFlag && commandArgument.isBlank()) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_argreq))
      throw MissingArgumentException()
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
     * Indicates that the command takes a count, not a range - effects default
     * Works like RANGE_OPTIONAL
     */
    RANGE_IS_COUNT
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
    ARGUMENT_FORBIDDEN
  }

  enum class Access {
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
    SELF_SYNCHRONIZED
  }

  enum class Flag {
    /**
     * This command should not exit visual mode.
     *
     * Vim exits visual mode before command execution, but in this case :action will work incorrect.
     *   With this flag visual mode will not be exited while command execution.
     */
    SAVE_VISUAL
  }

  data class CommandHandlerFlags(
    val rangeFlag: RangeFlag,
    val argumentFlag: ArgumentFlag,
    val access: Access,
    val flags: Set<Flag>,
  )

  fun flags(rangeFlag: RangeFlag, argumentFlag: ArgumentFlag, access: Access, vararg flags: Flag) =
    CommandHandlerFlags(rangeFlag, argumentFlag, access, flags.toSet())

  fun getLine(editor: Editor): Int = commandRanges.getLine(editor)

  fun getLine(editor: Editor, caret: Caret): Int = commandRanges.getLine(editor, caret)

  fun getCount(editor: Editor, defaultCount: Int, checkCount: Boolean): Int {
    val count = if (checkCount) countArgument else -1

    val res = commandRanges.getCount(editor, count)
    return if (res == -1) defaultCount else res
  }

  fun getCount(editor: Editor, caret: Caret, defaultCount: Int, checkCount: Boolean): Int {
    val count = commandRanges.getCount(editor, caret, if (checkCount) countArgument else -1)
    return if (count == -1) defaultCount else count
  }

  fun getLineRange(editor: Editor): LineRange = commandRanges.getLineRange(editor, -1)

  fun getLineRange(editor: Editor, caret: Caret, checkCount: Boolean = false): LineRange {
    return commandRanges.getLineRange(editor, caret, if (checkCount) countArgument else -1)
  }

  fun getTextRange(editor: Editor, checkCount: Boolean): TextRange {
    val count = if (checkCount) countArgument else -1
    return commandRanges.getTextRange(editor, count)
  }

  fun getTextRange(editor: Editor, caret: Caret, checkCount: Boolean): TextRange {
    return commandRanges.getTextRange(editor, caret, if (checkCount) countArgument else -1)
  }

  private val countArgument: Int
    get() = commandArgument.toIntOrNull() ?: -1
}
