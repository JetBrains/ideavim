/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ui.ex

import org.jetbrains.annotations.NonNls
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Document

@Deprecated("ExCommands should be migrated to KeyHandler like commands for other modes")
internal object ExEditorKit : DefaultEditorKit() {
  /**
   * Gets the MIME type of the data that this
   * kit represents support for.
   *
   * @return the type
   */
  @NonNls
  override fun getContentType(): String {
    return "text/ideavim"
  }

  /**
   * Creates an uninitialized text storage model
   * that is appropriate for this type of editor.
   *
   * @return the model
   */
  override fun createDefaultDocument(): Document {
    return ExDocument()
  }
}
