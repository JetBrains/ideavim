/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import scripts.release.getGit
import scripts.release.getRepo

fun main(args: Array<String>) {
  val rootDir = args[0]
  println("root dir: $rootDir")

  val currentBranch = getRepo(rootDir).branch
  println("Current branch is $currentBranch")

  val git = getGit(rootDir)

  if (currentBranch != "master") {
    git.checkout().setName("master").call()
    println("Check out master branch")
  }

  git.push()
    .setPushTags()
    .call()
  println("Master pushed with tags")

  git.checkout()
    .setName("release")
    .call()
  println("Checked out release")

  git
    .push()
    .setForce(true)
    .setPushTags()
    .call()
  println("Pushed release branch with tags")

  git.checkout()
    .setName(currentBranch)
    .call()
  println("Checked out $currentBranch branch")
}