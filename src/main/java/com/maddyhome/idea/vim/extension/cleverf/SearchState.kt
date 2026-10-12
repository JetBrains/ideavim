/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.cleverf

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.newapi.ij
import java.lang.ref.WeakReference

/**
 * The search started by [map] for [char] in [editor], and where it last left the primary caret.
 *
 * [map] is the key that started the search and defines its direction for the repeats. Until the first successful
 * jump, the position is where the search started.
 */
internal class SearchState(val map: Char, val char: Char, editor: VimEditor) {
  private var editor = WeakReference(editor.ij)
  private var offset = editor.primaryCaret().offset
  private var jumpTime = 0L
  private var hasJumped = false

  fun firstSearch(): CharSearch = CharSearch(this, map, char, isFirstMove = true)

  /**
   * By default, the direction of a repeat is relative to the original search: a lowercase key continues it and an
   * uppercase key reverses it. With `g:clever_f_fix_key_direction` a lowercase key always searches forward and an
   * uppercase key always backward.
   */
  fun repeatSearch(back: Boolean): CharSearch {
    val reverse = if (CleverFSettings.fixKeyDirection && map.isUpperCase()) !back else back
    val effectiveMap = if (reverse) map.swapCase() else map
    return CharSearch(this, effectiveMap, char, isFirstMove = !hasJumped)
  }

  fun canRepeat(editor: VimEditor): Boolean =
    hasJumped && !isTimedOut() && isAt(editor.ij, editor.primaryCaret().offset)

  fun isAt(editor: Editor, offset: Int): Boolean = isIn(editor) && this.offset == offset

  fun isIn(editor: Editor): Boolean = this.editor.get() === editor

  fun isIn(document: Document): Boolean = editor.get()?.document === document

  fun recordJump(editor: VimEditor, offset: Int) {
    this.editor = WeakReference(editor.ij)
    this.offset = offset
    jumpTime = System.currentTimeMillis()
    hasJumped = true
  }

  private fun isTimedOut(): Boolean {
    val timeout = CleverFSettings.timeoutMs
    return timeout > 0 && System.currentTimeMillis() - jumpTime > timeout
  }

  private fun Char.swapCase(): Char = if (isUpperCase()) lowercaseChar() else uppercaseChar()
}
