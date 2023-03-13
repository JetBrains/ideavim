/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.LineDeleteShift
import com.maddyhome.idea.vim.api.MutableVimEditor
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.setChangeMarks
import com.maddyhome.idea.vim.api.toType
import com.maddyhome.idea.vim.mark.VimMarkConstants
import kotlin.math.max
import kotlin.math.min

public data class Offset(val point: Int)
public data class Pointer(val point: Int)

public val Int.offset: Offset
  get() = Offset(this)
public val Int.pointer: Pointer
  get() = Pointer(this)

public sealed class EditorLine private constructor(public val line: Int) {
  public class Pointer(line: Int) : EditorLine(line) {
    public companion object {
      public fun init(line: Int, forEditor: VimEditor): Pointer {
        if (line < 0) error("")
        if (line >= forEditor.lineCount()) error("")
        return Pointer(line)
      }
    }
  }

  public class Offset(line: Int) : EditorLine(line) {

    public fun toPointer(forEditor: VimEditor): Pointer {
      return Pointer.init(line.coerceAtMost(forEditor.lineCount() - 1), forEditor)
    }

    public companion object {
      public fun init(line: Int, forEditor: VimEditor): Offset {
        if (line < 0) error("")
        // TODO: 28.12.2021 Is this logic correct?
        //   IJ has an additional line
        if (line > forEditor.lineCount()) error("")
        return Offset(line)
      }
    }
  }
}

public sealed class OperatedRange {
  public class Lines(
    public val text: CharSequence,
    public val lineAbove: EditorLine.Offset,
    public val linesOperated: Int,
    public val shiftType: LineDeleteShift,
  ) : OperatedRange()

  public class Characters(public val text: CharSequence, public val leftOffset: Offset, public val rightOffset: Offset) : OperatedRange()
  public class Block : OperatedRange() {
    init {
      TODO()
    }
  }
}
