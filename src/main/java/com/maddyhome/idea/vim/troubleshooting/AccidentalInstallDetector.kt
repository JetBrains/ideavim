/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.troubleshooting

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.EditorListener
import com.maddyhome.idea.vim.icons.VimIcons
import com.maddyhome.idea.vim.newapi.ij
import java.util.function.Function
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

// todo
@Service(Service.Level.APP)
class AccidentalInstallDetector {
  private var warningShown = false

  fun showVimWarning(editor: Editor) {
//    if (warningShown) return
//    if (VimPlugin.isNotEnabled()) return
//    if (VimRcService.findIdeaVimRc() != null) return
//    if (!injector.enabler.isNewIdeaVimUser()) return

    warningShown = true

    injector.application.invokeLater {
      // Add delay before the execution
      val text = HtmlBuilder().append("You're using Vim plugin.")
        .appendRaw("</br>")
        .append("Continue editing if you're familiar with it, or disable it if you don't know how to work using Vim.")
        .appendRaw("</br>").appendLink("Disable", "Disable").toString()
      var balloon: Balloon? = null
      balloon = JBPopupFactory.getInstance()
        .createHtmlTextBalloonBuilder(
          text, VimIcons.IDEAVIM,
          MessageType.INFO.titleForeground, MessageType.INFO.popupBackground,
          object : HyperlinkListener {
            override fun hyperlinkUpdate(e: HyperlinkEvent) {
              if (e.description == "Disable") {
                VimPlugin.setEnabled(false)
                balloon?.hide()
              }
            }
          }
        ).createBalloon()
      balloon.show(
        JBPopupFactory.getInstance().guessBestPopupLocation(editor),
        Balloon.Position.below
      )
    }
  }
}

internal class AccidentalInstallDetectorEditorListener : EditorListener {
  override fun created(editor: VimEditor) {
    ApplicationManager.getApplication().service<AccidentalInstallDetector>().showVimWarning(editor.ij)
  }
}

internal class AccidentalInstallDetectorEditorNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(
    project: Project,
    file: VirtualFile,
  ): Function<in FileEditor, out JComponent?>? {
    return Function { fileEditor: FileEditor ->
      val panel = EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info)
      panel.text = "<html>You're using the IdeaVim plugin. Disable it if you're not familiar with Vim.<br>This message will disappear after 10 commands.</html>"
      panel.icon(VimIcons.IDEAVIM)
      panel.createActionLabel("Disable") { VimPlugin.setEnabled(false) }
      panel.createActionLabel("Ok") {  }
      panel
    }
  }
}
