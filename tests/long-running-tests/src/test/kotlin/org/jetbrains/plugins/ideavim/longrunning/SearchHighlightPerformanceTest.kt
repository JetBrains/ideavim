/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.longrunning

import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.longrunning.SearchHighlightPerformanceTest.Companion.LINE
import org.jetbrains.plugins.ideavim.longrunning.SearchHighlightPerformanceTest.Companion.LINE_COUNT
import org.junit.jupiter.api.Test

/**
 * Benchmarks the worst case for search highlighting: `/.` over a large file, which matches every single character and
 * therefore produces one highlight per character.
 *
 * Vim computes 'hlsearch' attributes lazily, per screen cell, during redraw, and keeps no highlight objects at all -
 * `/.` costs it nothing beyond what is on screen. IdeaVim has to materialise a
 * [com.intellij.openapi.editor.markup.RangeHighlighter] per match into the editor's markup model, so
 * [com.maddyhome.idea.vim.helper.SearchHighlights] only materialises the matches that could be on screen. What this
 * benchmark measures is therefore the search plus a screen's worth of highlighter insertions, not
 * [LINE_COUNT] * [LINE].length of them - which is the whole point of scoping them to the viewport, and what the
 * numbers should show against an earlier run.
 *
 * This needs 'hlsearch' - it is off by default ([com.maddyhome.idea.vim.api.Options.hlsearch]), and with it off no
 * highlighters are created and there is nothing left to measure.
 *
 * Results are reported rather than asserted against a hard-coded budget: the harness publishes metrics to
 * `.intellijPlatform/sandbox/long-running-tests/<ide>/teamcity-artifacts-for-publish/` for IJ Perf to trend. Run it
 * with `./gradlew :tests:long-running-tests:test --tests "*SearchHighlightPerformanceTest*" --console=plain`.
 */
@TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
class SearchHighlightPerformanceTest : VimTestCase() {

  @Test
  fun `test search matching every character of a large file`() {
    configureByLines(LINE_COUNT, LINE)
    enterCommand("set hlsearch")

    PlatformTestUtil.newBenchmark("ideavim search dot ${LINE_COUNT / 1000}k lines") { enterSearch(".") }
      // Searching for the same pattern twice in a row is a no-op - `SearchHighlights` compares the request against
      // what the editor is already showing - so without this reset every attempt after the first would measure
      // nothing. `:nohlsearch` clears the highlights, so each attempt starts from an empty markup model; the measured
      // `/` turns highlighting back on itself via `setShouldShowSearchHighlights`. `gg` keeps the starting offset
      // constant. Setup is excluded from the measurement by the harness.
      .setup {
        enterCommand("nohlsearch")
        typeText("gg")
      }
      .warmupIterations(WARMUP_ITERATIONS)
      .attempts(ATTEMPTS)
      // The harness refuses to run outside stress mode ("not good for reliable benchmarks"). It also disables
      // debug-level platform assertions, which keeps the numbers closer to a production run.
      .runAsStressTest()
      .start()
  }

  companion object {
    private const val LINE_COUNT = 5000

    /**
     * Short on purpose. `/.` matches every character, so the search cost is driven by
     * [LINE_COUNT] * [LINE].length, not by the line count alone. It used to also drive the highlighter count, and
     * with a full 78-character Lorem line 10k lines meant 780k highlighters and an OutOfMemoryError. Keep the product
     * around 10^5 so the benchmark stays comparable with the runs from before the viewport scoping.
     */
    private const val LINE = "lorem ipsum,"

    private const val WARMUP_ITERATIONS = 2
    private const val ATTEMPTS = 5
  }
}
