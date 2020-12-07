/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.CharacterClasses;
import com.maddyhome.idea.vim.regexp.RegExp;
import com.maddyhome.idea.vim.ui.ModalEntry;
import com.maddyhome.idea.vim.ui.ex.ExEntryPanel;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import static com.maddyhome.idea.vim.helper.SearchHelperKtKt.shouldIgnoreCase;

@State(name = "VimSearchSettings", storages = {
  @Storage(value = "$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)
})
public class SearchGroup implements PersistentStateComponent<Element> {
  public SearchGroup() {
    final OptionsManager options = OptionsManager.INSTANCE;
    options.getHlsearch().addOptionChangeListener((oldValue, newValue) -> {
      resetShowSearchHighlight();
      forceUpdateSearchHighlights();
    });

    final OptionChangeListener<Boolean> updateHighlightsIfVisible = (oldValue, newValue) -> {
      if (showSearchHighlight) {
        forceUpdateSearchHighlights();
      }
    };
    options.getIgnorecase().addOptionChangeListener(updateHighlightsIfVisible);
    options.getSmartcase().addOptionChangeListener(updateHighlightsIfVisible);
  }

  public void turnOn() {
    updateSearchHighlights();
  }

  public void turnOff() {
    final boolean show = showSearchHighlight;
    clearSearchHighlight();
    showSearchHighlight = show;
  }

  public @Nullable String getLastSearch() {
    return lastSearch;
  }

  // This method is used in AceJump integration plugin
  @SuppressWarnings("unused")
  public int getLastDir() {
    return lastDir.toInt();
  }

  public @Nullable String getLastPattern() {
    return lastPattern;
  }

  public void resetState() {
    lastSearch = lastPattern = lastSubstitute = lastReplace = lastOffset = null;
    lastIgnoreSmartCase = false;
    lastDir = Direction.FORWARDS;
    resetShowSearchHighlight();
  }

  private void setLastPattern(@NotNull String lastPattern) {
    this.lastPattern = lastPattern;
    VimPlugin.getRegister().storeTextSpecial(RegisterGroup.LAST_SEARCH_REGISTER, lastPattern);
    VimPlugin.getHistory().addEntry(HistoryGroup.SEARCH, lastPattern);
  }

  /**
   * Find all occurrences of the pattern
   *
   * @deprecated Use SearchHelper#findAll instead. Kept for compatibility with existing plugins
   *
   * @param editor      The editor to search in
   * @param pattern     The pattern to search for
   * @param startLine   The start line of the range to search for, or -1 for the whole document
   * @param endLine     The end line of the range to search for, or -1 for the whole document
   * @param ignoreCase  Case sensitive or insensitive searching
   * @return            A list of TextRange objects representing the results
   */
  @Deprecated()
  public static @NotNull List<TextRange> findAll(@NotNull Editor editor,
                                                 @NotNull String pattern,
                                                 int startLine,
                                                 int endLine,
                                                 boolean ignoreCase) {
    return SearchHelper.findAll(editor, pattern, startLine, endLine, ignoreCase);
  }

  private static @NotNull ReplaceConfirmationChoice confirmChoice(@NotNull Editor editor, @NotNull String match, @NotNull Caret caret, int startoff) {
    final Ref<ReplaceConfirmationChoice> result = Ref.create(ReplaceConfirmationChoice.QUIT);
    final Function1<KeyStroke, Boolean> keyStrokeProcessor = key -> {
      final ReplaceConfirmationChoice choice;
      final char c = key.getKeyChar();
      if (StringHelper.isCloseKeyStroke(key) || c == 'q') {
        choice = ReplaceConfirmationChoice.QUIT;
      } else if (c == 'y') {
        choice = ReplaceConfirmationChoice.SUBSTITUTE_THIS;
      } else if (c == 'l') {
        choice = ReplaceConfirmationChoice.SUBSTITUTE_LAST;
      } else if (c == 'n') {
        choice = ReplaceConfirmationChoice.SKIP;
      } else if (c == 'a') {
        choice = ReplaceConfirmationChoice.SUBSTITUTE_ALL;
      } else {
        return true;
      }
      result.set(choice);
      return false;
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      MotionGroup.moveCaret(editor, caret, startoff);
      final TestInputModel inputModel = TestInputModel.getInstance(editor);
      for (KeyStroke key = inputModel.nextKeyStroke(); key != null; key = inputModel.nextKeyStroke()) {
        if (!keyStrokeProcessor.invoke(key)) {
          break;
        }
      }
    }
    else {
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for this method
      final ExEntryPanel exEntryPanel = ExEntryPanel.getInstanceWithoutShortcuts();
      exEntryPanel.activate(editor, new EditorDataContext(editor, null), MessageHelper.message("replace.with.0", match), "", 1);
      MotionGroup.moveCaret(editor, caret, startoff);
      ModalEntry.INSTANCE.activate(keyStrokeProcessor);
      exEntryPanel.deactivate(true, false);
    }
    return result.get();
  }

  public int search(@NotNull Editor editor, @NotNull String command, int count, EnumSet<CommandFlags> flags, boolean moveCursor) {
    return search(editor, editor.getCaretModel().getPrimaryCaret(), command, count, flags, moveCursor);
  }

  public int search(@NotNull Editor editor, @NotNull Caret caret, @NotNull String command, int count, EnumSet<CommandFlags> flags,
                    boolean moveCursor) {
    final int res = search(editor, command, caret.getOffset(), count, flags);

    if (res != -1 && moveCursor) {
      VimPlugin.getMark().saveJumpLocation(editor);
      MotionGroup.moveCaret(editor, caret, res);
    }

    return res;
  }

  public int search(@NotNull Editor editor, @NotNull String command, int startOffset, int count, @NotNull EnumSet<CommandFlags> flags) {
    Direction dir = Direction.FORWARDS;
    char type = '/';
    String pattern = lastSearch;
    String offset = lastOffset;
    if (flags.contains(CommandFlags.FLAG_SEARCH_REV)) {
      dir = Direction.BACKWARDS;
      type = '?';
    }

    if (command.length() > 0) {
      if (command.charAt(0) != type) {
        CharPointer p = new CharPointer(command);
        CharPointer end = RegExp.skip_regexp(p.ref(0), type, true);
        pattern = p.substring(end.pointer() - p.pointer());
        if (logger.isDebugEnabled()) logger.debug("pattern=" + pattern);
        if (p.charAt() != type) {
          if (end.charAt() == type) {
            end.inc();
            offset = end.toString();
          } else {
            logger.debug("no offset");
            offset = "";
          }
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
    lastIgnoreSmartCase = false;
    if (pattern != null) {
      setLastPattern(pattern);
    }
    lastOffset = offset;
    lastDir = dir;

    if (logger.isDebugEnabled()) {
      logger.debug("lastSearch=" + lastSearch);
      logger.debug("lastOffset=" + lastOffset);
      logger.debug("lastDir=" + lastDir);
    }

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    return findItOffset(editor, startOffset, count, lastDir);
  }

  public int searchWord(@NotNull Editor editor, @NotNull Caret caret, int count, boolean whole, Direction dir) {
    TextRange range = SearchHelper.findWordUnderCursor(editor, caret);
    if (range == null) {
      logger.warn("No range was found");
      return -1;
    }

    StringBuilder pattern = new StringBuilder();
    if (whole) {
      pattern.append("\\<");
    }
    pattern.append(EditorHelper.getText(editor, range.getStartOffset(), range.getEndOffset()));
    if (whole) {
      pattern.append("\\>");
    }

    MotionGroup.moveCaret(editor, caret, range.getStartOffset());

    lastSearch = pattern.toString();
    lastIgnoreSmartCase = true;
    setLastPattern(lastSearch);
    lastOffset = "";
    lastDir = dir;

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    return findItOffset(editor, caret.getOffset(), count, lastDir);
  }

  public int searchNext(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return searchNextWithDirection(editor, caret, count, lastDir);
  }

  public int searchPrevious(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return searchNextWithDirection(editor, caret, count, lastDir.reverse());
  }

  public int searchNextFromOffset(@NotNull Editor editor, int offset, int count) {
    resetShowSearchHighlight();
    updateSearchHighlights();
    return findItOffset(editor, offset, count, Direction.FORWARDS);
  }

  private int searchNextWithDirection(@NotNull Editor editor, @NotNull Caret caret, int count, Direction dir) {
    resetShowSearchHighlight();
    updateSearchHighlights();
    final int startOffset = caret.getOffset();
    int offset = findItOffset(editor, startOffset, count, dir);
    if (offset == startOffset) {
      /* Avoid getting stuck on the current cursor position, which can
       * happen when an offset is given and the cursor is on the last char
       * in the buffer: Repeat with count + 1. */
      offset = findItOffset(editor, startOffset, count + 1, dir);
    }
    return offset;
  }

  private void resetShowSearchHighlight() {
    showSearchHighlight = OptionsManager.INSTANCE.getHlsearch().isSet();
  }

  public void clearSearchHighlight() {
    showSearchHighlight = false;
    updateSearchHighlights();
  }

  private void forceUpdateSearchHighlights() {
    SearchHighlightsHelper.updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, true);
  }

  private void updateSearchHighlights() {
    SearchHighlightsHelper.updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, false);
  }

  public void resetIncsearchHighlights() {
    SearchHighlightsHelper.updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, true);
  }

  private void highlightSearchLines(@NotNull Editor editor, int startLine, int endLine) {
    if (lastSearch != null) {
      final List<TextRange> results = findAll(editor, lastSearch, startLine, endLine,
        shouldIgnoreCase(lastSearch, lastIgnoreSmartCase));
      SearchHighlightsHelper.highlightSearchResults(editor, lastSearch, results, -1);
    }
  }

  public @Nullable TextRange getNextSearchRange(@NotNull Editor editor, int count, boolean forwards) {
    editor.getCaretModel().removeSecondaryCarets();
    TextRange current = findUnderCaret(editor);

    if (current == null || CommandStateHelper.inVisualMode(editor) && atEdgeOfGnRange(current, editor, forwards)) {
      current = findNextSearchForGn(editor, count, forwards);
    }
    else if (count > 1) {
      current = findNextSearchForGn(editor, count - 1, forwards);
    }
    return current;
  }

  private boolean atEdgeOfGnRange(@NotNull TextRange nextRange, @NotNull Editor editor, boolean forwards) {
    int currentPosition = editor.getCaretModel().getOffset();
    if (forwards) {
      return nextRange.getEndOffset() - VimPlugin.getVisualMotion().getSelectionAdj() == currentPosition;
    }
    else {
      return nextRange.getStartOffset() == currentPosition;
    }
  }

  private @Nullable TextRange findNextSearchForGn(@NotNull Editor editor, int count, boolean forwards) {
    if (forwards) {
      final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE);
      return SearchHelper.findPattern(editor, lastSearch, editor.getCaretModel().getOffset(), count, searchOptions);
    } else {
      return searchBackward(editor, editor.getCaretModel().getOffset(), count);
    }
  }

  private @Nullable TextRange findUnderCaret(@NotNull Editor editor) {
    final TextRange backSearch = searchBackward(editor, editor.getCaretModel().getOffset() + 1, 1);
    if (backSearch == null) return null;
    return backSearch.contains(editor.getCaretModel().getOffset()) ? backSearch : null;
  }

  private @Nullable TextRange searchBackward(@NotNull Editor editor, int offset, int count) {
    // Backward search returns wrongs end offset for some cases. That's why we should perform additional forward search
    final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE, SearchOptions.BACKWARDS);
    final TextRange foundBackward = SearchHelper.findPattern(editor, lastSearch, offset, count, searchOptions);
    if (foundBackward == null) return null;
    int startOffset = foundBackward.getStartOffset() - 1;
    if (startOffset < 0) startOffset = EditorHelperRt.getFileSize(editor);
    searchOptions.remove(SearchOptions.BACKWARDS);
    return SearchHelper.findPattern(editor, lastSearch, startOffset, 1, searchOptions);
  }

  private int findItOffset(@NotNull Editor editor, int startOffset, int count, Direction dir) {
    boolean wrap = OptionsManager.INSTANCE.getWrapscan().isSet();
    logger.debug("Perform search. Direction: " + dir + " wrap: " + wrap);

    int offset = 0;
    boolean offsetIsLineOffset = false;
    boolean hasEndOffset = false;

    ParsePosition pp = new ParsePosition(0);

    if (lastOffset == null) {
      logger.warn("Last offset is null. Cannot perform search");
      return -1;
    }

    if (lastOffset.length() > 0) {
      if (Character.isDigit(lastOffset.charAt(0)) || lastOffset.charAt(0) == '+' || lastOffset.charAt(0) == '-') {
        offsetIsLineOffset = true;

        if (lastOffset.equals("+")) {
          offset = 1;
        } else if (lastOffset.equals("-")) {
          offset = -1;
        } else {
          if (lastOffset.charAt(0) == '+') {
            lastOffset = lastOffset.substring(1);
          }
          NumberFormat nf = NumberFormat.getIntegerInstance();
          pp = new ParsePosition(0);
          Number num = nf.parse(lastOffset, pp);
          if (num != null) {
            offset = num.intValue();
          }
        }
      } else if ("ebs".indexOf(lastOffset.charAt(0)) != -1) {
        if (lastOffset.length() >= 2) {
          if ("+-".indexOf(lastOffset.charAt(1)) != -1) {
            offset = 1;
          }
          NumberFormat nf = NumberFormat.getIntegerInstance();
          pp = new ParsePosition(lastOffset.charAt(1) == '+' ? 2 : 1);
          Number num = nf.parse(lastOffset, pp);
          if (num != null) {
            offset = num.intValue();
          }
        }

        hasEndOffset = lastOffset.charAt(0) == 'e';
      }
    }

    /*
     * If there is a character offset, subtract it from the current
     * position, so we don't get stuck at "?pat?e+2" or "/pat/s-2".
     * Skip this if pos.col is near MAXCOL (closed fold).
     * This is not done for a line offset, because then we would not be vi
     * compatible.
     */
    if (!offsetIsLineOffset && offset != 0) {
      startOffset = Math.max(0, Math.min(startOffset - offset, EditorHelperRt.getFileSize(editor) - 1));
    }

    EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.SHOW_MESSAGES, SearchOptions.WHOLE_FILE);
    if (dir == Direction.BACKWARDS) searchOptions.add(SearchOptions.BACKWARDS);
    if (lastIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE);
    if (wrap) searchOptions.add(SearchOptions.WRAP);
    if (hasEndOffset) searchOptions.add(SearchOptions.WANT_ENDPOS);
    TextRange range = SearchHelper.findPattern(editor, lastSearch, startOffset, count, searchOptions);
    if (range == null) {
      logger.warn("No range is found");
      return -1;
    }

    int res = range.getStartOffset();

    if (offsetIsLineOffset) {
      int line = editor.offsetToLogicalPosition(range.getStartOffset()).line;
      int newLine = EditorHelper.normalizeLine(editor, line + offset);

      res = VimPlugin.getMotion().moveCaretToLineStart(editor, newLine);
    }
    else if (hasEndOffset || offset != 0) {
      int base = hasEndOffset ? range.getEndOffset() - 1 : range.getStartOffset();
      res = Math.max(0, Math.min(base + offset, EditorHelperRt.getFileSize(editor) - 1));
    }

    int ppos = pp.getIndex();
    if (ppos < lastOffset.length() - 1 && lastOffset.charAt(ppos) == ';') {
      EnumSet<CommandFlags> flags = EnumSet.noneOf(CommandFlags.class);
      if (lastOffset.charAt(ppos + 1) == '/') {
        flags.add(CommandFlags.FLAG_SEARCH_FWD);
      }
      else if (lastOffset.charAt(ppos + 1) == '?') {
        flags.add(CommandFlags.FLAG_SEARCH_REV);
      }
      else {
        return res;
      }

      if (lastOffset.length() - ppos > 2) {
        ppos++;
      }

      res = search(editor, lastOffset.substring(ppos + 1), res, 1, flags);

    }
    return res;
  }

  @RWLockLabel.SelfSynchronized
  public boolean searchAndReplace(@NotNull Editor editor, @NotNull Caret caret, @NotNull LineRange range,
                                  @NotNull @NonNls String excmd, @NonNls String exarg) {
    // Explicitly exit visual mode here, so that visual mode marks don't change when we move the cursor to a match.
    if (CommandStateHelper.inVisualMode(editor)) {
      ModeHelper.exitVisualMode(editor);
    }

    CharPointer cmd = new CharPointer(new StringBuffer(exarg));

    int which_pat;
    if ("~".equals(excmd)) {
      which_pat = RE_LAST;    /* use last used regexp */
    }
    else {
      which_pat = RE_SUBST;   /* use last substitute regexp */
    }

    CharPointer pat;
    CharPointer sub;
    char delimiter;
    /* new pattern and substitution */
    if (excmd.charAt(0) == 's' && !cmd.isNul() && !Character.isWhitespace(
        cmd.charAt()) && "0123456789cegriIp|\"".indexOf(cmd.charAt()) == -1) {
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
      else {
        /* find the end of the regexp */
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
    else {
      /* use previous pattern and substitution */
      if (lastReplace == null) {
        /* there is no previous command */
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
      do_all = OptionsManager.INSTANCE.getGdefault().isSet();
      do_ask = false;
      do_error = true;
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
      else if (cmd.charAt() == 'r') {
        /* use last used regexp */
        which_pat = RE_LAST;
      }
      else if (cmd.charAt() == 'i') {
        /* ignore case */
        do_ic = 'i';
      }
      else if (cmd.charAt() == 'I') {
        /* don't ignore case */
        do_ic = 'I';
      }
      else if (cmd.charAt() != 'p') {
        break;
      }
      cmd.inc();
    }

    int line1 = range.startLine;
    int line2 = range.endLine;

    if (line1 < 0 || line2 < 0) {
      return false;
    }

    /*
     * check for a trailing count
     */
    cmd.skipWhitespaces();
    if (Character.isDigit(cmd.charAt())) {
      int i = cmd.getDigits();
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
    cmd.skipWhitespaces();
    if (!cmd.isNul() && cmd.charAt() != '"') {
      /* if not end-of-line or comment */
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
    lastSearch = pattern;
    if (pattern != null) {
      setLastPattern(pattern);
    }

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
    regmatch.rmm_ic = shouldIgnoreCase(pattern != null ? pattern : "", false);
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

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

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
          VimPlugin.getMark().saveJumpLocation(editor);
          firstMatch = false;
        }

        String match = sp.vim_regsub_multi(regmatch, lnum, sub, 1, false);
        if (match == null) {
          return false;
        }

        int line = lnum + regmatch.startpos[0].lnum;
        CharacterPosition startpos = new CharacterPosition(lnum + regmatch.startpos[0].lnum, regmatch.startpos[0].col);
        CharacterPosition endpos = new CharacterPosition(lnum + regmatch.endpos[0].lnum, regmatch.endpos[0].col);
        int startoff = startpos.toOffset(editor);
        int endoff = endpos.toOffset(editor);
        int newend = startoff + match.length();

        if (do_all || line != lastLine) {
          boolean doReplace = true;
          if (do_ask) {
            RangeHighlighter hl = SearchHighlightsHelper.addSubstitutionConfirmationHighlight(editor, startoff, endoff);
            final ReplaceConfirmationChoice choice = confirmChoice(editor, match, caret, startoff);
            editor.getMarkupModel().removeHighlighter(hl);
            switch (choice) {
              case SUBSTITUTE_THIS:
                doReplace = true;
                break;
              case SKIP:
                doReplace = false;
                break;
              case SUBSTITUTE_ALL:
                do_ask = false;
                break;
              case QUIT:
                doReplace = false;
                got_quit = true;
                break;
              case SUBSTITUTE_LAST:
                do_all = false;
                line2 = lnum;
                doReplace = true;
                break;
            }
          }

          if (doReplace) {
            ApplicationManager.getApplication().runWriteAction(() -> editor.getDocument().replaceString(startoff, endoff, match));
            lastMatch = startoff;
            newpos = CharacterPosition.Companion.fromOffset(editor, newend);

            lnum += newpos.line - endpos.line;
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
      if (!got_quit) {
        MotionGroup.moveCaret(editor, caret,
          VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, editor.offsetToLogicalPosition(lastMatch).line));
      }
    }
    else {
      VimPlugin.showMessage(MessageHelper.message(Msg.e_patnotf2, pattern));
    }

    return true;
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
    text.addContent(Integer.toString(lastDir.toInt()));
    search.addContent(text);

    text = new Element("show-last");
    text.addContent(Boolean.toString(showSearchHighlight));
    if (logger.isDebugEnabled()) logger.debug("text=" + text);
    search.addContent(text);

    element.addContent(search);
  }

  private static @NotNull Element createElementWithText(@NotNull String name, @NotNull String text) {
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
    lastDir = Direction.Companion.fromInt(Integer.parseInt(dir.getText()));

    Element show = search.getChild("show-last");
    final ListOption vimInfo = OptionsManager.INSTANCE.getViminfo();
    final boolean disableHighlight = vimInfo.contains("h");
    showSearchHighlight = !disableHighlight && Boolean.parseBoolean(show.getText());
    if (logger.isDebugEnabled()) {
      logger.debug("show=" + show + "(" + show.getText() + ")");
      logger.debug("showSearchHighlight=" + showSearchHighlight);
    }
  }

  private static @Nullable String getSafeChildText(@NotNull Element element, @NotNull String name) {
    final Element child = element.getChild(name);
    return child != null ? StringHelper.getSafeXmlText(child) : null;
  }

  /**
   * Updates search highlights when the selected editor changes
   */
  @SuppressWarnings("unused")
  public static void fileEditorManagerSelectionChangedCallback(@NotNull FileEditorManagerEvent event) {
    VimPlugin.getSearch().updateSearchHighlights();
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("search");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }

  public static class DocumentSearchListener implements DocumentListener {

    public static DocumentSearchListener INSTANCE = new DocumentSearchListener();

    @Contract(pure = true)
    private DocumentSearchListener () {
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      for (Project project : ProjectManager.getInstance().getOpenProjects()) {
        final Document document = event.getDocument();

        for (Editor editor : EditorFactory.getInstance().getEditors(document, project)) {
          Collection<RangeHighlighter> hls = UserDataManager.getVimLastHighlighters(editor);
          if (hls == null) {
            continue;
          }

          if (logger.isDebugEnabled()) {
            logger.debug("hls=" + hls);
            logger.debug("event=" + event);
          }

          // We can only re-highlight whole lines, so clear any highlights in the affected lines
          final LogicalPosition startPosition = editor.offsetToLogicalPosition(event.getOffset());
          final LogicalPosition endPosition = editor.offsetToLogicalPosition(event.getOffset() + event.getNewLength());
          final int startLineOffset = document.getLineStartOffset(startPosition.line);
          final int endLineOffset = document.getLineEndOffset(endPosition.line);

          final Iterator<RangeHighlighter> iter = hls.iterator();
          while (iter.hasNext()) {
            final RangeHighlighter highlighter = iter.next();
            if (!highlighter.isValid() || (highlighter.getStartOffset() >= startLineOffset && highlighter.getEndOffset() <= endLineOffset)) {
              iter.remove();
              editor.getMarkupModel().removeHighlighter(highlighter);
            }
          }

          VimPlugin.getSearch().highlightSearchLines(editor, startPosition.line, endPosition.line);

          if (logger.isDebugEnabled()) {
            hls = UserDataManager.getVimLastHighlighters(editor);
            logger.debug("sl=" + startPosition.line + ", el=" + endPosition.line);
            logger.debug("hls=" + hls);
          }
        }
      }
    }
  }

  private enum ReplaceConfirmationChoice {
    SUBSTITUTE_THIS,
    SUBSTITUTE_LAST,
    SKIP,
    QUIT,
    SUBSTITUTE_ALL,
  }

  private @Nullable String lastSearch;
  private @Nullable String lastPattern;
  private @Nullable String lastSubstitute;
  private @Nullable String lastReplace;
  private @Nullable String lastOffset;
  private boolean lastIgnoreSmartCase;
  private @NotNull Direction lastDir = Direction.FORWARDS;
  private boolean showSearchHighlight = OptionsManager.INSTANCE.getHlsearch().isSet();

  private boolean do_all = false; /* do multiple substitutions per line */
  private boolean do_ask = false; /* ask for confirmation */
  private boolean do_error = true; /* if false, ignore errors */
  //private boolean do_print = false; /* print last line with subs. */
  private char do_ic = 0; /* ignore case flag */

  private static final int RE_LAST = 1;
  private static final int RE_SEARCH = 2;
  private static final int RE_SUBST = 3;

  private static final Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
