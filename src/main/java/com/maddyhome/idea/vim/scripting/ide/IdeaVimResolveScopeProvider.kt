/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.scripting.ide

import com.intellij.codeInsight.multiverse.CodeInsightContext
import com.intellij.codeInsight.multiverse.defaultContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope

class IdeaVimResolveScopeProvider : ResolveScopeProvider() {
  override fun getResolveScope(file: VirtualFile, project: Project): GlobalSearchScope? =
    getResolveScope(file, defaultContext(), project)

  override fun getResolveScope(file: VirtualFile, context: CodeInsightContext, project: Project): GlobalSearchScope? {
    return null
  }
}