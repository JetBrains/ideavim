package com.maddyhome.idea.vim.api

interface VimEditorGroup {
  fun notifyIdeaJoin(editor: VimEditor)
  fun localEditors(): Collection<VimEditor>
}
