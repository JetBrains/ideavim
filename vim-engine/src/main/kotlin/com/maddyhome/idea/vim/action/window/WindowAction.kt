/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.window

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.SplitService
import com.maddyhome.idea.vim.group.SplitService.AdjustmentType
import com.maddyhome.idea.vim.group.SplitService.MaximizeType
import com.maddyhome.idea.vim.handler.VimActionHandler

/**
 * @author vlan
 */
public class WindowDownAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count, true)
    return true
  }
}

/**
 * @author vlan
 */
public class WindowLeftAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count * -1, false)
    return true
  }
}

/**
 * @author vlan
 */
public class WindowRightAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count, false)
    return true
  }
}

/**
 * @author vlan
 */
public class WindowUpAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.selectWindowInRow(editor.primaryCaret(), context, cmd.count * -1, true)
    return true
  }
}

/**
 * @author fsiglia
 */
public class WindowStretchVerticallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.adjustOwningWindow(editor.primaryCaret(), AdjustmentType.StretchVertically, context)
    return true
  }
}

public class WindowShrinkVerticallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.adjustOwningWindow(editor.primaryCaret(), AdjustmentType.ShrinkVertically, context)
    return true
  }
}

public class WindowStretchHorizontallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.adjustOwningWindow(editor.primaryCaret(), AdjustmentType.StretchHorizontally, context)
    return true
  }
}

public class WindowShrinkHorizontallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.adjustOwningWindow(editor.primaryCaret(), AdjustmentType.ShrinkHorizontally, context)
    return true
  }
}

public class WindowMaximizeVerticallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.maximizeOwningWindow(editor.primaryCaret(), MaximizeType.MaximizeVertically, context)
    return true
  }
}

public class WindowMaximizeHorizontallyAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.maximizeOwningWindow(editor.primaryCaret(), MaximizeType.MaximizeHorizontally, context)
    return true
  }
}

public class WindowEqualizeSplitsAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    injector.window.equalizeWindows(context)
    return true
  }
}
