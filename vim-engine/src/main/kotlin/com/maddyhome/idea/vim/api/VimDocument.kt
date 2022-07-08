package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.ChangesListener
import com.maddyhome.idea.vim.common.LiveRange
import com.maddyhome.idea.vim.common.Offset

interface VimDocument {
  fun addChangeListener(listener: ChangesListener)
  fun removeChangeListener(listener: ChangesListener)
  fun getOffsetGuard(offset: Offset): LiveRange?
}
