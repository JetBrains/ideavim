/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

fun main(args: Array<String>) {
  val (_, rootDir, releaseType) = readArgs(args)

  checkReleaseType(releaseType)

  withGit(rootDir) { git ->
    val branchName = when (releaseType) {
      "major", "minor", "patch" -> "release"
      else -> error("Unsupported release type: $releaseType")
    }

    git.checkoutBranch(branchName)

    val currentBranch = withRepo(rootDir) { it.branch }
    check(currentBranch == branchName) {
      "Branch wasn't checked out. Current branch: $currentBranch"
    }
    println("Checked out $branchName branch")
  }
}