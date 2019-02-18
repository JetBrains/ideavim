package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.ex.commands
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.VimTestCase.assertThrows

/**
 * @author Alex Plate
 */
class CommandParserTest : VimTestCase() {
    fun `test one letter without optional`() {
        val commands = commands("a")
        TestCase.assertEquals(1, commands.size)
        assertEquals("a", commands[0].required)
        assertEquals("", commands[0].optional)
    }

    fun `test without optional`() {
        val commands = commands("a_discovery")
        TestCase.assertEquals(1, commands.size)
        assertEquals("a_discovery", commands[0].required)
        assertEquals("", commands[0].optional)
    }

    fun `test empty optional`() {
        assertThrows<RuntimeException>(RuntimeException::class.java) {
            commands("a_discovery[]")
        }
    }

    fun `test empty`() {
        assertThrows<RuntimeException>(RuntimeException::class.java) {
            commands("")
        }
    }

    fun `test no closing bracket`() {
        assertThrows<RuntimeException>(RuntimeException::class.java) {
            commands("a_discovery[")
        }
    }

    fun `test with optional`() {
        val commands = commands("a[discovery]")
        TestCase.assertEquals(1, commands.size)
        assertEquals("a", commands[0].required)
        assertEquals("discovery", commands[0].optional)
    }
}