package com.maddyhome.idea.vim.lang.lexer;

import com.intellij.psi.tree.IElementType;
import com.maddyhome.idea.vim.file.VimScriptFileType;
import com.maddyhome.idea.vim.lang.VimScriptLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Date: 25.10.11</p>
 * <p></p>
 *
 * @author Ksenia V. Mamich
 * @version 1.0
 */
public class VimScriptElementType extends IElementType {
    public VimScriptElementType(@NotNull String name) {
        super(name, VimScriptFileType.VIM_SCRIPT_LANGUAGE);
    }
}
