/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ui.MorePanel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.TreeMap;

public class DigraphGroup extends AbstractActionGroup {
  public DigraphGroup() {
    loadDigraphs();
  }

  public char getDigraph(char ch1, char ch2) {
    String key = new String(new char[]{ch1, ch2});
    Character ch = digraphs.get(key);
    if (ch == null) {
      key = new String(new char[]{ch2, ch1});
      ch = digraphs.get(key);
    }

    if (ch == null) {
      return ch2;
    }
    else {
      return ch;
    }
  }

  public boolean parseCommandLine(@NotNull Editor editor, @NotNull String args, boolean failOnBad) {
    if (args.length() == 0) {
      showDigraphs(editor);

      return true;
    }

    return true;
  }

  private void showDigraphs(@NotNull Editor editor) {
    MorePanel panel = MorePanel.getInstance(editor);
    int width = panel.getDisplayWidth();
    if (width < 10) {
      width = 80;
    }
    int colCount = width / 12;
    int height = (int)Math.ceil((double)digraphs.size() / (double)colCount);

    if (logger.isDebugEnabled()) {
      logger.debug("width=" + width);
      logger.debug("colCount=" + colCount);
      logger.debug("height=" + height);
    }

    StringBuffer res = new StringBuffer();
    int cnt = 0;
    for (Character code : keys.keySet()) {
      String key = keys.get(code);

      res.append(key);
      res.append(' ');
      if (code < 32) {
        res.append('^');
        res.append((char)(code + '@'));
      }
      else if (code >= 128 && code <= 159) {
        res.append('~');
        res.append((char)(code - 128 + '@'));
      }
      else {
        res.append(code);
        res.append(' ');
      }
      res.append(' ');
      if (code < 0x1000) {
        res.append('0');
      }
      if (code < 0x100) {
        res.append('0');
      }
      if (code < 0x10) {
        res.append('0');
      }
      res.append(Integer.toHexString(code));
      res.append("  ");

      cnt++;
      if (cnt == colCount) {
        res.append('\n');
        cnt = 0;
      }
    }

    panel.setText(res.toString());
  }

  private void loadDigraphs() {
    for (int i = 0; i < defaultDigraphs.length; i += 3) {
      if (defaultDigraphs[i] != '\0' && defaultDigraphs[i + 1] != '\0') {
        char ch = defaultDigraphs[i + 2];
        String key = new String(new char[]{defaultDigraphs[i], defaultDigraphs[i + 1]});
        digraphs.put(key, ch);
        keys.put(ch, key);
      }
    }

    // TODO - load custom digraphs from .vimrc
  }

  @NotNull private HashMap<String, Character> digraphs = new HashMap<String, Character>(defaultDigraphs.length);
  @NotNull private TreeMap<Character, String> keys = new TreeMap<Character, String>();

