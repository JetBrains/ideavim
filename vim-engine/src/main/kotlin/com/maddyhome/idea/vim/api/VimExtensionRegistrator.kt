package com.maddyhome.idea.vim.api

interface VimExtensionRegistrator {
  fun setOptionByPluginAlias(alias: String): Boolean
  fun getExtensionNameByAlias(alias: String): String?
}
