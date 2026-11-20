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

class MatchupMotionTest : VimTestCase() {
  @Throws(Exception::class)
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("matchup")
  }

  @Test
  fun `test block start motion jumps to opening keyword of enclosing block`() {
    doTest(
      "[%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        if x
          ${c}if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion jumps to end of closing keyword of enclosing block`() {
    doTest(
      "]%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        if x
          if y
            foo
          en${c}d
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion with count jumps to outer block`() {
    doTest(
      "2[%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        ${c}if x
          if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion with count jumps to outer block`() {
    doTest(
      "2]%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        if x
          if y
            foo
          end
        en${c}d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion with too large count does not move`() {
    doTest(
      "3[%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion with too large count does not move`() {
    doTest(
      "3]%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion twice jumps to outer block`() {
    doTest(
      "[%[%",
      """
        if x
          if y
            ${c}foo
          end
        end
      """.trimIndent(),
      """
        ${c}if x
          if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion on opening keyword jumps to outer block`() {
    doTest(
      "[%",
      """
        if x
          ${c}if y
            foo
          end
        end
      """.trimIndent(),
      """
        ${c}if x
          if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion inside opening keyword jumps to its start`() {
    doTest(
      "[%",
      """
        if x
          i${c}f y
            foo
          end
        end
      """.trimIndent(),
      """
        if x
          ${c}if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion on end of closing keyword jumps to outer block`() {
    doTest(
      "]%",
      """
        if x
          if y
            foo
          en${c}d
        end
      """.trimIndent(),
      """
        if x
          if y
            foo
          end
        en${c}d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion on start of closing keyword jumps to its end`() {
    doTest(
      "]%",
      """
        if x
          if y
            foo
          ${c}end
        end
      """.trimIndent(),
      """
        if x
          if y
            foo
          en${c}d
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion on closing keyword jumps to its opening keyword`() {
    doTest(
      "[%",
      """
        if x
          if y
            foo
          ${c}end
        end
      """.trimIndent(),
      """
        if x
          ${c}if y
            foo
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion skips middle keywords`() {
    doTest(
      "[%",
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
          ${c}if x
            a
          else
            b
          end
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion skips middle keywords`() {
    doTest(
      "]%",
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
            a
          else
            b
          en${c}d
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion skips closed sibling blocks`() {
    doTest(
      "[%",
      """
        def foo
          if x
            a
          end
          ${c}b
        end
      """.trimIndent(),
      """
        ${c}def foo
          if x
            a
          end
          b
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion skips closed sibling blocks`() {
    doTest(
      "]%",
      """
        def foo
          ${c}b
          if x
            a
          end
        end
      """.trimIndent(),
      """
        def foo
          b
          if x
            a
          end
        en${c}d
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion outside of any block does not move`() {
    doTest(
      "[%",
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
  fun `test block end motion outside of any block does not move`() {
    doTest(
      "]%",
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
  fun `test block start motion treats brackets as blocks`() {
    doTest(
      "[%",
      """
        def foo
          bar(1, ${c}2)
        end
      """.trimIndent(),
      """
        def foo
          bar${c}(1, 2)
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion treats brackets as blocks`() {
    doTest(
      "]%",
      """
        def foo
          bar(1, ${c}2)
        end
      """.trimIndent(),
      """
        def foo
          bar(1, 2${c})
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion with count goes from brackets to keyword block`() {
    doTest(
      "2[%",
      """
        def foo
          bar(1, ${c}2)
        end
      """.trimIndent(),
      """
        ${c}def foo
          bar(1, 2)
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion in plain text uses brackets`() {
    doTest(
      "[%",
      """
        foo(a, [b, ${c}c])
      """.trimIndent(),
      """
        foo(a, ${c}[b, c])
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test block end motion in plain text uses brackets`() {
    doTest(
      "]%",
      """
        foo(a, [b, ${c}c])
      """.trimIndent(),
      """
        foo(a, [b, c${c}])
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test block end motion with count in plain text uses brackets`() {
    doTest(
      "2]%",
      """
        foo(a, [b, ${c}c])
      """.trimIndent(),
      """
        foo(a, [b, c]${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test block start motion jumps to enclosing html tag`() {
    doTest(
      "[%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <${c}p>hello</p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test block end motion jumps to enclosing html closing tag`() {
    doTest(
      "]%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>hello</${c}p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test block start motion with count jumps to outer html tag`() {
    doTest(
      "2[%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <${c}div>
          <p>hello</p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test block end motion with count jumps to outer html closing tag`() {
    doTest(
      "2]%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <p>hello</p>
        </di${c}v>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test block start motion ignores html tags without closing tag`() {
    doTest(
      "[%",
      """
        <div>
          <br>
          he${c}llo
        </div>
      """.trimIndent(),
      """
        <${c}div>
          <br>
          hello
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test block start motion jumps to html tag with attributes`() {
    doTest(
      "[%",
      """
        <div class="a">
          he${c}llo
        </div>
      """.trimIndent(),
      """
        <${c}div class="a">
          hello
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete to block end deletes up to closing keyword`() {
    doTest(
      "d]%",
      """
        if x
          ${c}foo
        end
      """.trimIndent(),
      """
        if x
          ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete to block end from middle of line`() {
    doTest(
      "d]%",
      """
        if x
          foo ${c}bar
        end
      """.trimIndent(),
      """
        if x
          foo ${c}end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test forced characterwise delete to block end includes closing keyword`() {
    doTest(
      "dv]%",
      """
        if x
          foo ${c}bar
        end
      """.trimIndent(),
      """
        if x
          foo${c} 
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete to block start deletes back to opening keyword`() {
    doTest(
      "d[%",
      """
        if x
          foo
          ${c}bar
        end
      """.trimIndent(),
      """
        ${c}bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete to block start from start of line keeps newline`() {
    doTest(
      "d[%",
      """
        if x
          foo
        ${c}end
      """.trimIndent(),
      """
        ${c}
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test forced characterwise delete to block start includes cursor character`() {
    doTest(
      "dv[%",
      """
        if x
          foo
          ${c}bar
        end
      """.trimIndent(),
      """
        ${c}ar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete to block end inside brackets`() {
    doTest(
      "d]%",
      """
        call foo(a, ${c}b, c)
      """.trimIndent(),
      """
        call foo(a, ${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete to block start inside brackets`() {
    doTest(
      "d[%",
      """
        call foo(a, ${c}b, c)
      """.trimIndent(),
      """
        call foo${c}b, c)
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test yank to block start yanks back to opening keyword`() {
    doTest(
      "y[%",
      """
        if x
          ${c}foo
        end
      """.trimIndent(),
      """
        ${c}if x
          foo
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test change to block end changes up to closing keyword`() {
    doTest(
      "c]%X<Esc>",
      """
        if x
          foo ${c}bar
        end
      """.trimIndent(),
      """
        if x
          foo ${c}Xend
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete to block end in html`() {
    doTest(
      "d]%",
      """
        <div class="a">
          he${c}llo
        </div>
      """.trimIndent(),
      """
        <div class="a">
          he${c}/div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test delete to block start in html`() {
    doTest(
      "d[%",
      """
        <div>
          <p>he${c}llo</p>
        </div>
      """.trimIndent(),
      """
        <div>
          <${c}llo</p>
        </div>
      """.trimIndent(),
      fileType = HtmlFileType.INSTANCE,
    )
  }

  @Test
  fun `test inside block motion jumps inside nearest bracket`() {
    doTest(
      "z%",
      "${c} call somefunction(      param1, param2)",
      " call somefunction(${c}      param1, param2)",
      fileName = "text.txt",
    )
  }

  @Test
  fun `test delete inside block motion eats whitespace`() {
    doTest(
      "dz%",
      """
        ${c} call somefunction(      param1, param2)
      """.trimIndent(),
      """
        ${c}param1, param2)
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test inside block motion jumps after opening keyword`() {
    doTest(
      "z%",
      """
        ${c}def foo
          bar
        end
      """.trimIndent(),
      """
        def${c} foo
          bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test inside block motion from inside opening keyword`() {
    doTest(
      "z%",
      """
        d${c}ef foo
          bar
        end
      """.trimIndent(),
      """
        def${c} foo
          bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test inside block motion at end of line skips indent`() {
    doTest(
      "z%",
      """
        ${c}foo.each do
          bar
        end
      """.trimIndent(),
      """
        foo.each do
          ${c}bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inside block motion at end of line eats newline and indent`() {
    doTest(
      "dz%",
      """
        ${c}foo.each do
          bar
        end
      """.trimIndent(),
      """
        ${c}bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inside block motion at end of line without indent keeps newline`() {
    doTest(
      "dz%",
      """
        ${c}foo.each do
        bar
        end
      """.trimIndent(),
      """
        ${c}
        bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test delete inside block motion eats trailing whitespace`() {
    doTest(
      "dz%",
      """
        ${c}foo.each do   
          bar
        end
      """.trimIndent(),
      """
        ${c}bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test inside block motion with count`() {
    doTest(
      "2z%",
      """
        ${c}call f(a, g(b))
      """.trimIndent(),
      """
        call f(a, g(${c}b))
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test inside block motion with too large count does not move`() {
    doTest(
      "3z%",
      """
        ${c}call f(a, g(b))
      """.trimIndent(),
      """
        ${c}call f(a, g(b))
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test inside block motion without block ahead does not move`() {
    doTest(
      "z%",
      """
        call f(a, g(b)${c})
      """.trimIndent(),
      """
        call f(a, g(b)${c})
      """.trimIndent(),
      fileName = "text.txt",
    )
  }

  @Test
  fun `test inside block motion ignores middle keywords`() {
    doTest(
      "z%",
      """
        if x
          ${c}foo
        else
          bar
        end
      """.trimIndent(),
      """
        if x
          ${c}foo
        else
          bar
        end
      """.trimIndent(),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block start motion in visual mode extends selection`() {
    doTest(
      "v[%",
      """
        if x
          ${c}foo
        end
      """.trimIndent(),
      """
        ${s}${c}if x
          f${se}oo
        end
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test block end motion in visual mode extends selection`() {
    doTest(
      "v]%",
      """
        if x
          ${c}foo
        end
      """.trimIndent(),
      """
        if x
          ${s}foo
        en${c}d${se}
      """.trimIndent(),
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "ruby.rb",
    )
  }

  @Test
  fun `test inside block motion in visual mode extends selection`() {
    doTest(
      "vz%",
      "${c}call f(a, g(b))",
      "${s}call f(${c}a${se}, g(b))",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
      fileName = "text.txt",
    )
  }
}
