/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimFile {
  fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean)
  fun displayHexInfo(editor: VimEditor)
  fun displayLocationInfo(vimEditor: VimEditor)
  fun selectPreviousTab(context: ExecutionContext)
  fun saveFile(editor: VimEditor, context: ExecutionContext)
  fun saveFiles(editor: VimEditor, context: ExecutionContext)
  fun closeFile(editor: VimEditor, context: ExecutionContext)
  fun closeFile(number: Int, context: ExecutionContext)
  fun selectFile(count: Int, context: ExecutionContext): Boolean
  fun selectNextFile(count: Int, context: ExecutionContext)
  fun openFile(filename: String, context: ExecutionContext): Boolean

  fun getProjectId(project: Any): String

  /**
   * Focuses or opens a new VimEditor by [documentPath]
   */
  fun selectEditor(projectId: String, documentPath: String, protocol: String?): VimEditor?

  fun findFileStartingWith(prefix:String, context: ExecutionContext): Collection<String>?
}
