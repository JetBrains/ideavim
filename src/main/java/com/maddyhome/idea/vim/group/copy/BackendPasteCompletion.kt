/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.copy

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.ide.CopyPasteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.ApiStatus
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.time.Duration.Companion.seconds

internal val backendPasteAwaiterEP = ExtensionPointName.create<BackendPasteAwaiter>("IdeaVIM.backendPasteAwaiter")

/**
 * On the frontend of split mode, the IDE paste is executed again on the backend, with the clipboard of the backend.
 */
@ApiStatus.Internal
interface BackendPasteAwaiter {
  /** Performs [paste] and returns a function awaiting the backend paste, if there is one */
  fun paste(context: DataContext, paste: (DataContext) -> Unit): (suspend () -> Unit)?
}

/**
 * The frontend clipboard of split mode is synced to the backend, so it must not change until the backend paste is done.
 *
 * Several puts in a row, e.g. in a macro, are all sent to the backend before the first one is pasted there. So the
 * clipboard is put back once, after the backend paste of the last one, and until then each put takes the clipboard
 * content from before the first one as the previous content.
 */
@Service(Service.Level.APP)
internal class BackendPasteCompletion(private val coroutineScope: CoroutineScope) {

  private class PendingRestore(val previousContent: Transferable?, var registerText: String, restores: List<() -> Unit>) {
    val restores = restores.toMutableList()

    fun run() = restores.forEach { it() }
  }

  // Changed on the EDT only
  @Volatile
  private var pending: PendingRestore? = null
  private var pendingJob: Job? = null

  /** The content the clipboard is put back to after the backend paste, while it still holds the pasted register text */
  fun pendingContent(): Transferable? {
    val pending = pending ?: return null
    return pending.previousContent.takeIf { clipboardHolds(pending.registerText) }
  }

  /** The clipboard was written with other content, which must not be replaced after the backend paste */
  fun dropPendingRestores() {
    takePendingRestores()
  }

  /**
   * Performs [paste] and runs [restore] once the backend has pasted too, or right away without a backend. [previousContent]
   * and [registerText] are the clipboard content before the paste and the text put into the clipboard for it.
   */
  fun paste(
    context: DataContext,
    previousContent: Transferable?,
    registerText: String,
    paste: (DataContext) -> Unit,
    restore: () -> Unit,
  ) {
    val awaitBackendPaste = try {
      pasteOnFrontend(context, paste)
    } catch (e: Throwable) {
      restoreAfterPendingPastes(registerText, restore)
      throw e
    }
    if (awaitBackendPaste == null) {
      restoreAfterPendingPastes(registerText, restore)
    } else {
      val restores = takePendingRestores() + restore
      restoreAfterBackendPaste(awaitBackendPaste, PendingRestore(previousContent, registerText, restores))
    }
  }

  private fun pasteOnFrontend(context: DataContext, paste: (DataContext) -> Unit): (suspend () -> Unit)? {
    val awaiter = backendPasteAwaiterEP.extensionList.firstOrNull()
    if (awaiter != null) return awaiter.paste(context, paste)
    paste(context)
    return null
  }

  private fun restoreAfterPendingPastes(registerText: String, restore: () -> Unit) {
    val pending = pending
    if (pending == null) {
      restore()
    } else {
      pending.registerText = registerText
      pending.restores += restore
    }
  }

  /** A later put takes over the restores, as its backend paste comes after the pending ones */
  private fun takePendingRestores(): List<() -> Unit> {
    val restores = pending?.restores.orEmpty()
    pending = null
    pendingJob?.cancel()
    pendingJob = null
    return restores
  }

  private fun restoreAfterBackendPaste(awaitBackendPaste: suspend () -> Unit, restore: PendingRestore) {
    pending = restore
    pendingJob = coroutineScope.launch(Dispatchers.EDT + ModalityState.any().asContextElement(), CoroutineStart.ATOMIC) {
      try {
        awaitWithTimeout(awaitBackendPaste)
      } finally {
        if (pending === restore) {
          pending = null
          pendingJob = null
          restore.run()
        }
      }
    }
  }

  private suspend fun awaitWithTimeout(awaitBackendPaste: suspend () -> Unit) {
    if (withTimeoutOrNull(BACKEND_TIMEOUT) { awaitBackendPaste() } == null) {
      logger.warn("The backend paste didn't complete in $BACKEND_TIMEOUT")
    }
  }

  private fun clipboardHolds(text: String): Boolean = CopyPasteManager.getInstance().contents?.getText() == text

  private fun Transferable.getText(): String? {
    if (!isDataFlavorSupported(DataFlavor.stringFlavor)) return null
    return runCatching { getTransferData(DataFlavor.stringFlavor) as? String }.getOrNull()
  }

  companion object {
    private val BACKEND_TIMEOUT = 5.seconds
    private val logger = logger<BackendPasteCompletion>()

    fun getInstance(): BackendPasteCompletion = service()
  }
}
