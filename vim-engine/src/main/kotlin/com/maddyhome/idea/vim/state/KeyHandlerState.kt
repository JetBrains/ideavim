/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.state

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.CommandBuilder
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.MappingState
import com.maddyhome.idea.vim.common.DigraphSequence
import com.maddyhome.idea.vim.impl.state.toMappingMode
import com.maddyhome.idea.vim.state.mode.Mode

public class KeyHandlerState {
  public val mappingState: MappingState = MappingState()
  public val digraphSequence: DigraphSequence = DigraphSequence()
  public val commandBuilder: CommandBuilder = CommandBuilder(injector.keyGroup.getKeyRoot(MappingMode.NORMAL))

  public fun partialReset(mode: Mode) {
    digraphSequence.reset()
    mappingState.resetMappingSequence()
    commandBuilder.resetInProgressCommandPart(injector.keyGroup.getKeyRoot(mode.toMappingMode()))
  }

  public fun reset(mode: Mode) {
    digraphSequence.reset()
    mappingState.resetMappingSequence()
    commandBuilder.resetAll(injector.keyGroup.getKeyRoot(mode.toMappingMode()))
  }
}