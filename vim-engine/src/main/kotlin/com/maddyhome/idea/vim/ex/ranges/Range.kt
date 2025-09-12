/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.ex.ranges

import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.exExceptionMessage
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly

/**
 * Handles the set of range values entered as part of an Ex command.
 */
class Range {
  // This property should be private, but is used in tests
  @TestOnly
  val addresses: MutableList<Address> = mutableListOf()

  /** Adds a range to the list */
  fun addAddresses(range: Array<Address>) {
    addresses.addAll(range)
  }

  /** Gets the number of ranges in the list */
  fun size(): Int = addresses.size

  /**
   * The default range for a command, if not specified.
   *
   * Most commands default to the current line (`.`). If the command expects a count, the default range would be `1`,
   * which would be a one-based count.
   */
  var defaultRange: String = "."

  /**
   * If a command expects a line, Vim uses the last line of any range passed to the command
   *
   * @param editor  The editor to get the line for
   * @param caret   The caret to use for current line, initial search line, etc. if required
   * @return The line number represented by the range
   */
  fun getLine(editor: VimEditor, caret: VimCaret): Int {
    return getLineRange(editor, caret).endLine
  }

  /**
   * If a command expects a count, Vim uses the last line of the range passed to the command
   *
   * Note that the command may also have a count passed as an argument, which takes precedence over any range. This
   * function only returns the count from the range. It is up to the caller to decide which count to use.
   *
   * @param editor  The editor to get the count for
   * @param caret   The caret to use for current line, initial search line, etc. if required
   * @return The last line specified in the range, to be treated as a count (one-based)
   */
  fun getCount(editor: VimEditor, caret: VimCaret): Int {
    return processRange(editor, caret).endLine1
  }

  /**
   * Gets the line range represented by this Ex range
   *
   * @param editor  The editor to get the range for
   * @param caret   The caret to use for current line, initial search line, etc. if required
   * @return The line range (zero-based)
   */
  fun getLineRange(editor: VimEditor, caret: VimCaret): LineRange {
    return processRange(editor, caret)
  }

  private fun processRange(editor: VimEditor, caret: VimCaret): LineRange {
    var startLine1 = 0
    var endLine1 = 0

    // Now process each range component, moving the cursor if appropriate
    val addresses = this.addresses.ifEmpty {
      Address.createRangeAddresses(defaultRange, 0, false)?.toList()
        ?: throw exExceptionMessage("E16")
    }
    for (address in addresses) {
      startLine1 = endLine1
      endLine1 = address.getLine1(editor, caret)
      if (address.isMove) {
        caret.moveToOffset(injector.motion.moveCaretToLineWithSameColumn(editor, endLine1 - 1, caret))
      }
    }

    // Offsets might give us a negative start/end line, which Vim treats as an error. A value of 0 is still acceptable.
    if (startLine1 < 0 || endLine1 < 0) {
      throw exExceptionMessage("E16")
    }

    // If only one address is given, make the start and end the same
    if (addresses.size == 1) startLine1 = endLine1

    // It is valid for the 1-based lines to be 0. E.g. copy/move use an address of 0 to copy/move to the line _before_
    // the first line (any other value is treated as _after_ that line) or `:0,$d` will delete the whole buffer, just
    // like `:1,$d`. LineRange is constructed with 0-based lines, which are coerced to 0, while the 1-based accessors
    // maintain the actual value we pass through (it might be neater to create LineRange with 1-based line numbers...)
    return LineRange(startLine1 - 1, endLine1 - 1)
  }

  @NonNls
  override fun toString(): String = "Ranges[addresses=$addresses]"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Range) return false

    if (defaultRange != other.defaultRange) return false
    if (addresses != other.addresses) return false

    return true
  }

  override fun hashCode(): Int {
    var result = defaultRange.hashCode()
    result = 31 * result + addresses.hashCode()
    return result
  }
}
