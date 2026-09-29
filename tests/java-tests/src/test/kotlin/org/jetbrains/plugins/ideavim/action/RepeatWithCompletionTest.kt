/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.keys
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * `.` and the code completion popup.
 *
 * `.` replays the keys of the last change, and accepting a completion item is not a key the user pressed - it is an
 * IDE action. IdeaVim records the keys that would have produced the same text instead (backspaces over the typed
 * prefix, the completed text, then the caret motions back into it), which is the same hook that lets a macro replay a
 * completion - see `recordSynthesizedKeys` in `IdeaSpecifics`.
 *
 * `.` then repeats the completion correctly only when both of these hold:
 *
 * 1. the item was accepted through the action system, so the listener fires and the text is recorded;
 * 2. the key that triggered the acceptance is harmless when replayed with no popup open.
 *
 * | accepted by                                   | (1) | (2) | `.`    |
 * |-----------------------------------------------|-----|-----|--------|
 * | `<Enter>` / `<Tab>` (in `'idealookupkeys'`)   | yes | yes - the key never reaches IdeaVim | works |
 * | double-click on the item                      | yes | yes - there is no key | works |
 * | mapping to `<Action>(EditorChooseLookupItem)`  | yes | yes - the action is disabled without a lookup | works |
 * | `<C-Y>`, or a mapping to it                   | yes | yes - the key is dropped | works |
 * | `<C-E>` (dismiss)                             | n/a | yes - the key is dropped | works |
 * | the only candidate, auto-inserted             | no  | yes | broken |
 *
 * `<C-Y>` used to fail both: IdeaVim handles it itself, and it reached the lookup through
 * `IjVimLookupManager.accept`, which invoked the action handler directly and never fired the listener. It now goes
 * through the action system, and the key itself is dropped from the recording by `VimRedoBuffer.dropLastKey`.
 *
 * Two of the rows are not tested here, for harness reasons rather than lack of interest:
 * - A literal `<Enter>` cannot be driven: `VimTestCase.typeTextViaIde` turns it into `performEditorAction`
 *   (`"EditorEnter"`), which does not reproduce the IDE's Enter-to-`ChooseItemAction` binding.
 * - A double-click is routed by the platform through `ActionUtil.performAction(ACTION_CHOOSE_LOOKUP_ITEM)` -
 *   `LookupImpl` does this deliberately, "to avoid the difference between mouse and shortcut complete".
 *
 * Both therefore end up in the same action as [acceptThroughActionSystem], which is what the first test drives.
 */
@TestWithoutNeovim(
  reason = SkipNeovimReason.SEE_DESCRIPTION,
  description = "The IntelliJ completion popup has no Neovim equivalent",
)
class RepeatWithCompletionTest : VimJavaTestCase() {

  /** Three members sharing the `fooB`/`foo` prefix, so completing really opens a lookup instead of auto-inserting. */
  private val twoStatements = """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    ${c}xx;
        |    xx;
        |  }
        |}
  """.trimMargin()

  private val singleCandidate = """
        |class Foo {
        |  void fooBar() {}
        |
        |  void test() {
        |    ${c}xx;
        |    xx;
        |  }
        |}
  """.trimMargin()

  override fun createFixture(factory: IdeaTestFixtureFactory): CodeInsightTestFixture {
    val fixture = factory.createLightFixtureBuilder(WITH_REAL_JDK, "IdeaVim").fixture
    return factory.createCodeInsightFixture(fixture, LightTempDirTestFixtureImpl(true))
  }

