/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.pages

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.CommonContainerFixture
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.byXpath
import ui.utils.escape
import java.awt.Point

@JvmOverloads
fun ContainerFixture.editor(title: String, function: Editor.() -> Unit = {}): Editor {
  find<ComponentFixture>(
    byXpath("//div[@class='EditorTabs']//div[@accessiblename='$title' and @class='SimpleColoredComponent']"),
  ).click()
  return find<Editor>(
    byXpath("title '$title'", "//div[@accessiblename='Editor for $title' and @class='EditorComponentImpl']"),
  )
    .apply { runJs("robot.moveMouse(component);") }
    .apply(function)
}

@FixtureName("Editor")
class Editor(
  remoteRobot: RemoteRobot,
  remoteComponent: RemoteComponent,
) : CommonContainerFixture(remoteRobot, remoteComponent) {
  val text: String
    get() = callJs("component.getEditor().getDocument().getText()", true)

  val selectedText: String
    get() = callJs("component.getEditor().getSelectionModel().getSelectedText()", true)

  val caretOffset: Int
    get() = callJs("component.getEditor().getCaretModel().getOffset()", runInEdt = true)

  val isBlockCursor: Boolean
    //    get() = callJs("component.getEditor().getSettings().isBlockCursor()", true)
    // Doesn't work at the moment because remote robot can't resolve classes from a plugin classloader
    get() = callJs("com.maddyhome.idea.vim.helper.CaretVisualAttributesHelperKt.hasBlockOrUnderscoreCaret(component.getEditor())", true)

  fun injectText(text: String) {
    runJs(
      """
      const app = com.intellij.openapi.application.ApplicationManager.getApplication()

      app.invokeLaterOnWriteThread(()=>{
          app['runWriteAction(com.intellij.openapi.util.Computable)'](()=>{
              component.getEditor().getDocument().setText('${text.escape()}')
          })
        })
""",
    )
  }

  @Suppress("unused")
  fun findPointByOffset(offset: Int): Point {
    return callJs(
      """
            const editor = component.getEditor()
            const visualPosition = editor.offsetToVisualPosition($offset)
            editor.visualPositionToXY(visualPosition) 
        """,
      true,
    )
  }

  @Suppress("unused")
  fun moveToLine(lineNumber: Int) {
    val pointToClick = callJs<Point>(
      """
            importClass(com.intellij.openapi.editor.ScrollType)
            const editor = component.getEditor()
            const document = editor.getDocument()
            const offset = document.getLineStartOffset($lineNumber - 1)
            editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.CENTER)
            const visualPosition = editor.offsetToVisualPosition(offset)
            editor.visualPositionToXY(visualPosition)
        """,
      runInEdt = true,
    )
    // wait a bit for scroll completed
    Thread.sleep(500)

    click(pointToClick)
  }
}
