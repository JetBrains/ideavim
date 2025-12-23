/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package scripts

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UpdateAuthorsTest {

  private val sampleAuthorsContent = """
    |IdeaVim Authors
    |===============
    |
    |Contributors:
    |
    |* [![icon][mail]](mailto:existing@example.com)
    |  [![icon][github]](https://github.com/existinguser)
    |  &nbsp;
    |  Existing User
    |
    |Contributors with JetBrains IP:
    |
    |* [![icon][mail]](mailto:jbuser@jetbrains.com)
    |  [![icon][github]](https://github.com/jbuser)
    |  &nbsp;
    |  JB User (JetBrains employee)
    |
    |Previous contributors:
    |
    |* [![icon][mail]](mailto:old@example.com)
    |  [![icon][github]](https://github.com/olduser)
    |  &nbsp;
    |  Old User
    |
    |[mail]: assets/icons/mail.png
    |[github]: assets/icons/github.png
  """.trimMargin()

  @Test
  fun `adds regular contributor to Contributors section`() {
    val newAuthor = Author("New User", "https://github.com/newuser", "new@example.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(newAuthor))

    assertTrue(result.newAuthors.contains(newAuthor))
    assertTrue(result.content.contains("new@example.com"))
    // Should be in Contributors section (before JetBrains IP section)
    val contributorsIndex = result.content.indexOf("Contributors:")
    val jetBrainsIndex = result.content.indexOf("Contributors with JetBrains IP:")
    val newUserIndex = result.content.indexOf("new@example.com")
    assertTrue(newUserIndex > contributorsIndex && newUserIndex < jetBrainsIndex)
  }

  @Test
  fun `adds JetBrains employee to JetBrains IP section`() {
    val jbAuthor = Author("New JB User", "https://github.com/newjbuser", "newjb@jetbrains.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(jbAuthor))

    assertTrue(result.newAuthors.contains(jbAuthor))
    assertTrue(result.content.contains("newjb@jetbrains.com"))
    assertTrue(result.content.contains("New JB User (JetBrains employee)"))
    // Should be in JetBrains IP section (before Previous contributors)
    val jetBrainsIndex = result.content.indexOf("Contributors with JetBrains IP:")
    val previousIndex = result.content.indexOf("Previous contributors:")
    val newJbUserIndex = result.content.indexOf("newjb@jetbrains.com")
    assertTrue(newJbUserIndex > jetBrainsIndex && newJbUserIndex < previousIndex)
  }

  @Test
  fun `skips author with existing email`() {
    val existingAuthor = Author("Different Name", "https://github.com/different", "existing@example.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(existingAuthor))

    assertTrue(result.newAuthors.isEmpty())
    assertEquals(sampleAuthorsContent, result.content)
  }

  @Test
  fun `skips author with existing GitHub URL`() {
    val existingAuthor = Author("Different Name", "https://github.com/existinguser", "different@example.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(existingAuthor))

    assertTrue(result.newAuthors.isEmpty())
    assertEquals(sampleAuthorsContent, result.content)
  }

  @Test
  fun `skips author already in JetBrains IP section`() {
    val existingJbAuthor = Author("Different Name", "https://github.com/different", "jbuser@jetbrains.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(existingJbAuthor))

    assertTrue(result.newAuthors.isEmpty())
    assertEquals(sampleAuthorsContent, result.content)
  }

  @Test
  fun `adds multiple authors to correct sections`() {
    val regularAuthor = Author("Regular", "https://github.com/regular", "regular@example.com")
    val jbAuthor = Author("JB Employee", "https://github.com/jbemp", "employee@jetbrains.com")
    val result = addAuthorsToContent(sampleAuthorsContent, listOf(regularAuthor, jbAuthor))

    assertEquals(2, result.newAuthors.size)
    assertTrue(result.content.contains("regular@example.com"))
    assertTrue(result.content.contains("employee@jetbrains.com"))
    assertTrue(result.content.contains("JB Employee (JetBrains employee)"))
    // Regular author should NOT have the JetBrains employee note
    assertFalse(result.content.contains("Regular (JetBrains employee)"))
  }

  @Test
  fun `extractExistingEmails finds all emails`() {
    val emails = extractExistingEmails(sampleAuthorsContent)

    assertTrue(emails.contains("existing@example.com"))
    assertTrue(emails.contains("jbuser@jetbrains.com"))
    assertTrue(emails.contains("old@example.com"))
  }

  @Test
  fun `extractExistingGitHubUrls finds all URLs`() {
    val urls = extractExistingGitHubUrls(sampleAuthorsContent)

    assertTrue(urls.contains("https://github.com/existinguser"))
    assertTrue(urls.contains("https://github.com/jbuser"))
    assertTrue(urls.contains("https://github.com/olduser"))
  }

  @Test
  fun `findSectionEndOffset finds correct position`() {
    val offset = findSectionEndOffset(sampleAuthorsContent, "Contributors:", "Contributors with JetBrains IP:")

    assertTrue(offset > 0)
    // The offset should be before "Contributors with JetBrains IP:"
    assertTrue(offset < sampleAuthorsContent.indexOf("Contributors with JetBrains IP:"))
    // And after the last contributor entry
    assertTrue(offset > sampleAuthorsContent.indexOf("Existing User"))
  }

  @Test
  fun `Author isJetBrainsEmployee property works correctly`() {
    val jbAuthor = Author("Test", "https://github.com/test", "test@jetbrains.com")
    val regularAuthor = Author("Test", "https://github.com/test", "test@example.com")

    assertTrue(jbAuthor.isJetBrainsEmployee)
    assertFalse(regularAuthor.isJetBrainsEmployee)
  }
}
