/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts.help

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Renders IdeaVim's help file, the one `:help` opens, from the command descriptions collected by `HelpProcessor`.
 *
 * Arguments: the path of the help file to write, followed by the paths of the JSON files written by `HelpProcessor`
 * (one per module).
 */
fun main(args: Array<String>) {
  require(args.size >= 2) { "Usage: generateHelp <output file> <help json>..." }
  val output = File(args[0])
  val modules = args.drop(1).map { parseHelpData(File(it).readText()) }

  output.parentFile.mkdirs()
  output.writeText(renderHelp(modules))
  println("Help file is written to $output")
}

data class ExCommandHelp(val names: List<String>, val description: String)

data class CommandHelp(val keys: List<String>, val modes: String, val lookup: Boolean, val description: String)

data class HelpData(val exCommands: List<ExCommandHelp>, val commands: List<CommandHelp>)

fun parseHelpData(json: String): HelpData {
  val root = Json.parseToJsonElement(json).jsonObject
  val exCommands = root.getValue("exCommands").jsonArray.map {
    val obj = it.jsonObject
    ExCommandHelp(obj.strings("names"), obj.string("description"))
  }
  val commands = root.getValue("commands").jsonArray.map {
    val obj = it.jsonObject
    CommandHelp(
      obj.strings("keys"),
      obj.string("modes"),
      obj.getValue("lookup").jsonPrimitive.boolean,
      obj.string("description"),
    )
  }
  return HelpData(exCommands, commands)
}

private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
private fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }

/**
 * A part of the help file. A command shows up in every section whose modes it is available in, except that Visual
 * mode only lists the commands that aren't in Normal mode already, the same way Vim's `index.txt` does.
 */
enum class Section(val title: String, val tag: String, val tagPrefix: String) {
  INSERT("Insert mode", "ideavim-insert", "i_"),
  NORMAL("Normal and Operator-pending mode", "ideavim-normal", ""),
  VISUAL("Visual and Select mode", "ideavim-visual", "v_"),
  CMDLINE("Command-line editing", "ideavim-cmdline", "c_"),
  EX("Ex commands", "ideavim-ex", ":"),
}

private class Entry(val display: String, val tags: List<String>, val description: String, val sortKey: String)

const val TEXT_WIDTH = 78
private const val DESCRIPTION_COLUMN = 24

fun renderHelp(modules: List<HelpData>): String {
  val entries = Section.entries.associateWith { mutableListOf<Entry>() }
  for (command in modules.flatMap { it.commands }) {
    for (section in sectionsOf(command.modes)) {
      val tags = command.keys.map { section.tagPrefix + keyToTag(it) }
      entries.getValue(section).add(Entry(command.keys.joinToString(" "), tags, command.description, command.sortKey))
    }
  }
  for (command in modules.flatMap { it.exCommands }) {
    val tags = command.names.flatMap { exCommandTags(it) }
    val display = command.names.joinToString(" ") { ":$it" }
    entries.getValue(Section.EX).add(Entry(display, tags, command.description, display.lowercase()))
  }

  return buildString {
    appendHeader()
    // A tag must lead to one place, so when two commands claim it, the first one keeps it. That happens for the
    // `lookup` handlers, which share their keys with the command they stand in for while a completion popup is open.
    val usedTags = mutableSetOf<String>()
    Section.entries.forEachIndexed { index, section ->
      append("=".repeat(TEXT_WIDTH)).append('\n')
      appendLine(alignRight("${index + 1}. ${section.title}", "*${section.tag}*"))
      append('\n')
      // Different handlers of the same keys, e.g. for different modes, can share the description
      val sectionEntries = entries.getValue(section).distinctBy { it.display to it.description }
      for (entry in sectionEntries.sortedWith(compareBy({ it.sortKey }, { it.display }))) {
        appendEntry(entry, entry.tags.distinct().filter { usedTags.add(it) })
      }
    }
    append("=".repeat(TEXT_WIDTH)).append('\n')
    append('\n')
    append("This file is generated from the IdeaVim sources by \"./gradlew generateHelp\".\n")
    append("Do not edit it by hand, edit the description of the command instead.\n")
    append('\n')
    append(" vim:tw=78:ts=8:noet:ft=help:norl:\n")
  }
}

private val CommandHelp.sortKey: String
  // Keep a lookup handler right after the command it shares its keys with
  get() = keys.first().lowercase() + if (lookup) "\u0001" else ""

private fun sectionsOf(modes: String): List<Section> = buildList {
  if ('I' in modes) add(Section.INSERT)
  if ('N' in modes || 'O' in modes) add(Section.NORMAL)
  else if ('X' in modes || 'S' in modes) add(Section.VISUAL)
  if ('C' in modes) add(Section.CMDLINE)
}

