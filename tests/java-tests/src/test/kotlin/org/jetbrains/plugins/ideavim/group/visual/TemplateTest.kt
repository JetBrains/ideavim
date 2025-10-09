/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.group.visual

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil.doInlineRename
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.jetbrains.plugins.ideavim.assertModeDoesNotChange
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.jetbrains.plugins.ideavim.waitUntilSelectionUpdated
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertNotNull
import kotlin.test.fail

@TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
class TemplateTest : VimJavaTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    TemplateManagerImpl.setTemplateTesting(fixture.testRootDisposable)
  }

  @Test
  fun `test accept lookup in inline rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    startRenaming(VariableInplaceRenameHandler())
    val lookupValue = fixture.lookupElementStrings?.get(0) ?: fail()
    ApplicationManager.getApplication().invokeAndWait {
      fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    }
    assertState(Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${lookupValue} = 5;
        |  }
        |}
      """.trimMargin()
    )
  }

  @Test
  fun `test typing with inline rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )

    typeText("myNewVar", "<CR>")

    assertState(Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateFinished()
  }

  @Test
  fun `test typing at start of inline rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }

    // <Left> will move the caret to the start of the selection, removing it as well
    typeText("<Left>")
    assertState(Mode.INSERT)
    typeText("pre" + "<CR>")

    // Accepting the rename switches to Normal mode, moving the caret back one character
    assertState(Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int pr${c}emyVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateFinished()
  }


  @Test
  fun `test motion left at start of rename symbol removes Select mode without cancelling rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int ${c}myVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}${c}myVar${se} = 5;
        |  }
        |}
      """.trimMargin()
    )

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }

    typeText("<Left>")

    // <Left> will move the caret to the start of the selection, removing it as well. We're still renaming.
    assertState(Mode.INSERT)
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${c}myVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test motion right while renaming removes Select mode without cancelling rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }

    typeText("<Right>")

    // <Right> will move the caret to the end of the selection, removing it as well. We're still renaming.
    assertState(Mode.INSERT)
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myVar${c} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test motion right at end of rename symbol removes Select mode without cancelling rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int myVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}myVa${c}r${se} = 5;
        |  }
        |}
      """.trimMargin()
    )

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }

    typeText("<Right>")

    // <Right> will move the caret to the end of the selection, removing it as well. We're still renaming.
    assertState(Mode.INSERT)
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myVar${c} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test Escape in Select mode removes selection without cancelling rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )

    typeText("<ESC>")

    // <Escape> will remove the selection, switching us from Select to Normal. We're still renaming.
    assertState(Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test Escape after typing in Select mode switches to Normal mode without cancelling rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )

    typeText("Hello", "<ESC>")

    assertState(Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int Hell${c}o = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with keep option remains in Normal mode when starting rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.NORMAL())
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with keep option remains in Insert mode when starting rename`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    typeText("i")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.INSERT)
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with keep option remains in Visual mode when starting`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    typeText("vll")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${s}Va${c}r${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with select option enters Select mode from Normal`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=select")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with select option enters Select mode from Insert`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=select")
    typeText("i")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with select option remains in Visual mode when starting from Visual`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=select")
    typeText("vll")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${s}Va${c}r${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with select option remains in Select mode when starting from Select`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=select")
    typeText("vll<C-G>")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${s}Var${c}${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with visual option enters Visual mode from Normal`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with visual option enters Visual mode from Insert`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    typeText("i")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int ${s}my${c}Var${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with visual option remains in Visual mode when starting from Visual`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    typeText("vll")
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${s}Va${c}r${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with visual option remains in Select mode when starting from Select`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    typeText(injector.parser.parseKeys("vll<C-G>"))
    startRenaming(VariableInplaceRenameHandler())
    assertModeDoesNotChange(fixture.editor, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${s}Var${c}${se} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertTemplateActive()
  }

  @Test
  fun `test inline rename with keep option ends in Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    ApplicationManager.getApplication().invokeAndWait {
      doInlineRename(VariableInplaceRenameHandler(), "myNewVar", fixture)
    }

    // Rename doesn't change the caret position
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}NewVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with keep option ends in Insert mode from Insert mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.NORMAL())
    typeText("viw", "c", "myNewVar", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVar${c} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.INSERT)
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with keep option ends in Normal mode from Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.NORMAL())
    typeText("viw", "c", "myNewVar", "<Esc>", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with keep option ends in Visual mode from Visual mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=keep")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.NORMAL())
    typeText("viw", "c", "myNewVar", "<Esc>", "viw", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with select option ends in Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    ApplicationManager.getApplication().invokeAndWait {
      doInlineRename(VariableInplaceRenameHandler(), "myNewVar", fixture)
    }

    // Rename doesn't change the caret position, but changing from Select to Normal mode does
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int m${c}yNewVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with select option ends in Normal mode from Insert mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    typeText("myNewVar", "<CR>")  // Typing first char switches to Insert

    // Insert ends after the symbol, then switching to Normal moves back a char
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with select option ends in Normal mode from Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    typeText("myNewVar", "<Esc>", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with select option ends in Normal mode from Visual mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    typeText("myNewVar", "<Esc>", "viw", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with visual option ends in Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    ApplicationManager.getApplication().invokeAndWait {
      doInlineRename(VariableInplaceRenameHandler(), "myNewVar", fixture)
    }

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}NewVar = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with visual option ends in Normal mode from Insert mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    typeText("c", "myNewVar", "<CR>") // Visual -> Change -> Type -> Accept

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVar${c} = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with visual option ends in Normal mode from Normal mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    typeText("c", "myNewVar", "<Esc>", "<CR>")  // Visual -> Change -> Type -> Normal -> Accept

    // Switching from Insert to Normal moves the caret back one char
    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  @Test
  fun `test inline rename with visual option ends in Normal mode from Visual mode`() {
    configureByJavaText(
      """
        |class Hello {
        |  public static void main() {
        |    int my${c}Var = 5;
        |  }
        |}
      """.trimMargin()
    )
    enterCommand("set idearefactormode=visual")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    typeText("c", "myNewVar", "<Esc>", "viw", "<CR>")

    assertState(
      """
        |class Hello {
        |  public static void main() {
        |    int myNewVa${c}r = 5;
        |  }
        |}
      """.trimMargin()
    )
    assertState(Mode.NORMAL())
    assertTemplateFinished()
  }

  // 'idearefactormode'=select
  @Test
  fun `test template with select option enters Select mode for each variable and ends in Insert mode`() {
    configureByJavaText("")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"$V1$ $V2$ = $V3$;")
    template.addVariable("V1", "", "\"var\"", true)
    template.addVariable("V2", "", "\"foo\"", true)
    template.addVariable("V3", "", "\"239\"", true)

    startTemplate(manager, template)

    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState("${s}var${c}${se} foo = 239;")

    moveToNextVariable()
    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState("var ${s}foo${c}${se} = 239;")

    typeText("<Esc>")
    assertMode(Mode.NORMAL())

    moveToNextVariable()
    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState("var foo = ${s}239${c}${se};")

    moveToNextVariable()
    assertMode(Mode.INSERT)
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  @Test
  fun `test template with select option enters Insert mode for variable with no default text to select`() {
    configureByJavaText("")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState("var ${s}foo${c}${se} = ;")

    moveToNextVariable()
    assertMode(Mode.INSERT)
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with select option enters Insert mode when no selection change when moving`() {
    configureByJavaText("")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState("var ${s}foo${c}${se} = ;")

    // Leave Select mode and tab to an empty next variable. No change in selection
    typeText("<Esc>")
    assertMode(Mode.NORMAL())
    moveToNextVariable()

    assertMode(Mode.INSERT)
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with select option ends in Insert mode when no selection change when moving to end`() {
    configureByJavaText("")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"239\"", true)

    startTemplate(manager, template)
    moveToNextVariable()  // V1 -> V2
    typeText("<Esc>") // Select -> Normal
    moveToNextVariable()  // V2 -> End

    assertMode(Mode.INSERT)
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  // 'idearefactormode'=visual
  @Test
  fun `test template with visual option enters Visual mode for each variable and ends in Normal mode`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=visual")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"$V1$ $V2$ = $V3$;")
    template.addVariable("V1", "", "\"var\"", true)
    template.addVariable("V2", "", "\"foo\"", true)
    template.addVariable("V3", "", "\"239\"", true)

    startTemplate(manager, template)

    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("${s}va${c}r${se} foo = 239;")

    moveToNextVariable()
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("var ${s}fo${c}o${se} = 239;")

    typeText("<Esc>")
    assertMode(Mode.NORMAL())

    moveToNextVariable()
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("var foo = ${s}23${c}9${se};")

    moveToNextVariable()
    assertMode(Mode.NORMAL())
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  @Test
  fun `test template with visual option enters Normal mode for variable with no default text to select`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=visual")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("var ${s}fo${c}o${se} = ;")

    moveToNextVariable()
    assertMode(Mode.NORMAL())
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with visual option enters Normal mode when no selection change when moving`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=visual")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("var ${s}fo${c}o${se} = ;")

    // Leave Visual mode and tab to an empty next variable. No change in selection
    typeText("<Esc>")
    assertMode(Mode.NORMAL())
    moveToNextVariable()

    assertMode(Mode.NORMAL())
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with visual option ends in Normal mode when no selection change when moving to end`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=visual")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"239\"", true)

    startTemplate(manager, template)
    moveToNextVariable()  // V1 -> V2
    typeText("<Esc>") // Visual -> Normal
    moveToNextVariable()  // V2 -> End

    assertMode(Mode.NORMAL())
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  // 'idearefactormode'=keep
  @Test
  fun `test template with keep option keeps current mode for each variable and end`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=keep")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"$V1$ $V2$ = $V3$;")
    template.addVariable("V1", "", "\"var\"", true)
    template.addVariable("V2", "", "\"foo\"", true)
    template.addVariable("V3", "", "\"239\"", true)

    startTemplate(manager, template)

    assertMode(Mode.NORMAL())
    assertState("${c}var foo = 239;")

    moveToNextVariable()
    assertMode(Mode.NORMAL())
    assertState("var ${c}foo = 239;")

    typeText("i")
    assertMode(Mode.INSERT)

    moveToNextVariable()
    assertMode(Mode.INSERT)
    assertState("var foo = ${c}239;")

    typeText("<Esc>")
    assertMode(Mode.NORMAL())

    // When hitting Escape from Insert, we move back a character. This puts us outside the editing segment
    // TODO: Should we prevent this? How?
    // VimChangeGroupBase.repeatInsert. We'd need to use VimTemplateManager and add the current variable range
    assertState("var foo =${c} 239;")
    typeText("l")

    moveToNextVariable()
    assertMode(Mode.NORMAL())
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  @Test
  fun `test template with keep option keeps current mode for variable with no default text to select`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=keep")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.NORMAL())
    assertState("var ${c}foo = ;")

    moveToNextVariable()
    assertMode(Mode.NORMAL())
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with keep option keeps current mode when no selection change when moving`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=keep")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"\"", true)

    startTemplate(manager, template)

    assertMode(Mode.NORMAL())
    assertState("var ${c}foo = ;")

    typeText("i")
    assertMode(Mode.INSERT)
    moveToNextVariable()

    assertMode(Mode.INSERT)
    assertState("var foo = ${c};")

    assertTemplateActive()
  }

  @Test
  fun `test template with keep option ends in current mode when no selection change when moving to end`() {
    configureByJavaText("")
    enterCommand("set idearefactormode=keep")

    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", $$"var $V1$ = $V2$;")
    template.addVariable("V1", "", "\"foo\"", true)
    template.addVariable("V2", "", "\"239\"", true)

    startTemplate(manager, template)
    moveToNextVariable()  // V1 -> V2
    typeText("v") // Normal -> Visual
    moveToNextVariable()  // V2 -> End

    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState("var foo = 239;${c}")
    assertTemplateFinished()
  }

  private fun startRenaming(handler: VariableInplaceRenameHandler) {
    val editor = if (fixture.editor is EditorWindow) (fixture.editor as EditorWindow).delegate else fixture.editor

    ApplicationManager.getApplication().invokeAndWait {
      var elementToRename: PsiElement? = null
      ApplicationManager.getApplication().runReadAction {
        elementToRename = fixture.elementAtCaret
      }
      ApplicationManager.getApplication().runWriteAction {
        handler.doRename(elementToRename!!, editor, dataContext)
      }
    }
  }

  private fun startTemplate(manager: TemplateManager, template: Template) {
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        manager.startTemplate(fixture.editor, template)
      }
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    // The selection (and Vim mode) is not guaranteed to be updated. We've just waited long enough for it to be change
    waitUntilSelectionUpdated(fixture.editor)
  }

  private fun moveToNextVariable() {
    typeText("<CR>")
    waitUntilSelectionUpdated(fixture.editor)
  }

  private fun assertTemplateActive() =
    assertNotNull(TemplateManagerImpl.getTemplateState(fixture.editor))

  private fun assertTemplateFinished() =
    assertNull(TemplateManagerImpl.getTemplateState(fixture.editor))

  private val dataContext
    get() = DataManager.getInstance().getDataContext(fixture.editor.component)
}
