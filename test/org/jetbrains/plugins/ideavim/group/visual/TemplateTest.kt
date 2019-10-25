/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.group.visual

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.notification.EventLog
import com.intellij.openapi.editor.Editor
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil.doInlineRename
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.inNormalMode
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SaveModeFor
import com.maddyhome.idea.vim.option.SelectModeOptionData
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType
import org.jetbrains.plugins.ideavim.assertDoesntChange
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.jetbrains.plugins.ideavim.waitAndAssertMode

/**
 * @author Alex Plate
 */
class TemplateTest : VimOptionTestCase(SaveModeFor.name) {

  override fun setUp() {
    super.setUp()
    // TODO: 24.10.2019 [VERSION UPDATE] 2019.1
    @Suppress("DEPRECATION")
    TemplateManagerImpl.setTemplateTesting(myFixture.project, myFixture.testRootDisposable)
  }

  @VimOptionDefaultAll
  fun `test simple rename`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    doInlineRename(VariableInplaceRenameHandler(), "myNewVar", myFixture)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int my${c}NewVar = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test type rename`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("myNewVar", "<CR>"))

    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int myNewVar${c} = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test selectmode without template`() {
    OptionsManager.selectmode.remove(SelectModeOptionData.template)
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.VISUAL)
    assertState(CommandState.Mode.VISUAL, CommandState.SubMode.VISUAL_CHARACTER)
    // Disable template
    typeText(parseKeys("<CR>"))
  }

  @VimOptionDefaultAll
  fun `test prepend`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("<Left>"))
    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    typeText(parseKeys("pre", "<CR>"))

    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int pre${c}myVar = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test motion right`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("<Right>"))
    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int myVar${c} = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test motion left on age`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int ${c}myVar = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("<Left>"))
    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int ${c}myVar = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test motion right on age`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int myVa${c}r = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("<Right>"))
    assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int myVar${c} = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test escape`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("<ESC>"))

    assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test escape after typing`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
    assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

    typeText(parseKeys("Hello", "<ESC>"))

    assertState(CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
    myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int Hell${c}o = 5;
                }
            }
        """.trimIndent())
  }

  @VimOptionDefaultAll
  fun `test notification on first time`() {
    EventLog.markAllAsRead(myFixture.project)
    VimPlugin.getVimState().isTemplateInSelectModeNotified = false
    OptionsManager.selectmode.set("")

    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    typeText(parseKeys("<CR>"))

    waitAndAssert(5000) {
      val notifications = EventLog.getLogModel(myFixture.project).notifications
      notifications.isNotEmpty() && notifications.last().title == NotificationService.IDEAVIM_NOTIFICATION_TITLE
    }
    val notification = EventLog.getLogModel(myFixture.project).notifications.last()

    assertEquals(NotificationService.IDEAVIM_NOTIFICATION_TITLE, notification.title)
    assertTrue(SelectModeOptionData.name in notification.content)
    assertEquals(3, notification.actions.size)
  }

  @VimOptionTestConfiguration(VimTestOption(SaveModeFor.name, VimTestOptionType.LIST, ["template"]))
  fun `test template in normal mode`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { myFixture.editor.inNormalMode }
  }

  @VimOptionTestConfiguration(VimTestOption(SaveModeFor.name, VimTestOptionType.LIST, ["i-template"]))
  fun `test save mode for different mode`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    startRenaming(VariableInplaceRenameHandler())
    waitAndAssertMode(myFixture, CommandState.Mode.SELECT)
  }

  @VimOptionTestConfiguration(VimTestOption(SaveModeFor.name, VimTestOptionType.LIST, ["i-template"]))
  fun `test save mode for insert mode`() {
    configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
    typeText(parseKeys("i"))
    startRenaming(VariableInplaceRenameHandler())
    assertDoesntChange { myFixture.editor.inInsertMode }
  }

  @VimOptionTestConfiguration(VimTestOption(SaveModeFor.name, VimTestOptionType.LIST, ["template"]))
  fun `test template with multiple times`() {
    configureByJavaText(c)
    val manager = TemplateManager.getInstance(myFixture.project)
    val template = manager.createTemplate("vn", "user", "\$V1$ var = \$V2$;")
    template.addVariable("V1", "", "\"123\"", true)
    template.addVariable("V2", "", "\"239\"", true)

    manager.startTemplate(myFixture.editor, template)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()

    assertMode(CommandState.Mode.COMMAND)
    assertOffset(2)
    typeText(parseKeys("<CR>"))
    assertMode(CommandState.Mode.COMMAND)
    assertOffset(12)
    typeText(parseKeys("<CR>"))
    assertNull(TemplateManagerImpl.getTemplateState(myFixture.editor))
  }

  private fun startRenaming(handler: VariableInplaceRenameHandler): Editor {
    val editor = if (myFixture.editor is EditorWindow) (myFixture.editor as EditorWindow).delegate else myFixture.editor
    VimListenerManager.EditorListeners.add(editor)

    handler.doRename(myFixture.elementAtCaret, editor, dataContext)
    return editor
  }

  private val dataContext
    get() = DataManager.getInstance().getDataContext(myFixture.editor.component)
}
