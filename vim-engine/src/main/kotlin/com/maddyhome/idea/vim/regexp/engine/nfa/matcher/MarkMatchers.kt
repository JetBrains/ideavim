/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.engine.nfa.matcher

import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.regexp.match.VimMatchGroupCollection

internal class AtMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>
  ): MatcherResult {
    val markIndexes = possibleCursors
      .mapNotNull { it.markStorage.getMark(mark) }
      .map { editor.bufferPositionToOffset(BufferPosition(it.line, it.col)) }

    return if (markIndexes.contains(index)){
      // now the only cursors possible are that contain a mark at this index
      val newPossibleCursors = possibleCursors.filter {
        it.markStorage.getMark(mark) != null &&
        index == editor.bufferPositionToOffset(BufferPosition(it.markStorage.getMark(mark)!!.line, it.markStorage.getMark(mark)!!.col))
      }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    }
    else MatcherResult.Failure
  }
}

internal class BeforeMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>
  ): MatcherResult {
    val markIndexes = possibleCursors
      .mapNotNull { it.markStorage.getMark(mark) }
      .map { editor.bufferPositionToOffset(BufferPosition(it.line, it.col)) }

    return if (markIndexes.any { index < it }){
      // now the only cursors possible are that contain a mark after this index
      val newPossibleCursors = possibleCursors.filter {
        it.markStorage.getMark(mark) != null &&
          index < editor.bufferPositionToOffset(BufferPosition(it.markStorage.getMark(mark)!!.line, it.markStorage.getMark(mark)!!.col))
      }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    }
    else MatcherResult.Failure
  }
}

internal class AfterMarkMatcher(val mark: Char) : Matcher {
  override fun matches(
    editor: VimEditor,
    index: Int,
    groups: VimMatchGroupCollection,
    isCaseInsensitive: Boolean,
    possibleCursors: MutableList<VimCaret>
  ): MatcherResult {
    val markIndexes = possibleCursors
      .mapNotNull { it.markStorage.getMark(mark) }
      .map { editor.bufferPositionToOffset(BufferPosition(it.line, it.col)) }

    return if (markIndexes.any { index > it }){
      // now the only cursors possible are that contain a mark before this index
      val newPossibleCursors = possibleCursors.filter {
        it.markStorage.getMark(mark) != null &&
          index > editor.bufferPositionToOffset(BufferPosition(it.markStorage.getMark(mark)!!.line, it.markStorage.getMark(mark)!!.col))
      }
      possibleCursors.clear()
      possibleCursors.addAll(newPossibleCursors)
      MatcherResult.Success(0)
    }
    else MatcherResult.Failure
  }
}