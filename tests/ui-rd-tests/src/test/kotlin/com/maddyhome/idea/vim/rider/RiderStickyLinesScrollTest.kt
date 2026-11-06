/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Scrolling with 'scrolloff' and the sticky lines panel. In Rider, the sticky lines of C# come from the ReSharper
 * backend, and the panel changes height as the scopes at the top of the screen start and end
 */
class RiderStickyLinesScrollTest : IdeaVimRiderTestBase() {

  @AfterEach
  fun resetMode() {
    esc()
  }

  // VIM-4372. A scrolloff of 20 in a window of about 40 lines switches to centring when the panel takes the window
  // below twice the scrolloff
  @ParameterizedTest
  @ValueSource(ints = [20, 999])
  fun `moving left and right does not scroll with sticky lines`(scrollOffset: Int) {
    exCommand("set scrolloff=$scrollOffset")
    setText(TEXT)
    typeVim("gg")

    val shakes = mutableListOf<String>()
    var stickyLinesShown = false
    repeat(LINES_TO_CHECK) {
      typeVim("j")
      settle()
      for (motion in listOf("l", "h", "l", "h")) {
        val before = verticalScroll()
        typeVim(motion)
        settle()
        val after = verticalScroll()
        if (after.second > 0) stickyLinesShown = true
        if (after.first != before.first) {
          shakes += "'$motion' on line ${caretLine()}: offset ${before.first} -> ${after.first}, " +
            "sticky lines panel ${before.second} -> ${after.second} px"
        }
      }
    }

    assertTrue(stickyLinesShown) { "The sticky lines panel was never shown" }
    assertTrue(shakes.isEmpty()) {
      "Moving within a line scrolled ${shakes.size} times:\n" + shakes.take(10).joinToString("\n")
    }
  }

  /** Waits until the editor has stopped scrolling, after the key and the sticky lines panel it changed are handled */
  private fun settle() {
    var last = verticalScroll()
    waitUntil(timeoutMs = 2000, pollMs = 300) {
      val current = verticalScroll()
      (current == last).also { last = current }
    }
  }

  companion object {
    private const val LINES_TO_CHECK = 80

    // Methods with nested blocks, so the panel grows and shrinks one line at a time
    private val TEXT = buildString {
      appendLine("namespace ConsoleApp")
      appendLine("{")
      repeat(2) { c ->
        appendLine("    public class Worker$c")
        appendLine("    {")
        repeat(4) { m ->
          appendLine("        public int Run$m(int value)")
          appendLine("        {")
          appendLine("            var total = 0;")
          appendLine("            for (var i = 0; i < value; i++)")
          appendLine("            {")
          appendLine("                if (i % 2 == 0)")
          appendLine("                {")
          appendLine("                    while (total < 100)")
          appendLine("                    {")
          repeat(12) { k -> appendLine("                        total += i * $k + value; // some line of code $k") }
          appendLine("                    }")
          appendLine("                }")
          appendLine("            }")
          appendLine("            return total;")
          appendLine("        }")
          appendLine()
        }
        appendLine("    }")
        appendLine()
      }
      appendLine("}")
    }
  }
}
