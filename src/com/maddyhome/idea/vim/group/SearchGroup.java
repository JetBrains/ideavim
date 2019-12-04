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
package com.maddyhome.idea.vim.group;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Ref;
import com.intellij.ui.ColorUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandFlags;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.LineRange;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.option.ListOption;
import com.maddyhome.idea.vim.option.OptionChangeListener;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.CharacterClasses;
import com.maddyhome.idea.vim.regexp.RegExp;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import com.maddyhome.idea.vim.ui.ModalEntry;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;
import java.util.*;

public class SearchGroup {
  public SearchGroup() {
    final OptionsManager options = OptionsManager.INSTANCE;
    options.getHlsearch().addOptionChangeListener(event -> {
      resetShowSearchHighlight();
      forceUpdateSearchHighlights();
    });

    final OptionChangeListener updateHighlightsIfVisible = event -> {
      if (showSearchHighlight) {
        forceUpdateSearchHighlights();
      }
    };
    options.getIgnorecase().addOptionChangeListener(updateHighlightsIfVisible);

    // It appears that when changing smartcase, Vim only redraws the highlights when the screen is redrawn. We can't
    // reliably copy that, so do the most intuitive thing
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

  @Nullable
  public String getLastSearch() {
    return lastSearch;
  }

  // This method is used in AceJump integration plugin
  @SuppressWarnings("unused")
  public int getLastDir() {
    return lastDir;
  }

  @Nullable
  public String getLastPattern() {
    return lastPattern;
  }

  public void resetState() {
    lastSearch = lastPattern = lastSubstitute = lastReplace = lastOffset = null;
    lastIgnoreSmartCase = false;
    lastDir = 0;
    resetShowSearchHighlight();
  }

  private void setLastPattern(@NotNull Editor editor, @NotNull String lastPattern) {
    this.lastPattern = lastPattern;
    VimPlugin.getRegister().storeTextInternal(editor, new TextRange(-1, -1),
                                                                lastPattern, SelectionType.CHARACTER_WISE, '/', false);

    VimPlugin.getHistory().addEntry(HistoryGroup.SEARCH, lastPattern);
  }

  // This method should not be private because it's used in external plugins
  @SuppressWarnings("WeakerAccess")
  @NotNull
  public static List<TextRange> findAll(@NotNull Editor editor,
                                         @NotNull String pattern,
                                         int startLine,
                                         int endLine,
                                         boolean ignoreCase) {
    final List<TextRange> results = Lists.newArrayList();
    final int lineCount = EditorHelper.getLineCount(editor);
    final int actualEndLine = endLine == -1 ? lineCount : endLine;

    final RegExp.regmmatch_T regMatch = new RegExp.regmmatch_T();
    final RegExp regExp = new RegExp();
    regMatch.regprog = regExp.vim_regcomp(pattern, 1);
    if (regMatch.regprog == null) {
      return results;
    }

    regMatch.rmm_ic = ignoreCase;

    int col = 0;
    for (int line = startLine; line <= actualEndLine; ) {
      int matchedLines = regExp.vim_regexec_multi(regMatch, editor, lineCount, line, col);
      if (matchedLines > 0) {
        final CharacterPosition startPos = new CharacterPosition(line + regMatch.startpos[0].lnum,
                                                                 regMatch.startpos[0].col);
        final CharacterPosition endPos = new CharacterPosition(line + regMatch.endpos[0].lnum,
                                                               regMatch.endpos[0].col);
        int start = startPos.toOffset(editor);
        int end = endPos.toOffset(editor);
        results.add(new TextRange(start, end));

        if (start != end) {
          line += matchedLines - 1;
          col = endPos.column;
        }
        else {
          line += matchedLines;
          col = 0;
        }
      }
      else {
        line++;
        col = 0;
      }
    }

    return results;
  }

  @NotNull
  private static ReplaceConfirmationChoice confirmChoice(@NotNull Editor editor, @NotNull String match, @NotNull Caret caret, int startoff) {
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
      exEntryPanel.activate(editor, new EditorDataContext(editor), "Replace with " + match + " (y/n/a/q/l)?", "", 1);
      MotionGroup.moveCaret(editor, caret, startoff);
      ModalEntry.INSTANCE.activate(keyStrokeProcessor);
      exEntryPanel.deactivate(true, false);
    }
    return result.get();
  }

