/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

public interface VimFile {
  public fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean)
  public fun displayHexInfo(editor: VimEditor)
  public fun displayLocationInfo(vimEditor: VimEditor)
  public fun selectPreviousTab(context: ExecutionContext)
  public fun saveFile(context: ExecutionContext)
  public fun saveFiles(context: ExecutionContext)
  public fun closeFile(editor: VimEditor, context: ExecutionContext)
  public fun closeFile(number: Int, context: ExecutionContext)
  public fun selectFile(count: Int, context: ExecutionContext): Boolean
  public fun selectNextFile(count: Int, context: ExecutionContext)
  public fun openFile(filename: String, context: ExecutionContext): Boolean
}
