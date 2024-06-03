/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.util.Ref;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.*;
import com.maddyhome.idea.vim.command.MotionType;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.Direction;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.ex.ranges.LineRange;
import com.maddyhome.idea.vim.helper.*;
import com.maddyhome.idea.vim.history.HistoryConstants;
import com.maddyhome.idea.vim.newapi.*;
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.CharacterClasses;
import com.maddyhome.idea.vim.regexp.RegExp;
import com.maddyhome.idea.vim.ui.ModalEntry;
import com.maddyhome.idea.vim.vimscript.model.VimLContext;
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString;
import com.maddyhome.idea.vim.vimscript.model.expressions.Expression;
import com.maddyhome.idea.vim.vimscript.model.expressions.SimpleExpression;
import com.maddyhome.idea.vim.vimscript.model.functions.handlers.SubmatchFunctionHandler;
import com.maddyhome.idea.vim.vimscript.parser.VimscriptParser;
import kotlin.Pair;
import kotlin.Triple;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.*;

import javax.swing.*;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;

import static com.maddyhome.idea.vim.api.VimInjectorKt.*;
import static com.maddyhome.idea.vim.helper.SearchHelperKtKt.shouldIgnoreCase;
import static com.maddyhome.idea.vim.newapi.IjVimInjectorKt.globalIjOptions;
import static com.maddyhome.idea.vim.register.RegisterConstants.LAST_SEARCH_REGISTER;

/**
 * @deprecated Replace with IjVimSearchGroup
 */
