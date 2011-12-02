package com.maddyhome.idea.vim.file;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import com.maddyhome.idea.vim.lang.VimScriptLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * <p>Date: 02.11.11</p>
 * <p></p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptFileType extends LanguageFileType {
  public static final VimScriptFileType VIM_SCRIPT_FILE_TYPE = new VimScriptFileType();
  public static final Language VIM_SCRIPT_LANGUAGE = VIM_SCRIPT_FILE_TYPE.getLanguage();

  public static final String DEFAULT_EXTENSION = "vim";

  public static final String [] extensions = {DEFAULT_EXTENSION}; //TODO: add "vimrc" or not?

  protected VimScriptFileType() {
    super(new VimScriptLanguage());
  }

  @NotNull
  public String getName() {
    return "VimScript";
  }

  @NotNull
  public String getDescription() {
    return "VimScript (vimL) programming language.";
  }

  @NotNull
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @Nullable
  public Icon getIcon() {
    return IconLoader.findIcon("/icons/logo.png"); //TODO: am I really should do this?
  }
}
