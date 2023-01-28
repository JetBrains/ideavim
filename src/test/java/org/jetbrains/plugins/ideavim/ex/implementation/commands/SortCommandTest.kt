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
import javax.swing.KeyStroke

@Suppress("SpellCheckingInspection")
class SortCommandTest : VimTestCase() {
  fun testBasicSort() {
    configureByText(
      """
    Test
    Hello World!
    
    """.trimIndent()
    )
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("\$j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState(
      """
    Hello World!
    Test
    
    """.trimIndent()
    )
  }

  fun testMultipleSortLine() {
    configureByText("zee\nyee\na\nb\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState("a\nb\nyee\nzee\n")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testInverseSort() {
    configureByText("kay\nzee\nyee\na\nb\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$4j"))
    typeText(keys)
    typeText(commandToKeys("sort !"))
    assertState("zee\nyee\nkay\nb\na\n")
  }

  fun testCaseSensitiveSort() {
    configureByText("apple\nAppetite\nApp\napparition\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort"))
    assertState("App\nAppetite\napparition\napple\n")
  }

  fun testCaseInsensitiveSort() {
    configureByText("apple\nAppetite\nApp\napparition\n")
    val keys: MutableList<KeyStroke?> = Lists.newArrayList(KeyStroke.getKeyStroke("control V"))
    keys.addAll(injector.parser.stringToKeys("$3j"))
    typeText(keys)
    typeText(commandToKeys("sort i"))
    assertState("App\napparition\nAppetite\napple\n")
  }

  fun testRangeSort() {
    configureByText("zee\nc\na\nb\nwhatever\n")
    typeText(commandToKeys("2,4sort"))
    assertState("zee\na\nb\nc\nwhatever\n")
  }

  fun testNumberSort() {
    configureByText("120\n70\n30\n2000")
    typeText(commandToKeys("sort n"))
    assertState("30\n70\n120\n2000")
  }

  fun testNaturalOrderSort() {
    configureByText("hello1000\nhello102\nhello70000\nhello1001")
    typeText(commandToKeys("sort n"))
    assertState("hello102\nhello1000\nhello1001\nhello70000")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testNaturalOrderReverseSort() {
    configureByText("hello1000\nhello102\nhello70000\nhello1001")
    typeText(commandToKeys("sort n!"))
    assertState("hello70000\nhello1001\nhello1000\nhello102")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.DIFFERENT)
  fun testNaturalOrderInsensitiveReverseSort() {
    configureByText("Hello1000\nhello102\nhEllo70000\nhello1001")
    typeText(commandToKeys("sort ni!"))
    assertState("hEllo70000\nhello1001\nHello1000\nhello102")
  }

  fun testGlobalSort() {
    configureByText("zee\nc\na\nb\nwhatever")
    typeText(commandToKeys("sort"))
    assertState("a\nb\nc\nwhatever\nzee")
  }

  fun testSortWithPrecedingWhiteSpace() {
    configureByText(" zee\n c\n a\n b\n whatever")
    typeText(commandToKeys("sort"))
    assertState(" a\n b\n c\n whatever\n zee")
  }
}