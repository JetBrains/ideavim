/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import javax.swing.KeyStroke

public interface VimApplication {
  public fun isMainThread(): Boolean
  public fun invokeLater(action: () -> Unit, editor: VimEditor)
  public fun invokeLater(action: () -> Unit)
  public fun <T> invokeAndWait(action: () -> T): T
  public fun executeOnPooledThread(action: () -> Unit)
  public fun isUnitTest(): Boolean
  public fun postKey(stroke: KeyStroke, editor: VimEditor)

  public fun localEditors(): List<VimEditor>

  public fun runWriteCommand(editor: VimEditor, name: String?, groupId: Any?, command: Runnable)
  public fun runReadCommand(editor: VimEditor, name: String?, groupId: Any?, command: Runnable)

  public fun <T> runWriteAction(action: () -> T): T
  public fun <T> runReadAction(action: () -> T): T

  public fun currentStackTrace(): String
  public fun runAfterGotFocus(runnable: Runnable)
}
