/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim

import com.maddyhome.idea.vim.newapi.PrimarySelectionWriter
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable

/**
 * PRIMARY of the windowing system, kept in memory for tests, so that a test sees neither the PRIMARY of the machine it
 * runs on nor what another test left there. Like the real one, it holds text only, and IdeaVim owns it from its write
 * until another application writes it ([writeFromOtherApplication]).
 *
 * It starts readable and empty, as PRIMARY of a session where nothing was selected yet.
 */
class FakePrimarySelection : PrimarySelectionWriter {
  @Volatile
  private var text: String = ""

  @Volatile
  private var ownedByIdeaVim = false

  override fun write(text: String, transferableData: List<Any>): Boolean {
    this.text = text
    ownedByIdeaVim = true
    return true
  }

  override fun ownsSelection(): Boolean = ownedByIdeaVim

  fun contents(): Transferable = StringSelection(text)

  /** Another application takes PRIMARY, as a selection in a browser or a terminal does. */
  fun writeFromOtherApplication(text: String) {
    this.text = text
    ownedByIdeaVim = false
  }

  /** The text in PRIMARY now. */
  val currentText: String
    get() = text
}
