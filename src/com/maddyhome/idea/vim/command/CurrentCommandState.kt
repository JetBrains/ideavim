package com.maddyhome.idea.vim.command

enum class CurrentCommandState {
  NEW_COMMAND,
  READY,
  BAD_COMMAND
}