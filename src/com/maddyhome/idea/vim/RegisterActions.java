/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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

import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.Mapping;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.key.Shortcut;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class RegisterActions {

  /**
   * Register all the key/action mappings for the plugin.
   */
  public static void registerActions() {
    final KeyParser parser = KeyParser.getInstance();

    registerInsertModeActions(parser);
    registerVisualModeActions(parser);
    registerNormalModeActions(parser);
    registerNVOModesActions(parser);
    registerCommandLineActions(parser);
    registerVariousModesActions(parser);

    updatePlatformActionHandlers(parser);
  }

  /**
   * Update many of the built-in IDEA actions with our key handlers.
   */
  private static void updatePlatformActionHandlers(@NotNull KeyParser parser) {
    // This group allows us to propagate the keystroke if action acts on something other than an editor
    parser.setupActionHandler("EditorBackSpace", "VimEditorBackSpace", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
    parser.setupActionHandler("EditorDelete", "VimEditorDelete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    parser.setupActionHandler("EditorDown", "VimEditorDown", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
    parser.setupActionHandler("EditorEnter", "VimEditorEnter", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), true);
    parser.setupActionHandler("EditorEscape", "VimEditorEscape", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), true);
    parser.setupActionHandler("EditorLeft", "VimEditorLeft", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
    parser.setupActionHandler("EditorLineStart", "VimEditorLineStart", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
    parser.setupActionHandler("EditorLineEnd", "VimEditorLineEnd", KeyStroke.getKeyStroke(KeyEvent.VK_END, 0));
    parser.setupActionHandler("EditorPageDown", "VimEditorPageDown", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0));
    parser.setupActionHandler("EditorPageUp", "VimEditorPageUp", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0));
    parser.setupActionHandler("EditorRight", "VimEditorRight", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
    parser.setupActionHandler("EditorTab", "VimEditorTab", KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), true);
    parser.setupActionHandler("EditorToggleInsertState", "VimEditorToggleInsertState", KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0));
    parser.setupActionHandler("EditorUp", "VimEditorUp", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));

    // All the Alt keys

    // All the Ctrl keys
    parser.setupActionHandler("EditorDeleteToWordEnd", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.CTRL_MASK));
    parser
      .setupActionHandler("EditorDeleteToWordStart", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorScrollDown", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorPreviousWord", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorNextWord", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorScrollUp", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorTextStart", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorTextEnd", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK));
    parser.setupActionHandler("EditorMoveToPageTop", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK));
    parser
      .setupActionHandler("EditorMoveToPageBottom", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK));

    // All the Shift keys
    parser.setupActionHandler("EditorDownWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorLeftWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorRightWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorUpWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorLineStartWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorLineEndWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.SHIFT_MASK));
    parser
      .setupActionHandler("EditorPageUpWithSelection", "VimDummyHandler", KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorPageDownWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_MASK));

    // All the Ctrl-Shift keys
    parser.setupActionHandler("EditorPreviousWordWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorNextWordWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorTextStartWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorTextEndWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorMoveToPageTopWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
    parser.setupActionHandler("EditorMoveToPageBottomWithSelection", "VimDummyHandler",
                              KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK));
  }

  private static void registerVariousModesActions(@NotNull KeyParser parser) {

    parser.registerAction(Mapping.NV, "VimVisualToggleCharacterMode", Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_CHARACTERWISE, new Shortcut('v'));
    parser.registerAction(Mapping.NV, "VimVisualToggleLineMode", Command.Type.OTHER_READONLY, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('V'));
    parser.registerAction(Mapping.NV, "VimVisualToggleBlockMode", Command.Type.OTHER_READONLY,
                          Command.FLAG_MOT_BLOCKWISE,
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK))}
    );
    parser.registerAction(Mapping.NV, "VimMotionMark", Command.Type.OTHER_READONLY, new Shortcut('m'),
                          Argument.Type.CHARACTER);
    parser.registerAction(Mapping.NV, "VimGotoDeclaration", Command.Type.OTHER_READONLY, Command.FLAG_SAVE_JUMP,
                          new Shortcut[]{new Shortcut("gD"), new Shortcut("gd"),
                            // TODO: <C-]> is a tag command similar to gD, the tag stack is not implemented
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, KeyEvent.CTRL_MASK)),}
    );
    parser.registerAction(Mapping.NV, "VimFileGetLocationInfo", Command.Type.OTHER_READONLY, new Shortcut(
                            new KeyStroke[]{KeyStroke.getKeyStroke('g'),
                              KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)})
    );
    // TODO - add zC
    // TODO - add zO
    parser.registerAction(Mapping.NV, "CollapseAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zM"));
    parser.registerAction(Mapping.NV, "CollapseRegion", Command.Type.OTHER_READONLY, new Shortcut("zc"));
    parser.registerAction(Mapping.NV, "ExpandAllRegions", Command.Type.OTHER_READONLY, new Shortcut("zR"));
    parser.registerAction(Mapping.NV, "ExpandRegion", Command.Type.OTHER_READONLY, new Shortcut("zo"));
    parser.registerAction(Mapping.NV, "VimToggleRecording",
                          Command.Type.OTHER_READONLY,
                          Command.FLAG_NO_ARG_RECORDING,
                          new Shortcut('q'), Argument.Type.CHARACTER);

    // Text Object Actions for Visual and Operator Pending Modes
    parser.registerAction(Mapping.VO, "VimMotionGotoFileMark", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP, new Shortcut('`'),
                          Argument.Type.CHARACTER);
    parser.registerAction(Mapping.VO, "VimMotionGotoFileMarkLine", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP, new Shortcut('\''),
                          Argument.Type.CHARACTER);
    parser.registerAction(Mapping.VO, "VimMotionGotoFileMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(Mapping.VO, "VimMotionGotoFileMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    parser.registerAction(Mapping.VO, "VimMotionTextOuterWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("aw"));
    parser.registerAction(Mapping.VO, "VimMotionTextOuterBigWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("aW"));
    parser.registerAction(Mapping.VO, "VimMotionTextInnerWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("iw"));
    parser.registerAction(Mapping.VO, "VimMotionTextInnerBigWord", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE, new Shortcut("iW"));
    parser.registerAction(Mapping.VO, "VimMotionInnerParagraph", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK, new Shortcut("ip"));
    parser.registerAction(Mapping.VO, "VimMotionOuterParagraph", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_TEXT_BLOCK, new Shortcut("ap"));
    parser.registerAction(Mapping.VO, "VimMotionInnerSentence",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut("is"));
    parser.registerAction(Mapping.VO, "VimMotionOuterSentence", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut("as"));
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockAngle", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i<"), new Shortcut("i>")}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockBrace", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("iB"), new Shortcut("i{"), new Shortcut("i}")}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockBracket", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i["), new Shortcut("i]")}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockParen", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("ib"), new Shortcut("i("), new Shortcut("i)")}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i\""),}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i'"),}
    );
    parser.registerAction(Mapping.VO, "VimMotionInnerBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("i`"),}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockAngle",
                          Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
        new Shortcut("a<"),
        new Shortcut("a>")
      });
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockBrace", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("aB"), new Shortcut("a{"), new Shortcut("a}")}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockBracket", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a["), new Shortcut("a]")}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockParen", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("ab"), new Shortcut("a("), new Shortcut("a)")}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockDoubleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a\""),}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockSingleQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK,
                          new Shortcut[]{new Shortcut("a'"),}
    );
    parser.registerAction(Mapping.VO, "VimMotionOuterBlockBackQuote", Command.Type.MOTION,
                          Command.FLAG_MOT_CHARACTERWISE | Command.FLAG_MOT_INCLUSIVE | Command.FLAG_TEXT_BLOCK, new Shortcut[]{
      new Shortcut("a`"),
    });
    parser.registerAction(Mapping.NO, "VimResetMode", Command.Type.RESET, new Shortcut(new KeyStroke[]{
      KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
      KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)
    }));

    // "Reserve" these keys so they don't work in IDEA. Eventually these may be valid plugin commands.
    parser.registerAction(Mapping.ALL, "VimNotImplementedHandler", Command.Type.OTHER_READONLY,
                          new Shortcut[]{new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)),
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK))});
  }

  private static void registerCommandLineActions(@NotNull KeyParser parser) {
    parser
      .registerAction(Mapping.C, "VimProcessExEntry", Command.Type.OTHER_READ_WRITE, Command.FLAG_COMPLETE_EX, new Shortcut[]{
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
        new Shortcut(KeyStroke.getKeyStroke((char)0x0a)),
        new Shortcut(KeyStroke.getKeyStroke((char)0x0d))
      });
  }

  /**
   * Register normal, visual, operator pending modes actions.
   */
  private static void registerNVOModesActions(@NotNull KeyParser parser) {
    parser.registerAction(Mapping.NVO, "VimCopySelectRegister", Command.Type.SELECT_REGISTER, Command.FLAG_EXPECT_MORE,
                          new Shortcut('"'), Argument.Type.CHARACTER);

    // Motion Actions
    // TODO - add ['
    // TODO - add [`
    // TODO - add ]'
    // TODO - add ]`
    // TODO - add zj
    // TODO - add zk

    parser.registerAction(Mapping.NVO, "VimMotionNextTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gt"));
    parser.registerAction(Mapping.NVO, "VimMotionPreviousTab", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gT"));
    parser.registerAction(Mapping.NVO, "VimMotionCamelEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]b"));
    parser.registerAction(Mapping.NVO, "VimMotionCamelEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("]w"));
    parser.registerAction(Mapping.NVO, "VimMotionCamelLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[b"));
    parser.registerAction(Mapping.NVO, "VimMotionCamelRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("[w"));
    parser.registerAction(Mapping.NVO, "VimMotionColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut('|'));
    parser.registerAction(Mapping.NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('j'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)),
    });
    parser.registerAction(Mapping.NVO, "VimMotionDown", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gj"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)})
    });
    parser.registerAction(Mapping.NVO, "VimMotionDownFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('+'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionDownLess1FirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('_'));
    parser.registerAction(Mapping.NVO, "VimMotionFirstColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('0'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0))
    });
    parser.registerAction(Mapping.NVO, "VimMotionFirstScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g0"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)})
    });
    parser.registerAction(Mapping.NVO, "VimMotionFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('^')
    });
    parser.registerAction(Mapping.NVO, "VimMotionFirstScreenNonSpace", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("g^")
    });
    parser
      .registerAction(Mapping.NVO, "VimMotionFirstScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut('H'),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_MASK))
                      });
    parser
      .registerAction(Mapping.NVO, "VimMotionGotoLineFirst", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut[]{
                        new Shortcut("gg"),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK))
                      });
    parser
      .registerAction(Mapping.NVO, "VimMotionGotoLineLast", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('G'));
    parser
      .registerAction(Mapping.NVO, "VimMotionGotoLineLastEnd", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionLastColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut('$'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0))
    });
    parser.registerAction(Mapping.NVO, "VimMotionLastScreenColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE, new Shortcut[]{
      new Shortcut("g$"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)})
    });
    parser.registerAction(Mapping.NVO, "VimMotionLastMatchChar", Command.Type.MOTION,
                          new Shortcut(';'));
    parser.registerAction(Mapping.NVO, "VimMotionLastMatchCharReverse", Command.Type.MOTION,
                          new Shortcut(','));
    parser.registerAction(Mapping.NVO, "VimMotionLastNonSpace", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("g_"));
    parser
      .registerAction(Mapping.NVO, "VimMotionLastScreenLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('L'));
    parser.registerAction(Mapping.NVO, "VimMotionLastScreenLineEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('h'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))
    });
    parser.registerAction(Mapping.NVO, "VimMotionLeftMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('F'), Argument.Type.DIGRAPH);
    parser.registerAction(Mapping.NVO, "VimMotionLeftTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('T'), Argument.Type.DIGRAPH);
    parser.registerAction(Mapping.NVO, "VimMotionLeftWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionMiddleColumn", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gm"));
    parser.registerAction(Mapping.NVO, "VimMotionMiddleScreenLine", Command.Type.MOTION,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('M'));
    parser
      .registerAction(Mapping.NVO, "VimMotionNthCharacter", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("go"));
    // This represents two commands and one is linewise and the other is inclusive - the handler will fix it
    parser.registerAction(Mapping.NVO, "VimMotionPercentOrMatch", Command.Type.MOTION, Command.FLAG_SAVE_JUMP,
                          new Shortcut('%'));
    parser.registerAction(Mapping.NVO, "VimMotionRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('l'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0))
    });
    parser.registerAction(Mapping.NVO, "VimMotionRightMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('f'), Argument.Type.DIGRAPH);
    parser.registerAction(Mapping.NVO, "VimMotionRightTillMatchChar", Command.Type.MOTION,
                          Command.FLAG_MOT_INCLUSIVE | Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('t'), Argument.Type.DIGRAPH);
    parser.registerAction(Mapping.NVO, "VimMotionRightWrap", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut(' '));
    parser.registerAction(Mapping.NVO, "VimMotionScrollFirstScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zt")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollFirstScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zs")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollFirstScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)})
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollFirstScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z+")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollHalfPageDown", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionScrollHalfPageUp", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SCROLL_JUMP,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionScrollLastScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zb")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollLastScreenColumn", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("ze")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollLastScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z-")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollLastScreenLinePageStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z^")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollLineDown", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionScrollLineUp", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.NVO, "VimMotionScrollMiddleScreenLine", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("zz")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollMiddleScreenLineStart", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut("z.")
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollColumnLeft", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zl"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)})
                          });
    parser.registerAction(Mapping.NVO, "VimMotionScrollColumnRight", Command.Type.OTHER_READONLY, Command.FLAG_IGNORE_SIDE_SCROLL_JUMP,
                          new Shortcut[]{
                            new Shortcut("zh"),
                            new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('z'), KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)})
                          });
    parser.registerAction(Mapping.NVO, "VimMotionScrollPageDown", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0))
    });
    parser.registerAction(Mapping.NVO, "VimMotionScrollPageUp", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('k'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
    });
    parser.registerAction(Mapping.NVO, "VimMotionUp", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut("gk"),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke('g'), KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)})
    });
    parser.registerAction(Mapping.NVO, "VimMotionUpFirstNonSpace", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('-'));
    parser.registerAction(Mapping.NVO, "VimMotionWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("ge"));
    parser.registerAction(Mapping.NVO, "VimMotionWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('e'));
    parser.registerAction(Mapping.NVO, "VimMotionWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('b'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('w'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionBigWordEndLeft", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut("gE"));
    parser.registerAction(Mapping.NVO, "VimMotionBigWordEndRight", Command.Type.MOTION, Command.FLAG_MOT_INCLUSIVE,
                          new Shortcut('E'));
    parser.registerAction(Mapping.NVO, "VimMotionBigWordLeft", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('B'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionBigWordRight", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE, new Shortcut[]{
      new Shortcut('W'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(Mapping.NVO, "VimMotionSentenceStartPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('('));
    parser.registerAction(Mapping.NVO, "VimMotionSentenceStartNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut(')'));
    parser.registerAction(Mapping.NVO, "VimMotionSentenceEndPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g("));
    parser.registerAction(Mapping.NVO, "VimMotionSentenceEndNext", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("g)"));
    parser.registerAction(Mapping.NVO, "VimMotionParagraphPrevious", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('{'));
    parser
      .registerAction(Mapping.NVO, "VimMotionParagraphNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('}'));
    parser.registerAction(Mapping.NVO, "VimMotionUnmatchedBraceOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[{"));
    parser.registerAction(Mapping.NVO, "VimMotionUnmatchedBraceClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]}"));
    parser.registerAction(Mapping.NVO, "VimMotionUnmatchedParenOpen", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[("));
    parser.registerAction(Mapping.NVO, "VimMotionUnmatchedParenClose", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("])"));
    parser.registerAction(Mapping.NVO, "VimMotionSectionBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[]"));
    parser.registerAction(Mapping.NVO, "VimMotionSectionBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[["));
    parser.registerAction(Mapping.NVO, "VimMotionSectionForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]]"));
    parser.registerAction(Mapping.NVO, "VimMotionSectionForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]["));
    parser.registerAction(Mapping.NVO, "VimMotionMethodBackwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[M"));
    parser.registerAction(Mapping.NVO, "VimMotionMethodBackwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("[m"));
    parser.registerAction(Mapping.NVO, "VimMotionMethodForwardEnd", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]M"));
    parser.registerAction(Mapping.NVO, "VimMotionMethodForwardStart", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut("]m"));

    // Misc Actions
    parser.registerAction(Mapping.NVO, "VimSearchFwdEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_FWD | Command.FLAG_SAVE_JUMP,
                          new Shortcut('/'), Argument.Type.EX_STRING);
    parser.registerAction(Mapping.NVO, "VimSearchRevEntry", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SEARCH_REV | Command.FLAG_SAVE_JUMP,
                          new Shortcut('?'), Argument.Type.EX_STRING);
    parser.registerAction(Mapping.NVO, "VimSearchAgainNext", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('n'));
    parser
      .registerAction(Mapping.NVO, "VimSearchAgainPrevious", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('N'));
    parser.registerAction(Mapping.NVO, "VimExEntry", Command.Type.OTHER_READ_WRITE,
                          new Shortcut(':'));
    parser.registerAction(Mapping.NVO, "VimSearchWholeWordForward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('*'));
    parser.registerAction(Mapping.NVO, "VimSearchWholeWordBackward", Command.Type.MOTION,
                          Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                          new Shortcut('#'));
    parser
      .registerAction(Mapping.NVO, "VimSearchWordForward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g*"));
    parser
      .registerAction(Mapping.NVO, "VimSearchWordBackward", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut("g#"));
  }

  private static void registerNormalModeActions(@NotNull KeyParser parser) {
    // Copy/Paste Actions
    parser.registerAction(Mapping.N, "VimCopyPutTextBeforeCursor", Command.Type.PASTE,
                          new Shortcut('P'));
    parser.registerAction(Mapping.N, "VimCopyPutTextAfterCursor", Command.Type.PASTE,
                          new Shortcut('p'));
    parser.registerAction(Mapping.N, "VimCopyPutTextBeforeCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gP"));
    parser.registerAction(Mapping.N, "VimCopyPutTextAfterCursorMoveCursor", Command.Type.PASTE,
                          new Shortcut("gp"));
    parser.registerAction(Mapping.N, "VimCopyPutTextBeforeCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[P"),
      new Shortcut("]P")
    });
    parser.registerAction(Mapping.N, "VimCopyPutTextAfterCursorNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[p"),
      new Shortcut("]p")
    });
    parser.registerAction(Mapping.N, "VimCopyYankLine", Command.Type.COPY,
                          new Shortcut('Y'));
    parser.registerAction(Mapping.N, "VimCopyYankLine", Command.Type.COPY, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("yy"));
    parser.registerAction(Mapping.N, "VimCopyYankMotion", Command.Type.COPY, Command.FLAG_OP_PEND,
                          new Shortcut('y'), Argument.Type.MOTION);

    // Insert/Replace/Change Actions
    parser.registerAction(Mapping.N, "VimChangeCaseLowerMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gu"), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimChangeCaseToggleCharacter", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(Mapping.N, "VimChangeCaseToggleMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("g~"), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimChangeCaseUpperMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("gU"), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimChangeCharacter", Command.Type.CHANGE, Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser
      .registerAction(Mapping.N, "VimChangeCharacters", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('s'));
    parser
      .registerAction(Mapping.N, "VimChangeEndOfLine", Command.Type.CHANGE, Command.FLAG_NO_REPEAT | Command.FLAG_MULTIKEY_UNDO,
                      new Shortcut('C'));
    parser.registerAction(Mapping.N, "VimChangeLine", Command.Type.CHANGE,
                          Command.FLAG_NO_REPEAT | Command.FLAG_ALLOW_MID_COUNT | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut("cc"),
        new Shortcut('S')
      });
    parser.registerAction(Mapping.N, "VimChangeNumberInc", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.N, "VimChangeNumberDec", Command.Type.CHANGE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.N, "VimChangeMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND | Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('c'), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimChangeReplace", Command.Type.CHANGE, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('R'));
    parser.registerAction(Mapping.N, "VimDeleteCharacter", Command.Type.DELETE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(Mapping.N, "VimDeleteCharacterLeft", Command.Type.DELETE,
                          new Shortcut('X'));
    parser.registerAction(Mapping.N, "VimDeleteCharacterRight", Command.Type.DELETE,
                          new Shortcut('x'));
    parser.registerAction(Mapping.N, "VimDeleteEndOfLine", Command.Type.DELETE,
                          new Shortcut('D'));
    parser.registerAction(Mapping.N, "VimDeleteJoinLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(Mapping.N, "VimDeleteJoinLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(Mapping.N, "VimDeleteLine", Command.Type.DELETE, Command.FLAG_ALLOW_MID_COUNT,
                          new Shortcut("dd"));
    parser.registerAction(Mapping.N, "VimDeleteMotion", Command.Type.DELETE, Command.FLAG_OP_PEND,
                          new Shortcut('d'), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimFilterCountLines", Command.Type.CHANGE,
                          new Shortcut("!!"));
    parser.registerAction(Mapping.N, "VimFilterMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut('!'), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimInsertAfterCursor", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('a'));
    parser.registerAction(Mapping.N, "VimInsertAfterLineEnd", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('A'));
    parser.registerAction(Mapping.N, "VimInsertAtPreviousInsert", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gi"));
    parser.registerAction(Mapping.N, "VimInsertBeforeCursor", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
      new Shortcut('i'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0))
    });
    parser.registerAction(Mapping.N, "VimInsertBeforeFirstNonBlank", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('I'));
    parser.registerAction(Mapping.N, "VimInsertLineStart", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut("gI"));
    parser.registerAction(Mapping.N, "VimInsertNewLineAbove", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('O'));
    parser.registerAction(Mapping.N, "VimInsertNewLineBelow", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('o'));
    // Motion Actions
    parser
      .registerAction(Mapping.N, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('`'), Argument.Type.CHARACTER);
    parser
      .registerAction(Mapping.N, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE | Command.FLAG_SAVE_JUMP,
                      new Shortcut('\''), Argument.Type.CHARACTER);
    parser.registerAction(Mapping.N, "VimMotionGotoMark", Command.Type.MOTION, Command.FLAG_MOT_EXCLUSIVE,
                          new Shortcut("g`"), Argument.Type.CHARACTER);
    parser.registerAction(Mapping.N, "VimMotionGotoMarkLine", Command.Type.MOTION, Command.FLAG_MOT_LINEWISE,
                          new Shortcut("g'"), Argument.Type.CHARACTER);
    // Misc Actions
    parser.registerAction(Mapping.N, "VimLastSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut('&'));
    parser.registerAction(Mapping.N, "VimLastGlobalSearchReplace", Command.Type.OTHER_WRITABLE,
                          new Shortcut("g&"));
    parser.registerAction(Mapping.N, "VimVisualSelectPrevious", Command.Type.OTHER_READONLY,
                          new Shortcut("gv"));
    parser.registerAction(Mapping.N, "VimRepeatChange", Command.Type.OTHER_WRITABLE,
                          new Shortcut('.'));
    parser.registerAction(Mapping.N, "VimRepeatExCommand", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@:"));
    parser.registerAction(Mapping.N, "QuickJavaDoc", Command.Type.OTHER_READONLY,
                          new Shortcut('K'));
    parser.registerAction(Mapping.N, "VimRedo", Command.Type.OTHER_WRITABLE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.N, "VimUndo", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut('u'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UNDO, 0))
    });

    // File Actions
    parser.registerAction(Mapping.N, "VimFileSaveClose", Command.Type.OTHER_WRITABLE, new Shortcut[]{
      new Shortcut("ZQ"),
      new Shortcut("ZZ")
    });
    parser.registerAction(Mapping.N, "VimFilePrevious", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_6, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_CIRCUMFLEX, KeyEvent.CTRL_MASK))
    });

    // Shift Actions
    // TODO - add =
    // TODO - == will ignore count and only auto-indent 1 lines
    parser.registerAction(Mapping.N, "VimAutoIndentLines", Command.Type.CHANGE,
                          new Shortcut("=="));
    parser.registerAction(Mapping.N, "VimShiftLeftLines", Command.Type.CHANGE,
                          new Shortcut("<<"));
    parser.registerAction(Mapping.N, "VimShiftLeftMotion", Command.Type.CHANGE,
                          new Shortcut('<'), Argument.Type.MOTION);
    parser.registerAction(Mapping.N, "VimShiftRightLines", Command.Type.CHANGE,
                          new Shortcut(">>"));
    parser.registerAction(Mapping.N, "VimShiftRightMotion", Command.Type.CHANGE,
                          new Shortcut('>'), Argument.Type.MOTION);

    // Jump Actions

    parser.registerAction(Mapping.N, "VimMotionJumpNext", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(Mapping.N, "VimMotionJumpPrevious", Command.Type.OTHER_READONLY,
                          new Shortcut[] {
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)),
                            // TODO: <C-T> is a tag command similar to <C-O>, the tag stack is not implemented
                            new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK))
                          });

    parser.registerAction(Mapping.N, "VimFileGetAscii", Command.Type.OTHER_READONLY,
                          new Shortcut("ga"));
    parser.registerAction(Mapping.N, "VimFileGetHex", Command.Type.OTHER_READONLY,
                          new Shortcut("g8"));
    parser.registerAction(Mapping.N, "VimFileGetFileInfo", Command.Type.OTHER_READONLY,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK)));

    // Window Actions
    // TODO - CTRL-W commands: +, -, =, S, s, _, b, c, n, o, q, s, t, <up>, <down>

    // Macro Actions
    parser.registerAction(Mapping.N, "VimPlaybackLastRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut("@@"));
    parser.registerAction(Mapping.N, "VimPlaybackRegister", Command.Type.OTHER_WRITABLE,
                          new Shortcut('@'), Argument.Type.CHARACTER);
    // TODO - support for :map macros
  }

  private static void registerVisualModeActions(@NotNull KeyParser parser) {
    parser.registerAction(Mapping.V, "VimAutoIndentVisual", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE,
                          new Shortcut('='));
    parser.registerAction(Mapping.V, "VimReformatVisual", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_FORCE_LINEWISE,
                          new Shortcut("gq"));
    parser.registerAction(Mapping.V, "VimChangeCaseLowerVisual", Command.Type.CHANGE,
                          new Shortcut('u'));
    parser.registerAction(Mapping.V, "VimChangeCaseToggleVisual", Command.Type.CHANGE,
                          new Shortcut('~'));
    parser.registerAction(Mapping.V, "VimChangeCaseUpperVisual", Command.Type.CHANGE,
                          new Shortcut('U'));
    parser.registerAction(Mapping.V, "VimChangeVisual", Command.Type.CHANGE, Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
      new Shortcut('c'),
      new Shortcut('s')
    });
    parser.registerAction(Mapping.V, "VimChangeVisualCharacter", Command.Type.CHANGE, Command.FLAG_ALLOW_DIGRAPH,
                          new Shortcut('r'), Argument.Type.DIGRAPH);
    parser.registerAction(Mapping.V, "VimChangeVisualLines", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut('R'),
        new Shortcut('S')
      });
    parser.registerAction(Mapping.V, "VimChangeVisualLinesEnd", Command.Type.CHANGE,
                          Command.FLAG_MOT_LINEWISE | Command.FLAG_MULTIKEY_UNDO, new Shortcut[]{
        new Shortcut('C')
      });
    parser.registerAction(Mapping.V, "VimCopyYankVisual", Command.Type.COPY,
                          new Shortcut('y'));
    parser.registerAction(Mapping.V, "VimCopyYankVisualLines", Command.Type.COPY, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('Y'));
    parser.registerAction(Mapping.V, "VimDeleteJoinVisualLines", Command.Type.DELETE,
                          new Shortcut("gJ"));
    parser.registerAction(Mapping.V, "VimDeleteJoinVisualLinesSpaces", Command.Type.DELETE,
                          new Shortcut('J'));
    parser.registerAction(Mapping.V, "VimDeleteVisual", Command.Type.DELETE, new Shortcut[]{
      new Shortcut('d'),
      new Shortcut('x'),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0))
    });
    parser.registerAction(Mapping.V, "VimDeleteVisualLinesEnd", Command.Type.DELETE, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('D')
    });
    parser.registerAction(Mapping.V, "VimDeleteVisualLines", Command.Type.DELETE, Command.FLAG_MOT_LINEWISE, new Shortcut[]{
      new Shortcut('X')
    });
    parser.registerAction(Mapping.V, "VimFilterVisualLines", Command.Type.CHANGE, Command.FLAG_MOT_LINEWISE,
                          new Shortcut('!'));
    parser.registerAction(Mapping.V, "VimShiftLeftVisual", Command.Type.CHANGE,
                          new Shortcut('<'));
    parser.registerAction(Mapping.V, "VimShiftRightVisual", Command.Type.CHANGE,
                          new Shortcut('>'));
    parser.registerAction(Mapping.V, "VimVisualExitMode", Command.Type.OTHER_READONLY, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
    });
    parser.registerAction(Mapping.V, "VimVisualPutText", Command.Type.PASTE, new Shortcut[]{
      new Shortcut('P'),
      new Shortcut('p')
    });
    parser.registerAction(Mapping.V, "VimVisualPutTextMoveCursor", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("gp"),
      new Shortcut("gP")
    });
    parser.registerAction(Mapping.V, "VimVisualPutTextNoIndent", Command.Type.PASTE, new Shortcut[]{
      new Shortcut("[p"),
      new Shortcut("]p"),
      new Shortcut("[P"),
      new Shortcut("]P")
    });
    parser.registerAction(Mapping.V, "VimVisualBlockInsert", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('I'));
    parser.registerAction(Mapping.V, "VimVisualBlockAppend", Command.Type.INSERT, Command.FLAG_MULTIKEY_UNDO,
                          new Shortcut('A'));
    parser.registerAction(Mapping.V, "VimVisualSwapEnds", Command.Type.OTHER_READONLY,
                          new Shortcut('o'));
    parser.registerAction(Mapping.V, "VimVisualSwapEndsBlock", Command.Type.OTHER_READONLY,
                          new Shortcut('O'));
    parser.registerAction(Mapping.V, "VimVisualSwapSelections", Command.Type.OTHER_READONLY,
                          new Shortcut("gv"));
  }

  private static void registerInsertModeActions(@NotNull KeyParser parser) {
    // Other insert actions
    parser
      .registerAction(Mapping.I, "VimEditorBackSpace", Command.Type.INSERT, Command.FLAG_IS_BACKSPACE,
                      new Shortcut[]{
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_MASK)),
                        new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0))
                      });
    parser.registerAction(Mapping.I, "VimEditorDelete", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0)));
    parser.registerAction(Mapping.I, "VimEditorDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0))
    });
    parser.registerAction(Mapping.I, "VimEditorTab", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
    });
    parser.registerAction(Mapping.I, "VimEditorUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0))
    });
    parser.registerAction(Mapping.I, "VimInsertCharacterAboveCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimInsertCharacterBelowCursor", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimInsertDeleteInsertedText", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimInsertDeletePreviousWord", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimInsertEnter", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    });
    parser.registerAction(Mapping.I, "VimInsertExitMode", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke('[', KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)),
      new Shortcut(new KeyStroke[]{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, KeyEvent.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK)})
    });
    parser.registerAction(Mapping.I, "VimInsertHelp", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0))
    });
    parser.registerAction(Mapping.I, "VimInsertPreviousInsert", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimInsertPreviousInsertExit", Command.Type.INSERT, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_AT, KeyEvent.CTRL_MASK))
    });
    parser.registerAction(Mapping.I, "VimInsertRegister", Command.Type.INSERT,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK)),
                          Argument.Type.CHARACTER);
    parser.registerAction(Mapping.I, "VimInsertReplaceToggle", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0)));
    parser.registerAction(Mapping.I, "VimInsertSingleCommand", Command.Type.INSERT,
                          Command.FLAG_CLEAR_STROKES | Command.FLAG_EXPECT_MORE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimMotionFirstColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0)));
    parser.registerAction(Mapping.I, "VimMotionGotoLineFirst", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimMotionGotoLineLastEnd", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimMotionLastColumn", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0)));
    parser.registerAction(Mapping.I, "VimMotionLeft", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0))
    });
    parser.registerAction(Mapping.I, "VimMotionRight", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0))
    });
    parser.registerAction(Mapping.I, "VimMotionScrollPageUp", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.I, "VimMotionScrollPageDown", Command.Type.INSERT, Command.FLAG_CLEAR_STROKES, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.I, "VimMotionWordLeft", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.I, "VimMotionWordRight", Command.Type.INSERT, Command.FLAG_SAVE_STROKE, new Shortcut[]{
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.CTRL_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.SHIFT_MASK)),
      new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.SHIFT_MASK))
    });
    parser.registerAction(Mapping.I, "VimShiftLeftLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)));
    parser.registerAction(Mapping.I, "VimShiftRightLines", Command.Type.INSERT, Command.FLAG_SAVE_STROKE,
                          new Shortcut(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK)));
  }
}
