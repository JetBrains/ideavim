/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

/**
 * Manages IdeaVim's virtual buffers (the command-line / search history windows and the control-chars
 * editor) — light in-memory editors that are not backed by a real file.
 */
interface VirtualBufferGroup {

  /**
   * Opens a virtual buffer of [kind] populated with [content]. Refused (with an error) if any
   * virtual buffer is already open, since these buffers cannot be nested (`:help cmdwin`).
   */
  fun open(context: ExecutionContext, editor: VimEditor, kind: VirtualBufferKind, content: String)
  fun close(editor: VimEditor)
}
