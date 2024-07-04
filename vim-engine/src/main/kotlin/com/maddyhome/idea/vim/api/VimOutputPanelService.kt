/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

interface VimOutputPanelService {
  /** TODO we do not need Editor **/
  fun create(editor: VimEditor, context: ExecutionContext): VimOutputPanel
  fun getOrCreate(editor: VimEditor, context: ExecutionContext): VimOutputPanel
  fun getCurrentOutputPanel(): VimOutputPanel?
}