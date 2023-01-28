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
import org.jetbrains.plugins.ideavim.VimTestCase

class ChangeNumberActionTest : VimTestCase() {
  fun testIncrementDecimalZero() {
    doTest("<C-A>", "0", "1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementHexZero() {
    doTest("<C-A>", "0x0", "0x1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementZero() {
    doTest("<C-X>", "0", "-1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementDecimal() {
    doTest("<C-A>", "199", "200", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementDecimal() {
    doTest("<C-X>", "1000", "999", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "0477", "0500", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testDecrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"), "010", "007", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementHex() {
    doTest("<C-A>", "0xff", "0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementHex() {
    doTest("<C-X>", "0xa100", "0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementNegativeDecimal() {
    doTest("<C-A>", "-199", "-198", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementNegativeDecimal() {
    doTest("<C-X>", "-1000", "-1001", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "-0477", "-0500", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testDecrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"), "-010", "-007", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementNegativeHex() {
    doTest("<C-A>", "-0xff", "-0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementNegativeHex() {
    doTest("<C-X>", "-0xa100", "-0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementWithCount() {
    doTest("123<C-A>", "456", "579", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testDecrementWithCount() {
    doTest("200<C-X>", "100", "-100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementAlphaWithoutNumberFormatAlpha() {
    doTest("<C-A>", "foo", "foo", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementAlphaWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "foo", "goo", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementZWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "zzz", "zzz", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "0<caret>x1", "0y1", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha,hex<Enter>", "<C-A>"), "0<caret>x1", "0x2", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementHexNumberWithoutNumberFormatHex() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "0x42", "1x42", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "077", "078", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "-077", "-076", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }

  fun testIncrementHexPreservesCaseOfX() {
    doTest("<C-A>", "0X88", "0X89", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementHexTakesCaseFromLastLetter() {
    doTest("<C-A>", "0xaB0", "0xAB1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
  }

  fun testIncrementLocatesNumberOnTheSameLine() {
    doTest(
      "<C-A>", "foo ->* bar 123\n", "foo ->* bar 12<caret>4\n", VimStateMachine.Mode.COMMAND,
      VimStateMachine.SubMode.NONE
    )
  }
}