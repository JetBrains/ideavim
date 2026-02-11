/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimOutputPanelService {
  /**
   * Creates a new VimOutputPanel instance for building output without affecting the current panel until displayed.
   */
  // TODO make it possible to pass null instead of editor
  fun create(editor: VimEditor, context: ExecutionContext): VimOutputPanel

  /**
   * Retrieves the current VimOutputPanel or creates a new one if none exists.
   */
  fun getOrCreate(editor: VimEditor, context: ExecutionContext): VimOutputPanel

  /**
   * Returns the currently active VimOutputPanel, if available.
   */
  fun getCurrentOutputPanel(): VimOutputPanel?

  /**
   * Appends text to the existing output panel or creates a new one with the given text and message type.
   */
  fun output(
    editor: VimEditor,
    context: ExecutionContext,
    text: String,
    messageType: MessageType = MessageType.STANDARD,
  )
}