/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.targets

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Port of targets.vim `s:testBasic` (test1, last fixture) for the **argument** source.
 *
 * Trigger `a` (`ia aa Ia Aa`). Arguments are delimited by braces `( [` / `] )` and commas, and
 * balanced braces are respected. Expected results transcribed from `test/test1.ok` for the fixture
 * `a ( b , c ( d ) , d ( x , e ) , f ) g`.
 *
 * NB: IdeaVim already ships the `argtextobj` extension providing `aa`/`ia`; these tests target the
 * `targets` extension's own argument source plus its seeking / `n` / `l` / `I` / `A` modifiers.
 *
 * See the README "Argument Text Objects" section and `cheatsheet.md` argument chart.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsArgumentTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // Cursor on `x`, the first argument of the inner `( x , e )`.
  private val line = "a ( b , c ( d ) , d ( ${c}x , e ) , f ) g"

  @ParameterizedTest(name = "change argument: {0}")
  @MethodSource("argumentChangeCases")
  fun `change argument`(keys: String, after: String) {
    doTest(keys, line, after, Mode.INSERT)
  }

  // ---- the four modifiers, visual operator ----

  @Test
  fun `visual inside argument`() {
    doTest(
      "via",
      line,
      "a ( b , c ( d ) , d ($s x $se, e ) , f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Inside argument excludes whitespace`() {
    doTest(
      "vIa",
      line,
      "a ( b , c ( d ) , d ( ${s}x$se , e ) , f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual an argument includes a separator`() {
    doTest(
      "vaa",
      line,
      "a ( b , c ( d ) , d ( ${s}x , ${se}e ) , f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Around argument`() {
    doTest(
      "vAa",
      line,
      "a ( b , c ( d ) , d $s( x , ${se}e ) , f ) g",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  companion object {
    @JvmStatic
    fun argumentChangeCases(): List<Array<String>> {
      val c = VimTestCase.c
      return listOf(
        // last argument (`l`)
        arrayOf("cIla", "a ( b , $c , d ( x , e ) , f ) g"),
        arrayOf("cila", "a ( b ,$c, d ( x , e ) , f ) g"),
        arrayOf("cala", "a ( b $c, d ( x , e ) , f ) g"),
        arrayOf("cAla", "a ( b ${c}d ( x , e ) , f ) g"),
        // current argument
        arrayOf("cIa", "a ( b , c ( d ) , d ( $c , e ) , f ) g"),
        arrayOf("cia", "a ( b , c ( d ) , d ($c, e ) , f ) g"),
        arrayOf("caa", "a ( b , c ( d ) , d ( ${c}e ) , f ) g"),
        arrayOf("cAa", "a ( b , c ( d ) , d ${c}e ) , f ) g"),
        // next argument (`n`)
        arrayOf("cIna", "a ( b , c ( d ) , d ( x , $c ) , f ) g"),
        arrayOf("cina", "a ( b , c ( d ) , d ( x ,$c) , f ) g"),
        arrayOf("cana", "a ( b , c ( d ) , d ( x$c ) , f ) g"),
        arrayOf("cAna", "a ( b , c ( d ) , d ( x $c, f ) g"),
        // count 1 == no count
        arrayOf("c1ia", "a ( b , c ( d ) , d ($c, e ) , f ) g"),
        // count 2 grows to the enclosing argument list
        arrayOf("c2Ila", "a ( b , c ( $c ) , d ( x , e ) , f ) g"),
        arrayOf("c2ila", "a ( b , c ($c) , d ( x , e ) , f ) g"),
        arrayOf("c2Ala", "a ( b , c $c, d ( x , e ) , f ) g"),
      )
    }
  }
}
