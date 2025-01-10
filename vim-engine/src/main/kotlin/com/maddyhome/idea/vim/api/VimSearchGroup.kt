/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.command.MotionType
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.vimscript.model.VimLContext

interface VimSearchGroup {

  /**
   * Last used pattern to perform a search.
   */
  var lastSearchPattern: String?

  /**
   * Last used pattern to perform a substitution.
   */
  var lastSubstitutePattern: String?

  fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?

  /**
   * Find the range of the next occurrence of the last used search pattern
   *
   * <p>Used for the implementation of the gn and gN commands.</p>
   *
   * <p>Searches for the range of the next occurrence of the last used search pattern. If the current primary
   * caret is inside the range of an occurrence, will return that instance. Uses the last used search pattern. Does not
   * update any other state. Direction is explicit, not from state.</p>
   *
   * @param editor    The editor to search in
   * @param count     Find the nth occurrence
   * @param forwards  Search forwards or backwards
   * @return          The TextRange of the next occurrence or null if not found
   */
  fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?

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
  fun processSearchRange(
    editor: VimEditor,
    pattern: String,
    patternOffset: Int,
    startOffset: Int,
    direction: Direction,
  ): Int

  /**
   * Find the next occurrence of the last used pattern.
   *
   * <p>Searches for the last used pattern, including last used pattern trailing. Direction is the same as the last used direction.
   * E.g. `?foo` followed by `n` will search backwards. scanwrap and ignorecase come from options.</p>
   *
   * @param editor  The editor to search in
   * @param caret   Used to get the offset to start searching from
   * @param count   Find the nth occurrence
   * @return        The offset of the next match, or -1 if not found
   */
  fun searchNext(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

  /**
   * Find the previous occurrence of the last used pattern.
   *
   * <p>Searches for last used pattern, including last used pattern trailing. Direction is the opposite of the last used direction.
   * E.g. `?foo` followed by `N` will be forwards. scanwrap and ignorecase come from options.</p>
   *
   * @param editor  The editor to search in
   * @param caret   Used to get the offset to starting searching from
   * @param count   Find the nth occurrence
   * @return        The offset of the next match, or -1 if not found
   */
  fun searchPrevious(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

  /**
   * Process the search command, searching for the pattern from the given document offset
   *
   * Parses the pattern from the search command and will search for the given pattern, immediately saving the last used
   * search pattern. Updates the search register and history and search highlights. Also updates last pattern offset and
   * direction. The `'scanwrap'` and `'ignorecase'` options are used by the implementation.
   *
   * Will parse the entire command, including patterns separated by `;`.
   *
   * If the search command is being used by an operator, and the pattern contains an offset (`/{pattern}/{offset}`), the
   * motion type for the operator becomes exclusive or linewise. See `:help search-offset`.
   *
   * Note that this method should only be called when the ex command argument should be parsed, and start should be
   * updated. I.e. only for the search commands. Consider using SearchHelper.findPattern to find text.
   *
   * Equivalent to normal.c:nv_search + search.c:do_search
   *
   * @param editor      The editor to search in
   * @param command     The command text entered into the Ex entry panel. Does not include the leading `/` or `?`.
   *                    Can include a trailing offset, e.g. /{pattern}/{offset}, or multiple commands separated by a semicolon.
   *                    If the pattern is empty, the last used (search? substitute?) pattern (and offset?) is used.
   * @param startOffset The offset to start searching from
   * @param count1      Find the nth occurrence, coerced to 1
   * @param dir         The direction to search
   * @return            Pair containing the offset to the next occurrence of the pattern, and the [MotionType] based on
   *                    the search offset. The value will be `null` if no result is found.
   */
  fun processSearchCommand(
    editor: VimEditor,
    command: String,
    startOffset: Int,
    count1: Int,
    dir: Direction,
  ): Pair<Int, MotionType>?

  /**
   * Search for the word under the given caret
   *
   * <p>Updates last search pattern, last pattern trailing and direction. Ignore smart case is set to true. Highlights
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
  fun searchWord(editor: VimEditor, caret: ImmutableVimCaret, count: Int, whole: Boolean, dir: Direction): Int

  /**
   * If [command] contains a pattern, this function finds the end of it that is marked with [delimiter].
   *
   * This is useful for commands like `:%s/123/321/s` to detect the end of `123` pattern. `/` will be a [delimiter].
   */
  fun findEndOfPattern(
    command: String,
    delimiter: Char,
    startIndex: Int = 0,
  ): Int

  /**
   * Parse and execute the substitute command
   *
   * <p>Updates state for the last substitute pattern and last replacement text. Updates search
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
   * @param range   Only search and substitute within the given line range. Must be valid
   * @param excmd   The command part of the ex command line, e.g. `s` or `substitute`, or `~`
   * @param exarg   The argument to the substitute command, such as `/{pattern}/{string}/[flags]`
   * @return        True if the substitution succeeds, false on error. Will succeed even if nothing is modified
   */
  fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean

  fun findDecimalNumber(line: String): Int?

  fun updateSearchHighlightsAfterGlobalCommand()

  /**
   * Clears all search highlights.
   */
  fun clearSearchHighlight()

  /**
   * Gets the direction lastly used in a search.
   */
  fun getLastSearchDirection(): Direction

  /**
   * Returns true if any text is selected in the visible editors, false otherwise.
   */
  fun isSomeTextHighlighted(): Boolean
}
