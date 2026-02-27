/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.newapi.ij

/**
 * COMPATIBILITY-LAYER: Created a class, renamed original class
 * Please see: https://jb.gg/zo8n0r
 */
interface VimExtensionHandler : ExtensionHandler {
  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    execute(editor.ij, context.ij)
  }

  fun execute(editor: Editor, context: DataContext)

  abstract class WithCallback : ExtensionHandler.WithCallback(), VimExtensionHandler
}
