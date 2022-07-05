/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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
package com.maddyhome.idea.vim.command

import java.util.*

/**
 * @author vlan
 *
 * COMPATIBILITY-LAYER: Do not move this class to a different package
 */
enum class MappingMode {
  /**
   * Indicates this key mapping applies to Normal mode
   */
  NORMAL,

  /**
   * Indicates this key mapping applies to Visual mode
   */
  VISUAL,

  /**
   * Indicates this key mapping applies to Select mode
   */
  SELECT,

  /**
   * Indicates this key mapping applies to Operator Pending mode
   */
  OP_PENDING,

  /**
   * Indicates this key mapping applies to Insert mode
   */
  INSERT,

  /**
   * Indicates this key mapping applies to Command Line mode
   */
  CMD_LINE;

  companion object {
    @JvmField
    val N: EnumSet<MappingMode> = EnumSet.of(NORMAL)
    val X: EnumSet<MappingMode> = EnumSet.of(VISUAL)
    val O: EnumSet<MappingMode> = EnumSet.of(OP_PENDING)
    val I: EnumSet<MappingMode> = EnumSet.of(INSERT)
    val C: EnumSet<MappingMode> = EnumSet.of(CMD_LINE)
    val S: EnumSet<MappingMode> = EnumSet.of(SELECT)
    val V: EnumSet<MappingMode> = EnumSet.of(VISUAL, SELECT)
    val NO: EnumSet<MappingMode> = EnumSet.of(NORMAL, OP_PENDING)

    @JvmField
    val XO: EnumSet<MappingMode> = EnumSet.of(VISUAL, OP_PENDING)
    val NX: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL)
    val IC: EnumSet<MappingMode> = EnumSet.of(INSERT, CMD_LINE)
    val NV: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, SELECT)

    @JvmField
    val NXO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING)

    @JvmField
    val NVO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING, SELECT)
    val ALL: EnumSet<MappingMode> = EnumSet.allOf(MappingMode::class.java)
  }
}
