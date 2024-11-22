/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class DeleteMotionActionTest : VimTestCase() {

  @Test
  fun `test delete last line`() {
    typeTextInFile(
      "dd",
      """
        def xxx():
          expression one
          expression${c} two
      """.trimIndent(),
    )
    assertState(
      """
        def xxx():
          ${c}expression one
      """.trimIndent(),
    )
  }

  @Test
  fun `test on line in middle`() {
    typeTextInFile(
      "dd",
      """
        def xxx():
          expression${c} one
          expression two
      """.trimIndent(),
    )
    assertState(
      """
        def xxx():
          ${c}expression two
      """.trimIndent(),
    )
  }

  @Test
  fun `test delete single line`() {
    typeTextInFile(
      "dd",
      """
        def x${c}xx():
      """.trimIndent(),
    )
    assertState(c)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test delete last line with nostartofline`() {
    configureByText(
      """
        |def xxx():
        |  expression one
        |  expression${c} two
      """.trimMargin(),
    )
    enterCommand("set nostartofline")
    typeText("dd")
    assertState(
      """
        |def xxx():
        |  expression${c} one
      """.trimMargin(),
    )
  }

  @VimBehaviorDiffers(originalVimAfter = "  expression two\n")
  @Test
  fun `test delete last line stored with new line`() {
    typeTextInFile(
      "dd",
      """
        def xxx():
          expression one
          expression${c} two
      """.trimIndent(),
    )
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val savedText = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)?.text ?: ""
    kotlin.test.assertEquals("  expression two\n", savedText)
  }

  @Test
  fun `test delete line action multicaret`() {
    typeTextInFile(
      "d3d",
      """
        abc${c}de
        abcde
        abcde
        abcde
        ab${c}cde
        abcde
        abcde
        
      """.trimIndent(),
    )
    assertState("${c}abcde\n${c}")
  }

  companion object {
    @JvmStatic
    fun repeatFindAndTillTestCase(): Stream<Arguments> {
      return Stream.of(
        arguments("${c}111b222b333", "dtbd;", "${c}b333"),
        arguments("111b2${c}22b333", "dtbd,", "111b${c}b333"),
        arguments("111b222b33${c}3", "dTbd;", "111b${c}3"),
        arguments("111b2${c}22b333", "dTbd,", "111b${c}b333"),
        arguments("${c}111b222b333", "dfbd;", "${c}333"),
        arguments("111b2${c}22b333", "dfbd,", "111${c}333"),
        arguments("111b222b33${c}3", "dFbd;", "111${c}3"),
        arguments("111b2${c}22b333", "dFbd,", "111${c}333"),
      )
    }
  }

  @ParameterizedTest
  @MethodSource("repeatFindAndTillTestCase")
  fun `test delete repeat find and till motion`(content: String, keys: String, expected: String) {
    typeTextInFile(keys, content)
    assertState(expected)
  }

  @Test
  fun `test delete motion action multicaret`() {
    typeTextInFile(
      "dt)",
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
      """.trimMargin(),
    )
    assertState(
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
      """.trimMargin(),
    )
  }

  @Test
  fun `test delete empty line`() {
    val file = """
            Lorem Ipsum
            ${c}
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val newFile = """
            Lorem Ipsum
            ${c}Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    typeTextInFile("dd", file)
    assertState(newFile)
  }

  @Test
  fun `test delete on last line`() {
    doTest(
      "dd",
      """
            Lorem Ipsum
            
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
            ${c}
      """.trimIndent(),
      """
            Lorem Ipsum
            
            Lorem ipsum dolor sit amet,
            ${c}consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  fun `test empty line`() {
    doTest(
      "dd",
      """
            Lorem Ipsum
            
            ${c}
            
            
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
      """.trimIndent(),
      """
            Lorem Ipsum
            
            ${c}
            
            Lorem ipsum dolor sit amet,
            consectetur adipiscing elit
      """.trimIndent(),
    )
  }

  @Test
  fun `test delete line clears status line`() {
    configureByPages(5) // Lorem ipsum
    enterSearch("egestas")
    assertStatusLineMessageContains("Pattern not found: egestas")
    typeText("dd")
    assertStatusLineCleared()
  }
}
