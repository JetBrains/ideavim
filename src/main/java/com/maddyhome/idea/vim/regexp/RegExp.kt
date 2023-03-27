package com.maddyhome.idea.vim.regexp

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineBuffer
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.diagnostic.VimLogger
import com.maddyhome.idea.vim.helper.Msg
import org.jetbrains.annotations.NonNls
import java.util.*

internal class RegExp {
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
  /* Obtain an operand that was stored as four bytes, MSB first. */ //    #define OPERAND_MIN(p)  (((long)(p)[3] << 24) + ((long)(p)[4] << 16) \
  //    + ((long)(p)[5] << 8) + (long)(p)[6])
  /* Obtain a second operand stored as four bytes. */ //    #define OPERAND_MAX(p)  OPERAND_MIN((p) + 4)
  /* Obtain a second single-byte operand stored after a four bytes operand. */ //    #define OPERAND_CMP(p)  (p)[7]
  /*
   * Utility definitions.
   */
  //    #define UCHARAT(p)      ((int)*(char_u *)(p))
  /* Used for an error (down from) vim_regcomp(): give the error message, set rc_did_emsg and return null */ /*
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
  // EMSG_M_RET_null("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL)
  private fun EMSG_RET_null(key: String) {
    injector.messages.showStatusBarMessage(null, injector.messages.message(key))
  }

  private fun EMSG_M_RET_null(key: String, isMagic: Boolean) {
    val `val` = if (isMagic) "" else "\\"
    injector.messages.showStatusBarMessage(null, injector.messages.message(key, `val`))
  }

  private fun EMSG_ONE_RET_null() {
    EMSG_M_RET_null(Msg.E369, reg_magic == MAGIC_ALL)
  }

  /*
   * Return NOT_MULTI if c is not a "multi" operator.
   * Return MULTI_ONE if c is a single "multi" operator.
   * Return MULTI_MULT if c is a multi "multi" operator.
   */
  private fun re_multi_type(c: Int): Int {
    if (c == Magic.AT || c == Magic.EQUAL || c == Magic.QUESTION) {
      return MULTI_ONE
    }
    return if (c == Magic.STAR || c == Magic.PLUS || c == Magic.LCURLY) {
      MULTI_MULT
    } else {
      NOT_MULTI
    }
  }

  /*
   * Translate '\x' to its control character, except "\n", which is Magic.
   */
  private fun backslash_trans(c: Int): Int {
    when (c) {
      'r'.code -> return '\r'.code
      't'.code -> return '\t'.code
      'e'.code -> return 0x1b
      'b'.code -> return '\b'.code
    }
    return c
  }

