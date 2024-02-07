/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class SubstituteCommandJavaTest : VimJavaTestCase() {
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test multiple carets`() {
    val before = """public class C {
      |  Stri${c}ng a;
      |$c  String b;
      |  Stri${c}ng c;
      |  String d;
      |}
    """.trimMargin()
    configureByJavaText(before)

    enterCommand("s/String/Integer")

    val after = """public class C {
      |  ${c}Integer a;
      |  ${c}Integer b;
      |  ${c}Integer c;
      |  String d;
      |}
    """.trimMargin()
    assertState(after)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  @Test
  fun `test multiple carets substitute all occurrences`() {
    val before = """public class C {
      |  Stri${c}ng a; String e;
      |$c  String b;
      |  Stri${c}ng c; String f;
      |  String d;
      |}
    """.trimMargin()
    configureByJavaText(before)

    enterCommand("s/String/Integer/g")

    val after = """public class C {
      |  ${c}Integer a; Integer e;
      |  ${c}Integer b;
      |  ${c}Integer c; Integer f;
      |  String d;
      |}
    """.trimMargin()
    assertState(after)
  }
}