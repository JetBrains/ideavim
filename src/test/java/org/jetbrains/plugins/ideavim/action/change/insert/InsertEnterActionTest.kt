/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.insert

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionHandlerBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.testFramework.ExtensionTestUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo

class InsertEnterActionTest : VimTestCase() {
  @BeforeEach
  fun setUp(repetitionInfo: RepetitionInfo) {
    // Set up a different combination of handlers for enter action
    // There is a specific that due to IDEA-300030 the existing for "forEach" handler may affect our handlers execution.
    val mainBean = EditorActionHandlerBean()
    mainBean.implementationClass = "com.maddyhome.idea.vim.handler.VimEnterHandler"
    mainBean.action = "EditorEnter"
    mainBean.setPluginDescriptor(PluginManagerCore.getPlugin(VimPlugin.getPluginId())!!)

    val singleBean = EditorActionHandlerBean()
    singleBean.implementationClass = DestroyerHandlerSingle::class.java.name
    singleBean.action = "EditorEnter"
    singleBean.setPluginDescriptor(PluginManagerCore.getPlugin(VimPlugin.getPluginId())!!)

    val forEachBean = EditorActionHandlerBean()
    forEachBean.implementationClass = DestroyerHandlerForEach::class.java.name
    forEachBean.action = "EditorEnter"
    forEachBean.setPluginDescriptor(PluginManagerCore.getPlugin(VimPlugin.getPluginId())!!)

    if (injector.application.isOctopusEnabled()) {
      if (repetitionInfo.currentRepetition == 1) {
        ExtensionTestUtil.maskExtensions(
          ExtensionPointName("com.intellij.editorActionHandler"),
          listOf(mainBean),
          fixture.testRootDisposable
        )
      } else if (repetitionInfo.currentRepetition == 2) {
        ExtensionTestUtil.maskExtensions(
          ExtensionPointName("com.intellij.editorActionHandler"),
          listOf(singleBean, mainBean),
          fixture.testRootDisposable
        )
      } else if (repetitionInfo.currentRepetition == 3) {
        ExtensionTestUtil.maskExtensions(
          ExtensionPointName("com.intellij.editorActionHandler"),
          listOf(forEachBean, mainBean),
          fixture.testRootDisposable
        )
      }
    }
  }

  @RepeatedTest(3)
  fun `test insert enter`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<Enter>"), before, after, Mode.INSERT)
  }

  @RepeatedTest(3)
  fun `test insert enter multicaret`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |${c}Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |
        |${c}Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<Enter>"), before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @RepeatedTest(3)
  fun `test insert enter with C-M`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<C-M>"), before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.CTRL_CODES)
  @RepeatedTest(3)
  fun `test insert enter with C-J`() {
    val before = """Lorem ipsum dolor sit amet,
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    val after = """Lorem ipsum dolor sit amet,
        |
        |${c}consectetur adipiscing elit
        |Sed in orci mauris.
        |Cras id tellus in ex imperdiet egestas.
    """.trimMargin()
    doTest(listOf("i", "<C-J>"), before, after, Mode.INSERT)
  }

  @TestWithoutNeovim(SkipNeovimReason.OPTION)
  @RepeatedTest(3)
  fun `test insert enter scrolls view up at scrolloff`() {
    configureByLines(50, "Lorem ipsum dolor sit amet,")
    enterCommand("set scrolloff=10")
    setPositionAndScroll(5, 29)
    typeText("i", "<Enter>")
    assertPosition(30, 0)
    assertVisibleArea(6, 40)
  }
}

/**
 * An empty handler that works as run "for each caret"
 */
internal class DestroyerHandlerForEach(private val nextHandler: EditorActionHandler) : EditorActionHandler(true) {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }
}

/**
 * An empty handler that works as run "single time"
 */
internal class DestroyerHandlerSingle(private val nextHandler: EditorActionHandler) : EditorActionHandler(false) {
  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
    nextHandler.execute(editor, caret, dataContext)
  }

  override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean {
    return nextHandler.isEnabled(editor, caret, dataContext)
  }
}
