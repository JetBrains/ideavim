package org.jetbrains.plugins.ideavim.extension.exchange

import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.extension.exchange.VimExchangeExtension
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.helper.VimBehaviorDiffers
import com.maddyhome.idea.vim.option.ClipboardOptionsData
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

class VimExchangeWithClipboardTest : VimOptionTestCase(ClipboardOptionsData.name) {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("exchange")
  }

  // |cx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test exchange words left to right`() {
    doTest(StringHelper.parseKeys("cxe", "w", "cxe"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |cx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test exchange words dot repeat`() {
    doTest(StringHelper.parseKeys("cxiw", "w", "."),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |cx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test exchange words right to left`() {
    doTest(StringHelper.parseKeys("cxe", "b", "cxe"),
      "The quick brown ${c}fox catch over the lazy dog",
      "The quick ${c}fox brown catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |cx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test exchange words right to left with dot`() {
    doTest(StringHelper.parseKeys("cxe", "b", "."),
      "The quick brown ${c}fox catch over the lazy dog",
      "The quick ${c}fox brown catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |X|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test visual exchange words left to right`() {
    doTest(StringHelper.parseKeys("veX", "w", "veX"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The quick fox ${c}brown catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |X|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter = "The ${c}brown catch over the lazy dog",
    shouldBeFixed = true
  )
  fun `test visual exchange words from inside`() {
    doTest(StringHelper.parseKeys("veX", "b", "v3e", "X"),
      "The quick ${c}brown fox catch over the lazy dog",
      "The brow${c}n catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |X|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter = "The brown ${c}catch over the lazy dog",
    shouldBeFixed = true
  )
  fun `test visual exchange words from outside`() {
    doTest(StringHelper.parseKeys("v3e", "X", "w", "veX"),
      "The ${c}quick brown fox catch over the lazy dog",
      "The brow${c}n catch over the lazy dog",
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |cxx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter =
    """The quick
       catch over
       ${c}brown fox
       the lazy dog
       """,
    shouldBeFixed = true
  )
  fun `test exchange lines top down`() {
    doTest(StringHelper.parseKeys("cxx", "j", "cxx"),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog""".trimIndent(),
      """The quick
         ${c}catch over
         brown fox
         the lazy dog""".trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  // |cxx|
  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter =
    """The quick
       catch over
       ${c}brown fox
       the lazy dog
       """,
    shouldBeFixed = true
  )
  fun `test exchange lines top down with dot`() {
    doTest(StringHelper.parseKeys("cxx", "j", "."),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog""".trimIndent(),
      """The quick
         ${c}catch over
         brown fox
         the lazy dog""".trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter = """
         The quick
         brown thecatch over
         fox
          lazy dog
    """
  )
  fun `test exchange to the line end`() {
    doTest(StringHelper.parseKeys("v$", "X", "jj^ve", "X"),
      """The quick
         brown ${c}fox
         catch over
         the lazy dog""".trimIndent(),
      """The quick
         brown the
         catch over
         fox lazy dog""".trimIndent(),
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  @VimBehaviorDiffers(
    originalVimAfter =
    """
         catch over
         the lazy dog
         ${c}The quick
         brown fox
      """,
    shouldBeFixed = true
  )
  fun `test exchange visual lines`() {
    doTest(StringHelper.parseKeys("Vj", "X", "jj", "Vj", "X"),
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
      CommandState.Mode.COMMAND,
      CommandState.SubMode.NONE
    )
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test visual char highlighter`() {
    val before = """
         The ${c}quick
         brown fox
         catch over
         the lazy dog
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("vlll", "X"))

    assertHighlighter(4, 8, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test visual line highdhitligthhter`() {
    val before = """
         The ${c}quick
         brown fox
         catch over
         the lazy dog
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("Vj", "X"))

    assertHighlighter(4, 15, HighlighterTargetArea.LINES_IN_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test till the line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("v$", "X"))

    assertHighlighter(4, 10, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test pre line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("v\$h", "X"))

    assertHighlighter(4, 9, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test pre pre line end highlighter`() {
    val before = """
         The ${c}quick
         brown fox
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("v\$hh", "X"))

    assertHighlighter(4, 8, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test to file end highlighter`() {
    val before = """
         The quick
         brown ${c}fox
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("v\$", "X"))

    assertHighlighter(16, 19, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  @VimOptionTestConfiguration(VimTestOption(ClipboardOptionsData.name, VimTestOptionType.LIST, ["unnamed"]))
  fun `test to file end with new line highlighter`() {
    val before = """
         The quick
         brown ${c}fox
         
         """.trimIndent()
    configureByText(before)
    typeText(StringHelper.parseKeys("v\$", "X"))

    assertHighlighter(16, 20, HighlighterTargetArea.EXACT_RANGE)

    // Exit vim-exchange
    exitExchange()
  }

  private fun exitExchange() {
    typeText(StringHelper.parseKeys("cxc"))
  }

  private fun assertHighlighter(start: Int, end: Int, area: HighlighterTargetArea) {
    val currentExchange = myFixture.editor.getUserData(VimExchangeExtension.EXCHANGE_KEY)!!
    val highlighter = currentExchange.getHighlighter()!!
    assertEquals(start, highlighter.startOffset)
    assertEquals(end, highlighter.endOffset)
    assertEquals(area, highlighter.targetArea)
  }
}
