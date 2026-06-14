/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.key

import com.maddyhome.idea.vim.state.mode.Mode
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

@Suppress("SpellCheckingInspection")
class LangMapTest : VimTestCase() {
  @Test
  fun `test langmap converts char in Normal mode`() {
    doTest(
      "a",  // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap converts char in Visual mode`() {
    doTest(
      listOf("ve", "a"),  // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem  dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap does not convert char in single-char Replace mode`() {
    // The user is already typing in their input language, use that for the replaced char
    doTest(
      "ra", // 'a' -> 'x', does not apply!
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem apsum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap does not convert in Comamnd-line mode`() {
    // This behaviour is a little surprings. It means Command-line entry will always use the typed characters.
    // This makes sense for Dvorak, where the user can type ASCII command lines with a different layout, but less
    // so for Greek, which uses different characters.
    configureByText("\n")
    enterCommand("set langmap=ax")
    assertCommandOutput("echo 'a'", "a")  // 'a' -> 'x', does not apply!
  }

  @Test
  fun `test langmap does not convert in Comamnd-line search mode`() {
    // This behaviour is a little surprings. It means Command-line entry will always use the typed characters.
    // This makes sense for Dvorak, where the user can type ASCII command lines with a different layout, but less
    // so for Greek, which uses different characters.
    doTest(
      listOf("/a<CR>"), // 'a' -> 'm', does not apply!
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit ${c}amet",
    ) {
      enterCommand("set langmap=am")
    }
  }

  @Test
  fun `test langmap does not convert char in Insert mode`() {
    doTest(
      listOf("i", "a"), // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem aipsum dolor sit amet",
      Mode.INSERT
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap converts char for register name in Insert mode`() {
    doTest(
      "i<C-R>a<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem xxxipsum dolor sit amet",
    ) {
      enterCommand("let @x='xxx'")
      enterCommand("set langmap=ax")
    }
  }

  @Test
//  @VimBehaviorDiffers("Lorem xxxum dolor sit amet",
//    description = "IdeaVim does not properly support overwrite when expanding a register")
  fun `test langmap converts char for register name in Replace mode`() {
    doTest(
      "R<C-R>a<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem xxxipsum dolor sit amet",
    ) {
      enterCommand("let @x='xxx'")
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap does not convert char for register name in Command-line mode`() {
    configureByText("\n")
    enterCommand("let @a='aaa'")
    enterCommand("let @x='xxx'")
    enterCommand("set langmap=ax")
    typeText(exCommand("echo '<C-R>a'"))  // Process <C-R>
    assertExOutput("aaa")
  }

  @Test
  fun `test langmap converts char when selecting register for current command`() {
    configureByText("Lorem ${c}ipsum dolor sit amet")
    enterCommand("set langmap=ax")
    typeText("\"ade") // Delete word and save in register `a`, which is mapped to `x`
    assertRegister('x', "ipsum")
  }

  @Test
  fun `test langmap only converts a typed char once`() {
    doTest(
      "a",  // 'a' -> '~'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem Ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=a~,~x")
    }
  }

  @Test
  fun `test langmap converts multiple characters - operator+motion`() {
    doTest(
      "ab",  // 'a' -> 'd', 'b' -> 'w'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem dolor sit amet",
    ) {
      enterCommand("set langmap=ad,bw")
    }
  }

  @Test
  fun `test langmap does not convert find char`() {
    doTest(
      "fa", // 'a' -> 'm', does not apply!
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit ${c}amet",
    ) {
      enterCommand("set langmap=am")
    }
  }

  @Test
  fun `test langmap does not convert find char 2`() {
    doTest(
      "ta", // 'a' -> 'm', does not apply!
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit${c} amet",
    ) {
      enterCommand("set langmap=am")
    }
  }

  @Test
  fun `test langmap should not affect recursive mapping count`() {
    // With 'maxmapdepth' set to 1, we would get an error for any map (because it immediately recurses to handle the RHS
    // of the map). However, langmap does not count toward the recursion depth guard, so this map from `a` to `x` is ok.
    // If we don't delete the character, the mapping didn't apply, so check the recursion guard.
    doTest(
      "a",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
      enterCommand("set maxmapdepth=1")
    }
    assertPluginError(false)
  }

  @Test
  fun `test langmap should not affect recursive mapping count 2`() {
    // `nmap` with 'maxmapdepth' of 1 will fail on the first recursive mapping. Langmap applies first (which we know
    // does not affect recursion) then `nmap` will try to process the RHS and hit the recursion guard and fail.
    doTest(
      "a",  // `a` -> `b` (langmap), `b` -> `x` (nmap)
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=ab")
      enterCommand("nmap b x")
      enterCommand("set maxmapdepth=1")
    }
    assertPluginError(true)
    assertPluginErrorMessage("E223: Recursive mapping")
  }

  @Test
  fun `test langmap should convert characters inside a typed mapping`() {
    doTest(
      "azc",  // 'z' -> 'b', which means 'abc' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("nmap abc x")
      enterCommand("set langmap=zb")
    }
  }

  @Test
  fun `test langmap does not convert chars typed while entering a digraph`() {
    doTest(
      "i<C-K>ok<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem こipsum dolor sit amet",
    ) {
      enterCommand("set langmap=oO,kK")
    }
  }

  @Test
  fun `test langmap does not convert chars typed while entering a literal`() {
    doTest(
      "i<C-V>223<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ßipsum dolor sit amet",
    ) {
      enterCommand("set langmap=24,31")
    }
  }

  @Test
  fun `test langmap does not convert char from entering digraph`() {
    doTest(
      "i<C-K>OK<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ✓ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=✓✗")
    }
  }

  @Test
  fun `test langmap does not convert char from entering literal`() {
    doTest(
      "i<C-V>233<Esc>",
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem éipsum dolor sit amet",  // 233=é, 234=ê
    ) {
      enterCommand("set langmap=éê")
    }
  }

  @Test
  fun `test langmap converts chars while saving marks`() {
    doTest(
      "ma", // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
    assertCommandOutput(
      "marks",
      """
      |mark line  col file/text
      | x      1    6 Lorem ipsum dolor sit amet
    """.trimMargin()
    )
  }

  @Test
  fun `test langmap converts chars while jumping to mark`() {
    doTest(
      listOf("ma","0", "`a"), // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap converts chars when recording a macro`() {
    doTest(
      listOf("qa", "lll", "q"), // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
    assertCommandOutput(":reg x",
      """
        |Type Name Content
        |  c  "x   lll
      """.trimMargin())
  }

  @Test
  fun `test langmap converts char in macro name when replaying a macro`() {
    doTest(
      "@a", // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ips${c}um dolor sit amet",
    ) {
      enterCommand("let @x='lll'")
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test recorded macro contains typed non-converted chars`() {
    doTest(
      listOf("qa", "a", "q"), // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
    assertCommandOutput(":reg x",
      """
        |Type Name Content
        |  c  "x   a
      """.trimMargin())
  }

  @Test
  fun `test langmap will convert chars in macro playback`() {
    doTest(
      "@a", // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("let @x='a'")
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap does not convert chars from normal command`() {
    doTest(
      ":normal a<CR>",  // 'a' -> 'x', does not apply!
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap converts chars in Insert-pending Normal mode (CTRL-O)`() {
    doTest(
      listOf("i", "<C-O>a"),  // 'a' -> 'x'
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
      Mode.INSERT
    ) {
      enterCommand("set langmap=ax")
    }
  }

  @Test
  fun `test langmap does not convert results of a map if nolangremap set`() {
    doTest(
      "a",  // 'a' -> '~' (nmap) '~' -> 'x' (langmap - does not apply!)
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem Ipsum dolor sit amet",
    ) {
      enterCommand("set nolangremap") // Default
      enterCommand("nmap a ~")
      enterCommand("set langmap=~x")
   }
  }

  @Test
  fun `test langmap converts the results of a map if langremap set`() {
    doTest(
      "a",  // 'a' -> '~' (nmap) '~' -> 'x' (langmap)
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem psum dolor sit amet",
    ) {
      enterCommand("set langremap")
      enterCommand("nmap a ~")
      enterCommand("set langmap=~x")
    }
  }

  @Test
  fun `test langremap reports recursive mapping`() {
    doTest(
      "b",  // 'b' -> '~' (nmap) '~' -> 'x' (langmap) -> 'b' (nmap) -> ...
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langremap")
      enterCommand("nmap b ~")
      enterCommand("set langmap=~x")
      enterCommand("nmap x b")
    }
    assertPluginError(true)
    assertPluginErrorMessage("E223: Recursive mapping")
  }

  @Test
  fun `test langmap converts the results of a map before recursive mapping if langremap set`() {
    // nmap a->b, langmap jumps in with b->0. The recursive b->~ nmap does not apply
    doTest(
      "a",  // 'a' -> 'b' (nmap) 'b' -> '0' (langmap) APPLIES! 'b' -> '~' (nmap) '~' -> 'x' (nmap)
      "Lorem ${c}ipsum dolor sit amet",
      "${c}Lorem ipsum dolor sit amet",
    ) {
      enterCommand("set langremap")
      enterCommand("nmap a b")
      enterCommand("nmap b ~")
      enterCommand("set langmap=b0,~x")
    }
  }

  @Test
  fun `test langmap does not convert chars in mapping results in Insert mode with nolangremap`() {
    doTest(
      listOf("i", "a", "<Esc>"),  // 'a' -> 'b' (imap) 'b' -> 'x' (langmap - does not apply!)
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem bipsum dolor sit amet",
    ) {
      enterCommand("set nolangremap") // Default
      enterCommand("imap a b")
      enterCommand("set langmap=bx")
    }
  }

  @Test
  fun `test langmap does not convert chars in mapping results in Insert mode with langremap`() {
    // The docs say that langmap applies in Insert/Replace for "mappings", but doesn't explain how.
    // Vim doesn't appear to apply langmap to the result of mappings in Insert mode, even with 'langremap'
    doTest(
      listOf("i", "a", "<Esc>"),  // 'a' -> 'b' (imap) 'b' -> 'x' (langmap - does not apply!)
      "Lorem ${c}ipsum dolor sit amet",
      "Lorem bipsum dolor sit amet",
    ) {
      enterCommand("set langremap")
      enterCommand("imap a b")
      enterCommand("set langmap=bx")
    }
  }

  @Test
  fun `test langmap does not convert control characters`() {
    doTest(
      "<C-A><C-B>",  // 'b' -> 'a', does not apply!
      "1",
      "2",
    ) {
      enterCommand("set langmap=ba,Ba,BA")
    }
  }
}
