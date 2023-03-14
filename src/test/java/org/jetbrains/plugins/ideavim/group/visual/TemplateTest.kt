/*
 * Copyright 2003-2023 The IdeaVim authors
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
import com.intellij.openapi.editor.Editor
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil.doInlineRename
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.VimStateMachine
import com.maddyhome.idea.vim.group.IjOptionConstants
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.helper.inSelectMode
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.listener.VimListenerManager
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestIjOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
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
class TemplateTest : VimTestCase() {

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
    doInlineRename(VariableInplaceRenameHandler(), "myNewVar", fixture)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("myNewVar" + "<CR>"))

    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.VISUAL)
    assertState(VimStateMachine.Mode.VISUAL, VimStateMachine.SubMode.VISUAL_CHARACTER)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    LookupManager.hideActiveLookup(fixture.project)
    typeText(injector.parser.parseKeys("<Left>"))
    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
    typeText(injector.parser.parseKeys("pre" + "<CR>"))

    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    LookupManager.hideActiveLookup(fixture.project)
    typeText(injector.parser.parseKeys("<Right>"))
    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    LookupManager.hideActiveLookup(fixture.project)
    typeText(injector.parser.parseKeys("<Left>"))
    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    LookupManager.hideActiveLookup(fixture.project)
    typeText(injector.parser.parseKeys("<Right>"))
    assertState(VimStateMachine.Mode.INSERT, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("<ESC>"))

    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
    assertState(VimStateMachine.Mode.SELECT, VimStateMachine.SubMode.VISUAL_CHARACTER)

    typeText(injector.parser.parseKeys("Hello" + "<ESC>"))

    assertState(VimStateMachine.Mode.COMMAND, VimStateMachine.SubMode.NONE)
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
    assertDoesntChange { fixture.editor.inNormalMode }
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.SELECT)
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
    assertDoesntChange { fixture.editor.inSelectMode }
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.VISUAL)
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
    waitAndAssertMode(fixture, VimStateMachine.Mode.VISUAL)
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
    assertDoesntChange { fixture.editor.inSelectMode }
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

    manager.startTemplate(fixture.editor, template)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    assertMode(VimStateMachine.Mode.COMMAND)
    assertOffset(2)
    typeText(injector.parser.parseKeys("<CR>"))
    assertMode(VimStateMachine.Mode.COMMAND)
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
    fixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
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
    VimListenerManager.EditorListeners.add(editor)

    handler.doRename(fixture.elementAtCaret, editor, dataContext)
    return editor
  }

  private val dataContext
    get() = DataManager.getInstance().getDataContext(fixture.editor.component)
}
