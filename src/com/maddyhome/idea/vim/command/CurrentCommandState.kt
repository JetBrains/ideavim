package com.maddyhome.idea.vim.command

enum class CurrentCommandState {
  /** Awaiting a new command */
  NEW_COMMAND,
  // TODO: This should be probably processed in some better way
  /** Awaiting char or digraph input. In this mode mappings doesn't work (even for <C-K>) */
  CHAR_OR_DIGRAPH,
  READY,
  BAD_COMMAND
}