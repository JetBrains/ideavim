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

package org.jetbrains.plugins.ideavim.ex.handler;

import com.google.common.collect.Lists;
import org.jetbrains.plugins.ideavim.VimTestCase;

import javax.swing.*;
import java.util.List;

import static com.maddyhome.idea.vim.helper.StringHelper.stringToKeys;

/**
 * @author Alex Selesse
 */
public class SortCommandTest extends VimTestCase {
  public void testBasicSort() {
    myFixture.configureByText("a.txt", "Test\n" + "Hello World!\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$j"));
    typeText(keys);
    typeText(commandToKeys("sort"));
    myFixture.checkResult("Hello World!\n" + "Test\n");
  }

  public void testMultipleSortLine() {
    myFixture.configureByText("a.txt", "zee\nyee\na\nb\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    typeText(commandToKeys("sort"));
    myFixture.checkResult("a\nb\nyee\nzee\n");
  }

  public void testInverseSort() {
    myFixture.configureByText("a.txt", "kay\nzee\nyee\na\nb\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$4j"));
    typeText(keys);
    typeText(commandToKeys("sort !"));
    myFixture.checkResult("zee\nyee\nkay\nb\na\n");
  }

  public void testCaseSensitiveSort() {
    myFixture.configureByText("a.txt", "apple\nAppetite\nApp\napparition\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    typeText(commandToKeys("sort"));
    myFixture.checkResult("App\nAppetite\napparition\napple\n");
  }

  public void testCaseInsensitiveSort() {
    myFixture.configureByText("a.txt", "apple\nAppetite\nApp\napparition\n");
    final List<KeyStroke> keys = Lists.newArrayList(KeyStroke.getKeyStroke("control V"));
    keys.addAll(stringToKeys("$3j"));
    typeText(keys);
    typeText(commandToKeys("sort i"));
    myFixture.checkResult("App\napparition\nAppetite\napple\n");
  }

  public void testRangeSort() {
    myFixture.configureByText("a.txt", "zee\nc\na\nb\nwhatever\n");
    typeText(commandToKeys("2,4sort"));
    myFixture.checkResult("zee\na\nb\nc\nwhatever\n");
  }

  public void testNumberSort() {
    myFixture.configureByText("a.txt", "120\n70\n30\n2000");
    typeText(commandToKeys("sort n"));
    myFixture.checkResult("30\n70\n120\n2000");
  }

  public void testNaturalOrderSort() {
    myFixture.configureByText("a.txt", "hello1000\nhello102\nhello70000\nhello1001");
    typeText(commandToKeys("sort n"));
    myFixture.checkResult("hello102\nhello1000\nhello1001\nhello70000");
  }

  public void testNaturalOrderReverseSort() {
    myFixture.configureByText("a.txt", "hello1000\nhello102\nhello70000\nhello1001");
    typeText(commandToKeys("sort n!"));
    myFixture.checkResult("hello70000\nhello1001\nhello1000\nhello102");
  }

  public void testNaturalOrderInsensitiveReverseSort() {
    myFixture.configureByText("a.txt", "Hello1000\nhello102\nhEllo70000\nhello1001");
    typeText(commandToKeys("sort ni!"));
    myFixture.checkResult("hEllo70000\nhello1001\nHello1000\nhello102");
  }

  public void testGlobalSort() {
    myFixture.configureByText("a.txt", "zee\nc\na\nb\nwhatever");
    typeText(commandToKeys("sort"));
    myFixture.checkResult("a\nb\nc\nwhatever\nzee");
  }

  public void testSortWithPrecedingWhiteSpace() {
    myFixture.configureByText("a.txt", " zee\n c\n a\n b\n whatever");
    typeText(commandToKeys("sort"));
    myFixture.checkResult(" a\n b\n c\n whatever\n zee");
  }
}
