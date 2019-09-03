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

package com.maddyhome.idea.vim.regexp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.helper.Msg;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RegExp {
  @Nullable public static reg_extmatch_T re_extmatch_out = null;
  @Nullable public static reg_extmatch_T re_extmatch_in = null;

  /*
     * The opcodes are:
     */

  /* definition                   number             opnd?    meaning */
  private static final int END = 0;       /*      End of program or NOMATCH operand. */
  private static final int BOL = 1;       /*      Match "" at beginning of line. */
  private static final int EOL = 2;       /*      Match "" at end of line. */
  private static final int BRANCH = 3;       /* node Match this alternative, or the next... */
  private static final int BACK = 4;       /*      Match "", "next" ptr points backward. */
  private static final int EXACTLY = 5;       /* str  Match this string. */
  private static final int NOTHING = 6;       /*      Match empty string. */
  private static final int STAR = 7;       /* node Match this (simple) thing 0 or more times. */
  private static final int PLUS = 8;       /* node Match this (simple) thing 1 or more times. */
  private static final int MATCH = 9;       /* node match the operand zero-width */
  private static final int NOMATCH = 10;      /* node check for no match with operand */
  private static final int BEHIND = 11;      /* node look behind for a match with operand */
  private static final int NOBEHIND = 12;      /* node look behind for no match with operand */
  private static final int SUBPAT = 13;      /* node match the operand here */
  private static final int BRACE_SIMPLE = 14;  /* node Match this (simple) thing between m and
                                                  *      n times (\{m,n\}). */
  private static final int BOW = 15;      /*      Match "" after [^a-zA-Z0-9_] */
  private static final int EOW = 16;      /*      Match "" at    [^a-zA-Z0-9_] */
  private static final int BRACE_LIMITS = 17;  /* nr nr  define the min & max for BRACE_SIMPLE
                                                  *      and BRACE_COMPLEX. */
  private static final int NEWL = 18;      /*      Match line-break */
  private static final int BHPOS = 19;      /*      End position for BEHIND or NOBEHIND */


  /* character classes: 20-48 normal, 50-78 include a line-break */
  private static final int ADD_NL = 30;
  private static final int ANY = 20;      /*      Match any one character. */
  private static final int FIRST_NL = ANY + ADD_NL;
  private static final int ANYOF = 21;      /* str  Match any character in this string. */
  private static final int ANYBUT = 22;      /* str  Match any character not in this
                                                  *      string. */
  private static final int IDENT = 23;      /*      Match identifier char */
  private static final int SIDENT = 24;      /*      Match identifier char but no digit */
  private static final int KWORD = 25;      /*      Match keyword char */
  private static final int SKWORD = 26;      /*      Match word char but no digit */
  private static final int FNAME = 27;      /*      Match file name char */
  private static final int SFNAME = 28;      /*      Match file name char but no digit */
  private static final int PRINT = 29;      /*      Match printable char */
  private static final int SPRINT = 30;      /*      Match printable char but no digit */
  private static final int WHITE = 31;      /*      Match whitespace char */
  private static final int NWHITE = 32;      /*      Match non-whitespace char */
  private static final int DIGIT = 33;      /*      Match digit char */
  private static final int NDIGIT = 34;      /*      Match non-digit char */
  private static final int HEX = 35;      /*      Match hex char */
  private static final int NHEX = 36;      /*      Match non-hex char */
  private static final int OCTAL = 37;      /*      Match octal char */
  private static final int NOCTAL = 38;      /*      Match non-octal char */
  private static final int WORD = 39;      /*      Match word char */
  private static final int NWORD = 40;      /*      Match non-word char */
  private static final int HEAD = 41;      /*      Match head char */
  private static final int NHEAD = 42;      /*      Match non-head char */
  private static final int ALPHA = 43;      /*      Match alpha char */
  private static final int NALPHA = 44;      /*      Match non-alpha char */
  private static final int LOWER = 45;      /*      Match lowercase char */
  private static final int NLOWER = 46;      /*      Match non-lowercase char */
  private static final int UPPER = 47;      /*      Match uppercase char */
  private static final int NUPPER = 48;      /*      Match non-uppercase char */
  private static final int LAST_NL = NUPPER + ADD_NL;
  private static final int MOPEN = 80;     /* -89       Mark this point in input as start of
                                                 *       \( subexpr.  MOPEN + 0 marks start of
                                                 *       match. */
  private static final int MCLOSE = 90;     /* -99       Analogous to MOPEN.  MCLOSE + 0 marks
                                                 *       end of match. */
  private static final int BACKREF = 100;     /* -109 node Match same string again \1-\9 */

  private static final int ZOPEN = 110;     /* -119      Mark this point in input as start of
                                                 *       \z( subexpr. */
  private static final int ZCLOSE = 120;     /* -129      Analogous to ZOPEN. */
  private static final int ZREF = 130;     /* -139 node Match external submatch \z1-\z9 */

  private static final int BRACE_COMPLEX = 140; /* -149 node Match nodes between m & n times */

  private static final int NOPEN = 150;     /*      Mark this point in input as start of
                                                        \%( subexpr. */
  private static final int NCLOSE = 151;     /*      Analogous to NOPEN. */

  private static final int RE_BOF = 201;     /*      Match "" at beginning of file. */
  private static final int RE_EOF = 202;     /*      Match "" at end of file. */
  private static final int CURSOR = 203;     /*      Match location of cursor. */

  private static final int RE_LNUM = 204;     /* nr cmp  Match line number */
  private static final int RE_COL = 205;     /* nr cmp  Match column number */
  private static final int RE_VCOL = 206;     /* nr cmp  Match virtual column number */

  private static final int REGMAGIC = 0234;

  private static final int REX_SET = 1;
  private static final int REX_USE = 2;

  private static final int MAX_LIMIT = Integer.MAX_VALUE;

  private static final int NOT_MULTI = 0;
  private static final int MULTI_ONE = 1;
  private static final int MULTI_MULT = 2;

  /*
    * Flags to be passed up and down.
    */
  private static final int HASWIDTH = 0x1;    /* Known never to match null string. */
  private static final int SIMPLE = 0x2;    /* Simple enough to be STAR/PLUS operand. */
  private static final int SPSTART = 0x4;    /* Starts with * or +. */
  private static final int HASNL = 0x8;    /* Contains some \n. */
  private static final int WORST = 0;      /* Worst case. */

  /*
    * REGEXP_INRANGE contains all characters which are always special in a []
    * range after '\'.
    * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
    * These are:
    *  \n  - New line (NL).
    *  \r  - Carriage Return (CR).
    *  \t  - Tab (TAB).
    *  \e  - Escape (ESC).
    *  \b  - Backspace (Ctrl_H).
    */
  private static final String REGEXP_INRANGE = "]^-n\\";
  private static final String REGEXP_ABBR = "nrteb";

  /* flags for regflags */
  private static final int RF_ICASE = 1;  /* ignore case */
  private static final int RF_NOICASE = 2;  /* don't ignore case */
  private static final int RF_HASNL = 4;  /* can match a NL */

  /*
     * Global work variables for vim_regcomp().
     */
  private static final int NSUBEXP = 10;

  private static final int MAGIC_NONE = 1;      /* "\V" very unmagic */
  private static final int MAGIC_OFF = 2;      /* "\M" or 'magic' off */
  private static final int MAGIC_ON = 3;      /* "\m" or 'magic' */
  private static final int MAGIC_ALL = 4;      /* "\v" very magic */

  /*
     * META contains all characters that may be magic, except '^' and '$'.
     */

  /* META[] is used often enough to justify turning it into a table. */
  private static final int[] META_flags = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /*                 %  &     (  )  *  +        .    */
    0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0,
    /*     1  2  3  4  5  6  7  8  9        <  =  >  ? */
    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1,
    /*  @  A     C  D     F     H  I     K  L  M     O */
    1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1,
    /*  P        S     U  V  W  X        [           _ */
    1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1,
    /*     a     c  d     f     h  i     k  l  m  n  o */
    0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1,
    /*  p        s     u  v  w  x     z  {  |     ~    */
    1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1
  };

  /* arguments for reg() */
  private static final int REG_NOPAREN = 0;       /* toplevel reg() */
  private static final int REG_PAREN = 1;       /* \(\) */
  private static final int REG_ZPAREN = 2;       /* \z(\) */
  private static final int REG_NPAREN = 3;       /* \%(\) */

  private static boolean WITH_NL(int op) {
    return op >= FIRST_NL && op <= LAST_NL;
  }

  /*
     * The first byte of the regexp internal "program" is actually this magic
     * number; the start node begins in the second byte.  It's used to catch the
     * most severe mutilation of the program by the caller.
     */

  /*
     * Opcode notes:
     *
     * BRANCH       The set of branches constituting a single choice are hooked
     *              together with their "next" pointers, since precedence prevents
     *              anything being concatenated to any individual branch.  The
     *              "next" pointer of the last BRANCH in a choice points to the
     *              thing following the whole choice.  This is also where the
     *              final "next" pointer of each individual branch points; each
     *              branch starts with the operand node of a BRANCH node.
     *
     * BACK         Normal "next" pointers all implicitly point forward; BACK
     *              exists to make loop structures possible.
     *
     * STAR,PLUS    '=', and complex '*' and '+', are implemented as circular
     *              BRANCH structures using BACK.  Simple cases (one character
     *              per match) are implemented with STAR and PLUS for speed
     *              and to minimize recursive plunges.
     *
     * BRACE_LIMITS This is always followed by a BRACE_SIMPLE or BRACE_COMPLEX
     *              node, and defines the min and max limits to be used for that
     *              node.
     *
     * MOPEN,MCLOSE ...are numbered at compile time.
     * ZOPEN,ZCLOSE ...ditto
     */

  /*
     * A node is one char of opcode followed by two chars of "next" pointer.
     * "Next" pointers are stored as two 8-bit bytes, high order first.  The
     * value is a positive offset from the opcode of the node containing it.
     * An operand, if any, simply follows the node.  (Note that much of the
     * code generation knows about this implicit relationship.)
     *
     * Using two bytes for the "next" pointer is vast overkill for most things,
     * but allows patterns to get big without disasters.
     */
