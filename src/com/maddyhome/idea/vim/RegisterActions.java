/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2016 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.key.Shortcut;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class RegisterActions {
  /**
   * Register all the key/action mappings for the plugin.
   */
  public static void registerActions() {
    registerVimCommandActions();

    registerInsertModeActions();
    registerNormalModeActions();
    registerNVOModesActions();
    registerCommandLineActions();
    registerVariousModesActions();
  }

  private static void registerVimCommandActions() {
    final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
    for (String actionId : manager.getPluginActions(VimPlugin.getPluginId())) {
      final AnAction action = manager.getAction(actionId);
      if (action instanceof VimCommandAction) {
        VimPlugin.getKey().registerCommandAction((VimCommandAction)action, actionId);
      }
    }
  }

  private static void registerVariousModesActions() {
    final KeyGroup parser = VimPlugin.getKey();
    parser.registerAction(MappingMode.NV, "VimVisualToggleLineMode", Command.Type.OTHER_READONLY, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('V'));
    parser.registerAction(MappingMode.NV, "VimVisualToggleBlockMode", Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_BLOCKWISE,
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK))}
    );
    parser.registerAction(MappingMode.NV, "VimMotionMark", Command.Type.OTHER_READONLY, new Shortcut('m'),
                          Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.NV, "VimGotoDeclaration", Command.Type.OTHER_READONLY, Command.FLAG_SAVE_JUMP,
                          new Shortcut[]{new Shortcut("gD"), new Shortcut("gd"),
                            // TODO: <C-]> is a tag command similar to gD, the tag stack is not implemented
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, KeyEvent.CTRL_MASK)),}
    );
    parser.registerAction(MappingMode.NV, "VimFileGetLocationInfo", Command.Type.OTHER_READONLY, new Shortcut(
                            new KeyStroke[]{KeyStroke.getKeyStroke('g'),
                              KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)})
    );
    parser.registerAction(MappingMode.NV, "CollapseAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zM"));
    parser.registerAction(MappingMode.NV, "CollapseRegion", Command.Type.OTHER_READONLY, new Shortcut("zc"));
    parser.registerAction(MappingMode.NV, "CollapseRegionRecursively", Command.Type.OTHER_READONLY, new Shortcut("zC"));
    parser.registerAction(MappingMode.NV, "ExpandAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zR"));
    parser.registerAction(MappingMode.NV, "ExpandRegion", Command.Type.OTHER_READONLY, new Shortcut("zo"));
    parser.registerAction(MappingMode.NV, "ExpandRegionRecursively", Command.Type.OTHER_READONLY, new Shortcut("zO"));
    parser.registerAction(MappingMode.NV, "VimToggleRecording",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_NO_ARG_RECORDING,
                          new Shortcut('q'), Argument.Type.CHARACTER);

    // Text Object Actions for Visual and Operator Pending Modes
    parser.registerAction(MappingMode.VO, "VimMotionGotoFileMark", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('`'),
                          Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.VO, "VimMotionGotoFileMarkLine", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('\''),
                          Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.VO, "VimMotionGotoFileMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.VO, "VimMotionGotoFileMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.VO, "VimMotionTextOuterWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("aw"));
    parser.registerAction(MappingMode.VO, "VimMotionTextOuterBigWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("aW"));
    parser.registerAction(MappingMode.VO, "VimMotionTextInnerWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("iw"));
    parser.registerAction(MappingMode.VO, "VimMotionTextInnerBigWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("iW"));
    parser.registerAction(MappingMode.VO, "VimMotionInnerParagraph", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK, new Shortcut("ip"));
    parser.registerAction(MappingMode.VO, "VimMotionOuterParagraph", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK, new Shortcut("ap"));
    parser.registerAction(MappingMode.VO, "VimMotionInnerSentence",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("is"));
    parser.registerAction(MappingMode.VO, "VimMotionOuterSentence", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut("as"));
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockAngle", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i<"), new Shortcut("i>")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockBrace", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("iB"), new Shortcut("i{"), new Shortcut("i}")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockBracket", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i["), new Shortcut("i]")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockParen", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("ib"), new Shortcut("i("), new Shortcut("i)")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i\""),}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i'"),}
    );
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i`"),}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockAngle",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("a<"),
        new Shortcut("a>")
      });
    parser.registerAction(MappingMode.VO, "VimMotionInnerBlockTag", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("it")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockBrace", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("aB"), new Shortcut("a{"), new Shortcut("a}")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockBracket", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a["), new Shortcut("a]")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockParen", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("ab"), new Shortcut("a("), new Shortcut("a)")}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a\""),}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a'"),}
    );
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("a`"),
    });
    parser.registerAction(MappingMode.VO, "VimMotionOuterBlockTag", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("at")}
    );
    parser.registerAction(MappingMode.NO, "VimResetMode", Command.Type.RESET, new Shortcut(new KeyStroke[]{
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)
    }));
  }

  private static void registerCommandLineActions() {
    final KeyGroup parser = VimPlugin.getKey();
    parser
      .registerAction(MappingMode.C, "VimProcessExEntry", Command.Type.OTHER_READ_WRITE, Command.FLAG_COMPLETE_EX,
                      new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
                        new Shortcut(KeyStroke.getKeyStroke((char)0x0a)),
                        new Shortcut(KeyStroke.getKeyStroke((char)0x0d))});
  }

  /**
   * Register normal, visual, operator pending modes actions.
   */
  private static void registerNVOModesActions() {
    final KeyGroup parser = VimPlugin.getKey();
    parser.registerAction(MappingMode.NVO, "VimCopySelectRegister", Command.Type.SELECT_REGISTER, Command.FLAG_EXPECT_MORE,
                          new Shortcut('"'), Argument.Type.CHARACTER);

    // Motion Actions
    // TODO - add ['
    // TODO - add [`
    // TODO - add ]'
    // TODO - add ]`
    // TODO - add zj
    // TODO - add zk

    parser.registerAction(MappingMode.NVO, "VimMotionNextTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gt"));
    parser.registerAction(MappingMode.NVO, "VimMotionPreviousTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gT"));
    parser.registerAction(MappingMode.NVO, "VimMotionCamelEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]b"));
    parser.registerAction(MappingMode.NVO, "VimMotionCamelEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]w"));
    parser.registerAction(MappingMode.NVO, "VimMotionCamelLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[b"));
    parser.registerAction(MappingMode.NVO, "VimMotionCamelRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[w"));
    parser.registerAction(MappingMode.NVO, "VimMotionColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut('|'));
    parser.registerAction(MappingMode.NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('j'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)),
    });
    parser.registerAction(MappingMode.NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gj"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)})
    });
    parser.registerAction(MappingMode.NVO, "VimMotionDownFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('+'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionDownLess1FirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('_'));
    parser.registerAction(MappingMode.NVO, "VimMotionFirstColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('0'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionFirstScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g0"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)})
    });
    parser.registerAction(MappingMode.NVO, "VimMotionFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('^')
    });
    parser.registerAction(MappingMode.NVO, "VimMotionFirstScreenNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g^")
    });
    parser
      .registerAction(MappingMode.NVO, "VimMotionFirstScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut('H')
                      });
    parser
      .registerAction(MappingMode.NVO, "VimMotionGotoLineFirst", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut("gg"),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK))
                      });
    parser
      .registerAction(MappingMode.NVO, "VimMotionGotoLineLast", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('G'));
    parser
      .registerAction(MappingMode.NVO, "VimMotionGotoLineLastEnd", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.NVO, "VimMotionLastColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut('$'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionLastScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut("g$"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)})
    });
    parser.registerAction(MappingMode.NVO, "VimMotionLastMatchChar", Command.Type.MOTION,
                          new Shortcut(';'));
    parser.registerAction(MappingMode.NVO, "VimMotionLastMatchCharReverse", Command.Type.MOTION,
                          new Shortcut(','));
    parser.registerAction(MappingMode.NVO, "VimMotionLastNonSpace", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("g_"));
    parser
      .registerAction(MappingMode.NVO, "VimMotionLastScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('L'));
    parser.registerAction(MappingMode.NVO, "VimMotionLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('h'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionLeftMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('F'), Argument.Type.DIGRAPH);
    parser.registerAction(MappingMode.NVO, "VimMotionLeftTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('T'), Argument.Type.DIGRAPH);
    parser.registerAction(MappingMode.NVO, "VimMotionLeftWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionMiddleColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gm"));
    parser.registerAction(MappingMode.NVO, "VimMotionMiddleScreenLine", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('M'));
    parser
      .registerAction(MappingMode.NVO, "VimMotionNthCharacter", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("go"));
    // This represents two commands and one is linewise and the other is inclusive - the handler will fix it
    parser.registerAction(MappingMode.NVO, "VimMotionPercentOrMatch", Command.Type.MOTION, Command.FLAG_SAVE_JUMP,
                          new Shortcut('%'));
    parser.registerAction(MappingMode.NVO, "VimMotionRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('l'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionRightMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('f'), Argument.Type.DIGRAPH);
    parser.registerAction(MappingMode.NVO, "VimMotionRightTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('t'), Argument.Type.DIGRAPH);
    parser.registerAction(MappingMode.NVO, "VimMotionRightWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut(' '));
    parser.registerAction(MappingMode.NVO, "VimMotionScrollFirstScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zt")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollFirstScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zs")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollFirstScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)})
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollFirstScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z+")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollHalfPageDown", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.NVO, "VimMotionScrollHalfPageUp", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLastScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zb")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLastScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("ze")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLastScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z-")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLastScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z^")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLineDown", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.NVO, "VimMotionScrollLineUp", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.NVO, "VimMotionScrollMiddleScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zz")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollMiddleScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z.")
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollColumnLeft", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zl"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)})
                          });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollColumnRight", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zh"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)})
                          });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollPageDown", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionScrollPageUp", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('k'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    });
    parser.registerAction(MappingMode.NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gk"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)})
    });
    parser.registerAction(MappingMode.NVO, "VimMotionUpFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('-'));
    parser.registerAction(MappingMode.NVO, "VimMotionWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("ge"));
    parser.registerAction(MappingMode.NVO, "VimMotionWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('e'));
    parser.registerAction(MappingMode.NVO, "VimMotionWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('b'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('w'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionBigWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gE"));
    parser.registerAction(MappingMode.NVO, "VimMotionBigWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('E'));
    parser.registerAction(MappingMode.NVO, "VimMotionBigWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('B'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionBigWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('W'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.NVO, "VimMotionSentenceStartPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('('));
    parser.registerAction(MappingMode.NVO, "VimMotionSentenceStartNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut(')'));
    parser.registerAction(MappingMode.NVO, "VimMotionSentenceEndPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g("));
    parser.registerAction(MappingMode.NVO, "VimMotionSentenceEndNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g)"));
    parser.registerAction(MappingMode.NVO, "VimMotionParagraphPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('{'));
    parser
      .registerAction(MappingMode.NVO, "VimMotionParagraphNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('}'));
    parser.registerAction(MappingMode.NVO, "VimMotionUnmatchedBraceOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[{"));
    parser.registerAction(MappingMode.NVO, "VimMotionUnmatchedBraceClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]}"));
    parser.registerAction(MappingMode.NVO, "VimMotionUnmatchedParenOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[("));
    parser.registerAction(MappingMode.NVO, "VimMotionUnmatchedParenClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("])"));
    parser.registerAction(MappingMode.NVO, "VimMotionSectionBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[]"));
    parser.registerAction(MappingMode.NVO, "VimMotionSectionBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[["));
    parser.registerAction(MappingMode.NVO, "VimMotionSectionForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]]"));
    parser.registerAction(MappingMode.NVO, "VimMotionSectionForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]["));
    parser.registerAction(MappingMode.NVO, "VimMotionMethodBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[M"));
    parser.registerAction(MappingMode.NVO, "VimMotionMethodBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[m"));
    parser.registerAction(MappingMode.NVO, "VimMotionMethodForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]M"));
    parser.registerAction(MappingMode.NVO, "VimMotionMethodForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]m"));

    // Misc Actions
    parser.registerAction(MappingMode.NVO, "VimSearchFwdEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_FWD | Command.FLAG_SAVE_JUMP,
                          new Shortcut('/'), Argument.Type.EX_STRING);
    parser.registerAction(MappingMode.NVO, "VimSearchRevEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_REV | Command.FLAG_SAVE_JUMP,
                          new Shortcut('?'), Argument.Type.EX_STRING);
    parser.registerAction(MappingMode.NVO, "VimSearchAgainNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('n'));
    parser
      .registerAction(MappingMode.NVO, "VimSearchAgainPrevious", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('N'));
    parser.registerAction(MappingMode.NVO, "VimExEntry", Command.Type.OTHER_READ_WRITE,
                          new Shortcut(':'));
    parser.registerAction(MappingMode.NVO, "VimSearchWholeWordForward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('*'));
    parser.registerAction(MappingMode.NVO, "VimSearchWholeWordBackward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('#'));
    parser
      .registerAction(MappingMode.NVO, "VimSearchWordForward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g*"));
    parser
      .registerAction(MappingMode.NVO, "VimSearchWordBackward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g#"));
  }

  private static void registerNormalModeActions() {
    final KeyGroup parser = VimPlugin.getKey();
    // Copy/Paste Actions
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursor", Command.Type.PASTE,
                          new Shortcut('P'));
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursor", Command.Type.PASTE,
                          new Shortcut('p'));
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gP"));
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gp"));
    parser.registerAction(MappingMode.N, "VimCopyPutTextBeforeCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[P"),
      new Shortcut("]P")
    });
    parser.registerAction(MappingMode.N, "VimCopyPutTextAfterCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[p"),
      new Shortcut("]p")
    });
    parser.registerAction(MappingMode.N, "VimCopyYankLine", Command.Type.COPY,
                          new Shortcut('Y'));
    parser.registerAction(MappingMode.N, "VimCopyYankLine", Command.Type.COPY, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("yy"));
    parser.registerAction(MappingMode.N, "VimCopyYankMotion", Command.Type.COPY, Command.FLAG_OP_PEND,
                          new Shortcut('y'), Argument.Type.MOTION);

    // Insert/Replace/Change Actions
    parser.registerAction(MappingMode.N, "VimChangeCaseLowerMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gu"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCaseToggleCharacter", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(MappingMode.N, "VimChangeCaseToggleMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("g~"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCaseUpperMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gU"), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeCharacter", Command.Type.CHANGE, Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser
      .registerAction(MappingMode.N, "VimChangeCharacters", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('s'));
    parser
      .registerAction(MappingMode.N, "VimChangeEndOfLine", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('C'));
    parser.registerAction(MappingMode.N, "VimChangeLine", Command.Type.CHANGE,
                          Command.FLAG_NO_REPEAT | Command.FLAG_ALLOW_MID_COUNT | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut("cc"),
        new Shortcut('S')
      });
    parser.registerAction(MappingMode.N, "VimChangeNumberInc", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimChangeNumberDec", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimChangeMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND | Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('c'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimChangeReplace", Command.Type.CHANGE, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('R'));
    parser.registerAction(MappingMode.N, "VimDeleteCharacter", Command.Type.DELETE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(MappingMode.N, "VimDeleteCharacterLeft", Command.Type.DELETE,
                          new Shortcut('X'));
    parser.registerAction(MappingMode.N, "VimDeleteCharacterRight", Command.Type.DELETE,
                          new Shortcut('x'));
    parser.registerAction(MappingMode.N, "VimDeleteEndOfLine", Command.Type.DELETE,
                          new Shortcut('D'));
    parser.registerAction(MappingMode.N, "VimDeleteJoinLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(MappingMode.N, "VimDeleteJoinLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(MappingMode.N, "VimDeleteLine", Command.Type.DELETE, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("dd"));
    parser.registerAction(MappingMode.N, "VimDeleteMotion", Command.Type.DELETE, Command.FLAG_OP_PEND,
                          new Shortcut('d'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimFilterCountLines", Command.Type.CHANGE,
                          new Shortcut("!!"));
    parser.registerAction(MappingMode.N, "VimFilterMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('!'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimInsertAfterCursor", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('a'));
    parser.registerAction(MappingMode.N, "VimInsertAfterLineEnd", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('A'));
    parser.registerAction(MappingMode.N, "VimInsertAtPreviousInsert", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gi"));
    parser.registerAction(MappingMode.N, "VimInsertBeforeFirstNonBlank", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('I'));
    parser.registerAction(MappingMode.N, "VimInsertLineStart", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gI"));
    parser.registerAction(MappingMode.N, "VimInsertNewLineAbove", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('O'));
    parser.registerAction(MappingMode.N, "VimInsertNewLineBelow", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('o'));
    // Motion Actions
    parser
      .registerAction(MappingMode.N, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('`'), Argument.Type.CHARACTER);
    parser
      .registerAction(MappingMode.N, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('\''), Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.N, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.N, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    // Misc Actions
    parser.registerAction(MappingMode.N, "VimLastSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut('&'));
    parser.registerAction(MappingMode.N, "VimLastGlobalSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut("g&"));
    parser.registerAction(MappingMode.N, "VimRepeatChange", Command.Type.OTHER_WRITABLE,
                          new Shortcut('.'));
    parser.registerAction(MappingMode.N, "VimRepeatExCommand", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@:"));
    parser.registerAction(MappingMode.N, "QuickJavaDoc", Command.Type.OTHER_READONLY,
                          new Shortcut('K'));
    parser.registerAction(MappingMode.N, "VimRedo", Command.Type.OTHER_WRITABLE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.N, "VimUndo", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut('u'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
    });

    // File Actions
    parser.registerAction(MappingMode.N, "VimFileSaveClose", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut("ZQ"),
      new Shortcut("ZZ")
    });
    parser.registerAction(MappingMode.N, "VimFilePrevious", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CIRCUMFLEX, KeyEvent.CTRL_MASK))
    });

    // Shift Actions
    parser.registerAction(MappingMode.N, "VimAutoIndentLines", Command.Type.CHANGE,
                          new Shortcut("=="));
    parser.registerAction(MappingMode.N, "VimAutoIndentMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('='), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimShiftLeftLines", Command.Type.CHANGE,
                          new Shortcut("<<"));
    parser.registerAction(MappingMode.N, "VimShiftLeftMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('<'), Argument.Type.MOTION);
    parser.registerAction(MappingMode.N, "VimShiftRightLines", Command.Type.CHANGE,
                          new Shortcut(">>"));
    parser.registerAction(MappingMode.N, "VimShiftRightMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('>'), Argument.Type.MOTION);

    // Jump Actions

    parser.registerAction(MappingMode.N, "VimMotionJumpNext", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(MappingMode.N, "VimMotionJumpPrevious", Command.Type.OTHER_READONLY,
                          new Shortcut[] {
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)),
                            // TODO: <C-T> is a tag command similar to <C-O>, the tag stack is not implemented
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK))
                          });

    parser.registerAction(MappingMode.N, "VimFileGetAscii", Command.Type.OTHER_READONLY,
                          new Shortcut("ga"));
    parser.registerAction(MappingMode.N, "VimFileGetHex", Command.Type.OTHER_READONLY,
                          new Shortcut("g8"));
    parser.registerAction(MappingMode.N, "VimFileGetFileInfo", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)));

    // Macro Actions
    parser.registerAction(MappingMode.N, "VimPlaybackLastRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@@"));
    parser.registerAction(MappingMode.N, "VimPlaybackRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut('@'), Argument.Type.CHARACTER);
    // TODO - support for :map macros
  }

  private static void registerInsertModeActions() {
    final KeyGroup parser = VimPlugin.getKey();
    // Other insert actions
    parser
      .registerAction(MappingMode.I, "EditorBackSpace", Command.Type.INSERT, Command.FLAG_IS_BACKSPACE,
                      new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))}
      );
    parser.registerAction(MappingMode.I, "EditorDelete", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(MappingMode.I, "EditorDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
    });
    parser.registerAction(MappingMode.I, "EditorTab", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(MappingMode.I, "EditorUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
    });
    parser.registerAction(MappingMode.I, "VimInsertCharacterAboveCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertCharacterBelowCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertDeleteInsertedText", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertDeletePreviousWord", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertEnter", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    });
    parser.registerAction(MappingMode.I, "VimInsertPreviousInsert", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimInsertPreviousInsertExit", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(MappingMode.I, "VimInsertRegister", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)),
                          Argument.Type.CHARACTER);
    parser.registerAction(MappingMode.I, "VimInsertReplaceToggle", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
    parser.registerAction(MappingMode.I, "VimInsertSingleCommand", Command.Type.INSERT,
                          Command.FLAG_CLEAR_STROKES | Command.FLAG_EXPECT_MORE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimMotionFirstColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)));
    parser.registerAction(MappingMode.I, "VimMotionGotoLineFirst", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimMotionGotoLineLastEnd", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimMotionLastColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)));
    parser.registerAction(MappingMode.I, "VimMotionLeft", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0))
    });
    parser.registerAction(MappingMode.I, "VimMotionRight", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0))
    });
    parser.registerAction(MappingMode.I, "VimMotionScrollPageUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.I, "VimMotionScrollPageDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.I, "VimMotionWordLeft", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.I, "VimMotionWordRight", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(MappingMode.I, "VimShiftLeftLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(MappingMode.I, "VimShiftRightLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK)));
  }
}