private fun StringBuilder.appendHeader() {
  appendLine(alignRight("*ideavim.txt*", "*ideavim*"))
  append('\n')
  append(" ".repeat((TEXT_WIDTH - 16) / 2)).append("IDEAVIM COMMANDS\n")
  append('\n')
  append(wrap("This file lists the commands that IdeaVim implements. Type \":help\" followed by a subject to jump to it, for example:", 0))
  append('\n')
  append("    :help x         Normal mode command \"x\"\n")
  append("    :help v_d       Visual mode command \"d\"\n")
  append("    :help i_CTRL-W  Insert mode command CTRL-W\n")
  append("    :help c_CTRL-R  Command-line editing command CTRL-R\n")
  append("    :help :s        Ex command \":s\"\n")
  append('\n')
  append("Contents:\n")
  Section.entries.forEachIndexed { index, section ->
    append(alignRight("${index + 1}. ${section.title}", "|${section.tag}|"))
    append('\n')
  }
  append('\n')
}

private fun StringBuilder.appendEntry(entry: Entry, tags: List<String>) {
  for (tagLine in tagLines(tags)) {
    append(" ".repeat(TEXT_WIDTH - tagLine.length)).append(tagLine).append('\n')
  }
  val description = wrap(entry.description, DESCRIPTION_COLUMN).trimStart()
  if (entry.display.length < DESCRIPTION_COLUMN - 1) {
    append(entry.display.padEnd(DESCRIPTION_COLUMN)).append(description)
  } else {
    append(wrap(entry.display, 0))
    append(" ".repeat(DESCRIPTION_COLUMN)).append(description)
  }
  append('\n')
}

/** Splits the tags into right-aligned lines that fit into the text width */
private fun tagLines(tags: List<String>): List<String> {
  val lines = mutableListOf<String>()
  var line = ""
  for (tag in tags.map { "*$it*" }) {
    line = when {
      line.isEmpty() -> tag
      line.length + 1 + tag.length <= TEXT_WIDTH -> "$line $tag"
      else -> {
        lines.add(line)
        tag
      }
    }
  }
  if (line.isNotEmpty()) lines.add(line)
  return lines
}

private fun alignRight(left: String, right: String): String =
  left + " ".repeat((TEXT_WIDTH - left.length - right.length).coerceAtLeast(1)) + right

/** Wraps [text] to the text width, indenting every line by [indent] spaces. The result ends with a line break. */
private fun wrap(text: String, indent: Int): String = buildString {
  var line = StringBuilder()
  for (word in text.split(Regex("\\s+")).filter { it.isNotEmpty() }) {
    if (line.isNotEmpty() && indent + line.length + 1 + word.length > TEXT_WIDTH) {
      append(" ".repeat(indent)).append(line).append('\n')
      line = StringBuilder()
    }
    if (line.isNotEmpty()) line.append(' ')
    line.append(word)
  }
  if (line.isNotEmpty()) append(" ".repeat(indent)).append(line).append('\n')
}

/**
 * The tags of an Ex command declared as `h[elp]` are `:h` and `:help`, the shortest abbreviation and the full name.
 */
fun exCommandTags(name: String): List<String> {
  val abbreviation = name.substringBefore('[')
  val fullName = name.replace("[", "").replace("]", "")
  return listOf(abbreviation, fullName).distinct().map { ":" + escapeTagChars(it) }
}

/**
 * Converts keys in IdeaVim's notation to a tag in Vim's: `<C-W>h` becomes `CTRL-W_h` and `g*` becomes `gstar`.
 *
 * Mirrors `HelpCommand.normalizeTopic`, which converts what the user types after `:help` the same way.
 */
fun keyToTag(keys: String): String {
  val parts = mutableListOf<String>()
  var i = 0
  while (i < keys.length) {
    val close = if (keys[i] == '<') keys.indexOf('>', i + 1) else -1
    if (close > i + 1) {
      parts.add(keys.substring(i, close + 1))
      i = close + 1
    } else {
      parts.add(keys[i].toString())
      i++
    }
  }
  return buildString {
    var previousIsCtrl = false
    for (part in parts) {
      val ctrl = ctrlKey(part)
      if (isNotEmpty() && (ctrl != null || previousIsCtrl)) append('_')
      append(ctrl ?: escapeTagChars(part))
      previousIsCtrl = ctrl != null
    }
  }
}

/** `<C-W>` is `CTRL-W` in Vim's help, `<C-Left>` stays as it is */
private fun ctrlKey(part: String): String? {
  if (part.length != 5 || !part.startsWith("<C-", ignoreCase = true) || !part.endsWith('>')) return null
  return "CTRL-" + part[3].uppercaseChar()
}

/** The characters that can't be a part of a tag in Vim's help, `*` and `|` delimit tags and links */
private fun escapeTagChars(text: String): String = text.replace("*", "star").replace("|", "bar")
