/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.release


fun checkReleaseType(releaseType: String) {
  check(releaseType in setOf("major", "minor", "patch")) {
    "This function accepts only major, minor, or path as release type. Current value: '$releaseType'"
  }
}
