package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.ChangesListener

interface VimDocument {
  fun addChangeListener(listener: ChangesListener)
  fun removeChangeListener(listener: ChangesListener)
}