  @Test
  fun `test dot replays a completion accepted from the lookup`() {
    configureByJavaText(twoStatements)
    changeWordCompletingTo("fooB", acceptWith = ::acceptThroughActionSystem)

    // The caret ends up inside the parens rather than after them, where the original change left it: the recorded
    // keys put it back with `<Left>`, and the completion has already moved it once
    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot inserts the completed text where the prefix completes to nothing`() {
    // `fooB` resolves to nothing in `Other`, so nothing could be completed there. `.` replays the text that was
    // completed, exactly as Vim's redo replays the characters an insert produced rather than re-running completion
    configureByJavaText(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    ${c}xx;
        |  }
        |}
        |
        |class Other {
        |  void test() {
        |    xx;
        |  }
        |}
      """.trimMargin(),
    )
    typeText("cw", "fooB")
    completeBasic()
    acceptThroughActionSystem()
    typeText("<Esc>")
    typeText("/xx<CR>", ".")

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |  }
        |}
        |
        |class Other {
        |  void test() {
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot replays a completion accepted through a mapping to the IDE action`() {
    configureByJavaText(twoStatements)
    enterCommand("imap <C-K> <Action>(EditorChooseLookupItem)")
    changeWordCompletingTo("fooB", acceptWith = { typeText("<C-K>") })

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot replays a completion accepted through a mapping to Ctrl-Y`() {
    configureByJavaText(twoStatements)
    injector.keyGroup.putKeyMapping(MappingMode.I, keys("<C-K>"), MappingOwner.IdeaVim.Other, keys("<C-Y>"), false)
    changeWordCompletingTo("fooB", acceptWith = { typeText("<C-K>") })

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot replays a completion accepted with Ctrl-Y`() {
    configureByJavaText(twoStatements)
    changeWordCompletingTo("fooB", acceptWith = { typeText("<C-Y>") })

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Test
  fun `test dot after dismissing the lookup with Ctrl-E keeps only the typed prefix`() {
    configureByJavaText(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    ${c}xx;
        |    xx;
        |    zzzzzzzzzzzzzzzzzzzzzz;
        |  }
        |}
      """.trimMargin(),
    )
    typeText("cw", "fooB")
    completeBasic()
    typeText("<C-E>")
    typeText("<Esc>")
    typeText("j", "0", "w", ".")

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooB;
        |    foo${c}B;
        |    zzzzzzzzzzzzzzzzzzzzzz;
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Disabled(
    "Broken: `.` inserts only the typed prefix `fooB`. With a single candidate the completion inserts it straight " +
      "away and no lookup is ever shown, so there is no ChooseItemAction and nothing records the text. This is the " +
      "same gap for any IDE edit made during an insert that is neither a keystroke nor a completion the listener " +
      "sees.",
  )
  @Test
  fun `test dot replays a completion that was auto inserted as the only candidate`() {
    configureByJavaText(singleCandidate)
    typeText("cw", "fooB")
    completeBasic()
    typeText("<Esc>")
    typeText("j", "0", "w", ".")

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  @Disabled(
    "A macro has the same bug `.` used to have, and the drop that fixed `.` does not reach it. The register ends up " +
      "as `cwfooB<BS><BS><BS><BS>fooBar()<C-Y><Esc>`: the key is recorded in " +
      "`KeyHandler.finishedCommandPreparation`, *after* the command has run, so at the point " +
      "`VimRedoBuffer.dropLastKey` is called it is not in the register yet and there is nothing to drop. Playing the " +
      "macro back therefore inserts one spurious character from the line above - `fooBar();;`. Fixing it needs the " +
      "recording of the current key suppressed for both sinks, rather than dropped after the fact.",
  )
  @Test
  fun `test a macro replays a completion accepted with Ctrl-Y`() {
    configureByJavaText(twoStatements)
    typeText("qa", "cw", "fooB")
    completeBasic()
    typeText("<C-Y>", "<Esc>", "q")
    typeText("j", "0", "w", "@a")

    assertState(
      """
        |class Foo {
        |  void fooBar() {}
        |  void fooBaz() {}
        |  void fooLong() {}
        |
        |  void test() {
        |    fooBar();
        |    fooBar(${c});
        |  }
        |}
      """.trimMargin(),
    )
  }

  /** `cw` on the first `xx`, complete [prefix], accept, leave Insert, then repeat on the second `xx`. */
  private fun changeWordCompletingTo(prefix: String, acceptWith: () -> Unit) {
    typeText("cw", prefix)
    completeBasic()
    acceptWith()
    typeText("<Esc>")
    typeText("j", "0", "w", ".")
  }

  private fun completeBasic() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.completeBasic()
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  /** Accepts the selected item the way `<Tab>` and `<Enter>` do, through the action the listener is attached to. */
  private fun acceptThroughActionSystem() {
    ApplicationManager.getApplication().invokeAndWait {
      fixture.performEditorAction(IdeActions.ACTION_CHOOSE_LOOKUP_ITEM)
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }
}
