/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret

interface VimWindowGroup {
  fun selectWindowInRow(caret: VimCaret, context: ExecutionContext, relativePosition: Int, vertical: Boolean)

  // Note: window selection functions below have a known limitation.
  // After calling these, FileEditorManager.getSelectedTextEditor() may return the old editor
  // because the platform propagates the change asynchronously (IJPL-235369).
  // These functions are not exposed in the extension API (VimApi) until the platform
  // provides a way to observe or await the propagation (VIM-4138).
  fun selectNextWindow(context: ExecutionContext)
  fun selectWindow(context: ExecutionContext, index: Int)
  fun selectPreviousWindow(context: ExecutionContext)
  fun closeAllExceptCurrent(context: ExecutionContext)
  fun splitWindowVertical(context: ExecutionContext, filename: String, focusNew: Boolean = true)
  fun splitWindowHorizontal(context: ExecutionContext, filename: String, focusNew: Boolean = true)
  fun closeCurrentWindow(context: ExecutionContext)
  fun closeAll(context: ExecutionContext)
}
