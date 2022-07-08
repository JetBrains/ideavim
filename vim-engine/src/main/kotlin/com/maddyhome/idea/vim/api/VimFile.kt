package com.maddyhome.idea.vim.api

interface VimFile {
  fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean)
  fun displayHexInfo(editor: VimEditor)
  fun displayLocationInfo(vimEditor: VimEditor)
  fun selectPreviousTab(context: ExecutionContext)
  fun saveFile(context: ExecutionContext)
  fun saveFiles(context: ExecutionContext)
  fun closeFile(editor: VimEditor, context: ExecutionContext)
  fun closeFile(number: Int, context: ExecutionContext)
  fun selectFile(count: Int, context: ExecutionContext): Boolean
  fun selectNextFile(count: Int, context: ExecutionContext)
  fun openFile(filename: String, context: ExecutionContext): Boolean
}
