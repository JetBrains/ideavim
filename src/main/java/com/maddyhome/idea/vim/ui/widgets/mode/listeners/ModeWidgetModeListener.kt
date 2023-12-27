/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.widgets.mode.listeners

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.mode
import com.maddyhome.idea.vim.ui.widgets.mode.VimModeWidget

internal class ModeWidgetModeListener(private val modeWidget: VimModeWidget): ModeChangeListener {
  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    val editorMode = editor.mode
    if (editorMode !is Mode.OP_PENDING && editor.ij.project == modeWidget.project) {
      modeWidget.updateWidget(editorMode)
    }
  }
}