/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.register.RegisterConstants.CLIPBOARD_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.PRIMARY_REGISTER
import com.maddyhome.idea.vim.register.RegisterConstants.UNNAMED_REGISTER

/** A selection of the windowing system that backs the `+` and `*` registers. */
enum class SystemSelection { CLIPBOARD, PRIMARY }

/**
 * The rules for the `+` and `*` registers that depend on `'clipboard'` and on the windowing system, kept in one place
 * so they read as a table. Pure: the caller passes the option value and whether there is a PRIMARY selection.
 *
 * PRIMARY exists on X11 (and XWayland) only. Without it, `*` is backed by CLIPBOARD, and `unnamedplus` selects `*`
 * rather than `+`, as `*` is where the clipboard register is kept there.
 */
object SystemRegisterPolicy {

  /** The register that commands without a register name use. */
  fun defaultRegister(clipboardOption: Collection<String>, isPrimarySupported: Boolean): Char = when {
    OptionConstants.clipboard_unnamedplus in clipboardOption ->
      if (isPrimarySupported) CLIPBOARD_REGISTER else PRIMARY_REGISTER

    OptionConstants.clipboard_unnamed in clipboardOption -> PRIMARY_REGISTER
    else -> UNNAMED_REGISTER
  }

  /**
   * The selections a write to [register] goes to, in the order they are written. Empty for a register that isn't
   * backed by a selection.
   *
   * A yank to the register selected by `'clipboard'` also goes to the other selection on X11: `unnamedplus` copies a
   * yank to PRIMARY, and `unnamed` copies it to CLIPBOARD. A delete or a register the user named is not copied.
   *
   * @param isExplicit whether the register was named, as in `"+y` or `:let @+ = …`, rather than chosen by `'clipboard'`
   * @param isDelete whether the text was deleted (or changed) rather than yanked
   */
  fun selectionsToWrite(
    register: Char,
    isExplicit: Boolean,
    isDelete: Boolean,
    clipboardOption: Collection<String>,
    isPrimarySupported: Boolean,
  ): List<SystemSelection> {
    val copiesToOtherSelection = !isExplicit && !isDelete && isPrimarySupported
    return when (register) {
      CLIPBOARD_REGISTER -> buildList {
        add(SystemSelection.CLIPBOARD)
        if (copiesToOtherSelection && OptionConstants.clipboard_unnamedplus in clipboardOption) {
          add(SystemSelection.PRIMARY)
        }
      }

      PRIMARY_REGISTER -> if (!isPrimarySupported) {
        listOf(SystemSelection.CLIPBOARD)
      } else buildList {
        add(SystemSelection.PRIMARY)
        if (copiesToOtherSelection && OptionConstants.clipboard_unnamed in clipboardOption) {
          add(SystemSelection.CLIPBOARD)
        }
      }

      else -> emptyList()
    }
  }
}
