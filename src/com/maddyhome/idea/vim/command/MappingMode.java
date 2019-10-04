/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package com.maddyhome.idea.vim.command;

import java.util.EnumSet;

/**
 * @author vlan
 */
public enum MappingMode {
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

  public static final EnumSet<MappingMode> N = EnumSet.of(NORMAL);
  public static final EnumSet<MappingMode> X = EnumSet.of(VISUAL);
  public static final EnumSet<MappingMode> O = EnumSet.of(OP_PENDING);
  public static final EnumSet<MappingMode> I = EnumSet.of(INSERT);
  public static final EnumSet<MappingMode> C = EnumSet.of(CMD_LINE);
  public static final EnumSet<MappingMode> S = EnumSet.of(SELECT);
  public static final EnumSet<MappingMode> V = EnumSet.of(VISUAL, SELECT);

  public static final EnumSet<MappingMode> NO = EnumSet.of(NORMAL, OP_PENDING);
  public static final EnumSet<MappingMode> XO = EnumSet.of(VISUAL, OP_PENDING);
  public static final EnumSet<MappingMode> NX = EnumSet.of(NORMAL, VISUAL);
  public static final EnumSet<MappingMode> IC = EnumSet.of(INSERT, CMD_LINE);
  public static final EnumSet<MappingMode> NV = EnumSet.of(NORMAL, VISUAL, SELECT);

  public static final EnumSet<MappingMode> NXO = EnumSet.of(NORMAL, VISUAL, OP_PENDING);
  public static final EnumSet<MappingMode> NVO = EnumSet.of(NORMAL, VISUAL, OP_PENDING, SELECT);

  public static final EnumSet<MappingMode> ALL = EnumSet.allOf(MappingMode.class);
}
