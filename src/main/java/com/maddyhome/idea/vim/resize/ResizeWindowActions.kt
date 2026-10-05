/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.resize

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * `CTRL-W +` - increase the current window's height by [count][Command.count] rows (default 1).
 *
 * see "h CTRL-W_+"
 */
@CommandOrMotion(
  keys = ["<C-W>+"],
  modes = [Mode.NORMAL],
  description = "Increase the current window height by [count] rows."
)
class IncreaseWindowHeightAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    ResizeService().resizeCurrentWindowHeight(editor, ResizeArgument.Relative(cmd.count))
    return true
  }
}

/**
 * `CTRL-W -` - decrease the current window's height by [count][Command.count] rows (default 1).
 *
 * see "h CTRL-W_-"
 */
@CommandOrMotion(
  keys = ["<C-W>-"],
  modes = [Mode.NORMAL],
  description = "Decrease the current window height by [count] rows."
)
class DecreaseWindowHeightAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    ResizeService().resizeCurrentWindowHeight(editor, ResizeArgument.Relative(-cmd.count))
    return true
  }
}

/**
 * `CTRL-W _` (and `CTRL-W CTRL-_`) - set the current window's height to [count][Command.rawCount] rows,
 * or maximise it when no count is given.
 *
 * see "h CTRL-W__"
 */
@CommandOrMotion(
  keys = ["<C-W>_", "<C-W><C-_>"],
  modes = [Mode.NORMAL],
  description = "Set the current window height to [count] rows; without [count], make it as high as possible."
)
class MaximizeWindowHeightAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = if (cmd.rawCount == 0) ResizeArgument.Maximize else ResizeArgument.Absolute(cmd.rawCount)
    ResizeService().resizeCurrentWindowHeight(editor, argument)
    return true
  }
}

/**
 * `CTRL-W >` - increase the current window's width by [count][Command.count] columns (default 1).
 *
 * see "h CTRL-W_>"
 */
@CommandOrMotion(
  keys = ["<C-W>>"],
  modes = [Mode.NORMAL],
  description = "Increase the current window width by [count] columns."
)
class IncreaseWindowWidthAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    ResizeService().resizeCurrentWindowWidth(editor, ResizeArgument.Relative(cmd.count))
    return true
  }
}

/**
 * `CTRL-W <` - decrease the current window's width by [count][Command.count] columns (default 1).
 *
 * see "h CTRL-W_<"
 */
@CommandOrMotion(
  keys = ["<C-W><lt>"],
  modes = [Mode.NORMAL],
  description = "Decrease the current window width by [count] columns."
)
class DecreaseWindowWidthAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    ResizeService().resizeCurrentWindowWidth(editor, ResizeArgument.Relative(-cmd.count))
    return true
  }
}

/**
 * `CTRL-W |` - set the current window's width to [count][Command.rawCount] columns, or maximise it when
 * no count is given.
 *
 * see "h CTRL-W_bar"
 */
@CommandOrMotion(
  keys = ["<C-W>|"],
  modes = [Mode.NORMAL],
  description = "Set the current window width to [count] columns; without [count], make it as wide as possible."
)
class MaximizeWindowWidthAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val argument = if (cmd.rawCount == 0) ResizeArgument.Maximize else ResizeArgument.Absolute(cmd.rawCount)
    ResizeService().resizeCurrentWindowWidth(editor, argument)
    return true
  }
}

/**
 * `CTRL-W =` - make all windows (almost) equally high and wide.
 *
 * see "h CTRL-W_="
 */
@CommandOrMotion(
  keys = ["<C-W>="],
  modes = [Mode.NORMAL],
  description = "Make all windows (almost) equally high and wide."
)
class EqualizeWindowsAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    ResizeService().equalizeWindows(editor)
    return true
  }
}
