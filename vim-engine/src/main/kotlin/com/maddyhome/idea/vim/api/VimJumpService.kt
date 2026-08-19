/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.mark.Jump
import org.jetbrains.annotations.TestOnly

// todo should it be multicaret?
// todo docs
// todo it would be better to have some Vim scope for this purpose (p:), to store things project-wise like for buffers
/**
 * This service manages jump lists, keyed by scope id
 *
 * The scope of a jump list is decided by [VimEditor.jumpListId]: project-wide by default, one per window (split) when
 * the 'windowjumps' option is set. Prefer the [VimEditor]-based extension functions below over passing a scope id
 * directly.
 */
interface VimJumpService {
  /**
   * Timestamp (`System.currentTimeMillis()`) of the last Jump command <C-o>, <C-i>
   * it's a temporary sticky tape to avoid difficulties with Platform, which counts <C-o>, <C-i> as new jump locations
   * and messes up our jump list
   */
  var lastJumpTimeStamp: Long

  fun getJump(scopeId: String, count: Int): Jump?
  fun getJumps(scopeId: String): List<Jump>
  fun getJumpSpot(scopeId: String): Int

  fun addJump(scopeId: String, jump: Jump, reset: Boolean)
  fun saveJumpLocation(editor: VimEditor)

  fun removeJump(scopeId: String, jump: Jump)
  fun dropLastJump(scopeId: String)
  fun clearJumps(scopeId: String)
  fun copyJumps(fromId: String, toId: String)

  fun updateJumpsFromInsert(scopeId: String, startOffset: Int, length: Int)
  fun updateJumpsFromDelete(scopeId: String, startOffset: Int, length: Int)

  fun includeCurrentCommandAsNavigation(editor: VimEditor)

  /**
   * Loads legacy jump state from an XML element for version migration.
   * Default no-op; overridden by implementations that support PersistentStateComponent.
   */
  fun loadLegacyState(element: Any) {}

  @TestOnly
  fun resetJumps()
}

fun VimJumpService.addJump(editor: VimEditor, reset: Boolean) {
  val virtualFile = editor.getVirtualFile() ?: return
  val path = virtualFile.path
  val protocol = virtualFile.protocol
  val position = editor.offsetToBufferPosition(editor.currentCaret().offset)
  val jump = Jump(position.line, position.column, path, protocol)
  addJump(editor, jump, reset)
}

fun VimJumpService.getJump(editor: VimEditor, count: Int): Jump? {
  return getJump(editor.jumpListId, count)
}

fun VimJumpService.getJumps(editor: VimEditor): List<Jump> {
  return getJumps(editor.jumpListId)
}

fun VimJumpService.getJumpSpot(editor: VimEditor): Int {
  return getJumpSpot(editor.jumpListId)
}

fun VimJumpService.addJump(editor: VimEditor, jump: Jump, reset: Boolean) {
  return addJump(editor.jumpListId, jump, reset)
}

fun VimJumpService.removeJump(editor: VimEditor, jump: Jump) {
  return removeJump(editor.jumpListId, jump)
}

fun VimJumpService.dropLastJump(editor: VimEditor) {
  return dropLastJump(editor.jumpListId)
}

fun VimJumpService.updateJumpsFromInsert(editor: VimEditor, startOffset: Int, length: Int) {
  return updateJumpsFromInsert(editor.jumpListId, startOffset, length)
}

fun VimJumpService.updateJumpsFromDelete(editor: VimEditor, startOffset: Int, length: Int) {
  return updateJumpsFromDelete(editor.jumpListId, startOffset, length)
}
