package org.jetbrains.plugins.ideavim.action.change.shift

import com.maddyhome.idea.vim.helper.StringHelper
import org.jetbrains.plugins.ideavim.VimTestCase

class ShiftLeftTest : VimTestCase() {
  fun `test shift till new line`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(StringHelper.parseKeys("<W"), file)
    myFixture.checkResult("""
            A Discovery

            I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent())
  }

  fun `test shift ctrl-D`() {
    val file = """
            A Discovery

              I found it in a legendary l${c}and
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(StringHelper.parseKeys("i<C-D>"), file)
    myFixture.checkResult("""
            A Discovery

            I found it in a legendary land
              all rocks and lavender and tufted grass,
              where it was settled on some sodden sand
              hard by the torrent of a mountain pass.
        """.trimIndent())
  }
}
