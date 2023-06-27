/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.group.SplitNode.*
import com.maddyhome.idea.vim.group.SplitNode.Orientation.*
import com.maddyhome.idea.vim.group.SplitService.*
import com.maddyhome.idea.vim.group.SplitService.AdjustmentType.*
import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.util.*
import javax.swing.JPanel

public class SplitServiceImpl : SplitService {

  public override val adjustmentAmount: Float = .05f

  public override fun adjustOwner(
    coord: Point,
    adjustmentType: AdjustmentType,
    context: ExecutionContext) {
    val ownerOrientation = when (adjustmentType) {
         ShrinkHorizontally,
         StretchHorizontally -> Horizontal
         ShrinkVertically,
         StretchVertically -> Vertical
    }
    val owner = getOwner(coord, ownerOrientation, context.context as DataContext) ?:
      return
    val owningPanelPosition = owner.getOwningPanel(coord)!!.position
    if (owningPanelPosition == SplitPanel.Position.Top ||
        owningPanelPosition == SplitPanel.Position.Left) {
      if (adjustmentType == ShrinkVertically || adjustmentType == ShrinkHorizontally) {
        owner.adjust(-adjustmentAmount)
      } else {
        owner.adjust(adjustmentAmount)
      }
    } else if (owningPanelPosition == SplitPanel.Position.Bottom ||
               owningPanelPosition == SplitPanel.Position.Right) {
      if (adjustmentType == StretchHorizontally || adjustmentType == StretchVertically) {
        owner.adjust(-adjustmentAmount)
      } else {
        owner.adjust(adjustmentAmount)
      }
    }
  }

  public override fun maximizeOwner(
    coord: Point,
    maximizeType: MaximizeType,
    context: ExecutionContext) {
    val rootNode = getRootNode(context.context as DataContext)
    val splitTree = getSplitTree(rootNode)
    val depthSorted = splitTree
      .groupBy { it.depth }
      .toSortedMap()
    for (group in depthSorted) {
      for (node in group.value) {
        val owningPanel = node.getOwningPanel(coord)
        if (owningPanel != null) {
          if (maximizeType == MaximizeType.MaximizeHorizontally) {
            if (owningPanel.position == SplitPanel.Position.Left) {
              node.setProportion(1.0f)
            } else if (owningPanel.position == SplitPanel.Position.Right) {
              node.setProportion(.0f)
            }
          } else if (maximizeType == MaximizeType.MaximizeVertically) {
            if (owningPanel.position == SplitPanel.Position.Top) {
              node.setProportion(1.0f)
            } else if (owningPanel.position == SplitPanel.Position.Bottom) {
              node.setProportion(.0f)
            }
          }
        }
      }
    }
  }

  public override fun equalizeSplits(context: ExecutionContext) {
    val rootNode = getRootNode(context.context as DataContext)
    val splitTree = getSplitTree(rootNode)
    val depthSorted = splitTree
      .groupBy { it.depth }
      .toSortedMap()
    for (group in depthSorted) {
      for (node in group.value) {
        node.setProportion(.5f)
      }
    }
  }

  private fun getOwner(
    coord: Point,
    ownerOrientation: Orientation,
    context: DataContext): SplitNode? {
    val rootNode = getRootNode(context)
    val splitTree = getSplitTree(rootNode)
    val depthSortedDesc = splitTree
      .filter { if (ownerOrientation == Vertical) it.isVertical()
                else !it.isVertical() }
      .groupBy { it.depth }
      .toSortedMap(compareByDescending { it })
    for (group in depthSortedDesc) {
      for (node in group.value) {
        if (node.getOwningPanel(coord) != null)
          return node
      }
    }
    return null
  }

  private fun getRootNode(context: DataContext): IjSplitNode {
    val project: Project? = PlatformDataKeys.PROJECT.getData(context)
    val editor = FileEditorManagerEx.getInstanceEx(Objects.requireNonNull(project)!!)
    val rootPanel = editor.splitters.getComponent(0) as JPanel
    val rootSplitter = rootPanel.getComponent(0) as Splitter
    return IjSplitNode(null, rootSplitter)
  }

