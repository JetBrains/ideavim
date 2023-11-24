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
public sealed interface Mode {
  public data class NORMAL(public val returnTo: ReturnTo? = null) : Mode, ReturnableFromCmd
  public data class OP_PENDING(public val returnTo: ReturnTo? = null, public val forcedVisual: SelectionType? = null) :
    Mode, ReturnableFromCmd
  public data class VISUAL(public val selectionType: SelectionType, public val returnTo: ReturnTo? = null) : Mode,
    ReturnableFromCmd
  public data class SELECT(public val selectionType: SelectionType, public val returnTo: ReturnTo? = null) : Mode
  public object INSERT : Mode
  public object REPLACE : Mode
  public data class CMD_LINE(public val returnTo: ReturnableFromCmd) : Mode
}

public sealed interface ReturnTo {
  public object INSERT : ReturnTo
  public object REPLACE : ReturnTo
}

// Marks modes that can we return from CMD_LINE mode
public sealed interface ReturnableFromCmd

public enum class SelectionType {
  LINE_WISE,
  CHARACTER_WISE,
  BLOCK_WISE,
}