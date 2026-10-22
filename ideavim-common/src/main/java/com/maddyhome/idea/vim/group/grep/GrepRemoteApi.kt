/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group.grep

import com.intellij.platform.project.ProjectId
import com.maddyhome.idea.vim.group.rpc.VimRemoteApiResolver
import fleet.rpc.RemoteApi
import fleet.rpc.Rpc
import fleet.rpc.remoteApiDescriptor
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

/**
 * A single `git grep` match.
 *
 * @param line 0-based line of the match
 * @param col 0-based character column of the match start
 * @param text the whole line containing the match
 */
@Serializable
data class GrepMatch(
  val filepath: String,
  val protocol: String,
  val line: Int,
  val col: Int,
  val text: String,
)

/**
 * Result of [GrepRemoteApi.grep]: either [matches] or an [errorMessage] ready to show to the user.
 */
@Serializable
data class GrepResult(
  val matches: List<GrepMatch> = emptyList(),
  val errorMessage: String? = null,
)

/**
 * RPC interface for running `git grep` on the backend.
 *
 * The repository and the git executable live on the backend in split mode, so git must run there.
 *
 * Called from [FugitiveExtension][com.maddyhome.idea.vim.extension.fugitive.FugitiveExtension].
 */
@Rpc
@ApiStatus.Internal
interface GrepRemoteApi : RemoteApi<Unit> {
  /**
   * Runs `git grep [args]` in the work tree containing [filepath], or the project directory if
   * there's no file.
   *
   * @return matches in `git grep` output order
   */
  suspend fun grep(projectId: ProjectId, filepath: String?, protocol: String?, args: List<String>): GrepResult

  companion object {
    @JvmStatic
    suspend fun getInstance(): GrepRemoteApi {
      return VimRemoteApiResolver.resolve(remoteApiDescriptor<GrepRemoteApi>())
    }
  }
}
