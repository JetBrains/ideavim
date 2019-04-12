@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.group.visual

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.ide.DataManager
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.testFramework.fixtures.CodeInsightTestUtil.doInlineRename
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.group.VimListenerManager
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class TemplateTest : VimTestCase() {

    lateinit var disposable: Disposable

    override fun setUp() {
        super.setUp()
        disposable = Disposer.newDisposable()
    }

    override fun tearDown() {
        super.tearDown()
        Disposer.dispose(disposable)
    }

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

    fun `test type rename`() {
        configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
        startRenaming(VariableInplaceRenameHandler())
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

    fun `test prepend`() {
        configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
        startRenaming(VariableInplaceRenameHandler())
        assertState(CommandState.Mode.SELECT, CommandState.SubMode.VISUAL_CHARACTER)

        typeText(parseKeys("<Left>"))
        assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
        typeText(parseKeys("pre", "<CR>"))

        assertState(CommandState.Mode.INSERT, CommandState.SubMode.NONE)
        myFixture.checkResult("""
            class Hello {
                public static void main() {
                    int mpre${c}yVar = 5;
                }
            }
        """.trimIndent())
    }

    fun `test escape`() {
        configureByJavaText("""
            class Hello {
                public static void main() {
                    int my${c}Var = 5;
                }
            }
        """.trimIndent())
        startRenaming(VariableInplaceRenameHandler())
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


    private fun startRenaming(handler: VariableInplaceRenameHandler): Editor {
        val editor = if (myFixture.editor is EditorWindow) (myFixture.editor as EditorWindow).delegate else myFixture.editor
        VimListenerManager.addEditorListeners(editor)

        TemplateManagerImpl.setTemplateTesting(myFixture.project, disposable)
        handler.doRename(myFixture.elementAtCaret, editor, dataContext)
        return editor
    }

    private val dataContext
        get() = DataManager.getInstance().getDataContext(myFixture.editor.component)
}