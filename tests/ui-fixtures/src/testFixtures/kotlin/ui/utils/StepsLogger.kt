/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.utils

import com.intellij.remoterobot.stepsProcessing.StepLogger
import com.intellij.remoterobot.stepsProcessing.StepWorker

object StepsLogger {
  private var initializaed = false
  private val initializationLock = Any()

  @JvmStatic
  fun init() = synchronized(initializationLock) {
    if (initializaed.not()) {
      StepWorker.registerProcessor(StepLogger())
      initializaed = true
    }
  }
}