@Deprecated
public class SearchGroup extends IjVimSearchGroup {
  public SearchGroup() {
    super();
    if (!globalIjOptions(injector).getUseNewRegex()) {
      // We use the global option listener instead of the effective listener that gets called for each affected editor
      // because we handle updating the affected editors ourselves (e.g., we can filter for visible windows).
      VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.hlsearch, () -> {
        resetShowSearchHighlight();
        forceUpdateSearchHighlights();
      });

      final GlobalOptionChangeListener updateHighlightsIfVisible = () -> {
        if (showSearchHighlight) {
          forceUpdateSearchHighlights();
        }
      };
      VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.ignorecase, updateHighlightsIfVisible);
      VimPlugin.getOptionGroup().addGlobalOptionChangeListener(Options.smartcase, updateHighlightsIfVisible);
    }
  }

  public void turnOn() {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.updateSearchHighlights(false);
      return;
    }
    updateSearchHighlights();
  }

  public void turnOff() {
    final boolean show = showSearchHighlight;
    clearSearchHighlight();
    showSearchHighlight = show;
  }

  @TestOnly
  @Override
  public void resetState() {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.resetState();
      return;
    }
    lastPatternIdx = RE_SEARCH;
    lastSearch = lastSubstitute = lastReplace = null;
    lastPatternOffset = "";
    lastIgnoreSmartCase = false;
    lastDir = Direction.FORWARDS;
    resetShowSearchHighlight();
  }

  /**
   * Get the last pattern used for searching. Does not include pattern used in substitution
   *
   * @return The pattern used for last search. Can be null
   */
  @Override
  public @Nullable String getLastSearchPattern() {
    if (globalIjOptions(injector).getUseNewRegex()) return super.getLastSearchPattern();
    return lastSearch;
  }

  /**
   * Get the last pattern used in substitution.
   * @return The pattern used for the last substitute command. Can be null
   */
  @Override
  public @Nullable String getLastSubstitutePattern() {
    if (globalIjOptions(injector).getUseNewRegex()) return super.getLastSubstitutePattern();
    return lastSubstitute;
  }

  /**
   * Get the pattern last used for either searching or substitution.
   *
   * @return The pattern last used for either searching or substitution. Can be null
   */
  @Override
  protected @Nullable String getLastUsedPattern() {
    if (globalIjOptions(injector).getUseNewRegex()) return super.getLastUsedPattern();
    return switch (lastPatternIdx) {
      case RE_SEARCH -> lastSearch;
      case RE_SUBST -> lastSubstitute;
      default -> null;
    };
  }

  /**
   * Get the last used search direction
   *
   * <p>This method is used in the AceJump integration plugin</p>
   *
   * @return Returns the integer value of Direction. 1 for FORWARDS, -1 for BACKWARDS
   */
  @SuppressWarnings("unused")
  public int getLastDir() {
    return lastDir.toInt();
  }

  /**
   * Set the last used pattern
   *
   * <p>Only updates the last used flag if the pattern is new. This prevents incorrectly setting the last used pattern
   * when search or substitute doesn't explicitly set the pattern but uses the last saved value. It also ensures the
   * last used pattern is updated when a new pattern with the same value is used.</p>
   *
   * <p>Also saves the text to the search register and history.</p>
   *
   * @param pattern       The pattern to remember
   * @param which_pat     Which pattern to save - RE_SEARCH, RE_SUBST or RE_BOTH
   * @param isNewPattern  Flag to indicate if the pattern is new, or comes from a last used pattern. True means to
   *                      update the last used pattern index
   */
  private void setLastUsedPattern(@NotNull String pattern, int which_pat, boolean isNewPattern) {
    // Only update the last pattern with a new input pattern. Do not update if we're reusing the last pattern
    if ((which_pat == RE_SEARCH || which_pat == RE_BOTH) && isNewPattern) {
      lastSearch = pattern;
      lastPatternIdx = RE_SEARCH;
    }
    if ((which_pat == RE_SUBST || which_pat == RE_BOTH) && isNewPattern) {
      lastSubstitute = pattern;
      lastPatternIdx = RE_SUBST;
    }

    // Vim never actually sets this register, but looks it up on request
    VimPlugin.getRegister().storeTextSpecial(LAST_SEARCH_REGISTER, pattern);

    // This will remove an existing entry and add it back to the end, and is expected to do so even if the string value
    // is the same
    VimPlugin.getHistory().addEntry(HistoryConstants.SEARCH, pattern);
  }

  /**
   * Sets the last search state, purely for tests
   *
   * @param editor          The editor to update
   * @param pattern         The pattern to save. This is the last search pattern, not the last substitute pattern
   * @param patternOffset   The pattern offset, e.g. `/{pattern}/{offset}`
   * @param direction       The direction to search
   */
  @TestOnly
  public void setLastSearchState(@SuppressWarnings("unused") @NotNull Editor editor, @NotNull String pattern,
                                 @NotNull String patternOffset, Direction direction) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.setLastSearchState(pattern, patternOffset, direction);
      return;
    }
    setLastUsedPattern(pattern, RE_SEARCH, true);
    lastIgnoreSmartCase = false;
    lastPatternOffset = patternOffset;
    lastDir = direction;
  }


  // *******************************************************************************************************************
  //
  // Search
  //
  // *******************************************************************************************************************

  /**
   * Find all occurrences of the pattern
   *
   * @deprecated Use SearchHelper#findAll instead. Kept for compatibility with existing plugins
   *
   * @param editor      The editor to search in
   * @param pattern     The pattern to search for
   * @param startLine   The start line of the range to search for
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
    return injector.getSearchHelper().findAll(new IjVimEditor(editor), pattern, startLine, endLine, ignoreCase);
  }

  /**
   * Process the search command, searching for the pattern from the given document offset
   *
   * <p>Parses the pattern from the search command and will search for the given pattern, immediately setting RE_SEARCH
   * and RE_LAST. Updates the search register and history and search highlights. Also updates last pattern offset and
   * direction. scanwrap and ignorecase come from options. Will ensure that RE_LAST is valid if the given pattern is
   * empty by using the existing RE_SEARCH or falling back to RE_SUBST. Will error if both are unset.</p>
   *
   * <p>Will parse the entire command, including patterns separated by `;`</p>
   *
   * <p>Note that this method should only be called when the ex command argument should be parsed, and start should be
   * updated. I.e. only for the search commands. Consider using SearchHelper.findPattern to find text.</p>
   *
   * <p>Equivalent to normal.c:nv_search + search.c:do_search</p>
   *
   * @param editor      The editor to search in
   * @param startOffset The offset to start searching from
   * @param command     The command text entered into the Ex entry panel. Does not include the leading `/` or `?`.
   *                    Can include a trailing offset, e.g. /{pattern}/{offset}, or multiple commands separated by a semicolon.
   *                    If the pattern is empty, the last used (search? substitute?) pattern (and offset?) is used.
   * @param dir         The direction to search
   * @return            Pair containing the offset to the next occurrence of the pattern, and the [MotionType] based on
   *                    the search offset. The value will be `null` if no result is found.
   */
  @Nullable
  @Override
  public Pair<Integer, MotionType> processSearchCommand(@NotNull VimEditor editor,
                                                        @NotNull String command,
                                                        int startOffset,
                                                        int count1,
                                                        @NotNull Direction dir) {

    if (globalIjOptions(injector).getUseNewRegex()) return super.processSearchCommand(editor, command, startOffset, count1, dir);

    boolean isNewPattern = false;
    String pattern = null;
    String patternOffset = null;

    final char type = dir == Direction.FORWARDS ? '/' : '?';

    if (!command.isEmpty()) {
      if (command.charAt(0) != type) {
        CharPointer p = new CharPointer(command);
        CharPointer end = RegExp.skip_regexp(p.ref(0), type, true);

        pattern = p.substring(end.pointer() - p.pointer());
        isNewPattern = true;

        if (logger.isDebugEnabled()) logger.debug("pattern=" + pattern);

        if (p.charAt() == type) {
          p.inc();
          patternOffset = p.toString();
          if (logger.isDebugEnabled()) logger.debug("offset=" + patternOffset);
        }
        if (end.charAt(0) == type) {
          end.inc();
          patternOffset = end.toString();
          if (logger.isDebugEnabled()) logger.debug("Pattern contains offset " + patternOffset);
        }
        else {
          logger.debug("no offset");
          patternOffset = "";
        }
      }
      else if (command.length() == 1) {
        patternOffset = "";
      }
      else {
        patternOffset = command.substring(1);
        if (logger.isDebugEnabled()) logger.debug("offset=" + patternOffset);
      }
    }

    // Vim's logic is spread over several methods (do_search -> searchit -> search_regcomp), and rather tricky to follow
    // When searching, it will search for the given pattern or RE_LAST. Pattern offset always come from RE_SEARCH.
    // If the pattern is explicitly entered, this is saved as RE_SEARCH and this becomes RE_LAST.
    //    Pattern offset is also parsed, and is saved (to RE_SEARCH)
    // If the pattern is missing, Vim checks RE_SEARCH:
    //    If RE_SEARCH is set, the given pattern is set to an empty string, meaning search will use RE_LAST.
    //       If RE_LAST is unset, then error e_noprevre (searchit -> search_regcomp)
    //       BUT: RE_LAST is *always* set. The default is RE_SEARCH, which we know is valid. If it's RE_SUBST, it's been
    //       explicitly set and is valid.
    //       Pattern offset always comes from RE_SEARCH.
    //    If RE_SEARCH is unset, fall back to RE_SUBST:
    //       If RE_SUBST is set, save this as RE_SEARCH, which becomes RE_LAST
    //          RE_SUBST does not have pattern offsets to save, so pattern offset will be RE_SEARCH - unset/default
    //       If RE_SUBST is unset, error e_noprevre
    // Pattern offset is always used from RE_SEARCH. Only saved when explicitly entered
    // Direction is saved in do_search
    // IgnoreSmartCase is only ever set for searching words (`*`, `#`, `g*`, etc.) and is reset for all other operations

    if (pattern == null || pattern.isEmpty()) {
      pattern = getLastSearchPattern();
      if (pattern == null || pattern.isEmpty()) {
        isNewPattern = true;
        pattern = getLastSubstitutePattern();
        if (pattern == null || pattern.isEmpty()) {
          VimPlugin.showMessage(MessageHelper.message("e_noprevre"));
          return null;
        }
      }
      if (patternOffset == null || patternOffset.isEmpty()) {
        patternOffset = lastPatternOffset;
      }
    }

    // Save the pattern. If it's explicitly entered, or comes from RE_SUBST, isNewPattern is true, and this becomes
    // RE_LAST. If it comes from RE_SEARCH, then a) it's not null and b) we know that RE_LAST is already valid.
    setLastUsedPattern(pattern, RE_SEARCH, isNewPattern);
    lastIgnoreSmartCase = false;
    lastPatternOffset = patternOffset;  // This might include extra search patterns separated by `;`
    lastDir = dir;

    if (logger.isDebugEnabled()) {
      logger.debug("lastSearch=" + lastSearch);
      logger.debug("lastOffset=" + lastPatternOffset);
      logger.debug("lastDir=" + lastDir);
    }

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    return findItOffset(((IjVimEditor)editor).getEditor(), startOffset, count1, lastDir);
  }

  /**
   * Process the pattern being used as a search range
   *
   * <p>Find the next offset of the search pattern, without processing the pattern further. This is not a full search
   * pattern, as handled by processSearchCommand. It does not contain a pattern offset and there are not multiple
   * patterns separated by `;`. Ranges do support multiple patterns, separation with both `;` and `,` and a `+/-{num}`
   * suffix, but these are all handled by the range itself.</p>
   *
   * <p>This method is essentially a wrapper around SearchHelper.findPattern (via findItOffset) that updates state and
   * highlighting.</p>
   *
   * @param editor        The editor to search in
   * @param pattern       The pattern to search for. Does not include leading or trailing `/` and `?` characters
   * @param patternOffset The offset applied to the range. Not used during searching, but used to populate lastPatternOffset
   * @param startOffset   The offset to start searching from
   * @param direction     The direction to search in
   * @return              The offset of the match or -1 if not found
   */
  public int processSearchRange(@NotNull Editor editor, @NotNull String pattern, int patternOffset, int startOffset, @NotNull Direction direction) {

    // Will set RE_LAST, required by findItOffset
    // IgnoreSmartCase and Direction are always reset.
    // PatternOffset is cleared before searching. ExRanges will add/subtract the line offset from the final search range
    // pattern, but we need the value to update lastPatternOffset for future searches.
    // TODO: Consider improving pattern offset handling
    setLastUsedPattern(pattern, RE_SEARCH, true);
    lastIgnoreSmartCase = false;
    lastPatternOffset = ""; // Do not apply a pattern offset yet!
    lastDir = direction;

    if (logger.isDebugEnabled()) {
      logger.debug("lastSearch=" + lastSearch);
      logger.debug("lastOffset=" + lastPatternOffset);
      logger.debug("lastDir=" + lastDir);
    }

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    final Pair<Integer, MotionType> result = findItOffset(editor, startOffset, 1, lastDir);

    // Set lastPatternOffset AFTER searching so it doesn't affect the result
    lastPatternOffset = patternOffset != 0 ? Integer.toString(patternOffset) : "";

    if (logger.isDebugEnabled()) {
      logger.debug("lastSearch=" + lastSearch);
      logger.debug("lastOffset=" + lastPatternOffset);
      logger.debug("lastDir=" + lastDir);
    }

    return result != null ? result.getFirst() : -1;
  }

  /**
   * Search for the word under the given caret
   *
   * <p>Updates RE_SEARCH and RE_LAST, last pattern offset and direction. Ignore smart case is set to true. Highlights
   * are updated. scanwrap and ignorecase come from options.</p>
   *
   * <p>Equivalent to normal.c:nv_ident</p>
   *
   * @param editor  The editor to search in
   * @param caret   The caret to use to look for the current word
   * @param count   Search for the nth occurrence of the current word
   * @param whole   Include word boundaries in the search pattern
   * @param dir     Which direction to search
   * @return        The offset of the result or the start of the word under the caret if not found. Returns -1 on error
   */
  @Override
  public int searchWord(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret, int count, boolean whole, @NotNull Direction dir) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.searchWord(editor, caret, count, whole, dir);
    TextRange range = SearchHelper.findWordUnderCursor(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret());
    if (range == null) {
      logger.warn("No range was found");
      return -1;
    }

    @NotNull final Editor editor1 = ((IjVimEditor)editor).getEditor();
    final int start = range.getStartOffset();
    final int end = range.getEndOffset();
    final String pattern = SearchHelper.makeSearchPattern(
      EngineEditorHelperKt.getText(new IjVimEditor(editor1), start, end), whole);

    // Updates RE_LAST, ready for findItOffset
    // Direction is always saved
    // IgnoreSmartCase is always set to true
    // There is no pattern offset available
    setLastUsedPattern(pattern, RE_SEARCH, true);
    lastIgnoreSmartCase = true;
    lastPatternOffset = "";
    lastDir = dir;

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    final Pair<Integer, MotionType> offsetAndMotion =
      findItOffset(((IjVimEditor)editor).getEditor(), range.getStartOffset(), count, lastDir);
    return offsetAndMotion == null ? range.getStartOffset() : offsetAndMotion.getFirst();
  }

  /**
   * Find the next occurrence of the last used pattern
   *
   * <p>Searches for RE_LAST, including last used pattern offset. Direction is the same as the last used direction.
   * E.g. `?foo` followed by `n` will search backwards. scanwrap and ignorecase come from options.</p>
   *
   * @param editor  The editor to search in
   * @param caret   Used to get the offset to start searching from
   * @param count   Find the nth occurrence
   * @return        The offset of the next match, or -1 if not found
   */
  @Override
  public int searchNext(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret, int count) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.searchNext(editor, caret, count);
    return searchNextWithDirection(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(), count, lastDir);
  }

  /**
   * Find the next occurrence of the last used pattern
   *
   * <p>Searches for RE_LAST, including last used pattern offset. Direction is the opposite of the last used direction.
   * E.g. `?foo` followed by `N` will be forwards. scanwrap and ignorecase come from options.</p>
   *
   * @param editor  The editor to search in
   * @param caret   Used to get the offset to starting searching from
   * @param count   Find the nth occurrence
   * @return        The offset of the next match, or -1 if not found
   */
  @Override
  public int searchPrevious(@NotNull VimEditor editor, @NotNull ImmutableVimCaret caret, int count) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.searchPrevious(editor, caret, count);
    return searchNextWithDirection(((IjVimEditor)editor).getEditor(), ((IjVimCaret)caret).getCaret(), count,
                                   lastDir.reverse());
  }

  // See normal.c:nv_next
  private int searchNextWithDirection(@NotNull Editor editor, @NotNull Caret caret, int count, Direction dir) {
    resetShowSearchHighlight();
    updateSearchHighlights();
    final int startOffset = caret.getOffset();
    Pair<Integer, MotionType> offsetAndMotion = findItOffset(editor, startOffset, count, dir);
    if (offsetAndMotion != null && offsetAndMotion.getFirst() == startOffset) {
      /* Avoid getting stuck on the current cursor position, which can
       * happen when an offset is given and the cursor is on the last char
       * in the buffer: Repeat with count + 1. */
      offsetAndMotion = findItOffset(editor, startOffset, count + 1, dir);
    }
    return offsetAndMotion != null ? offsetAndMotion.getFirst() : -1;
  }


  // *******************************************************************************************************************
  //
  // Substitute
  //
  // *******************************************************************************************************************

  /**
   * Parse and execute the substitute command
   *
   * <p>Updates state for the last substitute pattern (RE_SUBST and RE_LAST) and last replacement text. Updates search
   * history and register. Also updates stored substitution flags.</p>
   *
   * <p>Saves the current location as a jump location and restores caret location after completion. If confirmation is
   * enabled and the substitution is abandoned, the current caret location is kept, and the original location is not
   * restored.</p>
   *
   * <p>See ex_cmds.c:ex_substitute</p>
   *
   * @param editor  The editor to search in
   * @param caret   The caret to use for initial search offset, and to move for interactive substitution
   * @param context
   * @param range   Only search and substitute within the given line range. Must be valid
   * @param excmd   The command part of the ex command line, e.g. `s` or `substitute`, or `~`
   * @param exarg   The argument to the substitute command, such as `/{pattern}/{string}/[flags]`
   * @return True if the substitution succeeds, false on error. Will succeed even if nothing is modified
   */
  @Override
  @RWLockLabel.SelfSynchronized
  public boolean processSubstituteCommand(@NotNull VimEditor editor,
                                          @NotNull VimCaret caret,
                                          @NotNull ExecutionContext context,
                                          @NotNull LineRange range,
                                          @NotNull @NonNls String excmd,
                                          @NotNull @NonNls String exarg,
                                          @NotNull VimLContext parent) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      return super.processSubstituteCommand(editor, caret, context, range, excmd, exarg, parent);
    }

    // Explicitly exit visual mode here, so that visual mode marks don't change when we move the cursor to a match.
    List<ExException> exceptions = new ArrayList<>();
    if (CommandStateHelper.inVisualMode(((IjVimEditor) editor).getEditor())) {
      EngineModeExtensionsKt.exitVisualMode(editor);
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
          which_pat = RE_SEARCH;              /* use last '/' pattern */
        }
        pat = new CharPointer("");       /* empty search pattern */
        delimiter = cmd.charAt();             /* remember delimiter character */
        cmd.inc();
      }
      else {
        /* find the end of the regexp */
        which_pat = RE_LAST;                  /* use last used regexp */
        delimiter = cmd.charAt();             /* remember delimiter character */
        cmd.inc();
        pat = cmd.ref(0);                     /* remember start of search pat */
        cmd = RegExp.skip_regexp(cmd, delimiter, true);
        if (cmd.charAt() == delimiter)        /* end delimiter found */ {
          cmd.set('\u0000').inc();            /* replace it with a NUL */
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
      // :h :&& - "Note that :s and :& don't keep the flags"
      do_all = options(injector, editor).getGdefault();
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
      else if (cmd.charAt() != 'p' && cmd.charAt() != 'l' && cmd.charAt() != '#' && cmd.charAt() != 'n') {
        // TODO: Support printing last changed line, with options for line number/list format
        // TODO: Support 'n' to report number of matches without substituting
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
      line2 = EngineEditorHelperKt.normalizeLine(editor, line1 + i - 1);
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

    Pair<Boolean, Triple<Object, String, Object>> booleanregmmatch_tPair = search_regcomp(pat, which_pat, RE_SUBST);
    if (!booleanregmmatch_tPair.getFirst()) {
      if (do_error) {
        VimPlugin.showMessage(MessageHelper.message(Msg.e_invcmd));
        VimPlugin.indicateError();
      }
      return false;
    }
    RegExp.regmmatch_T regmatch = (RegExp.regmmatch_T)booleanregmmatch_tPair.getSecond().getFirst();
    String pattern = booleanregmmatch_tPair.getSecond().getSecond();
    RegExp sp = (RegExp)booleanregmmatch_tPair.getSecond().getThird();

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

    resetShowSearchHighlight();
    forceUpdateSearchHighlights();

    int start = ((IjVimEditor)editor).getEditor().getDocument().getLineStartOffset(line1);
    int end = ((IjVimEditor)editor).getEditor().getDocument().getLineEndOffset(line2);

    if (logger.isDebugEnabled()) {
      logger.debug("search range=[" + start + "," + end + "]");
      logger.debug("pattern=" + pattern + ", replace=" + sub);
    }

    int lastMatch = -1;
    int lastLine = -1;
    int searchcol = 0;
    boolean firstMatch = true;
    boolean got_quit = false;
    int lcount = editor.lineCount();
    Expression expression = null;
    for (int lnum = line1; lnum <= line2 && !got_quit; ) {
      CharacterPosition newpos = null;
      int nmatch = sp.vim_regexec_multi(regmatch, editor, lcount, lnum, searchcol);
      if (nmatch > 0) {
        if (firstMatch) {
          VimInjectorKt.injector.getJumpService().saveJumpLocation(editor);
          firstMatch = false;
        }

        String match = sp.vim_regsub_multi(regmatch, lnum, sub, 1, false);
        if (sub.charAt(0) == '\\' && sub.charAt(1) == '=') {
          String exprString = sub.toString().substring(2);
          expression = VimscriptParser.INSTANCE.parseExpression(exprString);
          if (expression == null) {
            exceptions.add(new ExException("E15: Invalid expression: " + exprString));
            expression = new SimpleExpression(new VimString(""));
          }
        }
        else if (match == null) {
          return false;
        }

        int line = lnum + regmatch.startpos[0].lnum;
        CharacterPosition startpos = new CharacterPosition(lnum + regmatch.startpos[0].lnum, regmatch.startpos[0].col);
        CharacterPosition endpos = new CharacterPosition(lnum + regmatch.endpos[0].lnum, regmatch.endpos[0].col);
        int startoff = startpos.toOffset(((IjVimEditor)editor).getEditor());
        int endoff = (endpos.line >= lcount) ? (int) editor.fileSize() : endpos.toOffset(((IjVimEditor)editor).getEditor());

        if (do_all || line != lastLine) {
          boolean doReplace = true;
          if (do_ask) {
            RangeHighlighter hl =
              SearchHighlightsHelper.addSubstitutionConfirmationHighlight(((IjVimEditor)editor).getEditor(), startoff,
                                                                          endoff);
            final ReplaceConfirmationChoice choice = confirmChoice(((IjVimEditor)editor).getEditor(), context, match, ((IjVimCaret)caret).getCaret(), startoff);
            ((IjVimEditor)editor).getEditor().getMarkupModel().removeHighlighter(hl);
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
            SubmatchFunctionHandler.Companion.getInstance().setLatestMatch(
              ((IjVimEditor)editor).getEditor().getDocument().getText(new com.intellij.openapi.util.TextRange(startoff, endoff)));
            caret.moveToOffset(startoff);
            if (expression != null) {
              try {
                match = expression.evaluate(editor, context, parent).toInsertableString();
              }
              catch (Exception e) {
                exceptions.add((ExException)e);
                match = "";
              }
            }

            String finalMatch = match;
            ApplicationManager.getApplication().runWriteAction(
              () -> ((IjVimEditor)editor).getEditor().getDocument().replaceString(startoff, endoff, finalMatch));
            lastMatch = startoff;
            int newend = startoff + match.length();
            newpos = CharacterPosition.Companion.fromOffset(((IjVimEditor)editor).getEditor(), newend);

            lnum += newpos.line - endpos.line;
            line2 += newpos.line - endpos.line;
          }
        }

        lastLine = line;

        if (do_all && startoff != endoff) {
          if (newpos != null) {
            lnum = newpos.line;
            searchcol = newpos.column;
          }
          else {
            lnum += Math.max(1, nmatch - 1);
            searchcol = endpos.column;
          }
        }
        else {
          lnum += Math.max(1, nmatch - 1);
          searchcol = 0;
        }
      }
      else {
        lnum += Math.max(1, nmatch - 1);
        searchcol = 0;
      }
    }

    if (!got_quit) {
      if (lastMatch != -1) {
        caret.moveToOffset(
          VimPlugin.getMotion().moveCaretToLineStartSkipLeading(editor, editor.offsetToBufferPosition(lastMatch).getLine()));
      }
      else {
        // E486: Pattern not found: {0}
        VimPlugin.showMessage(MessageHelper.message("E486", pattern));
      }
    }

    SubmatchFunctionHandler.Companion.getInstance().setLatestMatch("");

    // todo throw multiple exceptions at once
    if (!exceptions.isEmpty()) {
      VimPlugin.indicateError();
      VimPlugin.showMessage(exceptions.get(0).toString());
    }

    // TODO: Support reporting number of changes (:help 'report')

    return true;
  }

  @Override
  public void setLastSearchPattern(@Nullable String lastSearchPattern) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.setLastSearchPattern(lastSearchPattern);
      return;
    }
    this.lastSearch = lastSearchPattern;
    if (showSearchHighlight) {
      resetIncsearchHighlights();
      updateSearchHighlights();
    }
  }

  @Override
  public void setLastSubstitutePattern(@Nullable String lastSubstitutePattern) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.setLastSubstitutePattern(lastSubstitutePattern);
      return;
    }
    this.lastSubstitute = lastSubstitutePattern;
  }

  @Override
  public int processSearchRange(@NotNull VimEditor editor,
                                @NotNull String pattern,
                                int patternOffset,
                                int startOffset,
                                @NotNull Direction direction) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.processSearchRange(editor, pattern, patternOffset, startOffset, direction);
    return processSearchRange(((IjVimEditor) editor).getEditor(), pattern, patternOffset, startOffset, direction);
  }

  //public Pair<Boolean, Triple<RegExp.regmmatch_T, String, RegExp>> search_regcomp(CharPointer pat,
  public Pair<Boolean, Triple<Object, String, Object>> search_regcomp(CharPointer pat,
                                                                                  int which_pat,
                                                                                  int patSave) {
    // We don't need to worry about lastIgnoreSmartCase, it's always false. Vim resets after checking, and it only sets
    // it to true when searching for a word with `*`, `#`, `g*`, etc.
    boolean isNewPattern = true;
    String pattern = "";
    if (pat == null || pat.isNul()) {
      isNewPattern = false;
      if (which_pat == RE_LAST) {
        which_pat = lastPatternIdx;
      }
      String errorMessage = switch (which_pat) {
        case RE_SEARCH -> {
          pattern = lastSearch;
          yield MessageHelper.message("e_nopresub");
        }
        case RE_SUBST -> {
          pattern = lastSubstitute;
          yield MessageHelper.message("e_noprevre");
        }
        default -> null;
      };

      // Pattern was never defined
      if (pattern == null) {
        VimPlugin.showMessage(errorMessage);
        return new Pair<>(false, null);
      }
    }
    else {
      pattern = pat.toString();
    }

    // Set RE_SUBST and RE_LAST, but only for explicitly typed patterns. Reused patterns are not saved/updated
    setLastUsedPattern(pattern, patSave, isNewPattern);

    // Always reset after checking, only set for nv_ident
    lastIgnoreSmartCase = false;
    // Substitute does NOT reset last direction or pattern offset!

    RegExp sp;
    RegExp.regmmatch_T regmatch = new RegExp.regmmatch_T();
    regmatch.rmm_ic = shouldIgnoreCase(pattern, false);
    sp = new RegExp();
    regmatch.regprog = sp.vim_regcomp(pattern, 1);
    if (regmatch.regprog == null) {
      return new Pair<>(false, null);
    }
    return new Pair<>(true, new Triple<>(regmatch, pattern, sp));
  }

  private static @NotNull ReplaceConfirmationChoice confirmChoice(@NotNull Editor editor,
                                                                  @NotNull ExecutionContext context,
                                                                  @NotNull String match, @NotNull Caret caret, int startoff) {
    final Ref<ReplaceConfirmationChoice> result = Ref.create(ReplaceConfirmationChoice.QUIT);
    final Function1<KeyStroke, Boolean> keyStrokeProcessor = key -> {
      final ReplaceConfirmationChoice choice;
      final char c = key.getKeyChar();
      if (StringAndKeysKt.isCloseKeyStroke(key) || c == 'q') {
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
      // TODO: Handle <C-E> and <C-Y>
      result.set(choice);
      return false;
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      new IjVimCaret(caret).moveToOffset(startoff);
      final TestInputModel inputModel = TestInputModel.getInstance(editor);
      for (KeyStroke key = inputModel.nextKeyStroke(); key != null; key = inputModel.nextKeyStroke()) {
        if (!keyStrokeProcessor.invoke(key)) {
          break;
        }
      }
    }
    else {
      // XXX: The Ex entry panel is used only for UI here, its logic might be inappropriate for this method
      final VimCommandLine exEntryPanel = injector.getCommandLine().createWithoutShortcuts(
        new IjVimEditor(editor),
        context,
        MessageHelper.message("replace.with.0", match),
        ""
      );
      new IjVimCaret(caret).moveToOffset(startoff);
      ModalEntry.INSTANCE.activate(new IjVimEditor(editor), keyStrokeProcessor);
      exEntryPanel.deactivate(true, false);
    }
    return result.get();
  }


  // *******************************************************************************************************************
  //
  // gn implementation
  //
  // *******************************************************************************************************************

  /**
   * Find the range of the next occurrence of the last used search pattern
   *
   * <p>Used for the implementation of the gn and gN commands.</p>
   *
   * <p>Searches for the range of the next occurrence of the last used search pattern (RE_LAST). If the current primary
   * caret is inside the range of an occurrence, will return that instance. Uses the last used search pattern. Does not
   * update any other state. Direction is explicit, not from state.</p>
   *
   * @param editor    The editor to search in
   * @param count     Find the nth occurrence
   * @param forwards  Search forwards or backwards
   * @return          The TextRange of the next occurrence or null if not found
   */
  @Override
  public @Nullable TextRange getNextSearchRange(@NotNull VimEditor editor, int count, boolean forwards) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.getNextSearchRange(editor, count, forwards);
    editor.removeSecondaryCarets();
    TextRange current = findUnderCaret(editor);

    if (current == null || CommandStateHelper.inVisualMode(((IjVimEditor)editor).getEditor()) && atEdgeOfGnRange(current, ((IjVimEditor)editor).getEditor(), forwards)) {
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

  private @Nullable TextRange findNextSearchForGn(@NotNull VimEditor editor, int count, boolean forwards) {
    if (forwards) {
      final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE);
      return VimInjectorKt.getInjector().getSearchHelper().findPattern(editor, getLastUsedPattern(), editor.primaryCaret().getOffset(), count, searchOptions);
    } else {
      return searchBackward(editor, editor.primaryCaret().getOffset(), count);
    }
  }

  @Override
  @Nullable
  public TextRange searchBackward(@NotNull VimEditor editor, int offset, int count) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.searchBackward(editor, offset, count);
    // Backward search returns wrongs end offset for some cases. That's why we should perform additional forward search
    final EnumSet<SearchOptions> searchOptions = EnumSet.of(SearchOptions.WRAP, SearchOptions.WHOLE_FILE, SearchOptions.BACKWARDS);
    final TextRange foundBackward = VimInjectorKt.getInjector().getSearchHelper().findPattern(editor, getLastUsedPattern(), offset, count, searchOptions);
    if (foundBackward == null) return null;
    int startOffset = foundBackward.getStartOffset() - 1;
    if (startOffset < 0) startOffset = (int)editor.fileSize();
    searchOptions.remove(SearchOptions.BACKWARDS);
    return VimInjectorKt.getInjector().getSearchHelper().findPattern(editor, getLastUsedPattern(), startOffset, 1, searchOptions);
  }


  // *******************************************************************************************************************
  //
  // Highlighting
  //
  // *******************************************************************************************************************
  //region Search highlights
  @Override
  public void clearSearchHighlight() {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.clearSearchHighlight();
      return;
    }
    showSearchHighlight = false;
    updateSearchHighlights();
  }

  private void forceUpdateSearchHighlights() {
    // Sync the search highlights to the current state, potentially hiding or showing highlights. Will always update,
    // even if the pattern hasn't changed.
    SearchHighlightsHelper.updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true);
  }

  private void updateSearchHighlights() {
    // Sync the search highlights to the current state, potentially hiding or showing highlights. Will only update if
    // the pattern has changed.
    SearchHighlightsHelper.updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, false);
  }

  /**
   * Reset the search highlights to the last used pattern after highlighting incsearch results.
   */
  @Override
  public void resetIncsearchHighlights() {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.resetIncsearchHighlights();
      return;
    }
    SearchHighlightsHelper.updateSearchHighlights(getLastUsedPattern(), lastIgnoreSmartCase, showSearchHighlight, true);
  }

  private void resetShowSearchHighlight() {
    showSearchHighlight = globalOptions(injector).getHlsearch();
  }

  private void highlightSearchLines(@NotNull Editor editor, int startLine, int endLine) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.highlightSearchLines(new IjVimEditor(editor), startLine, endLine);
      return;
    }
    final String pattern = getLastUsedPattern();
    if (pattern != null) {
      final List<TextRange> results = injector.getSearchHelper().findAll(new IjVimEditor(editor), pattern, startLine, endLine,
        shouldIgnoreCase(pattern, lastIgnoreSmartCase));
      SearchHighlightsHelper.highlightSearchResults(editor, pattern, results, -1);
    }
  }

  /**
   * Updates search highlights when the selected editor changes
   */
  public void fileEditorManagerSelectionChangedCallback(@SuppressWarnings("unused") @NotNull FileEditorManagerEvent event) {
    if (globalIjOptions(injector).getUseNewRegex()) {
      super.updateSearchHighlights(false);
      return;
    }
    VimPlugin.getSearch().updateSearchHighlights();
  }

  @Override
  public Integer findDecimalNumber(@NotNull String line) {
    if (globalIjOptions(injector).getUseNewRegex()) return super.findDecimalNumber(line);
    Pair<TextRange, NumberType> searchResult = SearchHelper.findNumberInText(line, 0, false, false, false);
    if (searchResult != null) {
      TextRange range = searchResult.component1();
      return Integer.parseInt(line.substring(range.getStartOffset(), range.getEndOffset()));
    }
    return null;
  }

  @NotNull
  @Override
  public Direction getLastSearchDirection() {
    if (globalIjOptions(injector).getUseNewRegex()) return super.getLastSearchDirection();
    return lastDir;
  }

  // *******************************************************************************************************************
  //
  // Implementation details
  //
  // *******************************************************************************************************************

  /**
   * Searches for the RE_LAST saved pattern, applying the last saved pattern offset. Will loop over trailing search
   * commands.
   *
   * <p>Make sure that RE_LAST has been updated before calling this. wrapscan and ignorecase come from options.</p>
   *
   * <p>See search.c:do_search (and a little bit of normal.c:normal_search)</p>
   *
   * @param editor        The editor to search in
   * @param startOffset   The offset to search from
   * @param count         Find the nth occurrence
   * @param dir           The direction to search in
   * @return              Pair containing the offset to the next occurrence of the pattern, and the [MotionType] based
   *                      on the search offset. The value will be `null` if no result is found.
   */
  @Nullable
  private Pair<Integer, MotionType> findItOffset(@NotNull Editor editor, int startOffset, int count, Direction dir) {
    boolean wrap = globalOptions(injector).getWrapscan();
    logger.debug("Perform search. Direction: " + dir + " wrap: " + wrap);

    int offset = 0;
    boolean offsetIsLineOffset = false;
    boolean hasEndOffset = false;

    ParsePosition pp = new ParsePosition(0);

    if (!lastPatternOffset.isEmpty()) {
      if (Character.isDigit(lastPatternOffset.charAt(0)) || lastPatternOffset.charAt(0) == '+' || lastPatternOffset.charAt(0) == '-') {
        offsetIsLineOffset = true;

        if (lastPatternOffset.equals("+")) {
          offset = 1;
        } else if (lastPatternOffset.equals("-")) {
          offset = -1;
        } else {
          if (lastPatternOffset.charAt(0) == '+') {
            lastPatternOffset = lastPatternOffset.substring(1);
          }
          NumberFormat nf = NumberFormat.getIntegerInstance();
          pp = new ParsePosition(0);
          Number num = nf.parse(lastPatternOffset, pp);
          if (num != null) {
            offset = num.intValue();
          }
        }
      } else if ("ebs".indexOf(lastPatternOffset.charAt(0)) != -1) {
        if (lastPatternOffset.length() >= 2) {
          if ("+-".indexOf(lastPatternOffset.charAt(1)) != -1) {
            offset = 1;
          }
          NumberFormat nf = NumberFormat.getIntegerInstance();
          pp = new ParsePosition(lastPatternOffset.charAt(1) == '+' ? 2 : 1);
          Number num = nf.parse(lastPatternOffset, pp);
          if (num != null) {
            offset = num.intValue();
          }
        }

        hasEndOffset = lastPatternOffset.charAt(0) == 'e';
      }
    }

    // `/{pattern}/{offset}` is inclusive if offset contains `e`, and linewise if there's a line offset
    final MotionType motionType;
    if (offset != 0 && !hasEndOffset) {
      motionType = MotionType.LINE_WISE;
    }
    else if (hasEndOffset) {
      motionType = MotionType.INCLUSIVE;
    }
    else {
      motionType = MotionType.EXCLUSIVE;
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

    // Uses RE_LAST. We know this is always set before being called
    TextRange range = injector.getSearchHelper().findPattern(new IjVimEditor(editor), getLastUsedPattern(), startOffset, count, searchOptions);
    if (range == null) {
      logger.warn("No range is found");
      return null;
    }

    int res = range.getStartOffset();

    if (offsetIsLineOffset) {
      int line = editor.offsetToLogicalPosition(range.getStartOffset()).line;
      int newLine = EngineEditorHelperKt.normalizeLine(new IjVimEditor(editor), line + offset);
      res = VimPlugin.getMotion().moveCaretToLineStart(new IjVimEditor(editor), newLine);
    }
    else if (hasEndOffset || offset != 0) {
      int base = hasEndOffset ? range.getEndOffset() - 1 : range.getStartOffset();
      res = Math.max(0, Math.min(base + offset, EditorHelperRt.getFileSize(editor) - 1));
    }

    int ppos = pp.getIndex();
    if (ppos < lastPatternOffset.length() - 1 && lastPatternOffset.charAt(ppos) == ';') {
      final Direction nextDir;
      if (lastPatternOffset.charAt(ppos + 1) == '/') {
        nextDir = Direction.FORWARDS;
      }
      else if (lastPatternOffset.charAt(ppos + 1) == '?') {
        nextDir = Direction.BACKWARDS;
      }
      else {
        return new Pair(res, motionType);
      }

      if (lastPatternOffset.length() - ppos > 2) {
        ppos++;
      }

      Pair<Integer, MotionType> offsetAndMotion =
        processSearchCommand(new IjVimEditor(editor), lastPatternOffset.substring(ppos + 1), res, 1, nextDir);
      res = offsetAndMotion != null ? offsetAndMotion.getFirst() : -1;
    }

    return new Pair<Integer, MotionType>(res, motionType);
  }

  private enum ReplaceConfirmationChoice {
    SUBSTITUTE_THIS,
    SUBSTITUTE_LAST,
    SKIP,
    QUIT,
    SUBSTITUTE_ALL,
  }

  // Vim saves the patterns used for searching (`/`) and substitution (`:s`) separately
  // viminfo records them as `# Last Search Pattern` and `# Last Substitute Search Pattern` respectively
  // Vim also saves flags in viminfo - ~<magic><smartcase><line><end><off>[~]
  // The trailing tilde tracks which was the last used pattern, but line/end/off is only used for search, not substitution
  // Search values can contain new lines, etc. Vim saves these as CTRL chars, e.g. ^M
  // Before saving, Vim reads existing viminfo, merges and writes
  private @Nullable String lastSearch;             // Pattern used for last search command (`/`)
  private @Nullable String lastSubstitute;         // Pattern used for last substitute command (`:s`)
  private int lastPatternIdx;                      // Which pattern was used last? RE_SEARCH or RE_SUBST?
  private @Nullable String lastReplace;            // `# Last Substitute String` from viminfo
  private @NotNull String lastPatternOffset = "";  // /{pattern}/{offset}. Do not confuse with caret offset!
  private boolean lastIgnoreSmartCase;
  private @NotNull Direction lastDir = Direction.FORWARDS;
  private boolean showSearchHighlight = globalOptions(injector).getHlsearch();

  private boolean do_all = false; /* do multiple substitutions per line */
  private boolean do_ask = false; /* ask for confirmation */
  private boolean do_error = true; /* if false, ignore errors */
  //private boolean do_print = false; /* print last line with subs. */
  private char do_ic = 0; /* ignore case flag */

  // Matching the values defined in Vim. Do not change these values, they are used as indexes
  public static final int RE_SEARCH = 0; // Save/use search pattern
  public static final int RE_SUBST = 1;  // Save/use substitute pattern
  public static final int RE_BOTH = 2;   // Save to both patterns
  public static final int RE_LAST = 2;   // Use last used pattern if "pat" is NULL

  private static final Logger logger = Logger.getInstance(SearchGroup.class.getName());
}
