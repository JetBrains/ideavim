/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.MessageHelper

/**
 * This class is used to handle the Vim Plugin enabled/disabled toggle. This is most likely used as a menu option
 * but could also be used as a toolbar item.
 */
internal class VimPluginToggleAction : DumbAwareToggleAction()/*, LightEditCompatible*/ {
  override fun isSelected(event: AnActionEvent): Boolean = VimPlugin.isEnabled()

  override fun setSelected(event: AnActionEvent, b: Boolean) {
    VimPlugin.setEnabled(b)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)

    e.presentation.text = if (ActionPlaces.POPUP == e.place) {
      if (VimPlugin.isEnabled()) MessageHelper.message("action.VimPluginToggle.enabled") else MessageHelper.message("action.VimPluginToggle.enable")
    } else {
      MessageHelper.message("action.VimPluginToggle.text")
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
