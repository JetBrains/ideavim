/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * @author Alex Plate
 */

@CommandOrMotion(keys = ["g<C-h>"], modes = [Mode.NORMAL])
class SelectEnableBlockModeAction : VimActionHandler.SingleExecution() {

  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    editor.removeSecondaryCarets()
    val lineEnd = editor.getLineEndForOffset(editor.primaryCaret().offset)
    editor.primaryCaret().run {
      vimSetSystemSelectionSilently(offset, (offset + 1).coerceAtMost(lineEnd))
      moveToInlayAwareOffset((offset + 1).coerceAtMost(lineEnd))
    }
    return injector.visualMotionGroup.enterSelectMode(editor, SelectionType.BLOCK_WISE)
  }
}
