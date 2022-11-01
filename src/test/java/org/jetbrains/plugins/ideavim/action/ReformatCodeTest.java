/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action;

import com.maddyhome.idea.vim.api.VimInjectorKt;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

public class ReformatCodeTest extends VimTestCase {
  public void testEmpty() {
    configureByJavaText("<caret>");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("gqq"));
    assertState("<caret>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithCount() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("2gqq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithUpMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint b;\n" + "\tint <caret>c;\n" + "\tint d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("gqk"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithRightMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("gql"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithTextObject() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("gqi{"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithCountsAndDownMotion() {
    configureByJavaText("class C {\n" + "\tint <caret>a;\n" + "\tint b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("2gqj"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("v" + "l" + "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testLinewiseVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("V" + "l" + "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualMultiline() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("v" + "j" + "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualBlock() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("<C-V>" + "j" + "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }
}
