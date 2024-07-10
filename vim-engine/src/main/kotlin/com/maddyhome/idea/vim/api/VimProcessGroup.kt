/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.api

interface VimProcessGroup {
  @kotlin.jvm.Throws(java.lang.Exception::class)
  fun executeCommand(editor: VimEditor, command: String, input: CharSequence?, currentDirectoryPath: String?): String?
}
