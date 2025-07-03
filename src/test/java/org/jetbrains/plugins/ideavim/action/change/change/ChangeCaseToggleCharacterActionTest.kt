/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.change

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeCaseToggleCharacterActionTest : VimTestCase() {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap in the same line`() {
    doTest(
      listOf("~"),
      """
          Oh, hi M${c}ark
      """.trimIndent(),
      """
          Oh, hi MA${c}rk
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap at file end`() {
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
      """.trimIndent(),
      """
          Oh, hi Mar${c}K
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test whichwrap to next line`() {
    doTest(
      listOf("~"),
      """
          Oh, hi Mar${c}k
          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi MarK
          ${c}You are my favourite customer
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @Test
  fun `test from empty line to empty line`() {
    doTest(
      listOf("~"),
      """
          Oh, hi Mark
          $c

          You are my favourite customer
      """.trimIndent(),
      """
          Oh, hi Mark

          $c
          You are my favourite customer
      """.trimIndent(),
    ) {
      this.enterCommand("set whichwrap=~")
    }
  }

  @Test
  fun `test undo after toggle case single character`() {
    configureByText("Hello ${c}World")
    typeText("~")
    assertState("Hello w${c}orld")
    typeText("u")
    assertState("Hello ${c}World")
  }

  @Test
  fun `test undo after toggle case single character with oldundo`() {
    configureByText("Hello ${c}World")
    try {
      enterCommand("set oldundo")
      typeText("~")
      assertState("Hello w${c}orld")
      typeText("u")
      assertState("Hello ${c}World")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo after toggle case multiple characters`() {
    configureByText("${c}hello WORLD")
    typeText("5~")
    assertState("HELLO${c} WORLD")
    typeText("u")
    assertState("${c}hello WORLD")
  }

  @Test
  fun `test undo after toggle case multiple characters with oldundo`() {
    configureByText("${c}hello WORLD")
    try {
      enterCommand("set oldundo")
      typeText("5~")
      assertState("HELLO${c} WORLD")
      typeText("u")
      assertState("${c}hello WORLD")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test multiple undo after sequential toggle case`() {
    configureByText("${c}aBc")
    typeText("~")
    assertState("A${c}Bc")
    typeText("~")
    assertState("Ab${c}c")
    typeText("~")
    assertState("Ab${c}C")
    
    // Undo third toggle
    typeText("u")
    assertState("Ab${c}c")
    
    // Undo second toggle
    typeText("u")
    assertState("A${c}Bc")
    
    // Undo first toggle
    typeText("u")
    assertState("${c}aBc")
  }

  @Test
  fun `test multiple undo after sequential toggle case with oldundo`() {
    configureByText("${c}aBc")
    try {
      enterCommand("set oldundo")
      typeText("~")
      assertState("A${c}Bc")
      typeText("~")
      assertState("Ab${c}c")
      typeText("~")
      assertState("Ab${c}C")

      // Undo third toggle
      typeText("u")
      assertState("Ab${c}c")

      // Undo second toggle
      typeText("u")
      assertState("A${c}Bc")

      // Undo first toggle
      typeText("u")
      assertState("${c}aBc")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo toggle case motion`() {
    configureByText("${c}hello world")
    typeText("g~w")
    assertState("${c}HELLO world")
    typeText("u")
    assertState("${c}hello world")
  }

  @Test
  fun `test undo toggle case motion with oldundo`() {
    configureByText("${c}hello world")
    try {
      enterCommand("set oldundo")
      typeText("g~w")
      assertState("${c}HELLO world")
      typeText("u")
      assertState("${c}hello world")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo uppercase motion`() {
    configureByText("${c}hello world")
    typeText("gUw")
    assertState("${c}HELLO world")
    typeText("u")
    assertState("${c}hello world")
  }

  @Test
  fun `test undo uppercase motion with oldundo`() {
    configureByText("${c}hello world")
    try {
      enterCommand("set oldundo")
      typeText("gUw")
      assertState("${c}HELLO world")
      typeText("u")
      assertState("${c}hello world")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo lowercase motion`() {
    configureByText("${c}HELLO WORLD")
    typeText("guw")
    assertState("${c}hello WORLD")
    typeText("u")
    assertState("${c}HELLO WORLD")
  }

  @Test
  fun `test undo lowercase motion with oldundo`() {
    configureByText("${c}HELLO WORLD")
    try {
      enterCommand("set oldundo")
      typeText("guw")
      assertState("${c}hello WORLD")
      typeText("u")
      assertState("${c}HELLO WORLD")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo toggle case line`() {
    configureByText("${c}Hello World")
    typeText("g~~")
    assertState("${c}hELLO wORLD")
    typeText("u")
    assertState("${c}Hello World")
  }

  @Test
  fun `test undo toggle case line with oldundo`() {
    configureByText("${c}Hello World")
    try {
      enterCommand("set oldundo")
      typeText("g~~")
      assertState("${c}hELLO wORLD")
      typeText("u")
      assertState("${c}Hello World")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo uppercase line`() {
    configureByText("${c}Hello World")
    typeText("gUU")
    assertState("${c}HELLO WORLD")
    typeText("u")
    assertState("${c}Hello World")
  }

  @Test
  fun `test undo uppercase line with oldundo`() {
    configureByText("${c}Hello World")
    try {
      enterCommand("set oldundo")
      typeText("gUU")
      assertState("${c}HELLO WORLD")
      typeText("u")
      assertState("${c}Hello World")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test undo lowercase line`() {
    configureByText("${c}HELLO WORLD")
    typeText("guu")
    assertState("${c}hello world")
    typeText("u")
    assertState("${c}HELLO WORLD")
  }

  @Test
  fun `test undo lowercase line with oldundo`() {
    configureByText("${c}HELLO WORLD")
    try {
      enterCommand("set oldundo")
      typeText("guu")
      assertState("${c}hello world")
      typeText("u")
      assertState("${c}HELLO WORLD")
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @Test
  fun `test toggle case line caret position`() {
    configureByText("  Hello ${c}World")
    typeText("g~~")
    assertState("  ${c}hELLO wORLD")
    typeText("u")
    assertState("  Hello ${c}World")

    typeText("^g~~")
    assertState("  ${c}hELLO wORLD")
    typeText("u")
    assertState("  ${c}Hello World")

    typeText("hg~~")
    assertState(" $c hELLO wORLD")
    typeText("u")
    assertState(" $c Hello World")
  }

  @Test
  fun `test uppercase line caret position`() {
    configureByText("  Hello ${c}World")
    typeText("gUU")
    assertState("  ${c}HELLO WORLD")
    typeText("u")
    assertState("  Hello ${c}World")

    typeText("^gUU")
    assertState("  ${c}HELLO WORLD")
    typeText("u")
    assertState("  ${c}Hello World")

    typeText("hgUU")
    assertState(" $c HELLO WORLD")
    typeText("u")
    assertState(" $c Hello World")
  }

  @Test
  fun `test lowercase line caret position`() {
    configureByText("  Hello ${c}World")
    typeText("guu")
    assertState("  ${c}hello world")
    typeText("u")
    assertState("  Hello ${c}World")

    typeText("^guu")
    assertState("  ${c}hello world")
    typeText("u")
    assertState("  ${c}Hello World")

    typeText("hguu")
    assertState(" $c hello world")
    typeText("u")
    assertState(" $c Hello World")
  }
}
