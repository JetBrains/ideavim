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
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.OptionChangeEvent;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.regexp.CharHelper;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.CharacterClasses;
import com.maddyhome.idea.vim.regexp.RegExp;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public class SearchGroup extends AbstractActionGroup {
  public static final int KEEP_FLAGS = 1;
  public static final int CONFIRM = 2;
  public static final int IGNORE_ERROR = 4;
  public static final int GLOBAL = 8;
  public static final int IGNORE_CASE = 16;
  public static final int NO_IGNORE_CASE = 32;
  public static final int PRINT = 64;
  public static final int REUSE = 128;

  public SearchGroup() {
    Options.getInstance().getOption("hlsearch").addOptionChangeListener(new OptionChangeListener() {
      public void valueChange(OptionChangeEvent event) {
        showSearchHighlight = Options.getInstance().isSet("hlsearch");
        updateHighlight();
      }
    });
  }

  @Nullable
  public String getLastSearch() {
    return lastSearch;
  }

  @Nullable
  public String getLastPattern() {
    return lastPattern;
  }

  private void setLastPattern(@NotNull Editor editor, @NotNull String lastPattern) {
    this.lastPattern = lastPattern;
    CommandGroups.getInstance().getRegister().storeTextInternal(editor, new TextRange(-1, -1),
                                                                lastPattern, SelectionType.CHARACTER_WISE, '/', false);

    CommandGroups.getInstance().getHistory().addEntry(HistoryGroup.SEARCH, lastPattern);
  }

  public boolean searchAndReplace(@NotNull Editor editor, DataContext context, @NotNull LineRange range, @NotNull String excmd, String exarg) {
    boolean res = true;

    CharPointer cmd = new CharPointer(new StringBuffer(exarg));
    //sub_nsubs = 0;
    //sub_nlines = 0;

    int which_pat;
    if (excmd.equals("~")) {
      which_pat = RE_LAST;    /* use last used regexp */
    }
    else {
      which_pat = RE_SUBST;   /* use last substitute regexp */
    }

    CharPointer pat;
    CharPointer sub;
    char delimiter;
    /* new pattern and substitution */
    if (excmd.charAt(0) == 's' && !cmd.isNul() && !Character.isWhitespace(cmd.charAt()) &&
        "0123456789cegriIp|\"".indexOf(cmd.charAt()) == -1) {
      /* don't accept alphanumeric for separator */
      if (CharacterClasses.isAlpha(cmd.charAt())) {
        VimPlugin.showMessage(MessageHelper.message(Msg.E146));
        return false;
      }
      /*
      * undocumented vi feature:
      *  "\/sub/" and "\?sub?" use last used search pattern (almost like
      *  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
      */
      if (cmd.charAt() == '\\') {
        cmd.inc();
        if ("/?&".indexOf(cmd.charAt()) == -1) {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_backslash));
          return false;
        }
        if (cmd.charAt() != '&') {
          which_pat = RE_SEARCH;      /* use last '/' pattern */
        }
        pat = new CharPointer("");             /* empty search pattern */
        delimiter = cmd.charAt();             /* remember delimiter character */
        cmd.inc();
      }
      else            /* find the end of the regexp */ {
        which_pat = RE_LAST;            /* use last used regexp */
        delimiter = cmd.charAt();             /* remember delimiter character */
        cmd.inc();
        pat = cmd.ref(0);                      /* remember start of search pat */
        cmd = RegExp.skip_regexp(cmd, delimiter, true);
        if (cmd.charAt() == delimiter)        /* end delimiter found */ {
          cmd.set('\u0000').inc(); /* replace it with a NUL */
        }
      }

      /*
      * Small incompatibility: vi sees '\n' as end of the command, but in
      * Vim we want to use '\n' to find/substitute a NUL.
      */
      sub = cmd.ref(0);          /* remember the start of the substitution */

      while (!cmd.isNul()) {
        if (cmd.charAt() == delimiter)            /* end delimiter found */ {
          cmd.set('\u0000').inc(); /* replace it with a NUL */
          break;
        }
        if (cmd.charAt(0) == '\\' && cmd.charAt(1) != 0)  /* skip escaped characters */ {
          cmd.inc();
        }
        cmd.inc();
      }
    }
    else        /* use previous pattern and substitution */ {
      if (lastReplace == null)    /* there is no previous command */ {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_nopresub));
        return false;
      }
      pat = null;             /* search_regcomp() will use previous pattern */
      sub = new CharPointer(lastReplace);
    }

    /*
    * Find trailing options.  When '&' is used, keep old options.
    */
    if (cmd.charAt() == '&') {
      cmd.inc();
    }
    else {
      do_all = Options.getInstance().isSet("gdefault");
      do_ask = false;
      do_error = true;
      //do_print = false;
      do_ic = 0;
    }
    while (!cmd.isNul()) {
      /*
      * Note that 'g' and 'c' are always inverted, also when p_ed is off.
      * 'r' is never inverted.
      */
      if (cmd.charAt() == 'g') {
        do_all = !do_all;
      }
      else if (cmd.charAt() == 'c') {
        do_ask = !do_ask;
      }
      else if (cmd.charAt() == 'e') {
        do_error = !do_error;
      }
      else if (cmd.charAt() == 'r')       /* use last used regexp */ {
        which_pat = RE_LAST;
      }
      else if (cmd.charAt() == 'p') {
        //do_print = true;
      }
      else if (cmd.charAt() == 'i')       /* ignore case */ {
        do_ic = 'i';
      }
      else if (cmd.charAt() == 'I')       /* don't ignore case */ {
        do_ic = 'I';
      }
      else {
        break;
      }
      cmd.inc();
    }

    int line1 = range.getStartLine();
    int line2 = range.getEndLine();

    /*
    * check for a trailing count
    */
    cmd = CharHelper.skipwhite(cmd);
    if (CharacterClasses.isDigit(cmd.charAt())) {
      int i = CharHelper.getdigits(cmd);
      if (i <= 0 && do_error) {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_zerocount));
        return false;
      }
      line1 = line2;
      line2 = EditorHelper.normalizeLine(editor, line1 + i - 1);
    }

    /*
    * check for trailing command or garbage
    */
    cmd = CharHelper.skipwhite(cmd);
    if (!cmd.isNul() && cmd.charAt() != '"')        /* if not end-of-line or comment */ {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_trailing));
      return false;
    }

    String pattern = "";
    if (pat == null || pat.isNul()) {
      switch (which_pat) {
        case RE_LAST:
          pattern = lastPattern;
          break;
        case RE_SEARCH:
          pattern = lastSearch;
          break;
        case RE_SUBST:
          pattern = lastSubstitute;
          break;
      }
    }
    else {
      pattern = pat.toString();
    }

    lastSubstitute = pattern;
    if (pattern != null) {
      setLastPattern(editor, pattern);
    }

    //int start = editor.logicalPositionToOffset(new LogicalPosition(line1, 0));
    //int end = editor.logicalPositionToOffset(new LogicalPosition(line2, EditorHelper.getLineLength(editor, line2)));
    int start = editor.getDocument().getLineStartOffset(line1);
    int end = editor.getDocument().getLineEndOffset(line2);

    RegExp sp;
    RegExp.regmmatch_T regmatch = new RegExp.regmmatch_T();
    sp = new RegExp();
    regmatch.regprog = sp.vim_regcomp(pattern, 1);
    if (regmatch.regprog == null) {
      if (do_error) {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_invcmd));
      }
      return false;
    }

    /* the 'i' or 'I' flag overrules 'ignorecase' and 'smartcase' */
    if (do_ic == 'i') {
      regmatch.rmm_ic = true;
    }
    else if (do_ic == 'I') {
      regmatch.rmm_ic = false;
    }

    /*
    * ~ in the substitute pattern is replaced with the old pattern.
    * We do it here once to avoid it to be replaced over and over again.
    * But don't do it when it starts with "\=", then it's an expression.
    */
    if (!(sub.charAt(0) == '\\' && sub.charAt(1) == '=') && lastReplace != null) {
      StringBuffer tmp = new StringBuffer(sub.toString());
      int pos = 0;
      while ((pos = tmp.indexOf("~", pos)) != -1) {
        if (pos == 0 || tmp.charAt(pos - 1) != '\\') {
          tmp.replace(pos, pos + 1, lastReplace);
          pos += lastReplace.length();
        }
        pos++;
      }
      sub = new CharPointer(tmp);
    }

    lastReplace = sub.toString();

    searchHighlight(false);

    if (logger.isDebugEnabled()) {
      logger.debug("search range=[" + start + "," + end + "]");
      logger.debug("pattern=" + pattern + ", replace=" + sub);
    }
    int lastMatch = -1;
    int lastLine = -1;
    int searchcol = 0;
    boolean firstMatch = true;
    boolean got_quit = false;
    int lcount = EditorHelper.getLineCount(editor);
    for (int lnum = line1; lnum <= line2 && !got_quit; ) {
      CharacterPosition newpos = null;
      int nmatch = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, searchcol);
      if (nmatch > 0) {
        if (firstMatch) {
          CommandGroups.getInstance().getMark().saveJumpLocation(editor);
          firstMatch = false;
        }

        String match = sp.vim_regsub_multi(regmatch, lnum, sub, 1, false);
        //logger.debug("found match[" + spos + "," + epos + "] - replace " + match);

        int line = lnum + regmatch.startpos[0].lnum;
        CharacterPosition startpos = new CharacterPosition(lnum + regmatch.startpos[0].lnum,
                                                           regmatch.startpos[0].col);
        CharacterPosition endpos = new CharacterPosition(lnum + regmatch.endpos[0].lnum,
                                                         regmatch.endpos[0].col);
        int startoff = EditorHelper.characterPositionToOffset(editor, startpos);
        int endoff = EditorHelper.characterPositionToOffset(editor, endpos);
        int newend = startoff + match.length();

        if (do_all || line != lastLine) {
          boolean doReplace = true;
          if (do_ask) {
            //editor.getSelectionModel().setSelection(startoff, endoff);
            RangeHighlighter hl = highlightConfirm(editor, startoff, endoff);
            int choice = getConfirmChoice(match);
            //editor.getSelectionModel().removeSelection();
            editor.getMarkupModel().removeHighlighter(hl);
            switch (choice) {
              case 0: // Yes
                doReplace = true;
                break;
              case 1: // No
                doReplace = false;
                break;
              case 2: // All
                do_ask = false;
                break;
              case JOptionPane.CLOSED_OPTION:
              case 3: // Quit
                doReplace = false;
                got_quit = true;
                break;
              case 4: // Last
                do_all = false;
                line2 = lnum;
                doReplace = true;
                break;
            }
          }

          if (doReplace) {
            editor.getDocument().replaceString(startoff, endoff, match);
            lastMatch = startoff;
            newpos = EditorHelper.offsetToCharacterPosition(editor, newend);

            line2 += newpos.line - endpos.line;
          }
        }

        lastLine = line;

        lnum += nmatch - 1;
        if (do_all && startoff != endoff) {
          if (newpos != null) {
            lnum = newpos.line;
            searchcol = newpos.column;
          }
          else {
            searchcol = endpos.column;
          }
        }
        else {
          searchcol = 0;
          lnum++;
        }
      }
      else {
        lnum++;
        searchcol = 0;
      }
    }

    if (lastMatch != -1) {
      MotionGroup.moveCaret(editor,
                            CommandGroups.getInstance().getMotion().moveCaretToLineStartSkipLeading(editor,
                                                                                                    editor.offsetToLogicalPosition(
                                                                                                      lastMatch).line));
    }
    else {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_patnotf2, pattern));
    }

    return res;
  }

  private int getConfirmChoice(String match) {
    Object[] btns = getConfirmButtons();
    confirmDlg = new JOptionPane("Replace with " + match + " ?", JOptionPane.QUESTION_MESSAGE,
                                 JOptionPane.DEFAULT_OPTION, null, btns, btns[0]);
    JDialog dlg = confirmDlg.createDialog(null, "Confirm Replace");
    dlg.setVisible(true);
    Object res = confirmDlg.getValue();
    confirmDlg = null;
    if (res == null) {
      return JOptionPane.CLOSED_OPTION;
    }
    for (int i = 0; i < btns.length; i++) {
      if (btns[i].equals(res)) {
        return i;
      }
    }

    return JOptionPane.CLOSED_OPTION;
  }

  private boolean shouldIgnoreCase(@NotNull String pattern, boolean noSmartCase) {
    boolean sc = !noSmartCase && Options.getInstance().isSet("smartcase");
    boolean ic = Options.getInstance().isSet("ignorecase");

    return ic && !(sc && StringHelper.containsUpperCase(pattern));
  }

  public static int argsToFlags(@NotNull String args) {
    int res = 0;
    boolean global = Options.getInstance().isSet("gdefault");
    for (int i = 0; i < args.length(); i++) {
      switch (args.charAt(i)) {
        case '&':
          res |= KEEP_FLAGS;
          break;
        case 'c':
          res |= CONFIRM;
          break;
        case 'e':
          res |= IGNORE_ERROR;
          break;
        case 'g':
          global = !global;
          break;
        case 'i':
          res |= IGNORE_CASE;
          break;
        case 'I':
          res |= NO_IGNORE_CASE;
          break;
        case 'p':
          res |= PRINT;
          break;
        case 'r':
          res |= REUSE;
          break;
      }
    }

    if (global) {
      res |= GLOBAL;
    }

    return res;
  }

  private Object[] getConfirmButtons() {
    if (confirmBtns == null) {
      confirmBtns = new JButton[]{
        new JButton("Yes"),
        new JButton("No"),
        new JButton("All"),
        new JButton("Quit"),
        new JButton("Last")
      };

      confirmBtns[0].setMnemonic('Y');
      confirmBtns[1].setMnemonic('N');
      confirmBtns[2].setMnemonic('A');
      confirmBtns[3].setMnemonic('Q');
      confirmBtns[4].setMnemonic('L');

      for (int i = 0; i < confirmBtns.length; i++) {
        confirmBtns[i].addActionListener(new ButtonActionListener(i));
      }
      //confirmBtns = new String[] { "Yes", "No", "All", "Quit", "Last" };
    }

    return confirmBtns;
  }

  public int search(@NotNull Editor editor, @NotNull String command, int count, int flags, boolean moveCursor) {
    int res = search(editor, command, editor.getCaretModel().getOffset(), count, flags);

    if (res != -1 && moveCursor) {
      CommandGroups.getInstance().getMark().saveJumpLocation(editor);
      MotionGroup.moveCaret(editor, res);
    }

    return res;
  }

  public int search(@NotNull Editor editor, @NotNull String command, int startOffset, int count, int flags) {
    int dir = 1;
    char type = '/';
    String pattern = lastSearch;
    String offset = lastOffset;
    if ((flags & Command.FLAG_SEARCH_REV) != 0) {
      dir = -1;
      type = '?';
    }

    if (command.length() > 0) {
      if (command.charAt(0) != type) {
        CharPointer p = new CharPointer(command);
        CharPointer end = RegExp.skip_regexp(p.ref(0), type, true);
        pattern = p.substring(end.pointer() - p.pointer());
        if (logger.isDebugEnabled()) logger.debug("pattern=" + pattern);
        if (p.charAt() != type) {
          logger.debug("no offset");
          offset = "";
        }
        else {
          p.inc();
          offset = p.toString();
          if (logger.isDebugEnabled()) logger.debug("offset=" + offset);
        }
      }
      else if (command.length() == 1) {
        offset = "";
      }
      else {
        offset = command.substring(1);
        if (logger.isDebugEnabled()) logger.debug("offset=" + offset);
      }
    }

    lastSearch = pattern;
    if (pattern != null) {
      setLastPattern(editor, pattern);
    }
    lastOffset = offset;
    lastDir = dir;

    if (logger.isDebugEnabled()) {
      logger.debug("lastSearch=" + lastSearch);
      logger.debug("lastOffset=" + lastOffset);
      logger.debug("lastDir=" + lastDir);
    }

    searchHighlight(false);

    return findItOffset(editor, startOffset, count, lastDir, false);
  }

  public int searchWord(@NotNull Editor editor, int count, boolean whole, int dir) {
    TextRange range = SearchHelper.findWordUnderCursor(editor);
    if (range == null) {
      return -1;
    }

    StringBuffer pattern = new StringBuffer();
    if (whole) {
      pattern.append("\\<");
    }
    pattern.append(EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset()));
    if (whole) {
      pattern.append("\\>");
    }

    MotionGroup.moveCaret(editor, range.getStartOffset());

    lastSearch = pattern.toString();
    setLastPattern(editor, lastSearch);
    lastOffset = "";
    lastDir = dir;

    searchHighlight(true);

    return findItOffset(editor, editor.getCaretModel().getOffset(), count, lastDir, true);
  }

  public int searchNext(@NotNull Editor editor, int count) {
    searchHighlight(false);
    return findItOffset(editor, editor.getCaretModel().getOffset(), count, lastDir, false);
  }

  public int searchPrevious(@NotNull Editor editor, int count) {
    searchHighlight(false);
    return findItOffset(editor, editor.getCaretModel().getOffset(), count, -lastDir, false);
  }

  public void updateHighlight() {
    highlightSearch(false);
  }

  private void searchHighlight(boolean noSmartCase) {
    showSearchHighlight = Options.getInstance().isSet("hlsearch");
    highlightSearch(noSmartCase);
  }

  private void highlightSearch(final boolean noSmartCase) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      Editor current = FileEditorManager.getInstance(project).getSelectedTextEditor();
      Editor[] editors =
        current == null ? null : EditorFactory.getInstance().getEditors(current.getDocument(), project);
      if (editors == null) {
        continue;
      }

      for (final Editor editor : editors) {
        String els = EditorData.getLastSearch(editor);
        if (!showSearchHighlight) {
          removeSearchHighlight(editor);

          continue;
        }
        else if (lastSearch != null && lastSearch.equals(els)) {
          continue;
        }
        else if (lastSearch == null) {
          continue;
        }

        removeSearchHighlight(editor);
        highlightSearchLines(editor, 0, -1, lastSearch, shouldIgnoreCase(lastSearch, noSmartCase));

        EditorData.setLastSearch(editor, lastSearch);
      }
    }
  }

  private void highlightSearchLines(@NotNull Editor editor, boolean noSmartCase, int startLine, int endLine) {
    highlightSearchLines(editor, startLine, endLine, lastSearch, shouldIgnoreCase(lastSearch, noSmartCase));
  }

  private static void highlightSearchLines(@NotNull Editor editor, int startLine, int endLine, String text, boolean ic) {
    TextAttributes color = editor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    Collection<RangeHighlighter> hls = EditorData.getLastHighlights(editor);
    if (hls == null) {
      hls = new ArrayList<RangeHighlighter>();
      EditorData.setLastHighlights(editor, hls);
    }

    int line2 = endLine == -1 ? EditorHelper.getLineCount(editor) : endLine;

    RegExp sp;
    RegExp.regmmatch_T regmatch = new RegExp.regmmatch_T();
    sp = new RegExp();
    regmatch.regprog = sp.vim_regcomp(text, 1);
    if (regmatch.regprog == null) {
      return;
    }

    regmatch.rmm_ic = ic;

    int searchcol = 0;
    int lcount = EditorHelper.getLineCount(editor);
    for (int lnum = startLine; lnum <= line2; ) {
      int nmatch = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, searchcol);
      if (nmatch > 0) {
        CharacterPosition startpos = new CharacterPosition(lnum + regmatch.startpos[0].lnum,
                                                           regmatch.startpos[0].col);
        CharacterPosition endpos = new CharacterPosition(lnum + regmatch.endpos[0].lnum,
                                                         regmatch.endpos[0].col);
        int startoff = EditorHelper.characterPositionToOffset(editor, startpos);
        int endoff = EditorHelper.characterPositionToOffset(editor, endpos);

        RangeHighlighter rh = highlightMatch(editor, startoff, endoff);
        rh.setErrorStripeMarkColor(color.getBackgroundColor());
        rh.setErrorStripeTooltip(text);
        hls.add(rh);

        if (startoff != endoff) {
          lnum += nmatch - 1;
          searchcol = endpos.column;
        }
        else {
          lnum += nmatch;
          searchcol = 0;
        }
      }
      else {
        lnum++;
        searchcol = 0;
      }
    }
  }

  private int findItOffset(@NotNull Editor editor, int startOffset, int count, int dir,
                           boolean noSmartCase) {
    boolean wrap = Options.getInstance().isSet("wrapscan");
    TextRange range = findIt(editor, startOffset, count, dir, noSmartCase, wrap, true, true);
    if (range == null) {
      return -1;
    }

    //highlightMatch(editor, range.getStartOffset(), range.getEndOffset());

    ParsePosition pp = new ParsePosition(0);
    int res = range.getStartOffset();

    if (lastOffset.length() == 0) {
      return range.getStartOffset();
    }
    else if (Character.isDigit(lastOffset.charAt(0)) || lastOffset.charAt(0) == '+' || lastOffset.charAt(0) == '-') {
      int lineOffset = 0;
      if (lastOffset.equals("+")) {
        lineOffset = 1;
      }
      else if (lastOffset.equals("-")) {
        lineOffset = -1;
      }
      else {
        if (lastOffset.charAt(0) == '+') {
          lastOffset = lastOffset.substring(1);
        }
        NumberFormat nf = NumberFormat.getIntegerInstance();
        pp = new ParsePosition(0);
        Number num = nf.parse(lastOffset, pp);
        if (num != null) {
          lineOffset = num.intValue();
        }
      }

      int line = editor.offsetToLogicalPosition(range.getStartOffset()).line;
      int newLine = EditorHelper.normalizeLine(editor, line + lineOffset);

      res = CommandGroups.getInstance().getMotion().moveCaretToLineStart(editor, newLine);
    }
    else if ("ebs".indexOf(lastOffset.charAt(0)) != -1) {
      int charOffset = 0;
      if (lastOffset.length() >= 2) {
        if ("+-".indexOf(lastOffset.charAt(1)) != -1) {
          charOffset = 1;
        }
        NumberFormat nf = NumberFormat.getIntegerInstance();
        pp = new ParsePosition(lastOffset.charAt(1) == '+' ? 2 : 1);
        Number num = nf.parse(lastOffset, pp);
        if (num != null) {
          charOffset = num.intValue();
        }
      }

      int base = range.getStartOffset();
      if (lastOffset.charAt(0) == 'e') {
        base = range.getEndOffset() - 1;
      }

      res = Math.max(0, Math.min(base + charOffset, EditorHelper.getFileSize(editor) - 1));
    }

    int ppos = pp.getIndex();
    if (ppos < lastOffset.length() - 1 && lastOffset.charAt(ppos) == ';') {
      int flags;
      if (lastOffset.charAt(ppos + 1) == '/') {
        flags = Command.FLAG_SEARCH_FWD;
      }
      else if (lastOffset.charAt(ppos + 1) == '?') {
        flags = Command.FLAG_SEARCH_REV;
      }
      else {
        return res;
      }

      if (lastOffset.length() - ppos > 2) {
        ppos++;
      }

      res = search(editor, lastOffset.substring(ppos + 1), res, 1, flags);

      return res;
    }
    else {
      return res;
    }
  }

  @Nullable
  private TextRange findIt(@NotNull Editor editor, int startOffset, int count, int dir,
                           boolean noSmartCase, boolean wrap, boolean showMessages, boolean wholeFile) {
    TextRange res = null;

    if (lastSearch == null || lastSearch.length() == 0) {
      return res;
    }

    /*
    int pflags = RE.REG_MULTILINE;
    if (shouldIgnoreCase(lastSearch, noSmartCase))
    {
        pflags |= RE.REG_ICASE;
    }
    */
    //RE sp;
    RegExp sp;
    RegExp.regmmatch_T regmatch = new RegExp.regmmatch_T();
    regmatch.rmm_ic = shouldIgnoreCase(lastSearch, noSmartCase);
    sp = new RegExp();
    regmatch.regprog = sp.vim_regcomp(lastSearch, 1);
    if (regmatch == null) {
      if (logger.isDebugEnabled()) logger.debug("bad pattern: " + lastSearch);
      return res;
    }

    /*
    int extra_col = 1;
    int startcol = -1;
    boolean found = false;
    boolean match_ok = true;
    LogicalPosition pos = editor.offsetToLogicalPosition(startOffset);
    LogicalPosition endpos = null;
    //REMatch match = null;
    */

    CharacterPosition lpos = EditorHelper.offsetToCharacterPosition(editor, startOffset);
    RegExp.lpos_T pos = new RegExp.lpos_T();
    pos.lnum = lpos.line;
    pos.col = lpos.column;

    int found;
    int lnum;           /* no init to shut up Apollo cc */
    //RegExp.regmmatch_T regmatch;
    CharPointer ptr;
    int matchcol;
    int startcol;
    RegExp.lpos_T endpos = new RegExp.lpos_T();
    int loop;
    RegExp.lpos_T start_pos;
    boolean at_first_line;
    int extra_col = 1;
    boolean match_ok;
    long nmatched;
    //int         submatch = 0;
    int first_lnum;

    int lineCount = EditorHelper.getLineCount(editor);
    int startLine = 0;
    int endLine = lineCount;

    do  /* loop for count */ {
      start_pos = new RegExp.lpos_T(pos);       /* remember start pos for detecting no match */
      found = 0;              /* default: not found */
      at_first_line = true;   /* default: start in first line */
      if (pos.lnum == -1)     /* correct lnum for when starting in line 0 */ {
        pos.lnum = 0;
        pos.col = 0;
        at_first_line = false;  /* not in first line now */
      }

      /*
      * Start searching in current line, unless searching backwards and
      * we're in column 0.
      */
      if (dir == -1 && start_pos.col == 0) {
        lnum = pos.lnum - 1;
        at_first_line = false;
      }
      else {
        lnum = pos.lnum;
      }

      int lcount = EditorHelper.getLineCount(editor);
      for (loop = 0; loop <= 1; ++loop)   /* loop twice if 'wrapscan' set */ {
        if (!wholeFile) {
          startLine = lnum;
          endLine = lnum + 1;
        }
        for (; lnum >= startLine && lnum < endLine; lnum += dir, at_first_line = false) {
          /*
          * Look for a match somewhere in the line.
          */
          first_lnum = lnum;
          nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, 0);
          if (nmatched > 0) {
            /* match may actually be in another line when using \zs */
            lnum += regmatch.startpos[0].lnum;
            ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum));
            startcol = regmatch.startpos[0].col;
            endpos = regmatch.endpos[0];

            /*
            * Forward search in the first line: match should be after
            * the start position. If not, continue at the end of the
            * match (this is vi compatible) or on the next char.
            */
            if (dir == 1 && at_first_line) {
              match_ok = true;
              /*
              * When match lands on a NUL the cursor will be put
              * one back afterwards, compare with that position,
              * otherwise "/$" will get stuck on end of line.
              */
              while ((startcol - (startcol == ptr.strlen() ? 1 : 0)) < (start_pos.col + extra_col)) {
                if (nmatched > 1) {
                  /* end is in next line, thus no match in
               * this line */
                  match_ok = false;
                  break;
                }
                matchcol = endpos.col;
                /* for empty match: advance one char */
                if (matchcol == startcol && ptr.charAt(matchcol) != '\u0000') {
                  ++matchcol;
                }
                if (ptr.charAt(matchcol) == '\u0000' ||
                    (nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, matchcol)) == 0) {
                  match_ok = false;
                  break;
                }
                startcol = regmatch.startpos[0].col;
                endpos = regmatch.endpos[0];

                /* Need to get the line pointer again, a
        * multi-line search may have made it invalid. */
                ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum));
              }
              if (!match_ok) {
                continue;
              }
            }
            if (dir == -1) {
              /*
              * Now, if there are multiple matches on this line,
              * we have to get the last one. Or the last one before
              * the cursor, if we're on that line.
              * When putting the new cursor at the end, compare
              * relative to the end of the match.
              */
              match_ok = false;
              for (; ; ) {
                if (!at_first_line || (regmatch.startpos[0].col + extra_col <= start_pos.col)) {
                  /* Remember this position, we use it if it's
           * the last match in the line. */
                  match_ok = true;
                  startcol = regmatch.startpos[0].col;
                  endpos = regmatch.endpos[0];
                }
                else {
                  break;
                }

                /*
                * We found a valid match, now check if there is
                * another one after it.
                * If vi-compatible searching, continue at the end
                * of the match, otherwise continue one position
                * forward.
                */
                if (nmatched > 1) {
                  break;
                }
                matchcol = endpos.col;
                /* for empty match: advance one char */
                if (matchcol == startcol && ptr.charAt(matchcol) != '\u0000') {
                  ++matchcol;
                }
                if (ptr.charAt(matchcol) == '\u0000' ||
                    (nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, matchcol)) == 0) {
                  break;
                }

                /* Need to get the line pointer again, a
        * multi-line search may have made it invalid. */
                ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum));
              }

              /*
              * If there is only a match after the cursor, skip
              * this match.
              */
              if (!match_ok) {
                continue;
              }
            }

            pos.lnum = lnum;
            pos.col = startcol;
            endpos.lnum += first_lnum;
            found = 1;

            /* Set variables used for 'incsearch' highlighting. */
            //search_match_lines = endpos.lnum - (lnum - first_lnum);
            //search_match_endcol = endpos.col;
            break;
          }
          //line_breakcheck();      /* stop if ctrl-C typed */
          //if (got_int)
          //    break;

          if (loop != 0 && lnum == start_pos.lnum) {
            break;          /* if second loop, stop where started */
          }
        }
        at_first_line = false;

        /*
        * stop the search if wrapscan isn't set, after an interrupt and
        * after a match
        */
        if (!wrap || found != 0) {
          break;
        }

        /*
        * If 'wrapscan' is set we continue at the other end of the file.
        * If 'shortmess' does not contain 's', we give a message.
        * This message is also remembered in keep_msg for when the screen
        * is redrawn. The keep_msg is cleared whenever another message is
        * written.
        */
        if (dir == -1)    /* start second loop at the other end */ {
          lnum = lineCount - 1;
          //if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG))
          //    give_warning((char_u *)_(top_bot_msg), TRUE);
        }
        else {
          lnum = 0;
          //if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG))
          //    give_warning((char_u *)_(bot_top_msg), TRUE);
        }
      }
      //if (got_int || called_emsg || break_loop)
      //    break;
    }
    while (--count > 0 && found != 0);   /* stop after count matches or no match */

    if (found == 0)             /* did not find it */ {
      //if ((options & SEARCH_MSG) == SEARCH_MSG)
      if (showMessages) {
        if (wrap) {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_patnotf2, lastSearch));
        }
        else if (lnum <= 0) {
          VimPlugin.showMessage(MessageHelper.message(Msg.E384, lastSearch));
        }
        else {
          VimPlugin.showMessage(MessageHelper.message(Msg.E385, lastSearch));
        }
      }
      return null;
    }

    //return new TextRange(editor.logicalPositionToOffset(new LogicalPosition(pos.lnum, pos.col)),
    //    editor.logicalPositionToOffset(new LogicalPosition(endpos.lnum, endpos.col)));
    //return new TextRange(editor.logicalPositionToOffset(new LogicalPosition(pos.lnum, 0)) + pos.col,
    //    editor.logicalPositionToOffset(new LogicalPosition(endpos.lnum, 0)) + endpos.col);
    return new TextRange(EditorHelper.characterPositionToOffset(editor, new CharacterPosition(pos.lnum, pos.col)),
                         EditorHelper.characterPositionToOffset(editor, new CharacterPosition(endpos.lnum, endpos.col)));
  }

  @NotNull
  private RangeHighlighter highlightConfirm(@NotNull Editor editor, int start, int end) {
    TextAttributes color = new TextAttributes(
      editor.getColorsScheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
      editor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
      null, null, 0
    );
    return editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.ADDITIONAL_SYNTAX + 2,
                                                       color, HighlighterTargetArea.EXACT_RANGE);
  }

  @NotNull
  private static RangeHighlighter highlightMatch(@NotNull Editor editor, int start, int end) {
    TextAttributes color = editor.getColorsScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    return editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.ADDITIONAL_SYNTAX + 1,
                                                       color, HighlighterTargetArea.EXACT_RANGE);
  }

  public void clearSearchHighlight() {
    showSearchHighlight = false;
    updateHighlight();
  }

  private static void removeSearchHighlight(@NotNull Editor editor) {
    Collection<RangeHighlighter> ehl = EditorData.getLastHighlights(editor);
    if (ehl == null) {
      return;
    }

    for (RangeHighlighter rh : ehl) {
      editor.getMarkupModel().removeHighlighter(rh);
    }

    ehl.clear();

    EditorData.setLastHighlights(editor, null);
    EditorData.setLastSearch(editor, null);
  }

  public void saveData(@NotNull Element element) {
    logger.debug("saveData");
    Element search = new Element("search");
    if (lastSearch != null) {
      search.addContent(createElementWithText("last-search", lastSearch));
    }
    if (lastOffset != null) {
      search.addContent(createElementWithText("last-offset", lastOffset));
    }
    if (lastPattern != null) {
      search.addContent(createElementWithText("last-pattern", lastPattern));
    }
    if (lastReplace != null) {
      search.addContent(createElementWithText("last-replace", lastReplace));
    }
    if (lastSubstitute != null) {
      search.addContent(createElementWithText("last-substitute", lastSubstitute));
    }
    Element text = new Element("last-dir");
    text.addContent(Integer.toString(lastDir));
    search.addContent(text);

    text = new Element("show-last");
    text.addContent(Boolean.toString(showSearchHighlight));
    if (logger.isDebugEnabled()) logger.debug("text=" + text);
    search.addContent(text);

    element.addContent(search);
  }

  @NotNull
  private static Element createElementWithText(@NotNull String name, @NotNull String text) {
    return StringHelper.setSafeXmlText(new Element(name), text);
  }

  public void readData(@NotNull Element element) {
    logger.debug("readData");
    Element search = element.getChild("search");
    if (search == null) {
      return;
    }

    lastSearch = getSafeChildText(search, "last-search");
    lastOffset = getSafeChildText(search, "last-offset");
    lastPattern = getSafeChildText(search, "last-pattern");
    lastReplace = getSafeChildText(search, "last-replace");
    lastSubstitute = getSafeChildText(search, "last-substitute");

    Element dir = search.getChild("last-dir");
    lastDir = Integer.parseInt(dir.getText());

    Element show = search.getChild("show-last");
    showSearchHighlight = Boolean.valueOf(show.getText());
    if (logger.isDebugEnabled()) {
      logger.debug("show=" + show + "(" + show.getText() + ")");
      logger.debug("showSearchHighlight=" + showSearchHighlight);
    }
  }

  @Nullable
  private static String getSafeChildText(@NotNull Element element, @NotNull String name) {
    final Element child = element.getChild(name);
    return child != null ? StringHelper.getSafeXmlText(child) : null;
  }

  private class ButtonActionListener implements ActionListener {
    public ButtonActionListener(int i) {
      index = i;
    }

    public void actionPerformed(ActionEvent event) {
      if (confirmDlg != null) {
        confirmDlg.setValue(confirmBtns[index]);
      }
    }

    private int index;
  }

  public static class EditorSelectionCheck extends FileEditorManagerAdapter {
    /*
    public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile)
    {
        FileDocumentManager.getInstance().getDocument(virtualFile).addDocumentListener(listener);
    }

    public void fileClosed(FileEditorManager fileEditorManager, VirtualFile virtualFile)
    {
        FileDocumentManager.getInstance().getDocument(virtualFile).removeDocumentListener(listener);
    }
    */

    public void selectionChanged(FileEditorManagerEvent event) {
      CommandGroups.getInstance().getSearch().updateHighlight();
    }
  }

  public static class DocumentSearchListener extends DocumentAdapter {
    public void documentChanged(@NotNull DocumentEvent event) {
      if (!VimPlugin.isEnabled()) {
        return;
      }

      Project[] projs = ProjectManager.getInstance().getOpenProjects();
      for (Project proj : projs) {
        Editor[] editors = EditorFactory.getInstance().getEditors(event.getDocument(), proj);
        for (Editor editor : editors) {
          Collection hls = EditorData.getLastHighlights(editor);
          if (hls == null) {
            continue;
          }

          int soff = event.getOffset();
          int eoff = soff + event.getNewLength();

          if (logger.isDebugEnabled()) {
            logger.debug("hls=" + hls);
            logger.debug("event=" + event);
          }
          Iterator iter = hls.iterator();
          while (iter.hasNext()) {
            RangeHighlighter rh = (RangeHighlighter)iter.next();
            if (!rh.isValid() || (eoff >= rh.getStartOffset() && soff <= rh.getEndOffset())) {
              iter.remove();
              editor.getMarkupModel().removeHighlighter(rh);
            }
          }

          int sl = editor.offsetToLogicalPosition(soff).line;
          int el = editor.offsetToLogicalPosition(eoff).line;
          CommandGroups.getInstance().getSearch().highlightSearchLines(editor, false, sl, el);
          hls = EditorData.getLastHighlights(editor);
          if (logger.isDebugEnabled()) {
            logger.debug("sl=" + sl + ", el=" + el);
            logger.debug("hls=" + hls);
          }
        }
      }
    }
  }

  @Nullable private String lastSearch;
  @Nullable private String lastPattern;
  @Nullable private String lastSubstitute;
  @Nullable private String lastReplace;
  @Nullable private String lastOffset;
  private int lastDir;
  private JButton[] confirmBtns;
  @Nullable private JOptionPane confirmDlg = null;
  private boolean showSearchHighlight = Options.getInstance().isSet("hlsearch");

  private boolean do_all = false; /* do multiple substitutions per line */
  private boolean do_ask = false; /* ask for confirmation */
  private boolean do_error = true; /* if false, ignore errors */
  //private boolean do_print = false; /* print last line with subs. */
  private char do_ic = 0; /* ignore case flag */

  private static final int RE_LAST = 1;
  private static final int RE_SEARCH = 2;
  private static final int RE_SUBST = 3;

  private static Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
