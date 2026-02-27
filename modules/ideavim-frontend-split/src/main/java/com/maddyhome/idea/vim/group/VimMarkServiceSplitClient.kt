/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMarkServiceBase
import com.maddyhome.idea.vim.mark.Mark
import com.maddyhome.idea.vim.mark.VimMark
import kotlinx.coroutines.runBlocking

/**
 * Thin-client mark service for split (Remote Development) mode.
 *
 * Extends [VimMarkServiceBase] for per-caret local state (visual selection marks,
 * change marks, mark updates on text changes). Global mark operations are forwarded
 * via [MarkRemoteApi] to the backend where `VimMarkServiceImpl` manages persistence
 * and IDE bookmark integration.
 */
internal class VimMarkServiceSplitClient : VimMarkServiceBase() {

  override fun getGlobalMark(char: Char): Mark? {
    val info = rpc { getMark(null, char) } ?: return null
    return VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
  }

  override fun getAllGlobalMarks(): Set<Mark> {
    return rpc { getMarks(null) }.map { info ->
      VimMark(info.key, info.line, info.col, info.filepath, info.protocol)
    }.toSet()
  }

  override fun setGlobalMark(mark: Mark): Boolean {
    return rpc { setMark(null, mark.key, mark.filepath, mark.line, mark.col, mark.protocol) }
  }

  override fun setGlobalMark(editor: VimEditor, char: Char, offset: Int): Boolean {
    val position = editor.offsetToBufferPosition(offset)
    val filepath = editor.getVirtualFile()?.path ?: return false
    val protocol = editor.getVirtualFile()?.protocol ?: "file"
    return rpc { setMark(null, char, filepath, position.line, position.column, protocol) }
  }

  override fun removeGlobalMark(char: Char) {
    rpc { removeMark(null, char) }
  }

  private fun <T> rpc(block: suspend MarkRemoteApi.() -> T): T {
    val coroutineScope = ApplicationManager.getApplication().service<CoroutineScopeProvider>().coroutineScope
    return runBlocking(coroutineScope.coroutineContext) { MarkRemoteApi.getInstance().block() }
  }
}
