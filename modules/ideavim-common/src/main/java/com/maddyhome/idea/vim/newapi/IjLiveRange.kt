/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.RangeMarker
import com.maddyhome.idea.vim.common.LiveRange

internal class IjLiveRange(val marker: RangeMarker) : LiveRange {
  override val startOffset: Int
    get() = marker.startOffset

  override val endOffset: Int
    get() = marker.endOffset

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as IjLiveRange

    if (startOffset != other.startOffset) return false
    if (endOffset != other.endOffset) return false

    return true
  }

  override fun hashCode(): Int {
    var result = startOffset
    result = 31 * result + endOffset
    return result
  }
}

val RangeMarker.vim: LiveRange
  get() = IjLiveRange(this)

val LiveRange.ij: RangeMarker
  get() = (this as IjLiveRange).marker
