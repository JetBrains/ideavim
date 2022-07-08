package com.maddyhome.idea.vim.ex

open class ExException(s: String? = null) : Exception(s) {
  var code: String? = null
}
