/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.maddyhome.idea.vim.api.OwnedPrimaryContent
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.awt.Toolkit
import java.awt.datatransfer.Transferable
import java.util.concurrent.atomic.AtomicReference

/**
 * The PRIMARY selection of X11, the text pasted with the middle mouse button. Read through AWT, written through
 * [PrimarySelectionWriter], which also tells whether the selection still holds what was written.
 *
 * Disposable through [dispose], so the writer's background thread and helper process are released with the service.
 * The parameters replace PRIMARY in tests.
 */
internal class IjPrimarySelection(
  private val writer: PrimarySelectionWriter = primarySelectionWriter(),
  private val selectionContents: () -> Transferable? = ::readSystemSelection,
) {
  private val published = AtomicReference<OwnedPrimaryContent?>()

  /** The text in PRIMARY, or null where there is no PRIMARY or it has no text. */
  fun read(): IjVimCopiedText? = selectionContents()?.let(IjCopiedTextCollector::fromTransferable)

  /**
   * Publishes [textData] to PRIMARY. [selectionType] is remembered alongside it, as PRIMARY stores text only.
   *
   * @return whether the write was performed or reliably scheduled
   */
  fun write(textData: IjVimCopiedText, selectionType: SelectionType): Boolean {
    val written = writer.write(textData.text, textData.transferableData)
    published.set(if (written) OwnedPrimaryContent(textData, selectionType) else null)
    return written
  }

  /** What was last published, while nothing else has claimed PRIMARY since. */
  fun ownedContent(): OwnedPrimaryContent? = published.get()?.takeIf { writer.ownsSelection() }

  /** What was last published, whoever owns PRIMARY now. */
  fun lastPublishedContent(): OwnedPrimaryContent? = published.get()

  fun dispose() {
    writer.dispose()
    published.set(null)
  }
}

/** The content of PRIMARY through AWT, or null where there is no PRIMARY. */
private fun readSystemSelection(): Transferable? = Toolkit.getDefaultToolkit()?.systemSelection?.getContents(null)
