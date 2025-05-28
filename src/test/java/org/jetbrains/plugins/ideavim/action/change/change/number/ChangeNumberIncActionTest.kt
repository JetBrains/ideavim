/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change.number

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeNumberIncActionTest : VimTestCase() {
  @VimBehaviorDiffers(originalVimAfter = "11X0")
  @Test
  fun `test inc fancy number`() {
    doTest("<C-A>", "1${c}0X0", "10X1", Mode.NORMAL())
  }

  @Test
  fun `test undo after increment number`() {
    configureByText("The answer is ${c}42")
    typeText("<C-A>")
    assertState("The answer is 4${c}3")
    typeText("u")
    assertState("The answer is ${c}42")
  }

  @Test
  fun `test undo after increment number with oldundo`() {
    configureByText("The answer is ${c}42")
    try {
      enterCommand("set oldundo")
      typeText("<C-A>")
      assertState("The answer is 4${c}3")
      typeText("u")
      assertState("The answer is ${c}42")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after increment with count`() {
    configureByText("Count: ${c}10")
    typeText("5<C-A>")
    assertState("Count: 1${c}5")
    typeText("u")
    assertState("Count: ${c}10")
  }

  @Test
  fun `test undo after increment with count with oldundo`() {
    configureByText("Count: ${c}10")
    try {
      enterCommand("set oldundo")
      typeText("5<C-A>")
      assertState("Count: 1${c}5")
      typeText("u")
      assertState("Count: ${c}10")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after increment negative number`() {
    configureByText("Temperature: ${c}-5 degrees")
    typeText("<C-A>")
    assertState("Temperature: -${c}4 degrees")
    typeText("u")
    assertState("Temperature: ${c}-5 degrees")
  }

  @Test
  fun `test undo after increment negative number with oldundo`() {
    configureByText("Temperature: ${c}-5 degrees")
    try {
      enterCommand("set oldundo")
      typeText("<C-A>")
      assertState("Temperature: -${c}4 degrees")
      typeText("u")
      assertState("Temperature: ${c}-5 degrees")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test multiple undo after sequential increments`() {
    configureByText("Value: ${c}100")
    typeText("<C-A>")
    assertState("Value: 10${c}1")
    typeText("<C-A>")
    assertState("Value: 10${c}2")
    typeText("<C-A>")
    assertState("Value: 10${c}3")

    // Undo third increment
    typeText("u")
    assertState("Value: 10${c}2")

    // Undo second increment
    typeText("u")
    assertState("Value: 10${c}1")

    // Undo first increment
    typeText("u")
    assertState("Value: ${c}100")
  }

  @Test
  fun `test multiple undo after sequential increments with oldundo`() {
    configureByText("Value: ${c}100")
    try {
      enterCommand("set oldundo")
      typeText("<C-A>")
      assertState("Value: 10${c}1")
      typeText("<C-A>")
      assertState("Value: 10${c}2")
      typeText("<C-A>")
      assertState("Value: 10${c}3")

      // Undo third increment
      typeText("u")
      assertState("Value: 10${c}2")

      // Undo second increment
      typeText("u")
      assertState("Value: 10${c}1")

      // Undo first increment
      typeText("u")
      assertState("Value: ${c}100")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo increment with visual selection`() {
    configureByText(
      """
      ${c}10
      20
      30
    """.trimIndent()
    )
    typeText("Vj<C-A>")  // Visual select first two lines and increment
    assertState(
      """
      ${c}11
      21
      30
    """.trimIndent()
    )
    typeText("u")
    assertState(
      """
      ${c}10
      20
      30
    """.trimIndent()
    )
  }

  @Test
  fun `test undo increment with visual selection with oldundo`() {
    configureByText(
      """
      ${c}10
      20
      30
    """.trimIndent()
    )
    try {
      enterCommand("set oldundo")
      typeText("Vj<C-A>")  // Visual select first two lines and increment
      assertState(
        """
      ${c}11
      21
      30
    """.trimIndent()
      )
      typeText("u")
      assertState(
        """
      ${c}10
      20
      30
    """.trimIndent()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo increment octal number`() {
    // OCT is disabled by default
    configureByText("Octal: ${c}0777")
    typeText("<C-A>")
    assertState("Octal: 077${c}8")
    typeText("u")
    assertState("Octal: ${c}0777")
  }

  @Test
  fun `test undo increment octal number with oldundo`() {
    // OCT is disabled by default
    configureByText("Octal: ${c}0777")
    try {
      enterCommand("set oldundo")
      typeText("<C-A>")
      assertState("Octal: 077${c}8")
      typeText("u")
      assertState("Octal: ${c}0777")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo increment hex number`() {
    configureByText("Hex: ${c}0xff")
    typeText("<C-A>")
    assertState("Hex: 0x10${c}0")
    typeText("u")
    assertState("Hex: ${c}0xff")
  }

  @Test
  fun `test undo increment hex number with oldundo`() {
    configureByText("Hex: ${c}0xff")
    try {
      enterCommand("set oldundo")
      typeText("<C-A>")
      assertState("Hex: 0x10${c}0")
      typeText("u")
      assertState("Hex: ${c}0xff")
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
