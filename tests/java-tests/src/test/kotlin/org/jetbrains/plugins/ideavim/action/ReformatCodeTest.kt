/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

@Suppress("unused")
class ReformatCodeTest : VimJavaTestCase() {
  @Test
  fun testEmpty() {
    configureByJavaText("<caret>")
    typeText(injector.parser.parseKeys("gqq"))
    assertState("<caret>")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithCount() {
    configureByJavaText("class C {\n\tint a;\n\tint <caret>b;\n\tint c;\n\tint d;\n}\n")
    typeText(injector.parser.parseKeys("2gqq"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithUpMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint b;\n" + "\tint <caret>c;\n" + "\tint d;\n" + "}\n")
    typeText(injector.parser.parseKeys("gqk"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithRightMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n")
    typeText(injector.parser.parseKeys("gql"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithTextObject() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n")
    typeText(injector.parser.parseKeys("gqi{"))
    assertState(
      """class C {
    <caret>int a;
    int b;
    int c;
}
""",
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testWithCountsAndDownMotion() {
    configureByJavaText("class C {\n" + "\tint <caret>a;\n" + "\tint b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n")
    typeText(injector.parser.parseKeys("2gqj"))
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n")
    typeText(injector.parser.parseKeys("v" + "l" + "gq"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testLinewiseVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n")
    typeText(injector.parser.parseKeys("V" + "l" + "gq"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testVisualMultiline() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n")
    typeText(injector.parser.parseKeys("v" + "j" + "gq"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testVisualBlock() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n")
    typeText(injector.parser.parseKeys("<C-V>" + "j" + "gq"))
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n")
  }
}
