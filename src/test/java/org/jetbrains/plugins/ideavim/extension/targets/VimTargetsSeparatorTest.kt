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
 * Port of targets.vim `s:testBasic` (test1) for the **separator** source.
 *
 * Separator characters: `, . ; : + - = ~ _ * # / | \ & $`. Behaviour is identical across the set,
 * so the exhaustive matrix uses the comma fixture (transcribed from `test/test1.ok`), with a couple
 * of other separators spot-checked. Like quotes, count 2 with no `n`/`l` qualifier is unsupported
 * and not exercised.
 *
 * See the README "Separator Text Objects" section and `cheatsheet.md` separator chart.
 */
@Suppress("SpellCheckingInspection")
class VimTargetsSeparatorTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("targets")
  }

  // Cursor on `x`, surrounded by commas: `e , x , g`.
  private val line = "a , b , c , d , e , ${c}x , g , h , i , k , l"

  @ParameterizedTest(name = "change separator: {0}")
  @MethodSource("separatorChangeCases")
  fun `change separator`(keys: String, after: String) {
    doTest(keys, line, after, Mode.INSERT)
  }

  // ---- the four modifiers, visual operator ----

  @Test
  fun `visual inside separator`() {
    doTest(
      "vi,",
      line,
      "a , b , c , d , e ,$s x $se, g , h , i , k , l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Inside separator excludes whitespace`() {
    doTest(
      "vI,",
      line,
      "a , b , c , d , e , ${s}x$se , g , h , i , k , l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual a separator includes leading separator`() {
    doTest(
      "va,",
      line,
      "a , b , c , d , e$s , x $se, g , h , i , k , l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  @Test
  fun `visual Around separator includes both separators`() {
    doTest(
      "vA,",
      line,
      "a , b , c , d , e$s , x , ${se}g , h , i , k , l",
      Mode.VISUAL(SelectionType.CHARACTER_WISE),
    )
  }

  // ---- other separator triggers behave identically ----

  @Test
  fun `inside dot separator`() {
    doTest(
      "ci.",
      "a . b . ${c}x . c . d",
      "a . b .$c. c . d",
      Mode.INSERT,
    )
  }

  @Test
  fun `inside semicolon separator`() {
    doTest(
      "ci;",
      "a ; b ; ${c}x ; c ; d",
      "a ; b ;$c; c ; d",
      Mode.INSERT,
    )
  }

  // ---- README example: `da,` leaves a proper comma-separated list ----

  @Test
  fun `delete a separator leaves proper list`() {
    doTest(
      "da,",
      "Shopping list: oranges, apples,${c} bananas, tomatoes",
      "Shopping list: oranges, apples$c, tomatoes",
      Mode.NORMAL(),
    )
  }

  companion object {
    @JvmStatic
    fun separatorChangeCases(): List<Array<String>> {
      val c = VimTestCase.c
      return listOf(
        // last (`l`)
        arrayOf("cIl,", "a , b , c , d , $c , x , g , h , i , k , l"),
        arrayOf("cil,", "a , b , c , d ,$c, x , g , h , i , k , l"),
        arrayOf("cal,", "a , b , c , d $c, x , g , h , i , k , l"),
        arrayOf("cAl,", "a , b , c , d ${c}x , g , h , i , k , l"),
        // current
        arrayOf("cI,", "a , b , c , d , e , $c , g , h , i , k , l"),
        arrayOf("ci,", "a , b , c , d , e ,$c, g , h , i , k , l"),
        arrayOf("ca,", "a , b , c , d , e $c, g , h , i , k , l"),
        arrayOf("cA,", "a , b , c , d , e ${c}g , h , i , k , l"),
        // next (`n`)
        arrayOf("cIn,", "a , b , c , d , e , x , $c , h , i , k , l"),
        arrayOf("cin,", "a , b , c , d , e , x ,$c, h , i , k , l"),
        arrayOf("can,", "a , b , c , d , e , x $c, h , i , k , l"),
        arrayOf("cAn,", "a , b , c , d , e , x ${c}h , i , k , l"),
        // count 1 == no count
        arrayOf("c1i,", "a , b , c , d , e ,$c, g , h , i , k , l"),
      )
    }
  }
}
