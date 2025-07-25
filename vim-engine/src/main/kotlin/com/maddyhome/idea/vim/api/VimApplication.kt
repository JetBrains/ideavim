/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke

interface VimApplication {
  fun isMainThread(): Boolean
  fun invokeLater(editor: VimEditor, action: () -> Unit)
  fun invokeLater(action: () -> Unit)
  fun invokeAndWait(action: () -> Unit)
  fun isUnitTest(): Boolean
  fun isInternal(): Boolean
  fun postKey(stroke: KeyStroke, editor: VimEditor)

  fun <T> runWriteAction(action: () -> T): T
  fun <T> runReadAction(action: () -> T): T

  fun currentStackTrace(): String
  fun runAfterGotFocus(runnable: Runnable)
  fun isOctopusEnabled(): Boolean
}
