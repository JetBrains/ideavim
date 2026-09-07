/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Test

/**
 * While we are in insert mode the IDE changes the document behind our back, and those changes are recorded in the
 * strokes that `.` replays. Adding an import is such a change - the daemon, an Alt+Enter fix and the completion lookup
 * all do it - and `.` has to replay only what the user typed, not the import.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "Adding imports while typing has no Neovim equivalent",
)
class RepeatWithAutoImportTest : VimJavaTestCase() {

  private val twoSetFields = """
        |import java.util.Set;
        |
        |class Foo {
        |    ${c}Set<String> foo;
        |    Set<String> bar;
        |}
  """.trimMargin()

  private val twoSetLocals = """
        |import java.util.HashSet;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    ${c}Set<String> foo = new HashSet<String>();
        |    Set<String> bar = new HashSet<String>();
        |  }
        |}
  """.trimMargin()

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createLightFixtureBuilder(WITH_REAL_JDK, "IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture, LightTempDirTestFixtureImpl(true))
  }

  @Test
  fun `test repeating a change that auto-imported a class does not replay the import`() {
    configureByJavaText(twoSetFields)

    // We are still in insert mode when the IDE adds `import java.util.List;` at the top of the file
    typeText("cf>", "List<lt>String>")
    addImportWhileTyping("import java.util.List;")
    typeText("<Esc>")

    assertState(
      """
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |    List<String${c}> foo;
        |    Set<String> bar;
        |}
      """.trimMargin(),
    )

    typeText("j", "0", "w", ".")

    assertState(
      """
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |    List<String> foo;
        |    List<Strin${c}g> bar;
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test repeating a change whose import landed while the user kept typing`() {
    configureByJavaText(twoSetLocals)

    // The import lands in the middle of the insert, so the offsets recorded for the rest of it have to survive the
    // text above the caret growing
    typeText("cf;", "List<lt>String> f")
    addImportWhileTyping("import java.util.List;")
    typeText("oo = new HashSet<lt>String>();")
    typeText("<Esc>")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    List<String> foo = new HashSet<String>()${c};
        |    Set<String> bar = new HashSet<String>();
        |  }
        |}
      """.trimMargin(),
    )

    typeText("j", "0", "w", ".")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    List<String> foo = new HashSet<String>();
        |    List<String> foo = new HashSet<String>()${c};
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test repeating a change made with backspaces after an import landed`() {
    configureByJavaText(twoSetLocals)

    // Backspaces and caret motions end up in the strokes next to the typed text
    typeText("ea", "<BS><BS><BS>", "List")
    addImportWhileTyping("import java.util.List;")
    typeText("<Esc>")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    Lis${c}t<String> foo = new HashSet<String>();
        |    Set<String> bar = new HashSet<String>();
        |  }
        |}
      """.trimMargin(),
    )

    // `.` repeats `a`, so the caret has to be where the original append started: at the end of the word
    typeText("j", "0", "e", ".")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    List<String> foo = new HashSet<String>();
        |    Lis${c}t<String> bar = new HashSet<String>();
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test repeating a change completed from the lookup does not replay the qualified name round trip`() {
    configureByJavaText(twoSetLocals)

    // Picking `List` from the popup inserts `java.util.List`, adds the import and then shortens the name back to
    // `List` - all of it while we are recording strokes
    typeText("cw", "Lis")
    completeBasic()
    finishLookup()
    typeText("<Esc>")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    Lis${c}t<String> foo = new HashSet<String>();
        |    Set<String> bar = new HashSet<String>();
        |  }
        |}
      """.trimMargin(),
    )

    typeText("j", "0", "w", ".")

    assertState(
      """
        |import java.util.HashSet;
        |import java.util.List;
        |import java.util.Set;
        |
        |class Foo {
        |  void test() {
        |    List<String> foo = new HashSet<String>();
        |    Lis${c}t<String> bar = new HashSet<String>();
        |  }
        |}
      """.trimMargin(),
    )
  }

  /**
   * Adds the import into its sorted place while we are in insert mode, which is the document change "add unambiguous
   * imports on the fly" and the Alt+Enter fix both make. We deliberately don't run the highlighting passes to get a
   * real one: the daemon's caches hold on to the light project through soft references, and the leak checks on CI fail
   * because of it. The lookup test below covers a genuinely IDE-added import.
   */
  private fun addImportWhileTyping(importStatement: String) {
    ApplicationManager.getApplication().invokeAndWait {
      val document = fixture.editor.document
      val importLines = document.text.lines().withIndex().filter { it.value.startsWith("import ") }
      val line = importLines.firstOrNull { it.value > importStatement }?.index ?: (importLines.last().index + 1)
      WriteCommandAction.runWriteCommandAction(fixture.project) {
        document.insertString(document.getLineStartOffset(line), "$importStatement\n")
      }
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  private fun completeBasic() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.completeBasic()
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  /**
   * Accepts the selected lookup item the way pressing Enter does. We must not send `<C-Y>` instead: that is a Vim
   * command of its own (insert the character above the caret), and it would become the command `.` repeats.
   */
  private fun finishLookup() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  companion object {
    /**
     * The default light project descriptor has no JDK, so `java.util.List` wouldn't resolve and there would be nothing
     * to import. Shared between the tests of this class, so they share one light project.
     */
    private val WITH_REAL_JDK = object : LightProjectDescriptor() {
      override fun getSdk(): Sdk = JavaSdk.getInstance().createJdk("Test JDK", System.getProperty("java.home"), false)
    }
  }
}
