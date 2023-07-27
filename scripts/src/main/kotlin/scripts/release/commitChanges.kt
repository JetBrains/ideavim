/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import java.io.File

fun main(args: Array<String>) {
  val (newVersion, rootDir, releaseType) = readArgs(args)

  checkReleaseType(releaseType)

  if (releaseType == "patch") {
    println("Skip committing changes because release type is 'patch'")
    return
  }

  val git = getGit(rootDir)

  git
    .commit()
    .setAll(true)
    .setAuthor("IdeaVim Bot", "maintainers@ideavim.dev")
    .setMessage("Preparation to $newVersion release")
    .setSign(false)
    .call()

  val lastGitMessage = git.log().call().first().shortMessage
  println("Changes committed. Last gitlog message: $lastGitMessage")
}

