/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimFile {
  fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean)
  fun displayHexInfo(editor: VimEditor)
  fun displayLocationInfo(editor: VimEditor)
  fun selectPreviousTab(context: ExecutionContext)
  fun saveFile(editor: VimEditor, context: ExecutionContext)
  fun saveFiles(editor: VimEditor, context: ExecutionContext)
  fun closeFile(editor: VimEditor, context: ExecutionContext)
  fun closeFile(number: Int, context: ExecutionContext)
  fun selectFile(count: Int, context: ExecutionContext): Boolean
  fun selectNextFile(count: Int, context: ExecutionContext)
  fun openFile(filename: String, context: ExecutionContext, focusEditor: Boolean = true): Boolean

  /**
   * Finds a file by name or path, searching home directory, absolute path, content roots, and PSI index.
   * Returns the absolute file path as a String, or null if not found.
   *
   * In split (Remote Development) mode this goes via RPC to the backend where
   * ProjectRootManager and FilenameIndex are available.
   */
  fun findFile(filename: String, context: ExecutionContext): String? = null

  fun getProjectId(project: Any): String

  /**
   * Focuses or opens a new VimEditor by [documentPath]
   */
  fun selectEditor(projectId: String, documentPath: String, protocol: String): VimEditor?
}
