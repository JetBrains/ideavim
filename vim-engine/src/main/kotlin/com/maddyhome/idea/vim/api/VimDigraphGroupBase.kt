/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("LanguageDetectionInspection")

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.EngineStringHelper
import org.jetbrains.annotations.TestOnly
import java.util.*
import kotlin.math.ceil

private val logger = vimLogger<VimDigraphGroup>()

open class VimDigraphGroupBase : VimDigraphGroup {

  override fun getCharacterForDigraph(ch1: Char, ch2: Char): Int {
    fun getCodepoint(ch1: Char, ch2: Char, digraphs: Map<String, Int>): Int? {
      val chars = charArrayOf(ch1, ch2)
      var digraph = String(chars)
      return digraphs.getOrElse(digraph) {
        chars[0] = ch2
        chars[1] = ch1
        digraph = String(chars)
        digraphs[digraph]  // Possibly null
      }
    }

    return getCodepoint(ch1, ch2, customDigraphToCodepoint)
      ?: getCodepoint(ch1, ch2, digraphToCodepoint)
      ?: ch2.code
  }

  override fun displayAsciiInfo(editor: VimEditor) {
    val offset = editor.currentCaret().offset
    val charsSequence = editor.text()
    if (charsSequence.isEmpty() || offset >= charsSequence.length) return

    val codepoint = Character.codePointAt(charsSequence, offset)

    val digraph = customCodepointToDigraph[codepoint] ?: codepointToDigraph[codepoint]
    val digraphText = if (digraph == null) "" else ", Digr $digraph"

    if (codepoint < 0x100) {
      injector.messages.showMessage(
        editor,
        String.format(
          "<%s>  %d,  Hex %02x,  Oct %03o%s",
          EngineStringHelper.toPrintableCharacter(codepoint),
          codepoint,
          codepoint,
          codepoint,
          digraphText,
        ),
      )
    } else {
      injector.messages.showMessage(
        editor,
        String.format(
          "<%s> %d, Hex %04x, Oct %o%s",
          EngineStringHelper.toPrintableCharacter(codepoint),
          codepoint,
          codepoint,
          codepoint,
          digraphText,
        ),
      )
    }
  }

  private fun loadDigraphs() {
    var i = 0
    while (i < defaultDigraphs.size) {
      if (defaultDigraphs[i] != '\u0000' && defaultDigraphs[i + 1] != '\u0000') {
        val codepoint = defaultDigraphs[i + 2].code
        val digraph = String(defaultDigraphs, i, 2)
        digraphToCodepoint[digraph] = codepoint
        if (!codepointToDigraph.contains(codepoint)) {
          codepointToDigraph[codepoint] = digraph
        }
      }
      i += 3
    }
  }

  override fun parseCommandLine(editor: VimEditor, args: String): Boolean {
    val tokenizer = StringTokenizer(args)
    while (tokenizer.hasMoreTokens()) {
      val digraph = tokenizer.nextToken()
      if (digraph.length == 1) {
        throw exExceptionMessage("E1214", digraph)
      }

      if (!(tokenizer.hasMoreTokens())) {
        throw exExceptionMessage("E39")
      }

      val codepoint = tokenizer.nextToken().toIntOrNull()
      if (codepoint == null) {
        throw exExceptionMessage("E39")
      }

      addCustomDigraph(digraph.substring(0, 2), codepoint)
    }

    return true
  }

  override fun showDigraphs(editor: VimEditor, showHeaders: Boolean) {
    val width = injector.engineEditorHelper.getApproximateOutputPanelWidth(editor).let { if (it < 10) 80 else it }

    // Vim's columns are 13 characters wide, but for some reason, they suddenly switch to 12. It makes no obvious sense,
    // and it's a quirk too far to copy.
    val columnWidth = 13
    val columnCount = width / columnWidth
    val height = ceil(digraphToCodepoint.size.toDouble() / columnCount.toDouble()).toInt()

    if (logger.isDebug()) {
      logger.debug("width=$width")
      logger.debug("colCount=$columnCount")
      logger.debug("height=$height")
    }

    val digraphCount = (defaultDigraphs.size / 3) + customDigraphToCodepoint.size
    val capacity = (digraphCount * columnWidth) + (digraphCount / columnCount) + 300 // Text + newlines + headers
    val output = buildString(capacity) {
      var column = 0
      var columnLength = 0
      var previousUnicodeBlock: Character.UnicodeBlock? = null

      // We cannot guarantee ordering with the dictionaries, so let's use the defaultDigraphs list.
      // We output in codepoint order, but there are duplicate digraphs for some codepoints and we want control of order
      for (i in 0 until defaultDigraphs.size step 3) {
        val codepoint = defaultDigraphs[i + 2].code

        // Show headers if requested. Vim shows headers for some Unicode blocks, but not all. And its block boundaries
        // aren't necessarily correct
        val block = getVimCompatibleUnicodeBlock(codepoint)
        if (showHeaders && block != previousUnicodeBlock && digraphHeaderNames.containsKey(block)) {
          if (column != 0) {
            appendLine()
          }
          appendLine(digraphHeaderNames[block])
          previousUnicodeBlock = block
          column = 0
        }

        if (column != 0) {
          repeat(columnWidth - (columnLength % columnWidth)) { append(' ') }
        }

        columnLength = appendDigraph(defaultDigraphs[i], defaultDigraphs[i + 1], codepoint)
        column++

        if (column == columnCount) {
          appendLine()
          column = 0
        }
      }

      if (showHeaders && customDigraphToCodepoint.isNotEmpty()) {
        if (column != 0) {
          appendLine()
        }
        appendLine("Custom")
        column = 0
      }

      customDigraphToCodepoint.forEach { (digraph, char) ->
        if (column != 0) {
          repeat(columnWidth - (columnLength % columnWidth)) { append(' ') }
        }

        columnLength = appendDigraph(digraph[0], digraph[1], char)
        column++

        if (column == columnCount) {
          appendLine()
          column = 0
        }
      }
    }

    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.outputPanel.output(editor, context, output)
  }

  private fun StringBuilder.appendDigraph(ch1: Char, ch2: Char, codepoint: Int): Int {
    val start = this.length

    append(ch1)
    append(ch2)
    append(' ')

    // VIM highlights the printable character with HLF_8, which it also uses for special keys in `:map`
    val printable = EngineStringHelper.toPrintableCharacter(codepoint)
    val invisibleCharAdjustment = when {
      // Vim null handling weirdness... `NU` is NULL, and represented as NL ('\u000a`), but the char should be `^@`
      ch1 == 'N' && ch2 == 'U' -> {
        append(EngineStringHelper.toPrintableCharacter(0))
        0
      }

      printable.length == 1 && isRightToLeft(codepoint) -> {
        append('\u2067')  // RIGHT_TO_LEFT_ISOLATE - set RTL and isolate following content from the surrounding text
        append(printable)
        append('\u2069')  // POP_DIRECTIONAL_ISOLATE - close the isolation range and return to LTR
        2
      }

      printable.length == 1 && isCombiningCharacter(codepoint) -> {
        append(' ') // Give the combining character something to combine with
        append(printable)
        1
      }

      else -> {
        append(printable)
        0
      }
    }

    // Add an extra space if we've only used one text cell.
    // Ideally here, we'd check the EAST_ASIAN_WIDTH Unicode property of the printed character. If it's full width,
    // it's taken two "cells". I'm not sure this would work for all characters, e.g. Ⅵ seems to be 1.5 "cells" wide.
    // Perhaps we could set the output panel's tab size to 13, and use tab stops to make things line up?
    if (printable.length == 1) {
      append(' ')
    }

    // Print the code: ' %3d'
    append(' ')
    append(codepoint.toString().padStart(3))

    return length - start - invisibleCharAdjustment
  }

  private fun isRightToLeft(codepoint: Int): Boolean {
    val directionality = Character.getDirectionality(codepoint)
    return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
      || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
      || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
      || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
  }

  private fun isCombiningCharacter(codepoint: Int): Boolean {
    val type = Character.getType(codepoint).toByte()
    return type == Character.NON_SPACING_MARK
      || type == Character.COMBINING_SPACING_MARK
      || type == Character.ENCLOSING_MARK
      || type == Character.FORMAT
  }

  private fun getVimCompatibleUnicodeBlock(codepoint: Int): Character.UnicodeBlock {
    // Vim's block boundaries don't agree with Java's. Fudge things so they match
    val block = Character.UnicodeBlock.of(codepoint)
    return when {
      block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT && codepoint < 0xa1 -> Character.UnicodeBlock.BASIC_LATIN
      block == Character.UnicodeBlock.NUMBER_FORMS && codepoint < 0x2160 -> Character.UnicodeBlock.LETTERLIKE_SYMBOLS
      else -> block
    }
  }

  private fun addCustomDigraph(digraph: String, codepoint: Int) {
    customDigraphToCodepoint[digraph] = codepoint
    customCodepointToDigraph[codepoint] = digraph
  }

  // Surprisingly, Vim doesn't have a command line for removing custom digraphs
  @TestOnly
  override fun clearCustomDigraphs() {
    customDigraphToCodepoint.clear()
    customCodepointToDigraph.clear()
  }

