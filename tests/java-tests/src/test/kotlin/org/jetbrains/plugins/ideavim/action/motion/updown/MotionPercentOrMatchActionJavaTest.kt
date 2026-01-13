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

  @TestWithoutNeovim(SkipNeovimReason.PSI, "Comment matching uses IntelliJ's PSI to understand Java syntax")
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
}