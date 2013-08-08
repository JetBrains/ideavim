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

package com.maddyhome.idea.vim.regexp;

public class Magic {
  public static final int AMP = '&' - 256;
  public static final int AT = '@' - 256;
  public static final int DOLLAR = '$' - 256;
  public static final int DOT = '.' - 256;
  public static final int EQUAL = '=' - 256;
  public static final int GREATER = '>' - 256;
  public static final int HAT = '^' - 256;
  public static final int LBRACE = '[' - 256;
  public static final int LCURLY = '{' - 256;
  public static final int LESS = '<' - 256;
  public static final int LPAREN = '(' - 256;
  public static final int PERCENT = '%' - 256;
  public static final int PIPE = '|' - 256;
  public static final int PLUS = '+' - 256;
  public static final int QUESTION = '?' - 256;
  public static final int RPAREN = ')' - 256;
  public static final int STAR = '*' - 256;
  public static final int TILDE = '~' - 256;
  public static final int UNDER = '_' - 256;
  public static final int N0 = '0' - 256;
  public static final int N1 = '1' - 256;
  public static final int N2 = '2' - 256;
  public static final int N3 = '3' - 256;
  public static final int N4 = '4' - 256;
  public static final int N5 = '5' - 256;
  public static final int N6 = '6' - 256;
  public static final int N7 = '7' - 256;
  public static final int N8 = '8' - 256;
  public static final int N9 = '9' - 256;
  public static final int a = 'a' - 256;
  public static final int A = 'A' - 256;
  public static final int c = 'c' - 256;
  public static final int C = 'C' - 256;
  public static final int d = 'd' - 256;
  public static final int D = 'D' - 256;
  public static final int f = 'f' - 256;
  public static final int F = 'F' - 256;
  public static final int h = 'h' - 256;
  public static final int H = 'H' - 256;
  public static final int i = 'i' - 256;
  public static final int I = 'I' - 256;
  public static final int k = 'k' - 256;
  public static final int K = 'K' - 256;
  public static final int l = 'l' - 256;
  public static final int L = 'L' - 256;
  public static final int m = 'm' - 256;
  public static final int M = 'M' - 256;
  public static final int n = 'n' - 256;
  public static final int N = 'N' - 256;
  public static final int o = 'o' - 256;
  public static final int O = 'O' - 256;
  public static final int p = 'p' - 256;
  public static final int P = 'P' - 256;
  public static final int s = 's' - 256;
  public static final int S = 'S' - 256;
  public static final int u = 'u' - 256;
  public static final int U = 'U' - 256;
  public static final int v = 'v' - 256;
  public static final int V = 'V' - 256;
  public static final int w = 'w' - 256;
  public static final int W = 'W' - 256;
  public static final int x = 'x' - 256;
  public static final int X = 'X' - 256;
  public static final int z = 'z' - 256;

  /*
  * Magic characters have a special meaning, they don't match literally.
  * Magic characters are negative.  This separates them from literal characters
  * (possibly multi-byte).  Only ASCII characters can be Magic.
  */
  public static int Magic(int x) {
    return (x - 256);
  }

  public static int un_Magic(int x) {
    return (x + 256);
  }

  public static boolean is_Magic(int x) {
    return (x < 0);
  }

  public static int no_Magic(int x) {
    if (is_Magic(x)) {
      return un_Magic(x);
    }
    return x;
  }

  public static int toggle_Magic(int x) {
    if (is_Magic(x)) {
      return un_Magic(x);
    }
    return Magic(x);
  }
}
