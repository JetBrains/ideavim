package com.maddyhome.idea.vim.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.FlexLexer;
import com.intellij.lexer.Lexer;

import java.io.Reader;

/**
 * <p>Date: 01.11.11</p>
 * <p>VimScript lexer.</p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptFlexLexer extends FlexAdapter {
  public VimScriptFlexLexer() {
    super(new _VimScriptLexer((Reader)null));
  }
}
