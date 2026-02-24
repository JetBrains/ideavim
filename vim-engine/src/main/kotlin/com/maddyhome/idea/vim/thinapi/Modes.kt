/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.vim.api.models.Mode
import com.maddyhome.idea.vim.state.mode.Mode as EngineMode

fun EngineMode.toMode(): Mode {
  return when (this) {
    is EngineMode.NORMAL -> {
      // Check if this is a normal mode that returns to another mode
      if (returnTo is EngineMode.INSERT) {
        Mode.NORMAL_FROM_INSERT
      } else if (returnTo is EngineMode.REPLACE) {
        Mode.NORMAL_FROM_REPLACE
      } else {
        Mode.NORMAL
      }
    }
    is EngineMode.VISUAL -> {
      // Determine the visual mode based on selection type and return mode
      when (selectionType) {
        com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE -> {
          if (returnTo is EngineMode.SELECT) Mode.VISUAL_CHARACTER_FROM_SELECT else Mode.VISUAL_CHARACTER
        }
        com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE -> {
          if (returnTo is EngineMode.SELECT) Mode.VISUAL_LINE_FROM_SELECT else Mode.VISUAL_LINE
        }
        com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE -> {
          if (returnTo is EngineMode.SELECT) Mode.VISUAL_BLOCK_FROM_SELECT else Mode.VISUAL_BLOCK
        }
      }
    }
    is EngineMode.SELECT -> {
      // Determine the select mode based on selection type
      when (selectionType) {
        com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE -> Mode.SELECT_CHARACTER
        com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE -> Mode.SELECT_LINE
        com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE -> Mode.SELECT_BLOCK
      }
    }
    is EngineMode.OP_PENDING -> Mode.OP_PENDING
    is EngineMode.INSERT -> Mode.INSERT
    is EngineMode.CMD_LINE -> Mode.COMMAND_LINE
    is EngineMode.REPLACE -> Mode.REPLACE
  }
}

fun Mode.toEngineMode(): EngineMode {
  return when (this) {
    // Normal modes
    Mode.NORMAL -> EngineMode.NORMAL()
    Mode.NORMAL_FROM_INSERT -> EngineMode.NORMAL(EngineMode.INSERT)
    Mode.NORMAL_FROM_REPLACE -> EngineMode.NORMAL(EngineMode.REPLACE)
    Mode.NORMAL_FROM_VIRTUAL_REPLACE -> EngineMode.NORMAL(EngineMode.REPLACE) // Using REPLACE as fallback
    
    // Operator pending modes
    Mode.OP_PENDING, 
    Mode.OP_PENDING_CHARACTERWISE,
    Mode.OP_PENDING_LINEWISE,
    Mode.OP_PENDING_BLOCKWISE -> EngineMode.OP_PENDING(EngineMode.NORMAL())
    
    // Visual modes
    Mode.VISUAL_CHARACTER -> EngineMode.VISUAL(
      com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE, 
      EngineMode.NORMAL()
    )
    Mode.VISUAL_LINE -> EngineMode.VISUAL(
      com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE, 
      EngineMode.NORMAL()
    )
    Mode.VISUAL_BLOCK -> EngineMode.VISUAL(
      com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE, 
      EngineMode.NORMAL()
    )
    
    // Visual modes from select
    Mode.VISUAL_CHARACTER_FROM_SELECT -> {
      val selectMode = EngineMode.SELECT(
        com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE, 
        EngineMode.NORMAL()
      )
      EngineMode.VISUAL(
        com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE, 
        selectMode
      )
    }
    Mode.VISUAL_LINE_FROM_SELECT -> {
      val selectMode = EngineMode.SELECT(
        com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE, 
        EngineMode.NORMAL()
      )
      EngineMode.VISUAL(
        com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE, 
        selectMode
      )
    }
    Mode.VISUAL_BLOCK_FROM_SELECT -> {
      val selectMode = EngineMode.SELECT(
        com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE, 
        EngineMode.NORMAL()
      )
      EngineMode.VISUAL(
        com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE, 
        selectMode
      )
    }
    
    // Select modes
    Mode.SELECT_CHARACTER -> EngineMode.SELECT(
      com.maddyhome.idea.vim.state.mode.SelectionType.CHARACTER_WISE, 
      EngineMode.NORMAL()
    )
    Mode.SELECT_LINE -> EngineMode.SELECT(
      com.maddyhome.idea.vim.state.mode.SelectionType.LINE_WISE, 
      EngineMode.NORMAL()
    )
    Mode.SELECT_BLOCK -> EngineMode.SELECT(
      com.maddyhome.idea.vim.state.mode.SelectionType.BLOCK_WISE, 
      EngineMode.NORMAL()
    )
    
    // Other modes
    Mode.INSERT -> EngineMode.INSERT
    Mode.REPLACE -> EngineMode.REPLACE
    Mode.COMMAND_LINE -> EngineMode.CMD_LINE(EngineMode.NORMAL())
  }
}

