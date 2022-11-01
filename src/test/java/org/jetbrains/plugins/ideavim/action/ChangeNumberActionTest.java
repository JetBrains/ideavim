/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action;

import com.google.common.collect.Lists;
import com.maddyhome.idea.vim.command.VimStateMachine;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author Tuomas Tynkkynen
 */
public class ChangeNumberActionTest extends VimTestCase {
  public void testIncrementDecimalZero() {
    doTest("<C-A>", "0", "1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementHexZero() {
    doTest("<C-A>", "0x0", "0x1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementZero() {
    doTest("<C-X>", "0", "-1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementDecimal() {
    doTest("<C-A>", "199", "200", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementDecimal() {
    doTest("<C-X>", "1000", "999", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementOctal() {
    doTest(Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "0477", "0500", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testDecrementOctal() {
    doTest(Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"), "010", "007", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementHex() {
    doTest("<C-A>", "0xff", "0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementHex() {
    doTest("<C-X>", "0xa100", "0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementNegativeDecimal() {
    doTest("<C-A>", "-199", "-198", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementNegativeDecimal() {
    doTest("<C-X>", "-1000", "-1001", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementNegativeOctal() {
    // Minus isn't processed
    doTest(Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "-0477", "-0500", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testDecrementNegativeOctal() {
    // Minus isn't processed
    doTest(Lists.newArrayList(":set nf=octal<Enter>", "<C-X>"), "-010", "-007", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementNegativeHex() {
    doTest("<C-A>", "-0xff", "-0x100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementNegativeHex() {
    doTest("<C-X>", "-0xa100", "-0xa0ff", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementWithCount() {
    doTest("123<C-A>", "456", "579", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testDecrementWithCount() {
    doTest("200<C-X>", "100", "-100", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementAlphaWithoutNumberFormatAlpha() {
    doTest("<C-A>", "foo", "foo", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementAlphaWithNumberFormatAlpha() {
    doTest(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "foo", "goo", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementZWithNumberFormatAlpha() {
    doTest(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "zzz", "zzz", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTest(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "0<caret>x1", "0y1", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTest(Lists.newArrayList(":set nf=alpha,hex<Enter>", "<C-A>"), "0<caret>x1", "0x2", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementHexNumberWithoutNumberFormatHex() {
    doTest(Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "0x42", "1x42", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTest(Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "077", "078", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTest(Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "-077", "-076", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }

  public void testIncrementHexPreservesCaseOfX() {
    doTest("<C-A>", "0X88", "0X89", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementHexTakesCaseFromLastLetter() {
    doTest("<C-A>", "0xaB0", "0xAB1", VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE);
  }

  public void testIncrementLocatesNumberOnTheSameLine() {
    doTest("<C-A>", "foo ->* bar 123\n", "foo ->* bar 12<caret>4\n", VimStateMachine.Mode.COMMAND,
           VimStateMachine.SubMode.NONE);
  }
}
