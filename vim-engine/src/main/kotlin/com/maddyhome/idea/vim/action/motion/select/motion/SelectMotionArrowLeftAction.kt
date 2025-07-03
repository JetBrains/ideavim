/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select.motion

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.options.OptionConstants

/**
 * @author Alex Plate
 */

@CommandOrMotion(keys = ["<Left>"], modes = [Mode.SELECT])
class SelectMotionArrowLeftAction : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val keymodel = injector.globalOptions().keymodel
    if (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel) {
      logger.debug("Keymodel option has stopselect. Exiting select mode")
      val startSelection = caret.selectionStart
      val endSelection = caret.selectionEnd
      editor.exitSelectModeNative(false)
      if (editor.isTemplateActive()) {
        logger.debug("Template is active. Activate insert mode")
        injector.changeGroup.insertBeforeCaret(editor, context)
        if (caret.offset in startSelection..endSelection) {
          return startSelection.toMotion()
        }
      }
      // No return statement, perform motion to left
    }
    return injector.motion.getHorizontalMotion(editor, caret, -operatorArguments.count1, false)
  }

  private companion object {
    private val logger = vimLogger<SelectMotionArrowLeftAction>()
  }
}
