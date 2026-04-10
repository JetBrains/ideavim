/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import com.intellij.driver.client.Driver
import com.intellij.driver.sdk.openFile
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.config.ConfigurationStorage
import com.intellij.ide.starter.config.splitMode
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDERemDevTestContext
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

/**
 * Base class for IdeaVim split-mode tests.
 *
 * Launches a single IDE instance in split mode (backend + frontend) once per test class.
 * Individual tests create their own files and interact via high-level helpers:
 *
 * ```kotlin
 * openFile("src/Test.java")
 * typeVim("gcc")               // type vim keys
 * assertEditorContains("//")   // check editor text
 * assertCaretAtLine(2)         // check caret position
 * ```
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IdeaVimStarterTestBase {

  private lateinit var bgRun: BackgroundRun
  private lateinit var driver: Driver
  protected lateinit var projectDir: Path

  @BeforeAll
  fun launchIde() {
    projectDir = kotlin.io.path.createTempDirectory("ideavim-split-test")
    projectDir.resolve("src").createDirectories()
    projectDir.resolve("src/placeholder.txt").writeText("placeholder\n")

    ConfigurationStorage.splitMode(true)
    beforeContextCreated()

    val context = Starter.newContext(
      this::class.simpleName ?: "split-test",
      TestCase(IdeProductProvider.IU, LocalProjectInfo(projectDir)).useEAP()
    )

    val pluginPath = resolvePluginPath()
    PluginConfigurator(context).installPluginFromPath(pluginPath)

    if (ConfigurationStorage.splitMode()) {
      if (context is IDERemDevTestContext) {
        PluginConfigurator(context.frontendIDEContext).installPluginFromPath(pluginPath)
      }
      context.patchForMacOsSplitMode()
    }

    configureContext(context)

    bgRun = context.runIdeWithDriver()
    driver = bgRun.driver

    driver.withContext { waitForIndicators(5.minutes) }
  }

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
    ConfigurationStorage.splitMode(false)
  }

  protected open fun beforeContextCreated() {}
  protected open fun configureContext(context: IDETestContext) {}

  // ── File helpers ────────────────────────────────────────────

  /** Creates a file in the project. Returns relative path for [openFile]. */
  protected fun createFile(relativePath: String, content: String): String {
    val file = projectDir.resolve(relativePath)
    file.parent.createDirectories()
    file.writeText(content)
    return relativePath
  }

  // ── IDE interaction helpers ─────────────────────────────────

  /** Opens a file in the editor by relative path and waits until the editor is ready for input. */
  protected fun openFile(relativePath: String) {
    driver.withContext { openFile(relativePath) }
    ensureEditorReady(relativePath)
  }

  private fun ensureEditorReady(relativePath: String, timeoutMs: Long = 10_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var lastText = ""
    while (System.currentTimeMillis() < deadline) {
      try {
        lastText = editorText()
        if (lastText.isNotBlank()) break
      } catch (_: Exception) {
        // Editor component not yet available — retry
      }
      Thread.sleep(200)
    }
    check(lastText.isNotBlank()) {
      "Editor for '$relativePath' did not become ready within ${timeoutMs}ms"
    }
    clickEditor()

    // Verify IdeaVim's key handler is actually attached by sending `gg` (go to first line)
    // and checking the caret moves to line 1. This confirms vim is processing keystrokes.
    val vimReady = waitUntil(timeoutMs = 10_000, pollMs = 500) {
      try {
        driver.withContext {
          ideFrame { codeEditor().apply { waitFound(); keyboard { typeText("gg") } } }
        }
        Thread.sleep(300)
        caretLine() <= 1
      } catch (_: Exception) {
        false
      }
    }
    check(vimReady) {
      "IdeaVim key handler did not attach for '$relativePath' within timeout"
    }
  }

  /** Types vim keys in the active editor. */
  protected fun typeVim(keys: String) {
    driver.withContext {
      ideFrame { codeEditor().apply { waitFound(); keyboard { typeText(keys) } } }
    }
  }

  /** Types vim keys followed by Escape. */
  protected fun typeVimAndEscape(keys: String) {
    driver.withContext {
      ideFrame { codeEditor().apply { waitFound(); keyboard { typeText(keys); escape() } } }
    }
  }

  /** Types an ex command (without the leading `:`). */
  protected fun exCommand(command: String) {
    driver.withContext {
      ideFrame {
        codeEditor().apply {
          waitFound(); keyboard {
          typeText(
            ":$command",
            delayBetweenCharsInMs = 50
          ); enter()
        }
        }
      }
    }
  }

  protected fun esc() {
    driver.withContext {
      ideFrame { codeEditor().apply { waitFound(); keyboard { hotKey(java.awt.event.KeyEvent.VK_ESCAPE) } } }
    }
  }

  /** Presses Ctrl-O (jump backward). */
  protected fun ctrlO() {
    driver.withContext {
      ideFrame {
        codeEditor().apply {
          keyboard { hotKey(java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_O) }
        }
      }
    }
  }

  /** Triggers the IDE "Navigate > Back" action (Cmd+[ on macOS, Ctrl+Alt+Left on Linux). */
  protected fun ideaGoBack() {
    driver.withContext {
      ideFrame {
        codeEditor().apply {
          if (System.getProperty("os.name").lowercase().contains("mac")) {
            keyboard { hotKey(java.awt.event.KeyEvent.VK_META, java.awt.event.KeyEvent.VK_OPEN_BRACKET) }
          } else {
            keyboard {
              hotKey(
                java.awt.event.KeyEvent.VK_CONTROL,
                java.awt.event.KeyEvent.VK_ALT,
                java.awt.event.KeyEvent.VK_LEFT
              )
            }
          }
        }
      }
    }
  }

  /** Presses Ctrl-G (file info). */
  protected fun ctrlG() {
    driver.withContext {
      ideFrame {
        codeEditor().apply {
          keyboard { hotKey(java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_G) }
        }
      }
    }
  }

  /** Moves the caret to a specific line using the Driver SDK (not vim). */
  protected fun goToLine(line: Int) {
    driver.withContext {
      ideFrame { codeEditor().apply { waitFound(); goToLine(line) } }
    }
  }

  /** Clicks in the editor to ensure focus. */
  protected fun clickEditor() {
    driver.withContext {
      ideFrame { codeEditor().apply { waitFound(); click() } }
    }
  }

  /** Waits for a short time to let async operations settle. */
  protected fun pause(ms: Long = 500) {
    Thread.sleep(ms)
  }

  /**
   * Retries [check] every [pollMs] until it returns true or [timeoutMs] elapses.
   * Use instead of fixed pauses to handle variable split-mode RPC latency on CI.
   */
  protected fun waitUntil(
    timeoutMs: Long = 5000,
    pollMs: Long = 200,
    check: () -> Boolean,
  ): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (check()) return true
      Thread.sleep(pollMs)
    }
    return false
  }

  // ── Editor state helpers ────────────────────────────────────

  /** Reads the current editor text. */
  protected fun editorText(): String {
    var result = ""
    driver.withContext {
      ideFrame { result = codeEditor().apply { waitFound() }.text }
    }
    return result
  }

  /** Reads the current caret line (1-based). */
  protected fun caretLine(): Int {
    var result = 0
    driver.withContext {
      ideFrame { result = codeEditor().apply { waitFound() }.getCaretLine() }
    }
    return result
  }

  // ── Assertions ──────────────────────────────────────────────

  /** Asserts the editor contains the given text, polling until timeout. */
  protected fun assertEditorContains(expected: String, message: String? = null) {
    var text = ""
    val found = waitUntil { text = editorText(); text.contains(expected) }
    assertTrue(found) {
      (message ?: "Editor should contain '$expected'") + ". Actual: $text"
    }
  }

  /** Asserts the editor does NOT contain the given text, polling until timeout. */
  protected fun assertEditorNotContains(unexpected: String, message: String? = null) {
    // Give operations time to settle, then check
    pause(1000)
    val text = editorText()
    assertFalse(text.contains(unexpected)) {
      (message ?: "Editor should not contain '$unexpected'") + ". Actual: $text"
    }
  }

  /** Asserts the caret is at the given line (1-based), polling until timeout. */
  protected fun assertCaretAtLine(expected: Int, message: String? = null) {
    var actual = 0
    val found = waitUntil { actual = caretLine(); actual == expected }
    assertTrue(found) {
      (message ?: "Caret should be at line $expected") + ". Actual line: $actual"
    }
  }

  /** Asserts the caret is before the given line, polling until timeout. */
  protected fun assertCaretBefore(line: Int, message: String? = null) {
    var actual = 0
    val found = waitUntil { actual = caretLine(); actual < line }
    assertTrue(found) {
      (message ?: "Caret should be before line $line") + ". Actual line: $actual"
    }
  }

  /** Asserts the caret is past the given line, polling until timeout. */
  protected fun assertCaretAfter(line: Int, message: String? = null) {
    var actual = 0
    val found = waitUntil { actual = caretLine(); actual > line }
    assertTrue(found) {
      (message ?: "Caret should be after line $line") + ". Actual line: $actual"
    }
  }

  companion object {
    fun resolvePluginPath(): Path {
      val pathStr = System.getProperty("ideavim.plugin.path")
        ?: error(
          "System property 'ideavim.plugin.path' is not set. " +
            "Run via: ./gradlew :tests:split-mode-tests:testSplitMode"
        )
      return Path(pathStr)
    }
  }
}
