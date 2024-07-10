/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package com.maddyhome.idea.vim.state.mode

import com.maddyhome.idea.vim.state.VimStateMachine

/**
 * Represents a mode in IdeaVim.
 *
 * If mode has [returnTo] variable, it can be active during the one-command-mode (':h i_Ctrl-o'). If this value
 *   is not null, the one-command-mode is active and we should get back to [returnTo] mode.
 *
 * Modes with selection have [selectionType] variable representing if the selection is character-, line-, or block-wise.
 *
 * To update the current mode, use [VimStateMachine.setMode]. To get the current mode use [VimStateMachine.mode].
 *
 * [Mode] also has a bunch of extension functions like [Mode.isSingleModeActive].
 *
 * Also read about how modes work in Vim: https://github.com/JetBrains/ideavim/wiki/how-many-modes-does-vim-have
 */
sealed interface Mode {
  data class NORMAL(val returnTo: ReturnTo? = null) : Mode, ReturnableFromCmd
  data class OP_PENDING(val returnTo: ReturnTo? = null, val forcedVisual: SelectionType? = null) :
    Mode, ReturnableFromCmd
  data class VISUAL(val selectionType: SelectionType, val returnTo: ReturnTo? = null) : Mode,
    ReturnableFromCmd
  data class SELECT(val selectionType: SelectionType, val returnTo: ReturnTo? = null) : Mode
  object INSERT : Mode, ReturnableFromCmd
  object REPLACE : Mode
  data class CMD_LINE(val returnTo: ReturnableFromCmd) : Mode
}

sealed interface ReturnTo {
  object INSERT : ReturnTo
  object REPLACE : ReturnTo
}

// Marks modes that can we return from CMD_LINE mode
sealed interface ReturnableFromCmd

enum class SelectionType {
  LINE_WISE,
  CHARACTER_WISE,
  BLOCK_WISE,
}