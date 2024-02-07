/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class InsertNewLineBelowActionJavaTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN) // Java support would be a neovim plugin
  @Test
  fun `test insert new line below matches indent for java`() {
    val before = """public class C {
      |  ${c}Integer a;
      |  Integer b;
      |}
    """.trimMargin()
    val after = """public class C {
      |  Integer a;
      |  $c
      |  Integer b;
      |}
    """.trimMargin()
    configureByJavaText(before)
    typeText("o")
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN) // Java support would be a neovim plugin
  @Test
  fun `test insert new line below matches indent for java 1`() {
    val before = """public class C {
      |$c  Integer a;
      |  Integer b;
      |}
    """.trimMargin()
    val after = """public class C {
      |  Integer a;
      |  $c
      |  Integer b;
      |}
    """.trimMargin()
    configureByJavaText(before)
    typeText("o")
    assertState(after)
  }
}