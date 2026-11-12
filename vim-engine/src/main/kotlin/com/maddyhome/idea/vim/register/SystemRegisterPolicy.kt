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
 * PRIMARY exists on X11 (and XWayland) only. Without it there is one clipboard, and `+` and `*` are one register,
 * see [storageRegister].
 */
object SystemRegisterPolicy {

  /**
   * The register [register] is kept in. Without PRIMARY, `+` is kept in `*`, which is backed by CLIPBOARD, so both
   * names read and write the same register and `:registers` lists it as `"*`. This is what Vim does with one clipboard
   * (`PLUS_REGISTER` is `STAR_REGISTER` under `ONE_CLIPBOARD`).
   */
  fun storageRegister(register: Char, isPrimarySupported: Boolean): Char =
    if (register == CLIPBOARD_REGISTER && !isPrimarySupported) PRIMARY_REGISTER else register

  /** The register that commands without a register name use. */
  fun defaultRegister(clipboardOption: Collection<String>, isPrimarySupported: Boolean): Char = when {
    OptionConstants.clipboard_unnamedplus in clipboardOption -> storageRegister(CLIPBOARD_REGISTER, isPrimarySupported)
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
