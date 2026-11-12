/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import java.awt.Toolkit
import java.awt.datatransfer.Transferable

/**
 * The PRIMARY selection of X11, the text pasted with the middle mouse button. Read through AWT, written through
 * [PrimarySelectionWriter], which also tells whether the selection still holds what was written. What was written, and
 * its type, is remembered by the register group.
 *
 * Disposable through [dispose], so the writer's background thread and helper process are released with the service.
 * The parameters replace PRIMARY in tests.
 */
internal class IjPrimarySelection(
  private val writer: PrimarySelectionWriter = primarySelectionWriter(),
  private val selectionContents: () -> Transferable? = ::readSystemSelection,
) {
  /** The text in PRIMARY, or null where there is no PRIMARY or it has no text. */
  fun read(): IjVimCopiedText? = selectionContents()?.let(IjCopiedTextCollector::fromTransferable)

  /** @return whether the write was performed or reliably scheduled */
  fun write(textData: IjVimCopiedText): Boolean = writer.write(textData.text, textData.transferableData)

  /** Whether nothing else has claimed PRIMARY since the last [write]. */
  fun isOwned(): Boolean = writer.ownsSelection()

  fun dispose() {
    writer.dispose()
  }
}

/** The content of PRIMARY through AWT, or null where there is no PRIMARY. */
private fun readSystemSelection(): Transferable? = Toolkit.getDefaultToolkit()?.systemSelection?.getContents(null)
