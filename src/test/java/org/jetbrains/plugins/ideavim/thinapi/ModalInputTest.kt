/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.thinapi

import com.intellij.vim.api.VimApi
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
import com.maddyhome.idea.vim.thinapi.VimApiImpl
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
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModalInputTest : MockTestCase() {
  private lateinit var myVimApi: VimApi
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
    myVimApi = VimApiImpl(listenerOwner, mappingOwner, null)

    mockInjector = spy(injector)

    modalInputService = mock()
    Mockito.`when`(mockInjector.modalInput).thenReturn(modalInputService)

    modalInput = mock()
    commandLineCaret = mock()
    Mockito.`when`(modalInput.caret).thenReturn(commandLineCaret)


    vimEditor = fixture.editor.vim
    executionContext = mock()

    editorGroup = mock()
    Mockito.`when`(mockInjector.editorGroup).thenReturn(editorGroup)
    Mockito.`when`(mockInjector.fallbackWindow).thenReturn(vimEditor)

    executionContextManager = mock()
    Mockito.`when`(executionContextManager.getEditorExecutionContext(vimEditor)).thenReturn(executionContext)
    Mockito.`when`(mockInjector.executionContextManager).thenReturn(executionContextManager)

    Mockito.`when`(modalInputService.create(any(), any(), anyString(), any())).thenReturn(modalInput)

    realInjector = injector
    injector = mockInjector
  }

  @AfterEach
  override fun tearDown(testInfo: TestInfo) {
    // Restore real injector before tearDown to avoid EDT access violations
    // (mocked fallbackWindow returns the fixture editor which requires EDT for option resets)
    injector = realInjector

    super.tearDown(testInfo)
    // reset mocks
    Mockito.reset(modalInputService)
    Mockito.reset(modalInput)
    Mockito.reset(commandLineCaret)
    Mockito.reset(executionContext)
    Mockito.reset(editorGroup)
    Mockito.reset(executionContextManager)
    Mockito.reset(mockInjector)
  }

  fun assertEqualsEditor(expected: VimEditor, actual: VimEditor) {
    assertTrue(
      expected.getPath() == actual.getPath(),
      "Expected and actual VimEditor instances do not match in file path"
    )
  }

  fun consumeByInterceptor(chars: String, interceptor: VimInputInterceptor) {
    injector.parser.parseKeys(chars).forEach { c ->
      interceptor.consumeKey(c, vimEditor, executionContext)
    }
  }

  fun consumeByInterceptor(char: Char, interceptor: VimInputInterceptor) {
    consumeByInterceptor(char.toString(), interceptor)
  }

  @Test
  fun `test updateLabel with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    var currentLabel = ""

    Mockito.`when`(modalInput.label).thenReturn(label)

    runBlocking { myVimApi.modalInput() }
      .repeat(2)
      .updateLabel { newLabel ->
        currentLabel = "$newLabel - Updated"
        currentLabel
      }
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val testChars = "aa"
    consumeByInterceptor(chars = testChars, interceptor = interceptorCaptor.firstValue)

    assertEquals(2, handlerCalledCount)
    assertEquals("Enter character: - Updated", currentLabel)
    assertEquals(testChars.last(), receivedChar)
  }

  @Test
  fun `test repeatWhile with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    runBlocking { myVimApi.modalInput() }
      .repeatWhile {
        handlerCalledCount < 1
      }
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val testChar = 'a'
    consumeByInterceptor(char = testChar, interceptor = interceptorCaptor.firstValue)

    assertEquals(1, handlerCalledCount)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test repeat 3 times with inputChar`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    runBlocking { myVimApi.modalInput() }
      .repeat(3)
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val testChars = "aaa"
    consumeByInterceptor(chars = testChars, interceptor = interceptorCaptor.firstValue)

    assertEquals(3, handlerCalledCount)
    assertEquals(testChars.last(), receivedChar)
  }

  @Test
  fun `test inputString without modifiers`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedString: String? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    runBlocking { myVimApi.modalInput() }
      .inputString(label) { string ->
        handlerCalledCount++
        receivedString = string
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val testWord = "test"
    consumeByInterceptor(chars = "$testWord<CR>", interceptor = interceptorCaptor.firstValue)

    assertEquals(1, handlerCalledCount)
    assertEquals(testWord, receivedString)
  }

  @Test
  fun `test inputChar without other modifiers`() {
    val label = "Enter character:"
    var handlerCalledCount = 0
    var receivedChar: Char? = null

    Mockito.`when`(modalInput.label).thenReturn(label)

    runBlocking { myVimApi.modalInput() }
      .inputChar(label) { char ->
        handlerCalledCount++
        receivedChar = char
      }

    val interceptorCaptor = argumentCaptor<VimInputInterceptor>()
    val editorCaptor = argumentCaptor<VimEditor>()

    verify(modalInputService).create(
      editorCaptor.capture(),
      eq(executionContext),
      eq(label),
      interceptorCaptor.capture()
    )

    assertEqualsEditor(vimEditor, editorCaptor.firstValue)

    val testChar = 'a'
    consumeByInterceptor(char = testChar, interceptor = interceptorCaptor.firstValue)

    assertEquals(1, handlerCalledCount)
    assertEquals(testChar, receivedChar)
  }

  @Test
  fun `test closeCurrentInput with existing input`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(modalInput)

    val result = runBlocking { myVimApi.modalInput() }.closeCurrentInput()

    verify(modalInput).deactivate(eq(true), eq(true))

    assertTrue(result)
  }

  @Test
  fun `test closeCurrentInput with no existing input`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(null)

    val result = runBlocking { myVimApi.modalInput() }.closeCurrentInput()

    assertFalse(result)
  }

  @Test
  fun `test closeCurrentInput with refocusEditor false`() {
    Mockito.`when`(modalInputService.getCurrentModalInput()).thenReturn(modalInput)

    runBlocking { myVimApi.modalInput() }.closeCurrentInput(refocusEditor = false)

    verify(modalInput).deactivate(eq(false), eq(true))
  }
}
