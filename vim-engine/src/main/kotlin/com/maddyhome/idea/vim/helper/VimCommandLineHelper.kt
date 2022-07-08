package com.maddyhome.idea.vim.helper

import com.maddyhome.idea.vim.api.VimEditor

interface VimCommandLineHelper {
  fun inputString(vimEditor: VimEditor, prompt: String, finishOn: Char?): String?
}
