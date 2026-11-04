/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

/**
 * Manages IdeaVim's virtual buffers — light in-memory editors that are not backed by a real file, such as the
 * command-line window, the `:help` window or the quickfix window. How a buffer is shown is described by its
 * [VirtualBufferKind].
 */
interface VirtualBufferGroup {

  /**
   * Opens a virtual buffer of [kind] populated with [content].
   *
   * An [exclusive][VirtualBufferKind.exclusive] buffer refuses to open while another exclusive one is open. When
   * [kind] [reuses][VirtualBufferKind.reuseOpen] an open buffer, the open one gets [content] and is moved to instead.
   *
   * @param line when not null, the caret is put at the start of this line
   * @param focus when false, the buffer is shown without stealing focus (used by `inccommand=split`).
   * @return the editor hosting the opened buffer, or null if it wasn't opened or was opened without focus.
   */
  fun open(
    context: ExecutionContext,
    editor: VimEditor,
    kind: VirtualBufferKind,
    content: String,
    line: Int? = null,
    focus: Boolean = true,
  ): VimEditor?

  /** Closes the virtual buffer hosted in [editor] (typically the cmdwin editor itself). */
  fun close(editor: VimEditor)

  /** Closes the open virtual buffer of [kind] in the project associated with [context]. */
  fun close(context: ExecutionContext, kind: VirtualBufferKind)

  /** Returns whether a virtual buffer of [kind] is currently open in the project associated with [context]. */
  fun isOpen(context: ExecutionContext, kind: VirtualBufferKind): Boolean

  /**
   * Replaces the content of an already-open virtual buffer of [kind] in place. Does nothing when it isn't open.
   *
   * @param line when not null, the caret is put at the start of this line in every editor of the buffer
   */
  fun refresh(context: ExecutionContext, kind: VirtualBufferKind, content: String, line: Int? = null)

  /**
   * Makes the window [buffer] was opened from the current one, so files open there rather than next to the buffer.
   * When that window doesn't show the original file anymore, another window not showing [buffer] is used.
   *
   * @return the editor of that window, or null if there's no other window
   */
  fun selectOriginalWindow(buffer: VimEditor): VimEditor? = null
}
