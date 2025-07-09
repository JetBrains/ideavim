/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor

interface VimModalInputService {
  fun getCurrentModalInput(): VimModalInput?
  fun create(
    editor: VimEditor,
    context: ExecutionContext,
    label: String,
    inputInterceptor: VimInputInterceptor,
  ): VimModalInput
}
