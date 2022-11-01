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

/**
 * @author Aleksey Lagoshin
 */
public class AutoIndentTest extends VimTestCase {
  // VIM-256 |==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testCaretPositionAfterAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("=="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |2==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithCount() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("2=="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |=k|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithUpMotion() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int b;\n" + "   int <caret>c;\n" + "   int d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("=k"));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |=l|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithRightMotion() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("=l"));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |2=j|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithCountsAndDownMotion() {
    configureByJavaText("class C {\n" + "   int <caret>a;\n" + "   int b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("2=j"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |v| |l| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("v" + "l" + "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualMultilineAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("v" + "j" + "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |C-v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualBlockAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(VimInjectorKt.getInjector().getParser().parseKeys("<C-V>" + "j" + "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }
}
