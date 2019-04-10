@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SelectExtendVariousMotionsTest : VimTestCase() {

    fun `test with tabs`() {
        val code = """
            class Scratch {
            	public static void main(String[] args) {
            		try {
            			if ()
            		}
            	}
            }

            func myFunc() {
            	return anything
            ${c}}
        """.trimIndent()

        myFixture.configureByText(PlainTextFileType.INSTANCE, code)

        typeText(parseKeys("g<C-H>", "<S-UP>".repeat(2), "<S-Right>".repeat(2)))

        myFixture.checkResult("""
            class Scratch {
            	public static void main(String[] args) {
            		try {
            			if ()
            		}
            	}
            }

            ${s}fun${c}${se}c myFunc() {
            ${s}${c}${se}	return anything
            ${s}}${c}${se}
        """.trimIndent()
        )

        typeText(parseKeys("<S-UP>".repeat(7), "<S-Right>".repeat(3)))

        myFixture.checkResult("""
            class Scratch {
            ${s}	pu${c}${se}blic static void main(String[] args) {
            ${s}	${c}${se}	try {
            ${s}	${c}${se}		if ()
            ${s}	${c}${se}	}
            ${s}	}${c}${se}
            ${s}}${c}${se}

            ${s}func m${c}${se}yFunc() {
            ${s}	re${c}${se}turn anything
            ${s}}${c}${se}
        """.trimIndent()
        )

        typeText(parseKeys("<S-Right>".repeat(2)))

        myFixture.checkResult("""
            class Scratch {
            ${s}	publ${c}${se}ic static void main(String[] args) {
            ${s}		${c}${se}try {
            ${s}		${c}${se}	if ()
            ${s}		${c}${se}}
            ${s}	}${c}${se}
            ${s}}${c}${se}

            ${s}func myF${c}${se}unc() {
            ${s}	retu${c}${se}rn anything
            ${s}}${c}${se}
        """.trimIndent()
        )
    }
}