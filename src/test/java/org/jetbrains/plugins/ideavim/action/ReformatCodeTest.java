/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.jetbrains.plugins.ideavim.action;

import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

public class ReformatCodeTest extends VimTestCase {
  public void testEmpty() {
    configureByJavaText("<caret>");
    typeText(parseKeys("gqq"));
    assertState("<caret>");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithCount() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(parseKeys("2gqq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithUpMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint b;\n" + "\tint <caret>c;\n" + "\tint d;\n" + "}\n");
    typeText(parseKeys("gqk"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithRightMotion() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(parseKeys("gql"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithTextObject() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(parseKeys("gqi{"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testWithCountsAndDownMotion() {
    configureByJavaText("class C {\n" + "\tint <caret>a;\n" + "\tint b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(parseKeys("2gqj"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(parseKeys("v", "l", "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testLinewiseVisual() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "}\n");
    typeText(parseKeys("V", "l", "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "\tint c;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualMultiline() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(parseKeys("v", "j", "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualBlock() {
    configureByJavaText("class C {\n" + "\tint a;\n" + "\tint <caret>b;\n" + "\tint c;\n" + "\tint d;\n" + "}\n");
    typeText(parseKeys("<C-V>", "j", "gq"));
    assertState("class C {\n" + "\tint a;\n" + "    <caret>int b;\n" + "    int c;\n" + "\tint d;\n" + "}\n");
  }
}
