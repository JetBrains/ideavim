/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action;

import com.google.common.collect.Lists;
import com.maddyhome.idea.vim.command.CommandState;
import org.jetbrains.plugins.ideavim.SkipNeovimReason;
import org.jetbrains.plugins.ideavim.TestWithoutNeovim;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Tuomas Tynkkynen
 */
public class ChangeNumberActionTest extends VimTestCase {
  public void testIncrementDecimalZero() {
    doTestWithNeovim("<C-A>", "0", "1", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementHexZero() {
    doTestWithNeovim("<C-A>", "0x0", "0x1", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementZero() {
    doTestWithNeovim("<C-X>", "0", "-1", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementDecimal() {
    doTestWithNeovim("<C-A>", "199", "200", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementDecimal() {
    doTestWithNeovim("<C-X>", "1000", "999", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.COMMON, description = "Doesn't work for octal in neovim")
  public void testIncrementOctal() {
    doTestWithNeovim("<C-A>", "0477", "0500", CommandState.Mode.COMMAND,
                   CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.COMMON, description = "Doesn't work for octal in neovim")
  public void testDecrementOctal() {
    doTestWithNeovim("<C-X>", "010", "007", CommandState.Mode.COMMAND,
                   CommandState.SubMode.NONE);
  }

  public void testIncrementHex() {
    doTestWithNeovim("<C-A>", "0xff", "0x100", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementHex() {
    doTestWithNeovim("<C-X>", "0xa100", "0xa0ff", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementNegativeDecimal() {
    doTestWithNeovim("<C-A>", "-199", "-198", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementNegativeDecimal() {
    doTestWithNeovim("<C-X>", "-1000", "-1001", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.COMMON, description = "Doesn't work for octal in neovim")
  public void testIncrementNegativeOctal() {
    doTestWithNeovim("<C-A>", "-0477", "-0500", CommandState.Mode.COMMAND,
                   CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.COMMON, description = "Doesn't work for octal in neovim")
  public void testDecrementNegativeOctal() {
    doTestWithNeovim("<C-X>", "-010", "-007", CommandState.Mode.COMMAND,
                   CommandState.SubMode.NONE);
  }

  public void testIncrementNegativeHex() {
    doTestWithNeovim("<C-A>", "-0xff", "-0x100", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementNegativeHex() {
    doTestWithNeovim("<C-X>", "-0xa100", "-0xa0ff", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementWithCount() {
    doTestWithNeovim("123<C-A>", "456", "579", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testDecrementWithCount() {
    doTestWithNeovim("200<C-X>", "100", "-100", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementAlphaWithoutNumberFormatAlpha() {
    doTestWithNeovim("<C-A>", "foo", "foo", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementAlphaWithNumberFormatAlpha() {
    doTestWithNeovim(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "foo", "goo", CommandState.Mode.COMMAND,
                     CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementZWithNumberFormatAlpha() {
    doTestWithNeovim(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "zzz", "zzz", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementXInHexNumberWithNumberFormatAlphaButNotHex() {
    doTestWithNeovim(Lists.newArrayList(":set nf=alpha<Enter>", "<C-A>"), "0<caret>x1", "0y1", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementXInHexNumberWithNumberFormatHexAlpha() {
    doTestWithNeovim(Lists.newArrayList(":set nf=alpha,hex<Enter>", "<C-A>"), "0<caret>x1", "0x2", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementHexNumberWithoutNumberFormatHex() {
    doTestWithNeovim(Lists.newArrayList(":set nf=octal<Enter>", "<C-A>"), "0x42", "1x42", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementOctalNumberWithoutNumberFormatOctal() {
    doTestWithNeovim(Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "077", "078", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.EX_COMMAND)
  public void testIncrementNegativeOctalNumberWithoutNumberFormatOctal() {
    doTestWithNeovim(Lists.newArrayList(":set nf=hex<Enter>", "<C-A>"), "-077", "-076", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }

  public void testIncrementHexPreservesCaseOfX() {
    doTestWithNeovim("<C-A>", "0X88", "0X89", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementHexTakesCaseFromLastLetter() {
    doTestWithNeovim("<C-A>", "0xaB0", "0xAB1", CommandState.Mode.COMMAND, CommandState.SubMode.NONE);
  }

  public void testIncrementLocatesNumberOnTheSameLine() {
    doTestWithNeovim("<C-A>", "foo ->* bar 123\n", "foo ->* bar 12<caret>4\n", CommandState.Mode.COMMAND,
           CommandState.SubMode.NONE);
  }
}
