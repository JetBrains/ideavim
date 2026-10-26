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
   * @return the editor hosting the opened buffer, or null if it wasn't opened or was opened without focus.
   */
  fun open(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
    focus: Boolean = true,
  ): VimEditor?

  /** Closes the virtual buffer hosted in [editor] (typically the cmdwin editor itself). */
  fun close(editor: VimEditor)

  /** Closes the open virtual buffer of [kind] in the project associated with [context]. */
  fun close(context: ExecutionContext, kind: VirtualBufferKind)

  /** Returns whether a virtual buffer of [kind] is currently open in the project associated with [context]. */
  fun isOpen(context: ExecutionContext, kind: VirtualBufferKind): Boolean

  /** Replaces the content of an already-open virtual buffer of [kind] in place. */
  fun refresh(context: ExecutionContext, kind: VirtualBufferKind, content: String)

  /**
   * Shows [content] in the read-only help window, with the cursor at the start of [line], the way `:help` does.
   *
   * Like in Vim, an already open help window is reused rather than opening another one. It also doesn't take part in
   * the nesting restriction of the other virtual buffers.
   */
  fun openHelp(context: ExecutionContext, editor: VimEditor, content: String, line: Int)

  /**
   * Shows [content] in the read-only quickfix window with the cursor at the start of [line], the way `:copen` does.
   *
   * An already open quickfix window is reused. It doesn't take part in the nesting restriction of the other virtual
   * buffers.
   */
  fun openQuickFix(context: ExecutionContext, editor: VimEditor, content: String, line: Int)

  /**
   * Replaces the content of the open quickfix window and puts its cursor at the start of [line].
   * Does nothing when the window isn't open.
   */
  fun refreshQuickFix(context: ExecutionContext, content: String, line: Int)

  /**
   * Makes the window [buffer] was opened from the current one, so files open there rather than next to the buffer.
   * When that window doesn't show the original file anymore, another window not showing [buffer] is used.
   *
   * @return the editor of that window, or null if there's no other window
   */
  fun selectOriginalWindow(buffer: VimEditor): VimEditor? = null
}
