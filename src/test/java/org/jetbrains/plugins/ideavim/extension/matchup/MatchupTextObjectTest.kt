/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.matchup

import com.intellij.ide.highlighter.HtmlFileType
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class MatchupTextObjectTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchup")
  }

  @Test
  fun `test delete inner any block`() {
    doTest(
      "di%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
            a
          else
          ${c}end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inner whole block with count one`() {
    doTest(
      "d1i%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
        ${c}  end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inner outer block with count two`() {
    doTest(
      "d2i%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
        ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inner any block in the first branch`() {
    doTest(
      "di%",
      """
        def foo
          if x
            ${c}a
          else
            b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
        ${c}  else
            b
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete a any block`() {
    doTest(
      "da%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
            a
        ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete a whole block with count one`() {
    doTest(
      "d1a%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
        ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete a outer block with count two`() {
    doTest(
      "d2a%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        ${c}
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete a block deletes whole lines`() {
    doTest(
      "da%",
      """
        def foo
          if x
            ${c}a
          end
        end
      """.trimIndent(),
      """
        def foo
        ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete a block with text after it`() {
    doTest(
      "da%",
      """
        def foo
          if x
            ${c}a
          end; b
        end
      """.trimIndent(),
      """
        def foo
          ${c}; b
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inner block deletes whole lines`() {
    doTest(
      "di%",
      """
        def foo
          if x
            ${c}a
            b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
        ${c}  end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test change inner block`() {
    doTest(
      "ci%X<Esc>",
      """
        def foo
          if x
            ${c}a
          end
        end
      """.trimIndent(),
      """
        def foo
          if${c}X
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test change inner do block changes whole lines`() {
    doTest(
      "ci%X<Esc>",
      """
        foo.each do
          ${c}bar
        end
      """.trimIndent(),
      """
        foo.each do
          ${c}X
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test yank inner block yanks whole lines`() {
    doTest(
      "yi%",
      """
        if x
          ${c}a
        end
      """.trimIndent(),
      """
        if x
        ${c}  a
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
    assertRegisterString('"', "  a\n")
  }

  @Test
  fun `test delete inner block outside of any block does nothing`() {
    doTest(
      "di%",
      """
        if x
        end
        ${c}foo
      """.trimIndent(),
      """
        if x
        end
        ${c}foo
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inner brackets`() {
    doTest(
      "di%",
      """
        call foo(a, ${c}b)
      """.trimIndent(),
      """
        call foo(${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete a brackets`() {
    doTest(
      "da%",
      """
        call foo(a, ${c}b)
      """.trimIndent(),
      """
        call fo${c}o
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inner multi-line brackets`() {
    doTest(
      "di%",
      """
        call foo(
          ${c}a,
          b
        )
      """.trimIndent(),
      """
        call foo(
        ${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete a multi-line brackets`() {
    doTest(
      "da%",
      """
        call foo(
          ${c}a,
          b
        )
      """.trimIndent(),
      """
        call fo${c}o
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inner empty brackets does nothing`() {
    doTest(
      "di%",
      """
        call foo(${c})
      """.trimIndent(),
      """
        call foo(${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test change inner empty brackets`() {
    doTest(
      "ci%X<Esc>",
      """
        call foo(${c})
      """.trimIndent(),
      """
        call foo(${c}X)
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inner nested brackets`() {
    doTest(
      "di%",
      """
        f(a, g(${c}b), c)
      """.trimIndent(),
      """
        f(a, g(${c}), c)
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inner outer brackets with count two`() {
    doTest(
      "d2i%",
      """
        f(a, g(${c}b), c)
      """.trimIndent(),
      """
        f(${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inner html tag on one line`() {
    doTest(
      "di%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>${c}</p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete a html tag on one line`() {
    doTest(
      "da%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
         ${c} 
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete inner html tag on several lines`() {
    doTest(
      "di%",
      """
        <div>
          <p>
            he${c}llo
          </p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>
        ${c}  </p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete a html tag on several lines`() {
    doTest(
      "da%",
      """
        <div>
          <p>
            he${c}llo
          </p>
        </div>
      """.trimIndent(),
      """
        <div>
        ${c}</div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete inner html tag with attributes`() {
    doTest(
      "di%",
      """
        <div class="a">he${c}llo</div>
      """.trimIndent(),
      """
        <div class="a">${c}</div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test select inner any block`() {
    doTest(
      "vi%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
            a
          else
        ${s}    b${c}
        ${se}  end
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test select a any block`() {
    doTest(
      "va%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if x
            a
          ${s}else
            b
          en${c}d${se}
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test select inner whole block with count one`() {
    doTest(
      "v1i%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if${s} x
            a
          else
            b${c}
        ${se}  end
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test select inner block twice grows the selection`() {
    doTest(
      "vi%i%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          if${s} x
            a
          else
            b${c}
        ${se}  end
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test select a block twice grows the selection`() {
    doTest(
      "va%a%",
      """
        def foo
          if x
            a
          else
            ${c}b
          end
        end
      """.trimIndent(),
      """
        def foo
          ${s}if x
            a
          else
            b
          en${c}d${se}
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test select inner brackets`() {
    doTest(
      "vi%",
      """
        call foo(a, ${c}b)
      """.trimIndent(),
      """
        call foo(${s}a, ${c}b${se})
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test select inner brackets twice keeps a one character selection`() {
    doTest(
      "vi%i%",
      """
        f(a, g(${c}b), c)
      """.trimIndent(),
      """
        f(a, g(${s}${c}b${se}), c)
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test select inner multi-line brackets`() {
    doTest(
      "vi%",
      """
        call foo(
          ${c}a,
          b
        )
      """.trimIndent(),
      """
        call foo(
        ${s}  a,
          b${c}
        ${se})
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test select a html tag on one line`() {
    doTest(
      "va%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          ${s}<p>hello</p${c}>${se}
        </div>
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test select inner html tag on one line`() {
    doTest(
      "vi%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>${s}hell${c}o${se}</p>
        </div>
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileType = HtmlFileType.INSTANCE,
    )
  }
}
