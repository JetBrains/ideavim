package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.helper.StringHelper
import com.maddyhome.idea.vim.option.Options
import com.maddyhome.idea.vim.ui.ExDocument
import com.maddyhome.idea.vim.ui.ExEntryPanel
import org.jetbrains.plugins.ideavim.VimTestCase
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class ExEntryTest: VimTestCase() {
    override fun setUp() {
        super.setUp()
        configureByText("\n")
    }

    fun `test cancel entry`() {
        val options = Options.getInstance()

        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<Esc>")
        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()

        deactivateExEntry()

        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<C-[>")
        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()

        deactivateExEntry()

        // TODO: This has a different implementation, which is correct? What are the side effects?
        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<C-C>")
        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()
    }

    fun `test complete entry`() {
        val options = Options.getInstance()

        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<Enter>")
        assertTrue(options.isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()

        deactivateExEntry()
        options.resetAllOptions()

        assertFalse(options.isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<C-J>")
        assertTrue(options.isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()

        deactivateExEntry()
        options.resetAllOptions()

        assertFalse(Options.getInstance().isSet(Options.INCREMENTAL_SEARCH))
        typeExInput(":set incsearch<C-M>")
        assertTrue(Options.getInstance().isSet(Options.INCREMENTAL_SEARCH))
        assertIsDeactivated()
    }

    fun `test caret shape`() {
        typeExInput(":")
        assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

        typeText("set")
        assertEquals("BLOCK 100", exEntryPanel.entry.caretShape)

        typeText("<Home>")
        assertEquals("VER 25", exEntryPanel.entry.caretShape)

        typeText("<Insert>")
        assertEquals("HOR 20", exEntryPanel.entry.caretShape)

        typeText("<Insert>")
        assertEquals("VER 25", exEntryPanel.entry.caretShape)
    }

    fun `test move caret to beginning of line`() {
        typeExInput(":set incsearch<C-B>")
        assertExOffset(0)

        deactivateExEntry()

        typeExInput(":set incsearch<Home>")
        assertExOffset(0)
    }

    fun `test move caret to end of line`() {
        typeExInput(":set incsearch<C-B>")
        assertExOffset(0)

        typeText("<C-E>")
        assertExOffset(13)

        deactivateExEntry()
        typeExInput(":set incsearch<C-B>")
        assertExOffset(0)

        typeText("<End>")
        assertExOffset(13)
    }

    fun `test delete character in front of caret`() {
        typeExInput(":set incsearch<BS>")
        assertExText("set incsearc")

        typeText("<C-H>")
        assertExText("set incsear")
    }

    fun `test delete character in front of caret cancels entry`() {
        typeExInput(":<BS>")
        assertIsDeactivated()

        deactivateExEntry()

        typeExInput(":set<BS><BS><BS><BS>")
        assertIsDeactivated()

        deactivateExEntry()

        typeExInput(":<C-H>")
        assertIsDeactivated()

        deactivateExEntry()

        // TODO: Vim behaviour is to NOT deactivate if there is still text
        typeExInput(":set<C-B>")
        assertExOffset(0)
        typeText("<BS>")
        assertIsDeactivated()
    }

    fun `test delete character under caret`() {
        typeExInput(":set<Left>")
        typeText("<Del>")
        assertExText("se")
    }

    fun `test delete word before caret`() {
        typeExInput(":set incsearch<C-W>")
        assertExText("set ")

        deactivateExEntry()

        typeExInput(":set incsearch<Left><Left><Left>")
        typeText("<C-W>")
        assertExText("set rch")
    }

    fun `test delete to start of line`() {
        typeExInput(":set incsearch<C-U>")
        assertExText("")

        deactivateExEntry()

        typeExInput(":set incsearch<Left><Left><Left><C-U>")
        assertExText("rch")
    }

    fun `test command history`() {
        typeExInput(":set digraph<CR>")
        typeExInput(":digraph<CR>")
        typeExInput(":set incsearch<CR>")

        typeExInput(":<Up>")
        assertExText("set incsearch")
        typeText("<Up>")
        assertExText("digraph")
        typeText("<Up>")
        assertExText("set digraph")

        deactivateExEntry()

        // TODO: Vim behaviour reorders the history even when cancelling history
//        typeExInput(":<Up>")
//        assertExText("set digraph")
//        typeText("<Up>")
//        assertExText("set incsearch")

        typeExInput(":<S-Up>")
        assertExText("set incsearch")
        typeText("<Up>")
        assertExText("digraph")
        typeText("<Up>")
        assertExText("set digraph")

        deactivateExEntry()

        typeExInput(":<PageUp>")
        assertExText("set incsearch")
        typeText("<PageUp>")
        assertExText("digraph")
        typeText("<PageUp>")
        assertExText("set digraph")
    }

    fun `test matching command history`() {
        typeExInput(":set digraph<CR>")
        typeExInput(":digraph<CR>")
        typeExInput(":set incsearch<CR>")

        typeExInput(":set<Up>")
        assertExText("set incsearch")
        typeText("<Up>")
        assertExText("set digraph")

        deactivateExEntry()

        typeExInput(":set<S-Up>")
        assertExText("set incsearch")
        typeText("<S-Up>")
        assertExText("digraph")
        typeText("<S-Up>")
        assertExText("set digraph")

        deactivateExEntry()

        typeExInput(":set<PageUp>")
        assertExText("set incsearch")
        typeText("<PageUp>")
        assertExText("digraph")
        typeText("<PageUp>")
        assertExText("set digraph")
    }

    fun `test search history`() {
        typeExInput("/something cool<CR>")
        typeExInput("/not cool<CR>")
        typeExInput("/so cool<CR>")

        typeExInput("/<Up>")
        assertExText("so cool")
        typeText("<Up>")
        assertExText("not cool")
        typeText("<Up>")
        assertExText("something cool")

        deactivateExEntry()

        typeExInput("/<S-Up>")
        assertExText("so cool")
        typeText("<S-Up>")
        assertExText("not cool")
        typeText("<S-Up>")
        assertExText("something cool")

        deactivateExEntry()

        typeExInput("/<PageUp>")
        assertExText("so cool")
        typeText("<PageUp>")
        assertExText("not cool")
        typeText("<PageUp>")
        assertExText("something cool")
    }

    fun `test matching search history`() {
        typeExInput("/something cool<CR>")
        typeExInput("/not cool<CR>")
        typeExInput("/so cool<CR>")

        typeExInput("/so<Up>")
        assertExText("so cool")
        typeText("<Up>")
        assertExText("something cool")

        deactivateExEntry()

        // TODO: Vim behaviour reorders the history even when cancelling history
//        typeExInput(":<Up>")
//        assertEquals("set digraph", exEntryPanel.text)
//        typeText("<Up>")
//        assertEquals("set incsearch", exEntryPanel.text)

        typeExInput("/so<S-Up>")
        assertExText("so cool")
        typeText("<S-Up>")
        assertExText("not cool")
        typeText("<S-Up>")
        assertExText("something cool")

        deactivateExEntry()

        typeExInput("/so<PageUp>")
        assertExText("so cool")
        typeText("<PageUp>")
        assertExText("not cool")
        typeText("<PageUp>")
        assertExText("something cool")
    }

    fun `test toggle insert replace`() {
        val exDocument = exEntryPanel.entry.document as ExDocument
        assertFalse(exDocument.isOverwrite)
        typeExInput(":set<C-B>digraph")
        assertExText("digraphset")

        deactivateExEntry()

        typeExInput(":set<C-B><Insert>digraph")
        assertTrue(exDocument.isOverwrite)
        assertExText("digraph")

        typeText("<Insert><C-B>set ")
        assertFalse(exDocument.isOverwrite)
        assertExText("set digraph")
    }

    fun `test move caret one WORD left`() {
        typeExInput(":set incsearch<S-Left>")
        assertExOffset(4)
        typeText("<S-Left>")
        assertExOffset(0)

        deactivateExEntry()

        typeExInput(":set incsearch<C-Left>")
        assertExOffset(4)
        typeText("<C-Left>")
        assertExOffset(0)
    }

    fun `test move caret one WORD right`() {
        typeExInput(":set incsearch")
        caret.dot = 0
        typeText("<S-Right>")
        // TODO: Vim moves caret to "set| ", while we move it to "set |"
        assertExOffset(4)

        typeText("<S-Right>")
        assertExOffset(13)

        caret.dot = 0
        typeText("<C-Right>")
        // TODO: Vim moves caret to "set| ", while we move it to "set |"
        assertExOffset(4)

        typeText("<C-Right>")
        assertExOffset(13)
    }

    fun `test digraphs`() {
        typeExInput(":<C-K>OK")
        assertExText("✓")

        // TODO: Test caret feedback
        // Vim shows "?" as the char under the caret after <C-K>, then echoes the first char of the digraph
    }

    // TODO: Test inserting control characters, if/when supported

    fun `test enter literal character by code`() {
        typeExInput(":<C-V>123<C-V>080")
        assertExText("{P")

        deactivateExEntry()

        typeExInput(":<C-V>o123")
        assertExText("S")

        deactivateExEntry()

        typeExInput(":<C-V>u00A9")
        assertExText("©")

        deactivateExEntry()

        typeExInput(":<C-Q>123<C-Q>080")
        assertExText("{P")

        deactivateExEntry()

        typeExInput(":<C-Q>o123")
        assertExText("S")

        deactivateExEntry()

        typeExInput(":<C-Q>u00a9")
        assertExText("©")
    }

    fun `test escape cancels digraph`() {
        typeExInput(":<C-K><Esc>OK")
        assertIsActive()
        assertExText("OK")

        // TODO: The Vim docs states Esc exits command line context, but Vim actually cancels digraph context
//        deactivateExEntry()
//
//        typeExInput(":<C-K>O<Esc>K")
//        assertTrue(exEntryPanel.isActive)
//        assertEquals("K", exEntryPanel.text)
//
//        deactivateExEntry()
    }

    fun `test insert register`() {
        VimPlugin.getRegister().setKeys('c', StringHelper.parseKeys("hello world"))
        VimPlugin.getRegister().setKeys('5', StringHelper.parseKeys("greetings programs"))

        typeExInput(":<C-R>c")
        assertExText("hello world")

        deactivateExEntry()

        typeExInput(":<C-R>5")
        assertExText("greetings programs")

        // TODO: Test caret feedback
        // Vim shows " after hitting <C-R>
    }

    fun `test insert multi-line register`() {
        // parseKeys parses <CR> in a way that Register#getText doesn't like
        val keys = mutableListOf<KeyStroke>()
        keys.addAll(StringHelper.parseKeys("hello"))
        keys.add(KeyStroke.getKeyStroke('\n'))
        keys.addAll(StringHelper.parseKeys("world"))
        VimPlugin.getRegister().setKeys('c', keys)

        typeExInput(":<C-R>c")
        assertExText("hello world")
    }

    // TODO: Test other special registers, if/when supported
    // E.g. '.' '%' '#', etc.

    fun `test insert last command`() {
        typeExInput(":set incsearch<CR>")
        typeExInput(":<C-R>:")
        assertExText("set incsearch")
    }

    fun `test insert last search command`() {
        typeExInput("/hello<CR>")
        typeExInput(":<C-R>/")
        assertExText("hello")
    }

    private fun typeExInput(text: String) {
        assertTrue("Ex command must start with ':', '/' or '?'",
            text.startsWith(":") || text.startsWith('/') || text.startsWith('?'))

        val keys = mutableListOf<KeyStroke>()
        StringHelper.parseKeys(text).forEach {
            // <Left> doesn't work - DefaultEditorKit.NextVisualPositionAction fails to move the caret correctly because
            // the text component has never been painted
            if (it.keyCode == KeyEvent.VK_LEFT && it.modifiers == 0) {
                if (keys.count() > 0) {
                    typeText(keys)
                    keys.clear()
                }

                exEntryPanel.entry.caret.dot--
            }
            else {
                keys.add(it)
            }
        }
        if (keys.count() > 0)
            typeText(keys)
    }

    private fun typeText(text: String) {
        typeText(StringHelper.parseKeys(text))
    }

    private fun deactivateExEntry() {
        // We don't need to reset text, that's handled by #active
        if (exEntryPanel.isActive)
            typeText("<Esc>")
    }

    private fun assertExText(expected: String) {
        assertEquals(expected, exEntryPanel.text)
    }

    private fun assertIsActive() {
        assertTrue(exEntryPanel.isActive)
    }

    private fun assertIsDeactivated() {
        assertFalse(exEntryPanel.isActive)
    }

    private fun assertExOffset(expected: Int) {
        assertEquals(expected, caret.dot)
    }

    private val exEntryPanel
        get() = ExEntryPanel.getInstance()

    private val caret
        get() = exEntryPanel.entry.caret
}