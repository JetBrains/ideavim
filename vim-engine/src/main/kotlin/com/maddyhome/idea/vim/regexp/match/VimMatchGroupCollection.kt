/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.regexp.match

public class VimMatchGroupCollection(
  /**
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
   * Store the highest seen group number plus one, which
   * should correspond to the number of tracked groups
   */
  internal var groupCount: Int = 0

  public fun get(index: Int): VimMatchGroup? {
    return if (index < groupCount && index < groups.size && index >= 0) groups[index]
    else null
  }

  internal fun setGroupStart(groupNumber: Int, startIndex: Int) {
    groupStarts[groupNumber] = startIndex
  }

  internal fun setGroupEnd(groupNumber: Int, endIndex: Int, text: CharSequence) {
    val range = groupStarts[groupNumber] until endIndex
    groups[groupNumber] = VimMatchGroup(range, text.substring(range))
    groupCount = maxOf(groupCount, groupNumber + 1)
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
    TODO()
  }

}