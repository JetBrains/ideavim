/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.mark

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getText
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.pushCurrentPosition
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

/**
 * Jumps to the definition of the keyword under or after the caret, recording where we came from, see "h CTRL-]"
 *
 * Unlike `gd` and `gD` ([com.maddyhome.idea.vim.action.motion.search.GotoDeclarationAction]), this is a tag command, so
 * it pushes onto the tag stack and `<C-T>` walks back over it.
 */
@CommandOrMotion(keys = ["<C-]>"], modes = [Mode.NORMAL, Mode.VISUAL])
class TagJumpAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.jumpService.saveJumpLocation(editor)

    // Without a keyword there is no tag to jump to, so there is nothing to walk back over either. We still delegate to
    // the IDE, which is free to be cleverer about what the caret is on than Vim's notion of a keyword
    val wordRange = injector.searchHelper.findWordAtOrFollowingCursor(editor, editor.currentCaret(), isBigWord = false)
    if (wordRange != null) {
      injector.tagService.pushCurrentPosition(editor, editor.getText(wordRange))
    }

    injector.actionExecutor.executeAction(editor, name = "GotoDeclaration", context = context)
    return true
  }
}
