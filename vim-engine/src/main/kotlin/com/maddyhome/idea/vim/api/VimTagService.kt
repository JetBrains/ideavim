/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.tag.TagStackEntry
import org.jetbrains.annotations.TestOnly

/**
 * This service manages tag stacks, keyed by scope id, see "h tag-stack"
 *
 * A tag jump (`<C-]>`, `:tag`) pushes the position it was made from; `<C-T>` and `:pop` walk back up those positions,
 * `:tag` without an argument walks back down. Unlike the jump list, the tag stack is not persisted - Vim does not store
 * it in viminfo either.
 *
 * The scope is the same as the jump list's ([VimEditor.jumpListId]): project-wide by default, one per window (split)
 * when the 'windowjumps' option is set. Prefer the [VimEditor]-based extension functions below over passing a scope id
 * directly.
 */
interface VimTagService {
  /**
   * The entries of the stack, oldest first
   */
  fun getEntries(scopeId: String): List<TagStackEntry>

  /**
   * Where we currently are in the stack
   *
   * Equal to the stack's size while at the top - that is, when every tag jump made so far has been followed, and there
   * is nothing to walk back down to. [pop] decrements it, [moveDown] increments it.
   */
  fun getCurrentIndex(scopeId: String): Int

  /**
   * Records a tag jump, dropping any entries above the current index
   *
   * Once the stack is at its maximum depth, the oldest entry is dropped.
   */
  fun push(scopeId: String, entry: TagStackEntry)

  /**
   * Walks [count] entries up the stack, towards the position of the first tag jump - `<C-T>` and `:pop`
   *
   * Returns the entry whose [TagStackEntry.line]/[TagStackEntry.col] the caret should be moved to, or `null` when there
   * is nowhere to go: the index is left alone in that case. The caller tells the two failures apart by whether
   * [getEntries] is empty - an empty stack is `E73`, a stack already at the bottom is `E555`.
   */
  fun pop(scopeId: String, count: Int): TagStackEntry?

  /**
   * Walks [count] entries back down the stack, the opposite of [pop] - `:tag` without an argument
   *
   * Returns the entry whose [TagStackEntry.tagName] should be resolved again, or `null` when already at the top
   * (`E556`). Note the asymmetry with [pop], which Vim has too: walking up means going to a position we recorded,
   * while walking down means redoing a tag lookup, because the place a tag jump landed is never stored.
   */
  fun moveDown(scopeId: String, count: Int): TagStackEntry?

  fun clear(scopeId: String)

  fun copyTagStack(fromId: String, toId: String)

  /**
   * Gives a scope that has never had a stack of its own a copy of another scope's stack
   *
   * Mirrors [VimJumpService.inheritJumps]: a new window inherits the tag stack of the window it was split from, even
   * when that stack is empty, while a scope that already has one keeps it.
   */
  fun inheritTagStack(fromId: String, toId: String)

  @TestOnly
  fun resetTagStacks()
}

fun VimTagService.getEntries(editor: VimEditor): List<TagStackEntry> {
  return getEntries(editor.jumpListId)
}

fun VimTagService.getCurrentIndex(editor: VimEditor): Int {
  return getCurrentIndex(editor.jumpListId)
}

fun VimTagService.push(editor: VimEditor, entry: TagStackEntry) {
  return push(editor.jumpListId, entry)
}

/**
 * Records a jump to [tagName] made from the current caret position
 */
fun VimTagService.pushCurrentPosition(editor: VimEditor, tagName: String) {
  val virtualFile = editor.getVirtualFile() ?: return
  val position = editor.offsetToBufferPosition(editor.currentCaret().offset)
  push(editor, TagStackEntry(position.line, position.column, virtualFile.path, virtualFile.protocol, tagName))
}

fun VimTagService.pop(editor: VimEditor, count: Int): TagStackEntry? {
  return pop(editor.jumpListId, count)
}

fun VimTagService.moveDown(editor: VimEditor, count: Int): TagStackEntry? {
  return moveDown(editor.jumpListId, count)
}

fun VimTagService.clear(editor: VimEditor) {
  return clear(editor.jumpListId)
}
