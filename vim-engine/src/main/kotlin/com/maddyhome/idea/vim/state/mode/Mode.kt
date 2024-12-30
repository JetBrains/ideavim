/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("ClassName")

package com.maddyhome.idea.vim.state.mode

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.state.VimStateMachine

/**
 * Represents a mode in IdeaVim.
 *
 * IdeaVim's default mode is Normal, represented with the [NORMAL] subtype. It will enter other modes through keystrokes
 * such as `i`, `v` or `R`. Leaving a mode is usually handled with `<Escape>`, and the current mode will usually return
 * to the previous mode. For example, hitting `<Esc>` in Insert will return to Normal.
 *
 * Modes can be nested, too. For example, `v/foo` will start in Normal, switch to Visual (with character-wise selection)
 * and then switch to Command-line. Hitting `<Escape>` in this nested Command-line mode will return to Visual. Hitting
 * `<Enter>` to accept the search result will also exit Command-line and return to Visual. Commands such as `d` can also
 * end Visual mode and return to Normal. See also `i<C-O>d/foo`, which starts in Normal, switches to Insert, then nested
 * "Insert Normal", Operator-pending and finally Command-line.
 *
 * Not all modes are nested. For example, Select mode can be entered from Visual (`v<C-G>`), but this is a mode switch,
 * rather than nesting - hitting `v<C-G><Escape>` will result in Normal mode.
 *
 * Furthermore, a mode can be active for a single command, via `<C-O>` in Insert or Replace mode (`:help i_CTRL-O`), or
 * in Select mode (`:help v_CTRL-O`). When used in Insert or Replace mode, this enters Normal mode for a single command,
 * and then returns to Insert or Replace respectively. When in Select mode, `<C-O>` will enter Visual for a single
 * command, and will then return to Select or Normal, depending on if the selection has been removed or not. Note that
 * it is hard to know when a specific command is active for a single command - for example, `i<C-O>d/foo` would need to
 * recursively look at the [returnTo] value until it found a [NORMAL] mode that had a non-[NORMAL] return to mode, but
 * this wouldn't work for `v<C-G><C-O>/foo`, which doesn't have a nested [NORMAL] mode, but a nested [VISUAL] mode.
 *
 * The [VimStateMachine.mode] property can be used to set or get the current mode, but it is usually preferable to use
 * the [VimEditor.mode] property. There are also several extension functions such as [Mode.isSingleModeActive] and
 * [VimEditor.inVisualMode]. Setting the current selection and entering Visual or Select mode are usually two
 * (programmatic) operations, although there are helper functions for this.
 *
 * Modes with selection have [selectionType] variable representing if the selection is character-, line-, or block-wise.
 *
 * Also read about how modes work in Vim: https://github.com/JetBrains/ideavim/wiki/how-many-modes-does-vim-have
 *
 * One word of warning: don't try to map all the state transitions! [vim/vim#12115](https://github.com/vim/vim/issues/12115)
 */
sealed interface Mode {
  /**
   * The mode to return to when Escape is pressed, or the current command is finished
   */
  val returnTo: Mode

  data class NORMAL(private val originalMode: Mode? = null) : Mode {
    override val returnTo: Mode
      get() = originalMode ?: this

    /**
     * Returns true if Insert mode is pending, after the completion of the current Normal command. AKA "Insert Normal"
     *
     * When in Insert mode the `<C-O>` keystroke will temporarily switch to Normal for the duration of a single command.
     */
    val isInsertPending = originalMode is INSERT

    /**
     * Returns true if Replace mode is pending, after the completion of the current Normal command.
     *
     * Like "Insert Normal", but with `<C-O>` used in Replace mode.
     */
    val isReplacePending = originalMode is REPLACE
  }

  data class OP_PENDING(override val returnTo: Mode) : Mode {
    init {
      // OP_PENDING will normally return to NORMAL, but can return to INSERT or REPLACE if i_CTRL-O is followed by an
      // operator such as `d`. I.e. "Insert Normal mode" and "Replace Normal mode"
      require(returnTo is NORMAL || returnTo is INSERT || returnTo is REPLACE) {
        "OP_PENDING mode can be active only in NORMAL, INSERT or REPLACE modes, not ${returnTo.javaClass.simpleName}"
      }
    }
  }

  data class VISUAL(val selectionType: SelectionType, override val returnTo: Mode = NORMAL()) : Mode {
    init {
      // VISUAL will normally return to NORMAL, but can return to INSERT or REPLACE if i_CTRL-O is followed by `v`
      // I.e. "Insert Visual mode" and "Replace Visual mode"
      // VISUAL can return to SELECT after `<C-O>`
      require(returnTo is NORMAL || returnTo is INSERT || returnTo is REPLACE || returnTo is SELECT) {
        "VISUAL mode can be active only in NORMAL, INSERT, REPLACE or SELECT modes, not ${returnTo.javaClass.simpleName}"
      }
    }

    /**
     * Returns true if Insert mode is pending, after the completion of the current Visual command. AKA "Insert Visual"
     *
     * Vim can enter Visual mode from Insert mode, either using shifted keys (based on `'keymodel'` and `'selectmode'`
     * values) or via "Insert Normal" (`i<C-O>v`).
     */
    val isInsertPending = returnTo is INSERT

    /**
     * Returns true if Replace mode is pending, after the completion of the current Visual command.
     *
     * Like "Insert Visual", but starting from (and returning to) Replace (`R<C-O>v`).
     */
    val isReplacePending = returnTo is REPLACE

    /**
     * Returns true if the mode is temporarily switched from Select to Visual for the duration of one command
     *
     * See `:help v_CTRL-O`
     */
    val isSelectPending = returnTo is SELECT
  }

  data class SELECT(val selectionType: SelectionType, override val returnTo: Mode = NORMAL()) : Mode {
    init {
      // SELECT will normally return to NORMAL, but can return to INSERT or REPLACE if v_CTRL-O is followed by a command
      // that deletes the selection, e.g. `d`, or if "Insert Select mode" removes selection (`i`, `<C-O>`, `ve`, `x`)
      // SELECT can also be changed to VISUAL with v_CTRL-O
      require(returnTo is NORMAL || returnTo is INSERT || returnTo is REPLACE) {
        "SELECT mode can be active only in NORMAL, INSERT or REPLACE modes, not ${returnTo.javaClass.simpleName}"
      }
    }

    /**
     * Returns true if Insert mode is pending, after the completion of the current Visual command. AKA "Insert Visual"
     *
     * Vim can enter Select mode from Insert mode, either using shifted keys (based on `'keymodel'` and `'selectmode'`
     * values) or via "Insert Normal" (`i<C-O>gh`).
     */
    val isInsertPending = returnTo is INSERT

    /**
     * Returns true if Replace mode is pending, after the completion of the current Visual command.
     *
     * Like "Insert Select", but starting from (and returning to) Replace (e.g., `R<C-O>gh`).
     */
    val isReplacePending = returnTo is REPLACE
  }

  object INSERT : Mode {
    override val returnTo: Mode = NORMAL()
  }

  object REPLACE : Mode {
    override val returnTo: Mode = NORMAL()
  }

  data class CMD_LINE(override val returnTo: Mode) : Mode {
    init {
      require(returnTo is NORMAL || returnTo is OP_PENDING || returnTo is VISUAL || returnTo is INSERT) {
        "CMD_LINE mode can be active only in NORMAL, OP_PENDING, VISUAL or INSERT modes, not ${returnTo.javaClass.simpleName}"
      }
    }
  }
}

enum class SelectionType {
  LINE_WISE,
  CHARACTER_WISE,
  BLOCK_WISE,
}
