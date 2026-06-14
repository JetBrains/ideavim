/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.option

import com.maddyhome.idea.vim.options.helpers.LangMapOptionHelper
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("SpellCheckingInspection")
class LangMapOptionTest : VimTestCase("\n") {
  @Test
  fun `test set 'langmap' with empty value`() {
    enterCommand("set langmap=")
    assertEquals('a', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' with simple pair`() {
    enterCommand("set langmap=aA")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' with multiple pairs`() {
    enterCommand("set langmap=aAbBcCdD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' with pair separated by semicolon`() {
    enterCommand("set langmap=a;A")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' with multiple pairs separated by semicolon`() {
    enterCommand("set langmap=abcd;ABCD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' accepts unescaped semicolon in RHS of semicolon separated pairs`() {
    // Clearly a by product of the way the string is split+parsed
    enterCommand("set langmap=abc;A;C")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals(';', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
  }

  @Test
  fun `test set 'langmap' with pairs separated by commas`() {
    enterCommand("set langmap=aA,bB,cC,dD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' with empty comma-separated part`() {
    enterCommand("set langmap=aA,,bB")
    assertCommandOutput("set langmap?", "  langmap=aA,,bB")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
  }

  @Test
  fun `test set 'langmap' with multiple comma-separated parts containing multiple pairs`() {
    enterCommand("set langmap=aAbB,cCdD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' with multiple comma-separated parts containing semicolon-separated pairs`() {
    enterCommand("set langmap=ab;AB,cd;CD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' with multiple comma-separated parts containing mix of simple pairs and semicolon-separated pairs`() {
    enterCommand("set langmap=aAbB,cd;CD")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
    assertEquals('D', LangMapOptionHelper.mapChar('d'))
  }

  @Test
  fun `test set 'langmap' overwrites existing mapping`() {
    enterCommand("set langmap=aB")
    enterCommand("set langmap=aC")
    assertEquals('C', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' appends to existing mapping`() {
    enterCommand("set langmap=aA")
    enterCommand("set langmap+=bB")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar('b'))
  }

  @Test
  fun `test set 'langmap' removes existing mapping`() {
    enterCommand("set langmap=aA,bB")
    enterCommand("set langmap-=bB")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('b', LangMapOptionHelper.mapChar('b'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped comma for from char`() {
    // Vim appears to treat `\\,` as a single escaped comma, and seems to be handled at the parsing stage. Setting and
    // then querying the value (`:set langmap?`) below in Vim will result in `  langmap=\,a`.
    // Check out the Dvorak test/example below
    enterCommand("""set langmap=\\,a""")
    assertEquals('a', LangMapOptionHelper.mapChar(','))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped comma for from char separated by semicolon`() {
    enterCommand("""set langmap=\\,;a""")
    assertEquals('a', LangMapOptionHelper.mapChar(','))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped comma for to char`() {
    enterCommand("""set langmap=a\\,""")
    assertEquals(',', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped comma for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\\,""")
    assertEquals(',', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped semicolon for from char`() {
    enterCommand("""set langmap=\\;a""")
    assertEquals('a', LangMapOptionHelper.mapChar(';'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped semicolon for from char separated by semicolon`() {
    enterCommand("""set langmap=\\;;a""")
    assertEquals('a', LangMapOptionHelper.mapChar(';'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped semicolon for to char`() {
    enterCommand("""set langmap=a\\;""")
    assertEquals(';', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped semicolon for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\\;""")
    assertEquals(';', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' treats unescaped quote as comment character`() {
    enterCommand("""set langmap=aA"xbBcC""")
    assertCommandOutput("set langmap?", "  langmap=aA")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    // Mapping didn't happen
    assertEquals('b', LangMapOptionHelper.mapChar('b'))
    assertEquals('c', LangMapOptionHelper.mapChar('c'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped double-quotes for from char`() {
    // The single backslash is necessary, otherwise it becomes a comment
    // Vim accepts a single backslash here, while it expects double backslash for comma and semicolon. I guess this is
    // at a different level of parsing? E.g. the parsing for option value (double backslash) is separate to the parsing
    // of the initial `set` command (single backslash)
    enterCommand("""set langmap=\"a""")
    assertCommandOutput("set langmap?", """  langmap="a""")
    assertEquals('a', LangMapOptionHelper.mapChar('"'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped double-quotes for from char separated by semicolon`() {
    enterCommand("""set langmap=\";a""")
    assertCommandOutput("set langmap?", """  langmap=";a""")
    assertEquals('a', LangMapOptionHelper.mapChar('"'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped double-quotes for to char`() {
    enterCommand("""set langmap=a\"""")
    assertCommandOutput("set langmap?", """  langmap=a"""")
    assertEquals('"', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped double-quotes for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\"""")
    assertCommandOutput("set langmap?", """  langmap=a;"""")
    assertEquals('"', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped double-quotes for from char`() {
    // I think Vim parses this string in multiple passes. Firstly, unescaping bar and quote, which are to do with
    // separating the current command from the next, or from a comment. This would remove one backslash from a quote or
    // bar. Then, when parsing the option value, it unescapes all characters, by "halving" the number of backslashes.
    // I think that explains why this double backslash doesn't become a single backslash in the output of the command
    enterCommand("""set langmap=\\"a""")
    assertCommandOutput("set langmap?", """  langmap="a""")
    assertEquals('a', LangMapOptionHelper.mapChar('"'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped double-quotes for from char separated by semicolon`() {
    enterCommand("""set langmap=\\";a""")
    assertCommandOutput("set langmap?", """  langmap=";a""")
    assertEquals('a', LangMapOptionHelper.mapChar('"'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped double-quotes for to char`() {
    enterCommand("""set langmap=a\\"""")
    assertCommandOutput("set langmap?", """  langmap=a"""")
    assertEquals('"', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped double-quotes for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\\"""")
    assertCommandOutput("set langmap?", """  langmap=a;"""")
    assertEquals('"', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped bar for from char`() {
    enterCommand("""set langmap=\|a""")
    assertCommandOutput("set langmap?", """  langmap=|a""")
    assertEquals('a', LangMapOptionHelper.mapChar('|'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped bar for from char separated by semicolon`() {
    enterCommand("""set langmap=\|;a""")
    assertCommandOutput("set langmap?", """  langmap=|;a""")
    assertEquals('a', LangMapOptionHelper.mapChar('|'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped bar for to char`() {
    enterCommand("""set langmap=a\|""")
    assertCommandOutput("set langmap?", """  langmap=a|""")
    assertEquals('|', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts single backslash as escaped bar for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\|""")
    assertCommandOutput("set langmap?", """  langmap=a;|""")
    assertEquals('|', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped bar for from char`() {
    enterCommand("""set langmap=\\|a""")
    assertCommandOutput("set langmap?", """  langmap=|a""")
    assertEquals('a', LangMapOptionHelper.mapChar('|'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped bar for from char separated by semicolon`() {
    enterCommand("""set langmap=\\|;a""")
    assertCommandOutput("set langmap?", """  langmap=|;a""")
    assertEquals('a', LangMapOptionHelper.mapChar('|'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped bar for to char`() {
    enterCommand("""set langmap=a\\|""")
    assertCommandOutput("set langmap?", """  langmap=a|""")
    assertEquals('|', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' accepts double backslash as escaped bar for to char separated by semicolon`() {
    enterCommand("""set langmap=a;\\|""")
    assertCommandOutput("set langmap?", """  langmap=a;|""")
    assertEquals('|', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' requires 4 backslashes for a single escaped backslash as from char`() {
    enterCommand("""set langmap=\\\\a""")
    assertEquals('a', LangMapOptionHelper.mapChar('\\'))
  }

  @Test
  fun `test set 'langmap' requires 4 backslashes for a single escaped backslash as from char separated by semicolon`() {
    enterCommand("""set langmap=\\\\;a""")
    assertEquals('a', LangMapOptionHelper.mapChar('\\'))
  }

  @Test
  fun `test set 'langmap' requires 4 backslashes for a single escaped backslash as to char`() {
    enterCommand("""set langmap=a\\\\""")
    assertEquals('\\', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' requires 4 backslashes for a single escaped backslash as to char separated by semicolon`() {
    enterCommand("""set langmap=a;\\\\""")
    assertEquals('\\', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' with escaped values and comma separator`() {
    enterCommand("""set langmap=a\\,,\\;b""")
    assertEquals(',', LangMapOptionHelper.mapChar('a'))
    assertEquals('b', LangMapOptionHelper.mapChar(';'))
  }

  @Test
  fun `test set 'langmap' with escaped values and semicolon separator`() {
    enterCommand("""set langmap=a\\,;\\;b""")
    assertEquals(';', LangMapOptionHelper.mapChar('a'))
    assertEquals('b', LangMapOptionHelper.mapChar(','))
  }

  @Test
  fun `test set 'langmap' with escaped semicolon from char`() {
    enterCommand("""set langmap=a\\;c;ABC""")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals('B', LangMapOptionHelper.mapChar(';'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
  }

  @Test
  fun `test set 'langmap' with escaped semicolon to char`() {
    enterCommand("""set langmap=abc;A\\;C""")
    assertEquals('A', LangMapOptionHelper.mapChar('a'))
    assertEquals(';', LangMapOptionHelper.mapChar('b'))
    assertEquals('C', LangMapOptionHelper.mapChar('c'))
  }

  @Test
  fun `test set 'langmap' handles space as from char`() {
    enterCommand("""set langmap=\ abc""")
    assertEquals('a', LangMapOptionHelper.mapChar(' '))
    assertEquals('c', LangMapOptionHelper.mapChar('b'))
  }

  @Test
  fun `test set 'langmap' handles space as to char`() {
    enterCommand("""set langmap=a\ bc""")
    assertEquals(' ', LangMapOptionHelper.mapChar('a'))
    assertEquals('c', LangMapOptionHelper.mapChar('b'))
  }

  @Test
  fun `test set 'langmap' reports error with mismatched pair`() {
    enterCommand("set langmap=aAb")
    assertPluginError(true)
    assertPluginErrorMessage("E357: 'langmap': Matching character missing for b: langmap=aAb")
  }

  @Test
  fun `test set 'langmap' reports error with mismatched pair using given option name`() {
    enterCommand("set lmap=aAb")
    assertPluginError(true)
    assertPluginErrorMessage("E357: 'langmap': Matching character missing for b: lmap=aAb")
  }

  @Test
  fun `test set 'langmap' reports error with mismatched semicolon separated pair`() {
    enterCommand("""set langmap=ab;A""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for b: langmap=ab;A""")
  }

  @Test
  fun `test set 'langmap' reports error with additional character after semicolon`() {
    enterCommand("""set langmap=ab;ABC""")
    assertPluginError(true)
    assertPluginErrorMessage("E358: 'langmap': Extra characters after semicolon: C: langmap=ab;ABC")
  }

  @Test
  fun `test set 'langmap' reports error with empty from chars in semicolon pair`() {
    enterCommand("set langmap=;ABC")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for C: langmap=;ABC""")
  }

  @Test
  fun `test set 'langmap' reports error with empty to chars in semicolon pair`() {
    enterCommand("set langmap=ABC;")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for A: langmap=ABC;""")
  }

  @Test
  fun `test set 'langmap' reports error with single backslash escaped character as from char`() {
    // A single backslash isn't a proper escape character. Vim sees this as `langmap=,a` which is invalid
    enterCommand("""set langmap=\,a""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=\,a""")
  }

  @Test
  fun `test set 'langmap' reports error with single backslash escaped character as to char`() {
    // A single backslash isn't a proper escape character. Vim sees this as `langmap=,a` which is invalid
    enterCommand("""set langmap=a\,""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=a\,""")
  }

  @Test
  fun `test set 'langmap' reports error with double-escaped backslash as from char`() {
    // See `:help option-backslash`
    // Vim sees this as `langmap=\a`, which is an escaped `a` character
    enterCommand("""set langmap=\\a""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=\\a""")
  }

  @Test
  fun `test set 'langmap' reports error with double-escaped backslash as to char`() {
    // See `:help option-backslash`
    // Vim sees this as `langmap=a\`, which is a trailing/unfinished escape character
    enterCommand("""set langmap=a\\""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=a\\""")
  }

  @Test
  fun `test set 'langmap' reports error with triple-escaped backslash as from char`() {
    // See `:help option-backslash`
    // Vim sees this as `langmap=\a`, which is an escaped `a` character
    enterCommand("""set langmap=\\\a""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=\\\a""")
  }

  @Test
  fun `test set 'langmap' accepts triple-escaped backslash as from char`() {
    // See `:help option-backslash`
    // Vim sees this as `langmap=a\\`, which is a valid map
    enterCommand("""set langmap=a\\\""")
    assertEquals('\\', LangMapOptionHelper.mapChar('a'))
  }

  @Test
  fun `test set 'langmap' reports error with trailing escape character as from char`() {
    enterCommand("""set langmap=\""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for \: langmap=\""")
  }

  @Test
  fun `test set 'langmap' reports error with trailing escape character as to char`() {
    enterCommand("""set langmap=a\""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=a\""")
  }

  @Test
  fun `test set 'langmap' reports error with trailing double-escape character as from char`() {
    enterCommand("""set langmap=\\""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for \: langmap=\\""")
  }

  @Test
  fun `test set 'langmap' reports error with trailing double-escape character as to char`() {
    enterCommand("""set langmap=a\\""")
    assertPluginError(true)
    assertPluginErrorMessage("""E357: 'langmap': Matching character missing for a: langmap=a\\""")
  }

  @Test
  fun `test set 'langmap' correctly parses Greek example`() {
    // :help greek
    enterCommand("""set langmap=ΑA,ΒB,ΨC,ΔD,ΕE,ΦF,ΓG,ΗH,ΙI,ΞJ,ΚK,ΛL,ΜM,ΝN,ΟO,ΠP,QQ,ΡR,ΣS,ΤT,ΘU,ΩV,WW,ΧX,ΥY,ΖZ,αa,βb,ψc,δd,εe,φf,γg,ηh,ιi,ξj,κk,λl,μm,νn,οo,πp,qq,ρr,σs,τt,θu,ωv,ςw,χx,υy,ζz""")
    assertPluginError(false)
    assertEquals('C', LangMapOptionHelper.mapChar('Ψ'))
    assertEquals('z', LangMapOptionHelper.mapChar('ζ'))
  }

  @Test
  fun `test set 'langmap' correctly parses Dvorak example`() {
    // https://vim.fandom.com/wiki/Using_Vim_with_the_Dvorak_keyboard_layout
    // Note that `\\` after the `'q` pair. The next pair is `,w` - the double-backslash is escaping the comma.
    // Vim appears to handle double-backslash first, leaving us with `\,w` as an escaped comma.
    // Entering this, then retrieving it again with `:set langmap?` gives this:
    // (Vim also strips the escape from the `"`??)
    // 'q,\,w,.e,pr,yt,fy,gu,ci,ro,lp,/[,=],aa,os,ed,uf,ig,dh,hj,tk,nl,s\;,-',\;z,qx,jc,kv,xb,bn,mm,w\,,v.,z/,[-,]=,"Q,<W,>E,PR,YT,FY,GU,CI,RO,LP,?{,+},AA,OS,ED,UF,IG,DH,HJ,TK,NL,S:,_",:Z,QX,JC,KV,XB,BN,MM,W<,V>,Z?
    // This string is verbatim from the webpage - no backslashes are removed during compilation.
    enterCommand(
      """set langmap='q,\\,w,.e,pr,yt,fy,gu,ci,ro,lp,/[,=],aa,os,ed,uf,ig,dh,hj,tk,nl,s\\;,-',\\;z,qx,jc,kv,xb,bn,mm,w\\,,v.,z/,[-,]=,\"Q,<W,>E,PR,YT,FY,GU,CI,RO,LP,?{,+},AA,OS,ED,UF,IG,DH,HJ,TK,NL,S:,_\",:Z,QX,JC,KV,XB,BN,MM,W<,V>,Z?"""
    )
    assertPluginError(false)
    assertEquals('q', LangMapOptionHelper.mapChar('\''))
    assertEquals('w', LangMapOptionHelper.mapChar(','))
    assertEquals('e', LangMapOptionHelper.mapChar('.'))
    assertEquals('r', LangMapOptionHelper.mapChar('p'))
    assertEquals('[', LangMapOptionHelper.mapChar('/'))
    assertEquals(';', LangMapOptionHelper.mapChar('s'))
    assertEquals('z', LangMapOptionHelper.mapChar(';'))
    assertEquals(',', LangMapOptionHelper.mapChar('w'))
    assertEquals('.', LangMapOptionHelper.mapChar('v'))
    assertEquals('/', LangMapOptionHelper.mapChar('z'))
    assertEquals('Q', LangMapOptionHelper.mapChar('"'))
    assertEquals('{', LangMapOptionHelper.mapChar('?'))
    assertEquals(':', LangMapOptionHelper.mapChar('S'))
    assertEquals('"', LangMapOptionHelper.mapChar('_'))
    assertEquals('Z', LangMapOptionHelper.mapChar(':'))
    assertEquals('>', LangMapOptionHelper.mapChar('V'))
    assertEquals('?', LangMapOptionHelper.mapChar('Z'))

    // Test that, like Vim, we store a processed version of the user-entered string that simplifies the escaping. Only
    // comma, semicolon and backslash should be escaped
    typeText("S", "set langmap?<CR>") // `S` -> `:`
    assertExOutput(
      """  langmap='q,\,w,.e,pr,yt,fy,gu,ci,ro,lp,/[,=],aa,os,ed,uf,ig,dh,hj,tk,nl,s\;,-',\;z,qx,jc,kv,xb,bn,mm,w\,,v.,z/,[-,]=,"Q,<W,>E,PR,YT,FY,GU,CI,RO,LP,?{,+},AA,OS,ED,UF,IG,DH,HJ,TK,NL,S:,_",:Z,QX,JC,KV,XB,BN,MM,W<,V>,Z?"""
    )
  }
}
