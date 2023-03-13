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

public object VimRepeater {
  public var repeatHandler = false

  public var lastChangeCommand: Command? = null
    private set
  public var lastChangeRegister = injector.registerGroup.defaultRegister

  public fun saveLastChange(command: Command) {
    lastChangeCommand = command
    lastChangeRegister = injector.registerGroup.currentRegister
  }
}
