/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.scroll

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["z^"], modes = [Mode.NORMAL, Mode.VISUAL, Mode.OP_PENDING])
class MotionScrollLastScreenLinePageStartAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_IGNORE_SCROLL_JUMP)

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val scroll = injector.scroll

    // Without [count]: Redraw with the line just above the window at the bottom of the window. Put the cursor in that
    // line, at the first non-blank in the line.
    if (cmd.rawCount == 0) {
      val prevVisualLine =
        editor.normalizeVisualLine(injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor) - 1)
      val bufferLine = editor.visualLineToBufferLine(prevVisualLine)
      return scroll.scrollCurrentLineToDisplayBottom(editor, bufferLine + 1, true)
    }

    // [count]z^ first scrolls [count] to the bottom of the window, then moves the caret to the line that is now at
    // the top, and then move that line to the bottom of the window
    var bufferLine = editor.normalizeLine(cmd.rawCount - 1)
    if (scroll.scrollCurrentLineToDisplayBottom(editor, bufferLine + 1, false)) {
      bufferLine = editor.visualLineToBufferLine(injector.engineEditorHelper.getVisualLineAtTopOfScreen(editor))
      return scroll.scrollCurrentLineToDisplayBottom(editor, bufferLine + 1, true)
    }

    return false
  }
}
