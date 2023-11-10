/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.command

import com.maddyhome.idea.vim.ex.ExException
import java.util.*

/**
 * @author vlan
 *
 * COMPATIBILITY-LAYER: Do not move this class to a different package
 */
public enum class MappingMode {
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
  CMD_LINE,

  ;

  public companion object {
    @JvmField
    public val N: EnumSet<MappingMode> = EnumSet.of(NORMAL)
    public val X: EnumSet<MappingMode> = EnumSet.of(VISUAL)
    public val O: EnumSet<MappingMode> = EnumSet.of(OP_PENDING)
    public val I: EnumSet<MappingMode> = EnumSet.of(INSERT)
    public val C: EnumSet<MappingMode> = EnumSet.of(CMD_LINE)
    public val S: EnumSet<MappingMode> = EnumSet.of(SELECT)
    public val V: EnumSet<MappingMode> = EnumSet.of(VISUAL, SELECT)
    public val NO: EnumSet<MappingMode> = EnumSet.of(NORMAL, OP_PENDING)

    @JvmField
    public val XO: EnumSet<MappingMode> = EnumSet.of(VISUAL, OP_PENDING)
    public val NX: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL)
    public val IC: EnumSet<MappingMode> = EnumSet.of(INSERT, CMD_LINE)
    public val NV: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, SELECT)

    @JvmField
    public val NXO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING)

    @JvmField
    public val NVO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING, SELECT)
    public val INV: EnumSet<MappingMode> = EnumSet.of(INSERT, NORMAL, VISUAL, SELECT)
    public val ALL: EnumSet<MappingMode> = EnumSet.allOf(MappingMode::class.java)

    // This method is used only for single modes, not groups of them (V is not supported)
    internal fun parseModeChar(char: Char): MappingMode {
      return when (char.uppercaseChar()) {
        'N' -> NORMAL
        'X' -> VISUAL
        'S' -> SELECT
        'O' -> OP_PENDING
        'I' -> INSERT
        'C' -> CMD_LINE
        else -> throw ExException("Unexpected mode for char $char")
      }
    }
  }
}
