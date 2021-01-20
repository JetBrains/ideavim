/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
            byXpath("//div[@class='EditorTabs']//div[@accessiblename='$title' and @class='SingleHeightLabel']")).click()
    return find<Editor>(
            byXpath("title '$title'", "//div[@accessiblename='Editor for $title' and @class='EditorComponentImpl']"))
            .apply { runJs("robot.moveMouse(component);") }
            .apply(function)
}

@FixtureName("Editor")
class Editor(remoteRobot: RemoteRobot, remoteComponent: RemoteComponent) : CommonContainerFixture(remoteRobot, remoteComponent) {
  val text: String
    get() = callJs("component.getEditor().getDocument().getText()", true)

  val selectedText: String
    get() = callJs("component.getEditor().getSelectionModel().getSelectedText()", true)

  val caretOffset: Int
    get() = callJs("component.getEditor().getCaretModel().getOffset()", runInEdt = true)

  fun injectText(text: String) {
    runJs("component.getEditor().getDocument().setText('${text.escape()}')", runInEdt = true)
  }

  fun findPointByOffset(offset: Int): Point {
    return callJs("""
            const editor = component.getEditor()
            const visualPosition = editor.offsetToVisualPosition($offset)
            editor.visualPositionToXY(visualPosition) 
        """, true)
  }

  fun moveToLine(lineNumber: Int) {
    val pointToClick = callJs<Point>("""
            importClass(com.intellij.openapi.editor.ScrollType)
            const editor = component.getEditor()
            const document = editor.getDocument()
            const offset = document.getLineStartOffset($lineNumber - 1)
            editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(offset), ScrollType.CENTER)
            const visualPosition = editor.offsetToVisualPosition(offset)
            editor.visualPositionToXY(visualPosition)
        """, runInEdt = true)
    // wait a bit for scroll completed
    Thread.sleep(500)

    click(pointToClick)
  }
}