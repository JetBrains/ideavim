/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import javax.swing.KeyStroke

@Suppress("SpellCheckingInspection")
class SortCommandTest : VimTestCase() {
  @Test
  fun testBasicSort() {
    configureByText(
      """
    Test
    Hello World!
    
      """.trimIndent(),
    )
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("\$j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState(
      """
    Hello World!
    Test
    
      """.trimIndent(),
    )
  }

  @Test
  fun testMultipleSortLine() {
    configureByText("zee\nyee\na\nb\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState("a\nb\nyee\nzee\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testInverseSort() {
    configureByText("kay\nzee\nyee\na\nb\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$4j"))
    typeText(keys)
    typeText(commandToKeys("sort !"))
    assertState("zee\nyee\nkay\nb\na\n")
  }

  @Test
  fun testCaseSensitiveSort() {
    configureByText("apple\nAppetite\nApp\napparition\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState("App\nAppetite\napparition\napple\n")
  }

  @Test
  fun testCaseInsensitiveSort() {
    configureByText("apple\nAppetite\nApp\napparition\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort i"))
    assertState("App\napparition\nAppetite\napple\n")
  }

  @Test
  fun testRangeSort() {
    configureByText("zee\nc\na\nb\nwhatever\n")
    typeText(commandToKeys("2,4sort"))
    assertState("zee\na\nb\nc\nwhatever\n")
  }

  @Test
  fun testNumberSort() {
    configureByText("120\n70\n30\n2000")
    typeText(commandToKeys("sort n"))
    assertState("30\n70\n120\n2000")
  }

  @Test
  fun testNaturalOrderSort() {
    configureByText("hello1000\nhello102\nhello70000\nhello1001")
    typeText(commandToKeys("sort n"))
    assertState("hello102\nhello1000\nhello1001\nhello70000")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testNaturalOrderReverseSort() {
    configureByText("hello1000\nhello102\nhello70000\nhello1001")
    typeText(commandToKeys("sort n!"))
    assertState("hello70000\nhello1001\nhello1000\nhello102")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  @Test
  fun testNaturalOrderInsensitiveReverseSort() {
    configureByText("Hello1000\nhello102\nhEllo70000\nhello1001")
    typeText(commandToKeys("sort ni!"))
    assertState("hEllo70000\nhello1001\nHello1000\nhello102")
  }

  @Test
  fun testGlobalSort() {
    configureByText("zee\nc\na\nb\nwhatever")
    typeText(commandToKeys("sort"))
    assertState("a\nb\nc\nwhatever\nzee")
  }

  @Test
  fun testSortWithPrecedingWhiteSpace() {
    configureByText(" zee\n c\n a\n b\n whatever")
    typeText(commandToKeys("sort"))
    assertState(" a\n b\n c\n whatever\n zee")
  }
}
