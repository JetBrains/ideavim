/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import org.jetbrains.annotations.NonNls
import javax.swing.text.AbstractDocument
import javax.swing.text.StyledEditorKit
import javax.swing.text.ViewFactory

internal object ExEditorKit : StyledEditorKit() {
  @NonNls
  override fun getContentType() = "text/ideavim"
  override fun createDefaultDocument() = ExDocument()

  override fun getViewFactory() = ViewFactory { elem ->
    // Hierarchy for a styled document is section -> paragraph -> content/non-printable
    // For our document there will be one section, one paragraph and at least one content/non-printable
    // (A PlainDocument just contains a single content element)
    when (elem.name) {
      AbstractDocument.SectionElementName -> ScrollingInlineCompositeView(elem)
      AbstractDocument.ParagraphElementName -> InlineCompositeView(elem)
      ExDocument.NonPrintableElementName -> ExNonPrintableFieldView(elem)
      else -> super.viewFactory.create(elem)
    }
  }
}