  private fun getSplitTree(splitNode : IjSplitNode): MutableList<IjSplitNode> {
    val nodeTree: MutableList<IjSplitNode> = arrayListOf()
    var curr: IjSplitNode = splitNode
    while (true) {
      val rightSide = toBottom(curr, WalkingDirection.Right)
      if (rightSide.any()) {
        nodeTree.addAll(rightSide)
        curr = rightSide.last()
      }
      val part : IjSplitNode = lookupPartiallyWalked(curr) ?: break
      curr = part.walk(WalkingDirection.Left)
    }
    return nodeTree
  }

  private fun toBottom(node: IjSplitNode, dir: WalkingDirection): MutableList<IjSplitNode> {
    val nodeTree: MutableList<IjSplitNode> = arrayListOf()
    var curr : IjSplitNode = node
    while (curr.isWalkable(dir)) {
      nodeTree.add(curr)
      curr = curr.walk(dir)
    }
    return nodeTree
  }

  private fun lookupPartiallyWalked(node: IjSplitNode): IjSplitNode? {
    var current: IjSplitNode = node
    while (current.isFullyWalked() && current.isWalkable(WalkingDirection.Back)) {
      current = current.walk(WalkingDirection.Back)
    }
    if (current.isFullyWalked())
      return null
    return current
  }
}

public class IjSplitNode(
  public override val parent: IjSplitNode?,
  private val splitter: Splitter?) : SplitNode {

  private var walkedLeft = false
  private var walkedRight = false

  private var firstPanel : IjSplitPanel? = null
  private var secondPanel : IjSplitPanel? = null

  public var depth : Int = 0

  init {
    if (splitter != null) {
      firstPanel = IjSplitPanel(
        splitter.firstComponent as JPanel,
        if (splitter.isVertical) SplitPanel.Position.Top
        else SplitPanel.Position.Left
      )
      secondPanel = IjSplitPanel(
        splitter.secondComponent as JPanel,
        if (splitter.isVertical) SplitPanel.Position.Bottom
        else SplitPanel.Position.Right
      )
      if (parent!= null)
        depth = parent.depth + 1
    }
  }

  public override fun isVertical(): Boolean {
    return splitter != null && splitter.isVertical
  }

  public override fun adjust(adjustmentAmount: Float) {
    if (splitter == null)
      throw Exception("no adjustable splitter")
    val newProportion = splitter.proportion + adjustmentAmount
    if (newProportion in 0.0..1.0) {
      splitter.proportion = newProportion
    }
  }

  public override fun setProportion(proportion: Float) {
    if (splitter == null)
      throw Exception("no adjustable splitter")
    if (proportion < 0 || proportion > 1)
      throw Exception("invalid proportion")
    splitter.proportion = proportion
  }

  public fun walk(dir : WalkingDirection): IjSplitNode {
    if (!isWalkable(dir))
      throw Exception("node not walkable in direction $dir")
    val panel : JPanel
    when (dir) {
      WalkingDirection.Right -> {
        panel = splitter!!.firstComponent as JPanel
        walkedRight = true
      }
      WalkingDirection.Left -> {
        panel = splitter!!.secondComponent as JPanel
        walkedLeft = true
      }
      WalkingDirection.Back -> {
        return this.parent!!
      }
    }
    val splitter = getSplitter(panel)
    return IjSplitNode(this, splitter)
  }

  public override fun getOwningPanel(coord: Point): SplitPanel? {
    if (firstPanel == null || secondPanel == null)
      return null
    return if (firstPanel!!.getScreenRect().contains(coord))
      firstPanel
    else if (secondPanel!!.getScreenRect().contains(coord))
      secondPanel
    else null
  }

  public fun isWalkable(direction: WalkingDirection): Boolean {
    return when (direction) {
      WalkingDirection.Left, WalkingDirection.Right -> splitter != null
      WalkingDirection.Back -> parent != null
    }
  }

  public fun isFullyWalked(): Boolean {
    return !isWalkable(WalkingDirection.Left) && !isWalkable(WalkingDirection.Right) ||
            walkedLeft && walkedRight
  }

  private fun getSplitter(panel: JPanel): Splitter? {
    val component: Optional<Component> = Arrays.stream(panel.components)
      .filter { x: Component? -> x is Splitter }
      .findFirst()
    if (component.isEmpty) return null
    return component.get() as Splitter
  }
}

public class IjSplitPanel(
  private val panel: JPanel,
  public override val position: SplitPanel.Position) : SplitPanel {

  public override fun getScreenRect() : Rectangle
  {
    return Rectangle(panel.locationOnScreen, panel.size)
  }
}
