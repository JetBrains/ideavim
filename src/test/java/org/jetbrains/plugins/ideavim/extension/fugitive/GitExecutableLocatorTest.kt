/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.fugitive

import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import com.maddyhome.idea.vim.group.git.GitClient
import com.maddyhome.idea.vim.group.git.GitExecutableLocator
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GitExecutableLocatorTest : VimTestCase() {

  @Test
  fun `test failing locator is skipped`() {
    useLocators(FailingLocator(), FixedLocator("/opt/git/bin/git"))

    assertEquals("/opt/git/bin/git", GitClient.getInstance(fixture.project).executable)
  }

  @Test
  fun `test git on PATH is used when every locator fails`() {
    useLocators(FailingLocator())

    assertEquals("git", GitClient.getInstance(fixture.project).executable)
  }

  private fun useLocators(vararg locators: GitExecutableLocator) {
    ExtensionTestUtil.maskExtensions(GitExecutableLocator.EP_NAME, locators.toList(), fixture.testRootDisposable)
  }

  private class FailingLocator : GitExecutableLocator {
    override fun findExecutable(project: Project): String = throw ClassCastException("GitExecutableManager")
  }

  private class FixedLocator(private val path: String) : GitExecutableLocator {
    override fun findExecutable(project: Project): String = path
  }
}