  private static final char defaultDigraphs[] = {
    /*
    'N', 'U', // 0   ^@
    'S', 'H', // 1   ^A
    'S', 'X', // 2   ^B
    'E', 'X', // 3   ^C
    'E', 'T', // 4   ^D
    'E', 'Q', // 5   ^E
    'A', 'K', // 6   ^F
    'B', 'L', // 7   ^G
    'B', 'S', // 8   ^H
    'H', 'T', // 9   ^I
    'L', 'F', // 10  ^J
    'V', 'T', // 11  ^K
    'F', 'F', // 12  ^L
    'C', 'R', // 13  ^M
    'S', 'O', // 14  ^N
    'S', 'I', // 15  ^O
    'D', 'L', // 16  ^P
    'D', '1', // 17  ^Q
    'D', '2', // 18  ^R
    'D', '3', // 19  ^S
    'D', '4', // 20  ^T
    'N', 'K', // 21  ^U
    'S', 'Y', // 22  ^V
    'E', 'B', // 23  ^W
    'C', 'N', // 24  ^X
    'E', 'M', // 25  ^Y
    'S', 'B', // 26  ^Z
    'E', 'C', // 27  ^[
    'F', 'S', // 28  ^\
    'G', 'S', // 29  ^]
    'R', 'S', // 30  ^^
    'U', 'S', // 31  ^_
    'S', 'P', // 32  Space
    '\0', '\0', // 33  Unused
    '\0', '\0', // 34  Unused
    'N', 'b', // 35  #
    'D', 'O', // 36  $
    '\0', '\0', // 37  Unused
    '\0', '\0', // 38  Unused
    '\0', '\0', // 39  Unused
    '\0', '\0', // 40  Unused
    '\0', '\0', // 41  Unused
    '\0', '\0', // 42  Unused
    '\0', '\0', // 43  Unused
    '\0', '\0', // 44  Unused
    '\0', '\0', // 45  Unused
    '\0', '\0', // 46  Unused
    '\0', '\0', // 47  Unused
    '\0', '\0', // 48  Unused
    '\0', '\0', // 49  Unused
    '\0', '\0', // 50  Unused
    '\0', '\0', // 51  Unused
    '\0', '\0', // 52  Unused
    '\0', '\0', // 53  Unused
    '\0', '\0', // 54  Unused
    '\0', '\0', // 55  Unused
    '\0', '\0', // 56  Unused
    '\0', '\0', // 57  Unused
    '\0', '\0', // 58  Unused
    '\0', '\0', // 59  Unused
    '\0', '\0', // 60  Unused
    '\0', '\0', // 61  Unused
    '\0', '\0', // 62  Unused
    '\0', '\0', // 63  Unused
    'A', 't', // 64  @
    '\0', '\0', // 65  Unused
    '\0', '\0', // 66  Unused
    '\0', '\0', // 67  Unused
    '\0', '\0', // 68  Unused
    '\0', '\0', // 69  Unused
    '\0', '\0', // 70  Unused
    '\0', '\0', // 71  Unused
    '\0', '\0', // 72  Unused
    '\0', '\0', // 73  Unused
    '\0', '\0', // 74  Unused
    '\0', '\0', // 75  Unused
    '\0', '\0', // 76  Unused
    '\0', '\0', // 77  Unused
    '\0', '\0', // 78  Unused
    '\0', '\0', // 79  Unused
    '\0', '\0', // 80  Unused
    '\0', '\0', // 81  Unused
    '\0', '\0', // 82  Unused
    '\0', '\0', // 83  Unused
    '\0', '\0', // 84  Unused
    '\0', '\0', // 85  Unused
    '\0', '\0', // 86  Unused
    '\0', '\0', // 87  Unused
    '\0', '\0', // 88  Unused
    '\0', '\0', // 89  Unused
    '\0', '\0', // 90  Unused
    '<', '(', // 91  [
    '/', '/', // 92  \
    ')', '>', // 93  ]
    '\'', '>', // 94  ^
    '\0', '\0', // 95  Unused
    '\'', '!', // 96  `
    '\0', '\0', // 97  Unused
    '\0', '\0', // 98  Unused
    '\0', '\0', // 99  Unused
    '\0', '\0', // 100 Unused
    '\0', '\0', // 101 Unused
    '\0', '\0', // 102 Unused
    '\0', '\0', // 103 Unused
    '\0', '\0', // 104 Unused
    '\0', '\0', // 105 Unused
    '\0', '\0', // 106 Unused
    '\0', '\0', // 107 Unused
    '\0', '\0', // 108 Unused
    '\0', '\0', // 109 Unused
    '\0', '\0', // 110 Unused
    '\0', '\0', // 111 Unused
    '\0', '\0', // 112 Unused
    '\0', '\0', // 113 Unused
    '\0', '\0', // 114 Unused
    '\0', '\0', // 115 Unused
    '\0', '\0', // 116 Unused
    '\0', '\0', // 117 Unused
    '\0', '\0', // 118 Unused
    '\0', '\0', // 119 Unused
    '\0', '\0', // 120 Unused
    '\0', '\0', // 121 Unused
    '\0', '\0', // 122 Unused
    '(', '!', // 123 {
    '!', '!', // 124 |
    '!', ')', // 125 }
    '\'', '?', // 126 ~
    'D', 'T', // 127 ^?
    'P', 'A', // 128 ~@
    'H', 'O', // 129 ~A
    'B', 'H', // 130 ~B
    'N', 'H', // 131 ~C
    'I', 'N', // 132 ~D
    'N', 'L', // 133 ~E
    'S', 'A', // 134 ~F
    'E', 'S', // 135 ~G
    'H', 'S', // 136 ~H
    'H', 'J', // 137 ~I
    'V', 'S', // 138 ~J
    'P', 'D', // 139 ~K
    'P', 'U', // 140 ~L
    'R', 'I', // 141 ~M
    'S', '2', // 142 ~N
    'S', '3', // 143 ~O
    'D', 'C', // 144 ~P
    'P', '1', // 145 ~Q
    'P', '2', // 146 ~R
    'T', 'S', // 147 ~S
    'C', 'C', // 148 ~T
    'M', 'W', // 149 ~U
    'S', 'G', // 150 ~V
    'E', 'G', // 151 ~W
    'S', 'S', // 152 ~X
    'G', 'C', // 153 ~Y
    'S', 'C', // 154 ~Z
    'C', 'I', // 155 ~[
    'S', 'T', // 156 ~\
    'O', 'C', // 157 ~]
    'P', 'M', // 158 ~^
    'A', 'C', // 159 ~_
    'N', 'S', // 160 |
    '!', 'I', // 161
    'C', 't', // 162
    'P', 'd', // 163
    'C', 'u', // 164
    'Y', 'e', // 165
    'B', 'B', // 166
    'S', 'E', // 167
    '\'', ':', // 168
    'C', 'o', // 169
    '-', 'a', // 170
    '<', '<', // 171
    'N', 'O', // 172
    '-', '-', // 173
    'R', 'g', // 174
    '\'', 'm', // 175
    'D', 'G', // 176
    '+', '-', // 177
    '2', 'S', // 178
    '3', 'S', // 179
    '\'', '\'', // 180
    'M', 'y', // 181
    'P', 'I', // 182
    '.', 'M', // 183
    '\'', ',', // 184
    '1', 'S', // 185
    '-', 'o', // 186
    '>', '>', // 187
    '1', '4', // 188
    '1', '2', // 189
    '3', '4', // 190
    '?', 'I', // 191
    'A', '!', // 192
    'A', '\'', // 193
    'A', '>', // 194
    'A', '?', // 195
    'A', ':', // 196
    'A', 'A', // 197
    'A', 'E', // 198
    'C', ',', // 199
    'E', '!', // 200
    'E', '\'', // 201
    'E', '>', // 202
    'E', ':', // 203
    'I', '!', // 204
    'I', '\'', // 205
    'I', '>', // 206
    'I', ':', // 207
    'D', '-', // 208
    'N', '?', // 209
    'O', '!', // 210
    'O', '\'', // 211
    'O', '>', // 212
    'O', '?', // 213
    'O', ':', // 214
    '*', 'X', // 215
    'O', '/', // 216
    'U', '!', // 217
    'U', '\'', // 218
    'U', '>', // 219
    'U', ':', // 220
    'Y', '\'', // 221
    'T', 'H', // 222
    's', 's', // 223
    'a', '!', // 224
    'a', '\'', // 225
    'a', '>', // 226
    'a', '?', // 227
    'a', ':', // 228
    'a', 'a', // 229
    'a', 'e', // 230
    'c', ',', // 231
    'e', '!', // 232
    'e', '\'', // 233
    'e', '>', // 234
    'e', ':', // 235
    'i', '!', // 236
    'i', '\'', // 237
    'i', '>', // 238
    'i', ':', // 239
    'd', '-', // 240
    'n', '?', // 241
    'o', '!', // 242
    'o', '\'', // 243
    'o', '>', // 244
    'o', '?', // 245
    'o', ':', // 246
    '-', ':', // 247
    'o', '/', // 248
    'u', '!', // 249
    'u', '\'', // 250
    'u', '>', // 251
    'u', ':', // 252
    'y', '\'', // 253
    't', 'h', // 254
    'y', ':', // 255
    */
    'N', 'U', '\u0000', // NULL (NUL)
    'S', 'H', '\u0001', // START OF HEADING (SOH)
    'S', 'X', '\u0002', // START OF TEXT (STX)
    'E', 'X', '\u0003', // END OF TEXT (ETX)
    'E', 'T', '\u0004', // END OF TRANSMISSION (EOT)
    'E', 'Q', '\u0005', // ENQUIRY (ENQ)
    'A', 'K', '\u0006', // ACKNOWLEDGE (ACK)
    'B', 'L', '\u0007', // BELL (BEL)
    'B', 'S', '\u0008', // BACKSPACE (BS)
    'H', 'T', '\u0009', // CHARACTER TABULATION (HT)
    'L', 'F', 0x000a, // LINE FEED (LF)
    'V', 'T', '\u000b', // LINE TABULATION (VT)
    'F', 'F', '\u000c', // FORM FEED (FF)
    'C', 'R', 0x000d, // CARRIAGE RETURN (CR)
    'S', 'O', '\u000e', // SHIFT OUT (SO)
    'S', 'I', '\u000f', // SHIFT IN (SI)
    'D', 'L', '\u0010', // DATALINK ESCAPE (DLE)
    'D', '1', '\u0011', // DEVICE CONTROL ONE (DC1)
    'D', '2', '\u0012', // DEVICE CONTROL TWO (DC2)
    'D', '3', '\u0013', // DEVICE CONTROL THREE (DC3)
    'D', '4', '\u0014', // DEVICE CONTROL FOUR (DC4)
    'N', 'K', '\u0015', // NEGATIVE ACKNOWLEDGE (NAK)
    'S', 'Y', '\u0016', // SYNCRONOUS IDLE (SYN)
    'E', 'B', '\u0017', // END OF TRANSMISSION BLOCK (ETB)
    'C', 'N', '\u0018', // CANCEL (CAN)
    'E', 'M', '\u0019', // END OF MEDIUM (EM)
    'S', 'B', '\u001a', // SUBSTITUTE (SUB)
    'E', 'C', '\u001b', // ESCAPE (ESC)
    'F', 'S', '\u001c', // FILE SEPARATOR (IS4)
    'G', 'S', '\u001d', // GROUP SEPARATOR (IS3)
    'R', 'S', '\u001e', // RECORD SEPARATOR (IS2)
    'U', 'S', '\u001f', // UNIT SEPARATOR (IS1)
    'S', 'P', '\u0020', // SPACE
    'N', 'b', '\u0023', // NUMBER SIGN
    'D', 'O', '\u0024', // DOLLAR SIGN
    'A', 't', '\u0040', // COMMERCIAL AT
    '<', '(', '\u005b', // LEFT SQUARE BRACKET
    '/', '/', 0x005c, // REVERSE SOLIDUS
    ')', '>', '\u005d', // RIGHT SQUARE BRACKET
    '\'', '>', '\u005e', // CIRCUMFLEX ACCENT
    '\'', '!', '\u0060', // GRAVE ACCENT
    '(', '!', '\u007b', // LEFT CURLY BRACKET
    '!', '!', '\u007c', // VERTICAL LINE
    '!', ')', '\u007d', // RIGHT CURLY BRACKET
    '\'', '?', '\u007e', // TILDE
    'D', 'T', '\u007f', // DELETE (DEL)
    'P', 'A', '\u0080', // PADDING CHARACTER (PAD)
    'H', 'O', '\u0081', // HIGH OCTET PRESET (HOP)
    'B', 'H', '\u0082', // BREAK PERMITTED HERE (BPH)
    'N', 'H', '\u0083', // NO BREAK HERE (NBH)
    'I', 'N', '\u0084', // INDEX (IND)
    'N', 'L', '\u0085', // NEXT LINE (NEL)
    'S', 'A', '\u0086', // START OF SELECTED AREA (SSA)
    'E', 'S', '\u0087', // END OF SELECTED AREA (ESA)
    'H', 'S', '\u0088', // CHARACTER TABULATION SET (HTS)
    'H', 'J', '\u0089', // CHARACTER TABULATION WITH JUSTIFICATION (HTJ)
    'V', 'S', '\u008a', // LINE TABULATION SET (VTS)
    'P', 'D', '\u008b', // PARTIAL LINE FORWARD (PLD)
    'P', 'U', '\u008c', // PARTIAL LINE BACKWARD (PLU)
    'R', 'I', '\u008d', // REVERSE LINE FEED (RI)
    'S', '2', '\u008e', // SINGLE-SHIFT TWO (SS2)
    'S', '3', '\u008f', // SINGLE-SHIFT THREE (SS3)
    'D', 'C', '\u0090', // DEVICE CONTROL STRING (DCS)
    'P', '1', '\u0091', // PRIVATE USE ONE (PU1)
    'P', '2', '\u0092', // PRIVATE USE TWO (PU2)
    'T', 'S', '\u0093', // SET TRANSMIT STATE (STS)
    'C', 'C', '\u0094', // CANCEL CHARACTER (CCH)
    'M', 'W', '\u0095', // MESSAGE WAITING (MW)
    'S', 'G', '\u0096', // START OF GUARDED AREA (SPA)
    'E', 'G', '\u0097', // END OF GUARDED AREA (EPA)
    'S', 'S', '\u0098', // START OF STRING (SOS)
    'G', 'C', '\u0099', // SINGLE GRAPHIC CHARACTER INTRODUCER (SGCI)
    'S', 'C', '\u009a', // SINGLE CHARACTER INTRODUCER (SCI)
    'C', 'I', '\u009b', // CONTROL SEQUENCE INTRODUCER (CSI)
    'S', 'T', '\u009c', // STRING TERMINATOR (ST)
    'O', 'C', '\u009d', // OPERATING SYSTEM COMMAND (OSC)
    'P', 'M', '\u009e', // PRIVACY MESSAGE (PM)
    'A', 'C', '\u009f', // APPLICATION PROGRAM COMMAND (APC)
    'N', 'S', '\u00a0', // NO-BREAK SPACE
    '!', 'I', '\u00a1', // INVERTED EXCLAMATION MARK
    'C', 't', '\u00a2', // CENT SIGN
    'P', 'd', '\u00a3', // POUND SIGN
    'C', 'u', '\u00a4', // CURRENCY SIGN
    'Y', 'e', '\u00a5', // YEN SIGN
    'B', 'B', '\u00a6', // BROKEN BAR
    'S', 'E', '\u00a7', // SECTION SIGN
    '\'', ':', '\u00a8', // DIAERESIS
    'C', 'o', '\u00a9', // COPYRIGHT SIGN
    '-', 'a', '\u00aa', // FEMININE ORDINAL INDICATOR
    '<', '<', '\u00ab', // LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
    'N', 'O', '\u00ac', // NOT SIGN
    '-', '-', '\u00ad', // SOFT HYPHEN
    'R', 'g', '\u00ae', // REGISTERED SIGN
    '\'', 'm', '\u00af', // MACRON
    'D', 'G', '\u00b0', // DEGREE SIGN
    '+', '-', '\u00b1', // PLUS-MINUS SIGN
    '2', 'S', '\u00b2', // SUPERSCRIPT TWO
    '3', 'S', '\u00b3', // SUPERSCRIPT THREE
    '\'', '\'', '\u00b4', // ACUTE ACCENT
    'M', 'y', '\u00b5', // MICRO SIGN
    'P', 'I', '\u00b6', // PILCROW SIGN
    '.', 'M', '\u00b7', // MIDDLE DOT
    '\'', ',', '\u00b8', // CEDILLA
    '1', 'S', '\u00b9', // SUPERSCRIPT ONE
    '-', 'o', '\u00ba', // MASCULINE ORDINAL INDICATOR
    '>', '>', '\u00bb', // RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
    '1', '4', '\u00bc', // VULGAR FRACTION ONE QUARTER
    '1', '2', '\u00bd', // VULGAR FRACTION ONE HALF
    '3', '4', '\u00be', // VULGAR FRACTION THREE QUARTERS
    '?', 'I', '\u00bf', // INVERTED QUESTION MARK
    'A', '!', '\u00c0', // LATIN CAPITAL LETTER A WITH GRAVE
    'A', '\'', '\u00c1', // LATIN CAPITAL LETTER A WITH ACUTE
    'A', '>', '\u00c2', // LATIN CAPITAL LETTER A WITH CIRCUMFLEX
    'A', '?', '\u00c3', // LATIN CAPITAL LETTER A WITH TILDE
    'A', ':', '\u00c4', // LATIN CAPITAL LETTER A WITH DIAERESIS
    'A', 'A', '\u00c5', // LATIN CAPITAL LETTER A WITH RING ABOVE
    'A', 'E', '\u00c6', // LATIN CAPITAL LETTER AE
    'C', ',', '\u00c7', // LATIN CAPITAL LETTER C WITH CEDILLA
    'E', '!', '\u00c8', // LATIN CAPITAL LETTER E WITH GRAVE
    'E', '\'', '\u00c9', // LATIN CAPITAL LETTER E WITH ACUTE
    'E', '>', '\u00ca', // LATIN CAPITAL LETTER E WITH CIRCUMFLEX
    'E', ':', '\u00cb', // LATIN CAPITAL LETTER E WITH DIAERESIS
    'I', '!', '\u00cc', // LATIN CAPITAL LETTER I WITH GRAVE
    'I', '\'', '\u00cd', // LATIN CAPITAL LETTER I WITH ACUTE
    'I', '>', '\u00ce', // LATIN CAPITAL LETTER I WITH CIRCUMFLEX
    'I', ':', '\u00cf', // LATIN CAPITAL LETTER I WITH DIAERESIS
    'D', '-', '\u00d0', // LATIN CAPITAL LETTER ETH (Icelandic)
    'N', '?', '\u00d1', // LATIN CAPITAL LETTER N WITH TILDE
    'O', '!', '\u00d2', // LATIN CAPITAL LETTER O WITH GRAVE
    'O', '\'', '\u00d3', // LATIN CAPITAL LETTER O WITH ACUTE
    'O', '>', '\u00d4', // LATIN CAPITAL LETTER O WITH CIRCUMFLEX
    'O', '?', '\u00d5', // LATIN CAPITAL LETTER O WITH TILDE
    'O', ':', '\u00d6', // LATIN CAPITAL LETTER O WITH DIAERESIS
    '*', 'X', '\u00d7', // MULTIPLICATION SIGN
    'O', '/', '\u00d8', // LATIN CAPITAL LETTER O WITH STROKE
    'U', '!', '\u00d9', // LATIN CAPITAL LETTER U WITH GRAVE
    'U', '\'', '\u00da', // LATIN CAPITAL LETTER U WITH ACUTE
    'U', '>', '\u00db', // LATIN CAPITAL LETTER U WITH CIRCUMFLEX
    'U', ':', '\u00dc', // LATIN CAPITAL LETTER U WITH DIAERESIS
    'Y', '\'', '\u00dd', // LATIN CAPITAL LETTER Y WITH ACUTE
    'T', 'H', '\u00de', // LATIN CAPITAL LETTER THORN (Icelandic)
    's', 's', '\u00df', // LATIN SMALL LETTER SHARP S (German)
    'a', '!', '\u00e0', // LATIN SMALL LETTER A WITH GRAVE
    'a', '\'', '\u00e1', // LATIN SMALL LETTER A WITH ACUTE
    'a', '>', '\u00e2', // LATIN SMALL LETTER A WITH CIRCUMFLEX
    'a', '?', '\u00e3', // LATIN SMALL LETTER A WITH TILDE
    'a', ':', '\u00e4', // LATIN SMALL LETTER A WITH DIAERESIS
    'a', 'a', '\u00e5', // LATIN SMALL LETTER A WITH RING ABOVE
    'a', 'e', '\u00e6', // LATIN SMALL LETTER AE
    'c', ',', '\u00e7', // LATIN SMALL LETTER C WITH CEDILLA
    'e', '!', '\u00e8', // LATIN SMALL LETTER E WITH GRAVE
    'e', '\'', '\u00e9', // LATIN SMALL LETTER E WITH ACUTE
    'e', '>', '\u00ea', // LATIN SMALL LETTER E WITH CIRCUMFLEX
    'e', ':', '\u00eb', // LATIN SMALL LETTER E WITH DIAERESIS
    'i', '!', '\u00ec', // LATIN SMALL LETTER I WITH GRAVE
    'i', '\'', '\u00ed', // LATIN SMALL LETTER I WITH ACUTE
    'i', '>', '\u00ee', // LATIN SMALL LETTER I WITH CIRCUMFLEX
    'i', ':', '\u00ef', // LATIN SMALL LETTER I WITH DIAERESIS
    'd', '-', '\u00f0', // LATIN SMALL LETTER ETH (Icelandic)
    'n', '?', '\u00f1', // LATIN SMALL LETTER N WITH TILDE
    'o', '!', '\u00f2', // LATIN SMALL LETTER O WITH GRAVE
    'o', '\'', '\u00f3', // LATIN SMALL LETTER O WITH ACUTE
    'o', '>', '\u00f4', // LATIN SMALL LETTER O WITH CIRCUMFLEX
    'o', '?', '\u00f5', // LATIN SMALL LETTER O WITH TILDE
    'o', ':', '\u00f6', // LATIN SMALL LETTER O WITH DIAERESIS
    '-', ':', '\u00f7', // DIVISION SIGN
    'o', '/', '\u00f8', // LATIN SMALL LETTER O WITH STROKE
    'u', '!', '\u00f9', // LATIN SMALL LETTER U WITH GRAVE
    'u', '\'', '\u00fa', // LATIN SMALL LETTER U WITH ACUTE
    'u', '>', '\u00fb', // LATIN SMALL LETTER U WITH CIRCUMFLEX
    'u', ':', '\u00fc', // LATIN SMALL LETTER U WITH DIAERESIS
    'y', '\'', '\u00fd', // LATIN SMALL LETTER Y WITH ACUTE
    't', 'h', '\u00fe', // LATIN SMALL LETTER THORN (Icelandic)
    'y', ':', '\u00ff', // LATIN SMALL LETTER Y WITH DIAERESIS
    'A', '-', '\u0100', // LATIN CAPITAL LETTER A WITH MACRON
    'a', '-', '\u0101', // LATIN SMALL LETTER A WITH MACRON
    'A', '(', '\u0102', // LATIN CAPITAL LETTER A WITH BREVE
    'a', '(', '\u0103', // LATIN SMALL LETTER A WITH BREVE
    'A', ';', '\u0104', // LATIN CAPITAL LETTER A WITH OGONEK
    'a', ';', '\u0105', // LATIN SMALL LETTER A WITH OGONEK
    'C', '\'', '\u0106', // LATIN CAPITAL LETTER C WITH ACUTE
    'c', '\'', '\u0107', // LATIN SMALL LETTER C WITH ACUTE
    'C', '>', '\u0108', // LATIN CAPITAL LETTER C WITH CIRCUMFLEX
    'c', '>', '\u0109', // LATIN SMALL LETTER C WITH CIRCUMFLEX
    'C', '.', '\u010a', // LATIN CAPITAL LETTER C WITH DOT ABOVE
    'c', '.', '\u010b', // LATIN SMALL LETTER C WITH DOT ABOVE
    'C', '<', '\u010c', // LATIN CAPITAL LETTER C WITH CARON
    'c', '<', '\u010d', // LATIN SMALL LETTER C WITH CARON
    'D', '<', '\u010e', // LATIN CAPITAL LETTER D WITH CARON
    'd', '<', '\u010f', // LATIN SMALL LETTER D WITH CARON
    'D', '/', '\u0110', // LATIN CAPITAL LETTER D WITH STROKE
    'd', '/', '\u0111', // LATIN SMALL LETTER D WITH STROKE
    'E', '-', '\u0112', // LATIN CAPITAL LETTER E WITH MACRON
    'e', '-', '\u0113', // LATIN SMALL LETTER E WITH MACRON
    'E', '(', '\u0114', // LATIN CAPITAL LETTER E WITH BREVE
    'e', '(', '\u0115', // LATIN SMALL LETTER E WITH BREVE
    'E', '.', '\u0116', // LATIN CAPITAL LETTER E WITH DOT ABOVE
    'e', '.', '\u0117', // LATIN SMALL LETTER E WITH DOT ABOVE
    'E', ';', '\u0118', // LATIN CAPITAL LETTER E WITH OGONEK
    'e', ';', '\u0119', // LATIN SMALL LETTER E WITH OGONEK
    'E', '<', '\u011a', // LATIN CAPITAL LETTER E WITH CARON
    'e', '<', '\u011b', // LATIN SMALL LETTER E WITH CARON
    'G', '>', '\u011c', // LATIN CAPITAL LETTER G WITH CIRCUMFLEX
    'g', '>', '\u011d', // LATIN SMALL LETTER G WITH CIRCUMFLEX
    'G', '(', '\u011e', // LATIN CAPITAL LETTER G WITH BREVE
    'g', '(', '\u011f', // LATIN SMALL LETTER G WITH BREVE
    'G', '.', '\u0120', // LATIN CAPITAL LETTER G WITH DOT ABOVE
    'g', '.', '\u0121', // LATIN SMALL LETTER G WITH DOT ABOVE
    'G', ',', '\u0122', // LATIN CAPITAL LETTER G WITH CEDILLA
    'g', ',', '\u0123', // LATIN SMALL LETTER G WITH CEDILLA
    'H', '>', '\u0124', // LATIN CAPITAL LETTER H WITH CIRCUMFLEX
    'h', '>', '\u0125', // LATIN SMALL LETTER H WITH CIRCUMFLEX
    'H', '/', '\u0126', // LATIN CAPITAL LETTER H WITH STROKE
    'h', '/', '\u0127', // LATIN SMALL LETTER H WITH STROKE
    'I', '?', '\u0128', // LATIN CAPITAL LETTER I WITH TILDE
    'i', '?', '\u0129', // LATIN SMALL LETTER I WITH TILDE
    'I', '-', '\u012a', // LATIN CAPITAL LETTER I WITH MACRON
    'i', '-', '\u012b', // LATIN SMALL LETTER I WITH MACRON
    'I', '(', '\u012c', // LATIN CAPITAL LETTER I WITH BREVE
    'i', '(', '\u012d', // LATIN SMALL LETTER I WITH BREVE
    'I', ';', '\u012e', // LATIN CAPITAL LETTER I WITH OGONEK
    'i', ';', '\u012f', // LATIN SMALL LETTER I WITH OGONEK
    'I', '.', '\u0130', // LATIN CAPITAL LETTER I WITH DOT ABOVE
    'i', '.', '\u0131', // LATIN SMALL LETTER I DOTLESS
    'I', 'J', '\u0132', // LATIN CAPITAL LIGATURE IJ
    'i', 'j', '\u0133', // LATIN SMALL LIGATURE IJ
    'J', '>', '\u0134', // LATIN CAPITAL LETTER J WITH CIRCUMFLEX
    'j', '>', '\u0135', // LATIN SMALL LETTER J WITH CIRCUMFLEX
    'K', ',', '\u0136', // LATIN CAPITAL LETTER K WITH CEDILLA
    'k', ',', '\u0137', // LATIN SMALL LETTER K WITH CEDILLA
    'k', 'k', '\u0138', // LATIN SMALL LETTER KRA (Greenlandic)
    'L', '\'', '\u0139', // LATIN CAPITAL LETTER L WITH ACUTE
    'l', '\'', '\u013a', // LATIN SMALL LETTER L WITH ACUTE
    'L', ',', '\u013b', // LATIN CAPITAL LETTER L WITH CEDILLA
    'l', ',', '\u013c', // LATIN SMALL LETTER L WITH CEDILLA
    'L', '<', '\u013d', // LATIN CAPITAL LETTER L WITH CARON
    'l', '<', '\u013e', // LATIN SMALL LETTER L WITH CARON
    'L', '.', '\u013f', // LATIN CAPITAL LETTER L WITH MIDDLE DOT
    'l', '.', '\u0140', // LATIN SMALL LETTER L WITH MIDDLE DOT
    'L', '/', '\u0141', // LATIN CAPITAL LETTER L WITH STROKE
    'l', '/', '\u0142', // LATIN SMALL LETTER L WITH STROKE
    'N', '\'', '\u0143', // LATIN CAPITAL LETTER N WITH ACUTE
    'n', '\'', '\u0144', // LATIN SMALL LETTER N WITH ACUTE
    'N', ',', '\u0145', // LATIN CAPITAL LETTER N WITH CEDILLA
    'n', ',', '\u0146', // LATIN SMALL LETTER N WITH CEDILLA
    'N', '<', '\u0147', // LATIN CAPITAL LETTER N WITH CARON
    'n', '<', '\u0148', // LATIN SMALL LETTER N WITH CARON
    '\'', 'n', '\u0149', // LATIN SMALL LETTER N PRECEDED BY APOSTROPHE
    'N', 'G', '\u014a', // LATIN CAPITAL LETTER ENG (Lappish)
    'n', 'g', '\u014b', // LATIN SMALL LETTER ENG (Lappish)
    'O', '-', '\u014c', // LATIN CAPITAL LETTER O WITH MACRON
    'o', '-', '\u014d', // LATIN SMALL LETTER O WITH MACRON
    'O', '(', '\u014e', // LATIN CAPITAL LETTER O WITH BREVE
    'o', '(', '\u014f', // LATIN SMALL LETTER O WITH BREVE
    'O', '"', '\u0150', // LATIN CAPITAL LETTER O WITH DOUBLE ACUTE
    'o', '"', '\u0151', // LATIN SMALL LETTER O WITH DOUBLE ACUTE
    'O', 'E', '\u0152', // LATIN CAPITAL LIGATURE OE
    'o', 'e', '\u0153', // LATIN SMALL LIGATURE OE
    'R', '\'', '\u0154', // LATIN CAPITAL LETTER R WITH ACUTE
    'r', '\'', '\u0155', // LATIN SMALL LETTER R WITH ACUTE
    'R', ',', '\u0156', // LATIN CAPITAL LETTER R WITH CEDILLA
    'r', ',', '\u0157', // LATIN SMALL LETTER R WITH CEDILLA
    'R', '<', '\u0158', // LATIN CAPITAL LETTER R WITH CARON
    'r', '<', '\u0159', // LATIN SMALL LETTER R WITH CARON
    'S', '\'', '\u015a', // LATIN CAPITAL LETTER S WITH ACUTE
    's', '\'', '\u015b', // LATIN SMALL LETTER S WITH ACUTE
    'S', '>', '\u015c', // LATIN CAPITAL LETTER S WITH CIRCUMFLEX
    's', '>', '\u015d', // LATIN SMALL LETTER S WITH CIRCUMFLEX
    'S', ',', '\u015e', // LATIN CAPITAL LETTER S WITH CEDILLA
    's', ',', '\u015f', // LATIN SMALL LETTER S WITH CEDILLA
    'S', '<', '\u0160', // LATIN CAPITAL LETTER S WITH CARON
    's', '<', '\u0161', // LATIN SMALL LETTER S WITH CARON
    'T', ',', '\u0162', // LATIN CAPITAL LETTER T WITH CEDILLA
    't', ',', '\u0163', // LATIN SMALL LETTER T WITH CEDILLA
    'T', '<', '\u0164', // LATIN CAPITAL LETTER T WITH CARON
    't', '<', '\u0165', // LATIN SMALL LETTER T WITH CARON
    'T', '/', '\u0166', // LATIN CAPITAL LETTER T WITH STROKE
    't', '/', '\u0167', // LATIN SMALL LETTER T WITH STROKE
    'U', '?', '\u0168', // LATIN CAPITAL LETTER U WITH TILDE
    'u', '?', '\u0169', // LATIN SMALL LETTER U WITH TILDE
    'U', '-', '\u016a', // LATIN CAPITAL LETTER U WITH MACRON
    'u', '-', '\u016b', // LATIN SMALL LETTER U WITH MACRON
    'U', '(', '\u016c', // LATIN CAPITAL LETTER U WITH BREVE
    'u', '(', '\u016d', // LATIN SMALL LETTER U WITH BREVE
    'U', '0', '\u016e', // LATIN CAPITAL LETTER U WITH RING ABOVE
    'u', '0', '\u016f', // LATIN SMALL LETTER U WITH RING ABOVE
    'U', '"', '\u0170', // LATIN CAPITAL LETTER U WITH DOUBLE ACUTE
    'u', '"', '\u0171', // LATIN SMALL LETTER U WITH DOUBLE ACUTE
    'U', ';', '\u0172', // LATIN CAPITAL LETTER U WITH OGONEK
    'u', ';', '\u0173', // LATIN SMALL LETTER U WITH OGONEK
    'W', '>', '\u0174', // LATIN CAPITAL LETTER W WITH CIRCUMFLEX
    'w', '>', '\u0175', // LATIN SMALL LETTER W WITH CIRCUMFLEX
    'Y', '>', '\u0176', // LATIN CAPITAL LETTER Y WITH CIRCUMFLEX
    'y', '>', '\u0177', // LATIN SMALL LETTER Y WITH CIRCUMFLEX
    'Y', ':', '\u0178', // LATIN CAPITAL LETTER Y WITH DIAERESIS
    'Z', '\'', '\u0179', // LATIN CAPITAL LETTER Z WITH ACUTE
    'z', '\'', '\u017a', // LATIN SMALL LETTER Z WITH ACUTE
    'Z', '.', '\u017b', // LATIN CAPITAL LETTER Z WITH DOT ABOVE
    'z', '.', '\u017c', // LATIN SMALL LETTER Z WITH DOT ABOVE
    'Z', '<', '\u017d', // LATIN CAPITAL LETTER Z WITH CARON
    'z', '<', '\u017e', // LATIN SMALL LETTER Z WITH CARON
    'O', '9', '\u01a0', // LATIN CAPITAL LETTER O WITH HORN
    'o', '9', '\u01a1', // LATIN SMALL LETTER O WITH HORN
    'O', 'I', '\u01a2', // LATIN CAPITAL LETTER OI
    'o', 'i', '\u01a3', // LATIN SMALL LETTER OI
    'y', 'r', '\u01a6', // LATIN LETTER YR
    'U', '9', '\u01af', // LATIN CAPITAL LETTER U WITH HORN
    'u', '9', '\u01b0', // LATIN SMALL LETTER U WITH HORN
    'Z', '/', '\u01b5', // LATIN CAPITAL LETTER Z WITH STROKE
    'z', '/', '\u01b6', // LATIN SMALL LETTER Z WITH STROKE
    'E', 'D', '\u01b7', // LATIN CAPITAL LETTER EZH
    'A', '<', '\u01cd', // LATIN CAPITAL LETTER A WITH CARON
    'a', '<', '\u01ce', // LATIN SMALL LETTER A WITH CARON
    'I', '<', '\u01cf', // LATIN CAPITAL LETTER I WITH CARON
    'i', '<', '\u01d0', // LATIN SMALL LETTER I WITH CARON
    'O', '<', '\u01d1', // LATIN CAPITAL LETTER O WITH CARON
    'o', '<', '\u01d2', // LATIN SMALL LETTER O WITH CARON
    'U', '<', '\u01d3', // LATIN CAPITAL LETTER U WITH CARON
    'u', '<', '\u01d4', // LATIN SMALL LETTER U WITH CARON
    'A', '1', '\u01de', // LATIN CAPITAL LETTER A WITH DIAERESIS AND MACRON
    'a', '1', '\u01df', // LATIN SMALL LETTER A WITH DIAERESIS AND MACRON
    'A', '7', '\u01e0', // LATIN CAPITAL LETTER A WITH DOT ABOVE AND MACRON
    'a', '7', '\u01e1', // LATIN SMALL LETTER A WITH DOT ABOVE AND MACRON
    'A', '3', '\u01e2', // LATIN CAPITAL LETTER AE WITH MACRON
    'a', '3', '\u01e3', // LATIN SMALL LETTER AE WITH MACRON
    'G', '/', '\u01e4', // LATIN CAPITAL LETTER G WITH STROKE
    'g', '/', '\u01e5', // LATIN SMALL LETTER G WITH STROKE
    'G', '<', '\u01e6', // LATIN CAPITAL LETTER G WITH CARON
    'g', '<', '\u01e7', // LATIN SMALL LETTER G WITH CARON
    'K', '<', '\u01e8', // LATIN CAPITAL LETTER K WITH CARON
    'k', '<', '\u01e9', // LATIN SMALL LETTER K WITH CARON
    'O', ';', '\u01ea', // LATIN CAPITAL LETTER O WITH OGONEK
    'o', ';', '\u01eb', // LATIN SMALL LETTER O WITH OGONEK
    'O', '1', '\u01ec', // LATIN CAPITAL LETTER O WITH OGONEK AND MACRON
    'o', '1', '\u01ed', // LATIN SMALL LETTER O WITH OGONEK AND MACRON
    'E', 'Z', '\u01ee', // LATIN CAPITAL LETTER EZH WITH CARON
    'e', 'z', '\u01ef', // LATIN SMALL LETTER EZH WITH CARON
    'j', '<', '\u01f0', // LATIN SMALL LETTER J WITH CARON
    'G', '\'', '\u01f4', // LATIN CAPITAL LETTER G WITH ACUTE
    'g', '\'', '\u01f5', // LATIN SMALL LETTER G WITH ACUTE
    ';', 'S', '\u02bf', // MODIFIER LETTER LEFT HALF RING
    '\'', '<', '\u02c7', // CARON
    '\'', '(', '\u02d8', // BREVE
    '\'', '.', '\u02d9', // DOT ABOVE
    '\'', '0', '\u02da', // RING ABOVE
    '\'', ';', '\u02db', // OGONEK
    '\'', '"', '\u02dd', // DOUBLE ACUTE ACCENT
    'A', '%', '\u0386', // GREEK CAPITAL LETTER ALPHA WITH ACUTE
    'E', '%', '\u0388', // GREEK CAPITAL LETTER EPSILON WITH ACUTE
    'Y', '%', '\u0389', // GREEK CAPITAL LETTER ETA WITH ACUTE
    'I', '%', '\u038a', // GREEK CAPITAL LETTER IOTA WITH ACUTE
    'O', '%', '\u038c', // GREEK CAPITAL LETTER OMICRON WITH ACUTE
    'U', '%', '\u038e', // GREEK CAPITAL LETTER UPSILON WITH ACUTE
    'W', '%', '\u038f', // GREEK CAPITAL LETTER OMEGA WITH ACUTE
    'i', '3', '\u0390', // GREEK SMALL LETTER IOTA WITH ACUTE AND DIAERESIS
    'A', '*', '\u0391', // GREEK CAPITAL LETTER ALPHA
    'B', '*', '\u0392', // GREEK CAPITAL LETTER BETA
    'G', '*', '\u0393', // GREEK CAPITAL LETTER GAMMA
    'D', '*', '\u0394', // GREEK CAPITAL LETTER DELTA
    'E', '*', '\u0395', // GREEK CAPITAL LETTER EPSILON
    'Z', '*', '\u0396', // GREEK CAPITAL LETTER ZETA
    'Y', '*', '\u0397', // GREEK CAPITAL LETTER ETA
    'H', '*', '\u0398', // GREEK CAPITAL LETTER THETA
    'I', '*', '\u0399', // GREEK CAPITAL LETTER IOTA
    'K', '*', '\u039a', // GREEK CAPITAL LETTER KAPPA
    'L', '*', '\u039b', // GREEK CAPITAL LETTER LAMDA
    'M', '*', '\u039c', // GREEK CAPITAL LETTER MU
    'N', '*', '\u039d', // GREEK CAPITAL LETTER NU
    'C', '*', '\u039e', // GREEK CAPITAL LETTER XI
    'O', '*', '\u039f', // GREEK CAPITAL LETTER OMICRON
    'P', '*', '\u03a0', // GREEK CAPITAL LETTER PI
    'R', '*', '\u03a1', // GREEK CAPITAL LETTER RHO
    'S', '*', '\u03a3', // GREEK CAPITAL LETTER SIGMA
    'T', '*', '\u03a4', // GREEK CAPITAL LETTER TAU
    'U', '*', '\u03a5', // GREEK CAPITAL LETTER UPSILON
    'F', '*', '\u03a6', // GREEK CAPITAL LETTER PHI
    'X', '*', '\u03a7', // GREEK CAPITAL LETTER CHI
    'Q', '*', '\u03a8', // GREEK CAPITAL LETTER PSI
    'W', '*', '\u03a9', // GREEK CAPITAL LETTER OMEGA
    'J', '*', '\u03aa', // GREEK CAPITAL LETTER IOTA WITH DIAERESIS
    'V', '*', '\u03ab', // GREEK CAPITAL LETTER UPSILON WITH DIAERESIS
    'a', '%', '\u03ac', // GREEK SMALL LETTER ALPHA WITH ACUTE
    'e', '%', '\u03ad', // GREEK SMALL LETTER EPSILON WITH ACUTE
    'y', '%', '\u03ae', // GREEK SMALL LETTER ETA WITH ACUTE
    'i', '%', '\u03af', // GREEK SMALL LETTER IOTA WITH ACUTE
    'u', '3', '\u03b0', // GREEK SMALL LETTER UPSILON WITH ACUTE AND DIAERESIS
    'a', '*', '\u03b1', // GREEK SMALL LETTER ALPHA
    'b', '*', '\u03b2', // GREEK SMALL LETTER BETA
    'g', '*', '\u03b3', // GREEK SMALL LETTER GAMMA
    'd', '*', '\u03b4', // GREEK SMALL LETTER DELTA
    'e', '*', '\u03b5', // GREEK SMALL LETTER EPSILON
    'z', '*', '\u03b6', // GREEK SMALL LETTER ZETA
    'y', '*', '\u03b7', // GREEK SMALL LETTER ETA
    'h', '*', '\u03b8', // GREEK SMALL LETTER THETA
    'i', '*', '\u03b9', // GREEK SMALL LETTER IOTA
    'k', '*', '\u03ba', // GREEK SMALL LETTER KAPPA
    'l', '*', '\u03bb', // GREEK SMALL LETTER LAMDA
    'm', '*', '\u03bc', // GREEK SMALL LETTER MU
    'n', '*', '\u03bd', // GREEK SMALL LETTER NU
    'c', '*', '\u03be', // GREEK SMALL LETTER XI
    'o', '*', '\u03bf', // GREEK SMALL LETTER OMICRON
    'p', '*', '\u03c0', // GREEK SMALL LETTER PI
    'r', '*', '\u03c1', // GREEK SMALL LETTER RHO
    '*', 's', '\u03c2', // GREEK SMALL LETTER FINAL SIGMA
    's', '*', '\u03c3', // GREEK SMALL LETTER SIGMA
    't', '*', '\u03c4', // GREEK SMALL LETTER TAU
    'u', '*', '\u03c5', // GREEK SMALL LETTER UPSILON
    'f', '*', '\u03c6', // GREEK SMALL LETTER PHI
    'x', '*', '\u03c7', // GREEK SMALL LETTER CHI
    'q', '*', '\u03c8', // GREEK SMALL LETTER PSI
    'w', '*', '\u03c9', // GREEK SMALL LETTER OMEGA
    'j', '*', '\u03ca', // GREEK SMALL LETTER IOTA WITH DIAERESIS
    'v', '*', '\u03cb', // GREEK SMALL LETTER UPSILON WITH DIAERESIS
    'o', '%', '\u03cc', // GREEK SMALL LETTER OMICRON WITH ACUTE
    'u', '%', '\u03cd', // GREEK SMALL LETTER UPSILON WITH ACUTE
    'w', '%', '\u03ce', // GREEK SMALL LETTER OMEGA WITH ACUTE
    '\'', 'G', '\u03d8', // GREEK NUMERAL SIGN
    ',', 'G', '\u03d9', // GREEK LOWER NUMERAL SIGN
    'T', '3', '\u03da', // GREEK CAPITAL LETTER STIGMA
    't', '3', '\u03db', // GREEK SMALL LETTER STIGMA
    'M', '3', '\u03dc', // GREEK CAPITAL LETTER DIGAMMA
    'm', '3', '\u03dd', // GREEK SMALL LETTER DIGAMMA
    'K', '3', '\u03de', // GREEK CAPITAL LETTER KOPPA
    'k', '3', '\u03df', // GREEK SMALL LETTER KOPPA
    'P', '3', '\u03e0', // GREEK CAPITAL LETTER SAMPI
    'p', '3', '\u03e1', // GREEK SMALL LETTER SAMPI
    '\'', '%', '\u03f4', // ACUTE ACCENT AND DIAERESIS (Tonos and Dialytika)
    'j', '3', '\u03f5', // GREEK IOTA BELOW
    'I', 'O', '\u0401', // CYRILLIC CAPITAL LETTER IO
    'D', '%', '\u0402', // CYRILLIC CAPITAL LETTER DJE (Serbocroatian)
    'G', '%', '\u0403', // CYRILLIC CAPITAL LETTER GJE (Macedonian)
    'I', 'E', '\u0404', // CYRILLIC CAPITAL LETTER UKRAINIAN IE
    'D', 'S', '\u0405', // CYRILLIC CAPITAL LETTER DZE (Macedonian)
    'I', 'I', '\u0406', // CYRILLIC CAPITAL LETTER BYELORUSSIAN-UKRAINIAN I
    'Y', 'I', '\u0407', // CYRILLIC CAPITAL LETTER YI (Ukrainian)
    'J', '%', '\u0408', // CYRILLIC CAPITAL LETTER JE
    'L', 'J', '\u0409', // CYRILLIC CAPITAL LETTER LJE
    'N', 'J', '\u040a', // CYRILLIC CAPITAL LETTER NJE
    'T', 's', '\u040b', // CYRILLIC CAPITAL LETTER TSHE (Serbocroatian)
    'K', 'J', '\u040c', // CYRILLIC CAPITAL LETTER KJE (Macedonian)
    'V', '%', '\u040e', // CYRILLIC CAPITAL LETTER SHORT U (Byelorussian)
    'D', 'Z', '\u040f', // CYRILLIC CAPITAL LETTER DZHE
    'A', '=', '\u0410', // CYRILLIC CAPITAL LETTER A
    'B', '=', '\u0411', // CYRILLIC CAPITAL LETTER BE
    'V', '=', '\u0412', // CYRILLIC CAPITAL LETTER VE
    'G', '=', '\u0413', // CYRILLIC CAPITAL LETTER GHE
    'D', '=', '\u0414', // CYRILLIC CAPITAL LETTER DE
    'E', '=', '\u0415', // CYRILLIC CAPITAL LETTER IE
    'Z', '%', '\u0416', // CYRILLIC CAPITAL LETTER ZHE
    'Z', '=', '\u0417', // CYRILLIC CAPITAL LETTER ZE
    'I', '=', '\u0418', // CYRILLIC CAPITAL LETTER I
    'J', '=', '\u0419', // CYRILLIC CAPITAL LETTER SHORT I
    'K', '=', '\u041a', // CYRILLIC CAPITAL LETTER KA
    'L', '=', '\u041b', // CYRILLIC CAPITAL LETTER EL
    'M', '=', '\u041c', // CYRILLIC CAPITAL LETTER EM
    'N', '=', '\u041d', // CYRILLIC CAPITAL LETTER EN
    'O', '=', '\u041e', // CYRILLIC CAPITAL LETTER O
    'P', '=', '\u041f', // CYRILLIC CAPITAL LETTER PE
    'R', '=', '\u0420', // CYRILLIC CAPITAL LETTER ER
    'S', '=', '\u0421', // CYRILLIC CAPITAL LETTER ES
    'T', '=', '\u0422', // CYRILLIC CAPITAL LETTER TE
    'U', '=', '\u0423', // CYRILLIC CAPITAL LETTER U
    'F', '=', '\u0424', // CYRILLIC CAPITAL LETTER EF
    'H', '=', '\u0425', // CYRILLIC CAPITAL LETTER HA
    'C', '=', '\u0426', // CYRILLIC CAPITAL LETTER TSE
    'C', '%', '\u0427', // CYRILLIC CAPITAL LETTER CHE
    'S', '%', '\u0428', // CYRILLIC CAPITAL LETTER SHA
    'S', 'c', '\u0429', // CYRILLIC CAPITAL LETTER SHCHA
    '=', '"', '\u042a', // CYRILLIC CAPITAL LETTER HARD SIGN
    'Y', '=', '\u042b', // CYRILLIC CAPITAL LETTER YERU
    '%', '"', '\u042c', // CYRILLIC CAPITAL LETTER SOFT SIGN
    'J', 'E', '\u042d', // CYRILLIC CAPITAL LETTER E
    'J', 'U', '\u042e', // CYRILLIC CAPITAL LETTER YU
    'J', 'A', '\u042f', // CYRILLIC CAPITAL LETTER YA
    'a', '=', '\u0430', // CYRILLIC SMALL LETTER A
    'b', '=', '\u0431', // CYRILLIC SMALL LETTER BE
    'v', '=', '\u0432', // CYRILLIC SMALL LETTER VE
    'g', '=', '\u0433', // CYRILLIC SMALL LETTER GHE
    'd', '=', '\u0434', // CYRILLIC SMALL LETTER DE
    'e', '=', '\u0435', // CYRILLIC SMALL LETTER IE
    'z', '%', '\u0436', // CYRILLIC SMALL LETTER ZHE
    'z', '=', '\u0437', // CYRILLIC SMALL LETTER ZE
    'i', '=', '\u0438', // CYRILLIC SMALL LETTER I
    'j', '=', '\u0439', // CYRILLIC SMALL LETTER SHORT I
    'k', '=', '\u043a', // CYRILLIC SMALL LETTER KA
    'l', '=', '\u043b', // CYRILLIC SMALL LETTER EL
    'm', '=', '\u043c', // CYRILLIC SMALL LETTER EM
    'n', '=', '\u043d', // CYRILLIC SMALL LETTER EN
    'o', '=', '\u043e', // CYRILLIC SMALL LETTER O
    'p', '=', '\u043f', // CYRILLIC SMALL LETTER PE
    'r', '=', '\u0440', // CYRILLIC SMALL LETTER ER
    's', '=', '\u0441', // CYRILLIC SMALL LETTER ES
    't', '=', '\u0442', // CYRILLIC SMALL LETTER TE
    'u', '=', '\u0443', // CYRILLIC SMALL LETTER U
    'f', '=', '\u0444', // CYRILLIC SMALL LETTER EF
    'h', '=', '\u0445', // CYRILLIC SMALL LETTER HA
    'c', '=', '\u0446', // CYRILLIC SMALL LETTER TSE
    'c', '%', '\u0447', // CYRILLIC SMALL LETTER CHE
    's', '%', '\u0448', // CYRILLIC SMALL LETTER SHA
    's', 'c', '\u0449', // CYRILLIC SMALL LETTER SHCHA
    '=', '\'', '\u044a', // CYRILLIC SMALL LETTER HARD SIGN
    'y', '=', '\u044b', // CYRILLIC SMALL LETTER YERU
    '%', '\'', '\u044c', // CYRILLIC SMALL LETTER SOFT SIGN
    'j', 'e', '\u044d', // CYRILLIC SMALL LETTER E
    'j', 'u', '\u044e', // CYRILLIC SMALL LETTER YU
    'j', 'a', '\u044f', // CYRILLIC SMALL LETTER YA
    'i', 'o', '\u0451', // CYRILLIC SMALL LETTER IO
    'd', '%', '\u0452', // CYRILLIC SMALL LETTER DJE (Serbocroatian)
    'g', '%', '\u0453', // CYRILLIC SMALL LETTER GJE (Macedonian)
    'i', 'e', '\u0454', // CYRILLIC SMALL LETTER UKRAINIAN IE
    'd', 's', '\u0455', // CYRILLIC SMALL LETTER DZE (Macedonian)
    'i', 'i', '\u0456', // CYRILLIC SMALL LETTER BYELORUSSIAN-UKRAINIAN I
    'y', 'i', '\u0457', // CYRILLIC SMALL LETTER YI (Ukrainian)
    'j', '%', '\u0458', // CYRILLIC SMALL LETTER JE
    'l', 'j', '\u0459', // CYRILLIC SMALL LETTER LJE
    'n', 'j', '\u045a', // CYRILLIC SMALL LETTER NJE
    't', 's', '\u045b', // CYRILLIC SMALL LETTER TSHE (Serbocroatian)
    'k', 'j', '\u045c', // CYRILLIC SMALL LETTER KJE (Macedonian)
    'v', '%', '\u045e', // CYRILLIC SMALL LETTER SHORT U (Byelorussian)
    'd', 'z', '\u045f', // CYRILLIC SMALL LETTER DZHE
    'Y', '3', '\u0462', // CYRILLIC CAPITAL LETTER YAT
    'y', '3', '\u0463', // CYRILLIC SMALL LETTER YAT
    'O', '3', '\u046a', // CYRILLIC CAPITAL LETTER BIG YUS
    'o', '3', '\u046b', // CYRILLIC SMALL LETTER BIG YUS
    'F', '3', '\u0472', // CYRILLIC CAPITAL LETTER FITA
    'f', '3', '\u0473', // CYRILLIC SMALL LETTER FITA
    'V', '3', '\u0474', // CYRILLIC CAPITAL LETTER IZHITSA
    'v', '3', '\u0475', // CYRILLIC SMALL LETTER IZHITSA
    'C', '3', '\u0480', // CYRILLIC CAPITAL LETTER KOPPA
    'c', '3', '\u0481', // CYRILLIC SMALL LETTER KOPPA
    'G', '3', '\u0490', // CYRILLIC CAPITAL LETTER GHE WITH UPTURN
    'g', '3', '\u0491', // CYRILLIC SMALL LETTER GHE WITH UPTURN
    'A', '+', '\u05d0', // HEBREW LETTER ALEF
    'B', '+', '\u05d1', // HEBREW LETTER BET
    'G', '+', '\u05d2', // HEBREW LETTER GIMEL
    'D', '+', '\u05d3', // HEBREW LETTER DALET
    'H', '+', '\u05d4', // HEBREW LETTER HE
    'W', '+', '\u05d5', // HEBREW LETTER VAV
    'Z', '+', '\u05d6', // HEBREW LETTER ZAYIN
    'X', '+', '\u05d7', // HEBREW LETTER HET
    'T', 'j', '\u05d8', // HEBREW LETTER TET
    'J', '+', '\u05d9', // HEBREW LETTER YOD
    'K', '%', '\u05da', // HEBREW LETTER FINAL KAF
    'K', '+', '\u05db', // HEBREW LETTER KAF
    'L', '+', '\u05dc', // HEBREW LETTER LAMED
    'M', '%', '\u05dd', // HEBREW LETTER FINAL MEM
    'M', '+', '\u05de', // HEBREW LETTER MEM
    'N', '%', '\u05df', // HEBREW LETTER FINAL NUN
    'N', '+', '\u05e0', // HEBREW LETTER NUN
    'S', '+', '\u05e1', // HEBREW LETTER SAMEKH
    'E', '+', '\u05e2', // HEBREW LETTER AYIN
    'P', '%', '\u05e3', // HEBREW LETTER FINAL PE
    'P', '+', '\u05e4', // HEBREW LETTER PE
    'Z', 'j', '\u05e5', // HEBREW LETTER FINAL TSADI
    'Z', 'J', '\u05e6', // HEBREW LETTER TSADI
    'Q', '+', '\u05e7', // HEBREW LETTER QOF
    'R', '+', '\u05e8', // HEBREW LETTER RESH
    'S', 'h', '\u05e9', // HEBREW LETTER SHIN
    'T', '+', '\u05ea', // HEBREW LETTER TAV
    ',', '+', '\u060c', // ARABIC COMMA
    ';', '+', '\u061b', // ARABIC SEMICOLON
    '?', '+', '\u061f', // ARABIC QUESTION MARK
    'H', '\'', '\u0621', // ARABIC LETTER HAMZA
    'a', 'M', '\u0622', // ARABIC LETTER ALEF WITH MADDA ABOVE
    'a', 'H', '\u0623', // ARABIC LETTER ALEF WITH HAMZA ABOVE
    'w', 'H', '\u0624', // ARABIC LETTER WAW WITH HAMZA ABOVE
    'a', 'h', '\u0625', // ARABIC LETTER ALEF WITH HAMZA BELOW
    'y', 'H', '\u0626', // ARABIC LETTER YEH WITH HAMZA ABOVE
    'a', '+', '\u0627', // ARABIC LETTER ALEF
    'b', '+', '\u0628', // ARABIC LETTER BEH
    't', 'm', '\u0629', // ARABIC LETTER TEH MARBUTA
    't', '+', '\u062a', // ARABIC LETTER TEH
    't', 'k', '\u062b', // ARABIC LETTER THEH
    'g', '+', '\u062c', // ARABIC LETTER JEEM
    'h', 'k', '\u062d', // ARABIC LETTER HAH
    'x', '+', '\u062e', // ARABIC LETTER KHAH
    'd', '+', '\u062f', // ARABIC LETTER DAL
    'd', 'k', '\u0630', // ARABIC LETTER THAL
    'r', '+', '\u0631', // ARABIC LETTER REH
    'z', '+', '\u0632', // ARABIC LETTER ZAIN
    's', '+', '\u0633', // ARABIC LETTER SEEN
    's', 'n', '\u0634', // ARABIC LETTER SHEEN
    'c', '+', '\u0635', // ARABIC LETTER SAD
    'd', 'd', '\u0636', // ARABIC LETTER DAD
    't', 'j', '\u0637', // ARABIC LETTER TAH
    'z', 'H', '\u0638', // ARABIC LETTER ZAH
    'e', '+', '\u0639', // ARABIC LETTER AIN
    'i', '+', '\u063a', // ARABIC LETTER GHAIN
    '+', '+', '\u0640', // ARABIC TATWEEL
    'f', '+', '\u0641', // ARABIC LETTER FEH
    'q', '+', '\u0642', // ARABIC LETTER QAF
    'k', '+', '\u0643', // ARABIC LETTER KAF
    'l', '+', '\u0644', // ARABIC LETTER LAM
    'm', '+', '\u0645', // ARABIC LETTER MEEM
    'n', '+', '\u0646', // ARABIC LETTER NOON
    'h', '+', '\u0647', // ARABIC LETTER HEH
    'w', '+', '\u0648', // ARABIC LETTER WAW
    'j', '+', '\u0649', // ARABIC LETTER ALEF MAKSURA
    'y', '+', '\u064a', // ARABIC LETTER YEH
    ':', '+', '\u064b', // ARABIC FATHATAN
    '"', '+', '\u064c', // ARABIC DAMMATAN
    '=', '+', '\u064d', // ARABIC KASRATAN
    '/', '+', '\u064e', // ARABIC FATHA
    '\'', '+', '\u064f', // ARABIC DAMMA
    '1', '+', '\u0650', // ARABIC KASRA
    '3', '+', '\u0651', // ARABIC SHADDA
    '0', '+', '\u0652', // ARABIC SUKUN
    'a', 'S', '\u0670', // SUPERSCRIPT ARABIC LETTER ALEF
    'p', '+', '\u067e', // ARABIC LETTER PEH
    'v', '+', '\u06a4', // ARABIC LETTER VEH
    'g', 'f', '\u06af', // ARABIC LETTER GAF
    '0', 'a', '\u06f0', // EASTERN ARABIC-INDIC DIGIT ZERO
    '1', 'a', '\u06f1', // EASTERN ARABIC-INDIC DIGIT ONE
    '2', 'a', '\u06f2', // EASTERN ARABIC-INDIC DIGIT TWO
    '3', 'a', '\u06f3', // EASTERN ARABIC-INDIC DIGIT THREE
    '4', 'a', '\u06f4', // EASTERN ARABIC-INDIC DIGIT FOUR
    '5', 'a', '\u06f5', // EASTERN ARABIC-INDIC DIGIT FIVE
    '6', 'a', '\u06f6', // EASTERN ARABIC-INDIC DIGIT SIX
    '7', 'a', '\u06f7', // EASTERN ARABIC-INDIC DIGIT SEVEN
    '8', 'a', '\u06f8', // EASTERN ARABIC-INDIC DIGIT EIGHT
    '9', 'a', '\u06f9', // EASTERN ARABIC-INDIC DIGIT NINE
    'B', '.', '\u1e02', // LATIN CAPITAL LETTER B WITH DOT ABOVE
    'b', '.', '\u1e03', // LATIN SMALL LETTER B WITH DOT ABOVE
    'B', '_', '\u1e06', // LATIN CAPITAL LETTER B WITH LINE BELOW
    'b', '_', '\u1e07', // LATIN SMALL LETTER B WITH LINE BELOW
    'D', '.', '\u1e0a', // LATIN CAPITAL LETTER D WITH DOT ABOVE
    'd', '.', '\u1e0b', // LATIN SMALL LETTER D WITH DOT ABOVE
    'D', '_', '\u1e0e', // LATIN CAPITAL LETTER D WITH LINE BELOW
    'd', '_', '\u1e0f', // LATIN SMALL LETTER D WITH LINE BELOW
    'D', ',', '\u1e10', // LATIN CAPITAL LETTER D WITH CEDILLA
    'd', ',', '\u1e11', // LATIN SMALL LETTER D WITH CEDILLA
    'F', '.', '\u1e1e', // LATIN CAPITAL LETTER F WITH DOT ABOVE
    'f', '.', '\u1e1f', // LATIN SMALL LETTER F WITH DOT ABOVE
    'G', '-', '\u1e20', // LATIN CAPITAL LETTER G WITH MACRON
    'g', '-', '\u1e21', // LATIN SMALL LETTER G WITH MACRON
    'H', '.', '\u1e22', // LATIN CAPITAL LETTER H WITH DOT ABOVE
    'h', '.', '\u1e23', // LATIN SMALL LETTER H WITH DOT ABOVE
    'H', ':', '\u1e26', // LATIN CAPITAL LETTER H WITH DIAERESIS
    'h', ':', '\u1e27', // LATIN SMALL LETTER H WITH DIAERESIS
    'H', ',', '\u1e28', // LATIN CAPITAL LETTER H WITH CEDILLA
    'h', ',', '\u1e29', // LATIN SMALL LETTER H WITH CEDILLA
    'K', '\'', '\u1e30', // LATIN CAPITAL LETTER K WITH ACUTE
    'k', '\'', '\u1e31', // LATIN SMALL LETTER K WITH ACUTE
    'K', '_', '\u1e34', // LATIN CAPITAL LETTER K WITH LINE BELOW
    'k', '_', '\u1e35', // LATIN SMALL LETTER K WITH LINE BELOW
    'L', '_', '\u1e3a', // LATIN CAPITAL LETTER L WITH LINE BELOW
    'l', '_', '\u1e3b', // LATIN SMALL LETTER L WITH LINE BELOW
    'M', '\'', '\u1e3e', // LATIN CAPITAL LETTER M WITH ACUTE
    'm', '\'', '\u1e3f', // LATIN SMALL LETTER M WITH ACUTE
    'M', '.', '\u1e40', // LATIN CAPITAL LETTER M WITH DOT ABOVE
    'm', '.', '\u1e41', // LATIN SMALL LETTER M WITH DOT ABOVE
    'N', '.', '\u1e44', // LATIN CAPITAL LETTER N WITH DOT ABOVE
    'n', '.', '\u1e45', // LATIN SMALL LETTER N WITH DOT ABOVE
    'N', '_', '\u1e48', // LATIN CAPITAL LETTER N WITH LINE BELOW
    'n', '_', '\u1e49', // LATIN SMALL LETTER N WITH LINE BELOW
    'P', '\'', '\u1e54', // LATIN CAPITAL LETTER P WITH ACUTE
    'p', '\'', '\u1e55', // LATIN SMALL LETTER P WITH ACUTE
    'P', '.', '\u1e56', // LATIN CAPITAL LETTER P WITH DOT ABOVE
    'p', '.', '\u1e57', // LATIN SMALL LETTER P WITH DOT ABOVE
    'R', '.', '\u1e58', // LATIN CAPITAL LETTER R WITH DOT ABOVE
    'r', '.', '\u1e59', // LATIN SMALL LETTER R WITH DOT ABOVE
    'R', '_', '\u1e5e', // LATIN CAPITAL LETTER R WITH LINE BELOW
    'r', '_', '\u1e5f', // LATIN SMALL LETTER R WITH LINE BELOW
    'S', '.', '\u1e60', // LATIN CAPITAL LETTER S WITH DOT ABOVE
    's', '.', '\u1e61', // LATIN SMALL LETTER S WITH DOT ABOVE
    'T', '.', '\u1e6a', // LATIN CAPITAL LETTER T WITH DOT ABOVE
    't', '.', '\u1e6b', // LATIN SMALL LETTER T WITH DOT ABOVE
    'T', '_', '\u1e6e', // LATIN CAPITAL LETTER T WITH LINE BELOW
    't', '_', '\u1e6f', // LATIN SMALL LETTER T WITH LINE BELOW
    'V', '?', '\u1e7c', // LATIN CAPITAL LETTER V WITH TILDE
    'v', '?', '\u1e7d', // LATIN SMALL LETTER V WITH TILDE
    'W', '!', '\u1e80', // LATIN CAPITAL LETTER W WITH GRAVE
    'w', '!', '\u1e81', // LATIN SMALL LETTER W WITH GRAVE
    'W', '\'', '\u1e82', // LATIN CAPITAL LETTER W WITH ACUTE
    'w', '\'', '\u1e83', // LATIN SMALL LETTER W WITH ACUTE
    'W', ':', '\u1e84', // LATIN CAPITAL LETTER W WITH DIAERESIS
    'w', ':', '\u1e85', // LATIN SMALL LETTER W WITH DIAERESIS
    'W', '.', '\u1e86', // LATIN CAPITAL LETTER W WITH DOT ABOVE
    'w', '.', '\u1e87', // LATIN SMALL LETTER W WITH DOT ABOVE
    'X', '.', '\u1e8a', // LATIN CAPITAL LETTER X WITH DOT ABOVE
    'x', '.', '\u1e8b', // LATIN SMALL LETTER X WITH DOT ABOVE
    'X', ':', '\u1e8c', // LATIN CAPITAL LETTER X WITH DIAERESIS
    'x', ':', '\u1e8d', // LATIN SMALL LETTER X WITH DIAERESIS
    'Y', '.', '\u1e8e', // LATIN CAPITAL LETTER Y WITH DOT ABOVE
    'y', '.', '\u1e8f', // LATIN SMALL LETTER Y WITH DOT ABOVE
    'Z', '>', '\u1e90', // LATIN CAPITAL LETTER Z WITH CIRCUMFLEX
    'z', '>', '\u1e91', // LATIN SMALL LETTER Z WITH CIRCUMFLEX
    'Z', '_', '\u1e94', // LATIN CAPITAL LETTER Z WITH LINE BELOW
    'z', '_', '\u1e95', // LATIN SMALL LETTER Z WITH LINE BELOW
    'h', '_', '\u1e96', // LATIN SMALL LETTER H WITH LINE BELOW
    't', ':', '\u1e97', // LATIN SMALL LETTER T WITH DIAERESIS
    'w', '0', '\u1e98', // LATIN SMALL LETTER W WITH RING ABOVE
    'y', '0', '\u1e99', // LATIN SMALL LETTER Y WITH RING ABOVE
    'A', '2', '\u1ea2', // LATIN CAPITAL LETTER A WITH HOOK ABOVE
    'a', '2', '\u1ea3', // LATIN SMALL LETTER A WITH HOOK ABOVE
    'E', '2', '\u1eba', // LATIN CAPITAL LETTER E WITH HOOK ABOVE
    'e', '2', '\u1ebb', // LATIN SMALL LETTER E WITH HOOK ABOVE
    'E', '?', '\u1ebc', // LATIN CAPITAL LETTER E WITH TILDE
    'e', '?', '\u1ebd', // LATIN SMALL LETTER E WITH TILDE
    'I', '2', '\u1ec8', // LATIN CAPITAL LETTER I WITH HOOK ABOVE
    'i', '2', '\u1ec9', // LATIN SMALL LETTER I WITH HOOK ABOVE
    'O', '2', '\u1ece', // LATIN CAPITAL LETTER O WITH HOOK ABOVE
    'o', '2', '\u1ecf', // LATIN SMALL LETTER O WITH HOOK ABOVE
    'U', '2', '\u1ee6', // LATIN CAPITAL LETTER U WITH HOOK ABOVE
    'u', '2', '\u1ee7', // LATIN SMALL LETTER U WITH HOOK ABOVE
    'Y', '!', '\u1ef2', // LATIN CAPITAL LETTER Y WITH GRAVE
    'y', '!', '\u1ef3', // LATIN SMALL LETTER Y WITH GRAVE
    'Y', '2', '\u1ef6', // LATIN CAPITAL LETTER Y WITH HOOK ABOVE
    'y', '2', '\u1ef7', // LATIN SMALL LETTER Y WITH HOOK ABOVE
    'Y', '?', '\u1ef8', // LATIN CAPITAL LETTER Y WITH TILDE
    'y', '?', '\u1ef9', // LATIN SMALL LETTER Y WITH TILDE
    ';', '\'', '\u1f00', // GREEK DASIA AND ACUTE ACCENT
    ',', '\'', '\u1f01', // GREEK PSILI AND ACUTE ACCENT
    ';', '!', '\u1f02', // GREEK DASIA AND VARIA
    ',', '!', '\u1f03', // GREEK PSILI AND VARIA
    '?', ';', '\u1f04', // GREEK DASIA AND PERISPOMENI
    '?', ',', '\u1f05', // GREEK PSILI AND PERISPOMENI
    '!', ':', '\u1f06', // GREEK DIAERESIS AND VARIA
    '?', ':', '\u1f07', // GREEK DIAERESIS AND PERISPOMENI
    '1', 'N', '\u2002', // EN SPACE
    '1', 'M', '\u2003', // EM SPACE
    '3', 'M', '\u2004', // THREE-PER-EM SPACE
    '4', 'M', '\u2005', // FOUR-PER-EM SPACE
    '6', 'M', '\u2006', // SIX-PER-EM SPACE
    '1', 'T', '\u2009', // THIN SPACE
    '1', 'H', '\u200a', // HAIR SPACE
    '-', '1', '\u2010', // HYPHEN
    '-', 'N', '\u2013', // EN DASH
    '-', 'M', '\u2014', // EM DASH
    '-', '3', '\u2015', // HORIZONTAL BAR
    '!', '2', '\u2016', // DOUBLE VERTICAL LINE
    '=', '2', '\u2017', // DOUBLE LOW LINE
    '\'', '6', '\u2018', // LEFT SINGLE QUOTATION MARK
    '\'', '9', '\u2019', // RIGHT SINGLE QUOTATION MARK
    '.', '9', '\u201a', // SINGLE LOW-9 QUOTATION MARK
    '9', '\'', '\u201b', // SINGLE HIGH-REVERSED-9 QUOTATION MARK
    '"', '6', '\u201c', // LEFT DOUBLE QUOTATION MARK
    '"', '9', '\u201d', // RIGHT DOUBLE QUOTATION MARK
    ':', '9', '\u201e', // DOUBLE LOW-9 QUOTATION MARK
    '9', '"', '\u201f', // DOUBLE HIGH-REVERSED-9 QUOTATION MARK
    '/', '-', '\u2020', // DAGGER
    '/', '=', '\u2021', // DOUBLE DAGGER
    '.', '.', '\u2025', // TWO DOT LEADER
    '%', '0', '\u2030', // PER MILLE SIGN
    '1', '\'', '\u2032', // PRIME
    '2', '\'', '\u2033', // DOUBLE PRIME
    '3', '\'', '\u2034', // TRIPLE PRIME
    '1', '"', '\u2035', // REVERSED PRIME
    '2', '"', '\u2036', // REVERSED DOUBLE PRIME
    '3', '"', '\u2037', // REVERSED TRIPLE PRIME
    'C', 'a', '\u2038', // CARET
    '<', '1', '\u2039', // SINGLE LEFT-POINTING ANGLE QUOTATION MARK
    '>', '1', '\u203a', // SINGLE RIGHT-POINTING ANGLE QUOTATION MARK
    ':', 'X', '\u203b', // REFERENCE MARK
    '\'', '-', '\u203e', // OVERLINE
    '/', 'f', '\u2044', // FRACTION SLASH
    '0', 'S', '\u2070', // SUPERSCRIPT DIGIT ZERO
    '4', 'S', '\u2074', // SUPERSCRIPT DIGIT FOUR
    '5', 'S', '\u2075', // SUPERSCRIPT DIGIT FIVE
    '6', 'S', '\u2076', // SUPERSCRIPT DIGIT SIX
    '7', 'S', '\u2077', // SUPERSCRIPT DIGIT SEVEN
    '8', 'S', '\u2078', // SUPERSCRIPT DIGIT EIGHT
    '9', 'S', '\u2079', // SUPERSCRIPT DIGIT NINE
    '+', 'S', '\u207a', // SUPERSCRIPT PLUS SIGN
    '-', 'S', '\u207b', // SUPERSCRIPT MINUS
    '=', 'S', '\u207c', // SUPERSCRIPT EQUALS SIGN
    '(', 'S', '\u207d', // SUPERSCRIPT LEFT PARENTHESIS
    ')', 'S', '\u207e', // SUPERSCRIPT RIGHT PARENTHESIS
    'n', 'S', '\u207f', // SUPERSCRIPT LATIN SMALL LETTER N
    '0', 's', '\u2080', // SUBSCRIPT DIGIT ZERO
    '1', 's', '\u2081', // SUBSCRIPT DIGIT ONE
    '2', 's', '\u2082', // SUBSCRIPT DIGIT TWO
    '3', 's', '\u2083', // SUBSCRIPT DIGIT THREE
    '4', 's', '\u2084', // SUBSCRIPT DIGIT FOUR
    '5', 's', '\u2085', // SUBSCRIPT DIGIT FIVE
    '6', 's', '\u2086', // SUBSCRIPT DIGIT SIX
    '7', 's', '\u2087', // SUBSCRIPT DIGIT SEVEN
    '8', 's', '\u2088', // SUBSCRIPT DIGIT EIGHT
    '9', 's', '\u2089', // SUBSCRIPT DIGIT NINE
    '+', 's', '\u208a', // SUBSCRIPT PLUS SIGN
    '-', 's', '\u208b', // SUBSCRIPT MINUS
    '=', 's', '\u208c', // SUBSCRIPT EQUALS SIGN
    '(', 's', '\u208d', // SUBSCRIPT LEFT PARENTHESIS
    ')', 's', '\u208e', // SUBSCRIPT RIGHT PARENTHESIS
    'L', 'i', '\u20a4', // LIRA SIGN
    'P', 't', '\u20a7', // PESETA SIGN
    'W', '=', '\u20a9', // WON SIGN
    'o', 'C', '\u2103', // DEGREE CENTIGRADE
    'c', 'o', '\u2105', // CARE OF
    'o', 'F', '\u2109', // DEGREE FAHRENHEIT
    'N', '0', '\u2116', // NUMERO SIGN
    'P', 'O', '\u2117', // SOUND RECORDING COPYRIGHT
    'R', 'x', '\u211e', // PRESCRIPTION TAKE
    'S', 'M', '\u2120', // SERVICE MARK
    'T', 'M', '\u2122', // TRADE MARK SIGN
    'O', 'm', '\u2126', // OHM SIGN
    'A', 'O', '\u212b', // ANGSTROEM SIGN
    '1', '3', '\u2153', // VULGAR FRACTION ONE THIRD
    '2', '3', '\u2154', // VULGAR FRACTION TWO THIRDS
    '1', '5', '\u2155', // VULGAR FRACTION ONE FIFTH
    '2', '5', '\u2156', // VULGAR FRACTION TWO FIFTHS
    '3', '5', '\u2157', // VULGAR FRACTION THREE FIFTHS
    '4', '5', '\u2158', // VULGAR FRACTION FOUR FIFTHS
    '1', '6', '\u2159', // VULGAR FRACTION ONE SIXTH
    '5', '6', '\u215a', // VULGAR FRACTION FIVE SIXTHS
    '1', '8', '\u215b', // VULGAR FRACTION ONE EIGHTH
    '3', '8', '\u215c', // VULGAR FRACTION THREE EIGHTHS
    '5', '8', '\u215d', // VULGAR FRACTION FIVE EIGHTHS
    '7', '8', '\u215e', // VULGAR FRACTION SEVEN EIGHTHS
    '1', 'R', '\u2160', // ROMAN NUMERAL ONE
    '2', 'R', '\u2161', // ROMAN NUMERAL TWO
    '3', 'R', '\u2162', // ROMAN NUMERAL THREE
    '4', 'R', '\u2163', // ROMAN NUMERAL FOUR
    '5', 'R', '\u2164', // ROMAN NUMERAL FIVE
    '6', 'R', '\u2165', // ROMAN NUMERAL SIX
    '7', 'R', '\u2166', // ROMAN NUMERAL SEVEN
    '8', 'R', '\u2167', // ROMAN NUMERAL EIGHT
    '9', 'R', '\u2168', // ROMAN NUMERAL NINE
    'a', 'R', '\u2169', // ROMAN NUMERAL TEN
    'b', 'R', '\u216a', // ROMAN NUMERAL ELEVEN
    'c', 'R', '\u216b', // ROMAN NUMERAL TWELVE
    '1', 'r', '\u2170', // SMALL ROMAN NUMERAL ONE
    '2', 'r', '\u2171', // SMALL ROMAN NUMERAL TWO
    '3', 'r', '\u2172', // SMALL ROMAN NUMERAL THREE
    '4', 'r', '\u2173', // SMALL ROMAN NUMERAL FOUR
    '5', 'r', '\u2174', // SMALL ROMAN NUMERAL FIVE
    '6', 'r', '\u2175', // SMALL ROMAN NUMERAL SIX
    '7', 'r', '\u2176', // SMALL ROMAN NUMERAL SEVEN
    '8', 'r', '\u2177', // SMALL ROMAN NUMERAL EIGHT
    '9', 'r', '\u2178', // SMALL ROMAN NUMERAL NINE
    'a', 'r', '\u2179', // SMALL ROMAN NUMERAL TEN
    'b', 'r', '\u217a', // SMALL ROMAN NUMERAL ELEVEN
    'c', 'r', '\u217b', // SMALL ROMAN NUMERAL TWELVE
    '<', '-', '\u2190', // LEFTWARDS ARROW
    '-', '!', '\u2191', // UPWARDS ARROW
    '-', '>', '\u2192', // RIGHTWARDS ARROW
    '-', 'v', '\u2193', // DOWNWARDS ARROW
    '<', '>', '\u2194', // LEFT RIGHT ARROW
    'U', 'D', '\u2195', // UP DOWN ARROW
    '<', '=', '\u21d0', // LEFTWARDS DOUBLE ARROW
    '=', '>', '\u21d2', // RIGHTWARDS DOUBLE ARROW
    '=', '=', '\u21d4', // LEFT RIGHT DOUBLE ARROW
    'F', 'A', '\u2200', // FOR ALL
    'd', 'P', '\u2202', // PARTIAL DIFFERENTIAL
    'T', 'E', '\u2203', // THERE EXISTS
    '/', '0', '\u2205', // EMPTY SET
    'D', 'E', '\u2206', // INCREMENT
    'N', 'B', '\u2207', // NABLA
    '(', '-', '\u2208', // ELEMENT OF
    '-', ')', '\u220b', // CONTAINS AS MEMBER
    '*', 'P', '\u220f', // N-ARY PRODUCT
    '+', 'Z', '\u2211', // N-ARY SUMMATION
    '-', '2', '\u2212', // MINUS SIGN
    '-', '+', '\u2213', // MINUS-OR-PLUS SIGN
    '*', '-', '\u2217', // ASTERISK OPERATOR
    'O', 'b', '\u2218', // RING OPERATOR
    'S', 'b', '\u2219', // BULLET OPERATOR
    'R', 'T', '\u221a', // SQUARE ROOT
    '0', '(', '\u221d', // PROPORTIONAL TO
    '0', '0', '\u221e', // INFINITY
    '-', 'L', '\u221f', // RIGHT ANGLE
    '-', 'V', '\u2220', // ANGLE
    'P', 'P', '\u2225', // PARALLEL TO
    'A', 'N', '\u2227', // LOGICAL AND
    'O', 'R', '\u2228', // LOGICAL OR
    '(', 'U', '\u2229', // INTERSECTION
    ')', 'U', '\u222a', // UNION
    'I', 'n', '\u222b', // INTEGRAL
    'D', 'I', '\u222c', // DOUBLE INTEGRAL
    'I', 'o', '\u222e', // CONTOUR INTEGRAL
    '.', ':', '\u2234', // THEREFORE
    ':', '.', '\u2235', // BECAUSE
    ':', 'R', '\u2236', // RATIO
    ':', ':', '\u2237', // PROPORTION
    '?', '1', '\u223c', // TILDE OPERATOR
    'C', 'G', '\u223e', // INVERTED LAZY S
    '?', '-', '\u2243', // ASYMPTOTICALLY EQUAL TO
    '?', '=', '\u2245', // APPROXIMATELY EQUAL TO
    '?', '2', '\u2248', // ALMOST EQUAL TO
    '=', '?', '\u224c', // ALL EQUAL TO
    'H', 'I', '\u2253', // IMAGE OF OR APPROXIMATELY EQUAL TO
    '!', '=', '\u2260', // NOT EQUAL TO
    '=', '3', '\u2261', // IDENTICAL TO
    '=', '<', '\u2264', // LESS-THAN OR EQUAL TO
    '>', '=', '\u2265', // GREATER-THAN OR EQUAL TO
    '<', '*', '\u226a', // MUCH LESS-THAN
    '*', '>', '\u226b', // MUCH GREATER-THAN
    '!', '<', '\u226e', // NOT LESS-THAN
    '!', '>', '\u226f', // NOT GREATER-THAN
    '(', 'C', '\u2282', // SUBSET OF
    ')', 'C', '\u2283', // SUPERSET OF
    '(', '_', '\u2286', // SUBSET OF OR EQUAL TO
    ')', '_', '\u2287', // SUPERSET OF OR EQUAL TO
    '0', '.', '\u2299', // CIRCLED DOT OPERATOR
    '0', '2', '\u229a', // CIRCLED RING OPERATOR
    '-', 'T', '\u22a5', // UP TACK
    '.', 'P', '\u22c5', // DOT OPERATOR
    ':', '3', '\u22ee', // VERTICAL ELLIPSIS
    '.', '3', '\u22ef', // MIDLINE HORIZONTAL ELLIPSIS
    'E', 'h', '\u2302', // HOUSE
    '<', '7', '\u2308', // LEFT CEILING
    '>', '7', '\u2309', // RIGHT CEILING
    '7', '<', '\u230a', // LEFT FLOOR
    '7', '>', '\u230b', // RIGHT FLOOR
    'N', 'I', '\u2310', // REVERSED NOT SIGN
    '(', 'A', '\u2312', // ARC
    'T', 'R', '\u2315', // TELEPHONE RECORDER
    'I', 'u', '\u2320', // TOP HALF INTEGRAL
    'I', 'l', '\u2321', // BOTTOM HALF INTEGRAL
    '<', '/', '\u2329', // LEFT-POINTING ANGLE BRACKET
    '/', '>', '\u232a', // RIGHT-POINTING ANGLE BRACKET
    'V', 's', '\u2423', // OPEN BOX
    '1', 'h', '\u2440', // OCR HOOK
    '3', 'h', '\u2441', // OCR CHAIR
    '2', 'h', '\u2442', // OCR FORK
    '4', 'h', '\u2443', // OCR INVERTED FORK
    '1', 'j', '\u2446', // OCR BRANCH BANK IDENTIFICATION
    '2', 'j', '\u2447', // OCR AMOUNT OF CHECK
    '3', 'j', '\u2448', // OCR DASH
    '4', 'j', '\u2449', // OCR CUSTOMER ACCOUNT NUMBER
    '1', '.', '\u2488', // DIGIT ONE FULL STOP
    '2', '.', '\u2489', // DIGIT TWO FULL STOP
    '3', '.', '\u248a', // DIGIT THREE FULL STOP
    '4', '.', '\u248b', // DIGIT FOUR FULL STOP
    '5', '.', '\u248c', // DIGIT FIVE FULL STOP
    '6', '.', '\u248d', // DIGIT SIX FULL STOP
    '7', '.', '\u248e', // DIGIT SEVEN FULL STOP
    '8', '.', '\u248f', // DIGIT EIGHT FULL STOP
    '9', '.', '\u2490', // DIGIT NINE FULL STOP
    'h', 'h', '\u2500', // BOX DRAWINGS LIGHT HORIZONTAL
    'H', 'H', '\u2501', // BOX DRAWINGS HEAVY HORIZONTAL
    'v', 'v', '\u2502', // BOX DRAWINGS LIGHT VERTICAL
    'V', 'V', '\u2503', // BOX DRAWINGS HEAVY VERTICAL
    '3', '-', '\u2504', // BOX DRAWINGS LIGHT TRIPLE DASH HORIZONTAL
    '3', '_', '\u2505', // BOX DRAWINGS HEAVY TRIPLE DASH HORIZONTAL
    '3', '!', '\u2506', // BOX DRAWINGS LIGHT TRIPLE DASH VERTICAL
    '3', '/', '\u2507', // BOX DRAWINGS HEAVY TRIPLE DASH VERTICAL
    '4', '-', '\u2508', // BOX DRAWINGS LIGHT QUADRUPLE DASH HORIZONTAL
    '4', '_', '\u2509', // BOX DRAWINGS HEAVY QUADRUPLE DASH HORIZONTAL
    '4', '!', '\u250a', // BOX DRAWINGS LIGHT QUADRUPLE DASH VERTICAL
    '4', '/', '\u250b', // BOX DRAWINGS HEAVY QUADRUPLE DASH VERTICAL
    'd', 'r', '\u250c', // BOX DRAWINGS LIGHT DOWN AND RIGHT
    'd', 'R', '\u250d', // BOX DRAWINGS DOWN LIGHT AND RIGHT HEAVY
    'D', 'r', '\u250e', // BOX DRAWINGS DOWN HEAVY AND RIGHT LIGHT
    'D', 'R', '\u250f', // BOX DRAWINGS HEAVY DOWN AND RIGHT
    'd', 'l', '\u2510', // BOX DRAWINGS LIGHT DOWN AND LEFT
    'd', 'L', '\u2511', // BOX DRAWINGS DOWN LIGHT AND LEFT HEAVY
    'D', 'l', '\u2512', // BOX DRAWINGS DOWN HEAVY AND LEFT LIGHT
    'L', 'D', '\u2513', // BOX DRAWINGS HEAVY DOWN AND LEFT
    'u', 'r', '\u2514', // BOX DRAWINGS LIGHT UP AND RIGHT
    'u', 'R', '\u2515', // BOX DRAWINGS UP LIGHT AND RIGHT HEAVY
    'U', 'r', '\u2516', // BOX DRAWINGS UP HEAVY AND RIGHT LIGHT
    'U', 'R', '\u2517', // BOX DRAWINGS HEAVY UP AND RIGHT
    'u', 'l', '\u2518', // BOX DRAWINGS LIGHT UP AND LEFT
    'u', 'L', '\u2519', // BOX DRAWINGS UP LIGHT AND LEFT HEAVY
    'U', 'l', '\u251a', // BOX DRAWINGS UP HEAVY AND LEFT LIGHT
    'U', 'L', '\u251b', // BOX DRAWINGS HEAVY UP AND LEFT
    'v', 'r', '\u251c', // BOX DRAWINGS LIGHT VERTICAL AND RIGHT
    'v', 'R', '\u251d', // BOX DRAWINGS VERTICAL LIGHT AND RIGHT HEAVY
    'V', 'r', '\u2520', // BOX DRAWINGS VERTICAL HEAVY AND RIGHT LIGHT
    'V', 'R', '\u2523', // BOX DRAWINGS HEAVY VERTICAL AND RIGHT
    'v', 'l', '\u2524', // BOX DRAWINGS LIGHT VERTICAL AND LEFT
    'v', 'L', '\u2525', // BOX DRAWINGS VERTICAL LIGHT AND LEFT HEAVY
    'V', 'l', '\u2528', // BOX DRAWINGS VERTICAL HEAVY AND LEFT LIGHT
    'V', 'L', '\u252b', // BOX DRAWINGS HEAVY VERTICAL AND LEFT
    'd', 'h', '\u252c', // BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
    'd', 'H', '\u252f', // BOX DRAWINGS DOWN LIGHT AND HORIZONTAL HEAVY
    'D', 'h', '\u2530', // BOX DRAWINGS DOWN HEAVY AND HORIZONTAL LIGHT
    'D', 'H', '\u2533', // BOX DRAWINGS HEAVY DOWN AND HORIZONTAL
    'u', 'h', '\u2534', // BOX DRAWINGS LIGHT UP AND HORIZONTAL
    'u', 'H', '\u2537', // BOX DRAWINGS UP LIGHT AND HORIZONTAL HEAVY
    'U', 'h', '\u2538', // BOX DRAWINGS UP HEAVY AND HORIZONTAL LIGHT
    'U', 'H', '\u253b', // BOX DRAWINGS HEAVY UP AND HORIZONTAL
    'v', 'h', '\u253c', // BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
    'v', 'H', '\u253f', // BOX DRAWINGS VERTICAL LIGHT AND HORIZONTAL HEAVY
    'V', 'h', '\u2542', // BOX DRAWINGS VERTICAL HEAVY AND HORIZONTAL LIGHT
    'V', 'H', '\u254b', // BOX DRAWINGS HEAVY VERTICAL AND HORIZONTAL
    'F', 'D', '\u2571', // BOX DRAWINGS LIGHT DIAGONAL UPPER RIGHT TO LOWER LEFT
    'B', 'D', '\u2572', // BOX DRAWINGS LIGHT DIAGONAL UPPER LEFT TO LOWER RIGHT
    'T', 'B', '\u2580', // UPPER HALF BLOCK
    'L', 'B', '\u2584', // LOWER HALF BLOCK
    'F', 'B', '\u2588', // FULL BLOCK
    'l', 'B', '\u258c', // LEFT HALF BLOCK
    'R', 'B', '\u2590', // RIGHT HALF BLOCK
    '.', 'S', '\u2591', // LIGHT SHADE
    ':', 'S', '\u2592', // MEDIUM SHADE
    '?', 'S', '\u2593', // DARK SHADE
    'f', 'S', '\u25a0', // BLACK SQUARE
    'O', 'S', '\u25a1', // WHITE SQUARE
    'R', 'O', '\u25a2', // WHITE SQUARE WITH ROUNDED CORNERS
    'R', 'r', '\u25a3', // WHITE SQUARE CONTAINING BLACK SMALL SQUARE
    'R', 'F', '\u25a4', // SQUARE WITH HORIZONTAL FILL
    'R', 'Y', '\u25a5', // SQUARE WITH VERTICAL FILL
    'R', 'H', '\u25a6', // SQUARE WITH ORTHOGONAL CROSSHATCH FILL
    'R', 'Z', '\u25a7', // SQUARE WITH UPPER LEFT TO LOWER RIGHT FILL
    'R', 'K', '\u25a8', // SQUARE WITH UPPER RIGHT TO LOWER LEFT FILL
    'R', 'X', '\u25a9', // SQUARE WITH DIAGONAL CROSSHATCH FILL
    's', 'B', '\u25aa', // BLACK SMALL SQUARE
    'S', 'R', '\u25ac', // BLACK RECTANGLE
    'O', 'r', '\u25ad', // WHITE RECTANGLE
    'U', 'T', '\u25b2', // BLACK UP-POINTING TRIANGLE
    'u', 'T', '\u25b3', // WHITE UP-POINTING TRIANGLE
    'P', 'R', '\u25b6', // BLACK RIGHT-POINTING TRIANGLE
    'T', 'r', '\u25b7', // WHITE RIGHT-POINTING TRIANGLE
    'D', 't', '\u25bc', // BLACK DOWN-POINTING TRIANGLE
    'd', 'T', '\u25bd', // WHITE DOWN-POINTING TRIANGLE
    'P', 'L', '\u25c0', // BLACK LEFT-POINTING TRIANGLE
    'T', 'l', '\u25c1', // WHITE LEFT-POINTING TRIANGLE
    'D', 'b', '\u25c6', // BLACK DIAMOND
    'D', 'w', '\u25c7', // WHITE DIAMOND
    'L', 'Z', '\u25ca', // LOZENGE
    '0', 'm', '\u25cb', // WHITE CIRCLE
    '0', 'o', '\u25ce', // BULLSEYE
    '0', 'M', '\u25cf', // BLACK CIRCLE
    '0', 'L', '\u25d0', // CIRCLE WITH LEFT HALF BLACK
    '0', 'R', '\u25d1', // CIRCLE WITH RIGHT HALF BLACK
    'S', 'n', '\u25d8', // INVERSE BULLET
    'I', 'c', '\u25d9', // INVERSE WHITE CIRCLE
    'F', 'd', '\u25e2', // BLACK LOWER RIGHT TRIANGLE
    'B', 'd', '\u25e3', // BLACK LOWER LEFT TRIANGLE
    '*', '2', '\u2605', // BLACK STAR
    '*', '1', '\u2606', // WHITE STAR
    '<', 'H', '\u261c', // WHITE LEFT POINTING INDEX
    '>', 'H', '\u261e', // WHITE RIGHT POINTING INDEX
    '0', 'u', '\u263a', // WHITE SMILING FACE
    '0', 'U', '\u263b', // BLACK SMILING FACE
    'S', 'U', '\u263c', // WHITE SUN WITH RAYS
    'F', 'm', '\u2640', // FEMALE SIGN
    'M', 'l', '\u2642', // MALE SIGN
    'c', 'S', '\u2660', // BLACK SPADE SUIT
    'c', 'H', '\u2661', // WHITE HEART SUIT
    'c', 'D', '\u2662', // WHITE DIAMOND SUIT
    'c', 'C', '\u2663', // BLACK CLUB SUIT
    'M', 'd', '\u2669', // QUARTER NOTE
    'M', '8', '\u266a', // EIGHTH NOTE
    'M', '2', '\u266b', // BARRED EIGHTH NOTES
    'M', 'b', '\u266d', // MUSIC FLAT SIGN
    'M', 'x', '\u266e', // MUSIC NATURAL SIGN
    'M', 'X', '\u266f', // MUSIC SHARP SIGN
    'O', 'K', '\u2713', // CHECK MARK
    'X', 'X', '\u2717', // BALLOT X
    '-', 'X', '\u2720', // MALTESE CROSS
    'I', 'S', '\u3000', // IDEOGRAPHIC SPACE
    ',', '_', '\u3001', // IDEOGRAPHIC COMMA
    '.', '_', '\u3002', // IDEOGRAPHIC PERIOD
    '+', '"', '\u3003', // DITTO MARK
    '+', '_', '\u3004', // IDEOGRAPHIC DITTO MARK
    '*', '_', '\u3005', // IDEOGRAPHIC ITERATION MARK
    ';', '_', '\u3006', // IDEOGRAPHIC CLOSING MARK
    '0', '_', '\u3007', // IDEOGRAPHIC NUMBER ZERO
    '<', '+', '\u300a', // LEFT DOUBLE ANGLE BRACKET
    '>', '+', '\u300b', // RIGHT DOUBLE ANGLE BRACKET
    '<', '\'', '\u300c', // LEFT CORNER BRACKET
    '>', '\'', '\u300d', // RIGHT CORNER BRACKET
    '<', '"', '\u300e', // LEFT WHITE CORNER BRACKET
    '>', '"', '\u300f', // RIGHT WHITE CORNER BRACKET
    '(', '"', '\u3010', // LEFT BLACK LENTICULAR BRACKET
    ')', '"', '\u3011', // RIGHT BLACK LENTICULAR BRACKET
    '=', 'T', '\u3012', // POSTAL MARK
    '=', '_', '\u3013', // GETA MARK
    '(', '\'', '\u3014', // LEFT TORTOISE SHELL BRACKET
    ')', '\'', '\u3015', // RIGHT TORTOISE SHELL BRACKET
    '(', 'I', '\u3016', // LEFT WHITE LENTICULAR BRACKET
    ')', 'I', '\u3017', // RIGHT WHITE LENTICULAR BRACKET
    '-', '?', '\u301c', // WAVE DASH
    'A', '5', '\u3041', // HIRAGANA LETTER SMALL A
    'a', '5', '\u3042', // HIRAGANA LETTER A
    'I', '5', '\u3043', // HIRAGANA LETTER SMALL I
    'i', '5', '\u3044', // HIRAGANA LETTER I
    'U', '5', '\u3045', // HIRAGANA LETTER SMALL U
    'u', '5', '\u3046', // HIRAGANA LETTER U
    'E', '5', '\u3047', // HIRAGANA LETTER SMALL E
    'e', '5', '\u3048', // HIRAGANA LETTER E
    'O', '5', '\u3049', // HIRAGANA LETTER SMALL O
    'o', '5', '\u304a', // HIRAGANA LETTER O
    'k', 'a', '\u304b', // HIRAGANA LETTER KA
    'g', 'a', '\u304c', // HIRAGANA LETTER GA
    'k', 'i', '\u304d', // HIRAGANA LETTER KI
    'g', 'i', '\u304e', // HIRAGANA LETTER GI
    'k', 'u', '\u304f', // HIRAGANA LETTER KU
    'g', 'u', '\u3050', // HIRAGANA LETTER GU
    'k', 'e', '\u3051', // HIRAGANA LETTER KE
    'g', 'e', '\u3052', // HIRAGANA LETTER GE
    'k', 'o', '\u3053', // HIRAGANA LETTER KO
    'g', 'o', '\u3054', // HIRAGANA LETTER GO
    's', 'a', '\u3055', // HIRAGANA LETTER SA
    'z', 'a', '\u3056', // HIRAGANA LETTER ZA
    's', 'i', '\u3057', // HIRAGANA LETTER SI
    'z', 'i', '\u3058', // HIRAGANA LETTER ZI
    's', 'u', '\u3059', // HIRAGANA LETTER SU
    'z', 'u', '\u305a', // HIRAGANA LETTER ZU
    's', 'e', '\u305b', // HIRAGANA LETTER SE
    'z', 'e', '\u305c', // HIRAGANA LETTER ZE
    's', 'o', '\u305d', // HIRAGANA LETTER SO
    'z', 'o', '\u305e', // HIRAGANA LETTER ZO
    't', 'a', '\u305f', // HIRAGANA LETTER TA
    'd', 'a', '\u3060', // HIRAGANA LETTER DA
    't', 'i', '\u3061', // HIRAGANA LETTER TI
    'd', 'i', '\u3062', // HIRAGANA LETTER DI
    't', 'U', '\u3063', // HIRAGANA LETTER SMALL TU
    't', 'u', '\u3064', // HIRAGANA LETTER TU
    'd', 'u', '\u3065', // HIRAGANA LETTER DU
    't', 'e', '\u3066', // HIRAGANA LETTER TE
    'd', 'e', '\u3067', // HIRAGANA LETTER DE
    't', 'o', '\u3068', // HIRAGANA LETTER TO
    'd', 'o', '\u3069', // HIRAGANA LETTER DO
    'n', 'a', '\u306a', // HIRAGANA LETTER NA
    'n', 'i', '\u306b', // HIRAGANA LETTER NI
    'n', 'u', '\u306c', // HIRAGANA LETTER NU
    'n', 'e', '\u306d', // HIRAGANA LETTER NE
    'n', 'o', '\u306e', // HIRAGANA LETTER NO
    'h', 'a', '\u306f', // HIRAGANA LETTER HA
    'b', 'a', '\u3070', // HIRAGANA LETTER BA
    'p', 'a', '\u3071', // HIRAGANA LETTER PA
    'h', 'i', '\u3072', // HIRAGANA LETTER HI
    'b', 'i', '\u3073', // HIRAGANA LETTER BI
    'p', 'i', '\u3074', // HIRAGANA LETTER PI
    'h', 'u', '\u3075', // HIRAGANA LETTER HU
    'b', 'u', '\u3076', // HIRAGANA LETTER BU
    'p', 'u', '\u3077', // HIRAGANA LETTER PU
    'h', 'e', '\u3078', // HIRAGANA LETTER HE
    'b', 'e', '\u3079', // HIRAGANA LETTER BE
    'p', 'e', '\u307a', // HIRAGANA LETTER PE
    'h', 'o', '\u307b', // HIRAGANA LETTER HO
    'b', 'o', '\u307c', // HIRAGANA LETTER BO
    'p', 'o', '\u307d', // HIRAGANA LETTER PO
    'm', 'a', '\u307e', // HIRAGANA LETTER MA
    'm', 'i', '\u307f', // HIRAGANA LETTER MI
    'm', 'u', '\u3080', // HIRAGANA LETTER MU
    'm', 'e', '\u3081', // HIRAGANA LETTER ME
    'm', 'o', '\u3082', // HIRAGANA LETTER MO
    'y', 'A', '\u3083', // HIRAGANA LETTER SMALL YA
    'y', 'a', '\u3084', // HIRAGANA LETTER YA
    'y', 'U', '\u3085', // HIRAGANA LETTER SMALL YU
    'y', 'u', '\u3086', // HIRAGANA LETTER YU
    'y', 'O', '\u3087', // HIRAGANA LETTER SMALL YO
    'y', 'o', '\u3088', // HIRAGANA LETTER YO
    'r', 'a', '\u3089', // HIRAGANA LETTER RA
    'r', 'i', '\u308a', // HIRAGANA LETTER RI
    'r', 'u', '\u308b', // HIRAGANA LETTER RU
    'r', 'e', '\u308c', // HIRAGANA LETTER RE
    'r', 'o', '\u308d', // HIRAGANA LETTER RO
    'w', 'A', '\u308e', // HIRAGANA LETTER SMALL WA
    'w', 'a', '\u308f', // HIRAGANA LETTER WA
    'w', 'i', '\u3090', // HIRAGANA LETTER WI
    'w', 'e', '\u3091', // HIRAGANA LETTER WE
    'w', 'o', '\u3092', // HIRAGANA LETTER WO
    'n', '5', '\u3093', // HIRAGANA LETTER N
    'v', 'u', '\u3094', // HIRAGANA LETTER VU
    '"', '5', '\u309b', // KATAKANA-HIRAGANA VOICED SOUND MARK
    '0', '5', '\u309c', // KATAKANA-HIRAGANA SEMI-VOICED SOUND MARK
    '*', '5', '\u309d', // HIRAGANA ITERATION MARK
    '+', '5', '\u309e', // HIRAGANA VOICED ITERATION MARK
    'a', '6', '\u30a1', // KATAKANA LETTER SMALL A
    'A', '6', '\u30a2', // KATAKANA LETTER A
    'i', '6', '\u30a3', // KATAKANA LETTER SMALL I
    'I', '6', '\u30a4', // KATAKANA LETTER I
    'u', '6', '\u30a5', // KATAKANA LETTER SMALL U
    'U', '6', '\u30a6', // KATAKANA LETTER U
    'e', '6', '\u30a7', // KATAKANA LETTER SMALL E
    'E', '6', '\u30a8', // KATAKANA LETTER E
    'o', '6', '\u30a9', // KATAKANA LETTER SMALL O
    'O', '6', '\u30aa', // KATAKANA LETTER O
    'K', 'a', '\u30ab', // KATAKANA LETTER KA
    'G', 'a', '\u30ac', // KATAKANA LETTER GA
    'K', 'i', '\u30ad', // KATAKANA LETTER KI
    'G', 'i', '\u30ae', // KATAKANA LETTER GI
    'K', 'u', '\u30af', // KATAKANA LETTER KU
    'G', 'u', '\u30b0', // KATAKANA LETTER GU
    'K', 'e', '\u30b1', // KATAKANA LETTER KE
    'G', 'e', '\u30b2', // KATAKANA LETTER GE
    'K', 'o', '\u30b3', // KATAKANA LETTER KO
    'G', 'o', '\u30b4', // KATAKANA LETTER GO
    'S', 'a', '\u30b5', // KATAKANA LETTER SA
    'Z', 'a', '\u30b6', // KATAKANA LETTER ZA
    'S', 'i', '\u30b7', // KATAKANA LETTER SI
    'Z', 'i', '\u30b8', // KATAKANA LETTER ZI
    'S', 'u', '\u30b9', // KATAKANA LETTER SU
    'Z', 'u', '\u30ba', // KATAKANA LETTER ZU
    'S', 'e', '\u30bb', // KATAKANA LETTER SE
    'Z', 'e', '\u30bc', // KATAKANA LETTER ZE
    'S', 'o', '\u30bd', // KATAKANA LETTER SO
    'Z', 'o', '\u30be', // KATAKANA LETTER ZO
    'T', 'a', '\u30bf', // KATAKANA LETTER TA
    'D', 'a', '\u30c0', // KATAKANA LETTER DA
    'T', 'i', '\u30c1', // KATAKANA LETTER TI
    'D', 'i', '\u30c2', // KATAKANA LETTER DI
    'T', 'U', '\u30c3', // KATAKANA LETTER SMALL TU
    'T', 'u', '\u30c4', // KATAKANA LETTER TU
    'D', 'u', '\u30c5', // KATAKANA LETTER DU
    'T', 'e', '\u30c6', // KATAKANA LETTER TE
    'D', 'e', '\u30c7', // KATAKANA LETTER DE
    'T', 'o', '\u30c8', // KATAKANA LETTER TO
    'D', 'o', '\u30c9', // KATAKANA LETTER DO
    'N', 'a', '\u30ca', // KATAKANA LETTER NA
    'N', 'i', '\u30cb', // KATAKANA LETTER NI
    'N', 'u', '\u30cc', // KATAKANA LETTER NU
    'N', 'e', '\u30cd', // KATAKANA LETTER NE
    'N', 'o', '\u30ce', // KATAKANA LETTER NO
    'H', 'a', '\u30cf', // KATAKANA LETTER HA
    'B', 'a', '\u30d0', // KATAKANA LETTER BA
    'P', 'a', '\u30d1', // KATAKANA LETTER PA
    'H', 'i', '\u30d2', // KATAKANA LETTER HI
    'B', 'i', '\u30d3', // KATAKANA LETTER BI
    'P', 'i', '\u30d4', // KATAKANA LETTER PI
    'H', 'u', '\u30d5', // KATAKANA LETTER HU
    'B', 'u', '\u30d6', // KATAKANA LETTER BU
    'P', 'u', '\u30d7', // KATAKANA LETTER PU
    'H', 'e', '\u30d8', // KATAKANA LETTER HE
    'B', 'e', '\u30d9', // KATAKANA LETTER BE
    'P', 'e', '\u30da', // KATAKANA LETTER PE
    'H', 'o', '\u30db', // KATAKANA LETTER HO
    'B', 'o', '\u30dc', // KATAKANA LETTER BO
    'P', 'o', '\u30dd', // KATAKANA LETTER PO
    'M', 'a', '\u30de', // KATAKANA LETTER MA
    'M', 'i', '\u30df', // KATAKANA LETTER MI
    'M', 'u', '\u30e0', // KATAKANA LETTER MU
    'M', 'e', '\u30e1', // KATAKANA LETTER ME
    'M', 'o', '\u30e2', // KATAKANA LETTER MO
    'Y', 'A', '\u30e3', // KATAKANA LETTER SMALL YA
    'Y', 'a', '\u30e4', // KATAKANA LETTER YA
    'Y', 'U', '\u30e5', // KATAKANA LETTER SMALL YU
    'Y', 'u', '\u30e6', // KATAKANA LETTER YU
    'Y', 'O', '\u30e7', // KATAKANA LETTER SMALL YO
    'Y', 'o', '\u30e8', // KATAKANA LETTER YO
    'R', 'a', '\u30e9', // KATAKANA LETTER RA
    'R', 'i', '\u30ea', // KATAKANA LETTER RI
    'R', 'u', '\u30eb', // KATAKANA LETTER RU
    'R', 'e', '\u30ec', // KATAKANA LETTER RE
    'R', 'o', '\u30ed', // KATAKANA LETTER RO
    'W', 'A', '\u30ee', // KATAKANA LETTER SMALL WA
    'W', 'a', '\u30ef', // KATAKANA LETTER WA
    'W', 'i', '\u30f0', // KATAKANA LETTER WI
    'W', 'e', '\u30f1', // KATAKANA LETTER WE
    'W', 'o', '\u30f2', // KATAKANA LETTER WO
    'N', '6', '\u30f3', // KATAKANA LETTER N
    'V', 'u', '\u30f4', // KATAKANA LETTER VU
    'K', 'A', '\u30f5', // KATAKANA LETTER SMALL KA
    'K', 'E', '\u30f6', // KATAKANA LETTER SMALL KE
    'V', 'a', '\u30f7', // KATAKANA LETTER VA
    'V', 'i', '\u30f8', // KATAKANA LETTER VI
    'V', 'e', '\u30f9', // KATAKANA LETTER VE
    'V', 'o', '\u30fa', // KATAKANA LETTER VO
    '.', '6', '\u30fb', // KATAKANA MIDDLE DOT
    '-', '6', '\u30fc', // KATAKANA-HIRAGANA PROLONGED SOUND MARK
    '*', '6', '\u30fd', // KATAKANA ITERATION MARK
    '+', '6', '\u30fe', // KATAKANA VOICED ITERATION MARK
    'b', '4', '\u3105', // BOPOMOFO LETTER B
    'p', '4', '\u3106', // BOPOMOFO LETTER P
    'm', '4', '\u3107', // BOPOMOFO LETTER M
    'f', '4', '\u3108', // BOPOMOFO LETTER F
    'd', '4', '\u3109', // BOPOMOFO LETTER D
    't', '4', '\u310a', // BOPOMOFO LETTER T
    'n', '4', '\u310b', // BOPOMOFO LETTER N
    'l', '4', '\u310c', // BOPOMOFO LETTER L
    'g', '4', '\u310d', // BOPOMOFO LETTER G
    'k', '4', '\u310e', // BOPOMOFO LETTER K
    'h', '4', '\u310f', // BOPOMOFO LETTER H
    'j', '4', '\u3110', // BOPOMOFO LETTER J
    'q', '4', '\u3111', // BOPOMOFO LETTER Q
    'x', '4', '\u3112', // BOPOMOFO LETTER X
    'z', 'h', '\u3113', // BOPOMOFO LETTER ZH
    'c', 'h', '\u3114', // BOPOMOFO LETTER CH
    's', 'h', '\u3115', // BOPOMOFO LETTER SH
    'r', '4', '\u3116', // BOPOMOFO LETTER R
    'z', '4', '\u3117', // BOPOMOFO LETTER Z
    'c', '4', '\u3118', // BOPOMOFO LETTER C
    's', '4', '\u3119', // BOPOMOFO LETTER S
    'a', '4', '\u311a', // BOPOMOFO LETTER A
    'o', '4', '\u311b', // BOPOMOFO LETTER O
    'e', '4', '\u311c', // BOPOMOFO LETTER E
    'a', 'i', '\u311e', // BOPOMOFO LETTER AI
    'e', 'i', '\u311f', // BOPOMOFO LETTER EI
    'a', 'u', '\u3120', // BOPOMOFO LETTER AU
    'o', 'u', '\u3121', // BOPOMOFO LETTER OU
    'a', 'n', '\u3122', // BOPOMOFO LETTER AN
    'e', 'n', '\u3123', // BOPOMOFO LETTER EN
    'a', 'N', '\u3124', // BOPOMOFO LETTER ANG
    'e', 'N', '\u3125', // BOPOMOFO LETTER ENG
    'e', 'r', '\u3126', // BOPOMOFO LETTER ER
    'i', '4', '\u3127', // BOPOMOFO LETTER I
    'u', '4', '\u3128', // BOPOMOFO LETTER U
    'i', 'u', '\u3129', // BOPOMOFO LETTER IU
    'v', '4', '\u312a', // BOPOMOFO LETTER V
    'n', 'G', '\u312b', // BOPOMOFO LETTER NG
    'g', 'n', '\u312c', // BOPOMOFO LETTER GN
    '1', 'c', '\u3220', // PARENTHESIZED IDEOGRAPH ONE
    '2', 'c', '\u3221', // PARENTHESIZED IDEOGRAPH TWO
    '3', 'c', '\u3222', // PARENTHESIZED IDEOGRAPH THREE
    '4', 'c', '\u3223', // PARENTHESIZED IDEOGRAPH FOUR
    '5', 'c', '\u3224', // PARENTHESIZED IDEOGRAPH FIVE
    '6', 'c', '\u3225', // PARENTHESIZED IDEOGRAPH SIX
    '7', 'c', '\u3226', // PARENTHESIZED IDEOGRAPH SEVEN
    '8', 'c', '\u3227', // PARENTHESIZED IDEOGRAPH EIGHT
    '9', 'c', '\u3228', // PARENTHESIZED IDEOGRAPH NINE
    '/', 'c', '\ue001', // JOIN THIS LINE WITH NEXT LINE (Mnemonic)
    'U', 'A', '\ue002', // Unit space A (ISO-IR-8-1 064)
    'U', 'B', '\ue003', // Unit space B (ISO-IR-8-1 096)
    '"', '3', '\ue004', // NON-SPACING UMLAUT (ISO-IR-38 201) (character part)
    '"', '1', '\ue005', // NON-SPACING DIAERESIS WITH ACCENT (ISO-IR-70 192) (character part)
    '"', '!', '\ue006', // NON-SPACING GRAVE ACCENT (ISO-IR-103 193) (character part)
    '"', '\'', '\ue007', // NON-SPACING ACUTE ACCENT (ISO-IR-103 194) (character part)
    '"', '>', '\ue008', // NON-SPACING CIRCUMFLEX ACCENT (ISO-IR-103 195) (character part)
    '"', '?', '\ue009', // NON-SPACING TILDE (ISO-IR-103 196) (character part)
    '"', '-', '\ue00a', // NON-SPACING MACRON (ISO-IR-103 197) (character part)
    '"', '(', '\ue00b', // NON-SPACING BREVE (ISO-IR-103 198) (character part)
    '"', '.', '\ue00c', // NON-SPACING DOT ABOVE (ISO-IR-103 199) (character part)
    '"', ':', '\ue00d', // NON-SPACING DIAERESIS (ISO-IR-103 200) (character part)
    '"', '0', '\ue00e', // NON-SPACING RING ABOVE (ISO-IR-103 202) (character part)
    '"', '"', '\ue00f', // NON-SPACING DOUBLE ACCUTE (ISO-IR-103 204) (character part)
    '"', '<', '\ue010', // NON-SPACING CARON (ISO-IR-103 206) (character part)
    '"', ',', '\ue011', // NON-SPACING CEDILLA (ISO-IR-103 203) (character part)
    '"', ';', '\ue012', // NON-SPACING OGONEK (ISO-IR-103 206) (character part)
    '"', '_', '\ue013', // NON-SPACING LOW LINE (ISO-IR-103 204) (character part)
    '"', '=', '\ue014', // NON-SPACING DOUBLE LOW LINE (ISO-IR-38 217) (character part)
    '"', '/', '\ue015', // NON-SPACING LONG SOLIDUS (ISO-IR-128 201) (character part)
    '"', 'i', '\ue016', // GREEK NON-SPACING IOTA BELOW (ISO-IR-55 39) (character part)
    '"', 'd', '\ue017', // GREEK NON-SPACING DASIA PNEUMATA (ISO-IR-55 38) (character part)
    '"', 'p', '\ue018', // GREEK NON-SPACING PSILI PNEUMATA (ISO-IR-55 37) (character part)
    ';', ';', '\ue019', // GREEK DASIA PNEUMATA (ISO-IR-18 92)
    ',', ',', '\ue01a', // GREEK PSILI PNEUMATA (ISO-IR-18 124)
    'b', '3', '\ue01b', // GREEK SMALL LETTER MIDDLE BETA (ISO-IR-18 99)
    'C', 'i', '\ue01c', // CIRCLE (ISO-IR-83 0294)
    'f', '(', '\ue01d', // FUNCTION SIGN (ISO-IR-143 221)
    'e', 'd', '\ue01e', // LATIN SMALL LETTER EZH (ISO-IR-158 142)
    'a', 'm', '\ue01f', // ANTE MERIDIAM SIGN (ISO-IR-149 0267)
    'p', 'm', '\ue020', // POST MERIDIAM SIGN (ISO-IR-149 0268)
    'F', 'l', '\ue023', // DUTCH GUILDER SIGN (IBM437 159)
    'G', 'F', '\ue024', // GAMMA FUNCTION SIGN (ISO-10646-1DIS 032/032/037/122)
    '>', 'V', '\ue025', // RIGHTWARDS VECTOR ABOVE (ISO-10646-1DIS 032/032/038/046)
    '!', '*', '\ue026', // GREEK VARIA (ISO-10646-1DIS 032/032/042/164)
    '?', '*', '\ue027', // GREEK PERISPOMENI (ISO-10646-1DIS 032/032/042/165)
    'J', '<', '\ue028', // LATIN CAPITAL LETTER J WITH CARON (lowercase: 000/000/001/240)
    'f', 'f', '\ufb00', // LATIN SMALL LIGATURE FF
    'f', 'i', '\ufb01', // LATIN SMALL LIGATURE FI
    'f', 'l', '\ufb02', // LATIN SMALL LIGATURE FL
    'f', 't', '\ufb05', // LATIN SMALL LIGATURE FT
    's', 't', '\ufb06', // LATIN SMALL LIGATURE ST
  };

  private static Logger logger = Logger.getInstance(DigraphGroup.class.getName());
}