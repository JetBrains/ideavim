/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.action.change

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command

object VimRepeater {
  var repeatHandler: Boolean = false

  var lastChangeCommand: Command? = null
    private set
  var lastChangeRegister: Char = injector.registerGroup.defaultRegister

  fun saveLastChange(command: Command) {
    lastChangeCommand = command
    lastChangeRegister = injector.registerGroup.currentRegister
  }
}
