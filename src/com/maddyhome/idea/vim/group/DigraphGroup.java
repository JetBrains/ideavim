
package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ui.MorePanel;

import java.util.HashMap;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2004 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

public class DigraphGroup extends AbstractActionGroup
{
    public DigraphGroup()
    {
        loadDigraphs();
    }

    public char getDigraph(char ch1, char ch2)
    {
        String key = new String(new char[]{ch1, ch2});
        Character ch = (Character)digraphs.get(key);
        if (ch == null)
        {
            key = new String(new char[]{ch2, ch1});
            ch = (Character)digraphs.get(key);
        }

        if (ch == null)
        {
            return ch2;
        }
        else
        {
            return ch.charValue();
        }
    }

    public boolean parseCommandLine(Editor editor, String args, boolean failOnBad)
    {
        if (args.length() == 0)
        {
            showDigraphs(editor);

            return true;
        }

        return true;
    }

    private void showDigraphs(Editor editor)
    {
        MorePanel panel = MorePanel.getInstance(editor);
        int width = panel.getDisplayWidth();
        if (width < 10)
        {
            width = 80;
        }
        int colCount = width / 10;
        int height = (int)Math.ceil((double)digraphs.size() / (double)colCount);

        logger.debug("width=" + width);
        logger.debug("colCount=" + colCount);
        logger.debug("height=" + height);

        StringBuffer res = new StringBuffer();
        int cnt = 0;
        for (int i = 0; i < keys.length; i++)
        {
            String key = keys[i];
            if (key == null)
            {
                continue;
            }

            res.append(key);
            res.append(' ');
            if (i < 32)
            {
                res.append('^');
                res.append((char)(i + '@'));
            }
            else if (i >= 128 && i <=159)
            {
                res.append('~');
                res.append((char)(i - 128 + '@'));
            }
            else
            {
                res.append((char)i);
                res.append(' ');
            }
            res.append(' ');
            if (i < 100)
            {
                res.append(' ');
            }
            if (i < 10)
            {
                res.append(' ');
            }
            res.append(i);
            res.append("  ");

            cnt++;
            if (cnt == colCount)
            {
                res.append('\n');
                cnt = 0;
            }
        }

        panel.setText(res.toString());
        panel.setVisible(true);
    }

    private void loadDigraphs()
    {
        for (int i = 0; i < defaultDigraphs.length; i += 2)
        {
            if (defaultDigraphs[i] != '\0' && defaultDigraphs[i + 1] != '\0')
            {
                Character ch = new Character((char)(i / 2));
                String key = new String(new char[]{defaultDigraphs[i], defaultDigraphs[i + 1]});
                digraphs.put(key, ch);
                keys[i / 2] = key;
            }
        }

        // TODO - load custom digraphs from .vimrc
    }

    private HashMap digraphs = new HashMap(256);
    private String[] keys = new String[256];

    private static final char defaultDigraphs[] = {
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
    };

    private static Logger logger = Logger.getInstance(DigraphGroup.class.getName());
}