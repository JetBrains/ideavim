/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCommandLine
import com.maddyhome.idea.vim.api.VimCommandLineService
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij

public class ExEntryPanelService : VimCommandLineService {
  public override fun getActiveCommandLine(): VimCommandLine? {
    return ExEntryPanel.instance
  }

  public override fun create(editor: VimEditor, context: ExecutionContext, label: String, initText: String, count: Int): VimCommandLine {
    val panel = ExEntryPanel.getInstance()
    panel.activate(editor.ij, context.ij, label, initText, count)
    return panel
  }
}
