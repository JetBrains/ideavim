/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

/**
 * Manages the state of an active command-line completion session.
 *
 * Created when Tab is first pressed, and cycles through matches on subsequent Tab/Shift-Tab presses.
 * Invalidated when the command-line text is modified by anything other than the completion action.
 *
 * @param originalText The full command-line text before completion was triggered
 * @param completionStart The offset in the command-line text where the argument being completed starts
 * @param matches Sorted list of completion candidates
 */
class CommandLineCompletion(
  val originalText: String,
  val completionStart: Int,
  val matches: List<String>,
) {
  var currentIndex: Int = -1
    private set

  /** The full command-line text that was set after applying the current match */
  var expectedText: String = originalText
    private set

  fun nextMatch(): String? {
    if (matches.isEmpty()) return null
    currentIndex = (currentIndex + 1) % matches.size
    return matches[currentIndex]
  }

  fun previousMatch(): String? {
    if (matches.isEmpty()) return null
    currentIndex = if (currentIndex <= 0) matches.size - 1 else currentIndex - 1
    return matches[currentIndex]
  }

  fun updateExpectedText(text: String) {
    expectedText = text
  }
}
