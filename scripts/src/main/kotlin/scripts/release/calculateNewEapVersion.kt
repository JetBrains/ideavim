/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release

fun main(args: Array<String>) {
  println("HI!")
  val projectDir = args[0]
  println("Working directory: $projectDir")
  val (lastVersion, _) = getVersion(projectDir, onlyStable = false)

  val nextVersion = if (lastVersion.suffixTokens.isEmpty()) {
    lastVersion.nextMinor().withSuffix("eap.1").value
  } else {
    check(lastVersion.suffixTokens.size == 2) {
      "We should have exactly two suffix tokens. Current tokens: ${lastVersion.suffixTokens.toList()}"
    }
    check(lastVersion.suffixTokens[0] == "eap") {
      "First suffix token must be eap. Current tokens: ${lastVersion.suffixTokens.toList()}"
    }

    val newEapNumber = lastVersion.suffixTokens[1].toInt().inc()
    lastVersion.withSuffix("eap.$newEapNumber").value
  }

  println("Next eap version: $nextVersion")
  println("##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_version' value='$nextVersion']")
}
