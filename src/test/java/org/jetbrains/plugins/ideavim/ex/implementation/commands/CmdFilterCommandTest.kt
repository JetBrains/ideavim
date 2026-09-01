/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.CmdFilterCommand
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CmdFilterCommandTest : VimTestCase() {

  @TempDir
  lateinit var tempDir: Path
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("!ls")
    assertTrue(command is CmdFilterCommand)
    assertEquals("ls", command.argument)
  }

  @Test
  @TestWithoutNeovim(
    reason = SkipNeovimReason.SEE_DESCRIPTION,
    description = "Runs an external command. Neovim's own output for `:!` is not routed through IdeaVim's ex output " +
      "panel, so there is nothing to compare.",
  )
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses the POSIX 'sort' command and a POSIX shell")
  fun `command reading stdin does not wait for input`() {
    configureByText("Lorem ipsum dolor sit amet\n")

    // `sort` with no arguments reads stdin. Vim hands such a command the terminal it is running in, and Neovim
    // redirects stdin to /dev/null. We have no terminal to hand over, so the command must see an immediate EOF and
    // exit, rather than blocking forever waiting for input that can never arrive.
    assertCommandOutput("!sort", ":!sort\n")
  }

  // % filename modifier expansion in :!

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang percent colon h expands to directory of current file`() {
    val source = tempDir.resolve("file.txt")
    source.createFile()
    val vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(source)!!
    ApplicationManager.getApplication().invokeAndWait { fixture.openFileInEditor(vFile) }

    // The file lives outside every content root, so its buffer name is already absolute and %:h is tempDir —
    // touch creates "sentinel.txt" there
    enterCommand("!touch %:h/sentinel.txt")

    assertPluginError(false)
    assertTrue(tempDir.resolve("sentinel.txt").exists(), "sentinel.txt should be created in %:h directory")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang mv percent renames file using percent colon h for destination directory`() {
    val source = tempDir.resolve("original.txt")
    source.createFile()
    val vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(source)!!
    ApplicationManager.getApplication().invokeAndWait { fixture.openFileInEditor(vFile) }

    // % is the buffer name: absolute here, because the file is outside the project. %:h is its directory
    enterCommand("!mv % %:h/renamed.txt")

    assertPluginError(false)
    assertFalse(source.exists(), "original file should no longer exist after mv")
    assertTrue(tempDir.resolve("renamed.txt").exists(), "renamed.txt should exist in the same directory")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang cp percent colon p copies file to new location`() {
    val source = tempDir.resolve("original.txt")
    source.createFile()
    val vFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(source)!!
    ApplicationManager.getApplication().invokeAndWait { fixture.openFileInEditor(vFile) }

    // %:p is always the full path. It matches bare % only because this file is outside every content root, so
    // its buffer name is absolute too — see `bang percent is the buffer name while percent colon p is the full path`
    enterCommand("!cp %:p %:p:h/copy.txt")

    assertPluginError(false)
    assertTrue(source.exists(), "original should still exist after cp")
    assertTrue(tempDir.resolve("copy.txt").exists(), "copy.txt should exist in the same directory")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang percent is the buffer name while percent colon p is the full path`() {
    // Vim's `%` is the buffer name, which stays relative while the file is below the working directory; only `:p`
    // forces the full path. The light fixture puts the file under "/src", the project's content root, so this is
    // the in-project case the tempDir tests above cannot reach.
    configureByTextX("relative.txt", "")

    assertCommandOutput("!echo % %:p", ":!echo relative.txt /src/relative.txt\nrelative.txt /src/relative.txt")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang backslash percent is a literal percent`() {
    configureByTextX("probe.txt", "")

    // A backslash removes the special meaning; Vim drops the backslash and passes a bare % to the shell
    assertCommandOutput("""!echo \%""", ":!echo %\n%")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang leaves environment variables to the shell`() {
    configureByTextX("probe.txt", "")

    // Vim only expands the file-name specials in `:!`; `$FOO` has to reach the shell so its own assignment wins
    assertCommandOutput("!FOO=bar sh -c 'echo [\$FOO]'", ":!FOO=bar sh -c 'echo [\$FOO]'\n[bar]")
  }

  @Test
  @TestWithoutNeovim(reason = SkipNeovimReason.SEE_DESCRIPTION, description = "Uses real shell commands")
  @DisabledOnOs(OS.WINDOWS, disabledReason = "Uses POSIX shell commands")
  fun `bang expands a file name that itself contains a percent`() {
    configureByTextX("per%cent.txt", "")

    // The expanded name must not be rescanned, or the % it contains is expanded again, forever
    assertCommandOutput("!echo %", ":!echo per%cent.txt\nper%cent.txt")
  }
}
