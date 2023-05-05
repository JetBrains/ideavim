/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File

class FileWriter {
  fun getYAML(comment: String, any: Any): String {
    val options = DumperOptions()
    options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    val yaml = Yaml(options)
    return comment + yaml.dump(any)
  }

  fun writeFile(filePath: String, content: String) {
    val file = File(filePath)
    file.writeText(content)
  }
}