package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class JumpsHandlerTest : VimTestCase() {
  fun `test shows empty list`() {
    configureByText("")
    enterCommand("jumps")
    assertExOutput(" jump line  col file/text\n>\n")
  }

  fun `test show jump list`() {
    configureByText("""I found ${c}it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |
                      |The features it combines mark it as new
                      |to science: shape and shade -- the special tinge,
                      |akin to moonlight, tempering its blue,
                      |the dingy underside, the checquered fringe.
                      """.trimMargin())

    enterSearch("sodden")
    enterSearch("shape")
    enterSearch("rocks", false)
    enterSearch("underside")

    enterCommand("jumps")
    assertExOutput(""" jump line  col file/text
                     |   4     1    8 I found it in a legendary land
                     |   3     3   29 where it was settled on some sodden sand
                     |   2     7   12 to science: shape and shade -- the special tinge,
                     |   1     2    4 all rocks and lavender and tufted grass,
                     |>
                     |""".trimMargin())
  }

  fun `test highlights current jump spot`() {
    configureByText("""I found ${c}it in a legendary land
                      |all rocks and lavender and tufted grass,
                      |where it was settled on some sodden sand
                      |hard by the torrent of a mountain pass.
                      |
                      |The features it combines mark it as new
                      |to science: shape and shade -- the special tinge,
                      |akin to moonlight, tempering its blue,
                      |the dingy underside, the checquered fringe.
                      """.trimMargin())

    enterSearch("sodden")
    enterSearch("shape")
    enterSearch("rocks", false)
    enterSearch("underside")

    typeText(parseKeys("<C-O>", "<C-O>"))

    enterCommand("jumps")
    assertExOutput(""" jump line  col file/text
                     |   2     1    8 I found it in a legendary land
                     |   1     3   29 where it was settled on some sodden sand
                     |>  0     7   12 to science: shape and shade -- the special tinge,
                     |   1     2    4 all rocks and lavender and tufted grass,
                     |   2     9   10 the dingy underside, the checquered fringe.
                     |""".trimMargin())
  }

  fun `test list trims and truncates`() {
    val indent = " ".repeat(100)
    val text = "Really long line ".repeat(1000)
    configureByText(indent + text)

    enterSearch("long")

    enterCommand("jumps")
    assertExOutput(""" jump line  col file/text
                     |   1     1    0 ${text.substring(0, 200)}
                     |>
                     |""".trimMargin())
  }

  fun `test correctly encodes non-printable characters`() {
    configureByText("\u0009Hello\u0006World\u007f")

    typeText(parseKeys("G"))

    enterCommand("jumps")
    assertExOutput(""" jump line  col file/text
                     |   1     1    0 Hello^FWorld^?
                     |>
                     |""".trimMargin())
  }
}
