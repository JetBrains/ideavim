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

@file:Suppress("ClassName")

package org.jetbrains.plugins.ideavim.group.motion

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.ScrollJumpData
import com.maddyhome.idea.vim.option.ScrollOffData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

// These tests are sanity tests for scrolloff and scrolljump, with actions that move the cursor. Other actions that are
// affected by scrolloff or scrolljump should include that in the action specific tests
class MotionGroup_scrolloff_Test : VimOptionTestCase(ScrollOffData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["0"]))
  fun `test move up shows no context with scrolloff=0`() {
    configureByPages(5)
    setPositionAndScroll(25, 25)
    typeText(parseKeys("k"))
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["1"]))
  fun `test move up shows context line with scrolloff=1`() {
    configureByPages(5)
    setPositionAndScroll(25, 26)
    typeText(parseKeys("k"))
    assertPosition(25, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["10"]))
  fun `test move up shows context lines with scrolloff=10`() {
    configureByPages(5)
    setPositionAndScroll(25, 35)
    typeText(parseKeys("k"))
    assertPosition(34, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["0"]))
  fun `test move down shows no context with scrolloff=0`() {
    configureByPages(5)
    setPositionAndScroll(25, 59)
    typeText(parseKeys("j"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["1"]))
  fun `test move down shows context line with scrolloff=1`() {
    configureByPages(5)
    setPositionAndScroll(25, 58)
    typeText(parseKeys("j"))
    assertPosition(59, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["10"]))
  fun `test move down shows context lines with scrolloff=10`() {
    configureByPages(5)
    setPositionAndScroll(25, 49)
    typeText(parseKeys("j"))
    assertPosition(50, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["999"]))
  fun `test scrolloff=999 keeps cursor in centre of screen`() {
    configureByPages(5)
    setPositionAndScroll(25, 42)
    typeText(parseKeys("j"))
    assertPosition(43, 0)
    assertVisibleArea(26, 60)
  }
}

class MotionGroup_scrolljump_Test : VimOptionTestCase(ScrollJumpData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["0"]))
  fun `test move up scrolls single line with scrolljump=0`() {
    configureByPages(5)
    setPositionAndScroll(25, 25)
    typeText(parseKeys("k"))
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["1"]))
  fun `test move up scrolls single line with scrolljump=1`() {
    configureByPages(5)
    setPositionAndScroll(25, 25)
    typeText(parseKeys("k"))
    assertPosition(24, 0)
    assertVisibleArea(24, 58)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["10"]))
  fun `test move up scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    setPositionAndScroll(25, 25)
    typeText(parseKeys("k"))
    assertPosition(24, 0)
    assertVisibleArea(15, 49)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["0"]))
  fun `test move down scrolls single line with scrolljump=0`() {
    configureByPages(5)
    setPositionAndScroll(25, 59)
    typeText(parseKeys("j"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["1"]))
  fun `test move down scrolls single line with scrolljump=1`() {
    configureByPages(5)
    setPositionAndScroll(25, 59)
    typeText(parseKeys("j"))
    assertPosition(60, 0)
    assertVisibleArea(26, 60)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["10"]))
  fun `test move down scrolls multiple lines with scrolljump=10`() {
    configureByPages(5)
    setPositionAndScroll(25, 59)
    typeText(parseKeys("j"))
    assertPosition(60, 0)
    assertVisibleArea(35, 69)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["-50"]))
  fun `test negative scrolljump treated as percentage 1`() {
    configureByPages(5)
    setPositionAndScroll(39, 39)
    typeText(parseKeys("k"))
    assertPosition(38, 0)
    assertVisibleArea(22, 56)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["-10"]))
  fun `test negative scrolljump treated as percentage 2`() {
    configureByPages(5)
    setPositionAndScroll(39, 39)
    typeText(parseKeys("k"))
    assertPosition(38, 0)
    assertVisibleArea(36, 70)
  }
}

class MotionGroup_scrolloff_scrolljump_Test : VimOptionTestCase(ScrollJumpData.name, ScrollOffData.name) {
  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["10"]),
    VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["5"])
  )
  fun `test scroll up with scrolloff and scrolljump set`() {
    configureByPages(5)
    setPositionAndScroll(50, 55)
    typeText(parseKeys("k"))
    assertPosition(54, 0)
    assertVisibleArea(40, 74)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @VimOptionTestConfiguration(
    VimTestOption(ScrollJumpData.name, VimTestOptionType.NUMBER, ["10"]),
    VimTestOption(ScrollOffData.name, VimTestOptionType.NUMBER, ["5"])
  )
  fun `test scroll down with scrolloff and scrolljump set`() {
    configureByPages(5)
    setPositionAndScroll(50, 79)
    typeText(parseKeys("j"))
    assertPosition(80, 0)
    assertVisibleArea(60, 94)
  }
}
