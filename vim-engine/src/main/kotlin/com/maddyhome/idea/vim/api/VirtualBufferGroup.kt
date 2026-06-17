/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

/**
 * Manages IdeaVim's virtual buffers (the command-line / search history windows, the control-chars
 * editor, and the `inccommand=split` substitute preview) — light in-memory editors that are not
 * backed by a real file.
 */
interface VirtualBufferGroup {

  /**
   * Opens a virtual buffer of [kind] populated with [content].
   *
   * Command-line / search history buffers and the control-chars editor refuse to open while another
   * such buffer is already open, since these buffers cannot be nested (`:help cmdwin`). The
   * substitute preview is transient and does not participate in that restriction.
   *
   * @param focus when false, the buffer is shown without stealing focus (used by `inccommand=split`).
   */
  fun open(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
    focus: Boolean = true,
  )

  /** Closes the virtual buffer hosted in [editor] (typically the cmdwin editor itself). */
  fun close(editor: VimEditor)

  /** Closes the open virtual buffer of [kind] in the project associated with [context]. */
  fun close(context: ExecutionContext, kind: VirtualBufferKind)

  /** Returns whether a virtual buffer of [kind] is currently open in the project associated with [context]. */
  fun isOpen(context: ExecutionContext, kind: VirtualBufferKind): Boolean

  /** Replaces the content of an already-open virtual buffer of [kind] in place. */
  fun refresh(context: ExecutionContext, kind: VirtualBufferKind, content: String)
}
