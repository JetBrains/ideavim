/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.maddyhome.idea.vim.group.git.GitOutput
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Behaviour spec for `:Ggrep`, modelled on tpope/vim-fugitive (`s:GrepSubcommand`
 * in `autoload/fugitive.vim`). `:Ggrep {args}` runs `git grep {args}` in the work tree
 * of the current file and jumps to the first match (`:cfirst`); `:Ggrep!` doesn't jump.
 */
class FugitiveGrepTest : FugitiveTestCase() {

  @Test
  fun `test Ggrep runs git grep in work tree`() {
    configureByText("${c}foo")

    enterCommand("Ggrep foo")

    assertEquals("/src", git.lastWorkTree)
    assertEquals(listOf("-c", "core.quotePath=false", "grep", "--no-color", "--full-name", "-n", "--column", "foo"), git.lastArgs)
  }

  @Test
  fun `test Ggrep passes flags and quoted arguments to git`() {
    configureByText("${c}foo")

    enterCommand("Ggrep -i 'foo bar' -- '*.kt'")

    assertEquals(
      listOf("-c", "core.quotePath=false", "grep", "--no-color", "--full-name", "-n", "--column", "-i", "foo bar", "--", "*.kt"),
      git.lastArgs,
    )
  }

  @Test
  fun `test Ggrep jumps to match in current file`() {
    configureByText(
      """
        |${c}first line
        |second foo line
        |third line
      """.trimMargin()
    )
    git.respondWith("aaa.txt:2:8:second foo line")

    enterCommand("Ggrep foo")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(1, 7)
  }

  @Test
  fun `test Ggrep jumps to match in another file`() {
    configureByText("${c}nothing here")
    addFile("dir/other.txt", "one\ntwo\n  three foo\n")
    git.respondWith("dir/other.txt:3:9:  three foo")

    enterCommand("Ggrep foo")

    assertCurrentFile("other.txt")
    assertCurrentPosition(2, 8)
  }

  @Test
  fun `test Ggrep jumps to first match in git output`() {
    configureByText("${c}nothing here")
    addFile("b.txt", "\nfoo in b\n")
    addFile("c.txt", "foo in c\n")
    git.respondWith("b.txt:2:1:foo in b", "c.txt:1:1:foo in c")

    enterCommand("Ggrep foo")

    assertCurrentFile("b.txt")
    assertCurrentPosition(1, 0)
  }

  @Test
  fun `test Ggrep converts git byte column to character column`() {
    configureByText("${c}zażółć foo")
    git.respondWith("aaa.txt:1:12:zażółć foo")

    enterCommand("Ggrep foo")

    assertCurrentPosition(0, 7)
  }

  @Test
  fun `test Ggrep saves jump location`() {
    configureByText(
      """
        |first line
        |second ${c}line
        |third foo line
      """.trimMargin()
    )
    git.respondWith("aaa.txt:3:7:third foo line")

    enterCommand("Ggrep foo")
    assertCurrentPosition(2, 6)
    typeText("<C-o>")

    assertCurrentPosition(1, 7)
  }

  @Test
  fun `test Ggrep saves jump location before jumping to another file`() {
    configureByText(
      """
        |first line
        |second ${c}line
      """.trimMargin()
    )
    addFile("other.txt", "one\nfoo\n")
    git.respondWith("other.txt:2:1:foo")

    enterCommand("Ggrep foo")
    assertCurrentFile("other.txt")
    typeInCurrentEditor("<C-o>")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(1, 7)
  }

  @Test
  fun `test Ggrep with bang does not jump`() {
    configureByText("${c}nothing here")
    addFile("other.txt", "foo\n")
    git.respondWith("other.txt:1:1:foo")

    enterCommand("Ggrep! foo")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test Ggrep without matches does not jump`() {
    configureByText("nothing ${c}here")
    git.respondWith()

    enterCommand("Ggrep foo")

    assertCurrentFile("aaa.txt")
    assertCurrentPosition(0, 8)
    assertPluginError(false)
  }

  @Test
  fun `test Ggrep reports missing git`() {
    configureByText("${c}foo")
    git.executableFound = false

    enterCommand("Ggrep foo")

    assertPluginError(true)
    assertPluginErrorMessage("fugitive: cannot find 'git' in PATH")
    assertNull(git.lastArgs)
  }

  @Test
  fun `test Ggrep reports missing git with configured path`() {
    configureByText("${c}foo")
    git.executable = "/opt/git/bin/git"
    git.executableFound = false

    enterCommand("Ggrep foo")

    assertPluginError(true)
    assertPluginErrorMessage("fugitive: cannot find '/opt/git/bin/git' in PATH")
  }

  @Test
  fun `test Ggrep reports git that cannot be executed`() {
    configureByText("${c}foo")
    git.canExecute = false

    enterCommand("Ggrep foo")

    assertPluginError(true)
    assertPluginErrorMessage("fugitive: cannot execute Git")
    assertNull(git.lastArgs)
  }

  @Test
  fun `test Ggrep reports file outside repository`() {
    configureByText("${c}foo")
    git.workTree = null

    enterCommand("Ggrep foo")

    assertPluginError(true)
    assertPluginErrorMessage("fugitive: file does not belong to a Git repository")
    assertNull(git.lastArgs)
  }

  @Test
  fun `test Ggrep reports git error`() {
    configureByText("${c}foo")
    git.output = GitOutput(exitCode = 128, stdout = emptyList(), stderr = "fatal: no pattern given\n")

    enterCommand("Ggrep")

    assertPluginError(true)
    assertPluginErrorMessage("fatal: no pattern given")
    assertCurrentPosition(0, 0)
  }

  @Test
  fun `test Ggrep is unknown without extension`() {
    configureByText("${c}foo")
    enterCommand("set nofugitive")

    enterCommand("Ggrep foo")

    assertPluginError(true)
    assertPluginErrorMessage("E492: Not an editor command: Ggrep foo")
  }
}