  // Based on the digraphs listed in `:help digraph-table` and `:help digraph-table-mbyte`, which unfortunately doesn't
  // list all digraphs. The output of the `:digraphs` command (`redir @">|silent digraphs|redir END|enew|put`) is used
  // to fill in the missing entries. (Compare against the output in the tests)
  // This awk script will convert these tables into the format we need:
  // awk '{
  //  char1 = substr($2, 1, 1);
  //  char2 = substr($2, 2, 1);
  //  name = substr($0, index($0, $5))
  //  sub(/[ \t]+$/, "" name)
  //
  //  gsub(/'\''/, "\\'\''", char1)
  //  gsub(/'\''/, "\\'\''", char2)
  //
  //  codepoint = $3;
  //  gsub("0x", "", codepoint);
  //  unicode = tolower(sprintf("\\u%04s", codepoint))
  //
  //  printf "'\''%s'\'', '\''%s'\'', '\''%s'\'', // %4d %s %s\n", char1, char2, unicode, $4, $1, name;
  //}' $input_file
  //
  // NOTE:
  // * This script fails for `SP` - the space character will have to be updated manually
  // * The `NU` digraph is set to `\u000a`. This matches Vim behaviour, which ignores a digraph with a code of 0
  //   See `:help i_CTRL-V_digit` for more details (Vim uses `\u000a` internally to represent null)
  @Suppress("GrazieInspection", "SpellCheckingInspection")
  private val defaultDigraphs = charArrayOf(
    // See `:help digraph-table`
    'N', 'U', '\u000a', //   10 ^@ NULL (NUL)
    'S', 'H', '\u0001', //    1 ^A START OF HEADING (SOH)
    'S', 'X', '\u0002', //    2 ^B START OF TEXT (STX)
    'E', 'X', '\u0003', //    3 ^C END OF TEXT (ETX)
    'E', 'T', '\u0004', //    4 ^D END OF TRANSMISSION (EOT)
    'E', 'Q', '\u0005', //    5 ^E ENQUIRY (ENQ)
    'A', 'K', '\u0006', //    6 ^F ACKNOWLEDGE (ACK)
    'B', 'L', '\u0007', //    7 ^G BELL (BEL)
    'B', 'S', '\u0008', //    8 ^H BACKSPACE (BS)
    'H', 'T', '\u0009', //    9 ^I CHARACTER TABULATION (HT)
    'L', 'F', '\u000a', //   10 ^@ LINE FEED (LF)
    'V', 'T', '\u000b', //   11 ^K LINE TABULATION (VT)
    'F', 'F', '\u000c', //   12 ^L FORM FEED (FF)
    'C', 'R', '\u000d', //   13 ^M CARRIAGE RETURN (CR)
    'S', 'O', '\u000e', //   14 ^N SHIFT OUT (SO)
    'S', 'I', '\u000f', //   15 ^O SHIFT IN (SI)
    'D', 'L', '\u0010', //   16 ^P DATALINK ESCAPE (DLE)
    'D', '1', '\u0011', //   17 ^Q DEVICE CONTROL ONE (DC1)
    'D', '2', '\u0012', //   18 ^R DEVICE CONTROL TWO (DC2)
    'D', '3', '\u0013', //   19 ^S DEVICE CONTROL THREE (DC3)
    'D', '4', '\u0014', //   20 ^T DEVICE CONTROL FOUR (DC4)
    'N', 'K', '\u0015', //   21 ^U NEGATIVE ACKNOWLEDGE (NAK)
    'S', 'Y', '\u0016', //   22 ^V SYNCHRONOUS IDLE (SYN)
    'E', 'B', '\u0017', //   23 ^W END OF TRANSMISSION BLOCK (ETB)
    'C', 'N', '\u0018', //   24 ^X CANCEL (CAN)
    'E', 'M', '\u0019', //   25 ^Y END OF MEDIUM (EM)
    'S', 'B', '\u001a', //   26 ^Z SUBSTITUTE (SUB)
    'E', 'C', '\u001b', //   27 ^[ ESCAPE (ESC)
    'F', 'S', '\u001c', //   28 ^\ FILE SEPARATOR (IS4)
    'G', 'S', '\u001d', //   29 ^] GROUP SEPARATOR (IS3)
    'R', 'S', '\u001e', //   30 ^^ RECORD SEPARATOR (IS2)
    'U', 'S', '\u001f', //   31 ^_ UNIT SEPARATOR (IS1)
    'S', 'P', '\u0020', //   32    SPACE
    'N', 'b', '\u0023', //   35 # NUMBER SIGN
    'D', 'O', '\u0024', //   36 $ DOLLAR SIGN
    'A', 't', '\u0040', //   64 @ COMMERCIAL AT
    '<', '(', '\u005b', //   91 [ LEFT SQUARE BRACKET
    '/', '/', '\u005c', //   92 \ REVERSE SOLIDUS
    ')', '>', '\u005d', //   93 ] RIGHT SQUARE BRACKET
    '\'', '>', '\u005e', //   94 ^ CIRCUMFLEX ACCENT
    '\'', '!', '\u0060', //   96 ` GRAVE ACCENT
    '(', '!', '\u007b', //  123 { LEFT CURLY BRACKET
    '!', '!', '\u007c', //  124 | VERTICAL LINE
    '!', ')', '\u007d', //  125 } RIGHT CURLY BRACKET
    '\'', '?', '\u007e', //  126 ~ TILDE
    'D', 'T', '\u007f', //  127 ^? DELETE (DEL)
    'P', 'A', '\u0080', //  128 ~@ PADDING CHARACTER (PAD)
    'H', 'O', '\u0081', //  129 ~A HIGH OCTET PRESET (HOP)
    'B', 'H', '\u0082', //  130 ~B BREAK PERMITTED HERE (BPH)
    'N', 'H', '\u0083', //  131 ~C NO BREAK HERE (NBH)
    'I', 'N', '\u0084', //  132 ~D INDEX (IND)
    'N', 'L', '\u0085', //  133 ~E NEXT LINE (NEL)
    'S', 'A', '\u0086', //  134 ~F START OF SELECTED AREA (SSA)
    'E', 'S', '\u0087', //  135 ~G END OF SELECTED AREA (ESA)
    'H', 'S', '\u0088', //  136 ~H CHARACTER TABULATION SET (HTS)
    'H', 'J', '\u0089', //  137 ~I CHARACTER TABULATION WITH JUSTIFICATION (HTJ)
    'V', 'S', '\u008a', //  138 ~J LINE TABULATION SET (VTS)
    'P', 'D', '\u008b', //  139 ~K PARTIAL LINE FORWARD (PLD)
    'P', 'U', '\u008c', //  140 ~L PARTIAL LINE BACKWARD (PLU)
    'R', 'I', '\u008d', //  141 ~M REVERSE LINE FEED (RI)
    'S', '2', '\u008e', //  142 ~N SINGLE-SHIFT TWO (SS2)
    'S', '3', '\u008f', //  143 ~O SINGLE-SHIFT THREE (SS3)
    'D', 'C', '\u0090', //  144 ~P DEVICE CONTROL STRING (DCS)
    'P', '1', '\u0091', //  145 ~Q PRIVATE USE ONE (PU1)
    'P', '2', '\u0092', //  146 ~R PRIVATE USE TWO (PU2)
    'T', 'S', '\u0093', //  147 ~S SET TRANSMIT STATE (STS)
    'C', 'C', '\u0094', //  148 ~T CANCEL CHARACTER (CCH)
    'M', 'W', '\u0095', //  149 ~U MESSAGE WAITING (MW)
    'S', 'G', '\u0096', //  150 ~V START OF GUARDED AREA (SPA)
    'E', 'G', '\u0097', //  151 ~W END OF GUARDED AREA (EPA)
    'S', 'S', '\u0098', //  152 ~X START OF STRING (SOS)
    'G', 'C', '\u0099', //  153 ~Y SINGLE GRAPHIC CHARACTER INTRODUCER (SGCI)
    'S', 'C', '\u009a', //  154 ~Z SINGLE CHARACTER INTRODUCER (SCI)
    'C', 'I', '\u009b', //  155 ~[ CONTROL SEQUENCE INTRODUCER (CSI)
    'S', 'T', '\u009c', //  156 ~\ STRING TERMINATOR (ST)
    'O', 'C', '\u009d', //  157 ~] OPERATING SYSTEM COMMAND (OSC)
    'P', 'M', '\u009e', //  158 ~^ PRIVACY MESSAGE (PM)
    'A', 'C', '\u009f', //  159 ~_ APPLICATION PROGRAM COMMAND (APC)
    'N', 'S', '\u00a0', //  160 | NO-BREAK SPACE
    '!', 'I', '\u00a1', //  161 ¡ INVERTED EXCLAMATION MARK
    '~', '!', '\u00a1', //  161 ¡ INVERTED EXCLAMATION MARK (Vim 5.x compatible)
    'C', 't', '\u00a2', //  162 ¢ CENT SIGN
    'c', '|', '\u00a2', //  162 ¢ CENT SIGN (Vim 5.x compatible)
    'P', 'd', '\u00a3', //  163 £ POUND SIGN
    '$', '$', '\u00a3', //  163 £ POUND SIGN (Vim 5.x compatible)
    'C', 'u', '\u00a4', //  164 ¤ CURRENCY SIGN
    'o', 'x', '\u00a4', //  164 ¤ CURRENCY SIGN (Vim 5.x compatible)
    'Y', 'e', '\u00a5', //  165 ¥ YEN SIGN
    'Y', '-', '\u00a5', //  165 ¥ YEN SIGN (Vim 5.x compatible)
    'B', 'B', '\u00a6', //  166 ¦ BROKEN BAR
    '|', '|', '\u00a6', //  166 ¦ BROKEN BAR (Vim 5.x compatible)
    'S', 'E', '\u00a7', //  167 § SECTION SIGN
    '\'', ':', '\u00a8', //  168 ¨ DIAERESIS
    'C', 'o', '\u00a9', //  169 © COPYRIGHT SIGN
    'c', 'O', '\u00a9', //  169 © COPYRIGHT SIGN (Vim 5.x compatible)
    '-', 'a', '\u00aa', //  170 ª FEMININE ORDINAL INDICATOR
    '<', '<', '\u00ab', //  171 « LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
    'N', 'O', '\u00ac', //  172 ¬ NOT SIGN
    '-', ',', '\u00ac', //  172 ¬ NOT SIGN (Vim 5.x compatible)
    '-', '-', '\u00ad', //  173 ­ SOFT HYPHEN
    'R', 'g', '\u00ae', //  174 ® REGISTERED SIGN
    '\'', 'm', '\u00af', //  175 ¯ MACRON
    '-', '=', '\u00af', //  175 ¯ MACRON (Vim 5.x compatible)
    'D', 'G', '\u00b0', //  176 ° DEGREE SIGN
    '~', 'o', '\u00b0', //  176 ° DEGREE SIGN (Vim 5.x compatible)
    '+', '-', '\u00b1', //  177 ± PLUS-MINUS SIGN
    '2', 'S', '\u00b2', //  178 ² SUPERSCRIPT TWO
    '2', '2', '\u00b2', //  178 ² SUPERSCRIPT TWO (Vim 5.x compatible)
    '3', 'S', '\u00b3', //  179 ³ SUPERSCRIPT THREE
    '3', '3', '\u00b3', //  179 ³ SUPERSCRIPT THREE (Vim 5.x compatible)
    '\'', '\'', '\u00b4', //  180 ´ ACUTE ACCENT
    'M', 'y', '\u00b5', //  181 µ MICRO SIGN
    'P', 'I', '\u00b6', //  182 ¶ PILCROW SIGN
    'p', 'p', '\u00b6', //  182 ¶ PILCROW SIGN (Vim 5.x compatible)
    '.', 'M', '\u00b7', //  183 · MIDDLE DOT
    '~', '.', '\u00b7', //  183 · MIDDLE DOT (Vim 5.x compatible)
    '\'', ',', '\u00b8', //  184 ¸ CEDILLA
    '1', 'S', '\u00b9', //  185 ¹ SUPERSCRIPT ONE
    '1', '1', '\u00b9', //  185 ¹ SUPERSCRIPT ONE (Vim 5.x compatible)
    '-', 'o', '\u00ba', //  186 º MASCULINE ORDINAL INDICATOR
    '>', '>', '\u00bb', //  187 » RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
    '1', '4', '\u00bc', //  188 ¼ VULGAR FRACTION ONE QUARTER
    '1', '2', '\u00bd', //  189 ½ VULGAR FRACTION ONE HALF
    '3', '4', '\u00be', //  190 ¾ VULGAR FRACTION THREE QUARTERS
    '?', 'I', '\u00bf', //  191 ¿ INVERTED QUESTION MARK
    '~', '?', '\u00bf', //  191 ¿ INVERTED QUESTION MARK (Vim 5.x compatible)
    'A', '!', '\u00c0', //  192 À LATIN CAPITAL LETTER A WITH GRAVE
    'A', '`', '\u00c0', //  192 À LATIN CAPITAL LETTER A WITH GRAVE (Vim 5.x compatible)
    'A', '\'', '\u00c1', //  193 Á LATIN CAPITAL LETTER A WITH ACUTE
    'A', '>', '\u00c2', //  194 Â LATIN CAPITAL LETTER A WITH CIRCUMFLEX
    'A', '^', '\u00c2', //  194 Â LATIN CAPITAL LETTER A WITH CIRCUMFLEX (Vim 5.x compatible)
    'A', '?', '\u00c3', //  195 Ã LATIN CAPITAL LETTER A WITH TILDE
    'A', '~', '\u00c3', //  195 Ã LATIN CAPITAL LETTER A WITH TILDE (Vim 5.x compatible)
    'A', ':', '\u00c4', //  196 Ä LATIN CAPITAL LETTER A WITH DIAERESIS
    'A', '"', '\u00c4', //  196 Ä LATIN CAPITAL LETTER A WITH DIAERESIS (Vim 5.x compatible)
    'A', 'A', '\u00c5', //  197 Å LATIN CAPITAL LETTER A WITH RING ABOVE
    'A', '@', '\u00c5', //  197 Å LATIN CAPITAL LETTER A WITH RING ABOVE (Vim 5.x compatible)
    'A', 'E', '\u00c6', //  198 Æ LATIN CAPITAL LETTER AE
    'C', ',', '\u00c7', //  199 Ç LATIN CAPITAL LETTER C WITH CEDILLA
    'E', '!', '\u00c8', //  200 È LATIN CAPITAL LETTER E WITH GRAVE
    'E', '`', '\u00c8', //  200 È LATIN CAPITAL LETTER E WITH GRAVE (Vim 5.x compatible)
    'E', '\'', '\u00c9', //  201 É LATIN CAPITAL LETTER E WITH ACUTE
    'E', '>', '\u00ca', //  202 Ê LATIN CAPITAL LETTER E WITH CIRCUMFLEX
    'E', '^', '\u00ca', //  202 Ê LATIN CAPITAL LETTER E WITH CIRCUMFLEX (Vim 5.x compatible)
    'E', ':', '\u00cb', //  203 Ë LATIN CAPITAL LETTER E WITH DIAERESIS
    'E', '"', '\u00cb', //  203 Ë LATIN CAPITAL LETTER E WITH DIAERESIS (Vim 5.x compatible)
    'I', '!', '\u00cc', //  204 Ì LATIN CAPITAL LETTER I WITH GRAVE
    'I', '`', '\u00cc', //  204 Ì LATIN CAPITAL LETTER I WITH GRAVE (Vim 5.x compatible)
    'I', '\'', '\u00cd', //  205 Í LATIN CAPITAL LETTER I WITH ACUTE
    'I', '>', '\u00ce', //  206 Î LATIN CAPITAL LETTER I WITH CIRCUMFLEX
    'I', '^', '\u00ce', //  206 Î LATIN CAPITAL LETTER I WITH CIRCUMFLEX (Vim 5.x compatible)
    'I', ':', '\u00cf', //  207 Ï LATIN CAPITAL LETTER I WITH DIAERESIS
    'I', '"', '\u00cf', //  207 Ï LATIN CAPITAL LETTER I WITH DIAERESIS (Vim 5.x compatible)
    'D', '-', '\u00d0', //  208 Ð LATIN CAPITAL LETTER ETH (Icelandic)
    'N', '?', '\u00d1', //  209 Ñ LATIN CAPITAL LETTER N WITH TILDE
    'N', '~', '\u00d1', //  209 Ñ LATIN CAPITAL LETTER N WITH TILDE (Vim 5.x compatible)
    'O', '!', '\u00d2', //  210 Ò LATIN CAPITAL LETTER O WITH GRAVE
    'O', '`', '\u00d2', //  210 Ò LATIN CAPITAL LETTER O WITH GRAVE (Vim 5.x compatible)
    'O', '\'', '\u00d3', //  211 Ó LATIN CAPITAL LETTER O WITH ACUTE
    'O', '>', '\u00d4', //  212 Ô LATIN CAPITAL LETTER O WITH CIRCUMFLEX
    'O', '^', '\u00d4', //  212 Ô LATIN CAPITAL LETTER O WITH CIRCUMFLEX (Vim 5.x compatible)
    'O', '?', '\u00d5', //  213 Õ LATIN CAPITAL LETTER O WITH TILDE
    'O', '~', '\u00d5', //  213 Õ LATIN CAPITAL LETTER O WITH TILDE (Vim 5.x compatible)
    'O', ':', '\u00d6', //  214 Ö LATIN CAPITAL LETTER O WITH DIAERESIS
    '*', 'X', '\u00d7', //  215 × MULTIPLICATION SIGN
    '/', '\\', '\u00d7', //  215 × MULTIPLICATION SIGN (Vim 5.x compatible)
    'O', '/', '\u00d8', //  216 Ø LATIN CAPITAL LETTER O WITH STROKE
    'U', '!', '\u00d9', //  217 Ù LATIN CAPITAL LETTER U WITH GRAVE
    'U', '`', '\u00d9', //  217 Ù LATIN CAPITAL LETTER U WITH GRAVE (Vim 5.x compatible)
    'U', '\'', '\u00da', //  218 Ú LATIN CAPITAL LETTER U WITH ACUTE
    'U', '>', '\u00db', //  219 Û LATIN CAPITAL LETTER U WITH CIRCUMFLEX
    'U', '^', '\u00db', //  219 Û LATIN CAPITAL LETTER U WITH CIRCUMFLEX (Vim 5.x compatible)
    'U', ':', '\u00dc', //  220 Ü LATIN CAPITAL LETTER U WITH DIAERESIS
    'Y', '\'', '\u00dd', //  221 Ý LATIN CAPITAL LETTER Y WITH ACUTE
    'T', 'H', '\u00de', //  222 Þ LATIN CAPITAL LETTER THORN (Icelandic)
    'I', 'p', '\u00de', //  222 Þ LATIN CAPITAL LETTER THORN (Icelandic) (Vim 5.x compatible)
    's', 's', '\u00df', //  223 ß LATIN SMALL LETTER SHARP S (German)
    'a', '!', '\u00e0', //  224 à LATIN SMALL LETTER A WITH GRAVE
    'a', '`', '\u00e0', //  224 à LATIN SMALL LETTER A WITH GRAVE (Vim 5.x compatible)
    'a', '\'', '\u00e1', //  225 á LATIN SMALL LETTER A WITH ACUTE
    'a', '>', '\u00e2', //  226 â LATIN SMALL LETTER A WITH CIRCUMFLEX
    'a', '^', '\u00e2', //  226 â LATIN SMALL LETTER A WITH CIRCUMFLEX (Vim 5.x compatible)
    'a', '?', '\u00e3', //  227 ã LATIN SMALL LETTER A WITH TILDE
    'a', '~', '\u00e3', //  227 ã LATIN SMALL LETTER A WITH TILDE (Vim 5.x compatible)
    'a', ':', '\u00e4', //  228 ä LATIN SMALL LETTER A WITH DIAERESIS
    'a', '"', '\u00e4', //  228 ä LATIN SMALL LETTER A WITH DIAERESIS (Vim 5.x compatible)
    'a', 'a', '\u00e5', //  229 å LATIN SMALL LETTER A WITH RING ABOVE
    'a', '@', '\u00e5', //  229 å LATIN SMALL LETTER A WITH RING ABOVE (Vim 5.x compatible)
    'a', 'e', '\u00e6', //  230 æ LATIN SMALL LETTER AE
    'c', ',', '\u00e7', //  231 ç LATIN SMALL LETTER C WITH CEDILLA
    'e', '!', '\u00e8', //  232 è LATIN SMALL LETTER E WITH GRAVE
    'e', '`', '\u00e8', //  232 è LATIN SMALL LETTER E WITH GRAVE (Vim 5.x compatible)
    'e', '\'', '\u00e9', //  233 é LATIN SMALL LETTER E WITH ACUTE
    'e', '>', '\u00ea', //  234 ê LATIN SMALL LETTER E WITH CIRCUMFLEX
    'e', '^', '\u00ea', //  234 ê LATIN SMALL LETTER E WITH CIRCUMFLEX (Vim 5.x compatible)
    'e', ':', '\u00eb', //  235 ë LATIN SMALL LETTER E WITH DIAERESIS
    'e', '"', '\u00eb', //  235 ë LATIN SMALL LETTER E WITH DIAERESIS (Vim 5.x compatible)
    'i', '!', '\u00ec', //  236 ì LATIN SMALL LETTER I WITH GRAVE
    'i', '`', '\u00ec', //  236 ì LATIN SMALL LETTER I WITH GRAVE (Vim 5.x compatible)
    'i', '\'', '\u00ed', //  237 í LATIN SMALL LETTER I WITH ACUTE
    'i', '>', '\u00ee', //  238 î LATIN SMALL LETTER I WITH CIRCUMFLEX
    'i', '^', '\u00ee', //  238 î LATIN SMALL LETTER I WITH CIRCUMFLEX (Vim 5.x compatible)
    'i', ':', '\u00ef', //  239 ï LATIN SMALL LETTER I WITH DIAERESIS
    'd', '-', '\u00f0', //  240 ð LATIN SMALL LETTER ETH (Icelandic)
    'n', '?', '\u00f1', //  241 ñ LATIN SMALL LETTER N WITH TILDE
    'n', '~', '\u00f1', //  241 ñ LATIN SMALL LETTER N WITH TILDE (Vim 5.x compatible)
    'o', '!', '\u00f2', //  242 ò LATIN SMALL LETTER O WITH GRAVE
    'o', '`', '\u00f2', //  242 ò LATIN SMALL LETTER O WITH GRAVE (Vim 5.x compatible)
    'o', '\'', '\u00f3', //  243 ó LATIN SMALL LETTER O WITH ACUTE
    'o', '>', '\u00f4', //  244 ô LATIN SMALL LETTER O WITH CIRCUMFLEX
    'o', '^', '\u00f4', //  244 ô LATIN SMALL LETTER O WITH CIRCUMFLEX (Vim 5.x compatible)
    'o', '?', '\u00f5', //  245 õ LATIN SMALL LETTER O WITH TILDE
    'o', '~', '\u00f5', //  245 õ LATIN SMALL LETTER O WITH TILDE (Vim 5.x compatible)
    'o', ':', '\u00f6', //  246 ö LATIN SMALL LETTER O WITH DIAERESIS
    '-', ':', '\u00f7', //  247 ÷ DIVISION SIGN
    'o', '/', '\u00f8', //  248 ø LATIN SMALL LETTER O WITH STROKE
    'u', '!', '\u00f9', //  249 ù LATIN SMALL LETTER U WITH GRAVE
    'u', '`', '\u00f9', //  249 ù LATIN SMALL LETTER U WITH GRAVE (Vim 5.x compatible)
    'u', '\'', '\u00fa', //  250 ú LATIN SMALL LETTER U WITH ACUTE
    'u', '>', '\u00fb', //  251 û LATIN SMALL LETTER U WITH CIRCUMFLEX
    'u', '^', '\u00fb', //  251 û LATIN SMALL LETTER U WITH CIRCUMFLEX (Vim 5.x compatible)
    'u', ':', '\u00fc', //  252 ü LATIN SMALL LETTER U WITH DIAERESIS
    'y', '\'', '\u00fd', //  253 ý LATIN SMALL LETTER Y WITH ACUTE
    't', 'h', '\u00fe', //  254 þ LATIN SMALL LETTER THORN (Icelandic)
    'y', ':', '\u00ff', //  255 ÿ LATIN SMALL LETTER Y WITH DIAERESIS
    'y', '"', '\u00ff', //  255 ÿ LATIN SMALL LETTER Y WITH DIAERESIS (Vim 5.x compatible)

    // See `:help digraph-table-mbyte`
    'A', '-', '\u0100', //  256 Ā LATIN CAPITAL LETTER A WITH MACRON
    'a', '-', '\u0101', //  257 ā LATIN SMALL LETTER A WITH MACRON
    'A', '(', '\u0102', //  258 Ă LATIN CAPITAL LETTER A WITH BREVE
    'a', '(', '\u0103', //  259 ă LATIN SMALL LETTER A WITH BREVE
    'A', ';', '\u0104', //  260 Ą LATIN CAPITAL LETTER A WITH OGONEK
    'a', ';', '\u0105', //  261 ą LATIN SMALL LETTER A WITH OGONEK
    'C', '\'', '\u0106', //  262 Ć LATIN CAPITAL LETTER C WITH ACUTE
    'c', '\'', '\u0107', //  263 ć LATIN SMALL LETTER C WITH ACUTE
    'C', '>', '\u0108', //  264 Ĉ LATIN CAPITAL LETTER C WITH CIRCUMFLEX
    'c', '>', '\u0109', //  265 ĉ LATIN SMALL LETTER C WITH CIRCUMFLEX
    'C', '.', '\u010a', //  266 Ċ LATIN CAPITAL LETTER C WITH DOT ABOVE
    'c', '.', '\u010b', //  267 ċ LATIN SMALL LETTER C WITH DOT ABOVE
    'C', '<', '\u010c', //  268 Č LATIN CAPITAL LETTER C WITH CARON
    'c', '<', '\u010d', //  269 č LATIN SMALL LETTER C WITH CARON
    'D', '<', '\u010e', //  270 Ď LATIN CAPITAL LETTER D WITH CARON
    'd', '<', '\u010f', //  271 ď LATIN SMALL LETTER D WITH CARON
    'D', '/', '\u0110', //  272 Đ LATIN CAPITAL LETTER D WITH STROKE
    'd', '/', '\u0111', //  273 đ LATIN SMALL LETTER D WITH STROKE
    'E', '-', '\u0112', //  274 Ē LATIN CAPITAL LETTER E WITH MACRON
    'e', '-', '\u0113', //  275 ē LATIN SMALL LETTER E WITH MACRON
    'E', '(', '\u0114', //  276 Ĕ LATIN CAPITAL LETTER E WITH BREVE
    'e', '(', '\u0115', //  277 ĕ LATIN SMALL LETTER E WITH BREVE
    'E', '.', '\u0116', //  278 Ė LATIN CAPITAL LETTER E WITH DOT ABOVE
    'e', '.', '\u0117', //  279 ė LATIN SMALL LETTER E WITH DOT ABOVE
    'E', ';', '\u0118', //  280 Ę LATIN CAPITAL LETTER E WITH OGONEK
    'e', ';', '\u0119', //  281 ę LATIN SMALL LETTER E WITH OGONEK
    'E', '<', '\u011a', //  282 Ě LATIN CAPITAL LETTER E WITH CARON
    'e', '<', '\u011b', //  283 ě LATIN SMALL LETTER E WITH CARON
    'G', '>', '\u011c', //  284 Ĝ LATIN CAPITAL LETTER G WITH CIRCUMFLEX
    'g', '>', '\u011d', //  285 ĝ LATIN SMALL LETTER G WITH CIRCUMFLEX
    'G', '(', '\u011e', //  286 Ğ LATIN CAPITAL LETTER G WITH BREVE
    'g', '(', '\u011f', //  287 ğ LATIN SMALL LETTER G WITH BREVE
    'G', '.', '\u0120', //  288 Ġ LATIN CAPITAL LETTER G WITH DOT ABOVE
    'g', '.', '\u0121', //  289 ġ LATIN SMALL LETTER G WITH DOT ABOVE
    'G', ',', '\u0122', //  290 Ģ LATIN CAPITAL LETTER G WITH CEDILLA
    'g', ',', '\u0123', //  291 ģ LATIN SMALL LETTER G WITH CEDILLA
    'H', '>', '\u0124', //  292 Ĥ LATIN CAPITAL LETTER H WITH CIRCUMFLEX
    'h', '>', '\u0125', //  293 ĥ LATIN SMALL LETTER H WITH CIRCUMFLEX
    'H', '/', '\u0126', //  294 Ħ LATIN CAPITAL LETTER H WITH STROKE
    'h', '/', '\u0127', //  295 ħ LATIN SMALL LETTER H WITH STROKE
    'I', '?', '\u0128', //  296 Ĩ LATIN CAPITAL LETTER I WITH TILDE
    'i', '?', '\u0129', //  297 ĩ LATIN SMALL LETTER I WITH TILDE
    'I', '-', '\u012a', //  298 Ī LATIN CAPITAL LETTER I WITH MACRON
    'i', '-', '\u012b', //  299 ī LATIN SMALL LETTER I WITH MACRON
    'I', '(', '\u012c', //  300 Ĭ LATIN CAPITAL LETTER I WITH BREVE
    'i', '(', '\u012d', //  301 ĭ LATIN SMALL LETTER I WITH BREVE
    'I', ';', '\u012e', //  302 Į LATIN CAPITAL LETTER I WITH OGONEK
    'i', ';', '\u012f', //  303 į LATIN SMALL LETTER I WITH OGONEK
    'I', '.', '\u0130', //  304 İ LATIN CAPITAL LETTER I WITH DOT ABOVE
    'i', '.', '\u0131', //  305 ı LATIN SMALL LETTER DOTLESS I
    'I', 'J', '\u0132', //  306 Ĳ LATIN CAPITAL LIGATURE IJ
    'i', 'j', '\u0133', //  307 ĳ LATIN SMALL LIGATURE IJ
    'J', '>', '\u0134', //  308 Ĵ LATIN CAPITAL LETTER J WITH CIRCUMFLEX
    'j', '>', '\u0135', //  309 ĵ LATIN SMALL LETTER J WITH CIRCUMFLEX
    'K', ',', '\u0136', //  310 Ķ LATIN CAPITAL LETTER K WITH CEDILLA
    'k', ',', '\u0137', //  311 ķ LATIN SMALL LETTER K WITH CEDILLA
    'k', 'k', '\u0138', //  312 ĸ LATIN SMALL LETTER KRA
    'L', '\'', '\u0139', //  313 Ĺ LATIN CAPITAL LETTER L WITH ACUTE
    'l', '\'', '\u013a', //  314 ĺ LATIN SMALL LETTER L WITH ACUTE
    'L', ',', '\u013b', //  315 Ļ LATIN CAPITAL LETTER L WITH CEDILLA
    'l', ',', '\u013c', //  316 ļ LATIN SMALL LETTER L WITH CEDILLA
    'L', '<', '\u013d', //  317 Ľ LATIN CAPITAL LETTER L WITH CARON
    'l', '<', '\u013e', //  318 ľ LATIN SMALL LETTER L WITH CARON
    'L', '.', '\u013f', //  319 Ŀ LATIN CAPITAL LETTER L WITH MIDDLE DOT
    'l', '.', '\u0140', //  320 ŀ LATIN SMALL LETTER L WITH MIDDLE DOT
    'L', '/', '\u0141', //  321 Ł LATIN CAPITAL LETTER L WITH STROKE
    'l', '/', '\u0142', //  322 ł LATIN SMALL LETTER L WITH STROKE
    'N', '\'', '\u0143', //  323 Ń LATIN CAPITAL LETTER N WITH ACUTE
    'n', '\'', '\u0144', //  324 ń LATIN SMALL LETTER N WITH ACUTE
    'N', ',', '\u0145', //  325 Ņ LATIN CAPITAL LETTER N WITH CEDILLA
    'n', ',', '\u0146', //  326 ņ LATIN SMALL LETTER N WITH CEDILLA
    'N', '<', '\u0147', //  327 Ň LATIN CAPITAL LETTER N WITH CARON
    'n', '<', '\u0148', //  328 ň LATIN SMALL LETTER N WITH CARON
    '\'', 'n', '\u0149', //  329 ŉ LATIN SMALL LETTER N PRECEDED BY APOSTROPHE
    'N', 'G', '\u014a', //  330 Ŋ LATIN CAPITAL LETTER ENG
    'n', 'g', '\u014b', //  331 ŋ LATIN SMALL LETTER ENG
    'O', '-', '\u014c', //  332 Ō LATIN CAPITAL LETTER O WITH MACRON
    'o', '-', '\u014d', //  333 ō LATIN SMALL LETTER O WITH MACRON
    'O', '(', '\u014e', //  334 Ŏ LATIN CAPITAL LETTER O WITH BREVE
    'o', '(', '\u014f', //  335 ŏ LATIN SMALL LETTER O WITH BREVE
    'O', '"', '\u0150', //  336 Ő LATIN CAPITAL LETTER O WITH DOUBLE ACUTE
    'o', '"', '\u0151', //  337 ő LATIN SMALL LETTER O WITH DOUBLE ACUTE
    'O', 'E', '\u0152', //  338 Œ LATIN CAPITAL LIGATURE OE
    'o', 'e', '\u0153', //  339 œ LATIN SMALL LIGATURE OE
    'R', '\'', '\u0154', //  340 Ŕ LATIN CAPITAL LETTER R WITH ACUTE
    'r', '\'', '\u0155', //  341 ŕ LATIN SMALL LETTER R WITH ACUTE
    'R', ',', '\u0156', //  342 Ŗ LATIN CAPITAL LETTER R WITH CEDILLA
    'r', ',', '\u0157', //  343 ŗ LATIN SMALL LETTER R WITH CEDILLA
    'R', '<', '\u0158', //  344 Ř LATIN CAPITAL LETTER R WITH CARON
    'r', '<', '\u0159', //  345 ř LATIN SMALL LETTER R WITH CARON
    'S', '\'', '\u015a', //  346 Ś LATIN CAPITAL LETTER S WITH ACUTE
    's', '\'', '\u015b', //  347 ś LATIN SMALL LETTER S WITH ACUTE
    'S', '>', '\u015c', //  348 Ŝ LATIN CAPITAL LETTER S WITH CIRCUMFLEX
    's', '>', '\u015d', //  349 ŝ LATIN SMALL LETTER S WITH CIRCUMFLEX
    'S', ',', '\u015e', //  350 Ş LATIN CAPITAL LETTER S WITH CEDILLA
    's', ',', '\u015f', //  351 ş LATIN SMALL LETTER S WITH CEDILLA
    'S', '<', '\u0160', //  352 Š LATIN CAPITAL LETTER S WITH CARON
    's', '<', '\u0161', //  353 š LATIN SMALL LETTER S WITH CARON
    'T', ',', '\u0162', //  354 Ţ LATIN CAPITAL LETTER T WITH CEDILLA
    't', ',', '\u0163', //  355 ţ LATIN SMALL LETTER T WITH CEDILLA
    'T', '<', '\u0164', //  356 Ť LATIN CAPITAL LETTER T WITH CARON
    't', '<', '\u0165', //  357 ť LATIN SMALL LETTER T WITH CARON
    'T', '/', '\u0166', //  358 Ŧ LATIN CAPITAL LETTER T WITH STROKE
    't', '/', '\u0167', //  359 ŧ LATIN SMALL LETTER T WITH STROKE
    'U', '?', '\u0168', //  360 Ũ LATIN CAPITAL LETTER U WITH TILDE
    'u', '?', '\u0169', //  361 ũ LATIN SMALL LETTER U WITH TILDE
    'U', '-', '\u016a', //  362 Ū LATIN CAPITAL LETTER U WITH MACRON
    'u', '-', '\u016b', //  363 ū LATIN SMALL LETTER U WITH MACRON
    'U', '(', '\u016c', //  364 Ŭ LATIN CAPITAL LETTER U WITH BREVE
    'u', '(', '\u016d', //  365 ŭ LATIN SMALL LETTER U WITH BREVE
    'U', '0', '\u016e', //  366 Ů LATIN CAPITAL LETTER U WITH RING ABOVE
    'u', '0', '\u016f', //  367 ů LATIN SMALL LETTER U WITH RING ABOVE
    'U', '"', '\u0170', //  368 Ű LATIN CAPITAL LETTER U WITH DOUBLE ACUTE
    'u', '"', '\u0171', //  369 ű LATIN SMALL LETTER U WITH DOUBLE ACUTE
    'U', ';', '\u0172', //  370 Ų LATIN CAPITAL LETTER U WITH OGONEK
    'u', ';', '\u0173', //  371 ų LATIN SMALL LETTER U WITH OGONEK
    'W', '>', '\u0174', //  372 Ŵ LATIN CAPITAL LETTER W WITH CIRCUMFLEX
    'w', '>', '\u0175', //  373 ŵ LATIN SMALL LETTER W WITH CIRCUMFLEX
    'Y', '>', '\u0176', //  374 Ŷ LATIN CAPITAL LETTER Y WITH CIRCUMFLEX
    'y', '>', '\u0177', //  375 ŷ LATIN SMALL LETTER Y WITH CIRCUMFLEX
    'Y', ':', '\u0178', //  376 Ÿ LATIN CAPITAL LETTER Y WITH DIAERESIS
    'Z', '\'', '\u0179', //  377 Ź LATIN CAPITAL LETTER Z WITH ACUTE
    'z', '\'', '\u017a', //  378 ź LATIN SMALL LETTER Z WITH ACUTE
    'Z', '.', '\u017b', //  379 Ż LATIN CAPITAL LETTER Z WITH DOT ABOVE
    'z', '.', '\u017c', //  380 ż LATIN SMALL LETTER Z WITH DOT ABOVE
    'Z', '<', '\u017d', //  381 Ž LATIN CAPITAL LETTER Z WITH CARON
    'z', '<', '\u017e', //  382 ž LATIN SMALL LETTER Z WITH CARON
    'O', '9', '\u01a0', //  416 Ơ LATIN CAPITAL LETTER O WITH HORN
    'o', '9', '\u01a1', //  417 ơ LATIN SMALL LETTER O WITH HORN
    'O', 'I', '\u01a2', //  418 Ƣ LATIN CAPITAL LETTER OI
    'o', 'i', '\u01a3', //  419 ƣ LATIN SMALL LETTER OI
    'y', 'r', '\u01a6', //  422 Ʀ LATIN LETTER YR
    'U', '9', '\u01af', //  431 Ư LATIN CAPITAL LETTER U WITH HORN
    'u', '9', '\u01b0', //  432 ư LATIN SMALL LETTER U WITH HORN
    'Z', '/', '\u01b5', //  437 Ƶ LATIN CAPITAL LETTER Z WITH STROKE
    'z', '/', '\u01b6', //  438 ƶ LATIN SMALL LETTER Z WITH STROKE
    'E', 'D', '\u01b7', //  439 Ʒ LATIN CAPITAL LETTER EZH
    'A', '<', '\u01cd', //  461 Ǎ LATIN CAPITAL LETTER A WITH CARON
    'a', '<', '\u01ce', //  462 ǎ LATIN SMALL LETTER A WITH CARON
    'I', '<', '\u01cf', //  463 Ǐ LATIN CAPITAL LETTER I WITH CARON
    'i', '<', '\u01d0', //  464 ǐ LATIN SMALL LETTER I WITH CARON
    'O', '<', '\u01d1', //  465 Ǒ LATIN CAPITAL LETTER O WITH CARON
    'o', '<', '\u01d2', //  466 ǒ LATIN SMALL LETTER O WITH CARON
    'U', '<', '\u01d3', //  467 Ǔ LATIN CAPITAL LETTER U WITH CARON
    'u', '<', '\u01d4', //  468 ǔ LATIN SMALL LETTER U WITH CARON
    'A', '1', '\u01de', //  478 Ǟ LATIN CAPITAL LETTER A WITH DIAERESIS AND MACRON
    'a', '1', '\u01df', //  479 ǟ LATIN SMALL LETTER A WITH DIAERESIS AND MACRON
    'A', '7', '\u01e0', //  480 Ǡ LATIN CAPITAL LETTER A WITH DOT ABOVE AND MACRON
    'a', '7', '\u01e1', //  481 ǡ LATIN SMALL LETTER A WITH DOT ABOVE AND MACRON
    'A', '3', '\u01e2', //  482 Ǣ LATIN CAPITAL LETTER AE WITH MACRON
    'a', '3', '\u01e3', //  483 ǣ LATIN SMALL LETTER AE WITH MACRON
    'G', '/', '\u01e4', //  484 Ǥ LATIN CAPITAL LETTER G WITH STROKE
    'g', '/', '\u01e5', //  485 ǥ LATIN SMALL LETTER G WITH STROKE
    'G', '<', '\u01e6', //  486 Ǧ LATIN CAPITAL LETTER G WITH CARON
    'g', '<', '\u01e7', //  487 ǧ LATIN SMALL LETTER G WITH CARON
    'K', '<', '\u01e8', //  488 Ǩ LATIN CAPITAL LETTER K WITH CARON
    'k', '<', '\u01e9', //  489 ǩ LATIN SMALL LETTER K WITH CARON
    'O', ';', '\u01ea', //  490 Ǫ LATIN CAPITAL LETTER O WITH OGONEK
    'o', ';', '\u01eb', //  491 ǫ LATIN SMALL LETTER O WITH OGONEK
    'O', '1', '\u01ec', //  492 Ǭ LATIN CAPITAL LETTER O WITH OGONEK AND MACRON
    'o', '1', '\u01ed', //  493 ǭ LATIN SMALL LETTER O WITH OGONEK AND MACRON
    'E', 'Z', '\u01ee', //  494 Ǯ LATIN CAPITAL LETTER EZH WITH CARON
    'e', 'z', '\u01ef', //  495 ǯ LATIN SMALL LETTER EZH WITH CARON
    'j', '<', '\u01f0', //  496 ǰ LATIN SMALL LETTER J WITH CARON
    'G', '\'', '\u01f4', //  500 Ǵ LATIN CAPITAL LETTER G WITH ACUTE
    'g', '\'', '\u01f5', //  501 ǵ LATIN SMALL LETTER G WITH ACUTE
    ';', 'S', '\u02bf', //  703 ʿ MODIFIER LETTER LEFT HALF RING
    '\'', '<', '\u02c7', //  711 ˇ CARON
    '\'', '(', '\u02d8', //  728 ˘ BREVE
    '\'', '.', '\u02d9', //  729 ˙ DOT ABOVE
    '\'', '0', '\u02da', //  730 ˚ RING ABOVE
    '\'', ';', '\u02db', //  731 ˛ OGONEK
    '\'', '"', '\u02dd', //  733 ˝ DOUBLE ACUTE ACCENT
    'A', '%', '\u0386', //  902 Ά GREEK CAPITAL LETTER ALPHA WITH TONOS
    'E', '%', '\u0388', //  904 Έ GREEK CAPITAL LETTER EPSILON WITH TONOS
    'Y', '%', '\u0389', //  905 Ή GREEK CAPITAL LETTER ETA WITH TONOS
    'I', '%', '\u038a', //  906 Ί GREEK CAPITAL LETTER IOTA WITH TONOS
    'O', '%', '\u038c', //  908 Ό GREEK CAPITAL LETTER OMICRON WITH TONOS
    'U', '%', '\u038e', //  910 Ύ GREEK CAPITAL LETTER UPSILON WITH TONOS
    'W', '%', '\u038f', //  911 Ώ GREEK CAPITAL LETTER OMEGA WITH TONOS
    'i', '3', '\u0390', //  912 ΐ GREEK SMALL LETTER IOTA WITH DIALYTIKA AND TONOS
    'A', '*', '\u0391', //  913 Α GREEK CAPITAL LETTER ALPHA
    'B', '*', '\u0392', //  914 Β GREEK CAPITAL LETTER BETA
    'G', '*', '\u0393', //  915 Γ GREEK CAPITAL LETTER GAMMA
    'D', '*', '\u0394', //  916 Δ GREEK CAPITAL LETTER DELTA
    'E', '*', '\u0395', //  917 Ε GREEK CAPITAL LETTER EPSILON
    'Z', '*', '\u0396', //  918 Ζ GREEK CAPITAL LETTER ZETA
    'Y', '*', '\u0397', //  919 Η GREEK CAPITAL LETTER ETA
    'H', '*', '\u0398', //  920 Θ GREEK CAPITAL LETTER THETA
    'I', '*', '\u0399', //  921 Ι GREEK CAPITAL LETTER IOTA
    'K', '*', '\u039a', //  922 Κ GREEK CAPITAL LETTER KAPPA
    'L', '*', '\u039b', //  923 Λ GREEK CAPITAL LETTER LAMDA (aka LAMBDA)
    'M', '*', '\u039c', //  924 Μ GREEK CAPITAL LETTER MU
    'N', '*', '\u039d', //  925 Ν GREEK CAPITAL LETTER NU
    'C', '*', '\u039e', //  926 Ξ GREEK CAPITAL LETTER XI
    'O', '*', '\u039f', //  927 Ο GREEK CAPITAL LETTER OMICRON
    'P', '*', '\u03a0', //  928 Π GREEK CAPITAL LETTER PI
    'R', '*', '\u03a1', //  929 Ρ GREEK CAPITAL LETTER RHO
    'S', '*', '\u03a3', //  931 Σ GREEK CAPITAL LETTER SIGMA
    'T', '*', '\u03a4', //  932 Τ GREEK CAPITAL LETTER TAU
    'U', '*', '\u03a5', //  933 Υ GREEK CAPITAL LETTER UPSILON
    'F', '*', '\u03a6', //  934 Φ GREEK CAPITAL LETTER PHI
    'X', '*', '\u03a7', //  935 Χ GREEK CAPITAL LETTER CHI
    'Q', '*', '\u03a8', //  936 Ψ GREEK CAPITAL LETTER PSI
    'W', '*', '\u03a9', //  937 Ω GREEK CAPITAL LETTER OMEGA
    'J', '*', '\u03aa', //  938 Ϊ GREEK CAPITAL LETTER IOTA WITH DIALYTIKA
    'V', '*', '\u03ab', //  939 Ϋ GREEK CAPITAL LETTER UPSILON WITH DIALYTIKA
    'a', '%', '\u03ac', //  940 ά GREEK SMALL LETTER ALPHA WITH TONOS
    'e', '%', '\u03ad', //  941 έ GREEK SMALL LETTER EPSILON WITH TONOS
    'y', '%', '\u03ae', //  942 ή GREEK SMALL LETTER ETA WITH TONOS
    'i', '%', '\u03af', //  943 ί GREEK SMALL LETTER IOTA WITH TONOS
    'u', '3', '\u03b0', //  944 ΰ GREEK SMALL LETTER UPSILON WITH DIALYTIKA AND TONOS
    'a', '*', '\u03b1', //  945 α GREEK SMALL LETTER ALPHA
    'b', '*', '\u03b2', //  946 β GREEK SMALL LETTER BETA
    'g', '*', '\u03b3', //  947 γ GREEK SMALL LETTER GAMMA
    'd', '*', '\u03b4', //  948 δ GREEK SMALL LETTER DELTA
    'e', '*', '\u03b5', //  949 ε GREEK SMALL LETTER EPSILON
    'z', '*', '\u03b6', //  950 ζ GREEK SMALL LETTER ZETA
    'y', '*', '\u03b7', //  951 η GREEK SMALL LETTER ETA
    'h', '*', '\u03b8', //  952 θ GREEK SMALL LETTER THETA
    'i', '*', '\u03b9', //  953 ι GREEK SMALL LETTER IOTA
    'k', '*', '\u03ba', //  954 κ GREEK SMALL LETTER KAPPA
    'l', '*', '\u03bb', //  955 λ GREEK SMALL LETTER LAMDA (aka LAMBDA)
    'm', '*', '\u03bc', //  956 μ GREEK SMALL LETTER MU
    'n', '*', '\u03bd', //  957 ν GREEK SMALL LETTER NU
    'c', '*', '\u03be', //  958 ξ GREEK SMALL LETTER XI
    'o', '*', '\u03bf', //  959 ο GREEK SMALL LETTER OMICRON
    'p', '*', '\u03c0', //  960 π GREEK SMALL LETTER PI
    'r', '*', '\u03c1', //  961 ρ GREEK SMALL LETTER RHO
    '*', 's', '\u03c2', //  962 ς GREEK SMALL LETTER FINAL SIGMA
    's', '*', '\u03c3', //  963 σ GREEK SMALL LETTER SIGMA
    't', '*', '\u03c4', //  964 τ GREEK SMALL LETTER TAU
    'u', '*', '\u03c5', //  965 υ GREEK SMALL LETTER UPSILON
    'f', '*', '\u03c6', //  966 φ GREEK SMALL LETTER PHI
    'x', '*', '\u03c7', //  967 χ GREEK SMALL LETTER CHI
    'q', '*', '\u03c8', //  968 ψ GREEK SMALL LETTER PSI
    'w', '*', '\u03c9', //  969 ω GREEK SMALL LETTER OMEGA
    'j', '*', '\u03ca', //  970 ϊ GREEK SMALL LETTER IOTA WITH DIALYTIKA
    'v', '*', '\u03cb', //  971 ϋ GREEK SMALL LETTER UPSILON WITH DIALYTIKA
    'o', '%', '\u03cc', //  972 ό GREEK SMALL LETTER OMICRON WITH TONOS
    'u', '%', '\u03cd', //  973 ύ GREEK SMALL LETTER UPSILON WITH TONOS
    'w', '%', '\u03ce', //  974 ώ GREEK SMALL LETTER OMEGA WITH TONOS
    '\'', 'G', '\u03d8', //  984 Ϙ GREEK LETTER ARCHAIC KOPPA
    ',', 'G', '\u03d9', //  985 ϙ GREEK SMALL LETTER ARCHAIC KOPPA
    'T', '3', '\u03da', //  986 Ϛ GREEK LETTER STIGMA
    't', '3', '\u03db', //  987 ϛ GREEK SMALL LETTER STIGMA
    'M', '3', '\u03dc', //  988 Ϝ GREEK LETTER DIGAMMA
    'm', '3', '\u03dd', //  989 ϝ GREEK SMALL LETTER DIGAMMA
    'K', '3', '\u03de', //  990 Ϟ GREEK LETTER KOPPA
    'k', '3', '\u03df', //  991 ϟ GREEK SMALL LETTER KOPPA
    'P', '3', '\u03e0', //  992 Ϡ GREEK LETTER SAMPI
    'p', '3', '\u03e1', //  993 ϡ GREEK SMALL LETTER SAMPI
    '\'', '%', '\u03f4', // 1012 ϴ GREEK CAPITAL THETA SYMBOL
    'j', '3', '\u03f5', // 1013 ϵ GREEK LUNATE EPSILON SYMBOL
    'I', 'O', '\u0401', // 1025 Ё CYRILLIC CAPITAL LETTER IO
    'D', '%', '\u0402', // 1026 Ђ CYRILLIC CAPITAL LETTER DJE
    'G', '%', '\u0403', // 1027 Ѓ CYRILLIC CAPITAL LETTER GJE
    'I', 'E', '\u0404', // 1028 Є CYRILLIC CAPITAL LETTER UKRAINIAN IE
    'D', 'S', '\u0405', // 1029 Ѕ CYRILLIC CAPITAL LETTER DZE
    'I', 'I', '\u0406', // 1030 І CYRILLIC CAPITAL LETTER BYELORUSSIAN-UKRAINIAN I
    'Y', 'I', '\u0407', // 1031 Ї CYRILLIC CAPITAL LETTER YI
    'J', '%', '\u0408', // 1032 Ј CYRILLIC CAPITAL LETTER JE
    'L', 'J', '\u0409', // 1033 Љ CYRILLIC CAPITAL LETTER LJE
    'N', 'J', '\u040a', // 1034 Њ CYRILLIC CAPITAL LETTER NJE
    'T', 's', '\u040b', // 1035 Ћ CYRILLIC CAPITAL LETTER TSHE
    'K', 'J', '\u040c', // 1036 Ќ CYRILLIC CAPITAL LETTER KJE
    'V', '%', '\u040e', // 1038 Ў CYRILLIC CAPITAL LETTER SHORT U
    'D', 'Z', '\u040f', // 1039 Џ CYRILLIC CAPITAL LETTER DZHE
    'A', '=', '\u0410', // 1040 А CYRILLIC CAPITAL LETTER A
    'B', '=', '\u0411', // 1041 Б CYRILLIC CAPITAL LETTER BE
    'V', '=', '\u0412', // 1042 В CYRILLIC CAPITAL LETTER VE
    'G', '=', '\u0413', // 1043 Г CYRILLIC CAPITAL LETTER GHE
    'D', '=', '\u0414', // 1044 Д CYRILLIC CAPITAL LETTER DE
    'E', '=', '\u0415', // 1045 Е CYRILLIC CAPITAL LETTER IE
    'Z', '%', '\u0416', // 1046 Ж CYRILLIC CAPITAL LETTER ZHE
    'Z', '=', '\u0417', // 1047 З CYRILLIC CAPITAL LETTER ZE
    'I', '=', '\u0418', // 1048 И CYRILLIC CAPITAL LETTER I
    'J', '=', '\u0419', // 1049 Й CYRILLIC CAPITAL LETTER SHORT I
    'K', '=', '\u041a', // 1050 К CYRILLIC CAPITAL LETTER KA
    'L', '=', '\u041b', // 1051 Л CYRILLIC CAPITAL LETTER EL
    'M', '=', '\u041c', // 1052 М CYRILLIC CAPITAL LETTER EM
    'N', '=', '\u041d', // 1053 Н CYRILLIC CAPITAL LETTER EN
    'O', '=', '\u041e', // 1054 О CYRILLIC CAPITAL LETTER O
    'P', '=', '\u041f', // 1055 П CYRILLIC CAPITAL LETTER PE
    'R', '=', '\u0420', // 1056 Р CYRILLIC CAPITAL LETTER ER
    'S', '=', '\u0421', // 1057 С CYRILLIC CAPITAL LETTER ES
    'T', '=', '\u0422', // 1058 Т CYRILLIC CAPITAL LETTER TE
    'U', '=', '\u0423', // 1059 У CYRILLIC CAPITAL LETTER U
    'F', '=', '\u0424', // 1060 Ф CYRILLIC CAPITAL LETTER EF
    'H', '=', '\u0425', // 1061 Х CYRILLIC CAPITAL LETTER HA
    'C', '=', '\u0426', // 1062 Ц CYRILLIC CAPITAL LETTER TSE
    'C', '%', '\u0427', // 1063 Ч CYRILLIC CAPITAL LETTER CHE
    'S', '%', '\u0428', // 1064 Ш CYRILLIC CAPITAL LETTER SHA
    'S', 'c', '\u0429', // 1065 Щ CYRILLIC CAPITAL LETTER SHCHA
    '=', '"', '\u042a', // 1066 Ъ CYRILLIC CAPITAL LETTER HARD SIGN
    'Y', '=', '\u042b', // 1067 Ы CYRILLIC CAPITAL LETTER YERU
    '%', '"', '\u042c', // 1068 Ь CYRILLIC CAPITAL LETTER SOFT SIGN
    'J', 'E', '\u042d', // 1069 Э CYRILLIC CAPITAL LETTER E
    'J', 'U', '\u042e', // 1070 Ю CYRILLIC CAPITAL LETTER YU
    'J', 'A', '\u042f', // 1071 Я CYRILLIC CAPITAL LETTER YA
    'a', '=', '\u0430', // 1072 а CYRILLIC SMALL LETTER A
    'b', '=', '\u0431', // 1073 б CYRILLIC SMALL LETTER BE
    'v', '=', '\u0432', // 1074 в CYRILLIC SMALL LETTER VE
    'g', '=', '\u0433', // 1075 г CYRILLIC SMALL LETTER GHE
    'd', '=', '\u0434', // 1076 д CYRILLIC SMALL LETTER DE
    'e', '=', '\u0435', // 1077 е CYRILLIC SMALL LETTER IE
    'z', '%', '\u0436', // 1078 ж CYRILLIC SMALL LETTER ZHE
    'z', '=', '\u0437', // 1079 з CYRILLIC SMALL LETTER ZE
    'i', '=', '\u0438', // 1080 и CYRILLIC SMALL LETTER I
    'j', '=', '\u0439', // 1081 й CYRILLIC SMALL LETTER SHORT I
    'k', '=', '\u043a', // 1082 к CYRILLIC SMALL LETTER KA
    'l', '=', '\u043b', // 1083 л CYRILLIC SMALL LETTER EL
    'm', '=', '\u043c', // 1084 м CYRILLIC SMALL LETTER EM
    'n', '=', '\u043d', // 1085 н CYRILLIC SMALL LETTER EN
    'o', '=', '\u043e', // 1086 о CYRILLIC SMALL LETTER O
    'p', '=', '\u043f', // 1087 п CYRILLIC SMALL LETTER PE
    'r', '=', '\u0440', // 1088 р CYRILLIC SMALL LETTER ER
    's', '=', '\u0441', // 1089 с CYRILLIC SMALL LETTER ES
    't', '=', '\u0442', // 1090 т CYRILLIC SMALL LETTER TE
    'u', '=', '\u0443', // 1091 у CYRILLIC SMALL LETTER U
    'f', '=', '\u0444', // 1092 ф CYRILLIC SMALL LETTER EF
    'h', '=', '\u0445', // 1093 х CYRILLIC SMALL LETTER HA
    'c', '=', '\u0446', // 1094 ц CYRILLIC SMALL LETTER TSE
    'c', '%', '\u0447', // 1095 ч CYRILLIC SMALL LETTER CHE
    's', '%', '\u0448', // 1096 ш CYRILLIC SMALL LETTER SHA
    's', 'c', '\u0449', // 1097 щ CYRILLIC SMALL LETTER SHCHA
    '=', '\'', '\u044a', // 1098 ъ CYRILLIC SMALL LETTER HARD SIGN
    'y', '=', '\u044b', // 1099 ы CYRILLIC SMALL LETTER YERU
    '%', '\'', '\u044c', // 1100 ь CYRILLIC SMALL LETTER SOFT SIGN
    'j', 'e', '\u044d', // 1101 э CYRILLIC SMALL LETTER E
    'j', 'u', '\u044e', // 1102 ю CYRILLIC SMALL LETTER YU
    'j', 'a', '\u044f', // 1103 я CYRILLIC SMALL LETTER YA
    'i', 'o', '\u0451', // 1105 ё CYRILLIC SMALL LETTER IO
    'd', '%', '\u0452', // 1106 ђ CYRILLIC SMALL LETTER DJE
    'g', '%', '\u0453', // 1107 ѓ CYRILLIC SMALL LETTER GJE
    'i', 'e', '\u0454', // 1108 є CYRILLIC SMALL LETTER UKRAINIAN IE
    'd', 's', '\u0455', // 1109 ѕ CYRILLIC SMALL LETTER DZE
    'i', 'i', '\u0456', // 1110 і CYRILLIC SMALL LETTER BYELORUSSIAN-UKRAINIAN I
    'y', 'i', '\u0457', // 1111 ї CYRILLIC SMALL LETTER YI
    'j', '%', '\u0458', // 1112 ј CYRILLIC SMALL LETTER JE
    'l', 'j', '\u0459', // 1113 љ CYRILLIC SMALL LETTER LJE
    'n', 'j', '\u045a', // 1114 њ CYRILLIC SMALL LETTER NJE
    't', 's', '\u045b', // 1115 ћ CYRILLIC SMALL LETTER TSHE
    'k', 'j', '\u045c', // 1116 ќ CYRILLIC SMALL LETTER KJE
    'v', '%', '\u045e', // 1118 ў CYRILLIC SMALL LETTER SHORT U
    'd', 'z', '\u045f', // 1119 џ CYRILLIC SMALL LETTER DZHE
    'Y', '3', '\u0462', // 1122 Ѣ CYRILLIC CAPITAL LETTER YAT
    'y', '3', '\u0463', // 1123 ѣ CYRILLIC SMALL LETTER YAT
    'O', '3', '\u046a', // 1130 Ѫ CYRILLIC CAPITAL LETTER BIG YUS
    'o', '3', '\u046b', // 1131 ѫ CYRILLIC SMALL LETTER BIG YUS
    'F', '3', '\u0472', // 1138 Ѳ CYRILLIC CAPITAL LETTER FITA
    'f', '3', '\u0473', // 1139 ѳ CYRILLIC SMALL LETTER FITA
    'V', '3', '\u0474', // 1140 Ѵ CYRILLIC CAPITAL LETTER IZHITSA
    'v', '3', '\u0475', // 1141 ѵ CYRILLIC SMALL LETTER IZHITSA
    'C', '3', '\u0480', // 1152 Ҁ CYRILLIC CAPITAL LETTER KOPPA
    'c', '3', '\u0481', // 1153 ҁ CYRILLIC SMALL LETTER KOPPA
    'G', '3', '\u0490', // 1168 Ґ CYRILLIC CAPITAL LETTER GHE WITH UPTURN
    'g', '3', '\u0491', // 1169 ґ CYRILLIC SMALL LETTER GHE WITH UPTURN
    'A', '+', '\u05d0', // 1488 א HEBREW LETTER ALEF
    'B', '+', '\u05d1', // 1489 ב HEBREW LETTER BET
    'G', '+', '\u05d2', // 1490 ג HEBREW LETTER GIMEL
    'D', '+', '\u05d3', // 1491 ד HEBREW LETTER DALET
    'H', '+', '\u05d4', // 1492 ה HEBREW LETTER HE
    'W', '+', '\u05d5', // 1493 ו HEBREW LETTER VAV
    'Z', '+', '\u05d6', // 1494 ז HEBREW LETTER ZAYIN
    'X', '+', '\u05d7', // 1495 ח HEBREW LETTER HET
    'T', 'j', '\u05d8', // 1496 ט HEBREW LETTER TET
    'J', '+', '\u05d9', // 1497 י HEBREW LETTER YOD
    'K', '%', '\u05da', // 1498 ך HEBREW LETTER FINAL KAF
    'K', '+', '\u05db', // 1499 כ HEBREW LETTER KAF
    'L', '+', '\u05dc', // 1500 ל HEBREW LETTER LAMED
    'M', '%', '\u05dd', // 1501 ם HEBREW LETTER FINAL MEM
    'M', '+', '\u05de', // 1502 מ HEBREW LETTER MEM
    'N', '%', '\u05df', // 1503 ן HEBREW LETTER FINAL NUN `
    'N', '+', '\u05e0', // 1504 נ HEBREW LETTER NUN `
    'S', '+', '\u05e1', // 1505 ס HEBREW LETTER SAMEKH
    'E', '+', '\u05e2', // 1506 ע HEBREW LETTER AYIN
    'P', '%', '\u05e3', // 1507 ף HEBREW LETTER FINAL PE
    'P', '+', '\u05e4', // 1508 פ HEBREW LETTER PE
    'Z', 'j', '\u05e5', // 1509 ץ HEBREW LETTER FINAL TSADI
    'Z', 'J', '\u05e6', // 1510 צ HEBREW LETTER TSADI
    'Q', '+', '\u05e7', // 1511 ק HEBREW LETTER QOF
    'R', '+', '\u05e8', // 1512 ר HEBREW LETTER RESH
    'S', 'h', '\u05e9', // 1513 ש HEBREW LETTER SHIN
    'T', '+', '\u05ea', // 1514 ת HEBREW LETTER TAV
    ',', '+', '\u060c', // 1548 ، ARABIC COMMA
    ';', '+', '\u061b', // 1563 ؛ ARABIC SEMICOLON
    '?', '+', '\u061f', // 1567 ؟ ARABIC QUESTION MARK
    'H', '\'', '\u0621', // 1569 ء ARABIC LETTER HAMZA
    'a', 'M', '\u0622', // 1570 آ ARABIC LETTER ALEF WITH MADDA ABOVE
    'a', 'H', '\u0623', // 1571 أ ARABIC LETTER ALEF WITH HAMZA ABOVE
    'w', 'H', '\u0624', // 1572 ؤ ARABIC LETTER WAW WITH HAMZA ABOVE
    'a', 'h', '\u0625', // 1573 إ ARABIC LETTER ALEF WITH HAMZA BELOW
    'y', 'H', '\u0626', // 1574 ئ ARABIC LETTER YEH WITH HAMZA ABOVE
    'a', '+', '\u0627', // 1575 ا ARABIC LETTER ALEF
    'b', '+', '\u0628', // 1576 ب ARABIC LETTER BEH
    't', 'm', '\u0629', // 1577 ة ARABIC LETTER TEH MARBUTA
    't', '+', '\u062a', // 1578 ت ARABIC LETTER TEH
    't', 'k', '\u062b', // 1579 ث ARABIC LETTER THEH
    'g', '+', '\u062c', // 1580 ج ARABIC LETTER JEEM
    'h', 'k', '\u062d', // 1581 ح ARABIC LETTER HAH
    'x', '+', '\u062e', // 1582 خ ARABIC LETTER KHAH
    'd', '+', '\u062f', // 1583 د ARABIC LETTER DAL
    'd', 'k', '\u0630', // 1584 ذ ARABIC LETTER THAL
    'r', '+', '\u0631', // 1585 ر ARABIC LETTER REH
    'z', '+', '\u0632', // 1586 ز ARABIC LETTER ZAIN
    's', '+', '\u0633', // 1587 س ARABIC LETTER SEEN
    's', 'n', '\u0634', // 1588 ش ARABIC LETTER SHEEN
    'c', '+', '\u0635', // 1589 ص ARABIC LETTER SAD
    'd', 'd', '\u0636', // 1590 ض ARABIC LETTER DAD
    't', 'j', '\u0637', // 1591 ط ARABIC LETTER TAH
    'z', 'H', '\u0638', // 1592 ظ ARABIC LETTER ZAH
    'e', '+', '\u0639', // 1593 ع ARABIC LETTER AIN
    'i', '+', '\u063a', // 1594 غ ARABIC LETTER GHAIN
    '+', '+', '\u0640', // 1600 ـ ARABIC TATWEEL
    'f', '+', '\u0641', // 1601 ف ARABIC LETTER FEH
    'q', '+', '\u0642', // 1602 ق ARABIC LETTER QAF
    'k', '+', '\u0643', // 1603 ك ARABIC LETTER KAF
    'l', '+', '\u0644', // 1604 ل ARABIC LETTER LAM
    'm', '+', '\u0645', // 1605 م ARABIC LETTER MEEM
    'n', '+', '\u0646', // 1606 ن ARABIC LETTER NOON
    'h', '+', '\u0647', // 1607 ه ARABIC LETTER HEH
    'w', '+', '\u0648', // 1608 و ARABIC LETTER WAW
    'j', '+', '\u0649', // 1609 ى ARABIC LETTER ALEF MAKSURA
    'y', '+', '\u064a', // 1610 ي ARABIC LETTER YEH
    ':', '+', '\u064b', // 1611 ً ARABIC FATHATAN
    '"', '+', '\u064c', // 1612 ٌ ARABIC DAMMATAN
    '=', '+', '\u064d', // 1613 ٍ ARABIC KASRATAN
    '/', '+', '\u064e', // 1614 َ ARABIC FATHA
    '\'', '+', '\u064f', // 1615 ُ ARABIC DAMMA
    '1', '+', '\u0650', // 1616 ِ ARABIC KASRA
    '3', '+', '\u0651', // 1617 ّ ARABIC SHADDA
    '0', '+', '\u0652', // 1618 ْ ARABIC SUKUN
    'a', 'S', '\u0670', // 1648 ٰ ARABIC LETTER SUPERSCRIPT ALEF
    'p', '+', '\u067e', // 1662 پ ARABIC LETTER PEH
    'v', '+', '\u06a4', // 1700 ڤ ARABIC LETTER VEH
    'g', 'f', '\u06af', // 1711 گ ARABIC LETTER GAF
    '0', 'a', '\u06f0', // 1776 ۰ EXTENDED ARABIC-INDIC DIGIT ZERO
    '1', 'a', '\u06f1', // 1777 ۱ EXTENDED ARABIC-INDIC DIGIT ONE
    '2', 'a', '\u06f2', // 1778 ۲ EXTENDED ARABIC-INDIC DIGIT TWO
    '3', 'a', '\u06f3', // 1779 ۳ EXTENDED ARABIC-INDIC DIGIT THREE
    '4', 'a', '\u06f4', // 1780 ۴ EXTENDED ARABIC-INDIC DIGIT FOUR
    '5', 'a', '\u06f5', // 1781 ۵ EXTENDED ARABIC-INDIC DIGIT FIVE
    '6', 'a', '\u06f6', // 1782 ۶ EXTENDED ARABIC-INDIC DIGIT SIX
    '7', 'a', '\u06f7', // 1783 ۷ EXTENDED ARABIC-INDIC DIGIT SEVEN
    '8', 'a', '\u06f8', // 1784 ۸ EXTENDED ARABIC-INDIC DIGIT EIGHT
    '9', 'a', '\u06f9', // 1785 ۹ EXTENDED ARABIC-INDIC DIGIT NINE
    'B', '.', '\u1e02', // 7682 Ḃ LATIN CAPITAL LETTER B WITH DOT ABOVE
    'b', '.', '\u1e03', // 7683 ḃ LATIN SMALL LETTER B WITH DOT ABOVE
    'B', '_', '\u1e06', // 7686 Ḇ LATIN CAPITAL LETTER B WITH LINE BELOW
    'b', '_', '\u1e07', // 7687 ḇ LATIN SMALL LETTER B WITH LINE BELOW
    'D', '.', '\u1e0a', // 7690 Ḋ LATIN CAPITAL LETTER D WITH DOT ABOVE
    'd', '.', '\u1e0b', // 7691 ḋ LATIN SMALL LETTER D WITH DOT ABOVE
    'D', '_', '\u1e0e', // 7694 Ḏ LATIN CAPITAL LETTER D WITH LINE BELOW
    'd', '_', '\u1e0f', // 7695 ḏ LATIN SMALL LETTER D WITH LINE BELOW
    'D', ',', '\u1e10', // 7696 Ḑ LATIN CAPITAL LETTER D WITH CEDILLA
    'd', ',', '\u1e11', // 7697 ḑ LATIN SMALL LETTER D WITH CEDILLA
    'F', '.', '\u1e1e', // 7710 Ḟ LATIN CAPITAL LETTER F WITH DOT ABOVE
    'f', '.', '\u1e1f', // 7711 ḟ LATIN SMALL LETTER F WITH DOT ABOVE
    'G', '-', '\u1e20', // 7712 Ḡ LATIN CAPITAL LETTER G WITH MACRON
    'g', '-', '\u1e21', // 7713 ḡ LATIN SMALL LETTER G WITH MACRON
    'H', '.', '\u1e22', // 7714 Ḣ LATIN CAPITAL LETTER H WITH DOT ABOVE
    'h', '.', '\u1e23', // 7715 ḣ LATIN SMALL LETTER H WITH DOT ABOVE
    'H', ':', '\u1e26', // 7718 Ḧ LATIN CAPITAL LETTER H WITH DIAERESIS
    'h', ':', '\u1e27', // 7719 ḧ LATIN SMALL LETTER H WITH DIAERESIS
    'H', ',', '\u1e28', // 7720 Ḩ LATIN CAPITAL LETTER H WITH CEDILLA
    'h', ',', '\u1e29', // 7721 ḩ LATIN SMALL LETTER H WITH CEDILLA
    'K', '\'', '\u1e30', // 7728 Ḱ LATIN CAPITAL LETTER K WITH ACUTE
    'k', '\'', '\u1e31', // 7729 ḱ LATIN SMALL LETTER K WITH ACUTE
    'K', '_', '\u1e34', // 7732 Ḵ LATIN CAPITAL LETTER K WITH LINE BELOW
    'k', '_', '\u1e35', // 7733 ḵ LATIN SMALL LETTER K WITH LINE BELOW
    'L', '_', '\u1e3a', // 7738 Ḻ LATIN CAPITAL LETTER L WITH LINE BELOW
    'l', '_', '\u1e3b', // 7739 ḻ LATIN SMALL LETTER L WITH LINE BELOW
    'M', '\'', '\u1e3e', // 7742 Ḿ LATIN CAPITAL LETTER M WITH ACUTE
    'm', '\'', '\u1e3f', // 7743 ḿ LATIN SMALL LETTER M WITH ACUTE
    'M', '.', '\u1e40', // 7744 Ṁ LATIN CAPITAL LETTER M WITH DOT ABOVE
    'm', '.', '\u1e41', // 7745 ṁ LATIN SMALL LETTER M WITH DOT ABOVE
    'N', '.', '\u1e44', // 7748 Ṅ LATIN CAPITAL LETTER N WITH DOT ABOVE
    'n', '.', '\u1e45', // 7749 ṅ LATIN SMALL LETTER N WITH DOT ABOVE
    'N', '_', '\u1e48', // 7752 Ṉ LATIN CAPITAL LETTER N WITH LINE BELOW
    'n', '_', '\u1e49', // 7753 ṉ LATIN SMALL LETTER N WITH LINE BELOW
    'P', '\'', '\u1e54', // 7764 Ṕ LATIN CAPITAL LETTER P WITH ACUTE
    'p', '\'', '\u1e55', // 7765 ṕ LATIN SMALL LETTER P WITH ACUTE
    'P', '.', '\u1e56', // 7766 Ṗ LATIN CAPITAL LETTER P WITH DOT ABOVE
    'p', '.', '\u1e57', // 7767 ṗ LATIN SMALL LETTER P WITH DOT ABOVE
    'R', '.', '\u1e58', // 7768 Ṙ LATIN CAPITAL LETTER R WITH DOT ABOVE
    'r', '.', '\u1e59', // 7769 ṙ LATIN SMALL LETTER R WITH DOT ABOVE
    'R', '_', '\u1e5e', // 7774 Ṟ LATIN CAPITAL LETTER R WITH LINE BELOW
    'r', '_', '\u1e5f', // 7775 ṟ LATIN SMALL LETTER R WITH LINE BELOW
    'S', '.', '\u1e60', // 7776 Ṡ LATIN CAPITAL LETTER S WITH DOT ABOVE
    's', '.', '\u1e61', // 7777 ṡ LATIN SMALL LETTER S WITH DOT ABOVE
    'T', '.', '\u1e6a', // 7786 Ṫ LATIN CAPITAL LETTER T WITH DOT ABOVE
    't', '.', '\u1e6b', // 7787 ṫ LATIN SMALL LETTER T WITH DOT ABOVE
    'T', '_', '\u1e6e', // 7790 Ṯ LATIN CAPITAL LETTER T WITH LINE BELOW
    't', '_', '\u1e6f', // 7791 ṯ LATIN SMALL LETTER T WITH LINE BELOW
    'V', '?', '\u1e7c', // 7804 Ṽ LATIN CAPITAL LETTER V WITH TILDE
    'v', '?', '\u1e7d', // 7805 ṽ LATIN SMALL LETTER V WITH TILDE
    'W', '!', '\u1e80', // 7808 Ẁ LATIN CAPITAL LETTER W WITH GRAVE
    'W', '`', '\u1e80', // 7808 Ẁ LATIN CAPITAL LETTER W WITH GRAVE (Vim 5.x compatible)
    'w', '!', '\u1e81', // 7809 ẁ LATIN SMALL LETTER W WITH GRAVE
    'w', '`', '\u1e81', // 7809 ẁ LATIN SMALL LETTER W WITH GRAVE (Vim 5.x compatible)
    'W', '\'', '\u1e82', // 7810 Ẃ LATIN CAPITAL LETTER W WITH ACUTE
    'w', '\'', '\u1e83', // 7811 ẃ LATIN SMALL LETTER W WITH ACUTE
    'W', ':', '\u1e84', // 7812 Ẅ LATIN CAPITAL LETTER W WITH DIAERESIS
    'w', ':', '\u1e85', // 7813 ẅ LATIN SMALL LETTER W WITH DIAERESIS
    'W', '.', '\u1e86', // 7814 Ẇ LATIN CAPITAL LETTER W WITH DOT ABOVE
    'w', '.', '\u1e87', // 7815 ẇ LATIN SMALL LETTER W WITH DOT ABOVE
    'X', '.', '\u1e8a', // 7818 Ẋ LATIN CAPITAL LETTER X WITH DOT ABOVE
    'x', '.', '\u1e8b', // 7819 ẋ LATIN SMALL LETTER X WITH DOT ABOVE
    'X', ':', '\u1e8c', // 7820 Ẍ LATIN CAPITAL LETTER X WITH DIAERESIS
    'x', ':', '\u1e8d', // 7821 ẍ LATIN SMALL LETTER X WITH DIAERESIS
    'Y', '.', '\u1e8e', // 7822 Ẏ LATIN CAPITAL LETTER Y WITH DOT ABOVE
    'y', '.', '\u1e8f', // 7823 ẏ LATIN SMALL LETTER Y WITH DOT ABOVE
    'Z', '>', '\u1e90', // 7824 Ẑ LATIN CAPITAL LETTER Z WITH CIRCUMFLEX
    'z', '>', '\u1e91', // 7825 ẑ LATIN SMALL LETTER Z WITH CIRCUMFLEX
    'Z', '_', '\u1e94', // 7828 Ẕ LATIN CAPITAL LETTER Z WITH LINE BELOW
    'z', '_', '\u1e95', // 7829 ẕ LATIN SMALL LETTER Z WITH LINE BELOW
    'h', '_', '\u1e96', // 7830 ẖ LATIN SMALL LETTER H WITH LINE BELOW
    't', ':', '\u1e97', // 7831 ẗ LATIN SMALL LETTER T WITH DIAERESIS
    'w', '0', '\u1e98', // 7832 ẘ LATIN SMALL LETTER W WITH RING ABOVE
    'y', '0', '\u1e99', // 7833 ẙ LATIN SMALL LETTER Y WITH RING ABOVE
    'A', '2', '\u1ea2', // 7842 Ả LATIN CAPITAL LETTER A WITH HOOK ABOVE
    'a', '2', '\u1ea3', // 7843 ả LATIN SMALL LETTER A WITH HOOK ABOVE
    'E', '2', '\u1eba', // 7866 Ẻ LATIN CAPITAL LETTER E WITH HOOK ABOVE
    'e', '2', '\u1ebb', // 7867 ẻ LATIN SMALL LETTER E WITH HOOK ABOVE
    'E', '?', '\u1ebc', // 7868 Ẽ LATIN CAPITAL LETTER E WITH TILDE
    'e', '?', '\u1ebd', // 7869 ẽ LATIN SMALL LETTER E WITH TILDE
    'I', '2', '\u1ec8', // 7880 Ỉ LATIN CAPITAL LETTER I WITH HOOK ABOVE
    'i', '2', '\u1ec9', // 7881 ỉ LATIN SMALL LETTER I WITH HOOK ABOVE
    'O', '2', '\u1ece', // 7886 Ỏ LATIN CAPITAL LETTER O WITH HOOK ABOVE
    'o', '2', '\u1ecf', // 7887 ỏ LATIN SMALL LETTER O WITH HOOK ABOVE
    'U', '2', '\u1ee6', // 7910 Ủ LATIN CAPITAL LETTER U WITH HOOK ABOVE
    'u', '2', '\u1ee7', // 7911 ủ LATIN SMALL LETTER U WITH HOOK ABOVE
    'Y', '!', '\u1ef2', // 7922 Ỳ LATIN CAPITAL LETTER Y WITH GRAVE
    'Y', '`', '\u1ef2', // 7922 Ỳ LATIN CAPITAL LETTER Y WITH GRAVE (Vim 5.x compatible)
    'y', '!', '\u1ef3', // 7923 ỳ LATIN SMALL LETTER Y WITH GRAVE
    'y', '`', '\u1ef3', // 7923 ỳ LATIN SMALL LETTER Y WITH GRAVE (Vim 5.x compatible)
    'Y', '2', '\u1ef6', // 7926 Ỷ LATIN CAPITAL LETTER Y WITH HOOK ABOVE
    'y', '2', '\u1ef7', // 7927 ỷ LATIN SMALL LETTER Y WITH HOOK ABOVE
    'Y', '?', '\u1ef8', // 7928 Ỹ LATIN CAPITAL LETTER Y WITH TILDE
    'y', '?', '\u1ef9', // 7929 ỹ LATIN SMALL LETTER Y WITH TILDE
    ';', '\'', '\u1f00', // 7936 ἀ GREEK SMALL LETTER ALPHA WITH PSILI
    ',', '\'', '\u1f01', // 7937 ἁ GREEK SMALL LETTER ALPHA WITH DASIA
    ';', '!', '\u1f02', // 7938 ἂ GREEK SMALL LETTER ALPHA WITH PSILI AND VARIA
    ',', '!', '\u1f03', // 7939 ἃ GREEK SMALL LETTER ALPHA WITH DASIA AND VARIA
    '?', ';', '\u1f04', // 7940 ἄ GREEK SMALL LETTER ALPHA WITH PSILI AND OXIA
    '?', ',', '\u1f05', // 7941 ἅ GREEK SMALL LETTER ALPHA WITH DASIA AND OXIA
    '!', ':', '\u1f06', // 7942 ἆ GREEK SMALL LETTER ALPHA WITH PSILI AND PERISPOMENI
    '?', ':', '\u1f07', // 7943 ἇ GREEK SMALL LETTER ALPHA WITH DASIA AND PERISPOMENI
    '1', 'N', '\u2002', // 8194   EN SPACE
    '1', 'M', '\u2003', // 8195   EM SPACE
    '3', 'M', '\u2004', // 8196   THREE-PER-EM SPACE
    '4', 'M', '\u2005', // 8197   FOUR-PER-EM SPACE
    '6', 'M', '\u2006', // 8198   SIX-PER-EM SPACE
    '1', 'T', '\u2009', // 8201   THIN SPACE
    '1', 'H', '\u200a', // 8202   HAIR SPACE
    '-', '1', '\u2010', // 8208 ‐ HYPHEN
    '-', 'N', '\u2013', // 8211 – EN DASH
    '-', 'M', '\u2014', // 8212 — EM DASH
    '-', '3', '\u2015', // 8213 ― HORIZONTAL BAR
    '!', '2', '\u2016', // 8214 ‖ DOUBLE VERTICAL LINE
    '=', '2', '\u2017', // 8215 ‗ DOUBLE LOW LINE
    '\'', '6', '\u2018', // 8216 ‘ LEFT SINGLE QUOTATION MARK
    '\'', '9', '\u2019', // 8217 ’ RIGHT SINGLE QUOTATION MARK
    '.', '9', '\u201a', // 8218 ‚ SINGLE LOW-9 QUOTATION MARK
    '9', '\'', '\u201b', // 8219 ‛ SINGLE HIGH-REVERSED-9 QUOTATION MARK
    '"', '6', '\u201c', // 8220 “ LEFT DOUBLE QUOTATION MARK
    '"', '9', '\u201d', // 8221 ” RIGHT DOUBLE QUOTATION MARK
    ':', '9', '\u201e', // 8222 „ DOUBLE LOW-9 QUOTATION MARK
    '9', '"', '\u201f', // 8223 ‟ DOUBLE HIGH-REVERSED-9 QUOTATION MARK
    '/', '-', '\u2020', // 8224 † DAGGER
    '/', '=', '\u2021', // 8225 ‡ DOUBLE DAGGER
    'o', 'o', '\u2022', // 8226 • BULLET
    '.', '.', '\u2025', // 8229 ‥ TWO DOT LEADER
    ',', '.', '\u2026', // 8230 … HORIZONTAL ELLIPSIS (Vim 5.x compatible)
    '%', '0', '\u2030', // 8240 ‰ PER MILLE SIGN
    '1', '\'', '\u2032', // 8242 ′ PRIME
    '2', '\'', '\u2033', // 8243 ″ DOUBLE PRIME
    '3', '\'', '\u2034', // 8244 ‴ TRIPLE PRIME
    '4', '\'', '\u2057', // 8279 ⁗ QUADRUPLE PRIME
    '1', '"', '\u2035', // 8245 ‵ REVERSED PRIME
    '2', '"', '\u2036', // 8246 ‶ REVERSED DOUBLE PRIME
    '3', '"', '\u2037', // 8247 ‷ REVERSED TRIPLE PRIME
    'C', 'a', '\u2038', // 8248 ‸ CARET
    '<', '1', '\u2039', // 8249 ‹ SINGLE LEFT-POINTING ANGLE QUOTATION MARK
    '>', '1', '\u203a', // 8250 › SINGLE RIGHT-POINTING ANGLE QUOTATION MARK
    ':', 'X', '\u203b', // 8251 ※ REFERENCE MARK
    '\'', '-', '\u203e', // 8254 ‾ OVERLINE
    '/', 'f', '\u2044', // 8260 ⁄ FRACTION SLASH
    '0', 'S', '\u2070', // 8304 ⁰ SUPERSCRIPT ZERO
    '4', 'S', '\u2074', // 8308 ⁴ SUPERSCRIPT FOUR
    '5', 'S', '\u2075', // 8309 ⁵ SUPERSCRIPT FIVE
    '6', 'S', '\u2076', // 8310 ⁶ SUPERSCRIPT SIX
    '7', 'S', '\u2077', // 8311 ⁷ SUPERSCRIPT SEVEN
    '8', 'S', '\u2078', // 8312 ⁸ SUPERSCRIPT EIGHT
    '9', 'S', '\u2079', // 8313 ⁹ SUPERSCRIPT NINE
    '+', 'S', '\u207a', // 8314 ⁺ SUPERSCRIPT PLUS SIGN
    '-', 'S', '\u207b', // 8315 ⁻ SUPERSCRIPT MINUS
    '=', 'S', '\u207c', // 8316 ⁼ SUPERSCRIPT EQUALS SIGN
    '(', 'S', '\u207d', // 8317 ⁽ SUPERSCRIPT LEFT PARENTHESIS
    ')', 'S', '\u207e', // 8318 ⁾ SUPERSCRIPT RIGHT PARENTHESIS
    'n', 'S', '\u207f', // 8319 ⁿ SUPERSCRIPT LATIN SMALL LETTER N
    '0', 's', '\u2080', // 8320 ₀ SUBSCRIPT ZERO
    '1', 's', '\u2081', // 8321 ₁ SUBSCRIPT ONE
    '2', 's', '\u2082', // 8322 ₂ SUBSCRIPT TWO
    '3', 's', '\u2083', // 8323 ₃ SUBSCRIPT THREE
    '4', 's', '\u2084', // 8324 ₄ SUBSCRIPT FOUR
    '5', 's', '\u2085', // 8325 ₅ SUBSCRIPT FIVE
    '6', 's', '\u2086', // 8326 ₆ SUBSCRIPT SIX
    '7', 's', '\u2087', // 8327 ₇ SUBSCRIPT SEVEN
    '8', 's', '\u2088', // 8328 ₈ SUBSCRIPT EIGHT
    '9', 's', '\u2089', // 8329 ₉ SUBSCRIPT NINE
    '+', 's', '\u208a', // 8330 ₊ SUBSCRIPT PLUS SIGN
    '-', 's', '\u208b', // 8331 ₋ SUBSCRIPT MINUS
    '=', 's', '\u208c', // 8332 ₌ SUBSCRIPT EQUALS SIGN
    '(', 's', '\u208d', // 8333 ₍ SUBSCRIPT LEFT PARENTHESIS
    ')', 's', '\u208e', // 8334 ₎ SUBSCRIPT RIGHT PARENTHESIS
    'L', 'i', '\u20a4', // 8356 ₤ LIRA SIGN
    'P', 't', '\u20a7', // 8359 ₧ PESETA SIGN
    'W', '=', '\u20a9', // 8361 ₩ WON SIGN
    '=', 'e', '\u20ac', // 8364 € EURO SIGN
    'E', 'u', '\u20ac', // 8364 € EURO SIGN
    '=', 'R', '\u20bd', // 8381 ₽ ROUBLE SIGN
    '=', 'P', '\u20bd', // 8381 ₽ ROUBLE SIGN
    'o', 'C', '\u2103', // 8451 ℃ DEGREE CELSIUS
    'c', 'o', '\u2105', // 8453 ℅ CARE OF
    'o', 'F', '\u2109', // 8457 ℉ DEGREE FAHRENHEIT
    'N', '0', '\u2116', // 8470 № NUMERO SIGN
    'P', 'O', '\u2117', // 8471 ℗ SOUND RECORDING COPYRIGHT
    'R', 'x', '\u211e', // 8478 ℞ PRESCRIPTION TAKE
    'S', 'M', '\u2120', // 8480 ℠ SERVICE MARK
    'T', 'M', '\u2122', // 8482 ™ TRADE MARK SIGN
    'O', 'm', '\u2126', // 8486 Ω OHM SIGN
    'A', 'O', '\u212b', // 8491 Å ANGSTROM SIGN
    '1', '3', '\u2153', // 8531 ⅓ VULGAR FRACTION ONE THIRD
    '2', '3', '\u2154', // 8532 ⅔ VULGAR FRACTION TWO THIRDS
    '1', '5', '\u2155', // 8533 ⅕ VULGAR FRACTION ONE FIFTH
    '2', '5', '\u2156', // 8534 ⅖ VULGAR FRACTION TWO FIFTHS
    '3', '5', '\u2157', // 8535 ⅗ VULGAR FRACTION THREE FIFTHS
    '4', '5', '\u2158', // 8536 ⅘ VULGAR FRACTION FOUR FIFTHS
    '1', '6', '\u2159', // 8537 ⅙ VULGAR FRACTION ONE SIXTH
    '5', '6', '\u215a', // 8538 ⅚ VULGAR FRACTION FIVE SIXTHS
    '1', '8', '\u215b', // 8539 ⅛ VULGAR FRACTION ONE EIGHTH
    '3', '8', '\u215c', // 8540 ⅜ VULGAR FRACTION THREE EIGHTHS
    '5', '8', '\u215d', // 8541 ⅝ VULGAR FRACTION FIVE EIGHTHS
    '7', '8', '\u215e', // 8542 ⅞ VULGAR FRACTION SEVEN EIGHTHS
    '1', 'R', '\u2160', // 8544 Ⅰ ROMAN NUMERAL ONE
    '2', 'R', '\u2161', // 8545 Ⅱ ROMAN NUMERAL TWO
    '3', 'R', '\u2162', // 8546 Ⅲ ROMAN NUMERAL THREE
    '4', 'R', '\u2163', // 8547 Ⅳ ROMAN NUMERAL FOUR
    '5', 'R', '\u2164', // 8548 Ⅴ ROMAN NUMERAL FIVE
    '6', 'R', '\u2165', // 8549 Ⅵ ROMAN NUMERAL SIX
    '7', 'R', '\u2166', // 8550 Ⅶ ROMAN NUMERAL SEVEN
    '8', 'R', '\u2167', // 8551 Ⅷ ROMAN NUMERAL EIGHT
    '9', 'R', '\u2168', // 8552 Ⅸ ROMAN NUMERAL NINE
    'a', 'R', '\u2169', // 8553 Ⅹ ROMAN NUMERAL TEN
    'b', 'R', '\u216a', // 8554 Ⅺ ROMAN NUMERAL ELEVEN
    'c', 'R', '\u216b', // 8555 Ⅻ ROMAN NUMERAL TWELVE
    '1', 'r', '\u2170', // 8560 ⅰ SMALL ROMAN NUMERAL ONE
    '2', 'r', '\u2171', // 8561 ⅱ SMALL ROMAN NUMERAL TWO
    '3', 'r', '\u2172', // 8562 ⅲ SMALL ROMAN NUMERAL THREE
    '4', 'r', '\u2173', // 8563 ⅳ SMALL ROMAN NUMERAL FOUR
    '5', 'r', '\u2174', // 8564 ⅴ SMALL ROMAN NUMERAL FIVE
    '6', 'r', '\u2175', // 8565 ⅵ SMALL ROMAN NUMERAL SIX
    '7', 'r', '\u2176', // 8566 ⅶ SMALL ROMAN NUMERAL SEVEN
    '8', 'r', '\u2177', // 8567 ⅷ SMALL ROMAN NUMERAL EIGHT
    '9', 'r', '\u2178', // 8568 ⅸ SMALL ROMAN NUMERAL NINE
    'a', 'r', '\u2179', // 8569 ⅹ SMALL ROMAN NUMERAL TEN
    'b', 'r', '\u217a', // 8570 ⅺ SMALL ROMAN NUMERAL ELEVEN
    'c', 'r', '\u217b', // 8571 ⅻ SMALL ROMAN NUMERAL TWELVE
    '<', '-', '\u2190', // 8592 ← LEFTWARDS ARROW
    '-', '!', '\u2191', // 8593 ↑ UPWARDS ARROW
    '-', '>', '\u2192', // 8594 → RIGHTWARDS ARROW
    '-', 'v', '\u2193', // 8595 ↓ DOWNWARDS ARROW
    '<', '>', '\u2194', // 8596 ↔ LEFT RIGHT ARROW
    'U', 'D', '\u2195', // 8597 ↕ UP DOWN ARROW
    '<', '=', '\u21d0', // 8656 ⇐ LEFTWARDS DOUBLE ARROW
    '=', '>', '\u21d2', // 8658 ⇒ RIGHTWARDS DOUBLE ARROW
    '=', '=', '\u21d4', // 8660 ⇔ LEFT RIGHT DOUBLE ARROW
    'F', 'A', '\u2200', // 8704 ∀ FOR ALL
    'd', 'P', '\u2202', // 8706 ∂ PARTIAL DIFFERENTIAL
    'T', 'E', '\u2203', // 8707 ∃ THERE EXISTS
    '/', '0', '\u2205', // 8709 ∅ EMPTY SET
    'D', 'E', '\u2206', // 8710 ∆ INCREMENT
    'N', 'B', '\u2207', // 8711 ∇ NABLA
    '(', '-', '\u2208', // 8712 ∈ ELEMENT OF
    '-', ')', '\u220b', // 8715 ∋ CONTAINS AS MEMBER
    '*', 'P', '\u220f', // 8719 ∏ N-ARY PRODUCT
    '+', 'Z', '\u2211', // 8721 ∑ N-ARY SUMMATION
    '-', '2', '\u2212', // 8722 − MINUS SIGN
    '-', '+', '\u2213', // 8723 ∓ MINUS-OR-PLUS SIGN
    '*', '-', '\u2217', // 8727 ∗ ASTERISK OPERATOR
    'O', 'b', '\u2218', // 8728 ∘ RING OPERATOR
    'S', 'b', '\u2219', // 8729 ∙ BULLET OPERATOR
    'R', 'T', '\u221a', // 8730 √ SQUARE ROOT
    '0', '(', '\u221d', // 8733 ∝ PROPORTIONAL TO
    '0', '0', '\u221e', // 8734 ∞ INFINITY
    '-', 'L', '\u221f', // 8735 ∟ RIGHT ANGLE
    '-', 'V', '\u2220', // 8736 ∠ ANGLE
    'P', 'P', '\u2225', // 8741 ∥ PARALLEL TO
    'A', 'N', '\u2227', // 8743 ∧ LOGICAL AND
    'O', 'R', '\u2228', // 8744 ∨ LOGICAL OR
    '(', 'U', '\u2229', // 8745 ∩ INTERSECTION
    ')', 'U', '\u222a', // 8746 ∪ UNION
    'I', 'n', '\u222b', // 8747 ∫ INTEGRAL
    'D', 'I', '\u222c', // 8748 ∬ DOUBLE INTEGRAL
    'I', 'o', '\u222e', // 8750 ∮ CONTOUR INTEGRAL
    '.', ':', '\u2234', // 8756 ∴ THEREFORE
    ':', '.', '\u2235', // 8757 ∵ BECAUSE
    ':', 'R', '\u2236', // 8758 ∶ RATIO
    ':', ':', '\u2237', // 8759 ∷ PROPORTION
    '?', '1', '\u223c', // 8764 ∼ TILDE OPERATOR
    'C', 'G', '\u223e', // 8766 ∾ INVERTED LAZY S
    '?', '-', '\u2243', // 8771 ≃ ASYMPTOTICALLY EQUAL TO
    '?', '=', '\u2245', // 8773 ≅ APPROXIMATELY EQUAL TO
    '?', '2', '\u2248', // 8776 ≈ ALMOST EQUAL TO
    '=', '?', '\u224c', // 8780 ≌ ALL EQUAL TO
    'H', 'I', '\u2253', // 8787 ≓ IMAGE OF OR APPROXIMATELY EQUAL TO
    '!', '=', '\u2260', // 8800 ≠ NOT EQUAL TO
    '=', '3', '\u2261', // 8801 ≡ IDENTICAL TO
    '=', '<', '\u2264', // 8804 ≤ LESS-THAN OR EQUAL TO
    '>', '=', '\u2265', // 8805 ≥ GREATER-THAN OR EQUAL TO
    '<', '*', '\u226a', // 8810 ≪ MUCH LESS-THAN
    '*', '>', '\u226b', // 8811 ≫ MUCH GREATER-THAN
    '!', '<', '\u226e', // 8814 ≮ NOT LESS-THAN
    '!', '>', '\u226f', // 8815 ≯ NOT GREATER-THAN
    '(', 'C', '\u2282', // 8834 ⊂ SUBSET OF
    ')', 'C', '\u2283', // 8835 ⊃ SUPERSET OF
    '(', '_', '\u2286', // 8838 ⊆ SUBSET OF OR EQUAL TO
    ')', '_', '\u2287', // 8839 ⊇ SUPERSET OF OR EQUAL TO
    '0', '.', '\u2299', // 8857 ⊙ CIRCLED DOT OPERATOR
    '0', '2', '\u229a', // 8858 ⊚ CIRCLED RING OPERATOR
    '-', 'T', '\u22a5', // 8869 ⊥ UP TACK
    '.', 'P', '\u22c5', // 8901 ⋅ DOT OPERATOR
    ':', '3', '\u22ee', // 8942 ⋮ VERTICAL ELLIPSIS
    '.', '3', '\u22ef', // 8943 ⋯ MIDLINE HORIZONTAL ELLIPSIS
    'E', 'h', '\u2302', // 8962 ⌂ HOUSE
    '<', '7', '\u2308', // 8968 ⌈ LEFT CEILING
    '>', '7', '\u2309', // 8969 ⌉ RIGHT CEILING
    '7', '<', '\u230a', // 8970 ⌊ LEFT FLOOR
    '7', '>', '\u230b', // 8971 ⌋ RIGHT FLOOR
    'N', 'I', '\u2310', // 8976 ⌐ REVERSED NOT SIGN
    '(', 'A', '\u2312', // 8978 ⌒ ARC
    'T', 'R', '\u2315', // 8981 ⌕ TELEPHONE RECORDER
    'I', 'u', '\u2320', // 8992 ⌠ TOP HALF INTEGRAL
    'I', 'l', '\u2321', // 8993 ⌡ BOTTOM HALF INTEGRAL
    '<', '/', '\u2329', // 9001 〈 LEFT-POINTING ANGLE BRACKET
    '/', '>', '\u232a', // 9002 〉 RIGHT-POINTING ANGLE BRACKET
    'V', 's', '\u2423', // 9251 ␣ OPEN BOX
    '1', 'h', '\u2440', // 9280 ⑀ OCR HOOK
    '3', 'h', '\u2441', // 9281 ⑁ OCR CHAIR
    '2', 'h', '\u2442', // 9282 ⑂ OCR FORK
    '4', 'h', '\u2443', // 9283 ⑃ OCR INVERTED FORK
    '1', 'j', '\u2446', // 9286 ⑆ OCR BRANCH BANK IDENTIFICATION
    '2', 'j', '\u2447', // 9287 ⑇ OCR AMOUNT OF CHECK
    '3', 'j', '\u2448', // 9288 ⑈ OCR DASH
    '4', 'j', '\u2449', // 9289 ⑉ OCR CUSTOMER ACCOUNT NUMBER
    '1', '.', '\u2488', // 9352 ⒈ DIGIT ONE FULL STOP
    '2', '.', '\u2489', // 9353 ⒉ DIGIT TWO FULL STOP
    '3', '.', '\u248a', // 9354 ⒊ DIGIT THREE FULL STOP
    '4', '.', '\u248b', // 9355 ⒋ DIGIT FOUR FULL STOP
    '5', '.', '\u248c', // 9356 ⒌ DIGIT FIVE FULL STOP
    '6', '.', '\u248d', // 9357 ⒍ DIGIT SIX FULL STOP
    '7', '.', '\u248e', // 9358 ⒎ DIGIT SEVEN FULL STOP
    '8', '.', '\u248f', // 9359 ⒏ DIGIT EIGHT FULL STOP
    '9', '.', '\u2490', // 9360 ⒐ DIGIT NINE FULL STOP
    'h', 'h', '\u2500', // 9472 ─ BOX DRAWINGS LIGHT HORIZONTAL
    'H', 'H', '\u2501', // 9473 ━ BOX DRAWINGS HEAVY HORIZONTAL
    'v', 'v', '\u2502', // 9474 │ BOX DRAWINGS LIGHT VERTICAL
    'V', 'V', '\u2503', // 9475 ┃ BOX DRAWINGS HEAVY VERTICAL
    '3', '-', '\u2504', // 9476 ┄ BOX DRAWINGS LIGHT TRIPLE DASH HORIZONTAL
    '3', '_', '\u2505', // 9477 ┅ BOX DRAWINGS HEAVY TRIPLE DASH HORIZONTAL
    '3', '!', '\u2506', // 9478 ┆ BOX DRAWINGS LIGHT TRIPLE DASH VERTICAL
    '3', '/', '\u2507', // 9479 ┇ BOX DRAWINGS HEAVY TRIPLE DASH VERTICAL
    '4', '-', '\u2508', // 9480 ┈ BOX DRAWINGS LIGHT QUADRUPLE DASH HORIZONTAL
    '4', '_', '\u2509', // 9481 ┉ BOX DRAWINGS HEAVY QUADRUPLE DASH HORIZONTAL
    '4', '!', '\u250a', // 9482 ┊ BOX DRAWINGS LIGHT QUADRUPLE DASH VERTICAL
    '4', '/', '\u250b', // 9483 ┋ BOX DRAWINGS HEAVY QUADRUPLE DASH VERTICAL
    'd', 'r', '\u250c', // 9484 ┌ BOX DRAWINGS LIGHT DOWN AND RIGHT
    'd', 'R', '\u250d', // 9485 ┍ BOX DRAWINGS DOWN LIGHT AND RIGHT HEAVY
    'D', 'r', '\u250e', // 9486 ┎ BOX DRAWINGS DOWN HEAVY AND RIGHT LIGHT
    'D', 'R', '\u250f', // 9487 ┏ BOX DRAWINGS HEAVY DOWN AND RIGHT
    'd', 'l', '\u2510', // 9488 ┐ BOX DRAWINGS LIGHT DOWN AND LEFT
    'd', 'L', '\u2511', // 9489 ┑ BOX DRAWINGS DOWN LIGHT AND LEFT HEAVY
    'D', 'l', '\u2512', // 9490 ┒ BOX DRAWINGS DOWN HEAVY AND LEFT LIGHT
    'L', 'D', '\u2513', // 9491 ┓ BOX DRAWINGS HEAVY DOWN AND LEFT
    'u', 'r', '\u2514', // 9492 └ BOX DRAWINGS LIGHT UP AND RIGHT
    'u', 'R', '\u2515', // 9493 ┕ BOX DRAWINGS UP LIGHT AND RIGHT HEAVY
    'U', 'r', '\u2516', // 9494 ┖ BOX DRAWINGS UP HEAVY AND RIGHT LIGHT
    'U', 'R', '\u2517', // 9495 ┗ BOX DRAWINGS HEAVY UP AND RIGHT
    'u', 'l', '\u2518', // 9496 ┘ BOX DRAWINGS LIGHT UP AND LEFT
    'u', 'L', '\u2519', // 9497 ┙ BOX DRAWINGS UP LIGHT AND LEFT HEAVY
    'U', 'l', '\u251a', // 9498 ┚ BOX DRAWINGS UP HEAVY AND LEFT LIGHT
    'U', 'L', '\u251b', // 9499 ┛ BOX DRAWINGS HEAVY UP AND LEFT
    'v', 'r', '\u251c', // 9500 ├ BOX DRAWINGS LIGHT VERTICAL AND RIGHT
    'v', 'R', '\u251d', // 9501 ┝ BOX DRAWINGS VERTICAL LIGHT AND RIGHT HEAVY
    'V', 'r', '\u2520', // 9504 ┠ BOX DRAWINGS VERTICAL HEAVY AND RIGHT LIGHT
    'V', 'R', '\u2523', // 9507 ┣ BOX DRAWINGS HEAVY VERTICAL AND RIGHT
    'v', 'l', '\u2524', // 9508 ┤ BOX DRAWINGS LIGHT VERTICAL AND LEFT
    'v', 'L', '\u2525', // 9509 ┥ BOX DRAWINGS VERTICAL LIGHT AND LEFT HEAVY
    'V', 'l', '\u2528', // 9512 ┨ BOX DRAWINGS VERTICAL HEAVY AND LEFT LIGHT
    'V', 'L', '\u252b', // 9515 ┫ BOX DRAWINGS HEAVY VERTICAL AND LEFT
    'd', 'h', '\u252c', // 9516 ┬ BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
    'd', 'H', '\u252f', // 9519 ┯ BOX DRAWINGS DOWN LIGHT AND HORIZONTAL HEAVY
    'D', 'h', '\u2530', // 9520 ┰ BOX DRAWINGS DOWN HEAVY AND HORIZONTAL LIGHT
    'D', 'H', '\u2533', // 9523 ┳ BOX DRAWINGS HEAVY DOWN AND HORIZONTAL
    'u', 'h', '\u2534', // 9524 ┴ BOX DRAWINGS LIGHT UP AND HORIZONTAL
    'u', 'H', '\u2537', // 9527 ┷ BOX DRAWINGS UP LIGHT AND HORIZONTAL HEAVY
    'U', 'h', '\u2538', // 9528 ┸ BOX DRAWINGS UP HEAVY AND HORIZONTAL LIGHT
    'U', 'H', '\u253b', // 9531 ┻ BOX DRAWINGS HEAVY UP AND HORIZONTAL
    'v', 'h', '\u253c', // 9532 ┼ BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
    'v', 'H', '\u253f', // 9535 ┿ BOX DRAWINGS VERTICAL LIGHT AND HORIZONTAL HEAVY
    'V', 'h', '\u2542', // 9538 ╂ BOX DRAWINGS VERTICAL HEAVY AND HORIZONTAL LIGHT
    'V', 'H', '\u254b', // 9547 ╋ BOX DRAWINGS HEAVY VERTICAL AND HORIZONTAL
    'F', 'D', '\u2571', // 9585 ╱ BOX DRAWINGS LIGHT DIAGONAL UPPER RIGHT TO LOWER LEFT
    'B', 'D', '\u2572', // 9586 ╲ BOX DRAWINGS LIGHT DIAGONAL UPPER LEFT TO LOWER RIGHT
    'T', 'B', '\u2580', // 9600 ▀ UPPER HALF BLOCK
    'L', 'B', '\u2584', // 9604 ▄ LOWER HALF BLOCK
    'F', 'B', '\u2588', // 9608 █ FULL BLOCK
    'l', 'B', '\u258c', // 9612 ▌ LEFT HALF BLOCK
    'R', 'B', '\u2590', // 9616 ▐ RIGHT HALF BLOCK
    '.', 'S', '\u2591', // 9617 ░ LIGHT SHADE
    ':', 'S', '\u2592', // 9618 ▒ MEDIUM SHADE
    '?', 'S', '\u2593', // 9619 ▓ DARK SHADE
    'f', 'S', '\u25a0', // 9632 ■ BLACK SQUARE
    'O', 'S', '\u25a1', // 9633 □ WHITE SQUARE
    'R', 'O', '\u25a2', // 9634 ▢ WHITE SQUARE WITH ROUNDED CORNERS
    'R', 'r', '\u25a3', // 9635 ▣ WHITE SQUARE CONTAINING BLACK SMALL SQUARE
    'R', 'F', '\u25a4', // 9636 ▤ SQUARE WITH HORIZONTAL FILL
    'R', 'Y', '\u25a5', // 9637 ▥ SQUARE WITH VERTICAL FILL
    'R', 'H', '\u25a6', // 9638 ▦ SQUARE WITH ORTHOGONAL CROSSHATCH FILL
    'R', 'Z', '\u25a7', // 9639 ▧ SQUARE WITH UPPER LEFT TO LOWER RIGHT FILL
    'R', 'K', '\u25a8', // 9640 ▨ SQUARE WITH UPPER RIGHT TO LOWER LEFT FILL
    'R', 'X', '\u25a9', // 9641 ▩ SQUARE WITH DIAGONAL CROSSHATCH FILL
    's', 'B', '\u25aa', // 9642 ▪ BLACK SMALL SQUARE
    'S', 'R', '\u25ac', // 9644 ▬ BLACK RECTANGLE
    'O', 'r', '\u25ad', // 9645 ▭ WHITE RECTANGLE
    'U', 'T', '\u25b2', // 9650 ▲ BLACK UP-POINTING TRIANGLE
    'u', 'T', '\u25b3', // 9651 △ WHITE UP-POINTING TRIANGLE
    'P', 'R', '\u25b6', // 9654 ▶ BLACK RIGHT-POINTING TRIANGLE
    'T', 'r', '\u25b7', // 9655 ▷ WHITE RIGHT-POINTING TRIANGLE
    'D', 't', '\u25bc', // 9660 ▼ BLACK DOWN-POINTING TRIANGLE
    'd', 'T', '\u25bd', // 9661 ▽ WHITE DOWN-POINTING TRIANGLE
    'P', 'L', '\u25c0', // 9664 ◀ BLACK LEFT-POINTING TRIANGLE
    'T', 'l', '\u25c1', // 9665 ◁ WHITE LEFT-POINTING TRIANGLE
    'D', 'b', '\u25c6', // 9670 ◆ BLACK DIAMOND
    'D', 'w', '\u25c7', // 9671 ◇ WHITE DIAMOND
    'L', 'Z', '\u25ca', // 9674 ◊ LOZENGE
    '0', 'm', '\u25cb', // 9675 ○ WHITE CIRCLE
    '0', 'o', '\u25ce', // 9678 ◎ BULLSEYE
    '0', 'M', '\u25cf', // 9679 ● BLACK CIRCLE
    '0', 'L', '\u25d0', // 9680 ◐ CIRCLE WITH LEFT HALF BLACK
    '0', 'R', '\u25d1', // 9681 ◑ CIRCLE WITH RIGHT HALF BLACK
    'S', 'n', '\u25d8', // 9688 ◘ INVERSE BULLET
    'I', 'c', '\u25d9', // 9689 ◙ INVERSE WHITE CIRCLE
    'F', 'd', '\u25e2', // 9698 ◢ BLACK LOWER RIGHT TRIANGLE
    'B', 'd', '\u25e3', // 9699 ◣ BLACK LOWER LEFT TRIANGLE
    '*', '2', '\u2605', // 9733 ★ BLACK STAR
    '*', '1', '\u2606', // 9734 ☆ WHITE STAR
    '<', 'H', '\u261c', // 9756 ☜ WHITE LEFT POINTING INDEX
    '>', 'H', '\u261e', // 9758 ☞ WHITE RIGHT POINTING INDEX
    '0', 'u', '\u263a', // 9786 ☺ WHITE SMILING FACE
    '0', 'U', '\u263b', // 9787 ☻ BLACK SMILING FACE
    'S', 'U', '\u263c', // 9788 ☼ WHITE SUN WITH RAYS
    'F', 'm', '\u2640', // 9792 ♀ FEMALE SIGN
    'M', 'l', '\u2642', // 9794 ♂ MALE SIGN
    'c', 'S', '\u2660', // 9824 ♠ BLACK SPADE SUIT
    'c', 'H', '\u2661', // 9825 ♡ WHITE HEART SUIT
    'c', 'D', '\u2662', // 9826 ♢ WHITE DIAMOND SUIT
    'c', 'C', '\u2663', // 9827 ♣ BLACK CLUB SUIT
    'M', 'd', '\u2669', // 9833 ♩ QUARTER NOTE
    'M', '8', '\u266a', // 9834 ♪ EIGHTH NOTE
    'M', '2', '\u266b', // 9835 ♫ BEAMED EIGHTH NOTES
    'M', 'b', '\u266d', // 9837 ♭ MUSIC FLAT SIGN
    'M', 'x', '\u266e', // 9838 ♮ MUSIC NATURAL SIGN
    'M', 'X', '\u266f', // 9839 ♯ MUSIC SHARP SIGN
    'O', 'K', '\u2713', // 10003 ✓ CHECK MARK
    'X', 'X', '\u2717', // 10007 ✗ BALLOT X
    '-', 'X', '\u2720', // 10016 ✠ MALTESE CROSS
    'I', 'S', '\u3000', // 12288 　 IDEOGRAPHIC SPACE
    ',', '_', '\u3001', // 12289 、 IDEOGRAPHIC COMMA
    '.', '_', '\u3002', // 12290 。 IDEOGRAPHIC FULL STOP
    '+', '"', '\u3003', // 12291 〃 DITTO MARK
    '+', '_', '\u3004', // 12292 〄 JAPANESE INDUSTRIAL STANDARD SYMBOL
    '*', '_', '\u3005', // 12293 々 IDEOGRAPHIC ITERATION MARK
    ';', '_', '\u3006', // 12294 〆 IDEOGRAPHIC CLOSING MARK
    '0', '_', '\u3007', // 12295 〇 IDEOGRAPHIC NUMBER ZERO
    '<', '+', '\u300a', // 12298 《 LEFT DOUBLE ANGLE BRACKET
    '>', '+', '\u300b', // 12299 》 RIGHT DOUBLE ANGLE BRACKET
    '<', '\'', '\u300c', // 12300 「 LEFT CORNER BRACKET
    '>', '\'', '\u300d', // 12301 」 RIGHT CORNER BRACKET
    '<', '"', '\u300e', // 12302 『 LEFT WHITE CORNER BRACKET
    '>', '"', '\u300f', // 12303 』 RIGHT WHITE CORNER BRACKET
    '(', '"', '\u3010', // 12304 【 LEFT BLACK LENTICULAR BRACKET
    ')', '"', '\u3011', // 12305 】 RIGHT BLACK LENTICULAR BRACKET
    '=', 'T', '\u3012', // 12306 〒 POSTAL MARK
    '=', '_', '\u3013', // 12307 〓 GETA MARK
    '(', '\'', '\u3014', // 12308 〔 LEFT TORTOISE SHELL BRACKET
    ')', '\'', '\u3015', // 12309 〕 RIGHT TORTOISE SHELL BRACKET
    '(', 'I', '\u3016', // 12310 〖 LEFT WHITE LENTICULAR BRACKET
    ')', 'I', '\u3017', // 12311 〗 RIGHT WHITE LENTICULAR BRACKET
    '-', '?', '\u301c', // 12316 〜 WAVE DASH
    'A', '5', '\u3041', // 12353 ぁ HIRAGANA LETTER SMALL A
    'a', '5', '\u3042', // 12354 あ HIRAGANA LETTER A
    'I', '5', '\u3043', // 12355 ぃ HIRAGANA LETTER SMALL I
    'i', '5', '\u3044', // 12356 い HIRAGANA LETTER I
    'U', '5', '\u3045', // 12357 ぅ HIRAGANA LETTER SMALL U
    'u', '5', '\u3046', // 12358 う HIRAGANA LETTER U
    'E', '5', '\u3047', // 12359 ぇ HIRAGANA LETTER SMALL E
    'e', '5', '\u3048', // 12360 え HIRAGANA LETTER E
    'O', '5', '\u3049', // 12361 ぉ HIRAGANA LETTER SMALL O
    'o', '5', '\u304a', // 12362 お HIRAGANA LETTER O
    'k', 'a', '\u304b', // 12363 か HIRAGANA LETTER KA
    'g', 'a', '\u304c', // 12364 が HIRAGANA LETTER GA
    'k', 'i', '\u304d', // 12365 き HIRAGANA LETTER KI
    'g', 'i', '\u304e', // 12366 ぎ HIRAGANA LETTER GI
    'k', 'u', '\u304f', // 12367 く HIRAGANA LETTER KU
    'g', 'u', '\u3050', // 12368 ぐ HIRAGANA LETTER GU
    'k', 'e', '\u3051', // 12369 け HIRAGANA LETTER KE
    'g', 'e', '\u3052', // 12370 げ HIRAGANA LETTER GE
    'k', 'o', '\u3053', // 12371 こ HIRAGANA LETTER KO
    'g', 'o', '\u3054', // 12372 ご HIRAGANA LETTER GO
    's', 'a', '\u3055', // 12373 さ HIRAGANA LETTER SA
    'z', 'a', '\u3056', // 12374 ざ HIRAGANA LETTER ZA
    's', 'i', '\u3057', // 12375 し HIRAGANA LETTER SI
    'z', 'i', '\u3058', // 12376 じ HIRAGANA LETTER ZI
    's', 'u', '\u3059', // 12377 す HIRAGANA LETTER SU
    'z', 'u', '\u305a', // 12378 ず HIRAGANA LETTER ZU
    's', 'e', '\u305b', // 12379 せ HIRAGANA LETTER SE
    'z', 'e', '\u305c', // 12380 ぜ HIRAGANA LETTER ZE
    's', 'o', '\u305d', // 12381 そ HIRAGANA LETTER SO
    'z', 'o', '\u305e', // 12382 ぞ HIRAGANA LETTER ZO
    't', 'a', '\u305f', // 12383 た HIRAGANA LETTER TA
    'd', 'a', '\u3060', // 12384 だ HIRAGANA LETTER DA
    't', 'i', '\u3061', // 12385 ち HIRAGANA LETTER TI
    'd', 'i', '\u3062', // 12386 ぢ HIRAGANA LETTER DI
    't', 'U', '\u3063', // 12387 っ HIRAGANA LETTER SMALL TU
    't', 'u', '\u3064', // 12388 つ HIRAGANA LETTER TU
    'd', 'u', '\u3065', // 12389 づ HIRAGANA LETTER DU
    't', 'e', '\u3066', // 12390 て HIRAGANA LETTER TE
    'd', 'e', '\u3067', // 12391 で HIRAGANA LETTER DE
    't', 'o', '\u3068', // 12392 と HIRAGANA LETTER TO
    'd', 'o', '\u3069', // 12393 ど HIRAGANA LETTER DO
    'n', 'a', '\u306a', // 12394 な HIRAGANA LETTER NA
    'n', 'i', '\u306b', // 12395 に HIRAGANA LETTER NI
    'n', 'u', '\u306c', // 12396 ぬ HIRAGANA LETTER NU
    'n', 'e', '\u306d', // 12397 ね HIRAGANA LETTER NE
    'n', 'o', '\u306e', // 12398 の HIRAGANA LETTER NO
    'h', 'a', '\u306f', // 12399 は HIRAGANA LETTER HA
    'b', 'a', '\u3070', // 12400 ば HIRAGANA LETTER BA
    'p', 'a', '\u3071', // 12401 ぱ HIRAGANA LETTER PA
    'h', 'i', '\u3072', // 12402 ひ HIRAGANA LETTER HI
    'b', 'i', '\u3073', // 12403 び HIRAGANA LETTER BI
    'p', 'i', '\u3074', // 12404 ぴ HIRAGANA LETTER PI
    'h', 'u', '\u3075', // 12405 ふ HIRAGANA LETTER HU
    'b', 'u', '\u3076', // 12406 ぶ HIRAGANA LETTER BU
    'p', 'u', '\u3077', // 12407 ぷ HIRAGANA LETTER PU
    'h', 'e', '\u3078', // 12408 へ HIRAGANA LETTER HE
    'b', 'e', '\u3079', // 12409 べ HIRAGANA LETTER BE
    'p', 'e', '\u307a', // 12410 ぺ HIRAGANA LETTER PE
    'h', 'o', '\u307b', // 12411 ほ HIRAGANA LETTER HO
    'b', 'o', '\u307c', // 12412 ぼ HIRAGANA LETTER BO
    'p', 'o', '\u307d', // 12413 ぽ HIRAGANA LETTER PO
    'm', 'a', '\u307e', // 12414 ま HIRAGANA LETTER MA
    'm', 'i', '\u307f', // 12415 み HIRAGANA LETTER MI
    'm', 'u', '\u3080', // 12416 む HIRAGANA LETTER MU
    'm', 'e', '\u3081', // 12417 め HIRAGANA LETTER ME
    'm', 'o', '\u3082', // 12418 も HIRAGANA LETTER MO
    'y', 'A', '\u3083', // 12419 ゃ HIRAGANA LETTER SMALL YA
    'y', 'a', '\u3084', // 12420 や HIRAGANA LETTER YA
    'y', 'U', '\u3085', // 12421 ゅ HIRAGANA LETTER SMALL YU
    'y', 'u', '\u3086', // 12422 ゆ HIRAGANA LETTER YU
    'y', 'O', '\u3087', // 12423 ょ HIRAGANA LETTER SMALL YO
    'y', 'o', '\u3088', // 12424 よ HIRAGANA LETTER YO
    'r', 'a', '\u3089', // 12425 ら HIRAGANA LETTER RA
    'r', 'i', '\u308a', // 12426 り HIRAGANA LETTER RI
    'r', 'u', '\u308b', // 12427 る HIRAGANA LETTER RU
    'r', 'e', '\u308c', // 12428 れ HIRAGANA LETTER RE
    'r', 'o', '\u308d', // 12429 ろ HIRAGANA LETTER RO
    'w', 'A', '\u308e', // 12430 ゎ HIRAGANA LETTER SMALL WA
    'w', 'a', '\u308f', // 12431 わ HIRAGANA LETTER WA
    'w', 'i', '\u3090', // 12432 ゐ HIRAGANA LETTER WI
    'w', 'e', '\u3091', // 12433 ゑ HIRAGANA LETTER WE
    'w', 'o', '\u3092', // 12434 を HIRAGANA LETTER WO
    'n', '5', '\u3093', // 12435 ん HIRAGANA LETTER N
    'v', 'u', '\u3094', // 12436 ゔ HIRAGANA LETTER VU
    '"', '5', '\u309b', // 12443 ゛ KATAKANA-HIRAGANA VOICED SOUND MARK
    '0', '5', '\u309c', // 12444 ゜ KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
    '*', '5', '\u309d', // 12445 ゝ HIRAGANA ITERATION MARK
    '+', '5', '\u309e', // 12446 ゞ HIRAGANA VOICED ITERATION MARK
    'a', '6', '\u30a1', // 12449 ァ KATAKANA LETTER SMALL A
    'A', '6', '\u30a2', // 12450 ア KATAKANA LETTER A
    'i', '6', '\u30a3', // 12451 ィ KATAKANA LETTER SMALL I
    'I', '6', '\u30a4', // 12452 イ KATAKANA LETTER I
    'u', '6', '\u30a5', // 12453 ゥ KATAKANA LETTER SMALL U
    'U', '6', '\u30a6', // 12454 ウ KATAKANA LETTER U
    'e', '6', '\u30a7', // 12455 ェ KATAKANA LETTER SMALL E
    'E', '6', '\u30a8', // 12456 エ KATAKANA LETTER E
    'o', '6', '\u30a9', // 12457 ォ KATAKANA LETTER SMALL O
    'O', '6', '\u30aa', // 12458 オ KATAKANA LETTER O
    'K', 'a', '\u30ab', // 12459 カ KATAKANA LETTER KA
    'G', 'a', '\u30ac', // 12460 ガ KATAKANA LETTER GA
    'K', 'i', '\u30ad', // 12461 キ KATAKANA LETTER KI
    'G', 'i', '\u30ae', // 12462 ギ KATAKANA LETTER GI
    'K', 'u', '\u30af', // 12463 ク KATAKANA LETTER KU
    'G', 'u', '\u30b0', // 12464 グ KATAKANA LETTER GU
    'K', 'e', '\u30b1', // 12465 ケ KATAKANA LETTER KE
    'G', 'e', '\u30b2', // 12466 ゲ KATAKANA LETTER GE
    'K', 'o', '\u30b3', // 12467 コ KATAKANA LETTER KO
    'G', 'o', '\u30b4', // 12468 ゴ KATAKANA LETTER GO
    'S', 'a', '\u30b5', // 12469 サ KATAKANA LETTER SA
    'Z', 'a', '\u30b6', // 12470 ザ KATAKANA LETTER ZA
    'S', 'i', '\u30b7', // 12471 シ KATAKANA LETTER SI
    'Z', 'i', '\u30b8', // 12472 ジ KATAKANA LETTER ZI
    'S', 'u', '\u30b9', // 12473 ス KATAKANA LETTER SU
    'Z', 'u', '\u30ba', // 12474 ズ KATAKANA LETTER ZU
    'S', 'e', '\u30bb', // 12475 セ KATAKANA LETTER SE
    'Z', 'e', '\u30bc', // 12476 ゼ KATAKANA LETTER ZE
    'S', 'o', '\u30bd', // 12477 ソ KATAKANA LETTER SO
    'Z', 'o', '\u30be', // 12478 ゾ KATAKANA LETTER ZO
    'T', 'a', '\u30bf', // 12479 タ KATAKANA LETTER TA
    'D', 'a', '\u30c0', // 12480 ダ KATAKANA LETTER DA
    'T', 'i', '\u30c1', // 12481 チ KATAKANA LETTER TI
    'D', 'i', '\u30c2', // 12482 ヂ KATAKANA LETTER DI
    'T', 'U', '\u30c3', // 12483 ッ KATAKANA LETTER SMALL TU
    'T', 'u', '\u30c4', // 12484 ツ KATAKANA LETTER TU
    'D', 'u', '\u30c5', // 12485 ヅ KATAKANA LETTER DU
    'T', 'e', '\u30c6', // 12486 テ KATAKANA LETTER TE
    'D', 'e', '\u30c7', // 12487 デ KATAKANA LETTER DE
    'T', 'o', '\u30c8', // 12488 ト KATAKANA LETTER TO
    'D', 'o', '\u30c9', // 12489 ド KATAKANA LETTER DO
    'N', 'a', '\u30ca', // 12490 ナ KATAKANA LETTER NA
    'N', 'i', '\u30cb', // 12491 ニ KATAKANA LETTER NI
    'N', 'u', '\u30cc', // 12492 ヌ KATAKANA LETTER NU
    'N', 'e', '\u30cd', // 12493 ネ KATAKANA LETTER NE
    'N', 'o', '\u30ce', // 12494 ノ KATAKANA LETTER NO
    'H', 'a', '\u30cf', // 12495 ハ KATAKANA LETTER HA
    'B', 'a', '\u30d0', // 12496 バ KATAKANA LETTER BA
    'P', 'a', '\u30d1', // 12497 パ KATAKANA LETTER PA
    'H', 'i', '\u30d2', // 12498 ヒ KATAKANA LETTER HI
    'B', 'i', '\u30d3', // 12499 ビ KATAKANA LETTER BI
    'P', 'i', '\u30d4', // 12500 ピ KATAKANA LETTER PI
    'H', 'u', '\u30d5', // 12501 フ KATAKANA LETTER HU
    'B', 'u', '\u30d6', // 12502 ブ KATAKANA LETTER BU
    'P', 'u', '\u30d7', // 12503 プ KATAKANA LETTER PU
    'H', 'e', '\u30d8', // 12504 ヘ KATAKANA LETTER HE
    'B', 'e', '\u30d9', // 12505 ベ KATAKANA LETTER BE
    'P', 'e', '\u30da', // 12506 ペ KATAKANA LETTER PE
    'H', 'o', '\u30db', // 12507 ホ KATAKANA LETTER HO
    'B', 'o', '\u30dc', // 12508 ボ KATAKANA LETTER BO
    'P', 'o', '\u30dd', // 12509 ポ KATAKANA LETTER PO
    'M', 'a', '\u30de', // 12510 マ KATAKANA LETTER MA
    'M', 'i', '\u30df', // 12511 ミ KATAKANA LETTER MI
    'M', 'u', '\u30e0', // 12512 ム KATAKANA LETTER MU
    'M', 'e', '\u30e1', // 12513 メ KATAKANA LETTER ME
    'M', 'o', '\u30e2', // 12514 モ KATAKANA LETTER MO
    'Y', 'A', '\u30e3', // 12515 ャ KATAKANA LETTER SMALL YA
    'Y', 'a', '\u30e4', // 12516 ヤ KATAKANA LETTER YA
    'Y', 'U', '\u30e5', // 12517 ュ KATAKANA LETTER SMALL YU
    'Y', 'u', '\u30e6', // 12518 ユ KATAKANA LETTER YU
    'Y', 'O', '\u30e7', // 12519 ョ KATAKANA LETTER SMALL YO
    'Y', 'o', '\u30e8', // 12520 ヨ KATAKANA LETTER YO
    'R', 'a', '\u30e9', // 12521 ラ KATAKANA LETTER RA
    'R', 'i', '\u30ea', // 12522 リ KATAKANA LETTER RI
    'R', 'u', '\u30eb', // 12523 ル KATAKANA LETTER RU
    'R', 'e', '\u30ec', // 12524 レ KATAKANA LETTER RE
    'R', 'o', '\u30ed', // 12525 ロ KATAKANA LETTER RO
    'W', 'A', '\u30ee', // 12526 ヮ KATAKANA LETTER SMALL WA
    'W', 'a', '\u30ef', // 12527 ワ KATAKANA LETTER WA
    'W', 'i', '\u30f0', // 12528 ヰ KATAKANA LETTER WI
    'W', 'e', '\u30f1', // 12529 ヱ KATAKANA LETTER WE
    'W', 'o', '\u30f2', // 12530 ヲ KATAKANA LETTER WO
    'N', '6', '\u30f3', // 12531 ン KATAKANA LETTER N
    'V', 'u', '\u30f4', // 12532 ヴ KATAKANA LETTER VU
    'K', 'A', '\u30f5', // 12533 ヵ KATAKANA LETTER SMALL KA
    'K', 'E', '\u30f6', // 12534 ヶ KATAKANA LETTER SMALL KE
    'V', 'a', '\u30f7', // 12535 ヷ KATAKANA LETTER VA
    'V', 'i', '\u30f8', // 12536 ヸ KATAKANA LETTER VI
    'V', 'e', '\u30f9', // 12537 ヹ KATAKANA LETTER VE
    'V', 'o', '\u30fa', // 12538 ヺ KATAKANA LETTER VO
    '.', '6', '\u30fb', // 12539 ・ KATAKANA MIDDLE DOT
    '-', '6', '\u30fc', // 12540 ー KATAKANA-HIRAGANA PROLONGED SOUND MARK
    '*', '6', '\u30fd', // 12541 ヽ KATAKANA ITERATION MARK
    '+', '6', '\u30fe', // 12542 ヾ KATAKANA VOICED ITERATION MARK
    'b', '4', '\u3105', // 12549 ㄅ BOPOMOFO LETTER B
    'p', '4', '\u3106', // 12550 ㄆ BOPOMOFO LETTER P
    'm', '4', '\u3107', // 12551 ㄇ BOPOMOFO LETTER M
    'f', '4', '\u3108', // 12552 ㄈ BOPOMOFO LETTER F
    'd', '4', '\u3109', // 12553 ㄉ BOPOMOFO LETTER D
    't', '4', '\u310a', // 12554 ㄊ BOPOMOFO LETTER T
    'n', '4', '\u310b', // 12555 ㄋ BOPOMOFO LETTER N
    'l', '4', '\u310c', // 12556 ㄌ BOPOMOFO LETTER L
    'g', '4', '\u310d', // 12557 ㄍ BOPOMOFO LETTER G
    'k', '4', '\u310e', // 12558 ㄎ BOPOMOFO LETTER K
    'h', '4', '\u310f', // 12559 ㄏ BOPOMOFO LETTER H
    'j', '4', '\u3110', // 12560 ㄐ BOPOMOFO LETTER J
    'q', '4', '\u3111', // 12561 ㄑ BOPOMOFO LETTER Q
    'x', '4', '\u3112', // 12562 ㄒ BOPOMOFO LETTER X
    'z', 'h', '\u3113', // 12563 ㄓ BOPOMOFO LETTER ZH
    'c', 'h', '\u3114', // 12564 ㄔ BOPOMOFO LETTER CH
    's', 'h', '\u3115', // 12565 ㄕ BOPOMOFO LETTER SH
    'r', '4', '\u3116', // 12566 ㄖ BOPOMOFO LETTER R
    'z', '4', '\u3117', // 12567 ㄗ BOPOMOFO LETTER Z
    'c', '4', '\u3118', // 12568 ㄘ BOPOMOFO LETTER C
    's', '4', '\u3119', // 12569 ㄙ BOPOMOFO LETTER S
    'a', '4', '\u311a', // 12570 ㄚ BOPOMOFO LETTER A
    'o', '4', '\u311b', // 12571 ㄛ BOPOMOFO LETTER O
    'e', '4', '\u311c', // 12572 ㄜ BOPOMOFO LETTER E
    'a', 'i', '\u311e', // 12574 ㄞ BOPOMOFO LETTER AI
    'e', 'i', '\u311f', // 12575 ㄟ BOPOMOFO LETTER EI
    'a', 'u', '\u3120', // 12576 ㄠ BOPOMOFO LETTER AU
    'o', 'u', '\u3121', // 12577 ㄡ BOPOMOFO LETTER OU
    'a', 'n', '\u3122', // 12578 ㄢ BOPOMOFO LETTER AN
    'e', 'n', '\u3123', // 12579 ㄣ BOPOMOFO LETTER EN
    'a', 'N', '\u3124', // 12580 ㄤ BOPOMOFO LETTER ANG
    'e', 'N', '\u3125', // 12581 ㄥ BOPOMOFO LETTER ENG
    'e', 'r', '\u3126', // 12582 ㄦ BOPOMOFO LETTER ER
    'i', '4', '\u3127', // 12583 ㄧ BOPOMOFO LETTER I
    'u', '4', '\u3128', // 12584 ㄨ BOPOMOFO LETTER U
    'i', 'u', '\u3129', // 12585 ㄩ BOPOMOFO LETTER IU
    'v', '4', '\u312a', // 12586 ㄪ BOPOMOFO LETTER V
    'n', 'G', '\u312b', // 12587 ㄫ BOPOMOFO LETTER NG
    'g', 'n', '\u312c', // 12588 ㄬ BOPOMOFO LETTER GN
    '1', 'c', '\u3220', // 12832 ㈠ PARENTHESIZED IDEOGRAPH ONE
    '2', 'c', '\u3221', // 12833 ㈡ PARENTHESIZED IDEOGRAPH TWO
    '3', 'c', '\u3222', // 12834 ㈢ PARENTHESIZED IDEOGRAPH THREE
    '4', 'c', '\u3223', // 12835 ㈣ PARENTHESIZED IDEOGRAPH FOUR
    '5', 'c', '\u3224', // 12836 ㈤ PARENTHESIZED IDEOGRAPH FIVE
    '6', 'c', '\u3225', // 12837 ㈥ PARENTHESIZED IDEOGRAPH SIX
    '7', 'c', '\u3226', // 12838 ㈦ PARENTHESIZED IDEOGRAPH SEVEN
    '8', 'c', '\u3227', // 12839 ㈧ PARENTHESIZED IDEOGRAPH EIGHT
    '9', 'c', '\u3228', // 12840 ㈨ PARENTHESIZED IDEOGRAPH NINE
    'f', 'f', '\ufb00', // 64256 ﬀ LATIN SMALL LIGATURE FF
    'f', 'i', '\ufb01', // 64257 ﬁ LATIN SMALL LIGATURE FI
    'f', 'l', '\ufb02', // 64258 ﬂ LATIN SMALL LIGATURE FL
    'f', 't', '\ufb05', // 64261 ﬅ LATIN SMALL LIGATURE LONG S T
    's', 't', '\ufb06', // 64262 ﬆ LATIN SMALL LIGATURE ST
  )

