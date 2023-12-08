/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.LineRange
import com.maddyhome.idea.vim.regexp.CharPointer
import com.maddyhome.idea.vim.vimscript.model.VimLContext

public interface VimSearchGroup {

  /**
   * Last used pattern to perform a search.
   */
  public var lastSearchPattern: String?

  /**
   * Last used pattern to perform a substitution.
   */
  public var lastSubstitutePattern: String?

  public fun searchBackward(editor: VimEditor, offset: Int, count: Int): TextRange?

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
  public fun getNextSearchRange(editor: VimEditor, count: Int, forwards: Boolean): TextRange?

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
  public fun processSearchRange(
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
  public fun searchNext(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

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
  public fun searchPrevious(editor: VimEditor, caret: ImmutableVimCaret, count: Int): Int

  /**
   * Process the search command, searching for the pattern from the given document offset
   *
   * <p>Parses the pattern from the search command and will search for the given pattern, immediately saving the last used
   * search pattern. Updates the search register and history and search highlights. Also updates last pattern offset and
   * direction. scanwrap and ignorecase come from options.
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
   * @return            Offset to the next occurrence of the pattern or -1 if not found
   */
  public fun processSearchCommand(editor: VimEditor, command: String, startOffset: Int, dir: Direction): Int

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
  public fun searchWord(editor: VimEditor, caret: ImmutableVimCaret, count: Int, whole: Boolean, dir: Direction): Int

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
  public fun processSubstituteCommand(
    editor: VimEditor,
    caret: VimCaret,
    range: LineRange,
    excmd: String,
    exarg: String,
    parent: VimLContext,
  ): Boolean

  @Deprecated("Please use the new regexp engine")
  public fun search_regcomp(pat: CharPointer?, which_pat: Int, patSave: Int): Pair<Boolean, Triple<Any, String, Any>>
  public fun findDecimalNumber(line: String): Int?

  /**
   * Clears all search highlights.
   */
  public fun clearSearchHighlight()

  /**
   * Gets the direction lastly used in a search.
   */
  public fun getLastSearchDirection(): Direction

  @Deprecated("Please use the new regexp engine")
  // Matching the values defined in Vim. Do not change these values, they are used as indexes
  public companion object {
    public val RE_SEARCH: Int = 0 // Save/use search pattern

    public val RE_SUBST: Int = 1 // Save/use substitute pattern

    public val RE_BOTH: Int = 2 // Save to both patterns

    public val RE_LAST: Int = 2 // Use last used pattern if "pat" is NULL
  }
}
