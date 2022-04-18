/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.extension.matchit

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase

class MatchitGNUMakeTest : VimTestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    enableExtensions("matchit")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from define to endef`() {
    doTest(
      "%",
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endef to define`() {
    doTest(
      "%",
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifdef to endif`() {
    doTest(
      "%",
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifdef`() {
    doTest(
      "%",
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifndef to endif`() {
    doTest(
      "%",
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifndef`() {
    doTest(
      "%",
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifeq to endif`() {
    doTest(
      "%",
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifeq`() {
    doTest(
      "%",
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifneq to endif`() {
    doTest(
      "%",
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifneq`() {
    doTest(
      "%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifneq to else`() {
    doTest(
      "%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifeq to else in ifeq-else block`() {
    doTest(
      "%",
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else ifeq to else`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifeq in else block to else`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ${c}ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else to endif in ifeq-else block`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifeq in ifeq-else block`() {
    doTest(
      "%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifneq to else in ifneq-else block`() {
    doTest(
      "%",
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else ifneq to else`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from ifneq in else block to else`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ${c}ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from else to endif in ifneq-else block`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test jump from endif to ifneq in ifneq-else block`() {
    doTest(
      "%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  // Reverse tests

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from define to endef`() {
    doTest(
      "g%",
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endef to define`() {
    doTest(
      "g%",
      """
        define VAR
          first line
          second line
        ${c}endef
      """.trimIndent(),
      """
        ${c}define VAR
          first line
          second line
        endef
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifdef to endif`() {
    doTest(
      "g%",
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to ifdef`() {
    doTest(
      "g%",
      """
        ifdef var
          $(info defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifdef var
          $(info defined)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifndef to endif`() {
    doTest(
      "g%",
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to ifndef`() {
    doTest(
      "g%",
      """
        ifndef VAR
          $(info not defined)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifndef VAR
          $(info not defined)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifeq to endif`() {
    doTest(
      "g%",
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to ifeq`() {
    doTest(
      "g%",
      """
        ifeq (, $(var))
          $(info empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifeq (, $(var))
          $(info empty)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifneq to endif`() {
    doTest(
      "g%",
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to ifneq`() {
    doTest(
      "g%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}endif
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifneq to else`() {
    doTest(
      "g%",
      """
        ifneq (, $(var))
          $(info not empty)
        ${c}else
      """.trimIndent(),
      """
        ${c}ifneq (, $(var))
          $(info not empty)
        else
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifeq to endif in ifeq-else block`() {
    doTest(
      "g%",
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else ifeq to ifeq`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifeq in else block to ifeq`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ${c}ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else to else in ifeq-else block`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        ${c}else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to else in ifeq-else block`() {
    doTest(
      "g%",
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        else
          $(info not x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ifeq ($(TARGET),x86)
          $(info x86)
        else ifeq ($(TARGET),x86_64)
          $(info x86_64)
        ${c}else
          $(info not x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifneq to endif in ifneq-else block`() {
    doTest(
      "g%",
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else ifneq to ifneq`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from ifneq in else block to ifneq`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ${c}ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ${c}ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from else to else in ifneq-else block`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        ${c}else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  fun `test reverse jump from endif to else in ifneq-else block`() {
    doTest(
      "g%",
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        else
          $(info x86 based)
        ${c}endif
      """.trimIndent(),
      """
        ifneq ($(TARGET),x86)
          $(info not x86)
        else ifneq ($(TARGET),x86_64)
          $(info not x86_64)
        ${c}else
          $(info x86 based)
        endif
      """.trimIndent(),
      CommandState.Mode.COMMAND, CommandState.SubMode.NONE, "Makefile"
    )
  }
}
