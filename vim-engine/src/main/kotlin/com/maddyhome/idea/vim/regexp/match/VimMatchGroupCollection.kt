/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.match

import com.maddyhome.idea.vim.common.TextRange

/**
 * A collection of match results of capture groups
 */
class VimMatchGroupCollection(
  /**
   * The maximum amount of capture groups.
   * There can only be a maximum of 10 capture groups.
   * Index 0 is for the entire match and the other 9 for explicit
   * capture groups.
   */
  override val size: Int = 10,

  ) : Collection<VimMatchGroup> {
  /**
   * Store the capture groups
   */
  private val groups: MutableList<VimMatchGroup?> = MutableList(size) { null }

  /**
   * Store the start indexes of groups
   */
  private val groupStarts: IntArray = IntArray(size)

  /**
   * Used to check if a certain group capture has started and ended
   */
  private val completedGroups: BooleanArray = BooleanArray(size) { false }

  private val forceEnded: BooleanArray = BooleanArray(size) { false }

  /**
   * Store the highest seen group number plus one, which
   * should correspond to the number of tracked groups
   */
  private var groupCount: Int = 0

  /**
   * Gets a single capture group match
   *
   * @param index The number of the capture group to get
   *
   * @return The capture group with the desired number, or null if the number is too big
   */
  fun get(index: Int): VimMatchGroup? {
    return if (index < groupCount && index < groups.size && index >= 0) groups[index]
    else null
  }

  /**
   * Sets the start index of a certain capture group
   *
   * @param groupNumber The number of the capture group
   * @param startIndex  The index where the capture group match starts
   */
  internal fun setGroupStart(groupNumber: Int, startIndex: Int) {
    groupStarts[groupNumber] = startIndex
    if (groupNumber == 0) completedGroups[groupNumber] = false
  }

  /**
   * Sets the end index of a certain capture group if it wasn't previously set
   *
   * @param groupNumber The number of the capture group
   * @param endIndex    The index where the capture group match end
   * @param text        The text used to extract the matched string
   */
  internal fun setGroupEnd(groupNumber: Int, endIndex: Int, text: CharSequence) {
    if (completedGroups[groupNumber] && forceEnded[groupNumber]) return

    val range = TextRange(groupStarts[groupNumber], endIndex)
    groups[groupNumber] = VimMatchGroup(range, text.substring(range.startOffset, range.endOffset))
    groupCount = maxOf(groupCount, groupNumber + 1)
    completedGroups[groupNumber] = true
  }

  /**
   * Sets the end index of a certain capture group
   *
   * @param groupNumber The number of the capture group
   * @param endIndex    The index where the capture group match end
   * @param text        The text used to extract the matched string
   */
  internal fun setForceGroupEnd(groupNumber: Int, endIndex: Int, text: CharSequence) {
    val range = TextRange(groupStarts[groupNumber], endIndex)
    groups[groupNumber] = VimMatchGroup(range, text.substring(range.startOffset, range.endOffset))
    groupCount = maxOf(groupCount, groupNumber + 1)
    completedGroups[groupNumber] = true
    forceEnded[groupNumber] = true
  }

  internal fun clear() {
    groupCount = 0
    for (groupNumber in completedGroups.indices) completedGroups[groupNumber] = false
    for (groupNumber in forceEnded.indices) forceEnded[groupNumber] = false
  }

  override fun contains(element: VimMatchGroup): Boolean {
    return groups.subList(0, groupCount).contains(element)
  }

  override fun containsAll(elements: Collection<VimMatchGroup>): Boolean {
    return groups.subList(0, groupCount).containsAll(elements)
  }

  override fun isEmpty(): Boolean {
    return groups.subList(0, groupCount).isEmpty()
  }

  override fun iterator(): Iterator<VimMatchGroup> {
    return groups.subList(0, groupCount)
      .filterNotNull()
      .iterator()
  }


}