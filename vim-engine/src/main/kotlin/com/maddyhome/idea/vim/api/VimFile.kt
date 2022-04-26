package com.maddyhome.idea.vim.api

interface VimFile {
  fun displayFileInfo(vimEditor: VimEditor, fullPath: Boolean)
  fun displayHexInfo(editor: VimEditor)
  fun displayLocationInfo(vimEditor: VimEditor)
  fun selectPreviousTab(context: ExecutionContext)
  fun saveFile(context: ExecutionContext)
  fun closeFile(editor: VimEditor, context: ExecutionContext)
}