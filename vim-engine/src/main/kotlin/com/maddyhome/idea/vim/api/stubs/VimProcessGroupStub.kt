/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.KeyProcessResult
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimProcessGroupBase
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.state.mode.Mode
import javax.swing.KeyStroke

class VimProcessGroupStub : VimProcessGroupBase() {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimProcessGroupStub is used. Please replace it with your own implementation of VimProcessGroup.")
  }

  override var lastCommand: String? = null
  override var isCommandProcessing: Boolean = false
  override var modeBeforeCommandProcessing: Mode? = null

  override fun startExEntry(
    editor: VimEditor,
    context: ExecutionContext,
    command: Command,
    label: String,
    initialCommandText: String
  ) {
    TODO("Not yet implemented")
  }

  override fun processExKey(editor: VimEditor, stroke: KeyStroke, processResultBuilder: KeyProcessResult.KeyProcessResultBuilder): Boolean {
    TODO("Not yet implemented")
  }

  override fun cancelExEntry(editor: VimEditor, refocusOwningEditor: Boolean, resetCaret: Boolean) {
    TODO("Not yet implemented")
  }

  override fun executeCommand(
    editor: VimEditor,
    command: String,
    input: CharSequence?,
    currentDirectoryPath: String?,
  ): String? {
    TODO("Not yet implemented")
  }
}
