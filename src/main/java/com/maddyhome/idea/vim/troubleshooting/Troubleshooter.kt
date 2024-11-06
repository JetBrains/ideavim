/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.troubleshooting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.containers.MultiMap
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import com.maddyhome.idea.vim.key.ToKeysMappingInfo

// [WIP] https://youtrack.jetbrains.com/issue/VIM-2658/Troubleshooter
@Service
internal class Troubleshooter {
  private val problems: MultiMap<String, Problem> = MultiMap()

  fun removeByType(type: String) {
    problems.remove(type)
  }

  fun addProblem(type: String, problem: Problem) {
    problems.putValue(type, problem)
  }

  fun findIncorrectMappings(): List<Problem> {
    val problems = ArrayList<Problem>()
    MappingMode.entries.forEach { mode ->
      injector.keyGroup.getKeyMapping(mode).getAllByOwner(MappingOwner.IdeaVim.InitScript).forEach { entry ->
        (entry.mappingInfo as? ToKeysMappingInfo)?.let { mappingInfo ->
          if (":action" in mappingInfo.toKeys.joinToString { it.keyChar.toString() }) {
            problems += Problem("Mappings contain `:action` call")
          }
        }
      }
    }
    return problems
  }

  companion object {
    fun getInstance() = service<Troubleshooter>()
  }
}

internal class Problem(description: String)
