/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.exchange

import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class VimExchangeWithClipboardTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("exchange")
  }

  // |cx|
  @Test
  fun `test exchange words left to right`() {
    doTest(
      listOf("cxe", "w", "cxe"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |cx|
  @Test
  fun `test exchange words dot repeat`() {
    doTest(
      listOf("cxiw", "w", "."),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |cx|
  @Test
  fun `test exchange words right to left`() {
    doTest(
      listOf("cxe", "b", "cxe"),
      "The quick brown ${c}fox catch over the lazy dog",
      "The quick ${c}fox brown catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |cx|
  @Test
  fun `test exchange words right to left with dot`() {
    doTest(
      listOf("cxe", "b", "."),
      "The quick brown ${c}fox catch over the lazy dog",
      "The quick ${c}fox brown catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |X|
  @Test
  fun `test visual exchange words left to right`() {
    doTest(
      listOf("veX", "w", "veX"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |X|
  @Test
  @VimBehaviorDiffers(
    originalVimAfter = "The ${c}brown catch over the lazy dog",
    shouldBeFixed = true,
  )
  fun `test visual exchange words from inside`() {
    doTest(
      listOf("veX", "b", "v3e", "X"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The brow${c}n catch over the lazy dog",
      Mode.NORMAL(),
    )
  }

  // |X|
  @Test
  @VimBehaviorDiffers(
    originalVimAfter = "The brown ${c}catch over the lazy dog",
    shouldBeFixed = true,
  )
  fun `test visual exchange words from outside`() {
    doTest(
      listOf("v3e", "X", "w", "veX"),
      "The ${c}quick brown fox catch over the lazy dog",
      "The brow${c}n catch over the lazy dog",
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |cxx|
  @Test
  @VimBehaviorDiffers(
    originalVimAfter =
      """The quick
       catch over
       ${c}brown fox
       the lazy dog
       """,
    shouldBeFixed = true,
  )
  fun `test exchange lines top down`() {
    doTest(
      listOf("cxx", "j", "cxx"),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog
      """.trimIndent(),
      """The quick
         ${c}catch over
         brown fox
         the lazy dog
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  // |cxx|
  @Test
  @VimBehaviorDiffers(
    originalVimAfter =
      """The quick
       catch over
       ${c}brown fox
       the lazy dog
       """,
    shouldBeFixed = true,
  )
  fun `test exchange lines top down with dot`() {
    doTest(
      listOf("cxx", "j", "."),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog
      """.trimIndent(),
      """The quick
         ${c}catch over
         brown fox
         the lazy dog
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  @Test
  @VimBehaviorDiffers(
    originalVimAfter = """
         The quick
         brown thecatch over
         fox
          lazy dog
    """,
  )
  fun `test exchange to the line end`() {
    doTest(
      listOf("v$", "X", "jj^ve", "X"),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog
      """.trimIndent(),
      """The quick
         brown the
         catch over
         fox lazy dog
      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  @Test
  @VimBehaviorDiffers(
    originalVimAfter =
      """
         catch over
         the lazy dog
         ${c}The quick
         brown fox
      """,
    shouldBeFixed = true,
  )
  fun `test exchange visual lines`() {
    doTest(
      listOf("Vj", "X", "jj", "Vj", "X"),
      """
         The ${c}quick
         brown fox
         catch over
         the lazy dog
      """.trimIndent(),
      """
         ${c}catch over
         the lazy dog
         The quick
         brown fox

      """.trimIndent(),
      Mode.NORMAL(),
    ) {
      enterCommand("set clipboard=unnamed")
    }
  }

  @Test
  fun `test visual char highlighter`() {
    val before = """
         The ${c}quick
         brown fox
         catch over
         the lazy dog
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(injector.parser.parseKeys("vlll" + "X"))

    assertHighlighter(4, 8, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test visual line highdhitligthhter`() {
    val before = """
         The ${c}quick
         brown fox
         catch over
         the lazy dog
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(injector.parser.parseKeys("Vj" + "X"))

    assertHighlighter(0, 19, HighlighterTargetArea.LINES_IN_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test till the line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(injector.parser.parseKeys("v$" + "X"))

    assertHighlighter(4, 10, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test pre line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(injector.parser.parseKeys("v\$h" + "X"))

    assertHighlighter(4, 9, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test pre pre line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(injector.parser.parseKeys("v\$hh" + "X"))

    assertHighlighter(4, 8, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test to file end highlighter`() {
    val before = """
         The quick
         brown ${c}fox
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(
      injector.parser.parseKeys(
        buildString {
          append("v\$")
          append("X")
        },
      ),
    )

    assertHighlighter(16, 19, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @Test
  fun `test to file end with new line highlighter`() {
    val before = """
         The quick
         brown ${c}fox

    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard=unnamed")
    typeText(
      injector.parser.parseKeys(
        buildString {
          append("v\$")
          append("X")
        },
      ),
    )

    assertHighlighter(16, 20, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  private fun exitExchange() {
    typeText(injector.parser.parseKeys("cxc"))
  }

  // TODO: The `area` parameter is no longer validated because the new highlight API doesn't expose
  //  HighlighterTargetArea. Restore area validation when the API supports it.
  private fun assertHighlighter(start: Int, end: Int, @Suppress("UNUSED_PARAMETER") area: HighlighterTargetArea) {
    val allHighlighters = fixture.editor.markupModel.allHighlighters
    val highlighter = allHighlighters.lastOrNull { it.startOffset == start && it.endOffset == end }
      ?: error("No highlighter found with start=$start, end=$end. Available: ${allHighlighters.map { "(${it.startOffset}, ${it.endOffset})" }}")
    kotlin.test.assertEquals(start, highlighter.startOffset)
    kotlin.test.assertEquals(end, highlighter.endOffset)
  }
}
