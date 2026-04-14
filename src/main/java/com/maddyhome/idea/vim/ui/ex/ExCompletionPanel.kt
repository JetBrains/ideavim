/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.ui.JBColor
import com.intellij.ui.SideBorder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

/**
 * Single-row panel showing completion candidates above the command line.
 * Paginates when items don't fit, keeping the selected item always visible.
 */
internal class ExCompletionPanel : JPanel(BorderLayout()) {

  private val itemsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))

  private var items: List<String> = emptyList()
  private var selectedIndex: Int? = null
  private var pageStart: Int = 0
  private var pageEnd: Int = 0

  private var itemFont: Font = font
  private var normalFg: Color = JBColor.foreground()
  private var normalBg: Color = JBColor.background()

  init {
    itemsPanel.isOpaque = true
    isOpaque = true
    border = SideBorder(JBColor.border(), SideBorder.TOP or SideBorder.BOTTOM)
    add(itemsPanel, BorderLayout.CENTER)
  }

  fun setItems(matches: List<String>, selected: Int?) {
    items = matches
    selectedIndex = selected
    rebuildPage()
  }

  fun setSelectedIndex(index: Int) {
    if (index == selectedIndex) return
    selectedIndex = index

    if (isOnCurrentPage(index)) updateHighlight() else rebuildPage()
  }

  fun updateColors(fg: Color, bg: Color) {
    normalFg = fg
    normalBg = bg
    background = normalBg
    itemsPanel.background = normalBg
  }

  fun updateFont(font: Font) {
    itemFont = font
  }


  private fun rebuildPage() {
    itemsPanel.removeAll()
    if (items.isEmpty()) return refreshLayout()
    if (items.size == 1) return

    calculateVisibleRange()
    addLabelsForRange()
    refreshLayout()
  }

  private fun calculateVisibleRange() {
    val selected = selectedIndex
    var start = if (selected != null && selected < pageStart) selected else pageStart
    var end = fitForward(start)

    if (selected != null && selected >= end) {
      end = selected + 1
      start = fitBackward(end)
    }

    pageStart = start
    pageEnd = end
  }

  private fun fitForward(from: Int): Int {
    var usedWidth = 0
    var end = from
    while (end < items.size) {
      val w = measureItem(items[end])
      if (usedWidth + w > availableWidth() && end > from) break
      usedWidth += w
      end++
    }
    return end
  }

  private fun fitBackward(from: Int): Int {
    var usedWidth = 0
    var start = from
    while (start > 0) {
      val w = measureItem(items[start - 1])
      if (usedWidth + w > availableWidth() && start < from) break
      usedWidth += w
      start--
    }
    return start
  }

  private fun addLabelsForRange() {
    for (i in pageStart until pageEnd) {
      itemsPanel.add(createLabel(items[i], isSelected = i == selectedIndex))
    }
  }

  // --- Highlight ---

  private fun updateHighlight() {
    for ((i, comp) in itemsPanel.components.withIndex()) {
      if (comp is JLabel) styleLabel(comp, isSelected = pageStart + i == selectedIndex)
    }
    repaint()
  }

  // --- Label factory ---

  private fun createLabel(text: String, isSelected: Boolean): JLabel {
    return JLabel(text).apply {
      font = itemFont
      isOpaque = true
      border = ITEM_BORDER
      styleLabel(this, isSelected)
    }
  }

  private fun styleLabel(label: JLabel, isSelected: Boolean) {
    label.foreground = if (isSelected) normalBg else normalFg
    label.background = if (isSelected) normalFg else normalBg
  }

  private fun isOnCurrentPage(index: Int) = index in pageStart until pageEnd

  private fun measureItem(text: String) = getFontMetrics(itemFont).stringWidth(text) + ITEM_PADDING

  private fun availableWidth() = if (width > 0) width else Int.MAX_VALUE

  private fun refreshLayout() {
    revalidate()
    repaint()
  }

  companion object {
    private const val ITEM_PADDING = 12
    private val ITEM_BORDER = JBUI.Borders.empty(2, 6)
  }
}
