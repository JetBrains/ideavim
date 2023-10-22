/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper;

import com.google.common.collect.Lists;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.api.EngineEditorHelperKt;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.state.mode.Mode;
import com.maddyhome.idea.vim.state.VimStateMachine;
import com.maddyhome.idea.vim.common.CharacterPosition;
import com.maddyhome.idea.vim.common.Direction;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.newapi.IjVimCaret;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import com.maddyhome.idea.vim.regexp.CharPointer;
import com.maddyhome.idea.vim.regexp.RegExp;
import kotlin.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.maddyhome.idea.vim.api.VimInjectorKt.injector;
import static com.maddyhome.idea.vim.api.VimInjectorKt.options;
import static com.maddyhome.idea.vim.helper.SearchHelperKtKt.checkInString;
import static com.maddyhome.idea.vim.helper.SearchHelperKtKt.shouldIgnoreCase;

/**
 * Helper methods for searching text
 */
public class SearchHelper {

  public static String makeSearchPattern(String pattern, Boolean whole) {
    return whole ? "\\<" + pattern + "\\>" : pattern;
  }

  /**
   * Find text matching the given pattern.
   *
   * <p>See search.c:searchit</p>
   *
   * @param editor        The editor to search in
   * @param pattern       The pattern to search for
   * @param startOffset   The offset to start searching from
   * @param count         Find the nth next occurrence of the pattern. Must be 1 or greater.
   * @param searchOptions A set of options, such as direction and wrap
   * @return A TextRange representing the result, or null
   */
  @Nullable
  public static TextRange findPattern(@NotNull Editor editor,
                                      @Nullable String pattern,
                                      int startOffset,
                                      int count,
                                      EnumSet<SearchOptions> searchOptions) {
    if (pattern == null || pattern.length() == 0) {
      logger.warn("Pattern is null or empty. Cannot perform search");
      return null;
    }

    Direction dir = searchOptions.contains(SearchOptions.BACKWARDS) ? Direction.BACKWARDS : Direction.FORWARDS;

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
    int extra_col = dir == Direction.FORWARDS ? 1 : 0;
    boolean match_ok;
    long nmatched;
    //int         submatch = 0;
    boolean first_match = true;

    int lineCount = new IjVimEditor(editor).lineCount();
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
      if (dir == Direction.BACKWARDS && start_pos.col == 0) {
        lnum = pos.lnum - 1;
        at_first_line = false;
      } else {
        lnum = pos.lnum;
      }

      int lcount = new IjVimEditor(editor).lineCount();
      for (loop = 0; loop <= 1; ++loop)   /* loop twice if 'wrapscan' set */ {
        if (!searchOptions.contains(SearchOptions.WHOLE_FILE)) {
          startLine = lnum;
          endLine = lnum + 1;
        }
        for (; lnum >= startLine && lnum < endLine; lnum += dir.toInt(), at_first_line = false) {
          /*
           * Look for a match somewhere in the line.
           */
          nmatched = sp.vim_regexec_multi(regmatch, new IjVimEditor(editor), lcount, lnum, 0);
          if (nmatched > 0) {
            /* match may actually be in another line when using \zs */
            matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
            endpos = new RegExp.lpos_T(regmatch.endpos[0]);

            ptr = new CharPointer(EngineEditorHelperKt.getLineBuffer(new IjVimEditor(editor), lnum + matchpos.lnum));

            /*
             * Forward search in the first line: match should be after
             * the start position. If not, continue at the end of the
             * match (this is vi compatible) or on the next char.
             */
            if (dir == Direction.FORWARDS && at_first_line) {
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
                  (nmatched = sp.vim_regexec_multi(regmatch, new IjVimEditor(editor), lcount, lnum, matchcol)) == 0) {
                  match_ok = false;
                  break;
                }
                matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
                endpos = new RegExp.lpos_T(regmatch.endpos[0]);

                /* Need to get the line pointer again, a
                 * multi-line search may have made it invalid. */
                ptr = new CharPointer(EngineEditorHelperKt.getLineBuffer(new IjVimEditor(editor), lnum));
              }
              if (!match_ok) {
                continue;
              }
            }
            if (dir == Direction.BACKWARDS) {
              /*
               * Now, if there are multiple matches on this line,
               * we have to get the last one. Or the last one before
               * the cursor, if we're on that line.
               * When putting the new cursor at the end, compare
               * relative to the end of the match.
               */
              match_ok = false;
              for (; ; ) {
                if (loop != 0 ||
                  (searchOptions.contains(SearchOptions.WANT_ENDPOS)
                    ? (lnum + regmatch.endpos[0].lnum < start_pos.lnum || (lnum + regmatch.endpos[0].lnum == start_pos.lnum && regmatch.endpos[0].col - 1 < start_pos.col + extra_col))
                    : (lnum + regmatch.startpos[0].lnum < start_pos.lnum || (lnum + regmatch.startpos[0].lnum == start_pos.lnum && regmatch.startpos[0].col < start_pos.col + extra_col)))) {
                  /* Remember this position, we use it if it's
                   * the last match in the line. */
                  match_ok = true;
                  matchpos = new RegExp.lpos_T(regmatch.startpos[0]);
                  endpos = new RegExp.lpos_T(regmatch.endpos[0]);
                } else {
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
                  (nmatched = sp.vim_regexec_multi(regmatch, new IjVimEditor(editor), lcount, lnum + matchpos.lnum, matchcol)) == 0) {
                  break;
                }

                /* Need to get the line pointer again, a
                 * multi-line search may have made it invalid. */
                ptr = new CharPointer(EngineEditorHelperKt.getLineBuffer(new IjVimEditor(editor), lnum + matchpos.lnum));
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
        if (dir == Direction.BACKWARDS)    /* start second loop at the other end */ {
          lnum = lineCount - 1;
          //if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG))
          //    give_warning((char_u *)_(top_bot_msg), TRUE);
        } else {
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
        } else if (lnum <= 0) {
          VimPlugin.showMessage(MessageHelper.message(Msg.E384, pattern));
        } else {
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

  /**
   * Find all occurrences of the pattern.
   *
   * @param editor     The editor to search in
   * @param pattern    The pattern to search for
   * @param startLine  The start line of the range to search for
   * @param endLine    The end line of the range to search for, or -1 for the whole document
   * @param ignoreCase Case sensitive or insensitive searching
   * @return A list of TextRange objects representing the results
   */
  public static @NotNull
  List<TextRange> findAll(@NotNull Editor editor,
                          @NotNull String pattern,
                          int startLine,
                          int endLine,
                          boolean ignoreCase) {
    final List<TextRange> results = Lists.newArrayList();
    final int lineCount = new IjVimEditor(editor).lineCount();
    final int actualEndLine = endLine == -1 ? lineCount - 1 : endLine;

    final RegExp.regmmatch_T regMatch = new RegExp.regmmatch_T();
    final RegExp regExp = new RegExp();
    regMatch.regprog = regExp.vim_regcomp(pattern, 1);
    if (regMatch.regprog == null) {
      return results;
    }

    regMatch.rmm_ic = ignoreCase;

    int col = 0;
    for (int line = startLine; line <= actualEndLine; ) {
      int matchedLines = regExp.vim_regexec_multi(regMatch, new IjVimEditor(editor), lineCount, line, col);
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
        } else {
          line += matchedLines;
          col = 0;
        }
      } else {
        line++;
        col = 0;
      }
    }

    return results;
  }

  public static int findSection(@NotNull Editor editor, @NotNull Caret caret, char type, int dir, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int line = caret.getLogicalPosition().line + dir;
    int maxline = new IjVimEditor(editor).lineCount();
    int res = -1;

    while (line > 0 && line < maxline && count > 0) {
      int offset = new IjVimEditor(editor).getLineStartOffset(line);
      if (offset < chars.length()) { // This if was added because of exception and here might be different logic
        char ch = chars.charAt(offset);
        if (ch == type || ch == '\u000C') {
          res = offset;
          count--;
        }
      }

      line += dir;
    }

    if (res == -1) {
      res = dir < 0 ? 0 : chars.length() - 1;
    }

    return res;
  }

  public static int findUnmatchedBlock(@NotNull Editor editor, @NotNull Caret caret, char type, int count) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int loc = blockChars.indexOf(type);
    // What direction should we go now (-1 is backward, 1 is forward)
    Direction dir = loc % 2 == 0 ? Direction.BACKWARDS : Direction.FORWARDS;
    // Which character did we find and which should we now search for
    char match = blockChars.charAt(loc);
    char found = blockChars.charAt(loc - dir.toInt());

    if (pos < chars.length() && chars.charAt(pos) == type) {
      pos += dir.toInt();
    }
    return findBlockLocation(chars, found, match, dir, pos, count, false);
  }

  /**
   * Find block enclosing the caret
   *
   * @param editor  The editor to search in
   * @param caret   The caret currently at
   * @param type    The type of block, e.g. (, [, {, <
   * @param count   Find the nth next occurrence of the block
   * @param isOuter Control whether the match includes block character
   * @return When block is found, return text range matching where end offset is exclusive,
   * otherwise return null
   */
  public static @Nullable
  TextRange findBlockRange(@NotNull Editor editor,
                           @NotNull Caret caret,
                           char type,
                           int count,
                           boolean isOuter) {
    CharSequence chars = editor.getDocument().getCharsSequence();
    int pos = caret.getOffset();
    int start = caret.getSelectionStart();
    int end = caret.getSelectionEnd();

    int loc = blockChars.indexOf(type);
    char close = blockChars.charAt(loc + 1);

    // extend the range for blank line after type and before close, as they are excluded when inner match
    if (!isOuter) {
      if (start > 1 && chars.charAt(start - 2) == type && chars.charAt(start - 1) == '\n') {
        start--;
      }
      if (end < chars.length() && chars.charAt(end) == '\n') {
        boolean isSingleLineAllWhiteSpaceUntilClose = false;
        int countWhiteSpaceCharacter = 1;
        for (; end + countWhiteSpaceCharacter < chars.length(); countWhiteSpaceCharacter++) {
          if (Character.isWhitespace(chars.charAt(end + countWhiteSpaceCharacter)) &&
            chars.charAt(end + countWhiteSpaceCharacter) != '\n') {
            continue;
          }
          if (chars.charAt(end + countWhiteSpaceCharacter) == close) {
            isSingleLineAllWhiteSpaceUntilClose = true;
          }
          break;
        }
        if (isSingleLineAllWhiteSpaceUntilClose) {
          end += countWhiteSpaceCharacter;
        }
      }
    }

    boolean rangeSelection = end - start > 1;
    if (rangeSelection && start == 0) // early return not only for optimization
    {
      return null;                    // but also not to break the interval semantic on this edge case (see below)
    }

    /* In case of successive inner selection. We want to break out of
     * the block delimiter of the current inner selection.
     * In other terms, for the rest of the algorithm, a previous inner selection of a block
     * if equivalent to an outer one. */
    if (!isOuter &&
      (start - 1) >= 0 &&
      type == chars.charAt(start - 1) &&
      end < chars.length() &&
      close == chars.charAt(end)) {
      start -= 1;
      pos = start;
      rangeSelection = true;
    }

    /* when one char is selected, we want to find the enclosing block of (start,end]
     * although when a range of characters is selected, we want the enclosing block of [start, end]
     * shifting the position allow to express which kind of interval we work on */
    if (rangeSelection) pos = Math.max(0, start - 1);

    boolean initialPosIsInString = checkInString(chars, pos, true);

    int bstart = -1;
    int bend = -1;

    boolean startPosInStringFound = false;

    if (initialPosIsInString) {
      TextRange quoteRange = injector.getSearchHelper()
        .findBlockQuoteInLineRange(new IjVimEditor(editor), new IjVimCaret(caret), '"', false);
      if (quoteRange != null) {
        int startOffset = quoteRange.getStartOffset();
        int endOffset = quoteRange.getEndOffset();
        CharSequence subSequence = chars.subSequence(startOffset, endOffset);
        int inQuotePos = pos - startOffset;
        int inQuoteStart = findBlockLocation(subSequence, close, type, Direction.BACKWARDS, inQuotePos, count, false);
        if (inQuoteStart == -1) {
          inQuoteStart = findBlockLocation(subSequence, close, type, Direction.FORWARDS, inQuotePos, count, false);
        }
        if (inQuoteStart != -1) {
          startPosInStringFound = true;
          int inQuoteEnd = findBlockLocation(subSequence, type, close, Direction.FORWARDS, inQuoteStart, 1, false);
          if (inQuoteEnd != -1) {
            bstart = inQuoteStart + startOffset;
            bend = inQuoteEnd + startOffset;
          }
        }
      }
    }

    if (!startPosInStringFound) {
      bstart = findBlockLocation(chars, close, type, Direction.BACKWARDS, pos, count, false);
      if (bstart == -1) {
        bstart = findBlockLocation(chars, close, type, Direction.FORWARDS, pos, count, false);
      }
      if (bstart != -1) {
        bend = findBlockLocation(chars, type, close, Direction.FORWARDS, bstart, 1, false);
      }
    }

    if (bstart == -1 || bend == -1) {
      return null;
    }

    if (!isOuter) {
      bstart++;
      // exclude first line break after start for inner match
      if (chars.charAt(bstart) == '\n') {
        bstart++;
      }

      int o = EngineEditorHelperKt.getLineStartForOffset(new IjVimEditor(editor), bend);
      boolean allWhite = true;
      for (int i = o; i < bend; i++) {
        if (!Character.isWhitespace(chars.charAt(i))) {
          allWhite = false;
          break;
        }
      }

      if (allWhite) {
        bend = o - 2;
      } else {
        bend--;
      }
    }

    // End offset exclusive
    return new TextRange(bstart, bend + 1);
  }

  private static int findMatchingBlockCommentPair(@NotNull PsiComment comment,
                                                  int pos,
                                                  @Nullable String prefix,
                                                  @Nullable String suffix) {
    if (prefix != null && suffix != null) {
      // TODO: Try to get rid of `getText()` because it takes a lot of time to calculate the string
      final String commentText = comment.getText();
      if (commentText.startsWith(prefix) && commentText.endsWith(suffix)) {
        final int endOffset = comment.getTextOffset() + comment.getTextLength();
        if (pos < comment.getTextOffset() + prefix.length()) {
          return endOffset;
        } else if (pos >= endOffset - suffix.length()) {
          return comment.getTextOffset();
        }
      }
    }
    return -1;
  }

  private static int findMatchingBlockCommentPair(@NotNull PsiElement element, int pos) {
    final Language language = element.getLanguage();
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(language);
    final PsiComment comment = PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
    if (comment != null) {
      final int ret = findMatchingBlockCommentPair(comment, pos, commenter.getBlockCommentPrefix(),
        commenter.getBlockCommentSuffix());
      if (ret >= 0) {
        return ret;
      }
      if (commenter instanceof CodeDocumentationAwareCommenter docCommenter) {
        return findMatchingBlockCommentPair(comment, pos, docCommenter.getDocumentationCommentPrefix(),
          docCommenter.getDocumentationCommentSuffix());
      }
    }
    return -1;
  }

  /**
   * This looks on the current line, starting at the cursor position for one of {, }, (, ), [, or ]. It then searches
   * forward or backward, as appropriate for the associated match pair. String in double quotes are skipped over.
   * Single characters in single quotes are skipped too.
   *
   * @param editor The editor to search in
   * @return The offset within the editor of the found character or -1 if no match was found or none of the characters
   * were found on the remainder of the current line.
   */
  public static int findMatchingPairOnCurrentLine(@NotNull Editor editor, @NotNull Caret caret) {
    int pos = caret.getOffset();

    final int commentPos = findMatchingComment(editor, pos);
    if (commentPos >= 0) {
      return commentPos;
    }

    int line = caret.getLogicalPosition().line;
    final IjVimEditor vimEditor = new IjVimEditor(editor);
    int end = EngineEditorHelperKt.getLineEndOffset(vimEditor, line, true);

    // To handle the case where visual mode allows the user to go past the end of the line,
    // which will prevent loc from finding a pairable character below
    if (pos > 0 && pos == end) {
      pos = end - 1;
    }

    final String pairChars = parseMatchPairsOption(vimEditor);

    CharSequence chars = editor.getDocument().getCharsSequence();
    int loc = -1;
    // Search the remainder of the current line for one of the candidate characters
    while (pos < end) {
      loc = pairChars.indexOf(chars.charAt(pos));
      if (loc >= 0) {
        break;
      }

      pos++;
    }

    int res = -1;
    // If we found one ...
    if (loc >= 0) {
      // What direction should we go now (-1 is backward, 1 is forward)
      Direction dir = loc % 2 == 0 ? Direction.FORWARDS : Direction.BACKWARDS;
      // Which character did we find and which should we now search for
      char found = pairChars.charAt(loc);
      char match = pairChars.charAt(loc + dir.toInt());
      res = findBlockLocation(chars, found, match, dir, pos, 1, true);
    }

    return res;
  }

  /**
   * If on the start/end of a block comment, jump to the matching of that comment, or vice versa.
   */
  private static int findMatchingComment(@NotNull Editor editor, int pos) {
    final PsiFile psiFile = PsiHelper.getFile(editor);
    if (psiFile != null) {
      final PsiElement element = psiFile.findElementAt(pos);
      if (element != null) {
        return findMatchingBlockCommentPair(element, pos);
      }
    }
    return -1;
  }

  private static int findBlockLocation(@NotNull CharSequence chars,
                                       char found,
                                       char match,
                                       @NotNull Direction dir,
                                       int pos,
                                       int cnt,
                                       boolean allowInString) {
    int res = -1;
    int initialPos = pos;
    boolean initialInString = checkInString(chars, pos, true);
    Function<Integer, Integer> inCheckPosF = x -> dir == Direction.BACKWARDS && x > 0 ? x - 1 : x + 1;
    final int inCheckPos = inCheckPosF.apply(pos);
    boolean inString = checkInString(chars, inCheckPos, true);
    boolean inChar = checkInString(chars, inCheckPos, false);
    int stack = 0;
    // Search to start or end of file, as appropriate
    Set<Character> charsToSearch = new HashSet<>(Arrays.asList('\'', '"', '\n', match, found));
    while (pos >= 0 && pos < chars.length() && cnt > 0) {
      @Nullable Pair<Character, Integer> ci = findPositionOfFirstCharacter(chars, pos, charsToSearch, true, dir);
      if (ci == null) {
        return -1;
      }
      Character c = ci.getFirst();
      pos = ci.getSecond();
      // If we found a match and we're not in a string...
      if (c == match && (allowInString ? initialInString == inString : !inString) && !inChar) {
        // We found our match
        if (stack == 0) {
          res = pos;
          cnt--;
        }
        // Found the character but it "closes" a different pair
        else {
          stack--;
        }
      }
      // End of line - mark not in a string any more (in case we started in the middle of one
      else if (c == '\n') {
        inString = false;
        inChar = false;
      } else if (pos != initialPos) {
        // We found another character like our original - belongs to another pair
        if (!inString && !inChar && c == found) {
          stack++;
        }
        // We found the start/end of a string
        else if (!inChar) {
          inString = checkInString(chars, inCheckPosF.apply(pos), true);
        } else if (!inString) {
          inChar = checkInString(chars, inCheckPosF.apply(pos), false);
        }
      }
      pos += dir.toInt();
    }

    return res;
  }

  /**
   * Returns true if [quote] is at this [pos] and it's not escaped (like \")
   */
  private static boolean isQuoteWithoutEscape(@NotNull CharSequence chars, int pos, char quote) {
    if (chars.charAt(pos) != quote) return false;

    int backslashCounter = 0;
    while (pos-- > 0 && chars.charAt(pos) == '\\') {
      backslashCounter++;
    }
    return backslashCounter % 2 == 0;
  }

  public static @Nullable
  Pair<Character, Integer> findPositionOfFirstCharacter(@NotNull CharSequence chars,
                                                        int pos,
                                                        final Set<Character> needles,
                                                        boolean searchEscaped,
                                                        @NotNull Direction direction) {
    int dir = direction.toInt();
    while (pos >= 0 && pos < chars.length()) {
      final char c = chars.charAt(pos);
      if (needles.contains(c) && (pos == 0 || searchEscaped || isQuoteWithoutEscape(chars, pos, c))) {
        return new Pair<>(c, pos);
      }
      pos += dir;
    }
    return null;
  }

  /**
   * returns new position which ignore whitespaces at beginning of the line
   */
  private static int ignoreWhitespaceAtLineStart(CharSequence seq, int lineStart, int pos) {
    if (seq.subSequence(lineStart, pos).chars().allMatch(Character::isWhitespace)) {
      while (pos < seq.length() && seq.charAt(pos) != '\n' && Character.isWhitespace(seq.charAt(pos))) {
        pos++;
      }
    }
    return pos;
  }


  public static @Nullable
  TextRange findBlockTagRange(@NotNull Editor editor,
                              @NotNull Caret caret,
                              int count,
                              boolean isOuter) {
    final int position = caret.getOffset();
    final CharSequence sequence = editor.getDocument().getCharsSequence();

    final int selectionStart = caret.getSelectionStart();
    final int selectionEnd = caret.getSelectionEnd();

    final boolean isRangeSelection = selectionEnd - selectionStart > 1;

    int searchStartPosition;
    if (!isRangeSelection) {
      final int line = caret.getLogicalPosition().line;
      final int lineBegin = editor.getDocument().getLineStartOffset(line);
      searchStartPosition = ignoreWhitespaceAtLineStart(sequence, lineBegin, position);
    } else {
      searchStartPosition = selectionEnd;
    }

    if (isInHTMLTag(sequence, searchStartPosition, false)) {
      // caret is inside opening tag. Move to closing '>'.
      while (searchStartPosition < sequence.length() && sequence.charAt(searchStartPosition) != '>') {
        searchStartPosition++;
      }
    } else if (isInHTMLTag(sequence, searchStartPosition, true)) {
      // caret is inside closing tag. Move to starting '<'.
      while (searchStartPosition > 0 && sequence.charAt(searchStartPosition) != '<') {
        searchStartPosition--;
      }
    }

    while (true) {
      final @Nullable Pair<TextRange, String> closingTag =
        findUnmatchedClosingTag(sequence, searchStartPosition, count);
      if (closingTag == null) {
        return null;
      }
      final TextRange closingTagTextRange = closingTag.getFirst();
      final String tagName = closingTag.getSecond();

      TextRange openingTag = findUnmatchedOpeningTag(sequence, closingTagTextRange.getStartOffset(), tagName);
      if (openingTag == null) {
        return null;
      }

      if (isRangeSelection && openingTag.getEndOffset() - 1 >= selectionStart) {
        // If there was already some text selected and the new selection would not extend further, we try again
        searchStartPosition = closingTagTextRange.getEndOffset();
        count = 1;
        continue;
      }

      int selectionEndWithoutNewline = selectionEnd;
      while (selectionEndWithoutNewline < sequence.length() && sequence.charAt(selectionEndWithoutNewline) == '\n') {
        selectionEndWithoutNewline++;
      }

      final Mode mode = VimStateMachine.Companion.getInstance(new IjVimEditor(editor)).getMode();
      if (mode instanceof Mode.VISUAL) {
        if (closingTagTextRange.getStartOffset() == selectionEndWithoutNewline &&
          openingTag.getEndOffset() == selectionStart) {
          // Special case: if the inner tag is already selected we should like isOuter is active
          // Note that we need to ignore newlines, because their selection is lost between multiple "it" invocations
          isOuter = true;
        } else if (openingTag.getEndOffset() == closingTagTextRange.getStartOffset() &&
          selectionStart == openingTag.getEndOffset()) {
          // Special case: for an empty tag pair (e.g. <a></a>) the whole tag is selected if the caret is in the middle.
          isOuter = true;
        }
      }

      if (isOuter) {
        return new TextRange(openingTag.getStartOffset(), closingTagTextRange.getEndOffset());
      } else {
        return new TextRange(openingTag.getEndOffset(), closingTagTextRange.getStartOffset());
      }
    }
  }

  /**
   * Returns true if there is a html at the given position. Ignores tags with a trailing slash like <aaa/>.
   */
  private static boolean isInHTMLTag(final @NotNull CharSequence sequence, final int position, final boolean isEndtag) {
    int openingBracket = -1;
    for (int i = position; i >= 0 && i < sequence.length(); i--) {
      if (sequence.charAt(i) == '<') {
        openingBracket = i;
        break;
      }
      if (sequence.charAt(i) == '>' && i != position) {
        return false;
      }
    }

    if (openingBracket == -1) {
      return false;
    }

    boolean hasSlashAfterOpening = openingBracket + 1 < sequence.length() && sequence.charAt(openingBracket + 1) == '/';
    if ((isEndtag && !hasSlashAfterOpening) || (!isEndtag && hasSlashAfterOpening)) {
      return false;
    }

    int closingBracket = -1;
    for (int i = openingBracket; i < sequence.length(); i++) {
      if (sequence.charAt(i) == '>') {
        closingBracket = i;
        break;
      }
    }

    return closingBracket != -1 && sequence.charAt(closingBracket - 1) != '/';
  }

  private static @Nullable
  Pair<TextRange, String> findUnmatchedClosingTag(final @NotNull CharSequence sequence,
                                                  final int position,
                                                  int count) {
    // The tag name may contain any characters except slashes, whitespace and '>'
    final String tagNamePattern = "([^/\\s>]+)";
    // An opening tag consists of '<' followed by a tag name, optionally some additional text after whitespace and a '>'
    final String openingTagPattern = String.format("<%s(?:\\s[^>]*)?>", tagNamePattern);
    final String closingTagPattern = String.format("</%s>", tagNamePattern);
    final Pattern tagPattern = Pattern.compile(String.format("(?:%s)|(?:%s)", openingTagPattern, closingTagPattern));
    final Matcher matcher = tagPattern.matcher(sequence.subSequence(position, sequence.length()));

    final Deque<String> openTags = new ArrayDeque<>();

    while (matcher.find()) {
      boolean isClosingTag = matcher.group(1) == null;
      if (isClosingTag) {
        final String tagName = matcher.group(2);
        // Ignore unmatched open tags. Either the file is malformed or it might be a tag like <br> that does not need to be closed.
        while (!openTags.isEmpty() && !openTags.peek().equalsIgnoreCase(tagName)) {
          openTags.pop();
        }
        if (openTags.isEmpty()) {
          if (count <= 1) {
            return new Pair<>(new TextRange(position + matcher.start(), position + matcher.end()), tagName);
          } else {
            count--;
          }
        } else {
          openTags.pop();
        }
      } else {
        final String tagName = matcher.group(1);
        openTags.push(tagName);
      }
    }
    return null;
  }

  private static @Nullable
  TextRange findUnmatchedOpeningTag(@NotNull CharSequence sequence,
                                    int position,
                                    @NotNull String tagName) {
    final String quotedTagName = Pattern.quote(tagName);
    final String patternString = "(</%s>)"
      // match closing tags
      +
      "|(<%s"
      // or opening tags starting with tagName
      +
      "(\\s([^>]*"
      // After at least one whitespace there might be additional text in the tag. E.g. <html lang="en">
      +
      "[^/])?)?>)";  // Slash is not allowed as last character (this would be a self closing tag).
    final Pattern tagPattern =
      Pattern.compile(String.format(patternString, quotedTagName, quotedTagName), Pattern.CASE_INSENSITIVE);
    final Matcher matcher = tagPattern.matcher(sequence.subSequence(0, position + 1));
    final Deque<TextRange> openTags = new ArrayDeque<>();

    while (matcher.find()) {
      final TextRange match = new TextRange(matcher.start(), matcher.end());
      if (sequence.charAt(matcher.start() + 1) == '/') {
        if (!openTags.isEmpty()) {
          openTags.pop();
        }
      } else {
        openTags.push(match);
      }
    }

    if (openTags.isEmpty()) {
      return null;
    } else {
      return openTags.pop();
    }
  }

  /**
   * This counts all the words in the file.
   */
  public static @NotNull
  CountPosition countWords(@NotNull Editor editor) {
    int size = EditorHelperRt.getFileSize(editor);

    return countWords(editor, 0, size);
  }

  /**
   * This counts all the words in the file.
   */
  public static @NotNull
  CountPosition countWords(@NotNull Editor editor, int start, int end) {
    int offset = editor.getCaretModel().getOffset();
    final IjVimEditor vimEditor = new IjVimEditor(editor);

    int count = 1;
    int position = 0;
    int last = -1;
    int res = start;
    while (true) {
      res = injector.getSearchHelper().findNextWord(vimEditor, res, 1, true, false);
      if (res == start || res == 0 || res > end || res == last) {
        break;
      }

      count++;

      if (res == offset) {
        position = count;
      } else if (last < offset && res >= offset) {
        if (count == 2) {
          position = 1;
        } else {
          position = count - 1;
        }
      }

      last = res;
    }

    if (position == 0 && res == offset) {
      position = count;
    }

    return new CountPosition(count, position);
  }

  public static @NotNull
  List<Pair<TextRange, NumberType>> findNumbersInRange(final @NotNull Editor editor,
                                                       @NotNull TextRange textRange,
                                                       final boolean alpha,
                                                       final boolean hex,
                                                       final boolean octal) {
    List<Pair<TextRange, NumberType>> result = new ArrayList<>();


    for (int i = 0; i < textRange.size(); i++) {
      int startOffset = textRange.getStartOffsets()[i];
      final int end = textRange.getEndOffsets()[i];
      String text = EngineEditorHelperKt.getText(new IjVimEditor(editor), startOffset, end);
      String[] textChunks = text.split("\\n");
      int chunkStart = 0;
      for (String chunk : textChunks) {
        Pair<TextRange, NumberType> number = findNumberInText(chunk, 0, alpha, hex, octal);

        if (number != null) {
          result.add(new Pair<>(new TextRange(number.getFirst().getStartOffset() + startOffset + chunkStart,
            number.getFirst().getEndOffset() + startOffset + chunkStart),
            number.getSecond()));
        }
        chunkStart += 1 + chunk.length();
      }
    }

    /*


    int firstLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
    int lastLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line;

    int startOffset = textRange.getStartOffset();
    for (int lineNumber = firstLine; lineNumber <= lastLine; lineNumber++) {
      int endOffset = EditorHelper.getLineEndOffset(editor, lineNumber, true);
      if (endOffset > textRange.getEndOffset()) endOffset = textRange.getEndOffset();

      String text = EditorHelper.getText(editor, startOffset, endOffset);

      TextRange numberRange = findNumberInText(text, 0, alpha, hex, octal);

      if (numberRange == null) continue;

      result.add(new TextRange(numberRange.getStartOffset() + startOffset, numberRange.getEndOffset() + startOffset));

      startOffset = endOffset + 1;
    }

    */
    return result;
  }

  public static @Nullable
  Pair<TextRange, NumberType> findNumberUnderCursor(final @NotNull Editor editor,
                                                    @NotNull Caret caret,
                                                    final boolean alpha,
                                                    final boolean hex,
                                                    final boolean octal) {
    int lline = caret.getLogicalPosition().line;
    String text = new IjVimEditor(editor).getLineText(lline).toLowerCase();
    int startLineOffset = new IjVimEditor(editor).getLineStartOffset(lline);
    int posOnLine = caret.getOffset() - startLineOffset;

    Pair<TextRange, NumberType> numberTextRange = findNumberInText(text, posOnLine, alpha, hex, octal);

    if (numberTextRange == null) {
      return null;
    }
    return new Pair<>(new TextRange(numberTextRange.getFirst().getStartOffset() + startLineOffset,
      numberTextRange.getFirst().getEndOffset() + startLineOffset),
      numberTextRange.getSecond());
  }

  /**
   * Search for number in given text from start position
   *
   * @param textInRange    - text to search in
   * @param startPosOnLine - start offset to search
   * @return - text range with number
   */
  public static @Nullable
  Pair<TextRange, NumberType> findNumberInText(final @NotNull String textInRange,
                                               int startPosOnLine,
                                               final boolean alpha,
                                               final boolean hex,
                                               final boolean octal) {

    if (logger.isDebugEnabled()) {
      logger.debug("text=" + textInRange);
    }

    int pos = startPosOnLine;
    int lineEndOffset = textInRange.length();

    while (true) {
      // Skip over current whitespace if any
      while (pos < lineEndOffset && !isNumberChar(textInRange.charAt(pos), alpha, hex, octal, true)) {
        pos++;
      }

      if (logger.isDebugEnabled()) logger.debug("pos=" + pos);
      if (pos >= lineEndOffset) {
        logger.debug("no number char on line");
        return null;
      }

      boolean isHexChar = "abcdefABCDEF".indexOf(textInRange.charAt(pos)) >= 0;

      if (hex) {
        // Ox and OX handling
        if (textInRange.charAt(pos) == '0' &&
          pos < lineEndOffset - 1 &&
          "xX".indexOf(textInRange.charAt(pos + 1)) >= 0) {
          pos += 2;
        } else if ("xX".indexOf(textInRange.charAt(pos)) >= 0 && pos > 0 && textInRange.charAt(pos - 1) == '0') {
          pos++;
        }

        logger.debug("checking hex");
        final Pair<Integer, Integer> range = findRange(textInRange, pos, false, true, false, false);
        int start = range.getFirst();
        int end = range.getSecond();

        // Ox and OX
        if (start >= 2 && textInRange.substring(start - 2, start).equalsIgnoreCase("0x")) {
          logger.debug("found hex");
          return new Pair<>(new TextRange(start - 2, end), NumberType.HEX);
        }

        if (!isHexChar || alpha) {
          break;
        } else {
          pos++;
        }
      } else {
        break;
      }
    }

    if (octal) {
      logger.debug("checking octal");
      final Pair<Integer, Integer> range = findRange(textInRange, pos, false, false, true, false);
      int start = range.getFirst();
      int end = range.getSecond();

      if (end - start == 1 && textInRange.charAt(start) == '0') {
        return new Pair<>(new TextRange(start, end), NumberType.DEC);
      }
      if (textInRange.charAt(start) == '0' &&
        end > start &&
        !(start > 0 && isNumberChar(textInRange.charAt(start - 1), false, false, false, true))) {
        logger.debug("found octal");
        return new Pair<>(new TextRange(start, end), NumberType.OCT);
      }
    }

    if (alpha) {
      if (logger.isDebugEnabled()) logger.debug("checking alpha for " + textInRange.charAt(pos));
      if (isNumberChar(textInRange.charAt(pos), true, false, false, false)) {
        if (logger.isDebugEnabled()) logger.debug("found alpha at " + pos);
        return new Pair<>(new TextRange(pos, pos + 1), NumberType.ALPHA);
      }
    }

    final Pair<Integer, Integer> range = findRange(textInRange, pos, false, false, false, true);
    int start = range.getFirst();
    int end = range.getSecond();
    if (start > 0 && textInRange.charAt(start - 1) == '-') {
      start--;
    }

    return new Pair<>(new TextRange(start, end), NumberType.DEC);
  }

  /**
   * Searches for digits block that matches parameters
   */
  private static @NotNull
  Pair<Integer, Integer> findRange(final @NotNull String text,
                                   final int pos,
                                   final boolean alpha,
                                   final boolean hex,
                                   final boolean octal,
                                   final boolean decimal) {
    int end = pos;
    while (end < text.length() && isNumberChar(text.charAt(end), alpha, hex, octal, decimal || octal)) {
      end++;
    }
    int start = pos;
    while (start >= 0 && isNumberChar(text.charAt(start), alpha, hex, octal, decimal || octal)) {
      start--;
    }
    if (start < end &&
      (start == -1 ||
        0 <= start &&
          start < text.length() &&
          !isNumberChar(text.charAt(start), alpha, hex, octal, decimal || octal))) {
      start++;
    }
    if (octal) {
      for (int i = start; i < end; i++) {
        if (!isNumberChar(text.charAt(i), false, false, true, false)) return new Pair<>(0, 0);
      }
    }
    return new Pair<>(start, end);
  }

  private static boolean isNumberChar(char ch, boolean alpha, boolean hex, boolean octal, boolean decimal) {
    if (alpha && ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
      return true;
    } else if (octal && (ch >= '0' && ch <= '7')) {
      return true;
    } else if (hex && ((ch >= '0' && ch <= '9') || "abcdefABCDEF".indexOf(ch) >= 0)) {
      return true;
    } else {
      return decimal && (ch >= '0' && ch <= '9');
    }

  }

  /**
   * Find the word under the cursor or the next word to the right of the cursor on the current line.
   *
   * @param editor The editor to find the word in
   * @param caret  The caret to find word under
   * @return The text range of the found word or null if there is no word under/after the cursor on the line
   */
  public static @Nullable
  TextRange findWordUnderCursor(@NotNull Editor editor, @NotNull Caret caret) {
    final IjVimEditor vimEditor = new IjVimEditor(editor);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int stop = EngineEditorHelperKt.getLineEndOffset(vimEditor, caret.getLogicalPosition().line, true);

    int pos = caret.getOffset();
    // Technically the first condition is covered by the second one, but let it be
    if (chars.length() == 0 || chars.length() <= pos) return null;
    //if (pos == chars.length() - 1) return new TextRange(chars.length() - 1, chars.length());

    int start = pos;
    CharacterHelper.CharacterType[] types = new CharacterHelper.CharacterType[]{CharacterHelper.CharacterType.KEYWORD,
      CharacterHelper.CharacterType.PUNCTUATION};
    for (int i = 0; i < 2; i++) {
      start = pos;
      CharacterHelper.CharacterType type = CharacterHelper.charType(vimEditor, chars.charAt(start), false);
      if (type == types[i]) {
        // Search back for start of word
        while (start > 0 && CharacterHelper.charType(vimEditor, chars.charAt(start - 1), false) == types[i]) {
          start--;
        }
      } else {
        // Search forward for start of word
        while (start < stop && CharacterHelper.charType(vimEditor, chars.charAt(start), false) != types[i]) {
          start++;
        }
      }

      if (start != stop) {
        break;
      }
    }

    if (start == stop) {
      return null;
    }

    int end;
    // Special case 1 character words because 'findNextWordEnd' returns one to many chars
    if (start < stop &&
      (start >= chars.length() - 1 ||
        CharacterHelper.charType(vimEditor, chars.charAt(start + 1), false) != CharacterHelper.CharacterType.KEYWORD)) {
      end = start + 1;
    } else {
      end = injector.getSearchHelper().findNextWordEnd(vimEditor, start, 1, false, false) + 1;
    }

    return new TextRange(start, end);
  }

  @Contract("_, _, _, _, _, _, _ -> new")
  public static @NotNull
  TextRange findWordUnderCursor(@NotNull Editor editor,
                                @NotNull Caret caret,
                                int count,
                                int dir,
                                boolean isOuter,
                                boolean isBig,
                                boolean hasSelection) {
    if (logger.isDebugEnabled()) {
      logger.debug("count=" + count);
      logger.debug("dir=" + dir);
      logger.debug("isOuter=" + isOuter);
      logger.debug("isBig=" + isBig);
      logger.debug("hasSelection=" + hasSelection);
    }

    CharSequence chars = editor.getDocument().getCharsSequence();
    //int min = EditorHelper.getLineStartOffset(editor, EditorHelper.getCurrentLogicalLine(editor));
    //int max = EditorHelper.getLineEndOffset(editor, EditorHelper.getCurrentLogicalLine(editor), true);
    int min = 0;
    int max = EditorHelperRt.getFileSize(editor);
    if (max == 0) return new TextRange(0, 0);

    if (logger.isDebugEnabled()) {
      logger.debug("min=" + min);
      logger.debug("max=" + max);
    }

    int pos = caret.getOffset();
    if (chars.length() <= pos) return new TextRange(chars.length() - 1, chars.length() - 1);

    final IjVimEditor vimEditor = new IjVimEditor(editor);
    boolean startSpace = CharacterHelper.charType(vimEditor, chars.charAt(pos), isBig) == CharacterHelper.CharacterType.WHITESPACE;
    // Find word start
    boolean onWordStart = pos == min ||
      CharacterHelper.charType(vimEditor, chars.charAt(pos - 1), isBig) !=
        CharacterHelper.charType(vimEditor, chars.charAt(pos), isBig);
    int start = pos;

    if (logger.isDebugEnabled()) {
      logger.debug("pos=" + pos);
      logger.debug("onWordStart=" + onWordStart);
    }

    if ((!onWordStart && !(startSpace && isOuter)) || hasSelection || (count > 1 && dir == -1)) {
      if (dir == 1) {
        start = injector.getSearchHelper().findNextWord(vimEditor, pos, -1, isBig, !isOuter);
      } else {
        start = injector.getSearchHelper().findNextWord(vimEditor, pos, -(count - (onWordStart && !hasSelection ? 1 : 0)), isBig, !isOuter);
      }

      start = EngineEditorHelperKt.normalizeOffset(vimEditor, start, false);
    }

    if (logger.isDebugEnabled()) logger.debug("start=" + start);

    // Find word end
    boolean onWordEnd = pos >= max - 1 ||
      CharacterHelper.charType(vimEditor, chars.charAt(pos + 1), isBig) !=
        CharacterHelper.charType(vimEditor, chars.charAt(pos), isBig);

    if (logger.isDebugEnabled()) logger.debug("onWordEnd=" + onWordEnd);

    int end = pos;
    if (!onWordEnd || hasSelection || (count > 1 && dir == 1) || (startSpace && isOuter)) {
      if (dir == 1) {
        int c = count - (onWordEnd && !hasSelection && (!(startSpace && isOuter) || (startSpace && !isOuter)) ? 1 : 0);
        end = injector.getSearchHelper().findNextWordEnd(vimEditor, pos, c, isBig, !isOuter);
      } else {
        end = injector.getSearchHelper().findNextWordEnd(vimEditor, pos, 1, isBig, !isOuter);
      }
    }

    if (logger.isDebugEnabled()) logger.debug("end=" + end);

    boolean goBack = (startSpace && !hasSelection) || (!startSpace && hasSelection && !onWordStart);
    if (dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = injector.getSearchHelper().findNextWordEnd(vimEditor, pos, 1, isBig, false);
      }
      if (firstEnd < max - 1) {
        if (CharacterHelper.charType(vimEditor, chars.charAt(firstEnd + 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true;
        }
      }
    }
    if (dir == -1 && isOuter && startSpace) {
      if (pos > min) {
        if (CharacterHelper.charType(vimEditor, chars.charAt(pos - 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goBack = true;
        }
      }
    }

    boolean goForward =
      (dir == 1 && isOuter && ((!startSpace && !onWordEnd) || (startSpace && onWordEnd && hasSelection)));
    if (!goForward && dir == 1 && isOuter) {
      int firstEnd = end;
      if (count > 1) {
        firstEnd = injector.getSearchHelper().findNextWordEnd(vimEditor, pos, 1, isBig, false);
      }
      if (firstEnd < max - 1) {
        if (CharacterHelper.charType(vimEditor, chars.charAt(firstEnd + 1), false) != CharacterHelper.CharacterType.WHITESPACE) {
          goForward = true;
        }
      }
    }
    if (!goForward && dir == 1 && isOuter && !startSpace && !hasSelection) {
      if (end < max - 1) {
        if (CharacterHelper.charType(vimEditor, chars.charAt(end + 1), !isBig) !=
          CharacterHelper.charType(vimEditor, chars.charAt(end), !isBig)) {
          goForward = true;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("goBack=" + goBack);
      logger.debug("goForward=" + goForward);
    }

    if (goForward) {
      if (EngineEditorHelperKt.anyNonWhitespace(vimEditor, end, 1)) {
        while (end + 1 < max &&
          CharacterHelper.charType(vimEditor, chars.charAt(end + 1), false) == CharacterHelper.CharacterType.WHITESPACE) {
          end++;
        }
      }
    }
    if (goBack) {
      if (EngineEditorHelperKt.anyNonWhitespace(vimEditor, start, -1)) {
        while (start > min &&
          CharacterHelper.charType(vimEditor, chars.charAt(start - 1), false) == CharacterHelper.CharacterType.WHITESPACE) {
          start--;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("start=" + start);
      logger.debug("end=" + end);
    }

    // End offset is exclusive
    return new TextRange(start, end + 1);
  }

  /**
   * This locates the position with the document of the count-th occurrence of ch on the current line
   *
   * @param editor The editor to search in
   * @param caret  The caret to be searched starting from
   * @param count  The number of occurrences of ch to locate. Negative for backward searches
   * @param ch     The character on the line to find
   * @return The document offset of the matching character match, -1
   */
  public static int findNextCharacterOnLine(@NotNull Editor editor, @NotNull Caret caret, int count, char ch) {
    int line = caret.getLogicalPosition().line;
    int start = new IjVimEditor(editor).getLineStartOffset(line);
    int end = EngineEditorHelperKt.getLineEndOffset(new IjVimEditor(editor), line, true);
    CharSequence chars = editor.getDocument().getCharsSequence();
    int found = 0;
    int step = count >= 0 ? 1 : -1;
    int pos = caret.getOffset() + step;
    while (pos >= start && pos < end && pos < chars.length()) {
      if (chars.charAt(pos) == ch) {
        found++;
        if (found == Math.abs(count)) {
          break;
        }
      }
      pos += step;
    }

    if (found == Math.abs(count)) {
      return pos;
    } else {
      return -1;
    }
  }

  public static int findMethodStart(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return PsiHelper.findMethodStart(editor, caret.getOffset(), count);
  }

  public static int findMethodEnd(@NotNull Editor editor, @NotNull Caret caret, int count) {
    return PsiHelper.findMethodEnd(editor, caret.getOffset(), count);
  }

  private static @NotNull
  String parseMatchPairsOption(final VimEditor vimEditor) {
    List<String> pairs = options(injector, vimEditor).getMatchpairs();
    StringBuilder res = new StringBuilder();
    for (String s : pairs) {
      if (s.length() == 3) {
        res.append(s.charAt(0)).append(s.charAt(2));
      }
    }

    return res.toString();
  }

  public static class CountPosition {
    public CountPosition(int count, int position) {
      this.count = count;
      this.position = position;
    }

    public int getCount() {
      return count;
    }

    public int getPosition() {
      return position;
    }

    private final int count;
    private final int position;
  }

  private static final @NotNull
  String blockChars = "{}()[]<>";

  private static final Logger logger = Logger.getInstance(SearchHelper.class.getName());
}
