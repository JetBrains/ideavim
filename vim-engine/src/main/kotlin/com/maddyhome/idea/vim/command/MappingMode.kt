/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
    val INV: EnumSet<MappingMode> = EnumSet.of(INSERT, NORMAL, VISUAL, SELECT)
    val ALL: EnumSet<MappingMode> = EnumSet.allOf(MappingMode::class.java)
  }
}
