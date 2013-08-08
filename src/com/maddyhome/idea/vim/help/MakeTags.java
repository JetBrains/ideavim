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

package com.maddyhome.idea.vim.help;

import org.jetbrains.annotations.NotNull;

import java.io.*;

public class MakeTags {
  public static void main(@NotNull String[] args) {
    if (args.length == 0) {
      System.err.println("Usage: java com.maddyhome.idea.vim.help.MakeTags <filelist> > tags");
      return;
    }

    for (String filename : args) {
      final String origFile = filename;
      if (filename.lastIndexOf(File.separatorChar) >= 0) {
        filename = filename.substring(filename.lastIndexOf(File.separatorChar) + 1);
      }

      BufferedReader reader = null;
      try {
        System.err.println("Processing " + filename);
        reader = new BufferedReader(new FileReader(origFile));
        boolean inComment = false;
        String line;
        while ((line = reader.readLine()) != null) {
          if (!inComment && line.startsWith("<!--")) {
            inComment = true;
            continue;
          }
          else if (inComment) {
            if (line.startsWith("-->")) {
              inComment = false;
            }
            continue;
          }

          int start = line.indexOf('*');
          while (start >= 0) {
            int end = line.indexOf('*', start + 1);
            if (end > start + 1) {
              int c;
              for (c = start + 1; c < end; c++) {
                char ch = line.charAt(c);
                if (ch == ' ' || ch == '\t' || ch == '|') {
                  break;
                }
              }

              if (c == end &&
                  (start == 0 || line.charAt(start - 1) == ' ' || line.charAt(start - 1) == '\t') &&
                  (c + 1 >= line.length() || " \t\n\r".indexOf(line.charAt(c + 1)) >= 0)) {
                String tag = line.substring(start + 1, end);

                System.out.print(tag);
                System.out.print('\t');
                System.out.print(filename);
                System.out.print("\t/*");
                for (int j = 0; j < tag.length(); j++) {
                  char ch = tag.charAt(j);
                  if (ch == '\\' || ch == '/') {
                    System.out.print('\\');
                  }
                  System.out.print(ch);
                }
                System.out.print("*");
                System.out.println();

                end = line.indexOf('*', end + 1);
              }
            }
            start = end;
          }
        }
      }
      catch (FileNotFoundException e) {
        System.err.println("Unable to open " + filename);
      }
      catch (IOException e) {
        System.err.println("Unable to read from " + filename);
      }
      finally {
        if (reader != null) {
          try {
            reader.close();
          }
          catch (IOException e) {
            // no-op
          }
        }
      }
    }
  }
}
