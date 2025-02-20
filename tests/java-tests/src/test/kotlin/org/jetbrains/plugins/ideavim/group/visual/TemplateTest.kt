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
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil.doInlineRename
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inNormalMode
import com.maddyhome.idea.vim.state.mode.inSelectMode
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimJavaTestCase
import org.jetbrains.plugins.ideavim.assertDoesntChange
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

/**
 * @author Alex Plate
 */
@TraceOptions(TestIjOptionConstants.idearefactormode)
class TemplateTest : VimJavaTestCase() {

  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    TemplateManagerImpl.setTemplateTesting(fixture.testRootDisposable)
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  fun `test simple rename`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    ApplicationManager.getApplication().invokeAndWait {
      doInlineRename(VariableInplaceRenameHandler(), "myNewVar", fixture)
    }
    assertState(
      """
            class Hello {
                public static void main() {
                    int my${c}NewVar = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test type rename`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    typeText(injector.parser.parseKeys("myNewVar" + "<CR>"))

    assertState(Mode.INSERT)
    assertState(
      """
            class Hello {
                public static void main() {
                    int myNewVar${c} = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test selectmode without template`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    enterCommand("set idearefactormode=visual")
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertState(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    // Disable template
    typeText(injector.parser.parseKeys("<CR>"))
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test prepend`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }
    typeText(injector.parser.parseKeys("<Left>"))
    assertState(Mode.INSERT)
    typeText(injector.parser.parseKeys("pre" + "<CR>"))

    assertState(Mode.INSERT)
    assertState(
      """
            class Hello {
                public static void main() {
                    int pre${c}myVar = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test motion right`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }
    typeText(injector.parser.parseKeys("<Right>"))
    assertState(Mode.INSERT)
    assertState(
      """
            class Hello {
                public static void main() {
                    int myVar${c} = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test motion left on age`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int ${c}myVar = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }
    typeText(injector.parser.parseKeys("<Left>"))
    assertState(Mode.INSERT)
    assertState(
      """
            class Hello {
                public static void main() {
                    int ${c}myVar = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test motion right on age`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int myVa${c}r = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      LookupManager.hideActiveLookup(fixture.project)
    }
    typeText(injector.parser.parseKeys("<Right>"))
    assertState(Mode.INSERT)
    assertState(
      """
            class Hello {
                public static void main() {
                    int myVar${c} = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test escape`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    typeText(injector.parser.parseKeys("<ESC>"))

    assertState(Mode.NORMAL())
    assertState(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(VimOption(TestIjOptionConstants.idearefactormode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test escape after typing`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
    assertState(Mode.SELECT(SelectionType.CHARACTER_WISE))

    typeText(injector.parser.parseKeys("Hello" + "<ESC>"))

    assertState(Mode.NORMAL())
    assertState(
      """
            class Hello {
                public static void main() {
                    int Hell${c}o = 5;
                }
            }
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_keep]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template in normal mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.vim.inNormalMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_keep]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test save mode for insert mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("i"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.inInsertMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_keep]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test save mode for visual mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vll"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.inVisualMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_select]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to select in normal mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_select]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to select in insert mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("i"))
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_select]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to select in visual mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vll"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.inVisualMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_select]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to select in select mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vll<C-G>"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.vim.inSelectMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_visual]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to visual in normal mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_visual]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to visual in insert mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("i"))
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_visual]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to visual in visual mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vll"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.inVisualMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_visual]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template to visual in select mode`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("vll<C-G>"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { fixture.editor.vim.inSelectMode }
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_keep]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template with multiple times`() {
    configureByJavaText(c)
    val manager = TemplateManager.getInstance(fixture.project)
    val template = manager.createTemplate("vn", "user", "\$V1$ var = \$V2$;")
    template.addVariable("V1", "", "\"123\"", true)
    template.addVariable("V2", "", "\"239\"", true)

    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        manager.startTemplate(fixture.editor, template)
      }
      PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
    }

    assertMode(Mode.NORMAL())
    assertOffset(2)
    typeText(injector.parser.parseKeys("<CR>"))
    assertMode(Mode.NORMAL())
    assertOffset(12)
    typeText(injector.parser.parseKeys("<CR>"))
    kotlin.test.assertNull(TemplateManagerImpl.getTemplateState(fixture.editor))
  }

  @OptionTest(
    VimOption(TestIjOptionConstants.idearefactormode, limitedValues = [IjOptionConstants.idearefactormode_keep]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.TEMPLATES)
  fun `test template with lookup`() {
    configureByJavaText(
      """
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
      """.trimIndent(),
    )
    startRenaming(VariableInplaceRenameHandler())
    val lookupValue = fixture.lookupElementStrings?.get(0) ?: kotlin.test.fail()
    ApplicationManager.getApplication().invokeAndWait {
      fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
    }
    assertState(
      """
            class Hello {
                public static void main() {
                    int $lookupValue = 5;
                }
            }
      """.trimIndent(),
    )
  }

  private fun startRenaming(handler: VariableInplaceRenameHandler): Editor {
    val editor = if (fixture.editor is EditorWindow) (fixture.editor as EditorWindow).delegate else fixture.editor

    var elementToRename: PsiElement? = null
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runReadAction {
        elementToRename = fixture.elementAtCaret
      }
      ApplicationManager.getApplication().runWriteAction {
        handler.doRename(elementToRename!!, editor, dataContext)
      }
    }
    return editor
  }

  private val dataContext
    get() = DataManager.getInstance().getDataContext(fixture.editor.component)
}
