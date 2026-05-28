/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.ui.ex

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.util.messages.MessageBusConnection
import com.maddyhome.idea.vim.ui.ToolWindowPositioningListener
import java.awt.Component
import java.awt.Cursor
import java.awt.LayoutManager
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities

internal abstract class GlassPaneManager {
  private val resizeAdapter: ComponentAdapter = object : ComponentAdapter() {
    override fun componentResized(e: ComponentEvent?) {
      onResize()
    }
  }

  private var originalLayout: LayoutManager? = null
  private var wasOpaque: Boolean = false
  private var child: Component? = null
  private var cursorHandlerDisposable: Disposable? = null
  private var toolWindowListenerConnection: MessageBusConnection? = null

  var glassPane: JComponent? = null
    private set

  fun activate(editor: Editor, child: Component) {
    val root = SwingUtilities.getRootPane(editor.component) ?: return
    glassPane = root.glassPane as JComponent?
    glassPane?.let {
      originalLayout = it.layout
      wasOpaque = it.isOpaque
      this.child = child
      it.add(child)
      it.addComponentListener(resizeAdapter)

      (it as? IdeGlassPane)?.let { ideGlassPane ->
        val disposable = Disposer.newDisposable()
        ideGlassPane.addMouseMotionPreprocessor(GlassPaneCursorHandler(ideGlassPane, child), disposable)
        cursorHandlerDisposable = disposable
      }

      editor.project?.messageBus?.connect()?.let { c ->
        c.subscribe(ToolWindowManagerListener.TOPIC, ToolWindowPositioningListener { onResize() })
        toolWindowListenerConnection = c
      }
    }
  }

  fun show() {
    glassPane?.isVisible = true
  }

  fun deactivate() {
    glassPane?.let {
      it.removeComponentListener(resizeAdapter)
      it.isVisible = false
      it.layout = originalLayout
      it.isOpaque = wasOpaque
      it.remove(child)

      toolWindowListenerConnection?.disconnect()
      toolWindowListenerConnection = null

      cursorHandlerDisposable?.dispose()
      cursorHandlerDisposable = null
    }
    glassPane = null
  }

  protected abstract fun onResize()

  private class GlassPaneCursorHandler(private val ideGlassPane: IdeGlassPane, private val child: Component)
    : MouseAdapter() {

    override fun mouseMoved(e: MouseEvent) = updateCursor(e)
    override fun mousePressed(e: MouseEvent) = updateCursor(e)
    override fun mouseReleased(e: MouseEvent) = updateCursor(e)
    override fun mouseClicked(e: MouseEvent) = updateCursor(e)

    private fun updateCursor(e: MouseEvent) {
      val point = SwingUtilities.convertPoint(e.component, e.point, child)
      if (child.contains(point)) {
        val target = SwingUtilities.getDeepestComponentAt(child, point.x, point.y)
        ideGlassPane.setCursor(target?.cursor ?: Cursor.getDefaultCursor(), GlassPaneManager::class.java)
      }
    }
  }
}
