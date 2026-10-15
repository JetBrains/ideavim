/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.rider

import com.intellij.driver.client.Driver
import com.intellij.driver.client.Remote
import com.intellij.driver.model.OnDispatcher
import com.intellij.driver.sdk.Editor
import com.intellij.driver.sdk.VirtualFile
import com.intellij.driver.sdk.openEditor
import com.intellij.driver.sdk.singleProject
import com.intellij.driver.sdk.ui.components.UiComponent.Companion.waitFound
import com.intellij.driver.sdk.ui.components.common.JEditorUiComponent
import com.intellij.driver.sdk.ui.components.common.codeEditor
import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.requestFocusFromIde
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.BackgroundRun
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IDETestContext
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.LocalProjectInfo
import com.intellij.ide.starter.runner.Starter
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.awt.event.KeyEvent
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.minutes

/**
 * Base class for IdeaVim tests in Rider.
 *
 * Launches Rider once per test class on a generated console project, and opens [PROGRAM]. Rider implements a lot of
 * editor features on its ReSharper backend, so these tests cover behaviour the IntelliJ tests can't.
 *
 * Without a license Rider runs on a trial. To use a license, set `RIDER_LICENSE_KEY` to the base64-encoded `rider.key`
 * from the config directory of an activated Rider. Loading the project needs a .NET SDK, which Rider installs itself.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IdeaVimRiderTestBase {

  private lateinit var bgRun: BackgroundRun
  private lateinit var driver: Driver
  protected lateinit var projectDir: Path

  @BeforeAll
  fun launchIde() {
    projectDir = kotlin.io.path.createTempDirectory("ideavim-rider-test")
    createConsoleProject()

    val context = Starter.newContext(
      this::class.simpleName ?: "rider-test",
      // Open the .sln itself: given the directory, Rider asks which solution to open
      TestCase(RIDER, LocalProjectInfo(projectDir.resolve("$PROJECT.sln"))).useRelease(RIDER_VERSION)
    )
    context.trustAllSolutions()
    PluginConfigurator(context).installPluginFromPath(resolvePluginPath())
    context.useTestIdeaVimRc()
    // Without a license, Rider runs on a trial. GitHub Actions passes a missing secret as an empty string
    context.setLicense(System.getenv("RIDER_LICENSE_KEY")?.takeIf { it.isNotBlank() })

    bgRun = context.runIdeWithDriver()
    driver = bgRun.driver

    driver.withContext { waitForIndicators(10.minutes) }
    openProgram()
    waitForVim()
  }

  @AfterAll
  fun closeIde() {
    if (::bgRun.isInitialized) bgRun.closeIdeAndWait()
  }

  /** Content of the ideavimrc the launched IDE will use */
  protected open fun ideaVimRcContent(): String = ""

  /** Points the launched IDE at a test-owned ideavimrc, so the developer's real `~/.ideavimrc` doesn't change results */
  private fun IDETestContext.useTestIdeaVimRc() {
    val vimrc = kotlin.io.path.createTempDirectory("ideavim-rider-vimrc").resolve(".ideavimrc")
    vimrc.writeText(ideaVimRcContent())
    applyVMOptionsPatch { withEnv("IDEA_VIM_CUSTOM_VIMRC", vimrc.absolutePathString()) }
  }

  /**
   * Skips Rider's `Trust and Open Solution?` dialog. Rider keeps trusted solutions by their `.idea/.idea.<name>`
   * directory, which the starter doesn't know about, so trust everything instead
   */
  private fun IDETestContext.trustAllSolutions() {
    paths.configDir.resolve("options").createDirectories().resolve("trustedSolutions.xml").writeText(
      """
      |<application>
      |  <component name="TrustedSolutionStore">
      |    <option name="trustEverything" value="true" />
      |    <option name="trustedLocations">
      |      <set />
      |    </option>
      |  </component>
      |</application>
      |
      """.trimMargin()
    )
  }

  private fun createConsoleProject() {
    projectDir.resolve("$PROJECT.sln").writeText(
      """
      |
      |Microsoft Visual Studio Solution File, Format Version 12.00
      |Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "$PROJECT", "$PROJECT\$PROJECT.csproj", "{6F2C3B1E-8A4D-4C5B-9E7F-1A2B3C4D5E6F}"
      |EndProject
      |Global
      |	GlobalSection(SolutionConfigurationPlatforms) = preSolution
      |		Debug|Any CPU = Debug|Any CPU
      |	EndGlobalSection
      |	GlobalSection(ProjectConfigurationPlatforms) = postSolution
      |		{6F2C3B1E-8A4D-4C5B-9E7F-1A2B3C4D5E6F}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
      |		{6F2C3B1E-8A4D-4C5B-9E7F-1A2B3C4D5E6F}.Debug|Any CPU.Build.0 = Debug|Any CPU
      |	EndGlobalSection
      |EndGlobal
      |
      """.trimMargin()
    )
    val project = projectDir.resolve(PROJECT).createDirectories()
    project.resolve("$PROJECT.csproj").writeText(
      """
      |<Project Sdk="Microsoft.NET.Sdk">
      |  <PropertyGroup>
      |    <OutputType>Exe</OutputType>
      |    <TargetFramework>net8.0</TargetFramework>
      |    <ImplicitUsings>enable</ImplicitUsings>
      |  </PropertyGroup>
      |</Project>
      |
      """.trimMargin()
    )
    project.resolve("Program.cs").writeText("\n")
  }

  /** Opens [PROGRAM] by its absolute path: in Rider, the frontend's content roots don't contain the project files */
  private fun openProgram() {
    val path = projectDir.resolve(PROGRAM).absolutePathString()
    driver.withContext {
      val file = checkNotNull(utility(LocalFileSystemUtil::class).getInstance().refreshAndFindFileByPath(path)) {
        "Can't find $path"
      }
      openEditor(file)
    }
  }

  /** Checks that IdeaVim handles keys in the editor, by typing `gg` at the end of a two-line file */
  private fun waitForVim() {
    val ready = waitUntil(timeoutMs = 60_000, pollMs = 500) {
      try {
        setText("first\nsecond\n", caretAt = "second")
        typeVim("gg")
        waitUntil(timeoutMs = 3000) { caretOffset() == 0 }
      } catch (_: Exception) {
        false
      }
    }
    check(ready) { "IdeaVim did not handle keys in $PROGRAM" }
  }

  // ── IDE interaction helpers ─────────────────────────────────

  private fun <T> withEditor(block: JEditorUiComponent.() -> T): T {
    var result: Result<T>? = null
    driver.withContext {
      ideFrame { result = runCatching { codeEditor().apply { waitFound() }.block() } }
    }
    return checkNotNull(result).getOrThrow()
  }

  /** Runs [block] on the EDT, where the editor's models (caret, selection) may be accessed */
  private fun <T> onEdt(block: Editor.() -> T): T {
    val editor = withEditor { editor }
    return driver.withContext(OnDispatcher.EDT) { editor.block() }
  }

  /** Replaces the text of [PROGRAM] and puts the caret on the start of [caretAt], or at the start of the file */
  protected fun setText(text: String, caretAt: String? = null) {
    val offset = caretAt?.let { text.indexOf(it).also { offset -> check(offset >= 0) { "'$it' is not in the text" } } }
    withEditor { this.text = text }
    onEdt { getSelectionModel().removeSelection() }
    // Rider's backend applies its own caret state after the text change, which can move the caret again. Keep moving it
    // until it stays where it should be
    val target = offset ?: 0
    val placed = waitUntil(timeoutMs = 10_000, pollMs = 500) {
      if (caretOffset() != target) onEdt { getCaretModel().moveToOffset(target) }
      Thread.sleep(500)
      caretOffset() == target
    }
    check(placed) { "Could not put the caret at $target, it is at ${caretOffset()}" }
  }

  /** Types vim keys in the editor */
  protected fun typeVim(keys: String) {
    driver.withContext {
      requestFocusFromIde(singleProject())
      ideFrame {
        codeEditor().apply {
          waitFound()
          component.requestFocus()
          keyboard { typeText(keys) }
        }
      }
    }
  }

  /**
   * Runs an ex command (without the leading `:`). The command is pasted from the clipboard with `<C-R>+`, typing it
   * key by key is slow
   */
  protected fun exCommand(command: String) {
    driver.withContext(OnDispatcher.EDT) {
      utility(CopyPasteManagerUtil::class).getInstance().setContents(new(StringSelectionRef::class, command))
    }
    driver.withContext {
      ideFrame {
        codeEditor().apply {
          waitFound()
          keyboard {
            typeText(":")
            pressing(KeyEvent.VK_CONTROL) { key(KeyEvent.VK_R) }
            typeText("+")
            enter()
          }
        }
      }
    }
  }

  protected fun enter() {
    withEditor { keyboard { enter() } }
  }

  protected fun esc() {
    withEditor { keyboard { escape() } }
  }

  protected fun editorText(): String = withEditor { text }

  protected fun caretOffset(): Int = onEdt { getCaretModel().getOffset() }

  protected fun selectedText(): String? = onEdt {
    driver.cast(getSelectionModel(), SelectionModelRef::class).getSelectedText(false)
  }

  protected data class SelectionState(val start: Int, val end: Int, val caret: Int, val text: String?)

  protected fun selectionState(): SelectionState = onEdt {
    val selection = driver.cast(getSelectionModel(), SelectionModelRef::class)
    SelectionState(
      selection.getSelectionStart(),
      selection.getSelectionEnd(),
      getCaretModel().getOffset(),
      selection.getSelectedText(false),
    )
  }

  protected fun waitUntil(timeoutMs: Long = 5000, pollMs: Long = 200, check: () -> Boolean): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      if (check()) return true
      Thread.sleep(pollMs)
    }
    return false
  }

  // ── Assertions ──────────────────────────────────────────────

  /** Asserts the editor text, polling until timeout */
  protected fun assertText(expected: String) {
    var text = ""
    val found = waitUntil { text = editorText(); text == expected }
    assertTrue(found) { "Expected text:\n$expected\nActual:\n$text" }
  }

  /** Asserts the selected text, polling until timeout. Rider's backend applies some changes asynchronously */
  protected fun assertSelection(expected: String?) {
    var selection: String? = null
    val found = waitUntil { selection = selectedText(); selection == expected }
    assertTrue(found) { "Expected selection '$expected'. Actual: '$selection'" }
  }

  /**
   * Asserts the caret is on the first or the last selected character, as in Vim's Visual mode with the default
   * `selection=inclusive`, polling until timeout
   */
  protected fun assertCaretOnSelectionEnd() {
    var state: SelectionState? = null
    val found = waitUntil {
      val current = selectionState()
      state = current
      current.caret == current.start || current.caret == current.end - 1
    }
    assertTrue(found) { "Expected the caret on the first or the last selected character. Actual: $state" }
  }

  /** Asserts the selected text contains [text], polling until timeout */
  protected fun assertSelectionContains(text: String) {
    var selection: String? = null
    val found = waitUntil { selection = selectedText(); selection?.contains(text) == true }
    assertTrue(found) { "Expected selection containing '$text'. Actual: '$selection'" }
  }

  companion object {
    private const val PROJECT = "ConsoleApp"
    const val PROGRAM = "$PROJECT/Program.cs"

    /** `-PriderVersion=2026.1` runs the tests in another Rider release */
    private val RIDER_VERSION = System.getProperty("ideavim.rider.version", "2026.2.3.1")

    // Inlined IdeInfo for Rider, as the IdeInfo.Rider extension lives in a separate starter module
    private val RIDER = IdeInfo(
      productCode = "RD",
      platformPrefix = "Rider",
      executableFileName = "rider",
      fullName = "Rider",
      qodanaProductCode = "QDNET",
    )

    fun resolvePluginPath(): Path {
      val pathStr = System.getProperty("ideavim.plugin.path")
        ?: error(
          "System property 'ideavim.plugin.path' is not set. " +
            "Run via: ./gradlew :tests:ui-rd-tests:testUi"
        )
      return Path(pathStr)
    }
  }
}

@Remote("com.intellij.openapi.vfs.LocalFileSystem")
interface LocalFileSystemUtil {
  fun getInstance(): LocalFileSystem
}

@Remote("com.intellij.openapi.vfs.LocalFileSystem")
interface LocalFileSystem {
  fun refreshAndFindFileByPath(path: String): VirtualFile?
}

@Remote("com.intellij.openapi.editor.SelectionModel")
// The driver SDK's SelectionModel only has getSelectedText() without arguments in 2025.3
interface SelectionModelRef {
  fun getSelectionStart(): Int
  fun getSelectionEnd(): Int
  fun getSelectedText(allCarets: Boolean): String?
}

@Remote("com.intellij.openapi.ide.CopyPasteManager")
interface CopyPasteManagerUtil {
  fun getInstance(): CopyPasteManagerRef
}

@Remote("com.intellij.openapi.ide.CopyPasteManager")
interface CopyPasteManagerRef {
  fun setContents(content: StringSelectionRef)
}

@Remote("java.awt.datatransfer.StringSelection")
interface StringSelectionRef
