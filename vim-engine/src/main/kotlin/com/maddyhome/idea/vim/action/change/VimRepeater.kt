package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command

object VimRepeater {
  var repeatHandler = false

  var lastChangeCommand: Command? = null
    private set
  var lastChangeRegister = injector.registerGroup.defaultRegister

  fun saveLastChange(command: Command) {
    lastChangeCommand = command
    lastChangeRegister = injector.registerGroup.currentRegister
  }
}