  private static boolean shouldIgnoreCase(@NotNull String pattern, boolean ignoreSmartCase) {
    boolean sc = !ignoreSmartCase && OptionsManager.INSTANCE.getSmartcase().isSet();
    boolean ic = OptionsManager.INSTANCE.getIgnorecase().isSet();

    return ic && !(sc && StringHelper.containsUpperCase(pattern));
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
    int dir = DIR_FORWARDS;
    char type = '/';
    String pattern = lastSearch;
    String offset = lastOffset;
    if (flags.contains(CommandFlags.FLAG_SEARCH_REV)) {
      dir = DIR_BACKWARDS;
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
      setLastPattern(editor, pattern);
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

  public int searchWord(@NotNull Editor editor, @NotNull Caret caret, int count, boolean whole, int dir) {
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
    setLastPattern(editor, lastSearch);
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
    return searchNextWithDirection(editor, caret, count, -lastDir);
  }

  public int searchNextFromOffset(@NotNull Editor editor, int offset, int count) {
    resetShowSearchHighlight();
    updateSearchHighlights();
    return findItOffset(editor, offset, count, 1);
  }

  private int searchNextWithDirection(@NotNull Editor editor, @NotNull Caret caret, int count, int dir) {
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
    updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, true);
  }

  private void updateSearchHighlights() {
    updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, false);
  }

  public void resetIncsearchHighlights() {
    updateSearchHighlights(lastSearch, lastIgnoreSmartCase, showSearchHighlight, true);
  }

  public int updateIncsearchHighlights(@NotNull Editor editor, @NotNull String pattern, boolean forwards, int caretOffset, @Nullable LineRange searchRange) {
    final int searchStartOffset = searchRange != null ? EditorHelper.getLineStartOffset(editor, searchRange.getStartLine()) : caretOffset;
    final boolean showHighlights = OptionsManager.INSTANCE.getHlsearch().isSet();
    return updateSearchHighlights(pattern, false, showHighlights, searchStartOffset, searchRange, forwards, false);
  }

  private void updateSearchHighlights(@Nullable String pattern, boolean shouldIgnoreSmartCase, boolean showHighlights, boolean forceUpdate) {
    updateSearchHighlights(pattern, shouldIgnoreSmartCase, showHighlights, -1, null, true, forceUpdate);
  }

