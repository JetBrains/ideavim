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

  checkBranch(rootDir, releaseType)

  if (releaseType == "patch") {
    println("Skip release branch reset because release type is 'patch'")
    return
  }

  val git = getGit(rootDir)

  val currentCommit = git.log().setMaxCount(1).call().first()
  println("Current commit id: ${currentCommit.id.name}")

  git.checkout()
    .setCreateBranch(true)
    .setName("release")
    .call()
  println("Checked out release branch")

  git.reset()
    .setRef(currentCommit.id.name)
    .call()
  println("release branch reset")

  git.checkout()
    .setCreateBranch(true)
    .setName("master")
    .call()
  println("Checked out master branch")
}
