package com.maddyhome.idea.vim.lang;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.maddyhome.idea.vim.file.VimScriptFileType;

/**
 * <p>Date: 03.11.11</p>
 * <p>VimScript Language.</p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptLanguage extends Language {
  public VimScriptLanguage() {
    super("VimScript");
  }

  public LanguageFileType getAssociatedFileType() {
    return VimScriptFileType.VIM_SCRIPT_FILE_TYPE;
  }
}
