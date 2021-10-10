/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim.ex

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.helper.vimExOutput
import com.maddyhome.idea.vim.ui.ExOutputPanel

/**
 * @author vlan
 */
class ExOutputModel private constructor(private val myEditor: Editor) {
  var text: String? = null
    private set

  fun output(text: String) {
    this.text = text
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getInstance(myEditor).setText(text)
    }
  }

  fun clear() {
    text = null
    if (!ApplicationManager.getApplication().isUnitTestMode) {
      ExOutputPanel.getInstance(myEditor).deactivate(false)
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(editor: Editor): ExOutputModel {
      var model = editor.vimExOutput
      if (model == null) {
        model = ExOutputModel(editor)
        editor.vimExOutput = model
      }
      return model
    }
  }
}
