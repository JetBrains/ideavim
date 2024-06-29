/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.impl

import com.maddyhome.idea.vim.api.VimInjector
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.group.IjVimOptionGroup
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Test injector that can wraps the original injector and add some functionality
 *
 * THIS IS NECESSARY to be careful with it!
 * It's not supposed to be used to change the injector behaviour in tests. It only should cause some side effects
 *   that DOESN'T affect execution. For example, this may collect additional information about options access
 */
class TestInjector(val injector: VimInjector) : VimInjector by injector {
  private val tracers: MutableMap<Any, Any> = HashMap()

  fun setTracer(key: Any, collector: Any) {
    tracers[key] = collector
  }

  override val vimState
    get() = injector.vimState

  override val optionGroup: VimOptionGroup
    get() {
      val tracer = tracers[OptionsTracer] as? OptionsTraceCollector
      return if (tracer != null) {
        val ignoreFlag = tracers["OptionTracerIgnore"] as AtomicBoolean
        OptionsTracer(injector.optionGroup as IjVimOptionGroup, tracer, ignoreFlag)
      } else {
        injector.optionGroup
      }
    }
}
