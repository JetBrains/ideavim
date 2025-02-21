/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api.stubs

import com.maddyhome.idea.vim.api.VimApplicationBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.diagnostic.vimLogger
import javax.swing.KeyStroke

class VimApplicationStub : VimApplicationBase() {
  init {
    vimLogger<ExecutionContextManagerStub>().warn("VimApplicationStub is used. Please replace it with your own implementation of VimApplication.")
  }

  override fun isMainThread(): Boolean {
    TODO("Not yet implemented")
  }

  override fun invokeLater(action: () -> Unit, editor: VimEditor) {
    TODO("Not yet implemented")
  }

  override fun invokeLater(action: () -> Unit) {
    TODO("Not yet implemented")
  }

  override fun isUnitTest(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isInternal(): Boolean {
    TODO("Not yet implemented")
  }

  override fun postKey(stroke: KeyStroke, editor: VimEditor) {
    TODO("Not yet implemented")
  }

  override fun <T> runWriteAction(action: () -> T): T {
    TODO("Not yet implemented")
  }

  override fun <T> runReadAction(action: () -> T): T {
    TODO("Not yet implemented")
  }

  override fun currentStackTrace(): String {
    TODO("Not yet implemented")
  }

  override fun runAfterGotFocus(runnable: Runnable) {
    TODO("Not yet implemented")
  }

  override fun isOctopusEnabled(): Boolean {
    TODO("Not yet implemented")
  }
}
