package com.maddyhome.idea.vim.command;

import java.util.EnumSet;

/**
 * @author vlan
 */
public enum Mapping {
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

  public static final EnumSet<Mapping> N = EnumSet.of(NORMAL);
  public static final EnumSet<Mapping> V = EnumSet.of(VISUAL);
  public static final EnumSet<Mapping> O = EnumSet.of(OP_PENDING);
  public static final EnumSet<Mapping> I = EnumSet.of(INSERT);
  public static final EnumSet<Mapping> C = EnumSet.of(CMD_LINE);

  public static final EnumSet<Mapping> NO = EnumSet.of(NORMAL, OP_PENDING);
  public static final EnumSet<Mapping> VO = EnumSet.of(VISUAL, OP_PENDING);
  public static final EnumSet<Mapping> NV = EnumSet.of(NORMAL, VISUAL);

  public static final EnumSet<Mapping> NVO = EnumSet.of(NORMAL, VISUAL, OP_PENDING);
  public static final EnumSet<Mapping> ALL = EnumSet.allOf(Mapping.class);
}