  /*
   * Return true if compiled regular expression "prog" can match a line break.
   */
  fun re_multiline(prog: regprog_T): Int {
    return prog.regflags and RF_HASNL
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
  fun vim_regcomp(expr: String?, magic: Int): regprog_T? {
    val r: regprog_T
    var scan: CharPointer
    var longest: CharPointer?
    var len: Int
    val flags = Flags()
    if (expr == null) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_null))
      return null
    }
    r = regprog_T()

    /*
     * Second pass: emit code.
     */regcomp_start(expr, magic)
    regcode = CharPointer(r.program)
    regc(REGMAGIC)
    if (reg(REG_NOPAREN, flags) == null) {
      return null
    }

    /* Dig out information for optimizations. */r.regstart = 0.toChar() /* Worst-case defaults. */
    r.reganch = 0.toChar()
    r.regmust = null
    r.regmlen = 0
    r.regflags = regflags
    if (flags.isSet(HASNL)) {
      r.regflags = r.regflags or RF_HASNL
    }
    /* Remember whether this pattern has any \z specials in it. */r.reghasz = re_has_z
    scan = CharPointer(r.program).ref(1) /* First BRANCH. */
    if (regnext(scan)!!.OP() == END) /* Only one top-level choice. */ {
      scan = scan.OPERAND()

      /* Starting-point info. */if (scan.OP() == BOL || scan.OP() == RE_BOF) {
        r.reganch++
        scan = regnext(scan)!!
      }
      if (scan.OP() == EXACTLY) {
        r.regstart = scan.OPERAND().charAt()
      } else if ((scan.OP() == BOW || scan.OP() == EOW || scan.OP() == NOTHING || scan.OP() == MOPEN || scan.OP() == NOPEN || scan.OP() == MCLOSE || scan.OP() == NCLOSE) &&
        regnext(scan)!!.OP() == EXACTLY
      ) {
        r.regstart = regnext(scan)!!.OPERAND().charAt()
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
       */if ((flags.isSet(SPSTART) || scan.OP() == BOW || scan.OP() == EOW) &&
        !flags.isSet(HASNL)
      ) {
        longest = null
        len = 0
        while (scan != null) {
          val so = scan.OPERAND()
          if (scan.OP() == EXACTLY && so.strlen() >= len) {
            longest = so.ref(0)
            len = so.strlen()
          }
          scan = regnext(scan)!!
        }
        if (longest != null) {
          r.regmust = longest.ref(0)
        }
        r.regmlen = len
      }
    }
    if (logger.isDebug()) logger.debug(regdump(expr, r))
    return r
  }

  /*
   * Setup to parse the regexp.  Used once to get the length and once to do it.
   */
  private fun regcomp_start(expr: String, magic: Int) {
    initchr(expr)
    reg_magic = if (magic != 0) {
      MAGIC_ON
    } else {
      MAGIC_OFF
    }
    num_complex_braces = 0
    regnpar = 1
    Arrays.fill(had_endbrace, false)
    regnzpar = 1
    re_has_z = 0.toChar()
    regflags = 0
    had_eol = false
  }

  /*
   * Check if during the previous call to vim_regcomp the EOL item "$" has been
   * found.  This is messy, but it works fine.
   */
  fun vim_regcomp_had_eol(): Boolean {
    return had_eol
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
  private fun reg(paren: Int, flagp: Flags): CharPointer? {
    var ret: CharPointer?
    var br: CharPointer?
    val ender: CharPointer
    var parno = 0
    val flags = Flags()
    flagp.init(HASWIDTH) /* Tentatively. */
    if (paren == REG_ZPAREN) {
      /* Make a ZOPEN node. */
      if (regnzpar >= NSUBEXP) {
        injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E50))
        return null
      }
      parno = regnzpar
      regnzpar++
      ret = regnode(ZOPEN + parno)
    } else if (paren == REG_PAREN) {
      /* Make a MOPEN node. */
      if (regnpar >= NSUBEXP) {
        EMSG_M_RET_null(Msg.E51, reg_magic == MAGIC_ALL)
        return null
      }
      parno = regnpar
      ++regnpar
      ret = regnode(MOPEN + parno)
    } else if (paren == REG_NPAREN) {
      /* Make a NOPEN node. */
      ret = regnode(NOPEN)
    } else {
      ret = null
    }

    /* Pick up the branches, linking them together. */br = regbranch(flags)
    if (br == null) {
      return null
    }
    if (ret != null) {
      regtail(ret, br) /* [MZ]OPEN -> first. */
    } else {
      ret = br.ref(0)
    }
    /* If one of the branches can be zero-width, the whole thing can.
     * If one of the branches has * at start or matches a line-break, the
     * whole thing can. */if (!flags.isSet(HASWIDTH)) {
      flagp.unset(HASWIDTH)
    }
    flagp.set(flags.get() and (SPSTART or HASNL))
    while (peekchr() == Magic.PIPE) {
      skipchr()
      br = regbranch(flags)
      if (br == null) {
        return null
      }
      regtail(ret, br) /* BRANCH -> BRANCH. */
      if (!flags.isSet(HASWIDTH)) {
        flagp.unset(HASWIDTH)
      }
      flagp.set(flags.get() and (SPSTART or HASNL))
    }

    /* Make a closing node, and hook it on the end. */ender =
      regnode(if (paren == REG_ZPAREN) ZCLOSE + parno else if (paren == REG_PAREN) MCLOSE + parno else if (paren == REG_NPAREN) NCLOSE else END)
    regtail(ret, ender)

    /* Hook the tails of the branches to the closing node. */br = ret.ref(0)
    while (br != null) {
      regoptail(br, ender)
      br = regnext(br)
    }

    /* Check for proper termination. */if (paren != REG_NOPAREN && getchr() != Magic.RPAREN) {
      return if (paren == REG_ZPAREN) {
        injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E52))
        null
      } else if (paren == REG_NPAREN) {
        EMSG_M_RET_null(Msg.E53, reg_magic == MAGIC_ALL)
        null
      } else {
        EMSG_M_RET_null(Msg.E54, reg_magic == MAGIC_ALL)
        null
      }
    } else if (paren == REG_NOPAREN && peekchr() != '\u0000'.code) {
      return if (curchr == Magic.LPAREN) {
        EMSG_M_RET_null(Msg.E55, reg_magic == MAGIC_ALL)
        null
      } else {
        injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_trailing))
        null
      }
      /* NOTREACHED */
    }
    /*
     * Here we set the flag allowing back references to this set of
     * parentheses.
     */if (paren == REG_PAREN) {
      had_endbrace[parno] = true /* have seen the close paren */
    }
    return ret
  }

  /*
   * regbranch - one alternative of an | operator
   *
   * Implements the & operator.
   */
  private fun regbranch(flagp: Flags): CharPointer? {
    val ret: CharPointer
    var chain: CharPointer? = null
    var latest: CharPointer?
    val flags = Flags()
    flagp.init(WORST or HASNL) /* Tentatively. */
    ret = regnode(BRANCH)
    while (true) {
      latest = regconcat(flags)
      if (latest == null) {
        return null
      }

      /* If one of the branches has width, the whole thing has.  If one of
       * the branches anchors at start-of-line, the whole thing does. */flagp.set(flags.get() and (HASWIDTH or SPSTART))
      /* If one of the branches doesn't match a line-break, the whole thing
       * doesn't. */flagp.set(flagp.get() and (HASNL.inv() or (flags.get() and HASNL)))
      chain?.let { regtail(it, latest) }
      if (peekchr() != Magic.AMP) {
        break
      }
      skipchr()
      regtail(latest, regnode(END)) /* operand ends */
      reginsert(MATCH, latest.ref(0))
      chain = latest.ref(0)
    }
    return ret
  }

  /*
   * regbranch - one alternative of an | or & operator
   *
   * Implements the concatenation operator.
   */
  private fun regconcat(flagp: Flags): CharPointer? {
    var first: CharPointer? = null
    var chain: CharPointer? = null
    var latest: CharPointer?
    val flags = Flags()
    var cont = true
    flagp.init(WORST) /* Tentatively. */
    while (cont) {
      when (peekchr()) {
        '\u0000'.code, Magic.PIPE, Magic.AMP, Magic.RPAREN -> cont = false
        Magic.c -> {
          regflags = regflags or RF_ICASE
          skipchr_keepstart()
        }
        Magic.C -> {
          regflags = regflags or RF_NOICASE
          skipchr_keepstart()
        }
        Magic.v -> {
          reg_magic = MAGIC_ALL
          skipchr_keepstart()
          curchr = -1
        }
        Magic.m -> {
          reg_magic = MAGIC_ON
          skipchr_keepstart()
          curchr = -1
        }
        Magic.M -> {
          reg_magic = MAGIC_OFF
          skipchr_keepstart()
          curchr = -1
        }
        Magic.V -> {
          reg_magic = MAGIC_NONE
          skipchr_keepstart()
          curchr = -1
        }
        else -> {
          latest = regpiece(flags)
          if (latest == null) {
            return null
          }
          flagp.set(flags.get() and (HASWIDTH or HASNL))
          chain /* First piece. */?.let { regtail(it, latest) }
            ?: flagp.set(flags.get() and SPSTART)
          chain = latest.ref(0)
          if (first == null) {
            first = latest.ref(0)
          }
        }
      }
    }
    if (first == null) /* Loop ran zero times. */ {
      first = regnode(NOTHING)
    }
    return first
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
  private fun regpiece(flagp: Flags): CharPointer? {
    val ret: CharPointer?
    val op: Int
    val next: CharPointer
    val flags = Flags()
    ret = regatom(flags)
    if (ret == null) {
      return null
    }
    op = peekchr()
    if (re_multi_type(op) == NOT_MULTI) {
      flagp.init(flags.get())
      return ret
    }
    if (!flags.isSet(HASWIDTH) && re_multi_type(op) == MULTI_MULT) {
      if (op == Magic.STAR) {
        EMSG_M_RET_null(Msg.E56, reg_magic >= MAGIC_ON)
        return null
      }
      if (op == Magic.PLUS) {
        EMSG_M_RET_null(Msg.E57, reg_magic == MAGIC_ALL)
        return null
      }
      /* "\{}" is checked below, it's allowed when there is an upper limit */
    }
    flagp.init(WORST or SPSTART or (flags.get() and HASNL)) /* default flags */
    skipchr()
    when (op) {
      Magic.STAR -> if (flags.isSet(SIMPLE)) {
        reginsert(STAR, ret.ref(0))
      } else {
        /* Emit x* as (x&|), where & means "self". */
        reginsert(BRANCH, ret.ref(0)) /* Either x */
        regoptail(ret, regnode(BACK)) /* and loop */
        regoptail(ret, ret) /* back */
        regtail(ret, regnode(BRANCH)) /* or */
        regtail(ret, regnode(NOTHING)) /* null. */
      }
      Magic.PLUS -> {
        if (flags.isSet(SIMPLE)) {
          reginsert(PLUS, ret.ref(0))
        } else {
          /* Emit x+ as x(&|), where & means "self". */
          next = regnode(BRANCH) /* Either */
          regtail(ret, next)
          regtail(regnode(BACK), ret) /* loop back */
          regtail(next, regnode(BRANCH)) /* or */
          regtail(ret, regnode(NOTHING)) /* null. */
        }
        flagp.init(WORST or HASWIDTH or (flags.get() and HASNL))
      }
      Magic.AT -> {
        var lop = END
        when (Magic.no_Magic(getchr())) {
          '='.code -> lop = MATCH
          '!'.code -> lop = NOMATCH
          '>'.code -> lop = SUBPAT
          '<'.code -> when (Magic.no_Magic(getchr())) {
            '='.code -> lop = BEHIND
            '!'.code -> lop = NOBEHIND
          }
        }
        if (lop == END) {
          EMSG_M_RET_null(Msg.E59, reg_magic == MAGIC_ALL)
          return null
        }
        /* Look behind must match with behind_pos. */if (lop == BEHIND || lop == NOBEHIND) {
          regtail(ret, regnode(BHPOS))
        }
        regtail(ret, regnode(END)) /* operand ends */
        reginsert(lop, ret.ref(0))
      }
      Magic.QUESTION, Magic.EQUAL -> {
        /* Emit x= as (x|) */reginsert(BRANCH, ret.ref(0)) /* Either x */
        regtail(ret, regnode(BRANCH)) /* or */
        next = regnode(NOTHING) /* null. */
        regtail(ret, next)
        regoptail(ret, next)
      }
      Magic.LCURLY -> {
        val limits = read_limits() ?: return null
        val maxval = limits.maxvalue
        val minval = limits.minvalue
        if (!flags.isSet(HASWIDTH) && (if (maxval > minval) maxval >= MAX_LIMIT else minval >= MAX_LIMIT)) {
          EMSG_M_RET_null(Msg.E58, reg_magic == MAGIC_ALL)
          return null
        }
        if (flags.isSet(SIMPLE)) {
          reginsert(BRACE_SIMPLE, ret.ref(0))
          reginsert_limits(BRACE_LIMITS, minval, maxval, ret.ref(0))
        } else {
          if (num_complex_braces >= 10) {
            EMSG_M_RET_null(Msg.E60, reg_magic == MAGIC_ALL)
            return null
          }
          reginsert(BRACE_COMPLEX + num_complex_braces, ret.ref(0))
          regoptail(ret, regnode(BACK))
          regoptail(ret, ret)
          reginsert_limits(BRACE_LIMITS, minval, maxval, ret.ref(0))
          ++num_complex_braces
        }
        if (minval > 0 && maxval > 0) {
          flagp.init(HASWIDTH or (flags.get() and HASNL))
        }
      }
    }
    if (re_multi_type(peekchr()) != NOT_MULTI) {
      /* Can't have a multi follow a multi. */
      if (peekchr() == Magic.STAR) {
        val `val` = if (reg_magic >= MAGIC_ON) "" else "\\"
        injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E61, `val`))
      } else {
        val `val` = if (reg_magic >= MAGIC_ON) "" else "\\"
        injector.messages.showStatusBarMessage(
          null,
          injector.messages.message(
            Msg.E62,
            `val`,
            Character.toString(Magic.no_Magic(peekchr()).toChar()),
          ),
        )
      }
      return null
    }
    return ret
  }

  /*
   * regatom - the lowest level
   *
   * Optimization:  gobbles an entire sequence of ordinary characters so that
   * it can turn them into a single node, which is smaller to store and
   * faster to run.  Don't do this when one_exactly is set.
   */
  private fun regatom(flagp: Flags): CharPointer? {
    var ret: CharPointer? = null
    val flags = Flags()
    val cpo_lit = false /* 'cpoptions' contains 'l' flag */
    var c: Int
    val classchars = ".iIkKfFpPsSdDxXoOwWhHaAlLuU"
    val classcodes = intArrayOf(
      ANY, IDENT, SIDENT, KWORD, SKWORD,
      FNAME, SFNAME, PRINT, SPRINT,
      WHITE, NWHITE, DIGIT, NDIGIT,
      HEX, NHEX, OCTAL, NOCTAL,
      WORD, NWORD, HEAD, NHEAD,
      ALPHA, NALPHA, LOWER, NLOWER,
      UPPER, NUPPER,
    )
    val p: CharPointer
    var extra = 0
    flagp.init(WORST) /* Tentatively. */
    c = getchr()
    var doCollection = false
    var doDefault = false
    when (c) {
      Magic.HAT -> ret = regnode(BOL)
      Magic.DOLLAR -> {
        ret = regnode(EOL)
        had_eol = true
      }
      Magic.LESS -> ret = regnode(BOW)
      Magic.GREATER -> ret = regnode(EOW)
      Magic.UNDER -> {
        c = Magic.no_Magic(getchr())
        if (c == '^'.code) /* "\_^" is start-of-line */ {
          ret = regnode(BOL)
        }
        if (c == '$'.code) /* "\_$" is end-of-line */ {
          ret = regnode(EOL)
          had_eol = true
        }
        extra = ADD_NL
        flagp.set(HASNL)

        /* "\_[" is character range plus newline */if (c == '['.code) {
          // goto collection;
          doCollection = true
        }
        val i = classchars.indexOf(Magic.no_Magic(c).toChar())
        if (i == -1) {
          injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E63))
          return null
        }
        ret = regnode(classcodes[i] + extra)
        flagp.set(HASWIDTH or SIMPLE)
      }
      Magic.DOT, Magic.i, Magic.I, Magic.k, Magic.K, Magic.f, Magic.F, Magic.p, Magic.P, Magic.s, Magic.S, Magic.d, Magic.D, Magic.x, Magic.X, Magic.o, Magic.O, Magic.w, Magic.W, Magic.h, Magic.H, Magic.a, Magic.A, Magic.l, Magic.L, Magic.u, Magic.U -> {
        val i = classchars.indexOf(Magic.no_Magic(c).toChar())
        if (i == -1) {
          injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E63))
          return null
        }
        ret = regnode(classcodes[i] + extra)
        flagp.set(HASWIDTH or SIMPLE)
      }
      Magic.n -> {
        ret = regnode(NEWL)
        flagp.set(HASWIDTH or HASNL)
      }
      Magic.LPAREN -> {
        if (one_exactly) {
          EMSG_ONE_RET_null()
          return null
        }
        ret = reg(REG_PAREN, flags)
        if (ret == null) {
          return null
        }
        flagp.set(flags.get() and (HASWIDTH or SPSTART or HASNL))
      }
      '\u0000'.code, Magic.PIPE, Magic.AMP, Magic.RPAREN -> {
        EMSG_RET_null(Msg.e_internal) /* Supposed to be caught earlier. */
        return null
      }
      Magic.EQUAL, Magic.QUESTION, Magic.PLUS, Magic.AT, Magic.LCURLY, Magic.STAR -> {
        c = Magic.no_Magic(c)
        val `val` = if (if (c == '*'.code) reg_magic >= MAGIC_ON else reg_magic == MAGIC_ALL) "" else "\\"
        injector.messages.showStatusBarMessage(
          null,
          injector.messages.message(
            Msg.E64,
            `val`,
            Character.toString(c.toChar()),
          ),
        )
        return null
      }
      Magic.TILDE -> if (reg_prev_sub != null) {
        val lp: CharPointer
        ret = regnode(EXACTLY)
        lp = reg_prev_sub.ref(0)
        while (!lp.isNul) {
          regc(lp.charAt().code)
        }
        lp.inc()
        regc('\u0000'.code)
        if (!reg_prev_sub.isNul) {
          flagp.set(HASWIDTH)
          if (lp.pointer() - reg_prev_sub.pointer() == 1) {
            flagp.set(SIMPLE)
          }
        }
      } else {
        injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_nopresub))
        return null
      }
      Magic.N1, Magic.N2, Magic.N3, Magic.N4, Magic.N5, Magic.N6, Magic.N7, Magic.N8, Magic.N9 -> {
        val refnum: Int
        refnum = c - Magic.N0
        /*
             * Check if the back reference is legal. We must have seen the
             * close brace.
             * TODO: Should also check that we don't refer to something
             * that is repeated (+*=): what instance of the repetition
             * should we match?
             */if (!had_endbrace[refnum]) {
          /* Trick: check if "@<=" or "@<!" follows, in which case
               * the \1 can appear before the referenced match. */
          p = regparse!!.ref(0)
          while (!p.isNul) {
            if (p.charAt(0) == '@' && p.charAt(1) == '<' && (p.charAt(2) == '!' || p.charAt(2) == '=')) {
              break
            }
            p.inc()
          }
          if (p.isNul) {
            EMSG_RET_null(Msg.E65)
            return null
          }
        }
        ret = regnode(BACKREF + refnum)
      }
      Magic.z -> {
        c = Magic.no_Magic(getchr())
        when (c) {
          '('.code -> {
            if (reg_do_extmatch != REX_SET) {
              injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E66))
              return null
            }
            if (one_exactly) {
              EMSG_ONE_RET_null()
              return null
            }
            ret = reg(REG_ZPAREN, flags)
            if (ret == null) {
              return null
            }
            flagp.set(flags.get() and (HASWIDTH or SPSTART or HASNL))
            re_has_z = REX_SET.toChar()
          }
          '1'.code, '2'.code, '3'.code, '4'.code, '5'.code, '6'.code, '7'.code, '8'.code, '9'.code -> {
            if (reg_do_extmatch != REX_USE) {
              injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E67))
              return null
            }
            ret = regnode(ZREF + c - '0'.code)
            re_has_z = REX_USE.toChar()
          }
          's'.code -> ret = regnode(MOPEN)
          'e'.code -> ret = regnode(MCLOSE)
          else -> {
            injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E68))
            return null
          }
        }
      }
      Magic.PERCENT -> {
        c = Magic.no_Magic(getchr())
        when (c) {
          '('.code -> {
            if (one_exactly) {
              EMSG_ONE_RET_null()
              return null
            }
            ret = reg(REG_NPAREN, flags)
            if (ret == null) {
              return null
            }
            flagp.set(flags.get() and (HASWIDTH or SPSTART or HASNL))
          }
          '^'.code -> ret = regnode(RE_BOF)
          '$'.code -> ret = regnode(RE_EOF)
          '#'.code -> ret = regnode(CURSOR)
          '['.code -> if (one_exactly) /* doesn't nest */ {
            EMSG_ONE_RET_null()
            return null
          } else {
            val lastbranch: CharPointer
            var lastnode: CharPointer? = null
            var br: CharPointer?
            ret = null
            while (getchr().also { c = it } != ']'.code) {
              if (c == '\u0000'.code) {
                EMSG_M_RET_null(Msg.E69, reg_magic == MAGIC_ALL)
                return null
              }
              br = regnode(BRANCH)
              if (ret == null) {
                ret = br.ref(0)
              } else {
                regtail(lastnode!!, br)
              }
              ungetchr()
              one_exactly = true
              lastnode = regatom(flagp)
              one_exactly = false
              if (lastnode == null) {
                return null
              }
            }
            if (ret == null) {
              EMSG_M_RET_null(Msg.E70, reg_magic == MAGIC_ALL)
              return null
            }
            lastbranch = regnode(BRANCH)
            br = regnode(NOTHING)
            regtail(lastnode!!, br)
            regtail(lastbranch, br)
            /* connect all branches to the NOTHING
                     * branch at the end */br = ret.ref(0)
            while (br !== lastnode) {
              br = if (br!!.OP() == BRANCH) {
                regtail(br, lastbranch)
                br.OPERAND()
              } else {
                regnext(br)
              }
            }
            flagp.unset(HASWIDTH)
          }
          else -> {
            if (Character.isDigit(c.toChar()) || c == '<'.code || c == '>'.code) {
              var n = 0
              val cmp: Int
              cmp = c
              if (cmp == '<'.code || cmp == '>'.code) {
                c = getchr()
              }
              while (Character.isDigit(c.toChar())) {
                n = n * 10 + (c - '0'.code)
                c = getchr()
              }
              if (c == 'l'.code || c == 'c'.code || c == 'v'.code) {
                ret = if (c == 'l'.code) {
                  regnode(RE_LNUM)
                } else if (c == 'c'.code) {
                  regnode(RE_COL)
                } else {
                  regnode(RE_VCOL)
                }

                /* put the number and the optional
                     * comparator after the opcode */regcode = re_put_long(regcode!!.ref(0), n)
                regcode!!.set(cmp.toChar()).inc()
              }
            }
            EMSG_M_RET_null(Msg.E71, reg_magic == MAGIC_ALL)
            return null
          }
        }
      }
      Magic.LBRACE -> doCollection = true
      else -> doDefault = true
    }
    if (doCollection) {
      val lp: CharPointer

      /*
      * If there is no matching ']', we assume the '[' is a normal
      * character.  This makes 'incsearch' and ":help [" work.
      */lp = skip_anyof(regparse!!.ref(0))
      if (lp.charAt() == ']') /* there is a matching ']' */ {
        var startc = -1 /* > 0 when next '-' is a range */
        var endc: Int

        /*
        * In a character class, different parsing rules apply.
        * Not even \ is special anymore, nothing is.
        */if (regparse!!.charAt() == '^') /* Complement of range. */ {
          ret = regnode(ANYBUT + extra)
          regparse!!.inc()
        } else {
          ret = regnode(ANYOF + extra)
        }

        /* At the start ']' and '-' mean the literal character. */if (regparse!!.charAt() == ']' || regparse!!.charAt() == '-') {
          regc(regparse!!.charAt().code)
          regparse!!.inc()
        }
        while (!regparse!!.isNul && regparse!!.charAt() != ']') {
          if (regparse!!.charAt() == '-') {
            regparse!!.inc()
            /* The '-' is not used for a range at the end and
            * after or before a '\n'. */if (regparse!!.isNul || regparse!!.charAt() == ']' || startc == -1 ||
              regparse!!.charAt(0) == '\\' && regparse!!.charAt(1) == 'n'
            ) {
              regc('-'.code)
              startc = '-'.code /* [--x] is a range */
            } else {
              // endc = *regparse++;
              endc = regparse!!.charAt().code
              regparse!!.inc()
              if (startc > endc) {
                injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_invrange))
                return null
              }
              while (++startc <= endc) {
                regc(startc)
              }
              startc = -1
            }
          } else if (regparse!!.charAt() == '\\' &&
            (
              REGEXP_INRANGE.indexOf(regparse!!.charAt(1)) != -1 || !cpo_lit &&
                REGEXP_ABBR.indexOf(regparse!!.charAt(1)) != -1
              )
          ) {
            regparse!!.inc()
            if (regparse!!.charAt() == 'n') {
              /* '\n' in range: also match NL */
              if (ret.charAt().code == ANYBUT) {
                ret.set((ANYBUT + ADD_NL).toChar())
              } else if (ret.charAt().code == ANYOF) {
                ret.set((ANYOF + ADD_NL).toChar())
              }
              /* else: must have had a \n already */flagp.set(HASNL)
              regparse!!.inc()
              startc = -1
            } else {
              startc = backslash_trans(regparse!!.charAt().code)
              regparse!!.inc()
              regc(startc)
            }
          } else if (regparse!!.charAt() == '[') {
            var c_class: Int
            var cu: Int
            c_class = skip_class_name(regparse!!)
            startc = -1
            when (c_class) {
              CharacterClasses.CLASS_NONE -> {
                /* literal '[', allow [[-x] as a range */startc = regparse!!.charAt().code
                regparse!!.inc()
                regc(startc)
              }
              CharacterClasses.CLASS_ALNUM -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isLetterOrDigit(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_ALPHA -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isLetter(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_BLANK -> {
                regc(' '.code)
                regc('\t'.code)
              }
              CharacterClasses.CLASS_CNTRL -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isISOControl(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_DIGIT -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isDigit(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_GRAPH -> {
                cu = 1
                while (cu <= 255) {
                  if (CharacterClasses.isGraph(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_LOWER -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isLowerCase(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_PRINT -> {
                cu = 1
                while (cu <= 255) {
                  if (CharacterClasses.isPrint(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_PUNCT -> {
                cu = 1
                while (cu <= 255) {
                  if (CharacterClasses.isPunct(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_SPACE -> {
                cu = 9
                while (cu <= 13) {
                  regc(cu)
                  cu++
                }
                regc(' '.code)
              }
              CharacterClasses.CLASS_UPPER -> {
                cu = 1
                while (cu <= 255) {
                  if (Character.isUpperCase(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_XDIGIT -> {
                cu = 1
                while (cu <= 255) {
                  if (CharacterClasses.isHex(cu.toChar())) {
                    regc(cu)
                  }
                  cu++
                }
              }
              CharacterClasses.CLASS_TAB -> regc('\t'.code)
              CharacterClasses.CLASS_RETURN -> regc('\r'.code)
              CharacterClasses.CLASS_BACKSPACE -> regc('\b'.code)
              CharacterClasses.CLASS_ESCAPE -> regc('\u001b'.code)
            }
          } else {
            startc = regparse!!.charAt().code
            regparse!!.inc()
            regc(startc)
          }
        }
        regc('\u0000'.code)
        prevchr_len = 1 /* last char was the ']' */
        if (regparse!!.charAt() != ']') {
          injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_toomsbra))
          return null
        }
        skipchr() /* let's be friends with the lexer again */
        flagp.set(HASWIDTH or SIMPLE)
      } else {
        doDefault = true
      }
    }
    /* FALLTHROUGH */if (doDefault) {
      var len: Int
      ret = regnode(EXACTLY)

      /*
      * Append characters as long as:
      * - there is no following multi, we then need the character in
      *   front of it as a single character operand
      * - not running into a Magic character
      * - "one_exactly" is not set
      * But always emit at least one character.  Might be a Multi,
      * e.g., a "[" without matching "]".
      */len = 0
      while (c != '\u0000'.code && (
          len == 0 || re_multi_type(peekchr()) == NOT_MULTI &&
            !one_exactly && !Magic.is_Magic(c)
          )
      ) {
        c = Magic.no_Magic(c)
        regc(c)
        c = getchr()
        ++len
      }
      ungetchr()
      regc('\u0000'.code)
      flagp.set(HASWIDTH)
      if (len == 1) {
        flagp.set(SIMPLE)
      }
    }
    return ret
  }

  /*
   * emit a node
   * Return pointer to generated code.
   */
  private fun regnode(op: Int): CharPointer {
    val ret: CharPointer
    ret = regcode!!.ref(0)
    regcode!!.set(op.toChar()).inc()
    regcode!!.set('\u0000').inc() /* Null "next" pointer. */
    regcode!!.set('\u0000').inc()
    return ret
  }

  /*
   * Emit (if appropriate) a byte of code
   */
  private fun regc(b: Int) {
    regcode!!.set(b.toChar()).inc()
  }

  /*
   * reginsert - insert an operator in front of already-emitted operand
   *
   * Means relocating the operand.
   */
  private fun reginsert(op: Int, opnd: CharPointer) {
    val src: CharPointer
    val dst: CharPointer
    val place: CharPointer
    src = regcode!!.ref(0)
    regcode!!.inc(3)
    dst = regcode!!.ref(0)
    while (src.pointer() > opnd.pointer()) {
      // *--dst = *--src;
      dst.dec().set(src.dec().charAt())
    }
    place = opnd.ref(0) /* Op node, where operand used to be. */
    place.set(op.toChar()).inc()
    place.set('\u0000').inc()
    place.set('\u0000')
  }

  /*
   * reginsert_limits - insert an operator in front of already-emitted operand.
   * The operator has the given limit values as operands.  Also set next pointer.
   *
   * Means relocating the operand.
   */
  private fun reginsert_limits(op: Int, minval: Int, maxval: Int, opnd: CharPointer) {
    val src: CharPointer
    val dst: CharPointer
    var place: CharPointer
    src = regcode!!.ref(0)
    regcode!!.inc(11)
    dst = regcode!!.ref(0)
    while (src.pointer() > opnd.pointer()) {
      // *--dst = *--src;
      dst.dec().set(src.dec().charAt())
    }
    place = opnd.ref(0) /* Op node, where operand used to be. */
    place.set(op.toChar()).inc()
    place.set('\u0000').inc()
    place.set('\u0000').inc()
    place = re_put_long(place.ref(0), minval)
    place = re_put_long(place.ref(0), maxval)
    regtail(opnd, place)
  }

  /*
   * Write a long as four bytes at "p" and return pointer to the next char.
   */
  private fun re_put_long(p: CharPointer, `val`: Int): CharPointer {
    p.set((`val` shr 24 and 0xff).toChar()).inc()
    p.set((`val` shr 16 and 0xff).toChar()).inc()
    p.set((`val` shr 8 and 0xff).toChar()).inc()
    p.set((`val` and 0xff).toChar()).inc()
    return p
  }

  /*
   * regtail - set the next-pointer at the end of a node chain
   */
  private fun regtail(p: CharPointer, `val`: CharPointer) {
    var scan: CharPointer
    val offset: Int

    /* Find last node. */scan = p.ref(0)
    while (true) {
      val temp = regnext(scan) ?: break
      scan = temp
    }
    offset = if (scan.OP() == BACK) {
      scan.pointer() - `val`.pointer()
    } else {
      `val`.pointer() - scan.pointer()
    }
    scan.ref(1).set((offset.toChar().code shr 8 and 0xff).toChar())
    scan.ref(2).set((offset and 0xff).toChar())
  }

  /*
   * regoptail - regtail on item after a BRANCH; nop if none
   */
  private fun regoptail(p: CharPointer?, `val`: CharPointer) {
    /* When op is neither BRANCH nor BRACE_COMPLEX0-9, it is "operandless" */
    if (p == null || p.OP() != BRANCH && (p.OP() < BRACE_COMPLEX || p.OP() > BRACE_COMPLEX + 9)) {
      return
    }
    regtail(p.OPERAND(), `val`)
  }

  private fun initchr(str: String) {
    regparse = CharPointer(str)
    prevchr_len = 0
    nextchr = -1
    prevchr = nextchr
    prevprevchr = prevchr
    curchr = prevprevchr
    at_start = true
    prev_at_start = false
  }

  private fun peekchr(): Int {
    if (curchr == -1) {
      when (regparse!!.charAt(0).also { curchr = it.code }) {
        '.', '[', '~' -> /* magic when 'magic' is on */if (reg_magic >= MAGIC_ON) {
          curchr = Magic.magic(curchr)
        }
        '(', ')', '{', '%', '+', '=', '?', '@', '!', '&', '|', '<', '>', '#', '"', '\'', ',', '-', ':', ';', '`', '/' -> /* magic only after "\v" */if (reg_magic == MAGIC_ALL) {
          curchr = Magic.magic(curchr)
        }
        '*' -> /* * is not magic as the very first character, eg "?*ptr" and when
                     * after '^', eg "/^*ptr" */if (reg_magic >= MAGIC_ON && !at_start && !(prev_at_start && prevchr == Magic.HAT)) {
          curchr = Magic.STAR
        }
        '^' -> /* '^' is only magic as the very first character and if it's after
                     * "\(", "\|", "\&' or "\n" */if (reg_magic >= MAGIC_OFF &&
          (
            at_start || reg_magic == MAGIC_ALL || prevchr == Magic.LPAREN || prevchr == Magic.PIPE || prevchr == Magic.AMP || prevchr == Magic.n || Magic.no_Magic(
              prevchr,
            ) == '('.code &&
              prevprevchr == Magic.PERCENT
            )
        ) {
          curchr = Magic.HAT
          at_start = true
          prev_at_start = false
        }
        '$' -> /* '$' is only magic as the very last char and if it's in front of
                     * either "\|", "\)", "\&", or "\n" */if (reg_magic >= MAGIC_OFF) {
          val p = regparse!!.ref(1)

          /* ignore \c \C \m and \M after '$' */while (p.charAt(0) == '\\' && (
              p.charAt(1) == 'c' || p.charAt(
                1,
              ) == 'C' || p.charAt(1) == 'm' || p.charAt(1) == 'M'
              )
          ) {
            p.inc(2)
          }
          if (p.charAt(0) == '\u0000' || p.charAt(0) == '\\' &&
            (p.charAt(1) == '|' || p.charAt(1) == '&' || p.charAt(1) == ')' || p.charAt(1) == 'n') || reg_magic == MAGIC_ALL
          ) {
            curchr = Magic.DOLLAR
          }
        }
        '\\' -> {
          val c = regparse!!.charAt(1).code
          if (c == '\u0000'.code) {
            curchr = '\\'.code /* trailing '\' */
          } else if (c <= '~'.code && META_flags[c] != 0) {
            /*
                 * META contains everything that may be magic sometimes,
                 * except ^ and $ ("\^" and "\$" are only magic after
                 * "\v").  We now fetch the next character and toggle its
                 * magicness.  Therefore, \ is so meta-magic that it is
                 * not in META.
                 */
            curchr = -1
            prev_at_start = at_start
            at_start = false /* be able to say "/\*ptr" */
            regparse!!.inc()
            peekchr()
            regparse!!.dec()
            curchr = Magic.toggle_Magic(curchr)
          } else if (REGEXP_ABBR.indexOf(c.toChar()) != -1) {
            /*
                 * Handle abbreviations, like "\t" for TAB -- webb
                 */
            curchr = backslash_trans(c)
          } else if (reg_magic == MAGIC_NONE && (c == '$'.code || c == '^'.code)) {
            curchr = Magic.toggle_Magic(c)
          } else {
            /*
                 * Next character can never be (made) magic?
                 * Then backslashing it won't do anything.
                 */
            curchr = c
          }
        }
      }
    }
    return curchr
  }

  /*
   * Eat one lexed character.  Do this in a way that we can undo it.
   */
  private fun skipchr() {
    /* peekchr() eats a backslash, do the same here */
    prevchr_len = if (regparse!!.charAt() == '\\') {
      1
    } else {
      0
    }
    if (regparse!!.charAt(prevchr_len) != '\u0000') {
      ++prevchr_len
    }
    regparse!!.inc(prevchr_len)
    prev_at_start = at_start
    at_start = false
    prevprevchr = prevchr
    prevchr = curchr
    curchr = nextchr /* use previously unget char, or -1 */
    nextchr = -1
  }

  /*
   * Skip a character while keeping the value of prev_at_start for at_start.
   * prevchr and prevprevchr are also kept.
   */
  private fun skipchr_keepstart() {
    val `as` = prev_at_start
    val pr = prevchr
    val prpr = prevprevchr
    skipchr()
    at_start = `as`
    prevchr = pr
    prevprevchr = prpr
  }

  private fun getchr(): Int {
    val chr = peekchr()
    skipchr()
    return chr
  }

  /*
   * put character back.  Works only once!
   */
  private fun ungetchr() {
    nextchr = curchr
    curchr = prevchr
    prevchr = prevprevchr
    at_start = prev_at_start
    prev_at_start = false

    /* Backup regparse, so that it's at the same position as before the
     * getchr(). */regparse!!.dec(prevchr_len)
  }

  /*
   * read_limits - Read two integers to be taken as a minimum and maximum.
   * If the first character is '-', then the range is reversed.
   * Should end with 'end'.  If minval is missing, zero is default, if maxval is
   * missing, a very big number is the default.
   */
  private fun read_limits(): MinMax? {
    var reverse = false
    val first_char: CharPointer
    var minval: Int
    var maxval: Int
    if (regparse!!.charAt() == '-') {
      /* Starts with '-', so reverse the range later */
      regparse!!.inc()
      reverse = true
    }
    first_char = regparse!!.ref(0)
    minval = getdigits(regparse!!)
    maxval = if (regparse!!.charAt() == ',') /* There is a comma */ {
      if (Character.isDigit(regparse!!.inc().charAt())) {
        getdigits(regparse!!)
      } else {
        MAX_LIMIT
      }
    } else if (Character.isDigit(first_char.charAt())) {
      minval /* It was \{n} or \{-n} */
    } else {
      MAX_LIMIT /* It was \{} or \{-} */
    }
    if (regparse!!.charAt() == '\\') {
      regparse!!.inc() /* Allow either \{...} or \{...\} */
    }
    if (regparse!!.charAt() != '}' || maxval == 0 && minval == 0) {
      val `val` = if (reg_magic == MAGIC_ALL) "" else "\\"
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.synerror, `val`))
      return null
    }

    /*
     * Reverse the range if there was a '-', or make sure it is in the right
     * order otherwise.
     */if (!reverse && minval > maxval || reverse && minval < maxval) {
      val tmp = minval
      minval = maxval
      maxval = tmp
    }
    skipchr() /* let's be friends with the lexer again */
    val res = MinMax()
    res.maxvalue = maxval
    res.minvalue = minval
    return res
  }

  private fun getdigits(p: CharPointer): Int {
    var res = 0
    var neg = false
    if (p.charAt() == '-') {
      neg = true
      p.inc()
    }
    while (Character.isDigit(p.charAt())) {
      res = res * 10 + p.charAt().digitToIntOrNull()!! ?: -1
      p.inc()
    }
    if (neg) {
      res = -res
    }
    return res
  }

  /*
   * vim_regexec and friends
   */
  /*
   * Get pointer to the line "lnum", which is relative to "reg_firstlnum".
   */
  private fun reg_getline(lnum: Int): CharPointer? {
    /* when looking behind for a match/no-match lnum is negative.  But we
     * can't go before line 1 */
    return if (reg_firstlnum + lnum < 0) {
      null
    } else {
      CharPointer(
        reg_buf!!.getLineBuffer(reg_firstlnum + lnum),
      )
    }

    // return ml_get_buf(reg_buf, reg_firstlnum + lnum, false);
  }

  /*
   * Match a regexp against a string.
   * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
   * Uses curbuf for line count and 'iskeyword'.
   *
   * Return true if there is a match, false if not.
   */
  fun vim_regexec(rmp: regmatch_T, line: CharPointer?, col: Int): Boolean {
    reg_match = rmp
    reg_mmatch = null
    reg_maxline = 0
    // reg_win = null;
    ireg_ic = rmp.rm_ic
    return vim_regexec_both(line, col) != 0
  }

  /*
   * Match a regexp against multiple lines.
   * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
   * Uses curbuf for line count and 'iskeyword'.
   *
   * Return zero if there is no match.  Return number of lines contained in the
   * match otherwise.
   */
  fun vim_regexec_multi(
    rmp: regmmatch_T, /*win_T win,*/
    buf: VimEditor?,
    lcount: Int,
    lnum: Int,
    col: Int,
  ): Int /* window in which to search or null */ /* buffer in which to search */ /* nr of line to start looking for match */ /* column to start looking for match */ {
    val r: Int
    // VimEditor save_curbuf = curbuf;
    reg_match = null
    reg_mmatch = rmp
    reg_buf = buf
    // reg_win = win;
    reg_firstlnum = lnum
    reg_maxline = lcount - lnum
    ireg_ic = rmp.rmm_ic

    /* Need to switch to buffer "buf" to make vim_iswordc() work. */
    // curbuf = buf;
    r = vim_regexec_both(null, col)
    // curbuf = save_curbuf;
    return r
  }

  fun vim_string_contains_regexp(rmp: regmmatch_T, string: String): Boolean {
    reg_match = null
    reg_mmatch = rmp
    ireg_ic = rmp.rmm_ic
    val prog: regprog_T?
    var s: CharPointer
    var retval = 0
    reg_tofree = null
    prog = reg_mmatch!!.regprog
    val line = CharPointer(string)
    reg_startpos = reg_mmatch!!.startpos
    reg_endpos = reg_mmatch!!.endpos

    /* Be paranoid... */if (prog == null) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_null))
      return false
    }

    /* Check validity of program. */if (prog_magic_wrong()) {
      return false
    }

    /* If pattern contains "\c" or "\C": overrule value of ireg_ic */if (prog.regflags and RF_ICASE != 0) {
      ireg_ic = true
    } else if (prog.regflags and RF_NOICASE != 0) {
      ireg_ic = false
    }

    /* If there is a "must appear" string, look for it. */if (prog.regmust != null) {
      val c: Char
      c = prog.regmust!!.charAt()
      s = line
      while (cstrchr(s, c).also { s = it!! } != null) {
        if (cstrncmp(s, prog.regmust!!, prog.regmlen) == 0) {
          break /* Found it. */
        }
        s.inc()
      }
      if (s == null) /* Not present. */ {
        // goto the end;
        return false
      }
    }
    regline = line.ref(0)
    reglnum = 0
    out_of_stack = false
    val col = 0
    /* Simplest case: Anchored match need be tried only once. */
    val c: Char
    c = regline!!.charAt(col)
    if (prog.regstart == '\u0000' || prog.regstart == c ||
      ireg_ic && prog.regstart.lowercaseChar() == c.lowercaseChar()
    ) {
      retval = regtry(prog, col)
    }
    if (out_of_stack) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E363))
    }
    return retval > 0
  }

  /*
   * Match a regexp against a string ("line" points to the string) or multiple
   * lines ("line" is null, use reg_getline()).
   */
  private fun vim_regexec_both(line: CharPointer?, col: Int): Int /* column to start looking for match */ {
    var line = line
    var col = col
    val prog: regprog_T?
    var s: CharPointer?
    var retval: Int
    reg_tofree = null
    retval = 0
    if (reg_match == null) {
      prog = reg_mmatch!!.regprog
      line = reg_getline(0)
      reg_startpos = reg_mmatch!!.startpos
      reg_endpos = reg_mmatch!!.endpos
    } else {
      prog = reg_match!!.regprog
      reg_startp = reg_match!!.startp
      reg_endp = reg_match!!.endp
    }

    /* Be paranoid... */if (prog == null || line == null) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_null))
      return retval
    }

    /* Check validity of program. */if (prog_magic_wrong()) {
      return retval
    }

    /* If pattern contains "\c" or "\C": overrule value of ireg_ic */if (prog.regflags and RF_ICASE != 0) {
      ireg_ic = true
    } else if (prog.regflags and RF_NOICASE != 0) {
      ireg_ic = false
    }

    /* If there is a "must appear" string, look for it. */if (prog.regmust != null) {
      val c: Char
      c = prog.regmust!!.charAt()
      s = line.ref(col)
      while (cstrchr(s!!, c).also { s = it } != null) {
        if (cstrncmp(s!!, prog.regmust!!, prog.regmlen) == 0) {
          break /* Found it. */
        }
        s!!.inc()
      }
      if (s == null) /* Not present. */ {
        // goto theend;
        return retval
      }
    }
    regline = line.ref(0)
    reglnum = 0
    out_of_stack = false

    /* Simplest case: Anchored match need be tried only once. */if (prog.reganch.code != 0) {
      val c: Char
      c = regline!!.charAt(col)
      retval = if (prog.regstart == '\u0000' || prog.regstart == c ||
        ireg_ic && prog.regstart.lowercaseChar() == c.lowercaseChar()
      ) {
        regtry(prog, col)
      } else {
        0
      }
    } else {
      /* Messy cases:  unanchored match. */
      while (!got_int && !out_of_stack) {
        if (prog.regstart != '\u0000') {
          /* Skip until the char we know it must start with. */
          s = cstrchr(regline!!.ref(col), prog.regstart)
          if (s == null) {
            retval = 0
            break
          }
          col = s!!.pointer() - regline!!.pointer()
        }
        retval = regtry(prog, col)
        if (retval > 0) {
          break
        }

        /* if not currently on the first line, get it again */if (reglnum != 0) {
          regline = reg_getline(0)
          reglnum = 0
        }
        if (regline!!.charAt(col) == '\u0000') {
          break
        }
        ++col
      }
    }
    if (out_of_stack) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.E363))
    }

    /* Didn't find a match. */
    // vim_free(reg_tofree);
    return retval
  }

  class reg_extmatch_T {
    var matches = arrayOfNulls<String>(NSUBEXP)
  }

  /*
   * Create a new extmatch and mark it as referenced once.
   */
  private fun make_extmatch(): reg_extmatch_T {
    return reg_extmatch_T()
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
  private fun regtry(prog: regprog_T, col: Int): Int {
    reginput = regline!!.ref(col)
    need_clear_subexpr = true
    /* Clear the external match subpointers if necessary. */if (prog.reghasz.code == REX_SET) {
      need_clear_zsubexpr = true
    }
    if (regmatch(CharPointer(prog.program).ref(1))) {
      cleanup_subexpr()
      if (reg_match == null) {
        if (reg_startpos[0]!!.lnum < 0) {
          reg_startpos[0]!!.lnum = 0
          reg_startpos[0]!!.col = col
        }
        if (reg_endpos[0]!!.lnum < 0) {
          reg_endpos[0]!!.lnum = reglnum
          reg_endpos[0]!!.col = reginput!!.pointer() - regline!!.pointer()
        }
      } else {
        if (reg_startp[0] == null) {
          reg_startp[0] = regline!!.ref(col)
        }
        if (reg_endp[0] == null) {
          reg_endp[0] = reginput
        }
      }
      /* Package any found \z(...\) matches for export. Default is none. */
      // unref_extmatch(re_extmatch_out);
      re_extmatch_out = null
      if (prog.reghasz.code == REX_SET) {
        var i: Int
        cleanup_zsubexpr()
        re_extmatch_out = make_extmatch()
        i = 0
        while (i < NSUBEXP) {
          if (reg_match == null) {
            /* Only accept single line matches. */
            if (reg_startzpos[i]!!.lnum >= 0 && reg_endzpos[i]!!.lnum == reg_startzpos[i]!!.lnum) {
              re_extmatch_out!!.matches[i] = reg_getline(
                reg_startzpos[i]!!.lnum,
              )!!.ref(reg_startzpos[i]!!.col).substring(
                reg_endzpos[i]!!.col - reg_startzpos[i]!!.col,
              )
            }
          } else {
            if (reg_startzp[i] != null && reg_endzp[i] != null) {
              re_extmatch_out!!.matches[i] = reg_startzp[i]!!
                .substring(reg_endzp[i]!!.pointer() - reg_startzp[i]!!.pointer())
            }
          }
          i++
        }
      }
      return 1 + reglnum
    }
    return 0
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
  private fun regmatch(scan: CharPointer?): Boolean {
    var scan = scan
    var next: CharPointer? /* Next node. */
    var op: Int
    var c: Char

    /* Some patterns my cause a long time to match, even though they are not
     * illegal.  E.g., "\([a-z]\+\)\+Q".  Allow breaking them with CTRL-C. */
    // fast_breakcheck(); - TODO
    while (scan != null) {
      if (got_int || out_of_stack) {
        return false
      }
      next = regnext(scan)
      op = scan.OP()
      /* Check for character class with NL added. */if (WITH_NL(op) && reginput!!.isNul && reglnum < reg_maxline) {
        reg_nextline()
      } else {
        if (WITH_NL(op)) {
          op -= ADD_NL
        }
        c = reginput!!.charAt()
        when (op) {
          BOL -> if (!reginput!!.equals(regline)) {
            return false
          }
          EOL -> if (c != '\u0000') {
            return false
          }
          RE_BOF -> /* Passing -1 to the getline() function provided for the search
                         * should always return null if the current line is the first
                         * line of the file. */if (reglnum != 0 || !reginput!!.equals(regline) || reg_match == null && reg_getline(
              -1,
            ) != null
          ) {
            return false
          }
          RE_EOF -> if (reglnum != reg_maxline || c != '\u0000') {
            return false
          }
          CURSOR -> {
            /* Check if the buffer is in a window and compare the
             * reg_win->w_cursor position to the match position. */
            val curpos = reg_buf!!.currentCaret().getBufferPosition()
            if (reglnum + reg_firstlnum != curpos.line ||
              reginput!!.pointer() - regline!!.pointer() != curpos.column
            ) {
              return false
            }
          }
          RE_LNUM -> if (reg_match != null || !re_num_cmp(
              reglnum + reg_firstlnum,
              scan,
            )
          ) {
            return false
          }
          RE_COL -> if (!re_num_cmp(reginput!!.pointer() - regline!!.pointer() + 1, scan)) {
            return false
          }
          RE_VCOL -> {
          }
          BOW -> if (c == '\u0000') /* Can't match at end of line */ {
            return false
          } else {
            if (!CharacterClasses.isWord(c) || reginput!!.pointer() > regline!!.pointer() &&
              CharacterClasses.isWord(reginput!!.charAt(-1))
            ) {
              return false
            }
          }
          EOW -> {
            if (reginput!!.equals(regline)) /* Can't match at start of line */ {
              return false
            }
            if (!CharacterClasses.isWord(reginput!!.charAt(-1))) {
              return false
            }
            if (!reginput!!.isNul && CharacterClasses.isWord(c)) {
              return false
            }
          }
          ANY -> {
            if (c == '\u0000') {
              return false
            }
            reginput!!.inc()
          }
          IDENT -> {
            if (!Character.isJavaIdentifierPart(c)) {
              return false
            }
            reginput!!.inc()
          }
          SIDENT -> {
            if (Character.isDigit(reginput!!.charAt()) || !Character.isJavaIdentifierPart(c)) {
              return false
            }
            reginput!!.inc()
          }
          KWORD -> {
            if (!CharacterClasses.isWord(reginput!!.charAt())) {
              return false
            }
            reginput!!.inc()
          }
          SKWORD -> {
            if (Character.isDigit(reginput!!.charAt()) || !CharacterClasses.isWord(reginput!!.charAt())) {
              return false
            }
            reginput!!.inc()
          }
          FNAME -> {
            if (!CharacterClasses.isFile(c)) {
              return false
            }
            reginput!!.inc()
          }
          SFNAME -> {
            if (Character.isDigit(reginput!!.charAt()) || !CharacterClasses.isFile(c)) {
              return false
            }
            reginput!!.inc()
          }
          PRINT -> {
            if (!CharacterClasses.isPrint(reginput!!.charAt())) {
              return false
            }
            reginput!!.inc()
          }
          SPRINT -> {
            if (Character.isDigit(reginput!!.charAt()) || !CharacterClasses.isPrint(reginput!!.charAt())) {
              return false
            }
            reginput!!.inc()
          }
          WHITE -> {
            if (!CharacterClasses.isWhite(c)) {
              return false
            }
            reginput!!.inc()
          }
          NWHITE -> {
            if (c == '\u0000' || CharacterClasses.isWhite(c)) {
              return false
            }
            reginput!!.inc()
          }
          DIGIT -> {
            if (!Character.isDigit(c)) {
              return false
            }
            reginput!!.inc()
          }
          NDIGIT -> {
            if (c == '\u0000' || Character.isDigit(c)) {
              return false
            }
            reginput!!.inc()
          }
          HEX -> {
            if (!CharacterClasses.isHex(c)) {
              return false
            }
            reginput!!.inc()
          }
          NHEX -> {
            if (c == '\u0000' || CharacterClasses.isHex(c)) {
              return false
            }
            reginput!!.inc()
          }
          OCTAL -> {
            if (!CharacterClasses.isOctal(c)) {
              return false
            }
            reginput!!.inc()
          }
          NOCTAL -> {
            if (c == '\u0000' || CharacterClasses.isOctal(c)) {
              return false
            }
            reginput!!.inc()
          }
          WORD -> {
            if (!CharacterClasses.isWord(c)) {
              return false
            }
            reginput!!.inc()
          }
          NWORD -> {
            if (c == '\u0000' || CharacterClasses.isWord(c)) {
              return false
            }
            reginput!!.inc()
          }
          HEAD -> {
            if (!CharacterClasses.isHead(c)) {
              return false
            }
            reginput!!.inc()
          }
          NHEAD -> {
            if (c == '\u0000' || CharacterClasses.isHead(c)) {
              return false
            }
            reginput!!.inc()
          }
          ALPHA -> {
            if (!CharacterClasses.isAlpha(c)) {
              return false
            }
            reginput!!.inc()
          }
          NALPHA -> {
            if (c == '\u0000' || CharacterClasses.isAlpha(c)) {
              return false
            }
            reginput!!.inc()
          }
          LOWER -> {
            if (!CharacterClasses.isLower(c)) {
              return false
            }
            reginput!!.inc()
          }
          NLOWER -> {
            if (c == '\u0000' || CharacterClasses.isLower(c)) {
              return false
            }
            reginput!!.inc()
          }
          UPPER -> {
            if (!CharacterClasses.isUpper(c)) {
              return false
            }
            reginput!!.inc()
          }
          NUPPER -> {
            if (c == '\u0000' || CharacterClasses.isUpper(c)) {
              return false
            }
            reginput!!.inc()
          }
          EXACTLY -> {
            var len: Int
            var opnd: CharPointer
            opnd = scan.OPERAND()
            /* Inline the first byte, for speed. */if (opnd.charAt() != reginput!!.charAt() && (
                !ireg_ic ||
                  opnd.charAt().lowercaseChar() != reginput!!.charAt().lowercaseChar()
                )
            ) {
              return false
            }
            if (opnd.charAt(1) == '\u0000') {
              reginput!!.inc() /* matched a single char */
            } else {
              len = opnd.strlen()
              /* Need to match first byte again for multi-byte. */if (cstrncmp(
                  opnd,
                  reginput!!,
                  len,
                ) != 0
              ) {
                return false
              }
              reginput!!.inc(len)
            }
          }
          ANYOF, ANYBUT -> {
            if (c == '\u0000') {
              return false
            }
            if (cstrchr(scan.OPERAND(), c) == null == (op == ANYOF)) {
              return false
            }
            reginput!!.inc()
          }
          NOTHING -> {
          }
          BACK -> {
          }
          MOPEN, MOPEN + 1, MOPEN + 2, MOPEN + 3, MOPEN + 4, MOPEN + 5, MOPEN + 6, MOPEN + 7, MOPEN + 8, MOPEN + 9 -> {
            val no: Int
            val save = save_se_T()
            no = op - MOPEN
            cleanup_subexpr()
            save_se(save, reg_startpos[no]!!, reg_startp[no])
            if (regmatch(next)) {
              return true
            }
            restore_se(save, reg_startpos[no]!!, reg_startp[no])
            return false
          }
          NOPEN, NCLOSE -> return regmatch(next)
          ZOPEN + 1, ZOPEN + 2, ZOPEN + 3, ZOPEN + 4, ZOPEN + 5, ZOPEN + 6, ZOPEN + 7, ZOPEN + 8, ZOPEN + 9 -> {
            val no: Int
            val save = save_se_T()
            no = op - ZOPEN
            cleanup_zsubexpr()
            save_se(save, reg_startzpos[no]!!, reg_startzp[no])
            if (regmatch(next)) {
              return true
            }
            restore_se(save, reg_startzpos[no]!!, reg_startzp[no])
            return false
          }
          MCLOSE, MCLOSE + 1, MCLOSE + 2, MCLOSE + 3, MCLOSE + 4, MCLOSE + 5, MCLOSE + 6, MCLOSE + 7, MCLOSE + 8, MCLOSE + 9 -> {
            val no: Int
            val save = save_se_T()
            no = op - MCLOSE
            cleanup_subexpr()
            save_se(save, reg_endpos[no]!!, reg_endp[no])
            if (regmatch(next)) {
              return true
            }
            restore_se(save, reg_endpos[no]!!, reg_endp[no])
            return false
          }
          ZCLOSE + 1, ZCLOSE + 2, ZCLOSE + 3, ZCLOSE + 4, ZCLOSE + 5, ZCLOSE + 6, ZCLOSE + 7, ZCLOSE + 8, ZCLOSE + 9 -> {
            val no: Int
            val save = save_se_T()
            no = op - ZCLOSE
            cleanup_zsubexpr()
            save_se(save, reg_endzpos[no]!!, reg_endzp[no])
            if (regmatch(next)) {
              return true
            }
            restore_se(save, reg_endzpos[no]!!, reg_endzp[no])
            return false
          }
          BACKREF + 1, BACKREF + 2, BACKREF + 3, BACKREF + 4, BACKREF + 5, BACKREF + 6, BACKREF + 7, BACKREF + 8, BACKREF + 9 -> {
            var no: Int
            var len: Int
            var clnum: Int
            var ccol: Int
            var p: CharPointer?
            no = op - BACKREF
            cleanup_subexpr()
            if (reg_match != null) /* Single-line regexp */ {
              if (reg_endp[no] == null) {
                /* Backref was not set: Match an empty string. */
                len = 0
              } else {
                /* Compare current input with back-ref in the same
                     * line. */
                len = reg_endp[no]!!.pointer() - reg_startp[no]!!.pointer()
                if (cstrncmp(reg_startp[no]!!, reginput!!, len) != 0) {
                  return false
                }
              }
            } else /* Multi-line regexp */ {
              if (reg_endpos[no]!!.lnum < 0) {
                /* Backref was not set: Match an empty string. */
                len = 0
              } else {
                if (reg_startpos[no]!!.lnum == reglnum &&
                  reg_endpos[no]!!.lnum == reglnum
                ) {
                  /* Compare back-ref within the current line. */
                  len = reg_endpos[no]!!.col - reg_startpos[no]!!.col
                  if (cstrncmp(regline!!.ref(reg_startpos[no]!!.col), reginput!!, len) != 0) {
                    return false
                  }
                } else {
                  /* Messy situation: Need to compare between two
                       * lines. */
                  ccol = reg_startpos[no]!!.col
                  clnum = reg_startpos[no]!!.lnum
                  while (true) {
                    /* Since getting one line may invalidate
                         * the other, need to make copy.  Slow! */if (!regline!!.equals(reg_tofree)) {
                      reg_tofree = regline!!.ref(0)
                      reginput = reg_tofree!!.ref(reginput!!.pointer() - regline!!.pointer())
                      regline = reg_tofree!!.ref(0)
                    }

                    /* Get the line to compare with. */p = reg_getline(clnum)
                    len = if (clnum == reg_endpos[no]!!.lnum) {
                      reg_endpos[no]!!.col - ccol
                    } else {
                      p!!.ref(ccol).strlen()
                    }
                    if (cstrncmp(p!!.ref(ccol), reginput!!, len) != 0) {
                      return false /* doesn't match */
                    }
                    if (clnum == reg_endpos[no]!!.lnum) {
                      break /* match and at end! */
                    }
                    if (reglnum == reg_maxline) {
                      return false /* text too short */
                    }

                    /* Advance to next line. */reg_nextline()
                    ++clnum
                    ccol = 0
                    if (got_int || out_of_stack) {
                      return false
                    }
                  }

                  /* found a match!  Note that regline may now point
                       * to a copy of the line, that should not matter. */
                }
              }
            }

            /* Matched the backref, skip over it. */reginput!!.inc(len)
          }
          ZREF + 1, ZREF + 2, ZREF + 3, ZREF + 4, ZREF + 5, ZREF + 6, ZREF + 7, ZREF + 8, ZREF + 9 -> {
            var no: Int
            var len: Int
            cleanup_zsubexpr()
            no = op - ZREF
            val match = re_extmatch_in!!.matches[no]
            if (re_extmatch_in != null && match != null) {
              len = match.length
              if (cstrncmp(CharPointer(match), reginput!!, len) != 0) {
                return false
              }
              reginput!!.inc(len)
            } else {
              /* Backref was not set: Match an empty string. */
            }
          }
          BRANCH -> {
            if (next!!.OP() != BRANCH) /* No choice. */ {
              next = scan.OPERAND() /* Avoid recursion. */
            } else {
              val save = regsave_T()
              do {
                reg_save(save)
                if (regmatch(scan!!.OPERAND())) {
                  return true
                }
                reg_restore(save)
                scan = regnext(scan)
              } while (scan != null && scan.OP() == BRANCH)
              return false
              /* NOTREACHED */
            }
          }
          BRACE_LIMITS -> {
            var no: Int
            if (next!!.OP() == BRACE_SIMPLE) {
              bl_minval = scan.OPERAND_MIN()
              bl_maxval = scan.OPERAND_MAX()
            } else if (next.OP() >= BRACE_COMPLEX &&
              next.OP() < BRACE_COMPLEX + 10
            ) {
              no = next.OP() - BRACE_COMPLEX
              brace_min[no] = scan.OPERAND_MIN()
              brace_max[no] = scan.OPERAND_MAX()
              brace_count[no] = 0
            } else {
              injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_internal))
              return false
            }
          }
          BRACE_COMPLEX, BRACE_COMPLEX + 1, BRACE_COMPLEX + 2, BRACE_COMPLEX + 3, BRACE_COMPLEX + 4, BRACE_COMPLEX + 5, BRACE_COMPLEX + 6, BRACE_COMPLEX + 7, BRACE_COMPLEX + 8, BRACE_COMPLEX + 9 -> {
            var no: Int
            val save = regsave_T()
            no = op - BRACE_COMPLEX
            ++brace_count[no]

            /* If not matched enough times yet, try one more */if (brace_count[no] <= (if (brace_min[no] <= brace_max[no]) brace_min[no] else brace_max[no])) {
              reg_save(save)
              if (regmatch(scan.OPERAND())) {
                return true
              }
              reg_restore(save)
              --brace_count[no] /* failed, decrement match count */
              return false
            }

            /* If matched enough times, may try matching some more */if (brace_min[no] <= brace_max[no]) {
              /* Range is the normal way around, use longest match */
              if (brace_count[no] <= brace_max[no]) {
                reg_save(save)
                if (regmatch(scan.OPERAND())) {
                  return true /* matched some more times */
                }
                reg_restore(save)
                --brace_count[no] /* matched just enough times */
                /* continue with the items after \{} */
              }
            } else {
              /* Range is backwards, use shortest match first */
              if (brace_count[no] <= brace_min[no]) {
                reg_save(save)
                if (regmatch(next)) {
                  return true
                }
                reg_restore(save)
                next = scan.OPERAND()
                /* must try to match one more item */
              }
            }
          }
          BRACE_SIMPLE, STAR, PLUS -> {
            val nextb: Char /* next byte */
            val nextb_ic: Char /* next byte reverse case */
            var count: Int
            val save = regsave_T()
            val minval: Int
            val maxval: Int

            /*
                 * Lookahead to avoid useless match attempts when we know
                 * what character comes next.
                 */if (next!!.OP() == EXACTLY) {
              nextb = next.OPERAND().charAt()
              nextb_ic = if (ireg_ic) {
                if (Character.isUpperCase(nextb)) {
                  nextb.lowercaseChar()
                } else {
                  nextb.uppercaseChar()
                }
              } else {
                nextb
              }
            } else {
              nextb = '\u0000'
              nextb_ic = '\u0000'
            }
            if (op != BRACE_SIMPLE) {
              minval = if (op == STAR) 0 else 1
              maxval = MAX_LIMIT
            } else {
              minval = bl_minval
              maxval = bl_maxval
            }

            /*
                 * When maxval > minval, try matching as much as possible, up
                 * to maxval.  When maxval < minval, try matching at least the
                 * minimal number (since the range is backwards, that's also
                 * maxval!).
                 */count = regrepeat(scan.OPERAND(), maxval)
            if (got_int) {
              return false
            }
            if (minval <= maxval) {
              /* Range is the normal way around, use longest match */
              while (count >= minval) {
                /* If it could match, try it. */
                if (nextb == '\u0000' || reginput!!.charAt() == nextb || reginput!!.charAt() == nextb_ic) {
                  reg_save(save)
                  if (regmatch(next)) {
                    return true
                  }
                  reg_restore(save)
                }
                /* Couldn't or didn't match -- back up one char. */if (--count < minval) {
                  break
                }
                if (reginput!!.equals(regline)) {
                  /* backup to last char of previous line */
                  --reglnum
                  regline = reg_getline(reglnum)
                  /* Just in case regrepeat() didn't count right. */if (regline == null) {
                    return false
                  }
                  reginput = regline!!.ref(regline!!.strlen())
                  // fast_breakcheck(); - TOOD
                  if (got_int || out_of_stack) {
                    return false
                  }
                } else {
                  reginput!!.dec()
                }
              }
            } else {
              /* Range is backwards, use shortest match first.
                   * Careful: maxval and minval are exchanged! */
              if (count < maxval) {
                return false
              }
              while (true) {
                /* If it could work, try it. */if (nextb == '\u0000' || reginput!!.charAt() == nextb || reginput!!.charAt() == nextb_ic) {
                  reg_save(save)
                  if (regmatch(next)) {
                    return true
                  }
                  reg_restore(save)
                }
                /* Couldn't or didn't match: try advancing one char. */if (count == minval || regrepeat(
                    scan.OPERAND(),
                    1,
                  ) == 0
                ) {
                  break
                }
                ++count
                if (got_int || out_of_stack) {
                  return false
                }
              }
            }
            return false
          }
          NOMATCH -> {
            val save = regsave_T()

            /* If the operand matches, we fail.  Otherwise backup and
                 * continue with the next item. */reg_save(save)
            if (regmatch(scan.OPERAND())) {
              return false
            }
            reg_restore(save)
          }
          MATCH, SUBPAT -> {
            val save = regsave_T()

            /* If the operand doesn't match, we fail.  Otherwise backup
                 * and continue with the next item. */reg_save(save)
            if (!regmatch(scan.OPERAND())) {
              return false
            }
            if (op == MATCH) /* zero-width */ {
              reg_restore(save)
            }
          }
          BEHIND, NOBEHIND -> {
            val save_after = regsave_T()
            val save_start = regsave_T()
            val save_behind_pos: regsave_T?
            val needmatch = op == BEHIND

            /*
                 * Look back in the input of the operand matches or not. This
                 * must be done at every position in the input and checking if
                 * the match ends at the current position.
                 * First check if the next item matches, that's probably
                 * faster.
                 */reg_save(save_start)
            if (regmatch(next)) {
              /* save the position after the found match for next */
              reg_save(save_after)

              /* start looking for a match with operand at the current
                   * postion.  Go back one character until we find the
                   * result, hitting the start of the line or the previous
                   * line (for multi-line matching).
                   * Set behind_pos to where the match should end, BHPOS
                   * will match it. */save_behind_pos = if (behind_pos == null) {
                null
              } else {
                regsave_T(
                  behind_pos!!,
                )
              }
              behind_pos = regsave_T(save_start)
              while (true) {
                reg_restore(save_start)
                if (regmatch(scan.OPERAND()) && reg_save_equal(behind_pos!!)) {
                  behind_pos = save_behind_pos
                  /* found a match that ends where "next" started */if (needmatch) {
                    reg_restore(save_after)
                    return true
                  }
                  return false
                }
                /*
                     * No match: Go back one character.  May go to
                     * previous line once.
                     */if (reg_match == null) {
                  if (save_start.pos.col == 0) {
                    if (save_start.pos.lnum < behind_pos!!.pos.lnum ||
                      reg_getline(--save_start.pos.lnum) == null
                    ) {
                      break
                    }
                    reg_restore(save_start)
                    save_start.pos.col = regline!!.strlen()
                  } else {
                    --save_start.pos.col
                  }
                } else {
                  if (save_start.ptr === regline) {
                    break
                  }
                  save_start.ptr!!.dec()
                }
              }

              /* NOBEHIND succeeds when no match was found */behind_pos = save_behind_pos
              if (!needmatch) {
                reg_restore(save_after)
                return true
              }
            }
            return false
          }
          BHPOS -> if (reg_match == null) {
            if (behind_pos!!.pos.col != reginput!!.pointer() - regline!!.pointer() ||
              behind_pos!!.pos.lnum != reglnum
            ) {
              return false
            }
          } else if (behind_pos!!.ptr !== reginput) {
            return false
          }
          NEWL -> {
            if (c != '\u0000' || reglnum == reg_maxline) {
              return false
            }
            reg_nextline()
          }
          END -> return true /* Success! */
          else -> {
            injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_re_corr))
            return false
          }
        }
      }
      scan = next
    }

    /*
     * We get here only if there's trouble -- normally "case END" is the
     * terminating point.
     */injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_re_corr))
    return false
  }

  /*
   * regrepeat - repeatedly match something simple, return how many.
   * Advances reginput (and reglnum) to just after the matched chars.
   */
  private fun regrepeat(p: CharPointer, maxcount: Int): Int {
    var count = 0
    var scan: CharPointer
    val opnd: CharPointer
    var mask = 0
    var testval = 0
    scan = reginput!!.ref(0) /* Make local copy of reginput for speed. */
    opnd = p.OPERAND()
    when (p.OP()) {
      ANY, ANY + ADD_NL -> while (count < maxcount) {
        /* Matching anything means we continue until end-of-line (or
             * end-of-file for ANY + ADD_NL), only limited by maxcount. */
        while (!scan.isNul && count < maxcount) {
          ++count
          scan.inc()
        }
        if (!WITH_NL(p.OP()) || reglnum == reg_maxline || count == maxcount) {
          break
        }
        ++count /* count the line-break */
        reg_nextline()
        scan = reginput!!.ref(0)
        if (got_int) {
          break
        }
      }
      IDENT, IDENT + ADD_NL -> {
        testval = 1
        while (count < maxcount) {
          if (Character.isJavaIdentifierPart(scan.charAt()) &&
            (testval == 1 || !Character.isDigit(scan.charAt()))
          ) {
            scan.inc()
          } else if (scan.isNul) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break
            }
            reg_nextline()
            scan = reginput!!.ref(0)
            if (got_int) {
              break
            }
          } else {
            break
          }
          ++count
        }
      }
      SIDENT, SIDENT + ADD_NL -> while (count < maxcount) {
        if (Character.isJavaIdentifierPart(scan.charAt()) &&
          (testval == 1 || !Character.isDigit(scan.charAt()))
        ) {
          scan.inc()
        } else if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else {
          break
        }
        ++count
      }
      KWORD, KWORD + ADD_NL -> {
        testval = 1
        while (count < maxcount) {
          if (CharacterClasses.isWord(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc()
          } else if (scan.isNul) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break
            }
            reg_nextline()
            scan = reginput!!.ref(0)
            if (got_int) {
              break
            }
          } else {
            break
          }
          ++count
        }
      }
      SKWORD, SKWORD + ADD_NL -> while (count < maxcount) {
        if (CharacterClasses.isWord(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
          scan.inc()
        } else if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else {
          break
        }
        ++count
      }
      FNAME, FNAME + ADD_NL -> {
        testval = 1
        while (count < maxcount) {
          if (CharacterClasses.isFile(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
            scan.inc()
          } else if (scan.isNul) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break
            }
            reg_nextline()
            scan = reginput!!.ref(0)
            if (got_int) {
              break
            }
          } else {
            break
          }
          ++count
        }
      }
      SFNAME, SFNAME + ADD_NL -> while (count < maxcount) {
        if (CharacterClasses.isFile(scan.charAt()) && (testval == 1 || !Character.isDigit(scan.charAt()))) {
          scan.inc()
        } else if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else {
          break
        }
        ++count
      }
      PRINT, PRINT + ADD_NL -> {
        testval = 1
        while (count < maxcount) {
          if (scan.isNul) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break
            }
            reg_nextline()
            scan = reginput!!.ref(0)
            if (got_int) {
              break
            }
          } else if (CharacterClasses.isPrint(scan.charAt()) &&
            (testval == 1 || !Character.isDigit(scan.charAt()))
          ) {
            scan.inc()
          } else {
            break
          }
          ++count
        }
      }
      SPRINT, SPRINT + ADD_NL -> while (count < maxcount) {
        if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else if (CharacterClasses.isPrint(scan.charAt()) &&
          (testval == 1 || !Character.isDigit(scan.charAt()))
        ) {
          scan.inc()
        } else {
          break
        }
        ++count
      }
      WHITE, WHITE + ADD_NL -> {
        mask = CharacterClasses.RI_WHITE
        testval = mask
      }
      NWHITE, NWHITE + ADD_NL -> mask = CharacterClasses.RI_WHITE
      DIGIT, DIGIT + ADD_NL -> {
        mask = CharacterClasses.RI_DIGIT
        testval = mask
      }
      NDIGIT, NDIGIT + ADD_NL -> mask = CharacterClasses.RI_DIGIT
      HEX, HEX + ADD_NL -> {
        mask = CharacterClasses.RI_HEX
        testval = mask
      }
      NHEX, NHEX + ADD_NL -> mask = CharacterClasses.RI_HEX
      OCTAL, OCTAL + ADD_NL -> {
        mask = CharacterClasses.RI_OCTAL
        testval = mask
      }
      NOCTAL, NOCTAL + ADD_NL -> mask = CharacterClasses.RI_OCTAL
      WORD, WORD + ADD_NL -> {
        mask = CharacterClasses.RI_WORD
        testval = mask
      }
      NWORD, NWORD + ADD_NL -> mask = CharacterClasses.RI_WORD
      HEAD, HEAD + ADD_NL -> {
        mask = CharacterClasses.RI_HEAD
        testval = mask
      }
      NHEAD, NHEAD + ADD_NL -> mask = CharacterClasses.RI_HEAD
      ALPHA, ALPHA + ADD_NL -> {
        mask = CharacterClasses.RI_ALPHA
        testval = mask
      }
      NALPHA, NALPHA + ADD_NL -> mask = CharacterClasses.RI_ALPHA
      LOWER, LOWER + ADD_NL -> {
        mask = CharacterClasses.RI_LOWER
        testval = mask
      }
      NLOWER, NLOWER + ADD_NL -> mask = CharacterClasses.RI_LOWER
      UPPER, UPPER + ADD_NL -> {
        mask = CharacterClasses.RI_UPPER
        testval = mask
      }
      NUPPER, NUPPER + ADD_NL -> mask = CharacterClasses.RI_UPPER
      EXACTLY -> {
        val cu: Char
        val cl: Char

        /* This doesn't do a multi-byte character, because a MULTIBYTECODE
             * would have been used for it. */if (ireg_ic) {
          cu = opnd.charAt().uppercaseChar()
          cl = opnd.charAt().lowercaseChar()
          while (count < maxcount && (scan.charAt() == cu || scan.charAt() == cl)) {
            count++
            scan.inc()
          }
        } else {
          cu = opnd.charAt()
          while (count < maxcount && scan.charAt() == cu) {
            count++
            scan.inc()
          }
        }
      }
      ANYOF, ANYOF + ADD_NL -> {
        testval = 1
        while (count < maxcount) {
          if (scan.isNul) {
            if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
              break
            }
            reg_nextline()
            scan = reginput!!.ref(0)
            if (got_int) {
              break
            }
          } else {
            if (cstrchr(opnd, scan.charAt()) == null == (testval == 1)) {
              break
            }
            scan.inc()
          }
          ++count
        }
      }
      ANYBUT, ANYBUT + ADD_NL -> while (count < maxcount) {
        if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else {
          if (cstrchr(opnd, scan.charAt()) == null == (testval == 1)) {
            break
          }
          scan.inc()
        }
        ++count
      }
      NEWL -> while (count < maxcount && scan.isNul && reglnum < reg_maxline) {
        count++
        reg_nextline()
        scan = reginput!!.ref(0)
        if (got_int) {
          break
        }
      }
      else -> injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_re_corr))
    }
    if (mask != 0) {
      while (count < maxcount) {
        if (scan.isNul) {
          if (!WITH_NL(p.OP()) || reglnum == reg_maxline) {
            break
          }
          reg_nextline()
          scan = reginput!!.ref(0)
          if (got_int) {
            break
          }
        } else if (CharacterClasses.isMask(scan.charAt(), mask, testval)) {
          scan.inc()
        } else {
          break
        }
        ++count
      }
    }
    reginput = scan.ref(0)
    return count
  }

  /*
   * regnext - dig the "next" pointer out of a node
   */
  private fun regnext(p: CharPointer): CharPointer? {
    val offset: Int
    offset = p.NEXT()
    if (offset == 0) {
      return null
    }
    return if (p.OP() == BACK) {
      p.ref(-offset)
    } else {
      p.ref(offset)
    }
  }

  /*
   * Check the regexp program for its magic number.
   * Return true if it's wrong.
   */
  private fun prog_magic_wrong(): Boolean {
    if ((if (reg_match == null) reg_mmatch!!.regprog!!.program else reg_match!!.regprog!!.program)[0].code != REGMAGIC) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_re_corr))
      return true
    }
    return false
  }

  /*
   * Cleanup the subexpressions, if this wasn't done yet.
   * This construction is used to clear the subexpressions only when they are
   * used (to increase speed).
   */
  private fun cleanup_subexpr() {
    if (need_clear_subexpr) {
      if (reg_match == null) {
        /* Use 0xff to set lnum to -1 */
        for (i in 0 until NSUBEXP) {
          reg_startpos[i]!!.col = -1
          reg_startpos[i]!!.lnum = -1
          reg_endpos[i]!!.col = -1
          reg_endpos[i]!!.lnum = -1
        }
      } else {
        for (i in 0 until NSUBEXP) {
          reg_startp[i] = null
          reg_endp[i] = null
        }
      }
      need_clear_subexpr = false
    }
  }

  private fun cleanup_zsubexpr() {
    if (need_clear_zsubexpr) {
      if (reg_match == null) {
        /* Use 0xff to set lnum to -1 */
        for (i in 0 until NSUBEXP) {
          reg_startzpos[i]!!.col = -1
          reg_startzpos[i]!!.lnum = -1
          reg_endzpos[i]!!.col = -1
          reg_endzpos[i]!!.lnum = -1
        }
      } else {
        for (i in 0 until NSUBEXP) {
          reg_startzp[i] = null
          reg_endzp[i] = null
        }
      }
      need_clear_zsubexpr = false
    }
  }

  /*
   * Advance reglnum, regline and reginput to the next line.
   */
  private fun reg_nextline() {
    regline = reg_getline(++reglnum)
    reginput = regline!!.ref(0)
    // fast_breakcheck(); TODO
  }

  /*
   * Save the input line and position in a regsave_T.
   */
  private fun reg_save(save: regsave_T) {
    if (reg_match == null) {
      save.pos.col = reginput!!.pointer() - regline!!.pointer()
      save.pos.lnum = reglnum
    } else {
      save.ptr = reginput!!.ref(0)
    }
  }

  /*
   * Restore the input line and position from a regsave_T.
   */
  private fun reg_restore(save: regsave_T) {
    if (reg_match == null) {
      if (reglnum != save.pos.lnum) {
        /* only call reg_getline() when the line number changed to save
         * a bit of time */
        reglnum = save.pos.lnum
        regline = reg_getline(reglnum)
      }
      reginput = regline!!.ref(save.pos.col)
    } else {
      reginput = save.ptr!!.ref(0)
    }
  }

  /*
   * Return true if current position is equal to saved position.
   */
  private fun reg_save_equal(save: regsave_T): Boolean {
    return if (reg_match == null) {
      reglnum == save.pos.lnum && reginput!!.equals(regline!!.ref(save.pos.col))
    } else {
      reginput!!.equals(save.ptr)
    }
  }

  /*
   * Tentatively set the sub-expression start to the current position (after
   * calling regmatch() they will have changed).  Need to save the existing
   * values for when there is no match.
   * Use pointer or position, depending on reg_match == null.
   */
  private fun save_se(savep: save_se_T, posp: lpos_T, pp: CharPointer?) {
    if (reg_match == null) {
      savep.pos.lnum = posp.lnum
      savep.pos.col = posp.col
      posp.lnum = reglnum
      posp.col = reginput!!.pointer() - regline!!.pointer()
    } else if (pp != null) {
      savep.ptr = pp.ref(0)
      pp.assign(reginput!!)
    }
  }

  /*
   * We were wrong, restore the sub-expressions.
   */
  private fun restore_se(savep: save_se_T, posp: lpos_T, pp: CharPointer?) {
    if (reg_match == null) {
      posp.col = savep.pos.col
      posp.lnum = savep.pos.lnum
    } else {
      pp?.assign(savep.ptr!!)
    }
  }

  /*
   * Compare a number with the operand of RE_LNUM, RE_COL or RE_VCOL.
   */
  private fun re_num_cmp(`val`: Int, scan: CharPointer): Boolean {
    val n = scan.OPERAND_MIN()
    if (scan.OPERAND_CMP() == '>') {
      return `val` > n
    }
    return if (scan.OPERAND_CMP() == '<') {
      `val` < n
    } else {
      `val` == n
    }
  }

  /*
   * Compare two strings, ignore case if ireg_ic set.
   * Return 0 if strings match, non-zero otherwise.
   */
  private fun cstrncmp(s1: CharPointer, s2: CharPointer, n: Int): Int {
    return s1.strncmp(s2, n, ireg_ic)
  }

  /*
   * cstrchr: This function is used a lot for simple searches, keep it fast!
   */
  private fun cstrchr(s: CharPointer, c: Char): CharPointer? {
    return if (!ireg_ic) {
      s.strchr(c)
    } else {
      s.istrchr(c)
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

    // return null;
  }
  /***************************************************************
   * regsub stuff                             *
   */
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
  // public CharPointer regtilde(CharPointer source, boolean magic)
  // {
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
  // }
  /**
   * vim_regsub() - perform substitutions after a vim_regexec() or
   * vim_regexec_multi() match.
   *
   *
   * If "copy" is true really copy into "dest".
   * If "copy" is false nothing is copied, this is just to find out the length
   * of the result.
   *
   *
   * If "backslash" is true, a backslash will be removed later, need to double
   * them to keep them, and insert a backslash before a CR to avoid it being
   * replaced with a line break later.
   *
   *
   * Note: The matched text must not change between the call of
   * vim_regexec()/vim_regexec_multi() and vim_regsub()!  It would make the back
   * references invalid!
   *
   *
   * Returns the size of the replacement, including terminating '\u0000'.
   */
  fun vim_regsub(rmp: regmatch_T?, source: CharPointer?, magic: Int, backslash: Boolean): String? {
    reg_match = rmp
    reg_mmatch = null
    reg_maxline = 0
    return vim_regsub_both(source, magic, backslash)
  }

  fun vim_regsub_multi(rmp: regmmatch_T?, lnum: Int, source: CharPointer?, magic: Int, backslash: Boolean): String? {
    reg_match = null
    reg_mmatch = rmp
    // reg_buf = curbuf;           /* always works on the current buffer! */
    reg_firstlnum = lnum
    reg_maxline = reg_buf!!.lineCount() - lnum
    return vim_regsub_both(source, magic, backslash)
  }

  private fun subappend(mode: Int, dst: StringBuffer, c: Char): Int {
    var mode = mode
    when (mode) {
      'u'.code -> {
        mode = 0
        dst.append(c.uppercaseChar())
      }
      'U'.code -> dst.append(c.uppercaseChar())
      'l'.code -> {
        mode = 0
        dst.append(c.lowercaseChar())
      }
      'L'.code -> dst.append(c.lowercaseChar())
      else -> dst.append(c)
    }
    return mode
  }

  private fun vim_regsub_both(source: CharPointer?, magic: Int, backslash: Boolean): String? {
    val src: CharPointer
    val dst = StringBuffer()
    var s: CharPointer?
    var c: Char
    var no = -1
    var clnum = 0 /* init for GCC */
    var len = 0 /* init for GCC */
    // CharPointer eval_result = null;
    var mode = 0

    /* Be paranoid... */if (source == null) {
      injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_null))
      return null
    }
    if (prog_magic_wrong()) {
      return null
    }
    src = source.ref(0)

    /*
     * When the substitute part starts with "\=" evaluate it as an expression.
     */if (source.charAt(0) == '\\' && source.charAt(1) == '=') {
    } else {
      while (src.charAt().also { c = it } != '\u0000') {
        src.inc()
        if (c == '&' && magic != 0) {
          no = 0
        } else if (c == '\\' && !src.isNul) {
          if (src.charAt() == '&' && magic == 0) {
            src.inc()
            no = 0
          } else if ('0' <= src.charAt() && src.charAt() <= '9') {
            no = src.charAt() - '0'
            src.inc()
          } else if ("uUlLeE".indexOf(src.charAt()) != -1) {
            when (src.charAtInc()) {
              'u' -> {
                mode = 'u'.code
                continue
              }
              'U' -> {
                mode = 'U'.code
                continue
              }
              'l' -> {
                mode = 'l'.code
                continue
              }
              'L' -> {
                mode = 'L'.code
                continue
              }
              'e', 'E' -> {
                mode = 0
                continue
              }
            }
          }
        }
        if (no < 0) /* Ordinary character. */ {
          if (c == '\\' && !src.isNul) {
            /* Check for abbreviations -- webb */
            // In vim '\u0000' is represented in memory as '\n', and '\n' as '\r', see :help NL-used-for-Nul
            when (src.charAt()) {
              'r' -> {
                c = '\n'
                src.inc()
              }
              'n' -> {
                c = '\u0000'
                src.inc()
              }
              't' -> {
                c = '\t'
                src.inc()
              }
              'b' -> {
                c = '\b'
                src.inc()
              }
              else -> {
                if (backslash) {
                  dst.append('\\')
                }
                c = src.charAt()
                src.inc()
              }
            }
          }

          /* Write to buffer, if copy is set. */mode = subappend(mode, dst, c)
        } else {
          if (reg_match == null) {
            clnum = reg_mmatch!!.startpos[no]!!.lnum
            if (clnum < 0 || reg_mmatch!!.endpos[no]!!.lnum < 0) {
              s = null
            } else {
              s = reg_getline(clnum)!!.ref(reg_mmatch!!.startpos[no]!!.col)
              len = if (reg_mmatch!!.endpos[no]!!.lnum == clnum) {
                reg_mmatch!!.endpos[no]!!.col - reg_mmatch!!.startpos[no]!!.col
              } else {
                s.strlen()
              }
            }
          } else {
            s = reg_match!!.startp[no]
            if (reg_match!!.endp[no] == null) {
              s = null
            } else {
              len = reg_match!!.endp[no]!!.pointer() - s!!.pointer()
            }
          }
          if (s != null) {
            while (true) {
              if (len == 0) {
                if (reg_match == null) {
                  if (reg_mmatch!!.endpos[no]!!.lnum == clnum) {
                    break
                  }
                  dst.append('\r')
                  s = reg_getline(++clnum)
                  len = if (reg_mmatch!!.endpos[no]!!.lnum == clnum) {
                    reg_mmatch!!.endpos[no]!!.col
                  } else {
                    s!!.strlen()
                  }
                } else {
                  break
                }
              } else if (s!!.isNul) /* we hit '\u0000'. */ {
                injector.messages.showStatusBarMessage(null, injector.messages.message(Msg.e_re_damg))
                return dst.toString()
              } else {
                if (backslash && (s.charAt() == '\r' || s.charAt() == '\\')) {
                  /*
                   * Insert a backslash in front of a CR, otherwise
                   * it will be replaced by a line break.
                   * Number of backslashes will be halved later,
                   * double them here.
                   */
                  dst.append('\\')
                  dst.append(s.charAt())
                } else {
                  mode = subappend(mode, dst, s.charAt())
                }
                s.inc()
                --len
              }
            }
          }
          no = -1
        }
      }
    }
    return dst.toString()
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
  private fun regdump(pattern: String, r: regprog_T): String {
    val start: CharPointer
    val s: CharPointer
    var op = EXACTLY /* Arbitrary non-END op. */
    var next: CharPointer?
    var end: CharPointer? = null
    val res = StringBuilder()
    res.append("\nregcomp(").append(pattern).append("):\n")
    start = CharPointer(r.program)
    s = start.ref(1)
    /*
    * Loop until we find the END that isn't before a referred next (an END
    * can also appear in a NOMATCH operand).
    */while (op != END || !s.end()) {
      op = s.OP()
      res.append(s.pointer() - start.pointer())
      res.append(regprop(s))
      next = regnext(s)
      if (next == null) /* Next ptr. */ {
        res.append("(0)")
      } else {
        res.append("(").append(s.pointer() - start.pointer() + (next.pointer() - s.pointer())).append(")")
      }
      if (end == null || next != null && end.pointer() < next.pointer()) {
        end = next
      }
      if (op == BRACE_LIMITS) {
        /* Two short ints */
        res.append(" minval ")
        res.append(s.OPERAND_MIN())
        res.append(", maxval ")
        res.append(s.OPERAND_MAX())
        s.inc(8)
      }
      s.inc(3)
      if (op == ANYOF || op == ANYOF + ADD_NL || op == ANYBUT || op == ANYBUT + ADD_NL || op == EXACTLY) {
        /* Literal string, where present. */
        while (!s.isNul) {
          res.append(s.charAt())
          s.inc()
        }
        s.inc()
      }
      res.append("\n")
    }

    /* Header fields of interest. */if (r.regstart != '\u0000') {
      res.append("start `")
      if (r.regstart < ' ') {
        res.append("^").append('@' + r.regstart.code)
      } else {
        res.append(r.regstart)
      }
      res.append("' ")
      res.append(Integer.toString(r.regstart.code, 16))
    }
    if (r.reganch.code != 0) {
      res.append("anchored: ")
    }
    if (r.regmust != null) {
      res.append("must have \"").append(r.regmust!!.substring(r.regmust!!.strlen())).append("\"")
    }
    res.append("\n")
    return res.toString()
  }

  /*
* regprop - printable representation of opcode
*/
  private fun regprop(op: CharPointer): @NonNls String {
    val p: @NonNls String?
    val buf = StringBuilder()
    buf.append(':')
    p = when (op.OP()) {
      BOL -> "BOL"
      EOL -> "EOL"
      RE_BOF -> "BOF"
      RE_EOF -> "EOF"
      CURSOR -> "CURSOR"
      RE_LNUM -> "RE_LNUM"
      RE_COL -> "RE_COL"
      RE_VCOL -> "RE_VCOL"
      BOW -> "BOW"
      EOW -> "EOW"
      ANY -> "ANY"
      ANY + ADD_NL -> "ANY+NL"
      ANYOF -> "ANYOF"
      ANYOF + ADD_NL -> "ANYOF+NL"
      ANYBUT -> "ANYBUT"
      ANYBUT + ADD_NL -> "ANYBUT+NL"
      IDENT -> "IDENT"
      IDENT + ADD_NL -> "IDENT+NL"
      SIDENT -> "SIDENT"
      SIDENT + ADD_NL -> "SIDENT+NL"
      KWORD -> "KWORD"
      KWORD + ADD_NL -> "KWORD+NL"
      SKWORD -> "SKWORD"
      SKWORD + ADD_NL -> "SKWORD+NL"
      FNAME -> "FNAME"
      FNAME + ADD_NL -> "FNAME+NL"
      SFNAME -> "SFNAME"
      SFNAME + ADD_NL -> "SFNAME+NL"
      PRINT -> "PRINT"
      PRINT + ADD_NL -> "PRINT+NL"
      SPRINT -> "SPRINT"
      SPRINT + ADD_NL -> "SPRINT+NL"
      WHITE -> "WHITE"
      WHITE + ADD_NL -> "WHITE+NL"
      NWHITE -> "NWHITE"
      NWHITE + ADD_NL -> "NWHITE+NL"
      DIGIT -> "DIGIT"
      DIGIT + ADD_NL -> "DIGIT+NL"
      NDIGIT -> "NDIGIT"
      NDIGIT + ADD_NL -> "NDIGIT+NL"
      HEX -> "HEX"
      HEX + ADD_NL -> "HEX+NL"
      NHEX -> "NHEX"
      NHEX + ADD_NL -> "NHEX+NL"
      OCTAL -> "OCTAL"
      OCTAL + ADD_NL -> "OCTAL+NL"
      NOCTAL -> "NOCTAL"
      NOCTAL + ADD_NL -> "NOCTAL+NL"
      WORD -> "WORD"
      WORD + ADD_NL -> "WORD+NL"
      NWORD -> "NWORD"
      NWORD + ADD_NL -> "NWORD+NL"
      HEAD -> "HEAD"
      HEAD + ADD_NL -> "HEAD+NL"
      NHEAD -> "NHEAD"
      NHEAD + ADD_NL -> "NHEAD+NL"
      ALPHA -> "ALPHA"
      ALPHA + ADD_NL -> "ALPHA+NL"
      NALPHA -> "NALPHA"
      NALPHA + ADD_NL -> "NALPHA+NL"
      LOWER -> "LOWER"
      LOWER + ADD_NL -> "LOWER+NL"
      NLOWER -> "NLOWER"
      NLOWER + ADD_NL -> "NLOWER+NL"
      UPPER -> "UPPER"
      UPPER + ADD_NL -> "UPPER+NL"
      NUPPER -> "NUPPER"
      NUPPER + ADD_NL -> "NUPPER+NL"
      BRANCH -> "BRANCH"
      EXACTLY -> "EXACTLY"
      NOTHING -> "NOTHING"
      BACK -> "BACK"
      END -> "END"
      MOPEN -> "MATCH START"
      MOPEN + 1, MOPEN + 2, MOPEN + 3, MOPEN + 4, MOPEN + 5, MOPEN + 6, MOPEN + 7, MOPEN + 8, MOPEN + 9 -> {
        buf.append("MOPEN").append(op.OP() - MOPEN)
        null
      }
      MCLOSE -> "MATCH END"
      MCLOSE + 1, MCLOSE + 2, MCLOSE + 3, MCLOSE + 4, MCLOSE + 5, MCLOSE + 6, MCLOSE + 7, MCLOSE + 8, MCLOSE + 9 -> {
        buf.append("MCLOSE").append(op.OP() - MCLOSE)
        null
      }
      BACKREF + 1, BACKREF + 2, BACKREF + 3, BACKREF + 4, BACKREF + 5, BACKREF + 6, BACKREF + 7, BACKREF + 8, BACKREF + 9 -> {
        buf.append("BACKREF").append(op.OP() - BACKREF)
        null
      }
      NOPEN -> "NOPEN"
      NCLOSE -> "NCLOSE"
      ZOPEN + 1, ZOPEN + 2, ZOPEN + 3, ZOPEN + 4, ZOPEN + 5, ZOPEN + 6, ZOPEN + 7, ZOPEN + 8, ZOPEN + 9 -> {
        buf.append("ZOPEN").append(op.OP() - ZOPEN)
        null
      }
      ZCLOSE + 1, ZCLOSE + 2, ZCLOSE + 3, ZCLOSE + 4, ZCLOSE + 5, ZCLOSE + 6, ZCLOSE + 7, ZCLOSE + 8, ZCLOSE + 9 -> {
        buf.append("ZCLOSE").append(op.OP() - ZCLOSE)
        null
      }
      ZREF + 1, ZREF + 2, ZREF + 3, ZREF + 4, ZREF + 5, ZREF + 6, ZREF + 7, ZREF + 8, ZREF + 9 -> {
        buf.append("ZREF").append(op.OP() - ZREF)
        null
      }
      STAR -> "STAR"
      PLUS -> "PLUS"
      NOMATCH -> "NOMATCH"
      MATCH -> "MATCH"
      BEHIND -> "BEHIND"
      NOBEHIND -> "NOBEHIND"
      SUBPAT -> "SUBPAT"
      BRACE_LIMITS -> "BRACE_LIMITS"
      BRACE_SIMPLE -> "BRACE_SIMPLE"
      BRACE_COMPLEX, BRACE_COMPLEX + 1, BRACE_COMPLEX + 2, BRACE_COMPLEX + 3, BRACE_COMPLEX + 4, BRACE_COMPLEX + 5, BRACE_COMPLEX + 6, BRACE_COMPLEX + 7, BRACE_COMPLEX + 8, BRACE_COMPLEX + 9 -> {
        buf.append("BRACE_COMPLEX").append(op.OP() - BRACE_COMPLEX)
        null
      }
      NEWL -> "NEWL"
      else -> {
        buf.append("corrupt ").append(op.OP())
        null
      }
    }
    if (p != null) {
      buf.append(p)
    }
    return buf.toString()
  }

  class regprog_T {
    var regstart = 0.toChar()
    var reganch = 0.toChar()
    var regmust: CharPointer? = null
    var regmlen = 0
    var regflags = 0
    var reghasz = 0.toChar()
    var program = StringBuffer()
  }

  private class MinMax {
    var minvalue = 0
    var maxvalue = 0
  }

  class lpos_T {
    constructor(pos: lpos_T) {
      lnum = pos.lnum
      col = pos.col
    }

    constructor() {}

    @JvmField
    var lnum = 0

    @JvmField
    var col = 0
    override fun toString(): String {
      return "lpos: ($lnum, $col)"
    }
  }

  /*
  * Structure used to save the current input state, when it needs to be
  * restored after trying a match.  Used by reg_save() and reg_restore().
  */
  private class regsave_T {
    var ptr /* reginput pointer, for single-line regexp */: CharPointer? = null
    var pos = lpos_T() /* reginput pos, for multi-line regexp */

    constructor() {}
    constructor(rhs: regsave_T) {
      ptr = if (rhs.ptr == null) null else CharPointer("").assign(rhs.ptr!!)
      pos = lpos_T(rhs.pos)
    }
  }

  /* struct to save start/end pointer/position in for \(\) */
  private class save_se_T {
    var ptr: CharPointer? = null
    var pos = lpos_T()
  }

  class regmatch_T {
    var regprog: regprog_T? = null
    var startp = arrayOfNulls<CharPointer>(NSUBEXP)
    var endp = arrayOfNulls<CharPointer>(NSUBEXP)
    var rm_ic = false
  }

  class regmmatch_T {
    @JvmField
    var regprog: regprog_T? = null

    @JvmField
    var startpos = arrayOfNulls<lpos_T>(NSUBEXP)

    @JvmField
    var endpos = arrayOfNulls<lpos_T>(NSUBEXP)

    @JvmField
    var rmm_ic = false

    init {
      for (i in 0 until NSUBEXP) {
        startpos[i] = lpos_T()
        endpos[i] = lpos_T()
      }
    }
  }

  private val reg_do_extmatch = 0
  private val reg_prev_sub: CharPointer? = null
  private var regparse /* Input-scan pointer. */: CharPointer? = null
  private var prevchr_len /* byte length of previous char */ = 0
  private var num_complex_braces /* Complex \{...} count */ = 0
  private var regnpar /* () count. */ = 0
  private var regnzpar /* \z() count. */ = 0
  private var re_has_z /* \z item detected */ = 0.toChar()
  private var regcode /* Code-emit pointer */: CharPointer? = null
  private val had_endbrace = BooleanArray(NSUBEXP) /* flags, true if end of () found */
  private var regflags /* RF_ flags for prog */ = 0
  private val brace_min = IntArray(10) /* Minimums for complex brace repeats */
  private val brace_max = IntArray(10) /* Maximums for complex brace repeats */
  private val brace_count = IntArray(10) /* Current counts for complex brace repeats */
  private var had_eol /* true when EOL found by vim_regcomp() */ = false
  private var one_exactly = false /* only do one char for EXACTLY */
  private var reg_magic /* magicness of the pattern: */ = 0
  private var curchr = 0

  /*
  * getchr() - get the next character from the pattern. We know about
  * magic and such, so therefore we need a lexical analyzer.
  */
  /* static int       curchr; */
  private var prevprevchr = 0
  private var prevchr = 0
  private var nextchr /* used for ungetchr() */ = 0

  /*
  * Note: prevchr is sometimes -1 when we are not at the start,
  * eg in /[ ^I]^ the pattern was never found even if it existed, because ^ was
  * taken to be magic -- webb
  */
  private var at_start /* True when on the first character */ = false
  private var prev_at_start /* True when on the second character */ = false

  /*
  * Global work variables for vim_regexec().
  */
  /* The current match-position is remembered with these variables: */
  private var reglnum /* line number, relative to first line */ = 0
  private var regline /* start of current line */: CharPointer? = null
  private var reginput /* current input, points into "regline" */: CharPointer? = null
  private var need_clear_subexpr /* subexpressions still need to be
    * cleared */ = false
  private var need_clear_zsubexpr = false /* extmatch subexpressions
    * still need to be cleared */
  private var out_of_stack /* true when ran out of stack space */ = false

  /*
  * Internal copy of 'ignorecase'.  It is set at each call to vim_regexec().
  * Normally it gets the value of "rm_ic" or "rmm_ic", but when the pattern
  * contains '\c' or '\C' the value is overruled.
  */
  private var ireg_ic = false

  /*
  * Sometimes need to save a copy of a line.  Since alloc()/free() is very
  * slow, we keep one allocated piece of memory and only re-allocate it when
  * it's too small.  It's freed in vim_regexec_both() when finished.
  */
  private var reg_tofree: CharPointer? = null

  // private int reg_tofreelen;
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
  private var reg_match: regmatch_T? = null
  private var reg_mmatch: regmmatch_T? = null
  private var reg_startp = arrayOfNulls<CharPointer>(NSUBEXP)
  private var reg_endp = arrayOfNulls<CharPointer>(NSUBEXP)
  private var reg_startpos = arrayOfNulls<lpos_T>(NSUBEXP)
  private var reg_endpos = arrayOfNulls<lpos_T>(NSUBEXP)

  // static win_T            *reg_win;
  private var reg_buf: VimEditor? = null
  private var reg_firstlnum = 0
  private var reg_maxline = 0
  private var behind_pos: regsave_T? = null
  private val reg_startzp = arrayOfNulls<CharPointer>(NSUBEXP) /* Workspace to mark beginning */
  private val reg_endzp = arrayOfNulls<CharPointer>(NSUBEXP) /*   and end of \z(...\) matches */
  private val reg_startzpos = arrayOfNulls<lpos_T>(NSUBEXP) /* idem, beginning pos */
  private val reg_endzpos = arrayOfNulls<lpos_T>(NSUBEXP) /* idem, end pos */
  private val got_int = false

  /*
  * The arguments from BRACE_LIMITS are stored here.  They are actually local
  * to regmatch(), but they are here to reduce the amount of stack space used
  * (it can be called recursively many times).
  */
  private var bl_minval = 0
  private var bl_maxval = 0

  companion object {
    var re_extmatch_out: reg_extmatch_T? = null
    var re_extmatch_in: reg_extmatch_T? = null

    /*
 * The opcodes are:
 */
    /* definition                   number             opnd?    meaning */
    private const val END = 0 /*      End of program or NOMATCH operand. */
    private const val BOL = 1 /*      Match "" at beginning of line. */
    private const val EOL = 2 /*      Match "" at end of line. */
    private const val BRANCH = 3 /* node Match this alternative, or the next... */
    private const val BACK = 4 /*      Match "", "next" ptr points backward. */
    private const val EXACTLY = 5 /* str  Match this string. */
    private const val NOTHING = 6 /*      Match empty string. */
    private const val STAR = 7 /* node Match this (simple) thing 0 or more times. */
    private const val PLUS = 8 /* node Match this (simple) thing 1 or more times. */
    private const val MATCH = 9 /* node match the operand zero-width */
    private const val NOMATCH = 10 /* node check for no match with operand */
    private const val BEHIND = 11 /* node look behind for a match with operand */
    private const val NOBEHIND = 12 /* node look behind for no match with operand */
    private const val SUBPAT = 13 /* node match the operand here */
    private const val BRACE_SIMPLE = 14 /* node Match this (simple) thing between m and
                                                  *      n times (\{m,n\}). */
    private const val BOW = 15 /*      Match "" after [^a-zA-Z0-9_] */
    private const val EOW = 16 /*      Match "" at    [^a-zA-Z0-9_] */
    private const val BRACE_LIMITS = 17 /* nr nr  define the min & max for BRACE_SIMPLE
                                                  *      and BRACE_COMPLEX. */
    private const val NEWL = 18 /*      Match line-break */
    private const val BHPOS = 19 /*      End position for BEHIND or NOBEHIND */

    /* character classes: 20-48 normal, 50-78 include a line-break */
    private const val ADD_NL = 30
    private const val ANY = 20 /*      Match any one character. */
    private const val FIRST_NL = ANY + ADD_NL
    private const val ANYOF = 21 /* str  Match any character in this string. */
    private const val ANYBUT = 22 /* str  Match any character not in this
                                                  *      string. */
    private const val IDENT = 23 /*      Match identifier char */
    private const val SIDENT = 24 /*      Match identifier char but no digit */
    private const val KWORD = 25 /*      Match keyword char */
    private const val SKWORD = 26 /*      Match word char but no digit */
    private const val FNAME = 27 /*      Match file name char */
    private const val SFNAME = 28 /*      Match file name char but no digit */
    private const val PRINT = 29 /*      Match printable char */
    private const val SPRINT = 30 /*      Match printable char but no digit */
    private const val WHITE = 31 /*      Match whitespace char */
    private const val NWHITE = 32 /*      Match non-whitespace char */
    private const val DIGIT = 33 /*      Match digit char */
    private const val NDIGIT = 34 /*      Match non-digit char */
    private const val HEX = 35 /*      Match hex char */
    private const val NHEX = 36 /*      Match non-hex char */
    private const val OCTAL = 37 /*      Match octal char */
    private const val NOCTAL = 38 /*      Match non-octal char */
    private const val WORD = 39 /*      Match word char */
    private const val NWORD = 40 /*      Match non-word char */
    private const val HEAD = 41 /*      Match head char */
    private const val NHEAD = 42 /*      Match non-head char */
    private const val ALPHA = 43 /*      Match alpha char */
    private const val NALPHA = 44 /*      Match non-alpha char */
    private const val LOWER = 45 /*      Match lowercase char */
    private const val NLOWER = 46 /*      Match non-lowercase char */
    private const val UPPER = 47 /*      Match uppercase char */
    private const val NUPPER = 48 /*      Match non-uppercase char */
    private const val LAST_NL = NUPPER + ADD_NL
    private const val MOPEN = 80 /* -89       Mark this point in input as start of
                                                 *       \( subexpr.  MOPEN + 0 marks start of
                                                 *       match. */
    private const val MCLOSE = 90 /* -99       Analogous to MOPEN.  MCLOSE + 0 marks
                                                 *       end of match. */
    private const val BACKREF = 100 /* -109 node Match same string again \1-\9 */
    private const val ZOPEN = 110 /* -119      Mark this point in input as start of
                                                 *       \z( subexpr. */
    private const val ZCLOSE = 120 /* -129      Analogous to ZOPEN. */
    private const val ZREF = 130 /* -139 node Match external submatch \z1-\z9 */
    private const val BRACE_COMPLEX = 140 /* -149 node Match nodes between m & n times */
    private const val NOPEN = 150 /*      Mark this point in input as start of
                                                        \%( subexpr. */
    private const val NCLOSE = 151 /*      Analogous to NOPEN. */
    private const val RE_BOF = 201 /*      Match "" at beginning of file. */
    private const val RE_EOF = 202 /*      Match "" at end of file. */
    private const val CURSOR = 203 /*      Match location of cursor. */
    private const val RE_LNUM = 204 /* nr cmp  Match line number */
    private const val RE_COL = 205 /* nr cmp  Match column number */
    private const val RE_VCOL = 206 /* nr cmp  Match virtual column number */
    private const val REGMAGIC = 156
    private const val REX_SET = 1
    private const val REX_USE = 2
    private const val MAX_LIMIT = Int.MAX_VALUE
    private const val NOT_MULTI = 0
    private const val MULTI_ONE = 1
    private const val MULTI_MULT = 2

    /*
* Flags to be passed up and down.
*/
    private const val HASWIDTH = 0x1 /* Known never to match null string. */
    private const val SIMPLE = 0x2 /* Simple enough to be STAR/PLUS operand. */
    private const val SPSTART = 0x4 /* Starts with * or +. */
    private const val HASNL = 0x8 /* Contains some \n. */
    private const val WORST = 0 /* Worst case. */

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
    private const val REGEXP_INRANGE = "]^-n\\"
    private const val REGEXP_ABBR = "nrteb"

    /* flags for regflags */
    private const val RF_ICASE = 1 /* ignore case */
    private const val RF_NOICASE = 2 /* don't ignore case */
    private const val RF_HASNL = 4 /* can match a NL */

    /*
 * Global work variables for vim_regcomp().
 */
    private const val NSUBEXP = 10
    private const val MAGIC_NONE = 1 /* "\V" very unmagic */
    private const val MAGIC_OFF = 2 /* "\M" or 'magic' off */
    private const val MAGIC_ON = 3 /* "\m" or 'magic' */
    private const val MAGIC_ALL = 4 /* "\v" very magic */

    /*
 * META contains all characters that may be magic, except '^' and '$'.
 */
    /* META[] is used often enough to justify turning it into a table. */
    private val META_flags = intArrayOf(
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, /*                 %  &     (  )  *  +        .    */
      0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, /*     1  2  3  4  5  6  7  8  9        <  =  >  ? */
      0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, /*  @  A     C  D     F     H  I     K  L  M     O */
      1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, /*  P        S     U  V  W  X        [           _ */
      1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, /*     a     c  d     f     h  i     k  l  m  n  o */
      0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, /*  p        s     u  v  w  x     z  {  |     ~    */
      1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1,
    )

    /* arguments for reg() */
    private const val REG_NOPAREN = 0 /* toplevel reg() */
    private const val REG_PAREN = 1 /* \(\) */
    private const val REG_ZPAREN = 2 /* \z(\) */
    private const val REG_NPAREN = 3 /* \%(\) */
    private fun WITH_NL(op: Int): Boolean {
      return op >= FIRST_NL && op <= LAST_NL
    }

    /*
 * Check for a character class name.  "pp" points to the '['.
 * Returns one of the CLASS_ items. CLASS_NONE means that no item was
 * recognized.  Otherwise "pp" is advanced to after the item.
 */
    private fun skip_class_name(pp: CharPointer): Int {
      var i: Int
      if (pp.charAt(1) == ':') {
        i = 0
        while (i < (CharacterClasses.CLASS_NAMES?.size ?: 0)) {
          if (pp.ref(2)
              .strncmp(CharacterClasses.CLASS_NAMES!![i], CharacterClasses.CLASS_NAMES[i].length) == 0
          ) {
            pp.inc(CharacterClasses.CLASS_NAMES[i].length + 2)
            return i
          }
          ++i
        }
      }
      return CharacterClasses.CLASS_NONE
    }

    /*
 * Skip over a "[]" range.
 * "p" must point to the character after the '['.
 * The returned pointer is on the matching ']', or the terminating NUL.
 */
    private fun skip_anyof(p: CharPointer): CharPointer {
      if (p.charAt() == '^') /* Complement of range. */ {
        p.inc()
      }
      if (p.charAt() == ']' || p.charAt() == '-') {
        p.inc()
      }
      while (!p.end() && p.charAt() != ']') {
        if (p.charAt() == '-') {
          p.inc()
          if (!p.end() && p.charAt() != ']') {
            p.inc()
          }
        } else if (p.charAt() == '\\' &&
          (REGEXP_INRANGE.indexOf(p.charAt(1)) != -1 || REGEXP_ABBR.indexOf(p.charAt(1)) != -1)
        ) {
          p.inc(2)
        } else if (p.charAt() == '[') {
          if (skip_class_name(p) == CharacterClasses.CLASS_NONE) {
            p.inc() /* It was not a class name */
          }
        } else {
          p.inc()
        }
      }
      return p
    }

    /*
 * Skip past regular expression.
 * Stop at end of 'p' of where 'dirc' is found ('/', '?', etc).
 * Take care of characters with a backslash in front of it.
 * Skip strings inside [ and ].
 */
    @JvmStatic
    fun skip_regexp(p: CharPointer, dirc: Char, magic: Boolean): CharPointer {
      var p = p
      var mymagic: Int
      mymagic = if (magic) {
        MAGIC_ON
      } else {
        MAGIC_OFF
      }
      while (!p.end()) {
        if (p.charAt() == dirc) /* found end of regexp */ {
          break
        }
        if (p.charAt() == '[' && mymagic >= MAGIC_ON ||
          p.charAt() == '\\' && p.charAt(1) == '[' && mymagic <= MAGIC_OFF
        ) {
          p = skip_anyof(p.ref(1))
          if (p.end()) {
            break
          }
        } else if (p.charAt() == '\\' && p.charAt(1) != '\u0000') {
          p.inc() /* skip next character */
          if (p.charAt() == 'v') {
            mymagic = MAGIC_ALL
          } else if (p.charAt() == 'V') {
            mymagic = MAGIC_NONE
          }
        }
        p.inc()
      }
      return p
    }

    // private boolean can_f_submatch = false;      /* true when submatch() can be used */
    /* These pointers are used instead of reg_match and reg_mmatch for
* reg_submatch().  Needed for when the substitution string is an expression
* that contains a call to substitute() and submatch(). */
    // private regmatch_T       submatch_match;
    // private regmmatch_T      submatch_mmatch;
    private val logger: VimLogger = injector.getLogger<RegExp>(RegExp::class.java)
  }
}
