/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration;
import org.jetbrains.plugins.ideavim.VimTestCase;
import org.jetbrains.plugins.ideavim.VimTestOption;
import org.jetbrains.plugins.ideavim.VimTestOptionType;
import org.junit.Test;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;
import static org.junit.Assert.assertEquals;

public class ReformatCodeTest extends VimTestCase {

  /*
  public void testEmpty() {
    configureByJavaText("<caret>");
    typeText(parseKeys("gqq"));
    myFixture.checkResult("<caret>");
  }
  */

  public void testWithCount() {
    configureByJavaText(
      "class C {\n" +
      "\tint a;\n" +
      "\tint <caret>b;\n" +
      "\tint c;\n" +
      "\tint d;\n" +
      "}\n");
    typeText(parseKeys("2gqq"));
    myFixture.checkResult(
      "class C {\n" +
      "\tint a;\n" +
      "    <caret>int b;\n" +
      "    int c;\n" +
      "\tint d;\n" +
      "}\n");
  }

  public void testWithUpMotion() {
    configureByJavaText(
      "class C {\n" +
      "\tint a;\n" +
      "\tint b;\n" +
      "\tint <caret>c;\n" +
      "\tint d;\n" +
      "}\n");
    typeText(parseKeys("gqk"));
    myFixture.checkResult(
      "class C {\n" +
        "\tint a;\n" +
        "    <caret>int b;\n" +
        "    int c;\n" +
        "\tint d;\n" +
        "}\n");
  }

  public void testWithRightMotion() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "}\n");
    typeText(parseKeys("gql"));
    myFixture.checkResult(
      "class C {\n" +
        "\tint a;\n" +
        "    <caret>int b;\n" +
        "\tint c;\n" +
        "}\n");
  }

  public void testWithTextObject() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "}\n");
    typeText(parseKeys("gqi{"));
    myFixture.checkResult(
      "class C {\n" +
        "    <caret>int a;\n" +
        "    int b;\n" +
        "    int c;\n" +
        "}\n");
  }

  public void testWithCountsAndDownMotion() {
    configureByJavaText(
      "class C {\n" +
        "\tint <caret>a;\n" +
        "\tint b;\n" +
        "\tint c;\n" +
        "\tint d;\n" +
        "}\n");
    typeText(parseKeys("2gqj"));
    myFixture.checkResult(
      "class C {\n" +
      "    <caret>int a;\n" +
      "    int b;\n" +
      "    int c;\n" +
      "\tint d;\n" +
      "}\n");
  }

  public void testVisual() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "}\n");
    typeText(parseKeys("v", "l", "gq"));
    myFixture.checkResult(
      "class C {\n" +
      "\tint a;\n" +
      "    <caret>int b;\n" +
      "\tint c;\n" +
      "}\n");
  }

  public void testLinewiseVisual() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "}\n");
    typeText(parseKeys("V", "l", "gq"));
    myFixture.checkResult(
      "class C {\n" +
        "\tint a;\n" +
        "    <caret>int b;\n" +
        "\tint c;\n" +
        "}\n");
  }

  public void testVisualMultiline() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "\tint d;\n" +
        "}\n");
    typeText(parseKeys("v", "j", "gq"));
    myFixture.checkResult(
      "class C {\n" +
      "\tint a;\n" +
      "    <caret>int b;\n" +
      "    int c;\n" +
      "\tint d;\n" +
      "}\n");
  }

  public void testVisualBlock() {
    configureByJavaText(
      "class C {\n" +
        "\tint a;\n" +
        "\tint <caret>b;\n" +
        "\tint c;\n" +
        "\tint d;\n" +
        "}\n");
    typeText(parseKeys("<C-V>", "j", "gq"));
    myFixture.checkResult(
      "class C {\n" +
        "\tint a;\n" +
        "    <caret>int b;\n" +
        "    int c;\n" +
        "\tint d;\n" +
        "}\n");
  }

  public void testTextParagraph_gpap() {
    configureByText(
      "Please note that daily-run does not attempt to run the backup at exactly the\n" +
        "time. <caret>It can be up to time adding the interval setting. For the default 4min interval this means that the command can run from 01:00:00 up to 01:04:10.\n" +
        "    \n" +
        "     \n" +
        "\n" +
        "Next paragraph");
    typeText(parseKeys("gqap"));
    myFixture.checkResult(
      "<caret>Please note that daily-run does not attempt to run the backup at exactly the\n" +
        "time. It can be up to time adding the interval setting. For the default 4min\n" +
        "interval this means that the command can run from 01:00:00 up to 01:04:10.\n" +
        "    \n" +
        "     \n" +
        "\n" +
        "Next paragraph");
  }

  public void testTextParagraph_gpap_caret_in_heading_whitespace() {
    configureByText(
      "Please\n" +
        "    \n" +
        "     <caret>\n" +
        "\n" +
        "Next paragraph");
    typeText(parseKeys("gqap"));
    myFixture.checkResult(
      "Please\n" +
        "    <caret>\n" +
        "     \n" +
        "\n" +
        "Next paragraph");
  }

  public void testTextParagraph_gpap_custom_textwidth() {

    configureByText(
      "Please note that daily-run does not attempt to run the backup at exactly the\n" +
        "time. <caret>It can be up to time adding the interval setting. For the default 4min interval this means that the command can run from 01:00:00 up to 01:04:10.\n" +
        "    \n" +
        "     \n" +
        "\n" +
        "Next paragraph");
    OptionsManager.INSTANCE.getTextwidth().set(40);

    typeText(parseKeys("gqap"));
    myFixture.checkResult(
      "<caret>Please note that daily-run does not\n" +
        "attempt to run the backup at exactly the\n" +
        "time. It can be up to time adding the\n" +
        "interval setting. For the default 4min\n" +
        "interval this means that the command can\n" +
        "run from 01:00:00 up to 01:04:10.\n" +
        "    \n" +
        "     \n" +
        "\n" +
        "Next paragraph");
  }
}