  /**
   * Refreshes current search highlights for all editors of currently active text editor/document
   */
  private int updateSearchHighlights(@Nullable String pattern, boolean shouldIgnoreSmartCase, boolean showHighlights,
                                     int initialOffset, @Nullable LineRange searchRange, boolean forwards, boolean forceUpdate) {
    int currentMatchOffset = -1;

    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      Editor current = FileEditorManager.getInstance(project).getSelectedTextEditor();
      Editor[] editors = current == null ? null : EditorFactory.getInstance().getEditors(current.getDocument(), project);
      if (editors == null) {
        continue;
      }

      for (final Editor editor : editors) {
        // Force update for the situations where the text is the same, but the ignore case values have changed.
        // E.g. Use `*` to search for a word (which ignores smartcase), then use `/<Up>` to search for the same pattern,
        // which will match smartcase. Or changing the smartcase/ignorecase settings
        if (shouldRemoveSearchHighlight(editor, pattern, showHighlights) || forceUpdate) {
          removeSearchHighlight(editor);
        }

        if (shouldAddAllSearchHighlights(editor, pattern, showHighlights)) {
          final int startLine = searchRange == null ? 0 : searchRange.getStartLine();
          final int endLine = searchRange == null ? -1 : searchRange.getEndLine();
          List<TextRange> results = findAll(editor, pattern, startLine, endLine, shouldIgnoreCase(pattern, shouldIgnoreSmartCase));
          if (!results.isEmpty()) {
            currentMatchOffset = findClosestMatch(editor, results, initialOffset, forwards);
            highlightSearchResults(editor, pattern, results, currentMatchOffset);
          }
          UserDataManager.setVimLastSearch(editor, pattern);
        }
        else if (shouldAddCurrentMatchSearchHighlight(pattern, showHighlights, initialOffset)) {
          final boolean wrap = OptionsManager.INSTANCE.getWrapscan().isSet();
          final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WHOLE_FILE);
          if (wrap) searchOptions.add(SearchOptions.WRAP);
          if (shouldIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE);
          if (!forwards) searchOptions.add(SearchOptions.BACKWARDS);
          final TextRange result = findIt(editor, pattern, initialOffset, 1, searchOptions);
          if (result != null && pattern != null) {
            currentMatchOffset = result.getStartOffset();
            final List<TextRange> results = Collections.singletonList(result);
            highlightSearchResults(editor, pattern, results, currentMatchOffset);
          }
        }
        else if (shouldMaintainCurrentMatchOffset(pattern, initialOffset)) {
          final Integer offset = UserDataManager.getVimIncsearchCurrentMatchOffset(editor);
          if (offset != null) {
            currentMatchOffset = offset;
          }
        }
      }
    }

    return currentMatchOffset;
  }

  /**
   * Remove current search highlights if hlSearch is false, or if the pattern is changed
   */
  @Contract("_, _, false -> true; _, null, true -> false")
  private boolean shouldRemoveSearchHighlight(@NotNull Editor editor, String newPattern, boolean hlSearch) {
    return !hlSearch || (newPattern != null && !newPattern.equals(UserDataManager.getVimLastSearch(editor)));
  }

  /**
   * Add search highlights if hlSearch is true and the pattern is changed
   */
  @Contract("_, _, false -> false; _, null, true -> false")
  private boolean shouldAddAllSearchHighlights(@NotNull Editor editor, @Nullable String newPattern, boolean hlSearch) {
    return hlSearch && newPattern != null && !newPattern.equals(UserDataManager.getVimLastSearch(editor)) && !Objects.equals(newPattern, "");
  }

  /**
   * Add search highlight for current match if hlsearch is false and we're performing incsearch highlights
   */
  @Contract("_, true, _ -> false")
  private boolean shouldAddCurrentMatchSearchHighlight(@Nullable String pattern, boolean hlSearch, int initialOffset) {
    return !hlSearch && isIncrementalSearchHighlights(initialOffset) && pattern != null && pattern.length() > 0;
  }

  /**
   * Keep the current match offset if the pattern is still valid and we're performing incremental search highlights
   * This will keep the caret position when editing the offset in e.g. `/foo/e+1`
   */
  @Contract("null, _ -> false")
  private boolean shouldMaintainCurrentMatchOffset(@Nullable String pattern, int initialOffset) {
    return pattern != null && pattern.length() > 0 && isIncrementalSearchHighlights(initialOffset);
  }

  /**
   * initialOffset is only valid if we're highlighting incsearch
   */
  @Contract(pure = true)
  private boolean isIncrementalSearchHighlights(int initialOffset) {
    return initialOffset != -1;
  }

  private void highlightSearchLines(@NotNull Editor editor, int startLine, int endLine) {
    if (lastSearch != null) {
      final List<TextRange> results = findAll(editor, lastSearch, startLine, endLine, shouldIgnoreCase(lastSearch, lastIgnoreSmartCase));
      highlightSearchResults(editor, lastSearch, results, -1);
    }
  }

  private int findClosestMatch(@NotNull Editor editor, @NotNull List<TextRange> results, int initialOffset, boolean forwards) {
    if (results.isEmpty() || initialOffset == -1) {
      return -1;
    }

    final int size = EditorHelper.getFileSize(editor);
    final TextRange max = Collections.max(results, (r1, r2) -> {
      final int d1 = distance(r1, initialOffset, forwards, size);
      final int d2 = distance(r2, initialOffset, forwards, size);
      if (d1 < 0 && d2 >= 0) {
        return Integer.MAX_VALUE;
      }
      return d2 - d1;
    });

    if (!OptionsManager.INSTANCE.getWrapscan().isSet()) {
      final int start = max.getStartOffset();
      if (forwards && start < initialOffset) {
        return -1;
      }
      else if (start >= initialOffset) {
        return -1;
      }
    }

    return max.getStartOffset();
  }

  @Nullable
  public TextRange getNextSearchRange(@NotNull Editor editor, int count, boolean forwards) {
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

  @Nullable
  private TextRange findNextSearchForGn(@NotNull Editor editor, int count, boolean forwards) {
    if (forwards) {
      final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE);
      return findIt(editor, lastSearch, editor.getCaretModel().getOffset(), count, searchOptions);
    } else {
      return searchBackward(editor, editor.getCaretModel().getOffset(), count);
    }
  }

  @Nullable
  private TextRange findUnderCaret(@NotNull Editor editor) {
    final TextRange backSearch = searchBackward(editor, editor.getCaretModel().getOffset() + 1, 1);
    if (backSearch == null) return null;
    return backSearch.contains(editor.getCaretModel().getOffset()) ? backSearch : null;
  }

  @Nullable
  private TextRange searchBackward(@NotNull Editor editor, int offset, int count) {
    // Backward search returns wrongs end offset for some cases. That's why we should perform additional forward search
    final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE, SearchOptions.BACKWARDS);
    final TextRange foundBackward = findIt(editor, lastSearch, offset, count, searchOptions);
    if (foundBackward == null) return null;
    int startOffset = foundBackward.getStartOffset() - 1;
    if (startOffset < 0) startOffset = EditorHelper.getFileSize(editor);
    searchOptions.remove(SearchOptions.BACKWARDS);
    return findIt(editor, lastSearch, startOffset, 1, searchOptions);
  }

  private static int distance(@NotNull TextRange range, int pos, boolean forwards, int size) {
    final int start = range.getStartOffset();
    if (start <= pos) {
      return forwards ? size - pos + start : pos - start;
    }
    else {
      return forwards ? start - pos : pos + size - start;
    }
  }

  private static TextRange findIt(@NotNull Editor editor, @Nullable String pattern, int startOffset, int count, EnumSet<SearchOptions> searchOptions) {
    if (pattern == null || pattern.length() == 0) {
      logger.warn("Pattern is null or empty. Cannot perform search");
      return null;
    }

    int dir = searchOptions.contains(SearchOptions.BACKWARDS) ? DIR_BACKWARDS : DIR_FORWARDS;

    //RE sp;
    RegExp sp;
    RegExp.regmmatch_T regmatch = new RegExp.regmmatch_T();
    regmatch.rmm_ic = shouldIgnoreCase(pattern, searchOptions.contains(SearchOptions.IGNORE_SMARTCASE));
    sp = new RegExp();
    regmatch.regprog = sp.vim_regcomp(pattern, 1);
    if (regmatch.regprog == null) {
      if (logger.isDebugEnabled()) logger.debug("bad pattern: " + pattern);
      return null;
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

    CharacterPosition lpos = CharacterPosition.Companion.fromOffset(editor, startOffset);
    RegExp.lpos_T pos = new RegExp.lpos_T();
    pos.lnum = lpos.line;
    pos.col = lpos.column;

    int found;
    int lnum;           /* no init to shut up Apollo cc */
    //RegExp.regmmatch_T regmatch;
    CharPointer ptr;
    int matchcol;
    RegExp.lpos_T matchpos;
    RegExp.lpos_T endpos = new RegExp.lpos_T();
    int loop;
    RegExp.lpos_T start_pos;
    boolean at_first_line;
    int extra_col = dir == DIR_FORWARDS ? 1 : 0;
    boolean match_ok;
    long nmatched;
    //int         submatch = 0;
    boolean first_match = true;

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
      if (dir == DIR_BACKWARDS && start_pos.col == 0) {
        lnum = pos.lnum - 1;
        at_first_line = false;
      }
      else {
        lnum = pos.lnum;
      }

      int lcount = EditorHelper.getLineCount(editor);
      for (loop = 0; loop <= 1; ++loop)   /* loop twice if 'wrapscan' set */ {
        if (!searchOptions.contains(SearchOptions.WHOLE_FILE)) {
          startLine = lnum;
          endLine = lnum + 1;
        }
        for (; lnum >= startLine && lnum < endLine; lnum += dir, at_first_line = false) {
          /*
          * Look for a match somewhere in the line.
          */
          nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, 0);
          if (nmatched > 0) {
            /* match may actually be in another line when using \zs */
            matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
            endpos = new RegExp.lpos_T(regmatch.endpos[0]);

            ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum + matchpos.lnum));

            /*
            * Forward search in the first line: match should be after
            * the start position. If not, continue at the end of the
            * match (this is vi compatible) or on the next char.
            */
            if (dir == DIR_FORWARDS && at_first_line) {
              match_ok = true;
              /*
              * When match lands on a NUL the cursor will be put
              * one back afterwards, compare with that position,
              * otherwise "/$" will get stuck on end of line.
              */
              while (matchpos.lnum == 0
                && (searchOptions.contains(SearchOptions.WANT_ENDPOS) && first_match
                  ? (nmatched == 1 && endpos.col - 1 < start_pos.col + extra_col)
                  : (matchpos.col - (ptr.charAt(matchpos.col) == '\u0000' ? 1 : 0) < start_pos.col + extra_col))) {
                if (nmatched > 1) {
                  /* end is in next line, thus no match in
                   * this line */
                  match_ok = false;
                  break;
                }
                matchcol = endpos.col;
                /* for empty match: advance one char */
                if (matchcol == matchpos.col && ptr.charAt(matchcol) != '\u0000') {
                  ++matchcol;
                }
                if (ptr.charAt(matchcol) == '\u0000' ||
                    (nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, matchcol)) == 0) {
                  match_ok = false;
                  break;
                }
                matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
                endpos = new RegExp.lpos_T(regmatch.endpos[0]);

                /* Need to get the line pointer again, a
                 * multi-line search may have made it invalid. */
                ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum));
              }
              if (!match_ok) {
                continue;
              }
            }
            if (dir == DIR_BACKWARDS) {
              /*
              * Now, if there are multiple matches on this line,
              * we have to get the last one. Or the last one before
              * the cursor, if we're on that line.
              * When putting the new cursor at the end, compare
              * relative to the end of the match.
              */
              match_ok = false;
              for (;;) {
                if (loop != 0 ||
                  (searchOptions.contains(SearchOptions.WANT_ENDPOS)
                    ? (lnum + regmatch.endpos[0].lnum < start_pos.lnum || (lnum + regmatch.endpos[0].lnum == start_pos.lnum && regmatch.endpos[0].col - 1 < start_pos.col + extra_col))
                    : (lnum + regmatch.startpos[0].lnum < start_pos.lnum || (lnum + regmatch.startpos[0].lnum == start_pos.lnum && regmatch.startpos[0].col < start_pos.col + extra_col)))) {
                  /* Remember this position, we use it if it's
                   * the last match in the line. */
                  match_ok = true;
                  matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
                  endpos = new RegExp.lpos_T(regmatch.endpos[0]);
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
                if (matchcol == matchpos.col && ptr.charAt(matchcol) != '\u0000') {
                  ++matchcol;
                }
                if (ptr.charAt(matchcol) == '\u0000' ||
                    (nmatched = sp.vim_regexec_multi(regmatch, editor, lcount, lnum + matchpos.lnum, matchcol)) == 0) {
                  break;
                }

                /* Need to get the line pointer again, a
                 * multi-line search may have made it invalid. */
                ptr = new CharPointer(EditorHelper.getLineBuffer(editor, lnum + matchpos.lnum));
              }

              /*
              * If there is only a match after the cursor, skip
              * this match.
              */
              if (!match_ok) {
                continue;
              }
            }

            pos.lnum = lnum + matchpos.lnum;
            pos.col = matchpos.col;
            endpos.lnum = lnum + endpos.lnum;
            found = 1;
            first_match = false;

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
        if (!searchOptions.contains(SearchOptions.WRAP) || found != 0) {
          break;
        }

        /*
        * If 'wrapscan' is set we continue at the other end of the file.
        * If 'shortmess' does not contain 's', we give a message.
        * This message is also remembered in keep_msg for when the screen
        * is redrawn. The keep_msg is cleared whenever another message is
        * written.
        */
        if (dir == DIR_BACKWARDS)    /* start second loop at the other end */ {
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
      if (searchOptions.contains(SearchOptions.SHOW_MESSAGES)) {
        if (searchOptions.contains(SearchOptions.WRAP)) {
          VimPlugin.showMessage(MessageHelper.message(Msg.e_patnotf2, pattern));
        }
        else if (lnum <= 0) {
          VimPlugin.showMessage(MessageHelper.message(Msg.E384, pattern));
        }
        else {
          VimPlugin.showMessage(MessageHelper.message(Msg.E385, pattern));
        }
      }
      return null;
    }

    //return new TextRange(editor.logicalPositionToOffset(new LogicalPosition(pos.lnum, pos.col)),
    //    editor.logicalPositionToOffset(new LogicalPosition(endpos.lnum, endpos.col)));
    //return new TextRange(editor.logicalPositionToOffset(new LogicalPosition(pos.lnum, 0)) + pos.col,
    //    editor.logicalPositionToOffset(new LogicalPosition(endpos.lnum, 0)) + endpos.col);
    return new TextRange(new CharacterPosition(pos.lnum, pos.col).toOffset(editor),
                         new CharacterPosition(endpos.lnum, endpos.col).toOffset(editor));
  }

  private static void highlightSearchResults(@NotNull Editor editor, @NotNull String pattern, List<TextRange> results,
                                             int currentMatchOffset) {
    Collection<RangeHighlighter> highlighters = UserDataManager.getVimLastHighlighters(editor);
    if (highlighters == null) {
      highlighters = new ArrayList<>();
      UserDataManager.setVimLastHighlighters(editor, highlighters);
    }

    for (TextRange range : results) {
      final boolean current = range.getStartOffset() == currentMatchOffset;
      final RangeHighlighter highlighter = highlightMatch(editor, range.getStartOffset(), range.getEndOffset(), current, pattern);
      highlighters.add(highlighter);
    }

    UserDataManager.setVimIncsearchCurrentMatchOffset(editor, currentMatchOffset);
  }

  private int findItOffset(@NotNull Editor editor, int startOffset, int count, int dir) {
    boolean wrap = OptionsManager.INSTANCE.getWrapscan().isSet();
    logger.info("Perform search. Direction: " + dir + " wrap: " + wrap);

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
      startOffset = Math.max(0, Math.min(startOffset - offset, EditorHelper.getFileSize(editor) - 1));
    }

    EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.SHOW_MESSAGES, SearchOptions.WHOLE_FILE);
    if (dir == DIR_BACKWARDS) searchOptions.add(SearchOptions.BACKWARDS);
    if (lastIgnoreSmartCase) searchOptions.add(SearchOptions.IGNORE_SMARTCASE);
    if (wrap) searchOptions.add(SearchOptions.WRAP);
    if (hasEndOffset) searchOptions.add(SearchOptions.WANT_ENDPOS);
    TextRange range = findIt(editor, lastSearch, startOffset, count, searchOptions);
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
      res = Math.max(0, Math.min(base + offset, EditorHelper.getFileSize(editor) - 1));
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

      return res;
    }
    else {
      return res;
    }
  }

  @RWLockLabel.SelfSynchronized
  public boolean searchAndReplace(@NotNull Editor editor, @NotNull Caret caret, @NotNull LineRange range,
                                  @NotNull String excmd, String exarg) {
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

    int line1 = range.getStartLine();
    int line2 = range.getEndLine();

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
      setLastPattern(editor, pattern);
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
            RangeHighlighter hl = highlightConfirm(editor, startoff, endoff);
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

  @NotNull
  private RangeHighlighter highlightConfirm(@NotNull Editor editor, int start, int end) {
    TextAttributes color = new TextAttributes(
      editor.getColorsScheme().getColor(EditorColors.SELECTION_FOREGROUND_COLOR),
      editor.getColorsScheme().getColor(EditorColors.SELECTION_BACKGROUND_COLOR),
      editor.getColorsScheme().getColor(EditorColors.CARET_COLOR),
      EffectType.ROUNDED_BOX, Font.PLAIN
    );
    return editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.SELECTION,
                                                       color, HighlighterTargetArea.EXACT_RANGE);
  }

  @NotNull
  private static RangeHighlighter highlightMatch(@NotNull Editor editor, int start, int end, boolean current, String tooltip) {
    TextAttributes attributes = editor.getColorsScheme().getAttributes(EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES);
    if (current) {
      // This mimics what IntelliJ does with the Find live preview
      attributes = attributes.clone();
      attributes.setEffectType(EffectType.ROUNDED_BOX);
      attributes.setEffectColor(editor.getColorsScheme().getColor(EditorColors.CARET_COLOR));
    }
    if (attributes.getErrorStripeColor() == null) {
      attributes.setErrorStripeColor(getFallbackErrorStripeColor(attributes, editor.getColorsScheme()));
    }
    final RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(start, end, HighlighterLayer.SELECTION - 1,
      attributes, HighlighterTargetArea.EXACT_RANGE);
    highlighter.setErrorStripeTooltip(tooltip);
    return highlighter;
  }

  /**
   * Return a valid error stripe colour based on editor background
   *
   * Based on HighlightManager#addRangeHighlight behaviour, which we can't use because it will hide highlights when
   * hitting Escape
   */
  private static @Nullable Color getFallbackErrorStripeColor(TextAttributes attributes, EditorColorsScheme colorsScheme) {
    if (attributes.getBackgroundColor() != null) {
      boolean isDark = ColorUtil.isDark(colorsScheme.getDefaultBackground());
      return isDark ? attributes.getBackgroundColor().brighter() : attributes.getBackgroundColor().darker();
    }
    return null;
  }

  private static void removeSearchHighlight(@NotNull Editor editor) {
    UserDataManager.setVimLastSearch(editor, null);

    Collection<RangeHighlighter> ehl = UserDataManager.getVimLastHighlighters(editor);
    if (ehl == null) {
      return;
    }

    for (RangeHighlighter rh : ehl) {
      editor.getMarkupModel().removeHighlighter(rh);
    }

    ehl.clear();

    UserDataManager.setVimLastHighlighters(editor, null);
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
    final ListOption vimInfo = OptionsManager.INSTANCE.getViminfo();
    final boolean disableHighlight = vimInfo.contains("h");
    showSearchHighlight = !disableHighlight && Boolean.parseBoolean(show.getText());
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

  /**
   * Updates search highlights when the selected editor changes
   */
  @SuppressWarnings("unused")
  public static void fileEditorManagerSelectionChangedCallback(@NotNull FileEditorManagerEvent event) {
    VimPlugin.getSearch().updateSearchHighlights();
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
          Collection hls = UserDataManager.getVimLastHighlighters(editor);
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

          final Iterator iter = hls.iterator();
          while (iter.hasNext()) {
            final RangeHighlighter highlighter = (RangeHighlighter) iter.next();
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

  private enum SearchOptions {
    BACKWARDS,
    WANT_ENDPOS,
    WRAP,
    SHOW_MESSAGES,
    WHOLE_FILE,
    IGNORE_SMARTCASE,
  }

  @Nullable private String lastSearch;
  @Nullable private String lastPattern;
  @Nullable private String lastSubstitute;
  @Nullable private String lastReplace;
  @Nullable private String lastOffset;
  private boolean lastIgnoreSmartCase;
  private int lastDir;
  private boolean showSearchHighlight = OptionsManager.INSTANCE.getHlsearch().isSet();

  private boolean do_all = false; /* do multiple substitutions per line */
  private boolean do_ask = false; /* ask for confirmation */
  private boolean do_error = true; /* if false, ignore errors */
  //private boolean do_print = false; /* print last line with subs. */
  private char do_ic = 0; /* ignore case flag */

  private static final int RE_LAST = 1;
  private static final int RE_SEARCH = 2;
  private static final int RE_SUBST = 3;

  private static final int DIR_FORWARDS = 1;
  private static final int DIR_BACKWARDS = -1;

  private static final Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
