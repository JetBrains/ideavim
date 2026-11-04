/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction

/**
 * Identifies a kind of virtual buffer and describes how [VirtualBufferGroup] shows it.
 *
 * The kinds the engine itself depends on are declared here. The ones that belong to a single feature are declared next
 * to that feature, e.g. the `:help` window next to the `:help` command.
 */
abstract class VirtualBufferKind {
  abstract val fileName: String

  /** The buffer can't be edited */
  open val readOnly: Boolean = false

  /** The buffer is shown in a viewer editor: the user can't edit it, but its content can still be replaced */
  open val viewer: Boolean = false

  /**
   * Only one exclusive buffer can be open at a time, they can't be nested (`:help cmdwin`).
   * Opening one while another is open shows [alreadyOpenMessage] instead.
   */
  open val exclusive: Boolean = false

  open val alreadyOpenMessage: String
    get() = "$fileName is already open"

  /**
   * Opening the buffer while it's already open moves to the open one and gives it the new content instead of opening
   * another one
   */
  open val reuseOpen: Boolean = false

  /** A buffer where `<CR>` acts on the caret line instead of moving down */
  interface EnterHandler {
    /** Called for the primary caret only */
    fun onEnter(buffer: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext)
  }

  /** The command-line history window `q:` opens */
  data object Command : VirtualBufferKind(), EnterHandler {
    override val fileName: String = "[Command Line]"
    override val exclusive: Boolean = true
    override val alreadyOpenMessage: String = "E1292: Command-line window is already open"

    override fun onEnter(buffer: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext) {
      injector.searchWindowGroup.executeCurrentLineAndClose(buffer, caret, context)
    }
  }

  /** The search history window `q/` and `q?` open */
  data class Search(val direction: Direction) : VirtualBufferKind(), EnterHandler {
    override val fileName: String = "[Search Line]"
    override val exclusive: Boolean = true
    override val alreadyOpenMessage: String = "E1292: Command-line window is already open"

    override fun onEnter(buffer: VimEditor, caret: ImmutableVimCaret, context: ExecutionContext) {
      injector.searchWindowGroup.executeCurrentLineAndClose(buffer, caret, context)
    }
  }

  data object ControlCharsEditor : VirtualBufferKind() {
    override val fileName: String = "[Control Chars Editor]"
    override val exclusive: Boolean = true
    override val alreadyOpenMessage: String = "A control characters editor is already open"
  }

  /** The transient `inccommand=split` preview of `:substitute` */
  data object SubstitutePreview : VirtualBufferKind() {
    override val fileName: String = "[Preview Substitute]"
    override val viewer: Boolean = true
  }
}
