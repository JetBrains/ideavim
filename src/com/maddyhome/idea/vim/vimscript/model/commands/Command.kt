package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.MissingRangeException
import com.maddyhome.idea.vim.ex.NoRangeAllowedException
import com.maddyhome.idea.vim.ex.ranges.Ranges
import com.maddyhome.idea.vim.group.HistoryGroup
import com.maddyhome.idea.vim.helper.MessageHelper
import com.maddyhome.idea.vim.helper.Msg
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.helper.noneOfEnum
import com.maddyhome.idea.vim.vimscript.model.Executable
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.VimContext
import java.util.*

sealed class Command(var commandRanges: Ranges, val originalString: String) : Executable {

  abstract val argFlags: CommandHandlerFlags
  protected open val optFlags: EnumSet<CommandFlags> = noneOfEnum()

  abstract class ForEachCaret(ranges: Ranges, originalString: String) :
    Command(ranges, originalString = originalString) {
    abstract fun processCommand(
      editor: Editor,
      caret: Caret,
      context: DataContext,
      vimContext: VimContext,
    ): ExecutionResult
  }

  abstract class SingleExecution(ranges: Ranges, originalString: String) :
    Command(ranges, originalString = originalString) {
    abstract fun processCommand(editor: Editor?, context: DataContext?, vimContext: VimContext): ExecutionResult
  }

  @Throws(ExException::class)
  override fun execute(
    editor: Editor?,
    context: DataContext?,
    vimContext: VimContext,
    skipHistory: Boolean,
  ): ExecutionResult {

    if (!skipHistory) {
      VimPlugin.getHistory().addEntry(HistoryGroup.COMMAND, originalString)
    }
    checkRanges()

    var result: ExecutionResult = ExecutionResult.Success
    if (editor != null && context != null) {

      if (editor.inVisualMode && Flag.SAVE_VISUAL !in argFlags.flags) {
        editor.exitVisualMode()
      }

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
    } else {
      if (this is SingleExecution) {
        result = processCommand(editor, context, vimContext)
      } else {
        // todo something smarter
        throw RuntimeException("ForEachCaret command was passed with nullable Editor or DataContext")
      }
    }

    if (result !is ExecutionResult.Success) {
      VimPlugin.indicateError()
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
    DONT_SAVE_LAST,

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
    val access: Access,
    val flags: Set<Flag>,
  )

  fun getLine(editor: Editor, caret: Caret): Int = commandRanges.getLine(editor, caret)
}
