/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.hints

import com.maddyhome.idea.vim.extension.hints.HintGenerator
import com.maddyhome.idea.vim.extension.hints.HintTarget
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import java.awt.Dimension
import java.awt.Point
import javax.swing.JButton
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HintPermutationTest : VimTestCase() {

  private val defaultAlphabet = "ASDFGHJKL".toList()

  @Test
  fun `test alphabet must have at least two characters`() {
    assertFailsWith<IllegalArgumentException> {
      HintGenerator(listOf('A'))
    }
    assertFailsWith<IllegalArgumentException> {
      HintGenerator(emptyList())
    }
  }

  @Test
  fun `test single character hints when targets fit in alphabet`() {
    val alphabet = "ABC".toList()
    val generator = HintGenerator(alphabet)
    val targets = createDummyTargets(3)

    generator.generateHints(targets)

    assertTrue(targets.all { it.hint.length == 1 })
    assertEquals(setOf("A", "B", "C"), targets.map { it.hint }.toSet())
  }

  @Test
  fun `test two character hints when targets exceed single char capacity`() {
    val alphabet = "AB".toList()
    val generator = HintGenerator(alphabet)
    val targets = createDummyTargets(3)

    generator.generateHints(targets)

    assertTrue(targets.all { it.hint.length == 2 })
    assertEquals(3, targets.map { it.hint }.toSet().size)
  }

  @Test
  fun `test all hints are unique across various target counts`() {
    val generator = HintGenerator(defaultAlphabet)
    for (count in listOf(2, 5, 9, 10, 20, 50)) {
      val targets = createDummyTargets(count)
      generator.generateHints(targets)

      val hints = targets.map { it.hint }
      assertEquals(hints.toSet().size, hints.size, "All hints should be unique for $count targets")
    }
  }

  @Test
  fun `test all targets get non-empty hints`() {
    val generator = HintGenerator(defaultAlphabet)
    val targets = createDummyTargets(15)

    generator.generateHints(targets)

    assertTrue(targets.all { it.hint.isNotEmpty() })
  }

  @Test
  fun `test hints only use characters from alphabet`() {
    val alphabet = "XYZ".toList()
    val generator = HintGenerator(alphabet)
    val targets = createDummyTargets(5)

    generator.generateHints(targets)

    targets.forEach { target ->
      assertTrue(
        target.hint.all { it in alphabet },
        "Hint '${target.hint}' should only use chars from $alphabet",
      )
    }
  }

  @Test
  fun `test all hints have same length`() {
    val generator = HintGenerator(defaultAlphabet)
    for (count in listOf(2, 9, 10, 50)) {
      val targets = createDummyTargets(count)
      generator.generateHints(targets)

      val lengths = targets.map { it.hint.length }.toSet()
      assertEquals(1, lengths.size, "All hints should have same length for $count targets, got: $lengths")
    }
  }

  @Test
  fun `test hint length increases with target count`() {
    val alphabet = "AB".toList()
    val generator = HintGenerator(alphabet)

    // 2 targets fit in 1-char hints (alphabet size 2)
    val targets2 = createDummyTargets(2)
    generator.generateHints(targets2)
    assertEquals(1, targets2[0].hint.length, "2 targets with 2-char alphabet should use 1-char hints")

    // 3 targets need 2-char hints (alphabet^1=2 < 3, alphabet^2=4 >= 3)
    val targets3 = createDummyTargets(3)
    generator.generateHints(targets3)
    assertEquals(2, targets3[0].hint.length, "3 targets with 2-char alphabet should use 2-char hints")

    // 5 targets need 3-char hints (alphabet^2=4 < 5, alphabet^3=8 >= 5)
    val targets5 = createDummyTargets(5)
    generator.generateHints(targets5)
    assertEquals(3, targets5[0].hint.length, "5 targets with 2-char alphabet should use 3-char hints")
  }

  @Test
  fun `test nine targets fit in single char with default alphabet`() {
    val generator = HintGenerator(defaultAlphabet)
    val targets = createDummyTargets(9)

    generator.generateHints(targets)

    assertTrue(targets.all { it.hint.length == 1 }, "9 targets should fit in single-char hints with 9-char alphabet")
    assertEquals(defaultAlphabet.map { it.toString() }.toSet(), targets.map { it.hint }.toSet())
  }

  @Test
  fun `test ten targets need two chars with default alphabet`() {
    val generator = HintGenerator(defaultAlphabet)
    val targets = createDummyTargets(10)

    generator.generateHints(targets)

    assertTrue(targets.all { it.hint.length == 2 }, "10 targets should need 2-char hints with 9-char alphabet")
  }

  private fun createDummyTargets(count: Int): List<HintTarget> =
    (0 until count).map { i ->
      HintTarget(
        component = JButton("Button $i"),
        location = Point(0, i * 30),
        size = Dimension(100, 30),
        depth = 0,
      )
    }
}