//    #define OP(p)           ((int)*(p))
//    #define NEXT(p)         (((*((p) + 1) & 0377) << 8) + (*((p) + 2) & 0377))
//    #define OPERAND(p)      ((p) + 3)
/* Obtain an operand that was stored as four bytes, MSB first. */
//    #define OPERAND_MIN(p)  (((long)(p)[3] << 24) + ((long)(p)[4] << 16) \
//    + ((long)(p)[5] << 8) + (long)(p)[6])
/* Obtain a second operand stored as four bytes. */
//    #define OPERAND_MAX(p)  OPERAND_MIN((p) + 4)
/* Obtain a second single-byte operand stored after a four bytes operand. */
//    #define OPERAND_CMP(p)  (p)[7]

  /*
     * Utility definitions.
     */
  //    #define UCHARAT(p)      ((int)*(char_u *)(p))

  /* Used for an error (down from) vim_regcomp(): give the error message, set rc_did_emsg and return null */
  /*
    #define EMSG_RET_null(m)
    {
        EMSG(_(m));
        rc_did_emsg = true;
        return null;
    }
    #define EMSG_M_RET_null(m, c)
    {
        EMSG2(_(m), c ? "" : "\\");
        rc_did_emsg = true;
        return null;
    }
    #define EMSG_RET_FAIL(m)
    {
        EMSG(_(m));
        rc_did_emsg = true;
        return FAIL;
    }
    #define EMSG_ONE_RET_null
    */

  //EMSG_M_RET_null("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL)

  private void EMSG_RET_null(@NotNull String key) {
    VimPlugin.showMessage(MessageHelper.message(key));
  }

  private void EMSG_M_RET_null(@NotNull String key, boolean isMagic) {
    String val = isMagic ? "" : "\\";
    VimPlugin.showMessage(MessageHelper.message(key, val));
  }

  private void EMSG_ONE_RET_null() {
    EMSG_M_RET_null(Msg.E369, reg_magic == MAGIC_ALL);
  }

  /*
     * Return NOT_MULTI if c is not a "multi" operator.
     * Return MULTI_ONE if c is a single "multi" operator.
     * Return MULTI_MULT if c is a multi "multi" operator.
     */
  private int re_multi_type(int c) {
    if (c == Magic.AT || c == Magic.EQUAL || c == Magic.QUESTION) {
      return MULTI_ONE;
    }
    if (c == Magic.STAR || c == Magic.PLUS || c == Magic.LCURLY) {
      return MULTI_MULT;
    }
    return NOT_MULTI;
  }

  /*
     * Translate '\x' to its control character, except "\n", which is Magic.
     */
  private int backslash_trans(int c) {
    switch (c) {
      case 'r':
        return '\r';
      case 't':
        return '\t';
      case 'e':
        return 0x1b;
      case 'b':
        return '\b';
    }
    return c;
  }

  /*
     * Check for a character class name.  "pp" points to the '['.
     * Returns one of the CLASS_ items. CLASS_NONE means that no item was
     * recognized.  Otherwise "pp" is advanced to after the item.
     */
  private static int skip_class_name(@NotNull CharPointer pp) {
    int i;

    if (pp.charAt(1) == ':') {
      for (i = 0; i < CharacterClasses.CLASS_NAMES.length; ++i) {
        if (pp.ref(2).strncmp(CharacterClasses.CLASS_NAMES[i], CharacterClasses.CLASS_NAMES[i].length()) == 0) {
          pp.inc(CharacterClasses.CLASS_NAMES[i].length() + 2);
          return i;
        }
      }
    }

    return CharacterClasses.CLASS_NONE;
  }

  /*
     * Skip over a "[]" range.
     * "p" must point to the character after the '['.
     * The returned pointer is on the matching ']', or the terminating NUL.
     */
  @NotNull
  private static CharPointer skip_anyof(@NotNull CharPointer p) {
    if (p.charAt() == '^')      /* Complement of range. */ {
      p.inc();
    }
    if (p.charAt() == ']' || p.charAt() == '-') {
      p.inc();
    }
    while (!p.end() && p.charAt() != ']') {
      if (p.charAt() == '-') {
        p.inc();
        if (!p.end() && p.charAt() != ']') {
          p.inc();
        }
      }
      else if (p.charAt() == '\\' &&
               (REGEXP_INRANGE.indexOf(p.charAt(1)) != -1 || REGEXP_ABBR.indexOf(p.charAt(1)) != -1)) {
        p.inc(2);
      }
      else if (p.charAt() == '[') {
        if (skip_class_name(p) == CharacterClasses.CLASS_NONE) {
          p.inc(); /* It was not a class name */
        }
      }
      else {
        p.inc();
      }
    }

    return p;
  }

  /*
     * Return true if compiled regular expression "prog" can match a line break.
     */
  public int re_multiline(@NotNull regprog_T prog) {
    return (prog.regflags & RF_HASNL);
  }

  /*
     * Skip past regular expression.
     * Stop at end of 'p' of where 'dirc' is found ('/', '?', etc).
     * Take care of characters with a backslash in front of it.
     * Skip strings inside [ and ].
     */
  @NotNull
  public static CharPointer skip_regexp(@NotNull CharPointer p, char dirc, boolean magic) {
    int mymagic;

    if (magic) {
      mymagic = MAGIC_ON;
    }
    else {
      mymagic = MAGIC_OFF;
    }

    for (; !p.end(); p.inc()) {
      if (p.charAt() == dirc)       /* found end of regexp */ {
        break;
      }
      if ((p.charAt() == '[' && mymagic >= MAGIC_ON) ||
          (p.charAt() == '\\' && p.charAt(1) == '[' && mymagic <= MAGIC_OFF)) {
        p = skip_anyof(p.ref(1));
        if (p.end()) {
          break;
        }
      }
      else if (p.charAt() == '\\' && p.charAt(1) != '\u0000') {
        p.inc();    /* skip next character */
        if (p.charAt() == 'v') {
          mymagic = MAGIC_ALL;
        }
        else if (p.charAt() == 'V') {
          mymagic = MAGIC_NONE;
        }
      }
    }

    return p;
  }

  /*
     * vim_regcomp - compile a regular expression into internal code
     *
     * We can't allocate space until we know how big the compiled form will be,
     * but we can't compile it (and thus know how big it is) until we've got a
     * place to put the code.  So we cheat:  we compile it twice, once with code
     * generation turned off and size counting turned on, and once "for real".
     * This also means that we don't allocate space until we are sure that the
     * thing really will compile successfully, and we never have to move the
     * code and thus invalidate pointers into it.  (Note that it has to be in
     * one piece because vim_free() must be able to free it all.)
     *
     * Whether upper/lower case is to be ignored is decided when executing the
     * program, it does not matter here.
     *
     * Beware that the optimization-preparation code in here knows about some
     * of the structure of the compiled regexp.
     */
  @Nullable
  public regprog_T vim_regcomp(@Nullable String expr, int magic) {
    regprog_T r;
    CharPointer scan;
    CharPointer longest;
    int len;
    Flags flags = new Flags();

    if (expr == null) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_null));
      return null;
    }

    r = new regprog_T();

    /*
         * Second pass: emit code.
         */
    regcomp_start(expr, magic);
    regcode = new CharPointer(r.program);
    regc(REGMAGIC);
    if (reg(REG_NOPAREN, flags) == null) {
      return null;
    }

    /* Dig out information for optimizations. */
    r.regstart = 0;          /* Worst-case defaults. */
    r.reganch = 0;
    r.regmust = null;
    r.regmlen = 0;
    r.regflags = regflags;
    if (flags.isSet(HASNL)) {
      r.regflags |= RF_HASNL;
    }
    /* Remember whether this pattern has any \z specials in it. */
    r.reghasz = re_has_z;
    scan = (new CharPointer(r.program)).ref(1);      /* First BRANCH. */
    if (regnext(scan).OP() == END)   /* Only one top-level choice. */ {
      scan = scan.OPERAND();

      /* Starting-point info. */
      if (scan.OP() == BOL || scan.OP() == RE_BOF) {
        r.reganch++;
        scan = regnext(scan);
      }

      if (scan.OP() == EXACTLY) {
        r.regstart = scan.OPERAND().charAt();
      }
      else if ((scan.OP() == BOW
                || scan.OP() == EOW
                || scan.OP() == NOTHING
                || scan.OP() == MOPEN || scan.OP() == NOPEN
                || scan.OP() == MCLOSE || scan.OP() == NCLOSE)
               && regnext(scan).OP() == EXACTLY) {
        r.regstart = regnext(scan).OPERAND().charAt();
      }

      /*
             * If there's something expensive in the r.e., find the longest
             * literal string that must appear and make it the regmust.  Resolve
             * ties in favor of later strings, since the regstart check works
             * with the beginning of the r.e. and avoiding duplication
             * strengthens checking.  Not a strong reason, but sufficient in the
             * absence of others.
             */
      /*
             * When the r.e. starts with BOW, it is faster to look for a regmust
             * first. Used a lot for "#" and "*" commands. (Added by mool).
             */
      if ((flags.isSet(SPSTART) || scan.OP() == BOW || scan.OP() == EOW)
          && !(flags.isSet(HASNL))) {
        longest = null;
        len = 0;
        for (; scan != null; scan = regnext(scan)) {
          CharPointer so = scan.OPERAND();
          if (scan.OP() == EXACTLY && so.strlen() >= len) {
            longest = so.ref(0);
            len = so.strlen();
          }
        }
        if (longest != null) {
          r.regmust = longest.ref(0);
        }
        r.regmlen = len;
      }
    }

    if (logger.isDebugEnabled()) logger.debug(regdump(expr, r));

    return r;
  }

  /*
     * Setup to parse the regexp.  Used once to get the length and once to do it.
     */
  private void regcomp_start(@NotNull String expr, int magic) {
    initchr(expr);
    if (magic != 0) {
      reg_magic = MAGIC_ON;
    }
    else {
      reg_magic = MAGIC_OFF;
    }
    num_complex_braces = 0;
    regnpar = 1;
    for (int i = 0; i < had_endbrace.length; i++) {
      had_endbrace[i] = false;
    }
    regnzpar = 1;
    re_has_z = 0;
    regflags = 0;
    had_eol = false;
  }

  /*
     * Check if during the previous call to vim_regcomp the EOL item "$" has been
     * found.  This is messy, but it works fine.
     */
  public boolean vim_regcomp_had_eol() {
    return had_eol;
  }

  /*
     * reg - regular expression, i.e. main body or parenthesized thing
     *
     * Caller must absorb opening parenthesis.
     *
     * Combining parenthesis handling with the base level of regular expression
     * is a trifle forced, but the need to tie the tails of the branches to what
     * follows makes it hard to avoid.
     */
  @Nullable
  private CharPointer reg(int paren, @NotNull Flags flagp) {
    CharPointer ret;
    CharPointer br;
    CharPointer ender;
    int parno = 0;
    Flags flags = new Flags();

    flagp.init(HASWIDTH);          /* Tentatively. */

    if (paren == REG_ZPAREN) {
      /* Make a ZOPEN node. */
      if (regnzpar >= NSUBEXP) {
        VimPlugin.showMessage(MessageHelper.message(Msg.E50));
        return null;
      }
      parno = regnzpar;
      regnzpar++;
      ret = regnode(ZOPEN + parno);
    }
    else if (paren == REG_PAREN) {
      /* Make a MOPEN node. */
      if (regnpar >= NSUBEXP) {
        EMSG_M_RET_null(Msg.E51, reg_magic == MAGIC_ALL);
        return null;
      }
      parno = regnpar;
      ++regnpar;
      ret = regnode(MOPEN + parno);
    }
    else if (paren == REG_NPAREN) {
      /* Make a NOPEN node. */
      ret = regnode(NOPEN);
    }
    else {
      ret = null;
    }

    /* Pick up the branches, linking them together. */
    br = regbranch(flags);
    if (br == null) {
      return null;
    }
    if (ret != null) {
      regtail(ret, br);       /* [MZ]OPEN -> first. */
    }
    else {
      ret = br.ref(0);
    }
    /* If one of the branches can be zero-width, the whole thing can.
         * If one of the branches has * at start or matches a line-break, the
         * whole thing can. */
    if (!flags.isSet(HASWIDTH)) {
      flagp.unset(HASWIDTH);
    }
    flagp.set(flags.get() & (SPSTART | HASNL));
    while (peekchr() == Magic.PIPE) {
      skipchr();
      br = regbranch(flags);
      if (br == null) {
        return null;
      }
      regtail(ret, br);       /* BRANCH -> BRANCH. */
      if (!flags.isSet(HASWIDTH)) {
        flagp.unset(HASWIDTH);
      }
      flagp.set(flags.get() & (SPSTART | HASNL));
    }

    /* Make a closing node, and hook it on the end. */
    ender = regnode(paren == REG_ZPAREN ? ZCLOSE + parno : paren == REG_PAREN ? MCLOSE + parno :
                                                           paren == REG_NPAREN ? NCLOSE : END);
    regtail(ret, ender);

    /* Hook the tails of the branches to the closing node. */
    for (br = ret.ref(0); br != null; br = regnext(br)) {
      regoptail(br, ender);
    }

    /* Check for proper termination. */
    if (paren != REG_NOPAREN && getchr() != Magic.RPAREN) {
      if (paren == REG_ZPAREN) {
        VimPlugin.showMessage(MessageHelper.message(Msg.E52));
        return null;
      }
      else if (paren == REG_NPAREN) {
        EMSG_M_RET_null(Msg.E53, reg_magic == MAGIC_ALL);
        return null;
      }
      else {
        EMSG_M_RET_null(Msg.E54, reg_magic == MAGIC_ALL);
        return null;
      }
    }
    else if (paren == REG_NOPAREN && peekchr() != '\u0000') {
      if (curchr == Magic.LPAREN) {
        EMSG_M_RET_null(Msg.E55, reg_magic == MAGIC_ALL);
        return null;
      }
      else {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_trailing));
        return null;
      }
      /* NOTREACHED */
    }
    /*
         * Here we set the flag allowing back references to this set of
         * parentheses.
         */
    if (paren == REG_PAREN) {
      had_endbrace[parno] = true;     /* have seen the close paren */
    }

    return ret;
  }

  /*
     * regbranch - one alternative of an | operator
     *
     * Implements the & operator.
     */
  @Nullable
  private CharPointer regbranch(@NotNull Flags flagp) {
    CharPointer ret;
    CharPointer chain = null;
    CharPointer latest;
    Flags flags = new Flags();

    flagp.init(WORST | HASNL);             /* Tentatively. */

    ret = regnode(BRANCH);
    for (; ; ) {
      latest = regconcat(flags);
      if (latest == null) {
        return null;
      }

      /* If one of the branches has width, the whole thing has.  If one of
             * the branches anchors at start-of-line, the whole thing does. */
      flagp.set(flags.get() & (HASWIDTH | SPSTART));
      /* If one of the branches doesn't match a line-break, the whole thing
             * doesn't. */
      flagp.set(flagp.get() & (~HASNL | (flags.get() & HASNL)));
      if (chain != null) {
        regtail(chain, latest);
      }
      if (peekchr() != Magic.AMP) {
        break;
      }
      skipchr();
      regtail(latest, regnode(END)); /* operand ends */
      reginsert(MATCH, latest.ref(0));
      chain = latest.ref(0);
    }

    return ret;
  }

  /*
     * regbranch - one alternative of an | or & operator
     *
     * Implements the concatenation operator.
     */
  @Nullable
  private CharPointer regconcat(@NotNull Flags flagp) {
    CharPointer first = null;
    CharPointer chain = null;
    CharPointer latest;
    Flags flags = new Flags();
    boolean cont = true;

    flagp.init(WORST);             /* Tentatively. */

    while (cont) {
      switch (peekchr()) {
        case '\u0000':
        case Magic.PIPE:
        case Magic.AMP:
        case Magic.RPAREN:
          cont = false;
          break;
        case Magic.c:
          regflags |= RF_ICASE;
          skipchr_keepstart();
          break;
        case Magic.C:
          regflags |= RF_NOICASE;
          skipchr_keepstart();
          break;
        case Magic.v:
          reg_magic = MAGIC_ALL;
          skipchr_keepstart();
          curchr = -1;
          break;
        case Magic.m:
          reg_magic = MAGIC_ON;
          skipchr_keepstart();
          curchr = -1;
          break;
        case Magic.M:
          reg_magic = MAGIC_OFF;
          skipchr_keepstart();
          curchr = -1;
          break;
        case Magic.V:
          reg_magic = MAGIC_NONE;
          skipchr_keepstart();
          curchr = -1;
          break;
        default:
          latest = regpiece(flags);
          if (latest == null) {
            return null;
          }
          flagp.set(flags.get() & (HASWIDTH | HASNL));
          if (chain == null)  /* First piece. */ {
            flagp.set(flags.get() & SPSTART);
          }
          else {
            regtail(chain, latest);
          }
          chain = latest.ref(0);
          if (first == null) {
            first = latest.ref(0);
          }
          break;
      }
    }
    if (first == null)          /* Loop ran zero times. */ {
      first = regnode(NOTHING);
    }

    return first;
  }

  /*
     * regpiece - something followed by possible [*+=]
     *
     * Note that the branching code sequences used for = and the general cases
     * of * and + are somewhat optimized:  they use the same NOTHING node as
     * both the endmarker for their branch list and the body of the last branch.
     * It might seem that this node could be dispensed with entirely, but the
     * endmarker role is not redundant.
     */
  @Nullable
  private CharPointer regpiece(@NotNull Flags flagp) {
    CharPointer ret;
    int op;
    CharPointer next;
    Flags flags = new Flags();

    ret = regatom(flags);
    if (ret == null) {
      return null;
    }

    op = peekchr();
    if (re_multi_type(op) == NOT_MULTI) {
      flagp.init(flags.get());
      return ret;
    }
    if (!(flags.isSet(HASWIDTH)) && re_multi_type(op) == MULTI_MULT) {
      if (op == Magic.STAR) {
        EMSG_M_RET_null(Msg.E56, reg_magic >= MAGIC_ON);
        return null;
      }
      if (op == Magic.PLUS) {
        EMSG_M_RET_null(Msg.E57, reg_magic == MAGIC_ALL);
        return null;
      }
      /* "\{}" is checked below, it's allowed when there is an upper limit */
    }
    flagp.init((WORST | SPSTART | (flags.get() & HASNL)));       /* default flags */

    skipchr();
    switch (op) {
      case Magic.STAR:
        if (flags.isSet(SIMPLE)) {
          reginsert(STAR, ret.ref(0));
        }
        else {
          /* Emit x* as (x&|), where & means "self". */
          reginsert(BRANCH, ret.ref(0)); /* Either x */
          regoptail(ret, regnode(BACK));  /* and loop */
          regoptail(ret, ret);    /* back */
          regtail(ret, regnode(BRANCH));  /* or */
          regtail(ret, regnode(NOTHING)); /* null. */
        }
        break;

      case Magic.PLUS:
        if (flags.isSet(SIMPLE)) {
          reginsert(PLUS, ret.ref(0));
        }
        else {
          /* Emit x+ as x(&|), where & means "self". */
          next = regnode(BRANCH); /* Either */
          regtail(ret, next);
          regtail(regnode(BACK), ret);    /* loop back */
          regtail(next, regnode(BRANCH)); /* or */
          regtail(ret, regnode(NOTHING)); /* null. */
        }
        flagp.init((WORST | HASWIDTH | (flags.get() & HASNL)));
        break;

      case Magic.AT: {
        int lop = END;

        switch (Magic.no_Magic(getchr())) {
          case '=':
            lop = MATCH;
            break;                 /* \@= */
          case '!':
            lop = NOMATCH;
            break;               /* \@! */
          case '>':
            lop = SUBPAT;
            break;                /* \@> */
          case '<':
            switch (Magic.no_Magic(getchr())) {
              case '=':
                lop = BEHIND;
                break;   /* \@<= */
              case '!':
                lop = NOBEHIND;
                break; /* \@<! */
            }
        }
        if (lop == END) {
          EMSG_M_RET_null(Msg.E59, reg_magic == MAGIC_ALL);
          return null;
        }
        /* Look behind must match with behind_pos. */
        if (lop == BEHIND || lop == NOBEHIND) {
          regtail(ret, regnode(BHPOS));
        }
        regtail(ret, regnode(END)); /* operand ends */
        reginsert(lop, ret.ref(0));
        break;
      }

      case Magic.QUESTION:
      case Magic.EQUAL:
        /* Emit x= as (x|) */
        reginsert(BRANCH, ret.ref(0));             /* Either x */
        regtail(ret, regnode(BRANCH));      /* or */
        next = regnode(NOTHING);            /* null. */
        regtail(ret, next);
        regoptail(ret, next);
        break;

      case Magic.LCURLY:
        MinMax limits = read_limits();
        if (limits == null) {
          return null;
        }
        int maxval = limits.maxvalue;
        int minval = limits.minvalue;
        if (!(flags.isSet(HASWIDTH)) && (maxval > minval ? maxval >= MAX_LIMIT : minval >= MAX_LIMIT)) {
          EMSG_M_RET_null(Msg.E58, reg_magic == MAGIC_ALL);
          return null;
        }
        if (flags.isSet(SIMPLE)) {
          reginsert(BRACE_SIMPLE, ret.ref(0));
          reginsert_limits(BRACE_LIMITS, minval, maxval, ret.ref(0));
        }
        else {
          if (num_complex_braces >= 10) {
            EMSG_M_RET_null(Msg.E60, reg_magic == MAGIC_ALL);
            return null;
          }
          reginsert(BRACE_COMPLEX + num_complex_braces, ret.ref(0));
          regoptail(ret, regnode(BACK));
          regoptail(ret, ret);
          reginsert_limits(BRACE_LIMITS, minval, maxval, ret.ref(0));
          ++num_complex_braces;
        }
        if (minval > 0 && maxval > 0) {
          flagp.init((HASWIDTH | (flags.get() & HASNL)));
        }
        break;
    }
    if (re_multi_type(peekchr()) != NOT_MULTI) {
      /* Can't have a multi follow a multi. */
      if (peekchr() == Magic.STAR) {
        String val = reg_magic >= MAGIC_ON ? "" : "\\";
        VimPlugin.showMessage(MessageHelper.message(Msg.E61, val));
      }
      else {
        String val = reg_magic >= MAGIC_ON ? "" : "\\";
        VimPlugin.showMessage(MessageHelper.message(Msg.E62, val, Character.toString((char)Magic.no_Magic(peekchr()))));
      }
      return null;
    }

    return ret;
  }

  /*
     * regatom - the lowest level
     *
     * Optimization:  gobbles an entire sequence of ordinary characters so that
     * it can turn them into a single node, which is smaller to store and
     * faster to run.  Don't do this when one_exactly is set.
     */
  @Nullable
  private CharPointer regatom(@NotNull Flags flagp) {
    CharPointer ret = null;
    Flags flags = new Flags();
    boolean cpo_lit = false;        /* 'cpoptions' contains 'l' flag */
    int c;
    String classchars = ".iIkKfFpPsSdDxXoOwWhHaAlLuU";
    int[] classcodes = {ANY, IDENT, SIDENT, KWORD, SKWORD,
      FNAME, SFNAME, PRINT, SPRINT,
      WHITE, NWHITE, DIGIT, NDIGIT,
      HEX, NHEX, OCTAL, NOCTAL,
      WORD, NWORD, HEAD, NHEAD,
      ALPHA, NALPHA, LOWER, NLOWER,
      UPPER, NUPPER
    };
    CharPointer p;
    int extra = 0;

    flagp.init(WORST);             /* Tentatively. */

    c = getchr();
    boolean doCollection = false;
    boolean doDefault = false;
    switch (c) {
      case Magic.HAT:
        ret = regnode(BOL);
        break;

      case Magic.DOLLAR:
        ret = regnode(EOL);
        had_eol = true;
        break;

      case Magic.LESS:
        ret = regnode(BOW);
        break;

      case Magic.GREATER:
        ret = regnode(EOW);
        break;

      case Magic.UNDER:
        c = Magic.no_Magic(getchr());
        if (c == '^')           /* "\_^" is start-of-line */ {
          ret = regnode(BOL);
          break;
        }
        if (c == '$')           /* "\_$" is end-of-line */ {
          ret = regnode(EOL);
          had_eol = true;
          break;
        }

        extra = ADD_NL;
        flagp.set(HASNL);

        /* "\_[" is character range plus newline */
        if (c == '[') {
          //goto collection;
          doCollection = true;
        }

        /* "\_x" is character class plus newline */
        /*FALLTHROUGH*/

        /*
                 * Character classes.
                 */
      case Magic.DOT:
      case Magic.i:
      case Magic.I:
      case Magic.k:
      case Magic.K:
      case Magic.f:
      case Magic.F:
      case Magic.p:
      case Magic.P:
      case Magic.s:
      case Magic.S:
      case Magic.d:
      case Magic.D:
      case Magic.x:
      case Magic.X:
      case Magic.o:
      case Magic.O:
      case Magic.w:
      case Magic.W:
      case Magic.h:
      case Magic.H:
      case Magic.a:
      case Magic.A:
      case Magic.l:
      case Magic.L:
      case Magic.u:
      case Magic.U:
        int i = classchars.indexOf(Magic.no_Magic(c));
        if (i == -1) {
          VimPlugin.showMessage(MessageHelper.message(Msg.E63));
          return null;
        }
        ret = regnode(classcodes[i] + extra);
        flagp.set(HASWIDTH | SIMPLE);
        break;

      case Magic.n:
        ret = regnode(NEWL);
        flagp.set(HASWIDTH | HASNL);
        break;

      case Magic.LPAREN:
        if (one_exactly) {
          EMSG_ONE_RET_null();
          return null;
        }
        ret = reg(REG_PAREN, flags);
        if (ret == null) {
          return null;
        }
        flagp.set(flags.get() & (HASWIDTH | SPSTART | HASNL));
        break;

      case '\u0000':
      case Magic.PIPE:
      case Magic.AMP:
      case Magic.RPAREN:
        EMSG_RET_null(Msg.e_internal);          /* Supposed to be caught earlier. */
        return null;
      /* NOTREACHED */

      case Magic.EQUAL:
      case Magic.QUESTION:
      case Magic.PLUS:
      case Magic.AT:
      case Magic.LCURLY:
      case Magic.STAR:
        c = Magic.no_Magic(c);
        String val = (c == '*' ? reg_magic >= MAGIC_ON : reg_magic == MAGIC_ALL) ? "" : "\\";
        VimPlugin.showMessage(MessageHelper.message(Msg.E64, val, Character.toString((char)c)));
        return null;
      /* NOTREACHED */

      case Magic.TILDE:          /* previous substitute pattern */
        if (reg_prev_sub != null) {
          CharPointer lp;

          ret = regnode(EXACTLY);
          lp = reg_prev_sub.ref(0);
          while (!lp.isNul()) {
            regc(lp.charAt());
          }
          lp.inc();
          regc('\u0000');
          if (!reg_prev_sub.isNul()) {
            flagp.set(HASWIDTH);
            if ((lp.pointer() - reg_prev_sub.pointer()) == 1) {
              flagp.set(SIMPLE);
            }
          }
        }
        else {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_nopresub));
          return null;
        }
        break;

      case Magic.N1:
      case Magic.N2:
      case Magic.N3:
      case Magic.N4:
      case Magic.N5:
      case Magic.N6:
      case Magic.N7:
      case Magic.N8:
      case Magic.N9: {
        int refnum;

        refnum = c - Magic.N0;
        /*
                     * Check if the back reference is legal. We must have seen the
                     * close brace.
                     * TODO: Should also check that we don't refer to something
                     * that is repeated (+*=): what instance of the repetition
                     * should we match?
                     */
        if (!had_endbrace[refnum]) {
          /* Trick: check if "@<=" or "@<!" follows, in which case
                         * the \1 can appear before the referenced match. */
          for (p = regparse.ref(0); !p.isNul(); p.inc()) {
            if (p.charAt(0) == '@' && p.charAt(1) == '<' && (p.charAt(2) == '!' || p.charAt(2) == '=')) {
              break;
            }
          }
          if (p.isNul()) {
            EMSG_RET_null(Msg.E65);
            return null;
          }
        }
        ret = regnode(BACKREF + refnum);
      }
      break;
      case Magic.z: {
        c = Magic.no_Magic(getchr());
        switch (c) {
          case '(':
            if (reg_do_extmatch != REX_SET) {
              VimPlugin.showMessage(MessageHelper.message(Msg.E66));
              return null;
            }
            if (one_exactly) {
              EMSG_ONE_RET_null();
              return null;
            }
            ret = reg(REG_ZPAREN, flags);
            if (ret == null) {
              return null;
            }
            flagp.set(flags.get() & (HASWIDTH | SPSTART | HASNL));
            re_has_z = REX_SET;
            break;

          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            if (reg_do_extmatch != REX_USE) {
              VimPlugin.showMessage(MessageHelper.message(Msg.E67));
              return null;
            }
            ret = regnode(ZREF + c - '0');
            re_has_z = REX_USE;
            break;

          case 's':
            ret = regnode(MOPEN);
            break;

          case 'e':
            ret = regnode(MCLOSE);
            break;

          default:
            VimPlugin.showMessage(MessageHelper.message(Msg.E68));
            return null;
        }
      }
      break;

      case Magic.PERCENT: {
        c = Magic.no_Magic(getchr());
        switch (c) {
          /* () without a back reference */
          case '(':
            if (one_exactly) {
              EMSG_ONE_RET_null();
              return null;
            }
            ret = reg(REG_NPAREN, flags);
            if (ret == null) {
              return null;
            }
            flagp.set(flags.get() & (HASWIDTH | SPSTART | HASNL));
            break;

          /* Catch \%^ and \%$ regardless of where they appear in the
                             * pattern -- regardless of whether or not it makes sense. */
          case '^':
            ret = regnode(RE_BOF);
            break;

          case '$':
            ret = regnode(RE_EOF);
            break;

          case '#':
            ret = regnode(CURSOR);
            break;

          /* \%[abc]: Emit as a list of branches, all ending at the last
                             * branch which matches nothing. */
          case '[':
            if (one_exactly)      /* doesn't nest */ {
              EMSG_ONE_RET_null();
              return null;
            }
            else {
              CharPointer lastbranch;
              CharPointer lastnode = null;
              CharPointer br;

              ret = null;
              while ((c = getchr()) != ']') {
                if (c == '\u0000') {
                  EMSG_M_RET_null(Msg.E69, reg_magic == MAGIC_ALL);
                  return null;
                }
                br = regnode(BRANCH);
                if (ret == null) {
                  ret = br.ref(0);
                }
                else {
                  regtail(lastnode, br);
                }

                ungetchr();
                one_exactly = true;
                lastnode = regatom(flagp);
                one_exactly = false;
                if (lastnode == null) {
                  return null;
                }
              }
              if (ret == null) {
                EMSG_M_RET_null(Msg.E70, reg_magic == MAGIC_ALL);
                return null;
              }
              lastbranch = regnode(BRANCH);
              br = regnode(NOTHING);
              regtail(lastnode, br);
              regtail(lastbranch, br);
              /* connect all branches to the NOTHING
                                 * branch at the end */
              for (br = ret.ref(0); br != lastnode; ) {
                if (br.OP() == BRANCH) {
                  regtail(br, lastbranch);
                  br = br.OPERAND();
                }
                else {
                  br = regnext(br);
                }
              }
              flagp.unset(HASWIDTH);
              break;
            }

          default:
            if (Character.isDigit((char)c) || c == '<' || c == '>') {
              int n = 0;
              int cmp;

              cmp = c;
              if (cmp == '<' || cmp == '>') {
                c = getchr();
              }
              while (Character.isDigit((char)c)) {
                n = n * 10 + (c - '0');
                c = getchr();
              }
              if (c == 'l' || c == 'c' || c == 'v') {
                if (c == 'l') {
                  ret = regnode(RE_LNUM);
                }
                else if (c == 'c') {
                  ret = regnode(RE_COL);
                }
                else {
                  ret = regnode(RE_VCOL);
                }

                /* put the number and the optional
                                     * comparator after the opcode */
                regcode = re_put_long(regcode.ref(0), n);
                regcode.set((char)cmp).inc();
                break;
              }
            }

            EMSG_M_RET_null(Msg.E71, reg_magic == MAGIC_ALL);
            return null;
        }
      }
      break;
      case Magic.LBRACE:
        doCollection = true;
        break;
      default:
        doDefault = true;
        break;
    }

    if (doCollection) {
      CharPointer lp;

      /*
            * If there is no matching ']', we assume the '[' is a normal
            * character.  This makes 'incsearch' and ":help [" work.
            */
      lp = skip_anyof(regparse.ref(0));
      if (lp.charAt() == ']')     /* there is a matching ']' */ {
        int startc = -1;    /* > 0 when next '-' is a range */
        int endc;

        /*
                * In a character class, different parsing rules apply.
                * Not even \ is special anymore, nothing is.
                */
        if (regparse.charAt() == '^')       /* Complement of range. */ {
          ret = regnode(ANYBUT + extra);
          regparse.inc();
        }
        else {
          ret = regnode(ANYOF + extra);
        }

        /* At the start ']' and '-' mean the literal character. */
        if (regparse.charAt() == ']' || regparse.charAt() == '-') {
          regc(regparse.charAt());
          regparse.inc();
        }

        while (!regparse.isNul() && regparse.charAt() != ']') {
          if (regparse.charAt() == '-') {
            regparse.inc();
            /* The '-' is not used for a range at the end and
                        * after or before a '\n'. */
            if (regparse.isNul() || regparse.charAt() == ']' || startc == -1 ||
                (regparse.charAt(0) == '\\' && regparse.charAt(1) == 'n')) {
              regc('-');
              startc = '-';       /* [--x] is a range */
            }
            else {
              //endc = *regparse++;
              endc = regparse.charAt();
              regparse.inc();
              if (startc > endc) {
                VimPlugin.showMessage(MessageHelper.message(Msg.e_invrange));
                return null;
              }
              while (++startc <= endc) {
                regc(startc);
              }
              startc = -1;
            }
          }
          /*
                    * Only "\]", "\^", "\]" and "\\" are special in Vi.  Vim
                    * accepts "\t", "\e", etc., but only when the 'l' flag in
                    * 'cpoptions' is not included.
                    */
          else if (regparse.charAt() == '\\' &&
                   (REGEXP_INRANGE.indexOf(regparse.charAt(1)) != -1 || (!cpo_lit &&
                                                                         REGEXP_ABBR.indexOf(regparse.charAt(1)) != -1))) {
            regparse.inc();
            if (regparse.charAt() == 'n') {
              /* '\n' in range: also match NL */
              if (ret.charAt() == ANYBUT) {
                ret.set((char)(ANYBUT + ADD_NL));
              }
              else if (ret.charAt() == ANYOF) {
                ret.set((char)(ANYOF + ADD_NL));
              }
              /* else: must have had a \n already */
              flagp.set(HASNL);
              regparse.inc();
              startc = -1;
            }
            else {
              startc = backslash_trans(regparse.charAt());
              regparse.inc();
              regc(startc);
            }
          }
          else if (regparse.charAt() == '[') {
            int c_class;
            int cu;

            c_class = skip_class_name(regparse);
            startc = -1;
            /* Characters assumed to be 8 bits! */
            switch (c_class) {
              case CharacterClasses.CLASS_NONE:
                /* literal '[', allow [[-x] as a range */
                startc = regparse.charAt();
                regparse.inc();
                regc(startc);
                break;
              case CharacterClasses.CLASS_ALNUM:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isLetterOrDigit((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_ALPHA:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isLetter((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_BLANK:
                regc(' ');
                regc('\t');
                break;
              case CharacterClasses.CLASS_CNTRL:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isISOControl((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_DIGIT:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isDigit((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_GRAPH:
                for (cu = 1; cu <= 255; cu++) {
                  if (CharacterClasses.isGraph((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_LOWER:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isLowerCase((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_PRINT:
                for (cu = 1; cu <= 255; cu++) {
                  if (CharacterClasses.isPrint((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_PUNCT:
                for (cu = 1; cu <= 255; cu++) {
                  if (CharacterClasses.isPunct((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_SPACE:
                for (cu = 9; cu <= 13; cu++) {
                  regc(cu);
                }
                regc(' ');
                break;
              case CharacterClasses.CLASS_UPPER:
                for (cu = 1; cu <= 255; cu++) {
                  if (Character.isUpperCase((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_XDIGIT:
                for (cu = 1; cu <= 255; cu++) {
                  if (CharacterClasses.isHex((char)cu)) {
                    regc(cu);
                  }
                }
                break;
              case CharacterClasses.CLASS_TAB:
                regc('\t');
                break;
              case CharacterClasses.CLASS_RETURN:
                regc('\r');
                break;
              case CharacterClasses.CLASS_BACKSPACE:
                regc('\b');
                break;
              case CharacterClasses.CLASS_ESCAPE:
                regc('\033');
                break;
            }
          }
          else {
            startc = regparse.charAt();
            regparse.inc();
            regc(startc);
          }
        }
        regc('\u0000');
        prevchr_len = 1;        /* last char was the ']' */
        if (regparse.charAt() != ']') {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_toomsbra));
          return null;
        }
        skipchr();          /* let's be friends with the lexer again */
        flagp.set(HASWIDTH | SIMPLE);
      }
      else {
        doDefault = true;
      }
    }
    /* FALLTHROUGH */

    if (doDefault) {
      int len;

      ret = regnode(EXACTLY);

      /*
            * Append characters as long as:
            * - there is no following multi, we then need the character in
            *   front of it as a single character operand
            * - not running into a Magic character
            * - "one_exactly" is not set
            * But always emit at least one character.  Might be a Multi,
            * e.g., a "[" without matching "]".
            */
      for (len = 0; c != '\u0000' && (len == 0 || (re_multi_type(peekchr()) == NOT_MULTI &&
                                                   !one_exactly && !Magic.is_Magic(c))); ++len) {
        c = Magic.no_Magic(c);
        regc(c);
        c = getchr();
      }
      ungetchr();

      regc('\u0000');
      flagp.set(HASWIDTH);
      if (len == 1) {
        flagp.set(SIMPLE);
      }
    }

    return ret;
  }

  /*
     * emit a node
     * Return pointer to generated code.
     */
  private CharPointer regnode(int op) {
    CharPointer ret;

    ret = regcode.ref(0);
    regcode.set((char)op).inc();
    regcode.set('\u0000').inc();               /* Null "next" pointer. */
    regcode.set('\u0000').inc();
    return ret;
  }

  /*
     * Emit (if appropriate) a byte of code
     */
  private void regc(int b) {
    regcode.set((char)b).inc();
  }

  /*
     * reginsert - insert an operator in front of already-emitted operand
     *
     * Means relocating the operand.
     */
  private void reginsert(int op, @NotNull CharPointer opnd) {
    CharPointer src;
    CharPointer dst;
    CharPointer place;

    src = regcode.ref(0);
    regcode.inc(3);
    dst = regcode.ref(0);
    while (src.pointer() > opnd.pointer()) {
      //*--dst = *--src;
      dst.dec().set(src.dec().charAt());
    }

    place = opnd.ref(0);               /* Op node, where operand used to be. */
    place.set((char)op).inc();
    place.set('\u0000').inc();
    place.set('\u0000');
  }

  /*
     * reginsert_limits - insert an operator in front of already-emitted operand.
     * The operator has the given limit values as operands.  Also set next pointer.
     *
     * Means relocating the operand.
     */
  private void reginsert_limits(int op, int minval, int maxval, @NotNull CharPointer opnd) {
    CharPointer src;
    CharPointer dst;
    CharPointer place;

    src = regcode.ref(0);
    regcode.inc(11);
    dst = regcode.ref(0);
    while (src.pointer() > opnd.pointer()) {
      //*--dst = *--src;
      dst.dec().set(src.dec().charAt());
    }

    place = opnd.ref(0);               /* Op node, where operand used to be. */
    place.set((char)op).inc();
    place.set('\u0000').inc();
    place.set('\u0000').inc();
    place = re_put_long(place.ref(0), minval);
    place = re_put_long(place.ref(0), maxval);
    regtail(opnd, place);
  }

  /*
     * Write a long as four bytes at "p" and return pointer to the next char.
     */
  @NotNull
  private CharPointer re_put_long(@NotNull CharPointer p, int val) {
    p.set((char)((val >> 24) & 0xff)).inc();
    p.set((char)((val >> 16) & 0xff)).inc();
    p.set((char)((val >> 8) & 0xff)).inc();
    p.set((char)(val & 0xff)).inc();
    return p;
  }

  /*
     * regtail - set the next-pointer at the end of a node chain
     */
  private void regtail(@NotNull CharPointer p, @NotNull CharPointer val) {
    CharPointer scan;
    int offset;

    /* Find last node. */
    scan = p.ref(0);
    for (; ; ) {
      CharPointer temp = regnext(scan);
      if (temp == null) {
        break;
      }
      scan = temp;
    }

    if (scan.OP() == BACK) {
      offset = scan.pointer() - val.pointer();
    }
    else {
      offset = val.pointer() - scan.pointer();
    }

    scan.ref(1).set((char)(((char)offset >> 8) & 0xff));
    scan.ref(2).set((char)(offset & 0xff));
  }

  /*
     * regoptail - regtail on item after a BRANCH; nop if none
     */
  private void regoptail(@Nullable CharPointer p, @NotNull CharPointer val) {
    /* When op is neither BRANCH nor BRACE_COMPLEX0-9, it is "operandless" */
    if (p == null || (p.OP() != BRANCH && (p.OP() < BRACE_COMPLEX || p.OP() > BRACE_COMPLEX + 9))) {
      return;
    }

    regtail(p.OPERAND(), val);
  }

  private void initchr(@NotNull String str) {
    regparse = new CharPointer(str);
    prevchr_len = 0;
    curchr = prevprevchr = prevchr = nextchr = -1;
    at_start = true;
    prev_at_start = false;
  }

  private int peekchr() {
    if (curchr == -1) {
      switch (curchr = regparse.charAt(0)) {
        case '.':
        case '[':
        case '~':
          /* magic when 'magic' is on */
          if (reg_magic >= MAGIC_ON) {
            curchr = Magic.Magic(curchr);
          }
          break;
        case '(':
        case ')':
        case '{':
        case '%':
        case '+':
        case '=':
        case '?':
        case '@':
        case '!':
        case '&':
        case '|':
        case '<':
        case '>':
        case '#':       /* future ext. */
        case '"':       /* future ext. */
        case '\'':      /* future ext. */
        case ',':       /* future ext. */
        case '-':       /* future ext. */
        case ':':       /* future ext. */
        case ';':       /* future ext. */
        case '`':       /* future ext. */
        case '/':       /* Can't be used in / command */
          /* magic only after "\v" */
          if (reg_magic == MAGIC_ALL) {
            curchr = Magic.Magic(curchr);
          }
          break;
        case '*':
          /* * is not magic as the very first character, eg "?*ptr" and when
                     * after '^', eg "/^*ptr" */
          if (reg_magic >= MAGIC_ON && !at_start && !(prev_at_start && prevchr == Magic.HAT)) {
            curchr = Magic.STAR;
          }
          break;
        case '^':
          /* '^' is only magic as the very first character and if it's after
                     * "\(", "\|", "\&' or "\n" */
          if (reg_magic >= MAGIC_OFF &&
              (at_start || reg_magic == MAGIC_ALL || prevchr == Magic.LPAREN || prevchr == Magic.PIPE ||
               prevchr == Magic.AMP || prevchr == Magic.n || (Magic.no_Magic(prevchr) == '(' &&
                                                              prevprevchr == Magic.PERCENT))) {
            curchr = Magic.HAT;
            at_start = true;
            prev_at_start = false;
          }
          break;
        case '$':
          /* '$' is only magic as the very last char and if it's in front of
                     * either "\|", "\)", "\&", or "\n" */
          if (reg_magic >= MAGIC_OFF) {
            CharPointer p = regparse.ref(1);

            /* ignore \c \C \m and \M after '$' */
            while (p.charAt(0) == '\\' && (p.charAt(1) == 'c' || p.charAt(1) == 'C' ||
                                           p.charAt(1) == 'm' || p.charAt(1) == 'M')) {
              p.inc(2);
            }
            if (p.charAt(0) == '\u0000' || (p.charAt(0) == '\\' &&
                                            (p.charAt(1) == '|' || p.charAt(1) == '&' || p.charAt(1) == ')' ||
                                             p.charAt(1) == 'n')) || reg_magic == MAGIC_ALL) {
              curchr = Magic.DOLLAR;
            }
          }
          break;
        case '\\': {
          int c = regparse.charAt(1);

          if (c == '\u0000') {
            curchr = '\\';      /* trailing '\' */
          }
          else if (c <= '~' && META_flags[c] != 0) {
            /*
                             * META contains everything that may be magic sometimes,
                             * except ^ and $ ("\^" and "\$" are only magic after
                             * "\v").  We now fetch the next character and toggle its
                             * magicness.  Therefore, \ is so meta-magic that it is
                             * not in META.
                             */
            curchr = -1;
            prev_at_start = at_start;
            at_start = false;   /* be able to say "/\*ptr" */
            regparse.inc();
            peekchr();
            regparse.dec();
            curchr = Magic.toggle_Magic(curchr);
          }
          else if (REGEXP_ABBR.indexOf(c) != -1) {
            /*
                             * Handle abbreviations, like "\t" for TAB -- webb
                             */
            curchr = backslash_trans(c);
          }
          else if (reg_magic == MAGIC_NONE && (c == '$' || c == '^')) {
            curchr = Magic.toggle_Magic(c);
          }
          else {
            /*
                             * Next character can never be (made) magic?
                             * Then backslashing it won't do anything.
                             */
            curchr = c;
          }
          break;
        }
      }
    }

    return curchr;
  }

  /*
     * Eat one lexed character.  Do this in a way that we can undo it.
     */
  private void skipchr() {
    /* peekchr() eats a backslash, do the same here */
    if (regparse.charAt() == '\\') {
      prevchr_len = 1;
    }
    else {
      prevchr_len = 0;
    }
    if (regparse.charAt(prevchr_len) != '\u0000') {
      ++prevchr_len;
    }
    regparse.inc(prevchr_len);
    prev_at_start = at_start;
    at_start = false;
    prevprevchr = prevchr;
    prevchr = curchr;
    curchr = nextchr;       /* use previously unget char, or -1 */
    nextchr = -1;
  }

  /*
     * Skip a character while keeping the value of prev_at_start for at_start.
     * prevchr and prevprevchr are also kept.
     */
  private void skipchr_keepstart() {
    boolean as = prev_at_start;
    int pr = prevchr;
    int prpr = prevprevchr;

    skipchr();
    at_start = as;
    prevchr = pr;
    prevprevchr = prpr;
  }

  private int getchr() {
    int chr = peekchr();

    skipchr();
    return chr;
  }

  /*
     * put character back.  Works only once!
     */
  private void ungetchr() {
    nextchr = curchr;
    curchr = prevchr;
    prevchr = prevprevchr;
    at_start = prev_at_start;
    prev_at_start = false;

    /* Backup regparse, so that it's at the same position as before the
         * getchr(). */
    regparse.dec(prevchr_len);
  }

  /*
     * read_limits - Read two integers to be taken as a minimum and maximum.
     * If the first character is '-', then the range is reversed.
     * Should end with 'end'.  If minval is missing, zero is default, if maxval is
     * missing, a very big number is the default.
     */
  @Nullable
  private MinMax read_limits() {
    boolean reverse = false;
    CharPointer first_char;
    int minval;
    int maxval;

    if (regparse.charAt() == '-') {
      /* Starts with '-', so reverse the range later */
      regparse.inc();
      reverse = true;
    }

    first_char = regparse.ref(0);
    minval = getdigits(regparse);
    if (regparse.charAt() == ',')           /* There is a comma */ {
      if (Character.isDigit(regparse.inc().charAt())) {
        maxval = getdigits(regparse);
      }
      else {
        maxval = MAX_LIMIT;
      }
    }
    else if (Character.isDigit(first_char.charAt())) {
      maxval = minval;          /* It was \{n} or \{-n} */
    }
    else {
      maxval = MAX_LIMIT;        /* It was \{} or \{-} */
    }

    if (regparse.charAt() == '\\') {
      regparse.inc();     /* Allow either \{...} or \{...\} */
    }
    if (regparse.charAt() != '}' || (maxval == 0 && minval == 0)) {
      String val = reg_magic == MAGIC_ALL ? "" : "\\";
      VimPlugin.showMessage(MessageHelper.message(Msg.synerror, val));
      return null;
    }

    /*
         * Reverse the range if there was a '-', or make sure it is in the right
         * order otherwise.
         */
    if ((!reverse && minval > maxval) || (reverse && minval < maxval)) {
      int tmp = minval;
      minval = maxval;
      maxval = tmp;
    }
    skipchr();          /* let's be friends with the lexer again */

    MinMax res = new MinMax();
    res.maxvalue = maxval;
    res.minvalue = minval;

    return res;
  }

  private int getdigits(@NotNull CharPointer p) {
    int res = 0;
    boolean neg = false;

    if (p.charAt() == '-') {
      neg = true;
      p.inc();
    }
    while (Character.isDigit(p.charAt())) {
      res = res * 10 + Character.digit(p.charAt(), 10);
      p.inc();
    }
    if (neg) {
      res = -res;
    }

    return res;
  }

  /*
     * vim_regexec and friends
     */

  /*
     * Get pointer to the line "lnum", which is relative to "reg_firstlnum".
     */
  @Nullable
  private CharPointer reg_getline(int lnum) {
    /* when looking behind for a match/no-match lnum is negative.  But we
         * can't go before line 1 */
    if (reg_firstlnum + lnum < 0) {
      return null;
    }

    //return ml_get_buf(reg_buf, reg_firstlnum + lnum, false);
    return new CharPointer(EditorHelper.getLineBuffer(reg_buf, reg_firstlnum + lnum));
  }

  /*
     * Match a regexp against a string.
     * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     *
     * Return true if there is a match, false if not.
     */
  public boolean vim_regexec(@NotNull regmatch_T rmp, CharPointer line, int col) {
    reg_match = rmp;
    reg_mmatch = null;
    reg_maxline = 0;
    //reg_win = null;
    ireg_ic = rmp.rm_ic;

    return (vim_regexec_both(line, col) != 0);
  }

  /*
     * Match a regexp against multiple lines.
     * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     *
     * Return zero if there is no match.  Return number of lines contained in the
     * match otherwise.
     */
  public int vim_regexec_multi(@NotNull regmmatch_T rmp, /*win_T win,*/ Editor buf, int lcount, int lnum, int col)

  /* window in which to search or null */
  /* buffer in which to search */
  /* nr of line to start looking for match */
  /* column to start looking for match */ {
    int r;
    //Editor save_curbuf = curbuf;

    reg_match = null;
    reg_mmatch = rmp;
    reg_buf = buf;
    //reg_win = win;
    reg_firstlnum = lnum;
    reg_maxline = lcount - lnum;
    ireg_ic = rmp.rmm_ic;

    /* Need to switch to buffer "buf" to make vim_iswordc() work. */
    //curbuf = buf;
    r = vim_regexec_both(null, col);
    //curbuf = save_curbuf;

    return r;
  }

  /*
     * Match a regexp against a string ("line" points to the string) or multiple
     * lines ("line" is null, use reg_getline()).
     */
  private int vim_regexec_both(CharPointer line, int col)
  /* column to start looking for match */ {
    regprog_T prog;
    CharPointer s;
    int retval;
    reg_tofree = null;
    retval = 0;

    if (reg_match == null) {
      prog = reg_mmatch.regprog;
      line = reg_getline(0);
      reg_startpos = reg_mmatch.startpos;
      reg_endpos = reg_mmatch.endpos;
    }
    else {
      prog = reg_match.regprog;
      reg_startp = reg_match.startp;
      reg_endp = reg_match.endp;
    }

    /* Be paranoid... */
    if (prog == null || line == null) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_null));
      return retval;
    }

    /* Check validity of program. */
    if (prog_magic_wrong()) {
      return retval;
    }

    /* If pattern contains "\c" or "\C": overrule value of ireg_ic */
    if ((prog.regflags & RF_ICASE) != 0) {
      ireg_ic = true;
    }
    else if ((prog.regflags & RF_NOICASE) != 0) {
      ireg_ic = false;
    }

    /* If there is a "must appear" string, look for it. */
    if (prog.regmust != null) {
      char c;

      c = prog.regmust.charAt();
      s = line.ref(col);
      while ((s = cstrchr(s, c)) != null) {
        if (cstrncmp(s, prog.regmust, prog.regmlen) == 0) {
          break;          /* Found it. */
        }
        s.inc();
      }
      if (s == null)          /* Not present. */ {
        // goto theend;
        return retval;
      }
    }

    regline = line.ref(0);
    reglnum = 0;
    out_of_stack = false;

    /* Simplest case: Anchored match need be tried only once. */
    if (prog.reganch != 0) {
      char c;

      c = regline.charAt(col);
      if (prog.regstart == '\u0000' || prog.regstart == c ||
          (ireg_ic && Character.toLowerCase(prog.regstart) == Character.toLowerCase(c))) {
        retval = regtry(prog, col);
      }
      else {
        retval = 0;
      }
    }
    else {
      /* Messy cases:  unanchored match. */
      while (!got_int && !out_of_stack) {
        if (prog.regstart != '\u0000') {
          /* Skip until the char we know it must start with. */
          s = cstrchr(regline.ref(col), prog.regstart);
          if (s == null) {
            retval = 0;
            break;
          }
          col = s.pointer() - regline.pointer();
        }

        retval = regtry(prog, col);
        if (retval > 0) {
          break;
        }

        /* if not currently on the first line, get it again */
        if (reglnum != 0) {
          regline = reg_getline(0);
          reglnum = 0;
        }
        if (regline.charAt(col) == '\u0000') {
          break;
        }
        ++col;
      }
    }

    if (out_of_stack) {
      VimPlugin.showMessage(MessageHelper.message(Msg.E363));
    }

    /* Didn't find a match. */
    //vim_free(reg_tofree);
    return retval;
  }

  private static class reg_extmatch_T {
    @NotNull String[] matches = new String[NSUBEXP];
  }

  /*
     * Create a new extmatch and mark it as referenced once.
     */
  @NotNull
  private reg_extmatch_T make_extmatch() {
    return new reg_extmatch_T();
  }

  /*
     * Add a reference to an extmatch.
     */
  /*
    private reg_extmatch_T ref_extmatch(reg_extmatch_T em)
    {
        return em;
    }
    */

  /*
     * Remove a reference to an extmatch.  If there are no references left, free
     * the info.
     */
  /*
    private void unref_extmatch(reg_extmatch_T em)
    {
    }
    */

  /*
     * regtry - try match of "prog" with at regline["col"].
     * Returns 0 for failure, number of lines contained in the match otherwise.
     */
  private int regtry(@NotNull regprog_T prog, int col) {
    reginput = regline.ref(col);
    need_clear_subexpr = true;
    /* Clear the external match subpointers if necessary. */
    if (prog.reghasz == REX_SET) {
      need_clear_zsubexpr = true;
    }

    if (regmatch((new CharPointer(prog.program)).ref(1))) {
      cleanup_subexpr();
      if (reg_match == null) {
        if (reg_startpos[0].lnum < 0) {
          reg_startpos[0].lnum = 0;
          reg_startpos[0].col = col;
        }
        if (reg_endpos[0].lnum < 0) {
          reg_endpos[0].lnum = reglnum;
          reg_endpos[0].col = reginput.pointer() - regline.pointer();
        }
      }
      else {
        if (reg_startp[0] == null) {
          reg_startp[0] = regline.ref(col);
        }
        if (reg_endp[0] == null) {
          reg_endp[0] = reginput;
        }
      }
      /* Package any found \z(...\) matches for export. Default is none. */
      //unref_extmatch(re_extmatch_out);
      re_extmatch_out = null;

      if (prog.reghasz == REX_SET) {
        int i;

        cleanup_zsubexpr();
        re_extmatch_out = make_extmatch();
        for (i = 0; i < NSUBEXP; i++) {
          if (reg_match == null) {
            /* Only accept single line matches. */
            if (reg_startzpos[i].lnum >= 0 && reg_endzpos[i].lnum == reg_startzpos[i].lnum) {
              re_extmatch_out.matches[i] =
                reg_getline(reg_startzpos[i].lnum).ref(reg_startzpos[i].col).substring(
                  reg_endzpos[i].col - reg_startzpos[i].col);
            }
          }
          else {
            if (reg_startzp[i] != null && reg_endzp[i] != null) {
              re_extmatch_out.matches[i] =
                reg_startzp[i].substring(reg_endzp[i].pointer() - reg_startzp[i].pointer());
            }
          }
        }
      }
      return 1 + reglnum;
    }
    return 0;
  }

  /*
     * regmatch - main matching routine
     *
     * Conceptually the strategy is simple: Check to see whether the current
     * node matches, call self recursively to see whether the rest matches,
     * and then act accordingly.  In practice we make some effort to avoid
     * recursion, in particular by going through "ordinary" nodes (that don't
     * need to know whether the rest of the match failed) by a loop instead of
     * by recursion.
     *
     * Returns true when there is a match.  Leaves reginput and reglnum just after
     * the last matched character.
     * Returns false when there is no match.  Leaves reginput and reglnum in an
     * undefined state!
     */
  private boolean regmatch(@Nullable CharPointer scan) {
    CharPointer next;          /* Next node. */
    int op;
    char c;

    /* Some patterns my cause a long time to match, even though they are not
         * illegal.  E.g., "\([a-z]\+\)\+Q".  Allow breaking them with CTRL-C. */
    //fast_breakcheck(); - TODO

    while (scan != null) {
      if (got_int || out_of_stack) {
        return false;
      }
      next = regnext(scan);

      op = scan.OP();
      /* Check for character class with NL added. */
      if (WITH_NL(op) && reginput.isNul() && reglnum < reg_maxline) {
        reg_nextline();
      }
      else {
        if (WITH_NL(op)) {
          op -= ADD_NL;
        }
        c = reginput.charAt();
        switch (op) {
          case BOL:
            if (!reginput.equals(regline)) {
              return false;
            }
            break;

          case EOL:
            if (c != '\u0000') {
              return false;
            }
            break;

          case RE_BOF:
            /* Passing -1 to the getline() function provided for the search
                         * should always return null if the current line is the first
                         * line of the file. */
            if (reglnum != 0 || !reginput.equals(regline) || (reg_match == null && reg_getline(-1) != null)) {
              return false;
            }
            break;

          case RE_EOF:
            if (reglnum != reg_maxline || c != '\u0000') {
              return false;
            }
            break;

          case CURSOR:
            /* Check if the buffer is in a window and compare the
                         * reg_win->w_cursor position to the match position. */
            LogicalPosition curpos = reg_buf.getCaretModel().getLogicalPosition();
            if (reglnum + reg_firstlnum != curpos.line ||
                reginput.pointer() - regline.pointer() != curpos.column) {
              return false;
            }
            break;

          case RE_LNUM:
            if (reg_match != null || !re_num_cmp((reglnum + reg_firstlnum), scan)) {
              return false;
            }
            break;

          case RE_COL:
            if (!re_num_cmp((reginput.pointer() - regline.pointer()) + 1, scan)) {
              return false;
            }
            break;

          case RE_VCOL:
            /* TODO
                        if (!re_num_cmp(win_linetabsize(reg_win == null ? curwin : reg_win,
                            regline, (int)(reginput - regline)) + 1, scan))
                        {
                            return false;
                        }
                        */
            break;

          case BOW:     /* \<word; reginput points to w */
            if (c == '\u0000')       /* Can't match at end of line */ {
              return false;
            }
            else {
              if (!CharacterClasses.isWord(c) || (reginput.pointer() > regline.pointer() &&
                                                  CharacterClasses.isWord(reginput.charAt(-1)))) {
                return false;
              }
            }
            break;

          case EOW:     /* word\>; reginput points after d */
            if (reginput.equals(regline))    /* Can't match at start of line */ {
              return false;
            }
            if (!CharacterClasses.isWord(reginput.charAt(-1))) {
              return false;
            }
            if (!reginput.isNul() && CharacterClasses.isWord(c)) {
              return false;
            }
            break; /* Matched with EOW */

          case ANY:
            if (c == '\u0000') {
              return false;
            }
            reginput.inc();
            break;

          case IDENT:
            if (!Character.isJavaIdentifierPart(c)) {
              return false;
            }
            reginput.inc();
            break;

          case SIDENT:
            if (Character.isDigit(reginput.charAt()) || !Character.isJavaIdentifierPart(c)) {
              return false;
            }
            reginput.inc();
            break;

          case KWORD:
            if (!CharacterClasses.isWord(reginput.charAt())) {
              return false;
            }
            reginput.inc();
            break;

          case SKWORD:
            if (Character.isDigit(reginput.charAt()) || !CharacterClasses.isWord(reginput.charAt())) {
              return false;
            }
            reginput.inc();
            break;

          case FNAME:
            if (!CharacterClasses.isFile(c)) {
              return false;
            }
            reginput.inc();
            break;

          case SFNAME:
            if (Character.isDigit(reginput.charAt()) || !CharacterClasses.isFile(c)) {
              return false;
            }
            reginput.inc();
            break;

          case PRINT:
            if (!CharacterClasses.isPrint(reginput.charAt())) {
              return false;
            }
            reginput.inc();
            break;

          case SPRINT:
            if (Character.isDigit(reginput.charAt()) || !CharacterClasses.isPrint(reginput.charAt())) {
              return false;
            }
            reginput.inc();
            break;

          case WHITE:
            if (!CharacterClasses.isWhite(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NWHITE:
            if (c == '\u0000' || CharacterClasses.isWhite(c)) {
              return false;
            }
            reginput.inc();
            break;

          case DIGIT:
            if (!Character.isDigit(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NDIGIT:
            if (c == '\u0000' || Character.isDigit(c)) {
              return false;
            }
            reginput.inc();
            break;

          case HEX:
            if (!CharacterClasses.isHex(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NHEX:
            if (c == '\u0000' || CharacterClasses.isHex(c)) {
              return false;
            }
            reginput.inc();
            break;

          case OCTAL:
            if (!CharacterClasses.isOctal(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NOCTAL:
            if (c == '\u0000' || CharacterClasses.isOctal(c)) {
              return false;
            }
            reginput.inc();
            break;

          case WORD:
            if (!CharacterClasses.isWord(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NWORD:
            if (c == '\u0000' || CharacterClasses.isWord(c)) {
              return false;
            }
            reginput.inc();
            break;

          case HEAD:
            if (!CharacterClasses.isHead(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NHEAD:
            if (c == '\u0000' || CharacterClasses.isHead(c)) {
              return false;
            }
            reginput.inc();
            break;

          case ALPHA:
            if (!CharacterClasses.isAlpha(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NALPHA:
            if (c == '\u0000' || CharacterClasses.isAlpha(c)) {
              return false;
            }
            reginput.inc();
            break;

          case LOWER:
            if (!CharacterClasses.isLower(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NLOWER:
            if (c == '\u0000' || CharacterClasses.isLower(c)) {
              return false;
            }
            reginput.inc();
            break;

          case UPPER:
            if (!CharacterClasses.isUpper(c)) {
              return false;
            }
            reginput.inc();
            break;

          case NUPPER:
            if (c == '\u0000' || CharacterClasses.isUpper(c)) {
              return false;
            }
            reginput.inc();
            break;

          case EXACTLY: {
            int len;
            CharPointer opnd;

            opnd = scan.OPERAND();
            /* Inline the first byte, for speed. */
            if (opnd.charAt() != reginput.charAt() && (!ireg_ic ||
                                                       Character.toLowerCase(opnd.charAt()) != Character.toLowerCase(reginput.charAt()))) {
              return false;
            }
            if (opnd.charAt(1) == '\u0000') {
              reginput.inc();         /* matched a single char */
            }
            else {
              len = opnd.strlen();
              /* Need to match first byte again for multi-byte. */
              if (cstrncmp(opnd, reginput, len) != 0) {
                return false;
              }
              reginput.inc(len);
            }
          }
          break;

          case ANYOF:
          case ANYBUT:
            if (c == '\u0000') {
              return false;
            }
            if ((cstrchr(scan.OPERAND(), c) == null) == (op == ANYOF)) {
              return false;
            }
            reginput.inc();
            break;


          case NOTHING:
            break;

          case BACK:
            break;

          case MOPEN:   /* Match start: \zs */
          case MOPEN + 1:   /* \( */
          case MOPEN + 2:
          case MOPEN + 3:
          case MOPEN + 4:
          case MOPEN + 5:
          case MOPEN + 6:
          case MOPEN + 7:
          case MOPEN + 8:
          case MOPEN + 9: {
            int no;
            save_se_T save = new save_se_T();

            no = op - MOPEN;
            cleanup_subexpr();
            save_se(save, reg_startpos[no], reg_startp[no]);

            if (regmatch(next)) {
              return true;
            }

            restore_se(save, reg_startpos[no], reg_startp[no]);
            return false;
          }
          /* break; Not Reached */

          case NOPEN:       /* \%( */
          case NCLOSE:      /* \) after \%( */
            return regmatch(next);
          /* break; Not Reached */

          case ZOPEN + 1:
          case ZOPEN + 2:
          case ZOPEN + 3:
          case ZOPEN + 4:
          case ZOPEN + 5:
          case ZOPEN + 6:
          case ZOPEN + 7:
          case ZOPEN + 8:
          case ZOPEN + 9: {
            int no;
            save_se_T save = new save_se_T();

            no = op - ZOPEN;
            cleanup_zsubexpr();
            save_se(save, reg_startzpos[no], reg_startzp[no]);

            if (regmatch(next)) {
              return true;
            }

            restore_se(save, reg_startzpos[no], reg_startzp[no]);
            return false;
          }
          /* break; Not Reached */

          case MCLOSE:  /* Match end: \ze */
          case MCLOSE + 1:  /* \) */
          case MCLOSE + 2:
          case MCLOSE + 3:
          case MCLOSE + 4:
          case MCLOSE + 5:
          case MCLOSE + 6:
          case MCLOSE + 7:
          case MCLOSE + 8:
          case MCLOSE + 9: {
            int no;
            save_se_T save = new save_se_T();

            no = op - MCLOSE;
            cleanup_subexpr();
            save_se(save, reg_endpos[no], reg_endp[no]);

            if (regmatch(next)) {
              return true;
            }

            restore_se(save, reg_endpos[no], reg_endp[no]);
            return false;
          }
          /* break; Not Reached */

          case ZCLOSE + 1:  /* \) after \z( */
          case ZCLOSE + 2:
          case ZCLOSE + 3:
          case ZCLOSE + 4:
          case ZCLOSE + 5:
          case ZCLOSE + 6:
          case ZCLOSE + 7:
          case ZCLOSE + 8:
          case ZCLOSE + 9: {
            int no;
            save_se_T save = new save_se_T();

            no = op - ZCLOSE;
            cleanup_zsubexpr();
            save_se(save, reg_endzpos[no], reg_endzp[no]);

            if (regmatch(next)) {
              return true;
            }

            restore_se(save, reg_endzpos[no], reg_endzp[no]);
            return false;
          }
          /* break; Not Reached */

          case BACKREF + 1:
          case BACKREF + 2:
          case BACKREF + 3:
          case BACKREF + 4:
          case BACKREF + 5:
          case BACKREF + 6:
          case BACKREF + 7:
          case BACKREF + 8:
          case BACKREF + 9: {
            int no;
            int len;
            int clnum;
            int ccol;
            CharPointer p;

            no = op - BACKREF;
            cleanup_subexpr();
            if (reg_match != null)         /* Single-line regexp */ {
              if (reg_endp[no] == null) {
                /* Backref was not set: Match an empty string. */
                len = 0;
              }
              else {
                /* Compare current input with back-ref in the same
                                     * line. */
                len = reg_endp[no].pointer() - reg_startp[no].pointer();
                if (cstrncmp(reg_startp[no], reginput, len) != 0) {
                  return false;
                }
              }
            }
            else                            /* Multi-line regexp */ {
              if (reg_endpos[no].lnum < 0) {
                /* Backref was not set: Match an empty string. */
                len = 0;
              }
              else {
                if (reg_startpos[no].lnum == reglnum
                    && reg_endpos[no].lnum == reglnum) {
                  /* Compare back-ref within the current line. */
                  len = reg_endpos[no].col - reg_startpos[no].col;
                  if (cstrncmp(regline.ref(reg_startpos[no].col), reginput, len) != 0) {
                    return false;
                  }
                }
                else {
                  /* Messy situation: Need to compare between two
                                         * lines. */
                  ccol = reg_startpos[no].col;
                  clnum = reg_startpos[no].lnum;
                  for (; ; ) {
                    /* Since getting one line may invalidate
                                             * the other, need to make copy.  Slow! */
                    if (!regline.equals(reg_tofree)) {
                      reg_tofree = regline.ref(0);
                      reginput = reg_tofree.ref(reginput.pointer() - regline.pointer());
                      regline = reg_tofree.ref(0);
                    }

                    /* Get the line to compare with. */
                    p = reg_getline(clnum);
                    if (clnum == reg_endpos[no].lnum) {
                      len = reg_endpos[no].col - ccol;
                    }
                    else {
                      len = p.ref(ccol).strlen();
                    }

                    if (cstrncmp(p.ref(ccol), reginput, len) != 0) {
                      return false;       /* doesn't match */
                    }
                    if (clnum == reg_endpos[no].lnum) {
                      break;              /* match and at end! */
                    }
                    if (reglnum == reg_maxline) {
                      return false;       /* text too short */
                    }

                    /* Advance to next line. */
                    reg_nextline();
                    ++clnum;
                    ccol = 0;
                    if (got_int || out_of_stack) {
                      return false;
                    }
                  }

                  /* found a match!  Note that regline may now point
                                         * to a copy of the line, that should not matter. */
                }
              }
            }

            /* Matched the backref, skip over it. */
            reginput.inc(len);
          }
          break;

          case ZREF + 1:
          case ZREF + 2:
          case ZREF + 3:
          case ZREF + 4:
          case ZREF + 5:
          case ZREF + 6:
          case ZREF + 7:
          case ZREF + 8:
          case ZREF + 9: {
            int no;
            int len;

            cleanup_zsubexpr();
            no = op - ZREF;
            final String match = re_extmatch_in.matches[no];
            if (re_extmatch_in != null && match != null) {
              len = match.length();
              if (cstrncmp(new CharPointer(match), reginput, len) != 0) {
                return false;
              }
              reginput.inc(len);
            }
            else {
              /* Backref was not set: Match an empty string. */
            }
          }
          break;

          case BRANCH: {
            if (next.OP() != BRANCH) /* No choice. */ {
              next = scan.OPERAND();       /* Avoid recursion. */
            }
            else {
              regsave_T save = new regsave_T();

              do {
                reg_save(save);
                if (regmatch(scan.OPERAND())) {
                  return true;
                }
                reg_restore(save);
                scan = regnext(scan);
              }
              while (scan != null && scan.OP() == BRANCH);

              return false;
              /* NOTREACHED */
            }
          }
          break;

          case BRACE_LIMITS: {
            int no;

            if (next.OP() == BRACE_SIMPLE) {
              bl_minval = scan.OPERAND_MIN();
              bl_maxval = scan.OPERAND_MAX();
            }
            else if (next.OP() >= BRACE_COMPLEX
                     && next.OP() < BRACE_COMPLEX + 10) {
              no = next.OP() - BRACE_COMPLEX;
              brace_min[no] = scan.OPERAND_MIN();
              brace_max[no] = scan.OPERAND_MAX();
              brace_count[no] = 0;
            }
            else {
              VimPlugin.showMessage(MessageHelper.message(Msg.e_internal));
              return false;
            }
          }
          break;

          case BRACE_COMPLEX:
          case BRACE_COMPLEX + 1:
          case BRACE_COMPLEX + 2:
          case BRACE_COMPLEX + 3:
          case BRACE_COMPLEX + 4:
          case BRACE_COMPLEX + 5:
          case BRACE_COMPLEX + 6:
          case BRACE_COMPLEX + 7:
          case BRACE_COMPLEX + 8:
          case BRACE_COMPLEX + 9: {
            int no;
            regsave_T save = new regsave_T();

            no = op - BRACE_COMPLEX;
            ++brace_count[no];

            /* If not matched enough times yet, try one more */
            if (brace_count[no] <= (brace_min[no] <= brace_max[no]
                                    ? brace_min[no] : brace_max[no])) {
              reg_save(save);
              if (regmatch(scan.OPERAND())) {
                return true;
              }
              reg_restore(save);
              --brace_count[no];  /* failed, decrement match count */
              return false;
            }

            /* If matched enough times, may try matching some more */
            if (brace_min[no] <= brace_max[no]) {
              /* Range is the normal way around, use longest match */
              if (brace_count[no] <= brace_max[no]) {
                reg_save(save);
                if (regmatch(scan.OPERAND())) {
                  return true;        /* matched some more times */
                }
                reg_restore(save);
                --brace_count[no];  /* matched just enough times */
                /* continue with the items after \{} */
              }
            }
            else {
              /* Range is backwards, use shortest match first */
              if (brace_count[no] <= brace_min[no]) {
                reg_save(save);
                if (regmatch(next)) {
                  return true;
                }
                reg_restore(save);
                next = scan.OPERAND();
                /* must try to match one more item */
              }
            }
          }
          break;

          case BRACE_SIMPLE:
          case STAR:
          case PLUS: {
            char nextb;          /* next byte */
            char nextb_ic;       /* next byte reverse case */
            int count;
            regsave_T save = new regsave_T();
            int minval;
            int maxval;

            /*
                             * Lookahead to avoid useless match attempts when we know
                             * what character comes next.
                             */
            if (next.OP() == EXACTLY) {
              nextb = next.OPERAND().charAt();
              if (ireg_ic) {
                if (Character.isUpperCase(nextb)) {
                  nextb_ic = Character.toLowerCase(nextb);
                }
                else {
                  nextb_ic = Character.toUpperCase(nextb);
                }
              }
              else {
                nextb_ic = nextb;
              }
            }
            else {
              nextb = '\u0000';
              nextb_ic = '\u0000';
            }
            if (op != BRACE_SIMPLE) {
              minval = (op == STAR) ? 0 : 1;
              maxval = MAX_LIMIT;
            }
            else {
              minval = bl_minval;
              maxval = bl_maxval;
            }

            /*
                             * When maxval > minval, try matching as much as possible, up
                             * to maxval.  When maxval < minval, try matching at least the
                             * minimal number (since the range is backwards, that's also
                             * maxval!).
                             */
            count = regrepeat(scan.OPERAND(), maxval);
            if (got_int) {
              return false;
            }
            if (minval <= maxval) {
              /* Range is the normal way around, use longest match */
              while (count >= minval) {
                /* If it could match, try it. */
                if (nextb == '\u0000' || reginput.charAt() == nextb ||
                    reginput.charAt() == nextb_ic) {
                  reg_save(save);
                  if (regmatch(next)) {
                    return true;
                  }
                  reg_restore(save);
                }
                /* Couldn't or didn't match -- back up one char. */
                if (--count < minval) {
                  break;
                }
                if (reginput.equals(regline)) {
                  /* backup to last char of previous line */
                  --reglnum;
                  regline = reg_getline(reglnum);
                  /* Just in case regrepeat() didn't count right. */
                  if (regline == null) {
                    return false;
                  }
                  reginput = regline.ref(regline.strlen());
                  // fast_breakcheck(); - TOOD
                  if (got_int || out_of_stack) {
                    return false;
                  }
                }
                else {
                  reginput.dec();
                }
              }
            }
            else {
              /* Range is backwards, use shortest match first.
                                 * Careful: maxval and minval are exchanged! */
              if (count < maxval) {
                return false;
              }
              for (; ; ) {
                /* If it could work, try it. */
                if (nextb == '\u0000' || reginput.charAt() == nextb ||
                    reginput.charAt() == nextb_ic) {
                  reg_save(save);
                  if (regmatch(next)) {
                    return true;
                  }
                  reg_restore(save);
                }
                /* Couldn't or didn't match: try advancing one char. */
                if (count == minval || regrepeat(scan.OPERAND(), 1) == 0) {
                  break;
                }
                ++count;
                if (got_int || out_of_stack) {
                  return false;
                }
              }
            }
            return false;
          }
          /* break; Not Reached */

          case NOMATCH: {
            regsave_T save = new regsave_T();

            /* If the operand matches, we fail.  Otherwise backup and
                             * continue with the next item. */
            reg_save(save);
            if (regmatch(scan.OPERAND())) {
              return false;
            }
            reg_restore(save);
          }
          break;

          case MATCH:
          case SUBPAT: {
            regsave_T save = new regsave_T();

            /* If the operand doesn't match, we fail.  Otherwise backup
                             * and continue with the next item. */
            reg_save(save);
            if (!regmatch(scan.OPERAND())) {
              return false;
            }
            if (op == MATCH)            /* zero-width */ {
              reg_restore(save);
            }
          }
          break;

          case BEHIND:
          case NOBEHIND: {
            regsave_T save_after = new regsave_T(), save_start = new regsave_T();
            regsave_T save_behind_pos;
            boolean needmatch = (op == BEHIND);

            /*
                             * Look back in the input of the operand matches or not. This
                             * must be done at every position in the input and checking if
                             * the match ends at the current position.
                             * First check if the next item matches, that's probably
                             * faster.
                             */
            reg_save(save_start);
            if (regmatch(next)) {
              /* save the position after the found match for next */
              reg_save(save_after);

              /* start looking for a match with operand at the current
                                 * postion.  Go back one character until we find the
                                 * result, hitting the start of the line or the previous
                                 * line (for multi-line matching).
                                 * Set behind_pos to where the match should end, BHPOS
                                 * will match it. */
              save_behind_pos = behind_pos == null ? null : new regsave_T(behind_pos);
              behind_pos = new regsave_T(save_start);
              while (true) {
                reg_restore(save_start);
                if (regmatch(scan.OPERAND()) && reg_save_equal(behind_pos)) {
                  behind_pos = save_behind_pos;
                  /* found a match that ends where "next" started */
                  if (needmatch) {
                    reg_restore(save_after);
                    return true;
                  }
                  return false;
                }
                /*
                                     * No match: Go back one character.  May go to
                                     * previous line once.
                                     */
                if (reg_match == null) {
                  if (save_start.pos.col == 0) {
                    if (save_start.pos.lnum < behind_pos.pos.lnum ||
                        reg_getline(--save_start.pos.lnum) == null) {
                      break;
                    }
                    reg_restore(save_start);
                    save_start.pos.col = regline.strlen();
                  }
                  else {
                    --save_start.pos.col;
                  }
                }
                else {
                  if (save_start.ptr == regline) {
                    break;
                  }
                  save_start.ptr.dec();
                }
              }

              /* NOBEHIND succeeds when no match was found */
              behind_pos = save_behind_pos;
              if (!needmatch) {
                reg_restore(save_after);
                return true;
              }
            }
            return false;
          }

          case BHPOS:
            if (reg_match == null) {
              if (behind_pos.pos.col != reginput.pointer() - regline.pointer() ||
                  behind_pos.pos.lnum != reglnum) {
                return false;
              }
            }
            else if (behind_pos.ptr != reginput) {
              return false;
            }
            break;

          case NEWL:
            if (c != '\u0000' || reglnum == reg_maxline) {
              return false;
            }
            reg_nextline();
            break;

          case END:
            return true;        /* Success! */

          default:
            VimPlugin.showMessage(MessageHelper.message(Msg.e_re_corr));
            return false;
        }
      }

      scan = next;
    }

    /*
         * We get here only if there's trouble -- normally "case END" is the
         * terminating point.
         */
    VimPlugin.showMessage(MessageHelper.message(Msg.e_re_corr));
    return false;
  }

  /*
     * regrepeat - repeatedly match something simple, return how many.
     * Advances reginput (and reglnum) to just after the matched chars.
     */
  private int regrepeat(@NotNull CharPointer p, int maxcount) {
    int count = 0;
    CharPointer scan;
    CharPointer opnd;
    int mask = 0;
    int testval = 0;

    scan = reginput.ref(0);        /* Make local copy of reginput for speed. */
    opnd = p.OPERAND();
    switch (p.OP()) {
      case ANY:
      case ANY + ADD_NL:
        while (count < maxcount) {
          /* Matching anything means we continue until end-of-line (or
                     * end-of-file for ANY + ADD_NL), only limited by maxcount. */
          while (!scan.isNul() && count < maxcount) {
            ++count;
            scan.inc();
          }
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline || count == maxcount) {
            break;
          }
          ++count;            /* count the line-break */
          reg_nextline();
          scan = reginput.ref(0);
          if (got_int) {
            break;
          }
        }
        break;

      case IDENT:
      case IDENT + ADD_NL:
        testval = 1;
        /*FALLTHROUGH*/
      case SIDENT:
      case SIDENT + ADD_NL:
        while (count < maxcount) {
          if (Character.isJavaIdentifierPart(scan.charAt()) &&
              (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc();
          }
          else if (scan.isNul()) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break;
            }
            reg_nextline();
            scan = reginput.ref(0);
            if (got_int) {
              break;
            }
          }
          else {
            break;
          }
          ++count;
        }
        break;

      case KWORD:
      case KWORD + ADD_NL:
        testval = 1;
        /*FALLTHROUGH*/
      case SKWORD:
      case SKWORD + ADD_NL:
        while (count < maxcount) {
          if (CharacterClasses.isWord(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc();
          }
          else if (scan.isNul()) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break;
            }
            reg_nextline();
            scan = reginput.ref(0);
            if (got_int) {
              break;
            }
          }
          else {
            break;
          }
          ++count;
        }
        break;

      case FNAME:
      case FNAME + ADD_NL:
        testval = 1;
        /*FALLTHROUGH*/
      case SFNAME:
      case SFNAME + ADD_NL:
        while (count < maxcount) {
          if (CharacterClasses.isFile(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc();
          }
          else if (scan.isNul()) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break;
            }
            reg_nextline();
            scan = reginput.ref(0);
            if (got_int) {
              break;
            }
          }
          else {
            break;
          }
          ++count;
        }
        break;

      case PRINT:
      case PRINT + ADD_NL:
        testval = 1;
        /*FALLTHROUGH*/
      case SPRINT:
      case SPRINT + ADD_NL:
        while (count < maxcount) {
          if (scan.isNul()) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break;
            }
            reg_nextline();
            scan = reginput.ref(0);
            if (got_int) {
              break;
            }
          }
          else if (CharacterClasses.isPrint(scan.charAt()) &&
                   (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc();
          }
          else {
            break;
          }
          ++count;
        }
        break;

      case WHITE:
      case WHITE + ADD_NL:
        testval = mask = CharacterClasses.RI_WHITE;
        break;
      case NWHITE:
      case NWHITE + ADD_NL:
        mask = CharacterClasses.RI_WHITE;
        break;
      case DIGIT:
      case DIGIT + ADD_NL:
        testval = mask = CharacterClasses.RI_DIGIT;
        break;
      case NDIGIT:
      case NDIGIT + ADD_NL:
        mask = CharacterClasses.RI_DIGIT;
        break;
      case HEX:
      case HEX + ADD_NL:
        testval = mask = CharacterClasses.RI_HEX;
        break;
      case NHEX:
      case NHEX + ADD_NL:
        mask = CharacterClasses.RI_HEX;
        break;
      case OCTAL:
      case OCTAL + ADD_NL:
        testval = mask = CharacterClasses.RI_OCTAL;
        break;
      case NOCTAL:
      case NOCTAL + ADD_NL:
        mask = CharacterClasses.RI_OCTAL;
        break;
      case WORD:
      case WORD + ADD_NL:
        testval = mask = CharacterClasses.RI_WORD;
        break;
      case NWORD:
      case NWORD + ADD_NL:
        mask = CharacterClasses.RI_WORD;
        break;
      case HEAD:
      case HEAD + ADD_NL:
        testval = mask = CharacterClasses.RI_HEAD;
        break;
      case NHEAD:
      case NHEAD + ADD_NL:
        mask = CharacterClasses.RI_HEAD;
        break;
      case ALPHA:
      case ALPHA + ADD_NL:
        testval = mask = CharacterClasses.RI_ALPHA;
        break;
      case NALPHA:
      case NALPHA + ADD_NL:
        mask = CharacterClasses.RI_ALPHA;
        break;
      case LOWER:
      case LOWER + ADD_NL:
        testval = mask = CharacterClasses.RI_LOWER;
        break;
      case NLOWER:
      case NLOWER + ADD_NL:
        mask = CharacterClasses.RI_LOWER;
        break;
      case UPPER:
      case UPPER + ADD_NL:
        testval = mask = CharacterClasses.RI_UPPER;
        break;
      case NUPPER:
      case NUPPER + ADD_NL:
        mask = CharacterClasses.RI_UPPER;
        break;

      case EXACTLY: {
        char cu, cl;

        /* This doesn't do a multi-byte character, because a MULTIBYTECODE
                     * would have been used for it. */
        if (ireg_ic) {
          cu = Character.toUpperCase(opnd.charAt());
          cl = Character.toLowerCase(opnd.charAt());
          while (count < maxcount && (scan.charAt() == cu || scan.charAt() == cl)) {
            count++;
            scan.inc();
          }
        }
        else {
          cu = opnd.charAt();
          while (count < maxcount && scan.charAt() == cu) {
            count++;
            scan.inc();
          }
        }
        break;
      }

      case ANYOF:
      case ANYOF + ADD_NL:
        testval = 1;
        /*FALLTHROUGH*/

      case ANYBUT:
      case ANYBUT + ADD_NL:
        while (count < maxcount) {
          if (scan.isNul()) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break;
            }
            reg_nextline();
            scan = reginput.ref(0);
            if (got_int) {
              break;
            }
          }
          else {
            if ((cstrchr(opnd, scan.charAt()) == null) == (testval == 1)) {
              break;
            }
            scan.inc();
          }
          ++count;
        }
        break;

      case NEWL:
        while (count < maxcount && scan.isNul() && reglnum < reg_maxline) {
          count++;
          reg_nextline();
          scan = reginput.ref(0);
          if (got_int) {
            break;
          }
        }
        break;

      default:                  /* Oh dear.  Called inappropriately. */
        VimPlugin.showMessage(MessageHelper.message(Msg.e_re_corr));
        break;
    }

    if (mask != 0) {
      while (count < maxcount) {
        if (scan.isNul()) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break;
          }
          reg_nextline();
          scan = reginput.ref(0);
          if (got_int) {
            break;
          }
        }
        else if (CharacterClasses.isMask(scan.charAt(), mask, testval)) {
          scan.inc();
        }
        else {
          break;
        }
        ++count;
      }
    }

    reginput = scan.ref(0);

    return count;
  }

  /*
     * regnext - dig the "next" pointer out of a node
     */
  @Nullable
  private CharPointer regnext(@NotNull CharPointer p) {
    int offset;

    offset = p.NEXT();
    if (offset == 0) {
      return null;
    }

    if (p.OP() == BACK) {
      return p.ref(-offset);
    }
    else {
      return p.ref(offset);
    }
  }

  /*
     * Check the regexp program for its magic number.
     * Return true if it's wrong.
     */
  private boolean prog_magic_wrong() {
    if ((reg_match == null ? reg_mmatch.regprog.program : reg_match.regprog.program).charAt(0) != REGMAGIC) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_re_corr));
      return true;
    }
    return false;
  }

  /*
     * Cleanup the subexpressions, if this wasn't done yet.
     * This construction is used to clear the subexpressions only when they are
     * used (to increase speed).
     */
  private void cleanup_subexpr() {
    if (need_clear_subexpr) {
      if (reg_match == null) {
        /* Use 0xff to set lnum to -1 */
        for (int i = 0; i < NSUBEXP; i++) {
          reg_startpos[i].col = -1;
          reg_startpos[i].lnum = -1;
          reg_endpos[i].col = -1;
          reg_endpos[i].lnum = -1;
        }
      }
      else {
        for (int i = 0; i < NSUBEXP; i++) {
          reg_startp[i] = null;
          reg_endp[i] = null;
        }
      }
      need_clear_subexpr = false;
    }
  }

  private void cleanup_zsubexpr() {
    if (need_clear_zsubexpr) {
      if (reg_match == null) {
        /* Use 0xff to set lnum to -1 */
        for (int i = 0; i < NSUBEXP; i++) {
          reg_startzpos[i].col = -1;
          reg_startzpos[i].lnum = -1;
          reg_endzpos[i].col = -1;
          reg_endzpos[i].lnum = -1;
        }
      }
      else {
        for (int i = 0; i < NSUBEXP; i++) {
          reg_startzp[i] = null;
          reg_endzp[i] = null;
        }
      }
      need_clear_zsubexpr = false;
    }
  }

  /*
     * Advance reglnum, regline and reginput to the next line.
     */
  private void reg_nextline() {
    regline = reg_getline(++reglnum);
    reginput = regline.ref(0);
    // fast_breakcheck(); TODO
  }

  /*
     * Save the input line and position in a regsave_T.
     */
  private void reg_save(@NotNull regsave_T save) {
    if (reg_match == null) {
      save.pos.col = reginput.pointer() - regline.pointer();
      save.pos.lnum = reglnum;
    }
    else {
      save.ptr = reginput.ref(0);
    }
  }

  /*
     * Restore the input line and position from a regsave_T.
     */
  private void reg_restore(@NotNull regsave_T save) {
    if (reg_match == null) {
      if (reglnum != save.pos.lnum) {
        /* only call reg_getline() when the line number changed to save
                 * a bit of time */
        reglnum = save.pos.lnum;
        regline = reg_getline(reglnum);
      }
      reginput = regline.ref(save.pos.col);
    }
    else {
      reginput = save.ptr.ref(0);
    }
  }

  /*
     * Return true if current position is equal to saved position.
     */
  private boolean reg_save_equal(@NotNull regsave_T save) {
    if (reg_match == null) {
      return reglnum == save.pos.lnum && reginput.equals(regline.ref(save.pos.col));
    }
    return reginput.equals(save.ptr);
  }

  /*
     * Tentatively set the sub-expression start to the current position (after
     * calling regmatch() they will have changed).  Need to save the existing
     * values for when there is no match.
     * Use pointer or position, depending on reg_match == null.
     */
  private void save_se(@NotNull save_se_T savep, @NotNull lpos_T posp, @Nullable CharPointer pp) {
    if (reg_match == null) {
      savep.pos.lnum = posp.lnum;
      savep.pos.col = posp.col;
      posp.lnum = reglnum;
      posp.col = reginput.pointer() - regline.pointer();
    }
    else if (pp != null) {
      savep.ptr = pp.ref(0);
      pp.assign(reginput);
    }
  }

  /*
     * We were wrong, restore the sub-expressions.
     */
  private void restore_se(@NotNull save_se_T savep, @NotNull lpos_T posp, @Nullable CharPointer pp) {
    if (reg_match == null) {
      posp.col = savep.pos.col;
      posp.lnum = savep.pos.lnum;
    }
    else if (pp != null) {
      pp.assign(savep.ptr);
    }
  }

  /*
     * Compare a number with the operand of RE_LNUM, RE_COL or RE_VCOL.
     */
  private boolean re_num_cmp(int val, @NotNull CharPointer scan) {
    int n = scan.OPERAND_MIN();

    if (scan.OPERAND_CMP() == '>') {
      return val > n;
    }
    if (scan.OPERAND_CMP() == '<') {
      return val < n;
    }
    return val == n;
  }

  /*
     * Compare two strings, ignore case if ireg_ic set.
     * Return 0 if strings match, non-zero otherwise.
     */
  private int cstrncmp(@NotNull CharPointer s1, @NotNull CharPointer s2, int n) {
    return s1.strncmp(s2, n, ireg_ic);
  }

  /*
     * cstrchr: This function is used a lot for simple searches, keep it fast!
     */
  @Nullable
  private CharPointer cstrchr(@NotNull CharPointer s, char c) {
    if (!ireg_ic) {
      return s.strchr(c);
    }
    else {
      return s.istrchr(c);
    }

    /* tolower() and toupper() can be slow, comparing twice should be a lot
         * faster (esp. when using MS Visual C++!).
         * For UTF-8 need to use folded case. */
    /* was 1,173ms
        int cc;
        if (CharacterClasses.isUpper(c))
        {
            cc = Character.toLowerCase(c);
        }
        else if (CharacterClasses.isLower(c))
        {
            cc = Character.toUpperCase(c);
        }
        else
        {
            return s.strchr(c);
        }
        */

    /* Faster version for when there are no multi-byte characters. */
    /*
        CharPointer p = s.ref(0);
        char ch;
        while ((ch = p.charAt()) != '\u0000')
        {
            if (ch == c || ch == cc)
            {
                return p;
            }

            p.inc();
        }
        */

    /* was 2,053ms
        for (p = s.ref(0); !p.isNul(); p.inc())
        {
            char ch = p.charAt();
            if (ch == c || ch == cc)
            {
                return p;
            }
        }
        */

    //return null;
  }

  /***************************************************************
   *                    regsub stuff                             *
   ***************************************************************/

  /*
     * regtilde(): Replace tildes in the pattern by the old pattern.
     *
     * Short explanation of the tilde: It stands for the previous replacement
     * pattern.  If that previous pattern also contains a ~ we should go back a
     * step further...  But we insert the previous pattern into the current one
     * and remember that.
     * This still does not handle the case where "magic" changes. TODO?
     *
     * The tildes are parsed once before the first call to vim_regsub().
     */
  //public CharPointer regtilde(CharPointer source, boolean magic)
  //{
  //    CharPointer newsub = source.ref(0);
  //    CharPointer tmpsub;
  //    CharPointer p;
  //    int len;
  //    int prevlen;
  //
  //    for (p = newsub.ref(0); !p.isNul(); p.inc())
  //    {
  //        if ((p.charAt() == '~' && magic != 0) || (p.charAt() == '\\' && p.charAt(1) == '~' && magic == 0))
  //        {
  //            if (reg_prev_sub != null)
  //            {
  //                /* length = len(newsub) - 1 + len(prev_sub) + 1 */
  //                prevlen = reg_prev_sub.strlen();
  //                tmpsub = alloc((unsigned)(STRLEN(newsub) + prevlen));
  //                if (tmpsub != null)
  //                {
  //                    /* copy prefix */
  //                    len = (int)(p - newsub);    /* not including ~ */
  //                    mch_memmove(tmpsub, newsub, (size_t)len);
  //                    /* interpretate tilde */
  //                    mch_memmove(tmpsub + len, reg_prev_sub, (size_t)prevlen);
  //                    /* copy postfix */
  //                    if (!magic)
  //                        ++p;                    /* back off \ */
  //                    STRCPY(tmpsub + len + prevlen, p + 1);
  //
  //                    if (newsub != source)       /* already allocated newsub */
  //                        vim_free(newsub);
  //                    newsub = tmpsub;
  //                    p = newsub + len + prevlen;
  //                }
  //            }
  //            else if (magic)
  //                STRCPY(p, p + 1);               /* remove '~' */
  //            else
  //                STRCPY(p, p + 2);               /* remove '\~' */
  //            --p;
  //        }
  //        else if (*p == '\\' && p[1])            /* skip escaped characters */
  //        ++p;
  //    }
  //
  //    vim_free(reg_prev_sub);
  //    if (newsub != source)       /* newsub was allocated, just keep it */
  //        reg_prev_sub = newsub;
  //    else                        /* no ~ found, need to save newsub  */
  //        reg_prev_sub = vim_strsave(newsub);
  //    return newsub;
  //}

  /**
   * vim_regsub() - perform substitutions after a vim_regexec() or
   * vim_regexec_multi() match.
   * <p/>
   * If "copy" is true really copy into "dest".
   * If "copy" is false nothing is copied, this is just to find out the length
   * of the result.
   * <p/>
   * If "backslash" is true, a backslash will be removed later, need to double
   * them to keep them, and insert a backslash before a CR to avoid it being
   * replaced with a line break later.
   * <p/>
   * Note: The matched text must not change between the call of
   * vim_regexec()/vim_regexec_multi() and vim_regsub()!  It would make the back
   * references invalid!
   * <p/>
   * Returns the size of the replacement, including terminating '\u0000'.
   */
  @Nullable
  public String vim_regsub(regmatch_T rmp, CharPointer source, int magic, boolean backslash) {
    reg_match = rmp;
    reg_mmatch = null;
    reg_maxline = 0;

    return vim_regsub_both(source, magic, backslash);
  }

  @Nullable
  public String vim_regsub_multi(regmmatch_T rmp, int lnum, CharPointer source, int magic, boolean backslash) {
    reg_match = null;
    reg_mmatch = rmp;
    //reg_buf = curbuf;           /* always works on the current buffer! */
    reg_firstlnum = lnum;
    reg_maxline = EditorHelper.getLineCount(reg_buf) - lnum;

    return vim_regsub_both(source, magic, backslash);
  }

  private int subappend(int mode, @NotNull StringBuffer dst, char c) {
    switch (mode) {
      case 'u':
        mode = 0;
        // Fall through
      case 'U':
        dst.append(Character.toUpperCase(c));
        break;
      case 'l':
        mode = 0;
        // Fall through
      case 'L':
        dst.append(Character.toLowerCase(c));
        break;
      default:
        dst.append(c);
        break;
    }

    return mode;
  }

  @Nullable
  private String vim_regsub_both(@Nullable CharPointer source, int magic, boolean backslash) {
    CharPointer src;
    StringBuffer dst = new StringBuffer();
    CharPointer s;
    char c;
    int no = -1;
    int clnum = 0;      /* init for GCC */
    int len = 0;        /* init for GCC */
    //CharPointer eval_result = null;
    int mode = 0;

    /* Be paranoid... */
    if (source == null) {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_null));
      return null;
    }
    if (prog_magic_wrong()) {
      return null;
    }
    src = source.ref(0);

    /*
         * When the substitute part starts with "\=" evaluate it as an expression.
         */
    if (source.charAt(0) == '\\' && source.charAt(1) == '=') {
    }
    else {
      while ((c = src.charAt()) != '\u0000') {
        src.inc();
        if (c == '&' && magic != 0) {
          no = 0;
        }
        else if (c == '\\' && !src.isNul()) {
          if (src.charAt() == '&' && magic == 0) {
            src.inc();
            no = 0;
          }
          else if ('0' <= src.charAt() && src.charAt() <= '9') {
            no = src.charAt() - '0';
            src.inc();
          }
          else if ("uUlLeE".indexOf(src.charAt()) != -1) {
            switch (src.charAtInc()) {
              case 'u':
                mode = 'u';
                continue;
              case 'U':
                mode = 'U';
                continue;
              case 'l':
                mode = 'l';
                continue;
              case 'L':
                mode = 'L';
                continue;
              case 'e':
              case 'E':
                mode = 0;
                continue;
            }
          }
        }
        if (no < 0)           /* Ordinary character. */ {
          if (c == '\\' && !src.isNul()) {
            /* Check for abbreviations -- webb */
            // In vim '\u0000' is represented in memory as '\n', and '\n' as '\r', see :help NL-used-for-Nul
            switch (src.charAt()) {
              case 'r':
                c = '\n';
                src.inc();
                break;
              case 'n':
                c = '\u0000';
                src.inc();
                break;
              case 't':
                c = '\t';
                src.inc();
                break;
              /* Oh no!  \e already has meaning in subst pat :-( */
              /* case 'e':   c = ESC;        ++src;  break; */
              case 'b':
                c = '\b';
                src.inc();
                break;

              /* If "backslash" is true the backslash will be removed
                                 * later.  Used to insert a literal CR. */
              default:
                if (backslash) {
                  dst.append('\\');
                }
                c = src.charAt();
                src.inc();
            }
          }

          /* Write to buffer, if copy is set. */
          mode = subappend(mode, dst, c);
        }
        else {
          if (reg_match == null) {
            clnum = reg_mmatch.startpos[no].lnum;
            if (clnum < 0 || reg_mmatch.endpos[no].lnum < 0) {
              s = null;
            }
            else {
              s = reg_getline(clnum).ref(reg_mmatch.startpos[no].col);
              if (reg_mmatch.endpos[no].lnum == clnum) {
                len = reg_mmatch.endpos[no].col - reg_mmatch.startpos[no].col;
              }
              else {
                len = s.strlen();
              }
            }
          }
          else {
            s = reg_match.startp[no];
            if (reg_match.endp[no] == null) {
              s = null;
            }
            else {
              len = reg_match.endp[no].pointer() - s.pointer();
            }
          }
          if (s != null) {
            for (; ; ) {
              if (len == 0) {
                if (reg_match == null) {
                  if (reg_mmatch.endpos[no].lnum == clnum) {
                    break;
                  }
                  dst.append('\r');
                  s = reg_getline(++clnum);
                  if (reg_mmatch.endpos[no].lnum == clnum) {
                    len = reg_mmatch.endpos[no].col;
                  }
                  else {
                    len = s.strlen();
                  }
                }
                else {
                  break;
                }
              }
              else if (s.isNul()) /* we hit '\u0000'. */ {
                VimPlugin.showMessage(MessageHelper.message(Msg.e_re_damg));

                return dst.toString();
              }
              else {
                if (backslash && (s.charAt() == '\r' || s.charAt() == '\\')) {
                  /*
                                     * Insert a backslash in front of a CR, otherwise
                                     * it will be replaced by a line break.
                                     * Number of backslashes will be halved later,
                                     * double them here.
                                     */
                  dst.append('\\');
                  dst.append(s.charAt());
                }
                else {
                  mode = subappend(mode, dst, s.charAt());
                }
                s.inc();
                --len;
              }
            }
          }
          no = -1;
        }
      }
    }

    return dst.toString();
  }

  /*
     * Used for the submatch() function: get the string from tne n'th submatch in
     * allocated memory.
     * Returns null when not in a ":s" command and for a non-existing submatch.
     */
  /*
    public String reg_submatch(int no)
    {
        StringBuffer retval = null;
        CharPointer s;
        int len;
        int round;
        int lnum;

        if (!can_f_submatch)
            return null;

        if (submatch_match == null)
        {
            // First round: compute the length and allocate memory.
            // Second round: copy the text.
            for (round = 1; round <= 2; ++round)
            {
                lnum = submatch_mmatch.startpos[no].lnum;
                if (lnum < 0 || submatch_mmatch.endpos[no].lnum < 0)
                {
                    return null;
                }

                s = reg_getline(lnum).ref(submatch_mmatch.startpos[no].col);
                if (s == null)  // anti-crash check, cannot happen?
                {
                    break;
                }
                if (submatch_mmatch.endpos[no].lnum == lnum)
                {
                    // Within one line: take form start to end col.
                    len = submatch_mmatch.endpos[no].col - submatch_mmatch.startpos[no].col;
                    if (round == 2)
                    {
                        retval.append(s.substring(len));
                    }
                    ++len;
                }
                else
                {
                    // Multiple lines: take start line from start col, middle
                    // lines completely and end line up to end col.
                    len = s.strlen();
                    if (round == 2)
                    {
                        retval.append(s.substring(len));
                    }
                    ++len;
                    ++lnum;
                    while (lnum < submatch_mmatch.endpos[no].lnum)
                    {
                        s = reg_getline(lnum++);
                        if (round == 2)
                        {
                            retval.append(s.substring(s.strlen()));
                        }
                        len += s.strlen();
                        if (round == 2)
                        {
                            retval.append('\n');
                        }
                        ++len;
                    }
                    if (round == 2)
                    {
                        retval.append(reg_getline(lnum).substring(submatch_mmatch.endpos[no].col));
                    }
                    len += submatch_mmatch.endpos[no].col;
                    if (round == 2)
                    {
                        //retval[len] = '\u0000';
                    }
                    ++len;
                }

                if (round == 1)
                {
                    retval = new StringBuffer();
                    if (s == null)
                        return null;
                }
            }
        }
        else
        {
            if (submatch_match.endp[no] == null)
            {
                retval = null;
            }
            else
            {
                s = submatch_match.startp[no];
                retval = new StringBuffer(s.substring(submatch_match.endp[no].pointer() - s.pointer()));
            }
        }

        return retval == null ? null : retval.toString();
    }
    */

  /*
     * regdump - dump a regexp onto stdout in vaguely comprehensible form
     */
  @NotNull
  private String regdump(String pattern, @NotNull regprog_T r) {
    CharPointer start;
    CharPointer s;
    int op = EXACTLY;       /* Arbitrary non-END op. */
    CharPointer next;
    CharPointer end = null;
    StringBuffer res = new StringBuffer();

    res.append("\nregcomp(").append(pattern).append("):\n");

    start = (new CharPointer(r.program));
    s = start.ref(1);
    /*
        * Loop until we find the END that isn't before a referred next (an END
        * can also appear in a NOMATCH operand).
        */
    while (op != END || !s.end()) {
      op = s.OP();
      res.append(s.pointer() - start.pointer());
      res.append(regprop(s));
      next = regnext(s);
      if (next == null)       /* Next ptr. */ {
        res.append("(0)");
      }
      else {
        res.append("(").append(s.pointer() - start.pointer() + (next.pointer() - s.pointer())).append(")");
      }
      if (end == null || (next != null && end.pointer() < next.pointer())) {
        end = next;
      }
      if (op == BRACE_LIMITS) {
        /* Two short ints */
        res.append(" minval ");
        res.append(s.OPERAND_MIN());
        res.append(", maxval ");
        res.append(s.OPERAND_MAX());
        s.inc(8);
      }
      s.inc(3);
      if (op == ANYOF || op == ANYOF + ADD_NL
          || op == ANYBUT || op == ANYBUT + ADD_NL
          || op == EXACTLY) {
        /* Literal string, where present. */
        while (!s.isNul()) {
          res.append(s.charAt());
          s.inc();
        }
        s.inc();
      }
      res.append("\n");
    }

    /* Header fields of interest. */
    if (r.regstart != '\u0000') {
      res.append("start `");
      if (r.regstart < ' ') {
        res.append("^").append((char)('@' + r.regstart));
      }
      else {
        res.append(r.regstart);
      }
      res.append("' ");
      res.append(Integer.toString(r.regstart, 16));
    }
    if (r.reganch != 0) {
      res.append("anchored: ");
    }
    if (r.regmust != null) {
      res.append("must have \"").append(r.regmust.substring(r.regmust.strlen())).append("\"");
    }
    res.append("\n");

    return res.toString();
  }

  /*
* regprop - printable representation of opcode
*/
  @NotNull
  private String regprop(@NotNull CharPointer op) {
    String p;
    StringBuffer buf = new StringBuffer();

    buf.append(':');

    switch (op.OP()) {
      case BOL:
        p = "BOL";
        break;
      case EOL:
        p = "EOL";
        break;
      case RE_BOF:
        p = "BOF";
        break;
      case RE_EOF:
        p = "EOF";
        break;
      case CURSOR:
        p = "CURSOR";
        break;
      case RE_LNUM:
        p = "RE_LNUM";
        break;
      case RE_COL:
        p = "RE_COL";
        break;
      case RE_VCOL:
        p = "RE_VCOL";
        break;
      case BOW:
        p = "BOW";
        break;
      case EOW:
        p = "EOW";
        break;
      case ANY:
        p = "ANY";
        break;
      case ANY + ADD_NL:
        p = "ANY+NL";
        break;
      case ANYOF:
        p = "ANYOF";
        break;
      case ANYOF + ADD_NL:
        p = "ANYOF+NL";
        break;
      case ANYBUT:
        p = "ANYBUT";
        break;
      case ANYBUT + ADD_NL:
        p = "ANYBUT+NL";
        break;
      case IDENT:
        p = "IDENT";
        break;
      case IDENT + ADD_NL:
        p = "IDENT+NL";
        break;
      case SIDENT:
        p = "SIDENT";
        break;
      case SIDENT + ADD_NL:
        p = "SIDENT+NL";
        break;
      case KWORD:
        p = "KWORD";
        break;
      case KWORD + ADD_NL:
        p = "KWORD+NL";
        break;
      case SKWORD:
        p = "SKWORD";
        break;
      case SKWORD + ADD_NL:
        p = "SKWORD+NL";
        break;
      case FNAME:
        p = "FNAME";
        break;
      case FNAME + ADD_NL:
        p = "FNAME+NL";
        break;
      case SFNAME:
        p = "SFNAME";
        break;
      case SFNAME + ADD_NL:
        p = "SFNAME+NL";
        break;
      case PRINT:
        p = "PRINT";
        break;
      case PRINT + ADD_NL:
        p = "PRINT+NL";
        break;
      case SPRINT:
        p = "SPRINT";
        break;
      case SPRINT + ADD_NL:
        p = "SPRINT+NL";
        break;
      case WHITE:
        p = "WHITE";
        break;
      case WHITE + ADD_NL:
        p = "WHITE+NL";
        break;
      case NWHITE:
        p = "NWHITE";
        break;
      case NWHITE + ADD_NL:
        p = "NWHITE+NL";
        break;
      case DIGIT:
        p = "DIGIT";
        break;
      case DIGIT + ADD_NL:
        p = "DIGIT+NL";
        break;
      case NDIGIT:
        p = "NDIGIT";
        break;
      case NDIGIT + ADD_NL:
        p = "NDIGIT+NL";
        break;
      case HEX:
        p = "HEX";
        break;
      case HEX + ADD_NL:
        p = "HEX+NL";
        break;
      case NHEX:
        p = "NHEX";
        break;
      case NHEX + ADD_NL:
        p = "NHEX+NL";
        break;
      case OCTAL:
        p = "OCTAL";
        break;
      case OCTAL + ADD_NL:
        p = "OCTAL+NL";
        break;
      case NOCTAL:
        p = "NOCTAL";
        break;
      case NOCTAL + ADD_NL:
        p = "NOCTAL+NL";
        break;
      case WORD:
        p = "WORD";
        break;
      case WORD + ADD_NL:
        p = "WORD+NL";
        break;
      case NWORD:
        p = "NWORD";
        break;
      case NWORD + ADD_NL:
        p = "NWORD+NL";
        break;
      case HEAD:
        p = "HEAD";
        break;
      case HEAD + ADD_NL:
        p = "HEAD+NL";
        break;
      case NHEAD:
        p = "NHEAD";
        break;
      case NHEAD + ADD_NL:
        p = "NHEAD+NL";
        break;
      case ALPHA:
        p = "ALPHA";
        break;
      case ALPHA + ADD_NL:
        p = "ALPHA+NL";
        break;
      case NALPHA:
        p = "NALPHA";
        break;
      case NALPHA + ADD_NL:
        p = "NALPHA+NL";
        break;
      case LOWER:
        p = "LOWER";
        break;
      case LOWER + ADD_NL:
        p = "LOWER+NL";
        break;
      case NLOWER:
        p = "NLOWER";
        break;
      case NLOWER + ADD_NL:
        p = "NLOWER+NL";
        break;
      case UPPER:
        p = "UPPER";
        break;
      case UPPER + ADD_NL:
        p = "UPPER+NL";
        break;
      case NUPPER:
        p = "NUPPER";
        break;
      case NUPPER + ADD_NL:
        p = "NUPPER+NL";
        break;
      case BRANCH:
        p = "BRANCH";
        break;
      case EXACTLY:
        p = "EXACTLY";
        break;
      case NOTHING:
        p = "NOTHING";
        break;
      case BACK:
        p = "BACK";
        break;
      case END:
        p = "END";
        break;
      case MOPEN:
        p = "MATCH START";
        break;
      case MOPEN + 1:
      case MOPEN + 2:
      case MOPEN + 3:
      case MOPEN + 4:
      case MOPEN + 5:
      case MOPEN + 6:
      case MOPEN + 7:
      case MOPEN + 8:
      case MOPEN + 9:
        buf.append("MOPEN").append(op.OP() - MOPEN);
        p = null;
        break;
      case MCLOSE:
        p = "MATCH END";
        break;
      case MCLOSE + 1:
      case MCLOSE + 2:
      case MCLOSE + 3:
      case MCLOSE + 4:
      case MCLOSE + 5:
      case MCLOSE + 6:
      case MCLOSE + 7:
      case MCLOSE + 8:
      case MCLOSE + 9:
        buf.append("MCLOSE").append(op.OP() - MCLOSE);
        p = null;
        break;
      case BACKREF + 1:
      case BACKREF + 2:
      case BACKREF + 3:
      case BACKREF + 4:
      case BACKREF + 5:
      case BACKREF + 6:
      case BACKREF + 7:
      case BACKREF + 8:
      case BACKREF + 9:
        buf.append("BACKREF").append(op.OP() - BACKREF);
        p = null;
        break;
      case NOPEN:
        p = "NOPEN";
        break;
      case NCLOSE:
        p = "NCLOSE";
        break;
      case ZOPEN + 1:
      case ZOPEN + 2:
      case ZOPEN + 3:
      case ZOPEN + 4:
      case ZOPEN + 5:
      case ZOPEN + 6:
      case ZOPEN + 7:
      case ZOPEN + 8:
      case ZOPEN + 9:
        buf.append("ZOPEN").append(op.OP() - ZOPEN);
        p = null;
        break;
      case ZCLOSE + 1:
      case ZCLOSE + 2:
      case ZCLOSE + 3:
      case ZCLOSE + 4:
      case ZCLOSE + 5:
      case ZCLOSE + 6:
      case ZCLOSE + 7:
      case ZCLOSE + 8:
      case ZCLOSE + 9:
        buf.append("ZCLOSE").append(op.OP() - ZCLOSE);
        p = null;
        break;
      case ZREF + 1:
      case ZREF + 2:
      case ZREF + 3:
      case ZREF + 4:
      case ZREF + 5:
      case ZREF + 6:
      case ZREF + 7:
      case ZREF + 8:
      case ZREF + 9:
        buf.append("ZREF").append(op.OP() - ZREF);
        p = null;
        break;
      case STAR:
        p = "STAR";
        break;
      case PLUS:
        p = "PLUS";
        break;
      case NOMATCH:
        p = "NOMATCH";
        break;
      case MATCH:
        p = "MATCH";
        break;
      case BEHIND:
        p = "BEHIND";
        break;
      case NOBEHIND:
        p = "NOBEHIND";
        break;
      case SUBPAT:
        p = "SUBPAT";
        break;
      case BRACE_LIMITS:
        p = "BRACE_LIMITS";
        break;
      case BRACE_SIMPLE:
        p = "BRACE_SIMPLE";
        break;
      case BRACE_COMPLEX:
      case BRACE_COMPLEX + 1:
      case BRACE_COMPLEX + 2:
      case BRACE_COMPLEX + 3:
      case BRACE_COMPLEX + 4:
      case BRACE_COMPLEX + 5:
      case BRACE_COMPLEX + 6:
      case BRACE_COMPLEX + 7:
      case BRACE_COMPLEX + 8:
      case BRACE_COMPLEX + 9:
        buf.append("BRACE_COMPLEX").append(op.OP() - BRACE_COMPLEX);
        p = null;
        break;
      case NEWL:
        p = "NEWL";
        break;
      default:
        buf.append("corrupt ").append(op.OP());
        p = null;
        break;
    }
    if (p != null) {
      buf.append(p);
    }

    return buf.toString();
  }

  public static class regprog_T {
    char regstart;
    char reganch;
    @Nullable CharPointer regmust;
    int regmlen;
    int regflags;
    char reghasz;
    @NotNull StringBuffer program = new StringBuffer();
  }

  private static class MinMax {
    int minvalue;
    int maxvalue;
  }

  public static class lpos_T {
    public lpos_T(@NotNull lpos_T pos) {
      this.lnum = pos.lnum;
      this.col = pos.col;
    }

    public lpos_T() {
    }

    public int lnum = 0;
    public int col = 0;

    @Override
    public String toString() {
      return "lpos: (" + lnum + ", " + col + ")";
    }
  }

  /*
    * Structure used to save the current input state, when it needs to be
    * restored after trying a match.  Used by reg_save() and reg_restore().
    */
  private static class regsave_T {
    CharPointer ptr;   /* reginput pointer, for single-line regexp */
    @NotNull lpos_T pos = new lpos_T();    /* reginput pos, for multi-line regexp */

    public regsave_T() {
    }

    public regsave_T(regsave_T rhs) {
      ptr = rhs.ptr == null ? null : new CharPointer("").assign(rhs.ptr);
      pos = new lpos_T(rhs.pos);
    }
  }

  /* struct to save start/end pointer/position in for \(\) */
  private static class save_se_T {
    CharPointer ptr;
    @NotNull lpos_T pos = new lpos_T();
  }

  private static class regmatch_T {
    regprog_T regprog;
    @NotNull CharPointer[] startp = new CharPointer[NSUBEXP];
    @NotNull CharPointer[] endp = new CharPointer[NSUBEXP];
    boolean rm_ic;
  }

  public static class regmmatch_T {
    public regmmatch_T() {
      for (int i = 0; i < NSUBEXP; i++) {
        startpos[i] = new lpos_T();
        endpos[i] = new lpos_T();
      }
    }

    @Nullable public regprog_T regprog;
    @NotNull public lpos_T[] startpos = new lpos_T[NSUBEXP];
    @NotNull public lpos_T[] endpos = new lpos_T[NSUBEXP];
    public boolean rmm_ic;
  }

  private int reg_do_extmatch = 0;

  @Nullable private CharPointer reg_prev_sub = null;

  private CharPointer regparse;      /* Input-scan pointer. */
  private int prevchr_len;    /* byte length of previous char */
  private int num_complex_braces; /* Complex \{...} count */
  private int regnpar;        /* () count. */
  private int regnzpar;       /* \z() count. */
  private char re_has_z;       /* \z item detected */
  private CharPointer regcode;       /* Code-emit pointer */
  @NotNull private boolean[] had_endbrace = new boolean[NSUBEXP];  /* flags, true if end of () found */
  private int regflags;       /* RF_ flags for prog */
  @NotNull private int[] brace_min = new int[10];  /* Minimums for complex brace repeats */
  @NotNull private int[] brace_max = new int[10];  /* Maximums for complex brace repeats */
  @NotNull private int[] brace_count = new int[10]; /* Current counts for complex brace repeats */
  private boolean had_eol;        /* true when EOL found by vim_regcomp() */
  private boolean one_exactly = false;    /* only do one char for EXACTLY */

  private int reg_magic;      /* magicness of the pattern: */

  private int curchr;

  /*
    * getchr() - get the next character from the pattern. We know about
    * magic and such, so therefore we need a lexical analyzer.
    */

  /* static int       curchr; */
  private int prevprevchr;
  private int prevchr;
  private int nextchr;    /* used for ungetchr() */
  /*
    * Note: prevchr is sometimes -1 when we are not at the start,
    * eg in /[ ^I]^ the pattern was never found even if it existed, because ^ was
    * taken to be magic -- webb
    */
  private boolean at_start;       /* True when on the first character */
  private boolean prev_at_start;  /* True when on the second character */

  /*
    * Global work variables for vim_regexec().
    */

  /* The current match-position is remembered with these variables: */
  private int reglnum;        /* line number, relative to first line */
  @Nullable private CharPointer regline;       /* start of current line */
  private CharPointer reginput;      /* current input, points into "regline" */

  private boolean need_clear_subexpr;     /* subexpressions still need to be
    * cleared */
  private boolean need_clear_zsubexpr = false;    /* extmatch subexpressions
    * still need to be cleared */

  private boolean out_of_stack;   /* true when ran out of stack space */

  /*
    * Internal copy of 'ignorecase'.  It is set at each call to vim_regexec().
    * Normally it gets the value of "rm_ic" or "rmm_ic", but when the pattern
    * contains '\c' or '\C' the value is overruled.
    */
  private boolean ireg_ic;

  /*
    * Sometimes need to save a copy of a line.  Since alloc()/free() is very
    * slow, we keep one allocated piece of memory and only re-allocate it when
    * it's too small.  It's freed in vim_regexec_both() when finished.
    */
  @Nullable private CharPointer reg_tofree;
  //private int reg_tofreelen;

  /*
    * These variables are set when executing a regexp to speed up the execution.
    * Which ones are set depends on whethere a single-line or multi-line match is
    * done:
    *                      single-line             multi-line
    * reg_match            &regmatch_T             null
    * reg_mmatch           null                    &regmmatch_T
    * reg_startp           reg_match->startp       <invalid>
    * reg_endp             reg_match->endp         <invalid>
    * reg_startpos         <invalid>               reg_mmatch->startpos
    * reg_endpos           <invalid>               reg_mmatch->endpos
    * reg_win              null                    window in which to search
    * reg_buf              <invalid>               buffer in which to search
    * reg_firstlnum        <invalid>               first line in which to search
    * reg_maxline          0                       last line nr
    */
  @Nullable private regmatch_T reg_match;
  @Nullable private regmmatch_T reg_mmatch;
  @NotNull private CharPointer[] reg_startp = new CharPointer[NSUBEXP];
  @NotNull private CharPointer[] reg_endp = new CharPointer[NSUBEXP];
  @NotNull private lpos_T[] reg_startpos = new lpos_T[NSUBEXP];
  @NotNull private lpos_T[] reg_endpos = new lpos_T[NSUBEXP];
  //static win_T            *reg_win;
  private Editor reg_buf;
  private int reg_firstlnum;
  private int reg_maxline;

  private regsave_T behind_pos;

  @NotNull private CharPointer[] reg_startzp = new CharPointer[NSUBEXP];  /* Workspace to mark beginning */
  @NotNull private CharPointer[] reg_endzp = new CharPointer[NSUBEXP];    /*   and end of \z(...\) matches */
  @NotNull private lpos_T[] reg_startzpos = new lpos_T[NSUBEXP]; /* idem, beginning pos */
  @NotNull private lpos_T[] reg_endzpos = new lpos_T[NSUBEXP];   /* idem, end pos */

  private boolean got_int = false;

  /*
    * The arguments from BRACE_LIMITS are stored here.  They are actually local
    * to regmatch(), but they are here to reduce the amount of stack space used
    * (it can be called recursively many times).
    */
  private int bl_minval;
  private int bl_maxval;

  //private boolean can_f_submatch = false;      /* true when submatch() can be used */

  /* These pointers are used instead of reg_match and reg_mmatch for
    * reg_submatch().  Needed for when the substitution string is an expression
    * that contains a call to substitute() and submatch(). */
  //private regmatch_T       submatch_match;
  //private regmmatch_T      submatch_mmatch;

  private static Logger logger = Logger.getInstance(RegExp.class.getName());
}
