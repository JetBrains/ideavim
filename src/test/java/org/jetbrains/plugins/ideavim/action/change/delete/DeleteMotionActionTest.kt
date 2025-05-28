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

  @Test
  fun `test undo after delete motion with word`() {
    configureByText("Hello ${c}world and more text")
    typeText("dw")
    assertState("Hello ${c}and more text")
    typeText("u")
    assertState("Hello ${c}world and more text")
  }

  @Test
  fun `test undo after delete motion with word with oldundo`() {
    configureByText("Hello ${c}world and more text")
    try {
      enterCommand("set oldundo")
      typeText("dw")
      assertState("Hello ${c}and more text")
      typeText("u")
      assertState("Hello ${c}world and more text")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete line`() {
    configureByText(
      """
      First line
      ${c}Second line
      Third line
    """.trimIndent()
    )
    typeText("dd")
    assertState(
      """
      First line
      ${c}Third line
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      First line
      ${c}Second line
      Third line
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after delete line with oldundo`() {
    configureByText(
      """
      First line
      ${c}Second line
      Third line
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("dd")
      assertState(
        """
      First line
      ${c}Third line
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      First line
      ${c}Second line
      Third line
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete multiple lines`() {
    configureByText(
      """
      First line
      ${c}Second line
      Third line
      Fourth line
      Fifth line
    """.trimIndent()
    )
    typeText("3dd")
    assertState(
      """
      First line
      ${c}Fifth line
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      First line
      ${c}Second line
      Third line
      Fourth line
      Fifth line
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after delete multiple lines with oldundo`() {
    configureByText(
      """
      First line
      ${c}Second line
      Third line
      Fourth line
      Fifth line
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("3dd")
      assertState(
        """
      First line
      ${c}Fifth line
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      First line
      ${c}Second line
      Third line
      Fourth line
      Fifth line
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete with different motions`() {
    configureByText("The ${c}quick brown fox jumps")
    typeText("d3w")
    assertState("The ${c}jumps")
    typeText("u")
    assertState("The ${c}quick brown fox jumps")

    // Test with $ motion
    typeText("d$")
    assertState("The$c ")
    typeText("u")
    assertState("The ${c}quick brown fox jumps")

    // Test with 0 motion
    typeText("d0")
    assertState("${c}quick brown fox jumps")
    typeText("u")
    assertState("The ${c}quick brown fox jumps")
  }

  @Test
  fun `test undo after delete with different motions with oldundo`() {
    configureByText("The ${c}quick brown fox jumps")
    try {
      enterCommand("set oldundo")
      typeText("d3w")
      assertState("The ${c}jumps")
      typeText("u")
      assertState("The ${c}quick brown fox jumps")

      // Test with $ motion
      typeText("d$")
      assertState("The$c ")
      typeText("u")
      assertState("The ${c}quick brown fox jumps")

      // Test with 0 motion
      typeText("d0")
      assertState("${c}quick brown fox jumps")
      typeText("u")
      assertState("The ${c}quick brown fox jumps")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo delete with motion that includes caret movement`() {
    configureByText("a${c}bc(xxx)def")
    typeText("di(")
    assertState("abc(${c})def")
    typeText("u")
    assertState("a${c}bc(xxx)def")
  }

  @Test
  fun `test undo delete with motion that includes caret movement with oldundo`() {
    configureByText("a${c}bc(xxx)def")
    try {
      enterCommand("set oldundo")
      typeText("di(")
      assertState("abc(${c})def")
      typeText("u")
      assertState("a${c}bc(xxx)def")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after delete to mark`() {
    configureByText(
      """
      Line 1
      Li${c}ne 2
      Line 3
      Line 4
    """.trimIndent()
    )
    typeText("ma")  // Set mark a
    typeText("jj")
    assertState(
      """
      Line 1
      Line 2
      Line 3
      Li${c}ne 4
    """.trimIndent()
    )
    typeText("d'a")  // Delete to mark a
    assertState(
      """
      ${c}Line 1
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      Line 1
      Line 2
      Line 3
      Li${c}ne 4
    """.trimIndent()
    )
  }

  @Test
  fun `test undo after delete to mark with oldundo`() {
    configureByText(
      """
      Line 1
      Li${c}ne 2
      Line 3
      Line 4
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("ma")  // Set mark a
      typeText("jj")
      assertState(
        """
      Line 1
      Line 2
      Line 3
      Li${c}ne 4
    """.trimIndent()
      )
      typeText("d'a")  // Delete to mark a
      assertState(
        """
      ${c}Line 1
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      Line 1
      Line 2
      Line 3
      Li${c}ne 4
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
