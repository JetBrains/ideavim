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
  CMD_LINE,

  ;

  companion object {
    val N: EnumSet<MappingMode> = EnumSet.of(NORMAL)
    val X: EnumSet<MappingMode> = EnumSet.of(VISUAL)
    val O: EnumSet<MappingMode> = EnumSet.of(OP_PENDING)
    val I: EnumSet<MappingMode> = EnumSet.of(INSERT)
    val C: EnumSet<MappingMode> = EnumSet.of(CMD_LINE)
    val S: EnumSet<MappingMode> = EnumSet.of(SELECT)
    val V: EnumSet<MappingMode> = EnumSet.of(VISUAL, SELECT)
    val IC: EnumSet<MappingMode> = EnumSet.of(INSERT, CMD_LINE)
    // This requires the JvmField annotation as it is used (in Java) by an external plugin
    @JvmField val NVO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING, SELECT)

    // TODO: Consider removing/depracting XO, NV and NXO. They're not typical Vim modes that have a map command
    // E.g. `xmap` is for Visual, `vmap` is for Visual and Select. `map` is NVO and `map!` is IC
    // There are no Vim map commands for XO, NV or NXO. If there isn't a Vim API for them, we shouldn't offer one
    // either. It could lead to confusion or incorrect mapping
    // Note that builtin commands can be valid only in NXO (Normal, Visual and Op-pending) but that doesn't mean it's
    // a valid mapping mode.

    // Used externally, by Java
    @JvmField val XO: EnumSet<MappingMode> = EnumSet.of(VISUAL, OP_PENDING)
    // Used externally
    val NV: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, SELECT)
    // Used externally
    val NXO: EnumSet<MappingMode> = EnumSet.of(NORMAL, VISUAL, OP_PENDING)

    // This method is used only for single modes, not groups of them (V is not supported)
    fun parseModeChar(char: Char): MappingMode {
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

    fun Set<MappingMode>.toModeString(): String {
      if (this == MappingMode.IC) return "!"
      if (this == MappingMode.NVO) return " "
      if (this == MappingMode.C) return "c"
      if (this == MappingMode.I) return "i"
      //if (modes.equals(MappingMode.L)) return "l";

      // The following modes are concatenated
      val modes = this
      return buildString {
        if (modes.containsAll(MappingMode.N)) append("n")
        if (modes.containsAll(MappingMode.O)) append("o")
        if (modes.containsAll(MappingMode.V)) {
          append("v")
        } else {
          if (modes.containsAll(MappingMode.X)) append("x")
          if (modes.containsAll(MappingMode.S)) append("s")
        }
      }
    }

    fun fromModeString(modes: String): Set<MappingMode> {
      if (modes == "!") return MappingMode.IC
      if (modes == " ") return MappingMode.NVO

      if (modes.length == 2 && modes.contains("i") && modes.contains("c")) {
        return MappingMode.IC
      }
      if (modes.length == 3 && modes.contains("n") && modes.contains("v") && modes.contains("o")) {
        return MappingMode.NVO
      }

      return buildSet<MappingMode> {
        modes.forEach {
          when (it) {
            'c' -> addAll(MappingMode.C)
            'i' -> addAll(MappingMode.I)
            'n' -> addAll(MappingMode.N)
            'o' -> addAll(MappingMode.O)
            'v' -> addAll(MappingMode.V)
            'x' -> addAll(MappingMode.X)
            's' -> addAll(MappingMode.S)
          }
        }
      }
    }
  }
}
