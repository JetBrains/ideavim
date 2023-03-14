/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.command.VimStateMachine
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.junit.jupiter.api.Test

class ChangeNumberActionTest : VimTestCase() {
  @Test
  fun testIncrementDecimalZero() {
    doTest("<C-A>", "0", "1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementHexZero() {
    doTest("<C-A>", "0x0", "0x1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementZero() {
    doTest("<C-X>", "0", "-1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementDecimal() {
    doTest("<C-A>", "199", "200", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementDecimal() {
    doTest("<C-X>", "1000", "999", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "0477",
      "0500",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testDecrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"),
      "010",
      "007",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementHex() {
    doTest("<C-A>", "0xff", "0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementHex() {
    doTest("<C-X>", "0xa100", "0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementNegativeDecimal() {
    doTest("<C-A>", "-199", "-198", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementNegativeDecimal() {
    doTest("<C-X>", "-1000", "-1001", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "-0477",
      "-0500",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @OptionTest(VimOption(TestOptionConstants.nrformats, limitedValues = ["octal"]))
  fun testDecrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"),
      "-010",
      "-007",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementNegativeHex() {
    doTest("<C-A>", "-0xff", "-0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementNegativeHex() {
    doTest("<C-X>", "-0xa100", "-0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementWithCount() {
    doTest("123<C-A>", "456", "579", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testDecrementWithCount() {
    doTest("200<C-X>", "100", "-100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementAlphaWithoutNumberFormatAlpha() {
    doTest("<C-A>", "foo", "foo", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementAlphaWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "foo",
      "goo",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementZWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "zzz",
      "zzz",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "0<caret>x1",
      "0y1",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha,hex<Enter>", "<C-A>"),
      "0<caret>x1",
      "0x2",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementHexNumberWithoutNumberFormatHex() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "0x42",
      "1x42",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"),
      "077",
      "078",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"),
      "-077",
      "-076",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }

  @Test
  fun testIncrementHexPreservesCaseOfX() {
    doTest("<C-A>", "0X88", "0X89", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementHexTakesCaseFromLastLetter() {
    doTest("<C-A>", "0xaB0", "0xAB1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  @Test
  fun testIncrementLocatesNumberOnTheSameLine() {
    doTest(
      "<C-A>",
      "foo ->* bar 123\n",
      "foo ->* bar 12<caret>4\n",
      VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE,
    )
  }
}