  /**
   * A map of digraph to character codepoint
   *
   * Note that this might contain duplicates for the character!
   */
  private val digraphToCodepoint: MutableMap<String, Int> = HashMap<String, Int>(defaultDigraphs.size)

  /**
   * A map of character codepoint to digraph, as a concatenated string
   *
   * Note that when a character has multiple digraphs (e.g. `!I` and `~!`), only the first is kept!
   */
  private val codepointToDigraph: MutableMap<Int, String> = TreeMap<Int, String>()

  /**
   * A map of custom digraph to a digraph codepoint
   *
   * This property uses [LinkedHashMap] so that iteration order matches insertion order. The digraph character is
   * represented as a codepoint to handle wide Unicode characters that cannot be represented in a 16-bit [Char].
   */
  private val customDigraphToCodepoint: MutableMap<String, Int> = LinkedHashMap<String, Int>()

  /**
   * A map of custom digraph codepoints to digraph
   */
  private val customCodepointToDigraph: MutableMap<Int, String> = HashMap<Int, String>()

  /**
   * A map of Unicode block to Vim digraph header name/display text
   *
   * This map only contains a name for the Unicode blocks that Vim outputs. If no display text exists for a Unicode
   * block, then it's not displayed as a separate header
   */
  private val digraphHeaderNames = mapOf(
    Character.UnicodeBlock.LATIN_1_SUPPLEMENT to "Latin supplement",
    Character.UnicodeBlock.GREEK to "Greek and Coptic",
    Character.UnicodeBlock.CYRILLIC to "Cyrillic",
    Character.UnicodeBlock.HEBREW to "Hebrew",
    Character.UnicodeBlock.ARABIC to "Arabic",
    Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL to "Latin extended",
    Character.UnicodeBlock.GREEK_EXTENDED to "Greek extended",
    Character.UnicodeBlock.GENERAL_PUNCTUATION to "Punctuation",
    Character.UnicodeBlock.SUPERSCRIPTS_AND_SUBSCRIPTS to "Super- and subscripts",
    Character.UnicodeBlock.CURRENCY_SYMBOLS to "Currency",
    Character.UnicodeBlock.LETTERLIKE_SYMBOLS to "Other",
    Character.UnicodeBlock.NUMBER_FORMS to "Roman numbers",
    Character.UnicodeBlock.ARROWS to "Arrows",
    Character.UnicodeBlock.MATHEMATICAL_OPERATORS to "Mathematical operators",
    Character.UnicodeBlock.MISCELLANEOUS_TECHNICAL to "Technical",
    Character.UnicodeBlock.CONTROL_PICTURES to "Other",
    Character.UnicodeBlock.BOX_DRAWING to "Box drawing",
    Character.UnicodeBlock.BLOCK_ELEMENTS to "Block elements",
    Character.UnicodeBlock.GEOMETRIC_SHAPES to "Geometric shapes",
    Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS to "Symbols",
    Character.UnicodeBlock.DINGBATS to "Dingbats",
    Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION to "CJK symbols and punctuation",
    Character.UnicodeBlock.HIRAGANA to "Hiragana",
    Character.UnicodeBlock.KATAKANA to "Katakana",
    Character.UnicodeBlock.BOPOMOFO to "Bopomofo",
    Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS to "Other",
  )

  init {
    loadDigraphs()
  }
}
