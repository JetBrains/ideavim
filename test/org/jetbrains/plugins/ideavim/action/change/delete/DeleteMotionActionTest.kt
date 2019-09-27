@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class DeleteMotionActionTest : VimTestCase() {

  fun `ignoreTest delete last line`() {
    typeTextInFile(parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
          """.trimIndent())
    myFixture.checkResult("""
        def xxx():
          ${c}expression one
          """.trimIndent())
  }

  fun `ignoreTest delete last line stored with new line`() {
    typeTextInFile(parseKeys("dd"),
      """
        def xxx():
          expression one
          expression${c} two
          """.trimIndent())
    val savedText = VimPlugin.getRegister().lastRegister?.text ?: ""
    assertEquals("  expression two\n", savedText)
  }

  fun `ignoreTest delete line action multicaret`() {
    typeTextInFile(parseKeys("d3d"),
      """
        abc${c}de
        abcde
        abcde
        abcde
        ab${c}cde
        abcde
        abcde
        
        """.trimIndent())
    myFixture.checkResult("${c}abcde\n")
  }

  fun `test delete motion action multicaret`() {
    typeTextInFile(parseKeys("dt)"),
      """|public class Foo {
         |  int foo(int a, int b) {
         |    boolean bar = (a < 0 && (b < 0 || a > 0)${c} || b != 0);
         |    if (bar${c} || b != 0) {
         |      return a;
         |    }
         |    else {
         |      return b;
         |    }
         |  }
         |}
        """.trimMargin()
    )
    myFixture.checkResult(
      """|public class Foo {
         |  int foo(int a, int b) {
         |    boolean bar = (a < 0 && (b < 0 || a > 0)${c});
         |    if (bar${c}) {
         |      return a;
         |    }
         |    else {
         |      return b;
         |    }
         |  }
         |}
         """.trimMargin()
    )
  }

  fun `test delete empty line`() {
    val file = """
            A Discovery
            ${c}
            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val newFile = """
            A Discovery
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    typeTextInFile(parseKeys("dd"), file)
    myFixture.checkResult(newFile)
  }
}
