/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.mock

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl
import com.intellij.testFramework.replaceService
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.newapi.vim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.mockito.Mockito
import javax.swing.JTextArea

open class MockTestCase : VimTestCase() {

  val editorStub = TextComponentEditorImpl(null, JTextArea()).vim
  val contextStub: ExecutionContext = DataContext.EMPTY_CONTEXT.vim

  fun <T : Any> mockService(service: Class<T>): T {
    val mock = Mockito.mock(service)
    val applicationManager = ApplicationManager.getApplication()
    applicationManager.replaceService(service, mock, this.testRootDisposable)
    return mock
  }
}
