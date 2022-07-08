package com.maddyhome.idea.vim.api

interface VimRegexpService {
  fun matches(pattern: String, text: String?, ignoreCase: Boolean = false): Boolean
}
