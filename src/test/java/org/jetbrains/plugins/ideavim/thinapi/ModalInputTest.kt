/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.scopes.VimScope
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ExecutionContextManager
import com.maddyhome.idea.vim.api.VimCommandLineCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimEditorGroup
import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.VimModalInput
import com.maddyhome.idea.vim.api.VimModalInputService
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.ListenerOwner
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.interceptors.VimInputInterceptor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.thinapi.VimScopeImpl
import org.jetbrains.plugins.ideavim.mock.MockTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModalInputTest : MockTestCase() {
  private lateinit var vimScope: VimScope
  private lateinit var modalInputService: VimModalInputService
  private lateinit var modalInput: VimModalInput
  private lateinit var vimEditor: VimEditor
  private lateinit var executionContext: ExecutionContext
  private lateinit var mockInjector: VimInjector
  private lateinit var realInjector: VimInjector
  private lateinit var commandLineCaret: VimCommandLineCaret
  private lateinit var editorGroup: VimEditorGroup
  private lateinit var executionContextManager: ExecutionContextManager

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")

    val listenerOwner = ListenerOwner.Plugin.get("test")
    val mappingOwner = MappingOwner.Plugin.get("test")
    vimScope = VimScopeImpl(listenerOwner, mappingOwner)

    mockInjector = spy(injector)

    modalInputService = mock()
    Mockito.`when`(mockInjector.modalInput).thenReturn(modalInputService)

    modalInput = mock()
    commandLineCaret = mock()
    Mockito.`when`(modalInput.caret).thenReturn(commandLineCaret)


    vimEditor = fixture.editor.vim
    executionContext = mock()

    editorGroup = mock()
    Mockito.`when`(editorGroup.getFocusedEditor()).thenReturn(vimEditor)
    Mockito.`when`(mockInjector.editorGroup).thenReturn(editorGroup)

    executionContextManager = mock()
    Mockito.`when`(executionContextManager.getEditorExecutionContext(vimEditor)).thenReturn(executionContext)
    Mockito.`when`(mockInjector.executionContextManager).thenReturn(executionContextManager)

    Mockito.`when`(modalInputService.create(any(), any(), anyString(), any())).thenReturn(modalInput)

    realInjector = injector
    injector = mockInjector
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    super.tearDown(testInfo)
    injector = realInjector
  }

  fun assertEqualsEditor(expected: VimEditor, actual: VimEditor) {
    assertTrue(
      expected.getPath() == actual.getPath(),
      "Expected and actual VimEditor instances do not match in file path"
    )
  }

  @Test
  fun `test updateLabel with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    var currentLabel = ""

    Mockito.`when`(modalInput.label).thenReturn(label)

    vimScope.modalInput()
      .repeat(2)
      .updateLabel { newLabel ->
        currentLabel = "$newLabel - Updated"
        currentLabel
      }
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor<*>>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val interceptor = interceptorCaptor.firstValue as VimInputInterceptor<Char>
    val testChar = 'a'
    interceptor.executeInput(testChar, vimEditor, executionContext)
    interceptor.executeInput(testChar, vimEditor, executionContext)

    assertEquals(2, handlerCalledCount)
    assertEquals("Enter character: - Updated", currentLabel)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test repeatWhile with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    vimScope.modalInput()
      .repeatWhile {
        handlerCalledCount < 1
      }
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor<*>>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val interceptor = interceptorCaptor.firstValue as VimInputInterceptor<Char>
    val testChar = 'a'
    interceptor.executeInput(testChar, vimEditor, executionContext)

    assertEquals(1, handlerCalledCount)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test repeat 3 times with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    vimScope.modalInput()
      .repeat(3)
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor<*>>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val interceptor = interceptorCaptor.firstValue as VimInputInterceptor<Char>
    val testChar = 'a'
    interceptor.executeInput(testChar, vimEditor, executionContext)
    interceptor.executeInput(testChar, vimEditor, executionContext)
    interceptor.executeInput(testChar, vimEditor, executionContext)

    assertEquals(3, handlerCalledCount)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test inputString without modifiers`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedString: String? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    vimScope.modalInput()
      .inputString(label) { string ->
        handlerCalledCount++
        receivedString = string
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor<*>>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val interceptor = interceptorCaptor.firstValue as VimInputInterceptor<String>
    val testString = "test"
    interceptor.executeInput(testString, vimEditor, executionContext)

    assertEquals(1, handlerCalledCount)
    assertEquals(testString, receivedString)
  }

  @Test
  fun `test inputChar without other modifiers`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    vimScope.modalInput()
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor<*>>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val interceptor = interceptorCaptor.firstValue as VimInputInterceptor<Char>
    val testChar = 'a'
    interceptor.executeInput(testChar, vimEditor, executionContext)

    assertEquals(1, handlerCalledCount)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test closeCurrentInput with existing input`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(modalInput)

    val result = vimScope.modalInput().closeCurrentInput()

    verify(modalInput).deactivate(eq(true), eq(true))

    assertTrue(result)
  }

  @Test
  fun `test closeCurrentInput with no existing input`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(null)

    val result = vimScope.modalInput().closeCurrentInput()

    assertFalse(result)
  }

  @Test
  fun `test closeCurrentInput with refocusEditor false`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(modalInput)

    vimScope.modalInput().closeCurrentInput(refocusEditor = false)

    verify(modalInput).deactivate(eq(false), eq(true))
  }
}
