/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.handler

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.group.visual.VimSelection

/**
 * Represents an action that has already been performed, externally to IdeaVim
 *
 * This class is used to allow IdeaVim to work with IDE actions that move the caret(s) externally to the IdeaVim action
 * system. It supports for extensions such as IdeaVim-EasyMotion, which provides commands and mappings for the AceJump
 * IntelliJ plugin. Simple mappings to AceJump IDE actions would move the caret(s), but wouldn't integrate with IdeaVim
 * operators, such as `d` for delete or extend the Visual selection.
 *
 * It will track the start and end offsets of all carets, and acts very much like [TextObjectActionHandler], providing
 * a range for each caret that can be used by an operator action, or to extend selection.
 *
 * In more detail: IdeaVim will track the caret start locations before invoking the IdeaVim-EasyMotion extension mapping
 * handlers. The invoked handler will use the AceJump API to register a callback for when the movement is complete, and
 * then asks AceJump to move the caret. Once complete, the handler notifies IdeaVim, which calculates the end locations.
 * The start/end ranges are wrapped in this action handler and pushed to the [CommandBuilder], which completes and then
 * executes the command. Just like a text object handler, an operator will get the range for a caret, and act on it.
 */
class ExternalActionHandler(private val ranges: Map<ImmutableVimCaret, VimSelection>) : EditorActionHandlerBase(true) {
  override val type: Command.Type = Command.Type.MOTION

  // If all we have are ranges, then almost by definition, this is character-wise motion. However, the ranges might be
  // complete lines...
  val isLinewiseMotion = false

  override fun baseExecute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {

    // This would normally act like a simple motion, but that's already been handled by the external action. No need to
    // do anything.
    return true
  }

  fun getRange(caret: ImmutableVimCaret) = ranges[caret]?.toVimTextRange()
}
