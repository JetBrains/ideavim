/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.ex

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.state.mode.ReturnableFromCmd
import java.util.*

@CommandOrMotion(keys = [":"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
internal class ExEntryAction : VimActionHandler.SingleExecution(), CmdLineAction {
  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_START_EX)
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(editor: VimEditor, context: ExecutionContext, cmd: Command, operatorArguments: OperatorArguments): Boolean {
    if (editor.isOneLineMode()) return false

    val currentMode = editor.vimStateMachine.mode
    check(currentMode is ReturnableFromCmd) {
      "Cannot enable cmd mode from current mode $currentMode"
    }

    injector.processGroup.isCommandProcessing = true
    injector.processGroup.modeBeforeCommandProcessing = currentMode

    val initText = getRange(editor, cmd)

    // Make sure the Visual selection marks are up to date.
    injector.markService.setVisualSelectionMarks(editor)

    // Note that we should remove selection and reset caret offset before we switch back to Normal mode and then enter
    // Command-line mode. However, some IdeaVim commands can handle multiple carets, including multiple carets with
    // selection (which might or might not be a block selection). Unfortunately, because we're just entering
    // Command-line mode, we don't know which command is going to be entered, so we can't remove selection here.
    // Therefore, we switch to Normal and then Command-line even though we might still have a Visual selection...
    // On the plus side, it means we still show selection while editing the command line, which Vim also does
    // (Normal then Command-line is not strictly necessary, but done for completeness and autocmd)
    // Caret selection is finally handled in Command.execute
    editor.mode = com.maddyhome.idea.vim.state.mode.Mode.NORMAL()
    editor.mode = com.maddyhome.idea.vim.state.mode.Mode.CMD_LINE(currentMode)

    injector.commandLine.create(editor, context, ":", initText, 1)
    return true
  }
}
