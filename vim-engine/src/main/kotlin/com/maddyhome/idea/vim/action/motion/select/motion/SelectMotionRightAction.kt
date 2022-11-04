/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.motion.select.motion

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.Motion
import com.maddyhome.idea.vim.handler.MotionActionHandler
import com.maddyhome.idea.vim.handler.toMotion
import com.maddyhome.idea.vim.handler.toMotionOrError
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * @author Alex Plate
 */

class SelectMotionRightAction : MotionActionHandler.ForEachCaret() {

  override val motionType: MotionType = MotionType.EXCLUSIVE

  override fun getOffset(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Motion {
    val keymodel =
      (injector.optionService.getOptionValue(OptionScope.GLOBAL, OptionConstants.keymodelName) as VimString).value
    if (OptionConstants.keymodel_stopsel in keymodel || OptionConstants.keymodel_stopselect in keymodel) {
      logger.debug("Keymodel option has stopselect. Exiting select mode")
      val startSelection = caret.selectionStart
      val endSelection = caret.selectionEnd
      editor.exitSelectModeNative(false)
      if (editor.isTemplateActive()) {
        logger.debug("Template is active. Activate insert mode")
        injector.changeGroup.insertBeforeCursor(editor, context)
        if (caret.offset.point in startSelection..endSelection) {
          return endSelection.toMotion()
        }
      }
      return caret.offset.point.toMotion()
    }
    return injector.motion.getOffsetOfHorizontalMotion(editor, caret, operatorArguments.count1, false).toMotionOrError()
  }

  companion object {
    private val logger = injector.getLogger(SelectMotionRightAction::class.java)
  }
}
