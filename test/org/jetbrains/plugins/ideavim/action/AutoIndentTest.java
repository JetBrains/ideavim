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

/**
 * @author Aleksey Lagoshin
 */
public class AutoIndentTest extends VimTestCase {
  // VIM-256 |==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testCaretPositionAfterAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(parseKeys("=="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |2==|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithCount() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(parseKeys("2=="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |=k|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithUpMotion() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int b;\n" + "   int <caret>c;\n" + "   int d;\n" + "}\n");
    typeText(parseKeys("=k"));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |=l|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithRightMotion() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(parseKeys("=l"));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |2=j|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testAutoIndentWithCountsAndDownMotion() {
    configureByJavaText("class C {\n" + "   int <caret>a;\n" + "   int b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(parseKeys("2=j"));
    assertState("class C {\n" + "    <caret>int a;\n" + "    int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |v| |l| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "}\n");
    typeText(parseKeys("v", "l", "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "   int c;\n" + "}\n");
  }

  // |v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualMultilineAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(parseKeys("v", "j", "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }

  // |C-v| |j| |=|
  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  public void testVisualBlockAutoIndent() {
    configureByJavaText("class C {\n" + "   int a;\n" + "   int <caret>b;\n" + "   int c;\n" + "   int d;\n" + "}\n");
    typeText(parseKeys("<C-V>", "j", "="));
    assertState("class C {\n" + "   int a;\n" + "    <caret>int b;\n" + "    int c;\n" + "   int d;\n" + "}\n");
  }
}
