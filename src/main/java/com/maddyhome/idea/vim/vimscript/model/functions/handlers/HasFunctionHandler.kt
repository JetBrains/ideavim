/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.functions.handlers

import com.intellij.openapi.util.SystemInfoRt
import com.intellij.util.system.CpuArch
import com.intellij.vim.annotations.VimscriptFunction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.statistic.VimscriptState
import com.maddyhome.idea.vim.vimscript.model.VimLContext
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import com.maddyhome.idea.vim.vimscript.model.functions.BuiltinFunctionHandler

@VimscriptFunction(name = "has")
internal class HasFunctionHandler : BuiltinFunctionHandler<VimInt>(minArity = 1, maxArity = 2) {
  private val supportedFeatures = Features.discover()

  override fun doFunction(arguments: Arguments, editor: VimEditor, context: ExecutionContext, vimContext: VimLContext): VimInt {
    val feature = arguments.getString(0).value
    if (feature == "ide") {
      VimscriptState.Util.isIDESpecificConfigurationUsed = true
    }
    return supportedFeatures.contains(feature).asVimInt()
  }

  private object Features {
    fun discover(): Set<String> {
      val result = mutableSetOf("ide")

      collectOperatingSystemType(result)

      return result
    }

    private fun collectOperatingSystemType(result: MutableSet<String>) {
      if (SystemInfoRt.isWindows) {
        result.add("win32")
        if (CpuArch.CURRENT.width == 64) {
          result.add("win64")
        }
      } else if (SystemInfoRt.isLinux) {
        result.add("linux")
      } else if (SystemInfoRt.isMac) {
        result.add("mac")
        result.add("macunix")
        result.add("osx")
        result.add("osxdarwin")
      } else if (SystemInfoRt.isFreeBSD) {
        result.add("bsd")
      } else if (SystemInfoRt.isSolaris) {
        result.add("sun")
      }

      if (SystemInfoRt.isUnix) {
        result.add("unix")
      }
    }
  }
}
