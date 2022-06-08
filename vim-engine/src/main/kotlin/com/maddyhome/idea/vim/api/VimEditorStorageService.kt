package com.maddyhome.idea.vim.api

interface VimEditorStorageService {
  fun <T> getDataFromEditor(editor: VimEditor, key: Key<T>): T?
  fun <T> putDataToEditor(editor: VimEditor, key: Key<T>, data: T)
}

data class Key<T>(val name: String)