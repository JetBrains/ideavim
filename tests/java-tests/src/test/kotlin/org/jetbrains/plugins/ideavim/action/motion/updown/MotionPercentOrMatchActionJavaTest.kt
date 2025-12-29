/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.intellij.idea.TestFor
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

class MotionPercentOrMatchActionJavaTest : VimJavaTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  @Test
  fun `test percent match java comment start`() {
    configureByJavaText("/$c* foo */")
    typeText("%")
    assertState("/* foo *$c/")
  }

  @Test
  fun `test percent doesnt match partial java comment`() {
    configureByJavaText("$c/* ")
    typeText("%")
    assertState("$c/* ")
  }

  @Test
  fun `test percent match java comment end`() {
    configureByJavaText("/* foo $c*/")
    typeText("%")
    assertState("$c/* foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test percent match java doc comment start`() {
    configureByJavaText("/*$c* foo */")
    typeText("%")
    assertState("/** foo *$c/")
  }

  @Test
  fun `test percent match java doc comment end`() {
    configureByJavaText("/** foo *$c/")
    typeText("%")
    assertState("$c/** foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.PLUGIN, description = "Matchit plugin affects neovim")
  @Test
  fun `test percent doesnt match after comment start`() {
    configureByJavaText("/*$c foo */")
    typeText("%")
    assertState("/*$c foo */")
  }

  @TestWithoutNeovim(SkipNeovimReason.UNCLEAR)
  @Test
  fun `test percent doesnt match before comment end`() {
    configureByJavaText("/* foo $c */")
    typeText("%")
    assertState("/* foo $c */")
  }

  @Test
  @TestFor(issues = ["VIM-1399"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent ignores brace inside comment`() {
    configureByJavaText("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      TokenStream result = new EmptyTokenFilter(in); /* $c{
              * some text
              */
      result = new LowerCaseFilter(result);
      return result;
    }
    """.trimIndent())
    typeText("%")
    assertState("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      TokenStream result = new EmptyTokenFilter(in); /* $c{
              * some text
              */
      result = new LowerCaseFilter(result);
      return result;
    }
    """.trimIndent())
  }

  @Test
  @TestFor(issues = ["VIM-1399"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent doesnt match brace inside comment`() {
    configureByJavaText("""
      protected TokenStream normalize(String fieldName, TokenStream in) $c{
      TokenStream result = new EmptyTokenFilter(in); /* {
              * some text
              */
      result = new LowerCaseFilter(result);
      return result;
    }
    """.trimIndent())
    typeText("%")
    assertState("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      TokenStream result = new EmptyTokenFilter(in); /* {
              * some text
              */
      result = new LowerCaseFilter(result);
      return result;
    $c}
    """.trimIndent())
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test matching works with a sequence of single-line comments`() {
    configureByJavaText("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      // $c{
      // result = new LowerCaseFilter(result);
      // }
      return result;
    }
    """.trimIndent())
    typeText("%")
    assertState("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      // {
      // result = new LowerCaseFilter(result);
      // $c}
      return result;
    }
    """.trimIndent())
  }

  @Test
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test matching doesn't work if a sequence of single-line comments is broken`() {
    configureByJavaText("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      // $c{
        result = new LowerCaseFilter(result);
      // }
      return result;
    }
    """.trimIndent())
    typeText("%")
    assertState("""
      protected TokenStream normalize(String fieldName, TokenStream in) {
      // $c{
        result = new LowerCaseFilter(result);
      // }
      return result;
    }
    """.trimIndent())
  }

  @Test
  @TestFor(issues = ["VIM-3530"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test block in comment`() {
    configureByJavaText("""
  /**
   * Compose Multiplatform Gradle Plugin
   * [gradle portal](https://plugins${c}.gradle.org/plugin/org.jetbrains.compose)
   * [github](https://github.com/JetBrains/compose-multiplatform/releases)
   * [maven runtime](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/runtime/)
   * [maven runtime](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/runtime/runtime/)
   * [maven ui-js](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/ui/ui-js/)
   */
      """.trimIndent())
    typeText("di)")
    assertState("""
  /**
   * Compose Multiplatform Gradle Plugin
   * [gradle portal]()
   * [github](https://github.com/JetBrains/compose-multiplatform/releases)
   * [maven runtime](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/runtime/)
   * [maven runtime](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/runtime/runtime/)
   * [maven ui-js](https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/ui/ui-js/)
   */
      """.trimIndent())
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent skips brace inside string literal`() {
    configureByJavaText(
      """
      void test() $c{
        String json = "{\"key\":\"value\"}";
      }
      """.trimIndent()
    )
    typeText("%")
    assertState(
      """
      void test() {
        String json = "{\"key\":\"value\"}";
      $c}
      """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent skips brace inside string with unbalanced quotes`() {
    // This test specifically targets the bug: the string contains a closing brace
    // preceded by an unescaped quote which confuses text-based detection
    configureByJavaText(
      """
      void test() $c{
        String s = "{\"}";
      }
      """.trimIndent()
    )
    typeText("%")
    assertState(
      """
      void test() {
        String s = "{\"}";
      $c}
      """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent skips brace inside text block`() {
    // Java text blocks (triple-quoted strings) should be recognized via PSI
    // The text-based detection would misinterpret the triple quotes
    configureByJavaText(
      """
      void test() $c{
        String json = ${"\"\"\""}
          {"key":"value"}
          ${"\"\"\""};
      }
      """.trimIndent()
    )
    typeText("%")
    assertState(
      """
      void test() {
        String json = ${"\"\"\""}
          {"key":"value"}
          ${"\"\"\""};
      $c}
      """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test percent on brace inside string stays in place`() {
    // When cursor is on a brace inside a string, it should try to match within the string
    // If no match is found within the string, the cursor should not move
    configureByJavaText(
      """
      String s = "$c{";
      """.trimIndent()
    )
    typeText("%")
    assertState(
      """
      String s = "$c{";
      """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test va{ with brace inside string literal`() {
    configureByJavaText(
      """
      void test() $c{
        String json = "{\"key\":\"value\"}";
      }
      """.trimIndent()
    )
    typeText("va{")
    assertState(
      """
      void test() <selection>{
        String json = "{\"key\":\"value\"}";
      }</selection>
      """.trimIndent()
    )
  }

  @Test
  @TestFor(issues = ["VIM-4030"])
  @TestWithoutNeovim(SkipNeovimReason.PSI)
  fun `test vi{ with brace inside string literal`() {
    configureByJavaText(
      """
      void test() $c{
        String json = "{\"key\":\"value\"}";
      }
      """.trimIndent()
    )
    typeText("vi{")
    assertState(
      """
      void test() {<selection>
        String json = "{\"key\":\"value\"}";
      </selection>}
      """.trimIndent()
    )
  }
}
