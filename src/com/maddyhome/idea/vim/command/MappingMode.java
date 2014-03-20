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
  public static final EnumSet<MappingMode> V = EnumSet.of(VISUAL);
  public static final EnumSet<MappingMode> O = EnumSet.of(OP_PENDING);
  public static final EnumSet<MappingMode> I = EnumSet.of(INSERT);
  public static final EnumSet<MappingMode> C = EnumSet.of(CMD_LINE);

  public static final EnumSet<MappingMode> NO = EnumSet.of(NORMAL, OP_PENDING);
  public static final EnumSet<MappingMode> VO = EnumSet.of(VISUAL, OP_PENDING);
  public static final EnumSet<MappingMode> NV = EnumSet.of(NORMAL, VISUAL);

  public static final EnumSet<MappingMode> NVO = EnumSet.of(NORMAL, VISUAL, OP_PENDING);
  public static final EnumSet<MappingMode> ALL = EnumSet.allOf(MappingMode.class);
}
