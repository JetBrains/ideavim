/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.google.common.collect.Lists
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class ChangeNumberActionTest : VimTestCase() {
  @Test
  fun testIncrementDecimalZero() {
    doTest("<C-A>", "0", "1", Mode.NORMAL())
  }

  @Test
  fun testIncrementHexZero() {
    doTest("<C-A>", "0x0", "0x1", Mode.NORMAL())
  }

  @Test
  fun testDecrementZero() {
    doTest("<C-X>", "0", "-1", Mode.NORMAL())
  }

  @Test
  fun testIncrementDecimal() {
    doTest("<C-A>", "199", "200", Mode.NORMAL())
  }

  @Test
  fun testDecrementDecimal() {
    doTest("<C-X>", "1000", "999", Mode.NORMAL())
  }

  @Test
  fun testIncrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "0477",
      "0500",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDecrementOctal() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"),
      "010",
      "007",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementHex() {
    doTest("<C-A>", "0xff", "0x100", Mode.NORMAL())
  }

  @Test
  fun testDecrementHex() {
    doTest("<C-X>", "0xa100", "0xa0ff", Mode.NORMAL())
  }

  @Test
  fun testIncrementNegativeDecimal() {
    doTest("<C-A>", "-199", "-198", Mode.NORMAL())
  }

  @Test
  fun testDecrementNegativeDecimal() {
    doTest("<C-X>", "-1000", "-1001", Mode.NORMAL())
  }

  @Test
  fun testIncrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "-0477",
      "-0500",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testDecrementNegativeOctal() {
    // Minus isn't processed
    doTest(
      "<C-X>",
      "-010",
      "-007",
      Mode.NORMAL(),
    ) {
      enterCommand("set nf=octal")
    }
  }

  @Test
  fun testIncrementNegativeHex() {
    doTest("<C-A>", "-0xff", "-0x100", Mode.NORMAL())
  }

  @Test
  fun testDecrementNegativeHex() {
    doTest("<C-X>", "-0xa100", "-0xa0ff", Mode.NORMAL())
  }

  @Test
  fun testIncrementWithCount() {
    doTest("123<C-A>", "456", "579", Mode.NORMAL())
  }

  @Test
  fun testDecrementWithCount() {
    doTest("200<C-X>", "100", "-100", Mode.NORMAL())
  }

  @Test
  fun testIncrementAlphaWithoutNumberFormatAlpha() {
    doTest("<C-A>", "foo", "foo", Mode.NORMAL())
  }

  @Test
  fun testIncrementAlphaWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "foo",
      "goo",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementZWithNumberFormatAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "zzz",
      "zzz",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTest(
      Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"),
      "0<caret>x1",
      "0y1",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTest(
      Lists.newArrayList(":set nf=alpha,hex<Enter>", "<C-A>"),
      "0<caret>x1",
      "0x2",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementHexNumberWithoutNumberFormatHex() {
    doTest(
      Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"),
      "0x42",
      "1x42",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"),
      "077",
      "078",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTest(
      Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"),
      "-077",
      "-076",
      Mode.NORMAL(),
    )
  }

  @Test
  fun testIncrementHexPreservesCaseOfX() {
    doTest("<C-A>", "0X88", "0X89", Mode.NORMAL())
  }

  @Test
  fun testIncrementHexTakesCaseFromLastLetter() {
    doTest("<C-A>", "0xaB0", "0xAB1", Mode.NORMAL())
  }

  @Test
  fun testIncrementLocatesNumberOnTheSameLine() {
    doTest(
      "<C-A>",
      "foo ->* bar 123\n",
      "foo ->* bar 12<caret>4\n",
      Mode.NORMAL(),
    )
  }
}
