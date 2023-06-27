/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.maddyhome.idea.vim.api.ExecutionContext
import java.awt.Point
import java.awt.Rectangle

public interface SplitService {

  public val adjustmentAmount: Float
  public enum class AdjustmentType
  {
    StretchVertically,
    ShrinkVertically,
    StretchHorizontally,
    ShrinkHorizontally,
  }
  public enum class MaximizeType
  {
    MaximizeVertically,
    MaximizeHorizontally,
  }
  public fun adjustOwner(coord: Point, adjustmentType: AdjustmentType, context: ExecutionContext)
  public fun maximizeOwner(coord: Point, maximizeType: MaximizeType, context: ExecutionContext)
  public fun equalizeSplits(context: ExecutionContext)
}

public interface SplitNode {

  public enum class Orientation { Vertical, Horizontal }
  public enum class WalkingDirection { Left, Right, Back }
  public val parent: SplitNode?
  public fun isVertical() : Boolean
  public fun getOwningPanel(coord: Point) : SplitPanel?
  public fun adjust(adjustmentAmount: Float)
  public fun setProportion(proportion: Float)
}

public interface SplitPanel {

  public enum class Position { Left, Right, Top, Bottom }
  public val position: Position
  public fun getScreenRect() : Rectangle
}