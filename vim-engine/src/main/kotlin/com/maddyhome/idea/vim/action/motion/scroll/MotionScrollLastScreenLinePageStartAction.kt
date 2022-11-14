/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.scroll

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

class MotionScrollLastScreenLinePageStartAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_IGNORE_SCROLL_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val motion = injector.motion

    // Without [count]: Redraw with the line just above the window at the bottom of the window. Put the cursor in that
    // line, at the first non-blank in the line.
    if (cmd.rawCount == 0) {
      val prevVisualLine = injector.engineEditorHelper.normalizeVisualLine(
        editor,
        injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor) - 1
      )
      val logicalLine = injector.engineEditorHelper.visualLineToLogicalLine(editor, prevVisualLine)
      return motion.scrollCurrentLineToDisplayBottom(editor, logicalLine + 1, true)
    }

    // [count]z^ first scrolls [count] to the bottom of the window, then moves the caret to the line that is now at
    // the top, and then move that line to the bottom of the window
    var logicalLine = injector.engineEditorHelper.normalizeLine(editor, cmd.rawCount - 1)
    if (motion.scrollCurrentLineToDisplayBottom(editor, logicalLine + 1, false)) {
      logicalLine = injector.engineEditorHelper.visualLineToLogicalLine(
        editor,
        injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor)
      )
      return motion.scrollCurrentLineToDisplayBottom(editor, logicalLine + 1, true)
    }

    return false
  }
}
