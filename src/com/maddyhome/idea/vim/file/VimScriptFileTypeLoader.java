package com.maddyhome.idea.vim.file;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Date: 02.11.11</p>
 * <p></p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptFileTypeLoader extends FileTypeFactory {
  @Override
  public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
    fileTypeConsumer.consume(VimScriptFileType.VIM_SCRIPT_FILE_TYPE, VimScriptFileType.DEFAULT_EXTENSION);
  }
}
