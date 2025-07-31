/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.thinapi

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.extension.ExtensionBean
import com.maddyhome.idea.vim.extension.JsonExtensionProvider
import com.maddyhome.idea.vim.extension.KspExtensionBean
import com.maddyhome.idea.vim.extension.toExtensionBean
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import kotlin.io.path.writeText

/**
 * Implementation of [JsonExtensionProvider] that handles reading and writing extensions to a JSON file.
 * This service manages IdeaVim extensions stored in a JSON file located in the IDE's config directory.
 * It provides functionality to initialize, read, write, add, and remove extensions.
 */
@Service
class IjJsonExtensionProvider : JsonExtensionProvider {
  private val json = Json { prettyPrint = true }
  private val logger = vimLogger<IjJsonExtensionProvider>()

  /**
   * Reads extensions from the JSON file.
   *
   * @return List of [ExtensionBean] objects deserialized from the JSON file
   */
  @OptIn(ExperimentalSerializationApi::class)
  private fun readExtensionsFromJson(): List<ExtensionBean> {
    return Json.decodeFromStream(getConfigFile().inputStream())
  }

  /**
   * Writes extensions to the JSON file.
   *
   * @param extensions List of [ExtensionBean] objects to be serialized and written to the JSON file
   */
  private fun writeExtensionsToJson(extensions: List<ExtensionBean>) {
    val encodedExtensions = json.encodeToString(extensions)
    val filePath = getConfigFile().toPath()
    filePath.writeText(encodedExtensions)
  }

  /**
   * Retrieves an extension by its name.
   *
   * @param name The name of the extension to find
   * @return The [ExtensionBean] with the specified name, or null if not found
   */
  override fun getExtension(name: String): ExtensionBean? {
    return readExtensionsFromJson().find { it.extensionName == name }
  }

  /**
   * Retrieves all extensions from the JSON file.
   *
   * @return A list of all [ExtensionBean] objects
   */
  override fun getAllExtensions(): List<ExtensionBean> {
    return readExtensionsFromJson()
  }

  /**
   * Retrieves all extensions associated with a specific plugin.
   *
   * @param pluginId The ID of the plugin to filter extensions by
   * @return A list of [ExtensionBean] objects associated with the specified plugin
   */
  override fun getExtensionsForPlugin(pluginId: String): List<ExtensionBean> {
    return readExtensionsFromJson().filter { it.pluginId == pluginId }
  }

  /**
   * Adds a single extension to the JSON file.
   * If an extension with the same properties already exists, it will not be duplicated.
   *
   * @param extension The [ExtensionBean] to add
   */
  override fun addExtension(extension: ExtensionBean) {
    val allExtensions: Set<ExtensionBean> = readExtensionsFromJson().toSet()
    val newExtensions = allExtensions + extension
    writeExtensionsToJson(newExtensions.toList())
  }

  /**
   * Adds multiple extensions to the JSON file.
   * If any extensions with the same properties already exist, they will not be duplicated.
   *
   * @param extensions Collection of [ExtensionBean] objects to add
   */
  override fun addExtensions(extensions: Collection<ExtensionBean>) {
    val allExtensions: Set<ExtensionBean> = readExtensionsFromJson().toSet()
    val newExtensions = allExtensions + extensions
    writeExtensionsToJson(newExtensions.toList())
  }

  /**
   * Removes all extensions associated with a specific plugin.
   *
   * @param pluginId The ID of the plugin whose extensions should be removed
   */
  @OptIn(ExperimentalSerializationApi::class)
  override fun removeExtensionForPlugin(pluginId: String) {
    val allExtensions: MutableList<ExtensionBean> = readExtensionsFromJson().toMutableList()
    allExtensions.removeIf { it.pluginId == pluginId }
    writeExtensionsToJson(allExtensions)
  }

  /**
   * Initializes the extensions system by creating a new JSON file in the `${PathManager.getConfigPath()}/ideavim`
   * directory.
   * If the file already exists, it will be deleted and recreated.
   * The method copies bundled extensions (within IdeaVim) to the new config file.
   */
  @OptIn(ExperimentalSerializationApi::class)
  override fun init() {
    // 1) Create a new file in the extensions directory (delete the existing one)
    val targetFile = File(ideaVimConfigPath, EXTENSION_LIST_FILE_NAME)

    if (!targetFile.parentFile.exists()) {
      targetFile.parentFile.mkdirs()
    }

    if (targetFile.exists()) {
      targetFile.delete()
    }

    // 2) Copy bundled extensions into the new config file
    val bundledExtensionsFile = "ksp-generated/$EXTENSION_LIST_FILE_NAME"
    val resourceStream: InputStream? =
      this.javaClass.classLoader.getResourceAsStream(bundledExtensionsFile)

    if (resourceStream == null) {
      // even if a file with bundled extensions does not exist, create a config file
      val extensions: List<ExtensionBean> = emptyList()
      Json.encodeToStream(extensions, targetFile.outputStream())

      // throw exception/log
      logger.error("Failed to fetch extensions from $bundledExtensionsFile")
      return
    }

    val bundledExtensions: List<KspExtensionBean> = Json.decodeFromStream(resourceStream)
    val extensions: List<ExtensionBean> = bundledExtensions.map { it.toExtensionBean(IDEAVIM_ID) }

    Json.encodeToStream(extensions, targetFile.outputStream())
  }

  /**
   * Gets the File object representing the extension JSON file.
   *
   * @return File object pointing to the extensions JSON file
   */
  fun getConfigFile(): File {
    val targetFile = File(ideaVimConfigPath, EXTENSION_LIST_FILE_NAME)
    return targetFile
  }

  /**
   * Path to the IdeaVim configuration directory within the IDE's config path.
   */
  private val ideaVimConfigPath: String = "${PathManager.getConfigPath()}/ideavim"

  companion object {
    private const val EXTENSION_LIST_FILE_NAME: String = "ideavim_extensions.json"
    private const val IDEAVIM_ID: String = "IdeaVIM"
  }
}
