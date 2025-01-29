/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.miniai

import com.intellij.ide.highlighter.JavaFileType
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

@Suppress("SpellCheckingInspection")
class MiniAIExtensionTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    enableExtensions("mini-ai")
  }

  @Test
  fun testChangeInsideNestedQuotes() {
    doTest(
      "ciq",
      "this 'simple<caret> \"test\"'",
      "this '<caret>'",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteInsideNestedQuotes() {
    doTest(
      "diq",
      "this 'simple<caret> \"test\"'",
      "this '<caret>'",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeInsideNestedQuotesDoubleInner() {
    doTest(
      "ciq",
      "this \"simple<caret> 'test'\"",
      "this \"<caret>\"",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteInsideNestedQuotesDoubleInner() {
    doTest(
      "diq",
      "this \"simple<caret> 'test'\"",
      "this \"<caret>\"",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeInsideNestedQuotesBackQuote() {
    doTest(
      "ciq",
      "this `simple<caret> \"test\"`",
      "this `<caret>`",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteInsideNestedQuotesBackQuote() {
    doTest(
      "diq",
      "this `simple<caret> \"test\"`",
      "this `<caret>`",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeAroundNestedSingleQuotes() {
    doTest(
      "caq",
      "this 'simple<caret> \"test\"'",
      "this <caret>",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundNestedSingleQuotes() {
    doTest(
      "daq",
      "this 'simple<caret> \"test\"' test",
      "this <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeAroundNestedDoubleQuotes() {
    doTest(
      "caq",
      "this \"simple<caret> 'test'\"",
      "this <caret>",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundNestedDoubleQuotes() {
    doTest(
      "daq",
      "this \"simple<caret> 'test'\" test",
      "this <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeAroundNestedBackQuotes() {
    doTest(
      "caq",
      "this `simple<caret> \"test\"`",
      "this <caret>",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundNestedBackQuotes() {
    doTest(
      "daq",
      "this `simple<caret> \"test\"` test",
      "this <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // Additional edge cases with cursor on different positions
  @Test
  fun testChangeAroundNestedQuotesOnInnerQuote() {
    doTest(
      "caq",
      "this 'simple \"<caret>test\"'",
      "this 'simple <caret>'",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundNestedQuotesOnInnerQuote() {
    doTest(
      "daq",
      "this 'simple \"<caret>test\"'",
      "this 'simple '",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // Test with multiple levels of nesting
  @Test
  fun testChangeAroundTripleNestedQuotes() {
    doTest(
      "caq",
      "this 'simple \"nested `<caret>test`\"'",
      "this 'simple \"nested <caret>\"'",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundTripleNestedQuotes() {
    doTest(
      "daq",
      "this 'simple \"nested `<caret>test`\"'",
      "this 'simple \"nested <caret>\"'",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeInsideSingleQuote() {
    doTest(
      "ciq",
      "<caret>This is a 'simple' test",
      "This is a '<caret>' test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideSingleQuoteNotBalanced() {
    doTest(
      "ciq",
      "<caret>This is a 'simple test",
      "<caret>This is a 'simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideDoubleQuote() {
    doTest(
      "ciq",
      "<caret>This is a \"simple\" test",
      "This is a \"<caret>\" test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideDoubleQuoteNotBalanced() {
    doTest(
      "ciq",
      "<caret>This is a \"simple test",
      "<caret>This is a \"simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideBackQuote() {
    doTest(
      "ciq",
      "<caret>This is a `simple` test",
      "This is a `<caret>` test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideBackQuoteNotBalanced() {
    doTest(
      "ciq",
      "<caret>This is a `simple test",
      "<caret>This is a `simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundSingleQuote() {
    doTest(
      "caq",
      "<caret>This is a 'simple' test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundSingleQuoteNotBalanced() {
    doTest(
      "caq",
      "<caret>This is a 'simple test",
      "<caret>This is a 'simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundDoubleQuote() {
    doTest(
      "caq",
      "<caret>This is a \"simple\" test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundDoubleQuoteNotBalanced() {
    doTest(
      "caq",
      "<caret>This is a \"simple test",
      "<caret>This is a \"simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeAroundBackQuote() {
    doTest(
      "caq",
      "<caret>This is a `simple` test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundBackQuoteNotBalanced() {
    doTest(
      "caq",
      "<caret>This is a `simple test",
      "<caret>This is a `simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeInsideRoundBrackets() {
    doTest(
      "cib",
      "<caret>This is a (simple) test",
      "This is a (<caret>) test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideRoundBracketsNotBalanced() {
    doTest(
      "cib",
      "<caret>This is a (simple test",
      "<caret>This is a (simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideSquareBrackets() {
    doTest(
      "cib",
      "<caret>This is a [simple] test",
      "This is a [<caret>] test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideSquareBracketsNotBalanced() {
    doTest(
      "cib",
      "<caret>This is a [simple test",
      "<caret>This is a [simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideCurlyBrackets() {
    doTest(
      "cib",
      "<caret>This is a {simple} test",
      "This is a {<caret>} test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeInsideCurlyBracketsNotBalanced() {
    doTest(
      "cib",
      "<caret>This is a {simple test",
      "<caret>This is a {simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundRoundBrackets() {
    doTest(
      "cab",
      "<caret>This is a (simple) test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundRoundBracketsNotBalanced() {
    doTest(
      "cab",
      "<caret>This is a (simple test",
      "<caret>This is a (simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundSquareBrackets() {
    doTest(
      "cab",
      "<caret>This is a [simple] test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testChangeAroundSquareBracketsNotBalanced() {
    doTest(
      "cab",
      "<caret>This is a [simple test",
      "<caret>This is a [simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundCurlyBrackets() {
    doTest(
      "cab",
      "<caret>This is a {simple} test",
      "This is a <caret> test",
      Mode.INSERT,
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testChangeAroundCurlyBracketsNotBalanced() {
    doTest(
      "cab",
      "<caret>This is a {simple test",
      "<caret>This is a {simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  // DELETE tests
  @Test
  fun testDeleteInsideSingleQuote() {
    doTest(
      "diq",
      "<caret>This is a 'simple' test",
      "This is a '<caret>' test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideSingleQuoteNotBalanced() {
    doTest(
      "diq",
      "<caret>This is a 'simple test",
      "<caret>This is a 'simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideDoubleQuote() {
    doTest(
      "diq",
      "<caret>This is a \"simple\" test",
      "This is a \"<caret>\" test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideDoubleQuoteNotBalanced() {
    doTest(
      "diq",
      "<caret>This is a \"simple test",
      "<caret>This is a \"simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideBackQuote() {
    doTest(
      "diq",
      "<caret>This is a `simple` test",
      "This is a `<caret>` test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideBackQuoteNotBalanced() {
    doTest(
      "diq",
      "<caret>This is a `simple test",
      "<caret>This is a `simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundSingleQuote() {
    doTest(
      "daq",
      "<caret>This is a 'simple' test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }

  @Test
  fun testDeleteAroundSingleQuoteNotBalanced() {
    doTest(
      "daq",
      "<caret>This is a 'simple test",
      "<caret>This is a 'simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundDoubleQuote() {
    doTest(
      "daq",
      "<caret>This is a \"simple\" test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundDoubleQuoteNotBalanced() {
    doTest(
      "daq",
      "<caret>This is a \"simple test",
      "<caret>This is a \"simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundBackQuote() {
    doTest(
      "daq",
      "<caret>This is a `simple` test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundBackQuoteNotBalanced() {
    doTest(
      "daq",
      "<caret>This is a `simple test",
      "<caret>This is a `simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideRoundBrackets() {
    doTest(
      "dib",
      "<caret>This is a (simple) test",
      "This is a (<caret>) test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideRoundBracketsNotBalanced() {
    doTest(
      "dib",
      "<caret>This is a (simple test",
      "<caret>This is a (simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideSquareBrackets() {
    doTest(
      "dib",
      "<caret>This is a [simple] test",
      "This is a [<caret>] test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideSquareBracketsNotBalanced() {
    doTest(
      "dib",
      "<caret>This is a [simple test",
      "<caret>This is a [simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideCurlyBrackets() {
    doTest(
      "dib",
      "<caret>This is a {simple} test",
      "This is a {<caret>} test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteInsideCurlyBracketsNotBalanced() {
    doTest(
      "dib",
      "<caret>This is a {simple test",
      "<caret>This is a {simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundRoundBrackets() {
    doTest(
      "dab",
      "<caret>This is a (simple) test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundRoundBracketsNotBalanced() {
    doTest(
      "dab",
      "<caret>This is a (simple test",
      "<caret>This is a (simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundSquareBrackets() {
    doTest(
      "dab",
      "<caret>This is a [simple] test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundSquareBracketsNotBalanced() {
    doTest(
      "dab",
      "<caret>This is a [simple test",
      "<caret>This is a [simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundCurlyBrackets() {
    doTest(
      "dab",
      "<caret>This is a {simple} test",
      "This is a <caret> test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }


  @Test
  fun testDeleteAroundCurlyBracketsNotBalanced() {
    doTest(
      "dab",
      "<caret>This is a {simple test",
      "<caret>This is a {simple test",
      Mode.NORMAL(),
      JavaFileType.INSTANCE,
    )
    assertSelection(null)
  }
}
