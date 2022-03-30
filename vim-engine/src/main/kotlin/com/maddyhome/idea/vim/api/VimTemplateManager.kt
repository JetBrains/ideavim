package com.maddyhome.idea.vim.api

interface VimTemplateManager {
  fun getTemplateState(editor: VimEditor): VimTemplateState?
}

interface VimTemplateState
