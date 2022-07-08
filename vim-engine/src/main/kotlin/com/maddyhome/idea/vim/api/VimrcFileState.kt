package com.maddyhome.idea.vim.api

interface VimrcFileState {
  var filePath: String?

  fun saveFileState(filePath: String)
}
