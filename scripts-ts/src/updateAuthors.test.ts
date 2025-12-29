import { describe, it, expect } from "vitest";
import {
  Author,
  isJetBrainsEmployee,
  addAuthorsToContent,
  extractExistingEmails,
  extractExistingGitHubUrls,
  findSectionEndOffset,
} from "./updateAuthors.js";

const sampleAuthorsContent = `IdeaVim Authors
===============

Contributors:

* [![icon][mail]](mailto:existing@example.com)
  [![icon][github]](https://github.com/existinguser)
  &nbsp;
  Existing User

Contributors with JetBrains IP:

* [![icon][mail]](mailto:jbuser@jetbrains.com)
  [![icon][github]](https://github.com/jbuser)
  &nbsp;
  JB User (JetBrains employee)

Previous contributors:

* [![icon][mail]](mailto:old@example.com)
  [![icon][github]](https://github.com/olduser)
  &nbsp;
  Old User

[mail]: assets/icons/mail.png
[github]: assets/icons/github.png`;

describe("updateAuthors", () => {
  describe("addAuthorsToContent", () => {
    it("adds regular contributor to Contributors section", () => {
      const newAuthor: Author = {
        name: "New User",
        url: "https://github.com/newuser",
        mail: "new@example.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [newAuthor]);

      expect(result.newAuthors).toContainEqual(newAuthor);
      expect(result.content).toContain("new@example.com");

      // Should be in Contributors section (before JetBrains IP section)
      const contributorsIndex = result.content.indexOf("Contributors:");
      const jetBrainsIndex = result.content.indexOf("Contributors with JetBrains IP:");
      const newUserIndex = result.content.indexOf("new@example.com");
      expect(newUserIndex).toBeGreaterThan(contributorsIndex);
      expect(newUserIndex).toBeLessThan(jetBrainsIndex);
    });

    it("adds JetBrains employee to JetBrains IP section", () => {
      const jbAuthor: Author = {
        name: "New JB User",
        url: "https://github.com/newjbuser",
        mail: "newjb@jetbrains.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [jbAuthor]);

      expect(result.newAuthors).toContainEqual(jbAuthor);
      expect(result.content).toContain("newjb@jetbrains.com");
      expect(result.content).toContain("New JB User (JetBrains employee)");

      // Should be in JetBrains IP section (before Previous contributors)
      const jetBrainsIndex = result.content.indexOf("Contributors with JetBrains IP:");
      const previousIndex = result.content.indexOf("Previous contributors:");
      const newJbUserIndex = result.content.indexOf("newjb@jetbrains.com");
      expect(newJbUserIndex).toBeGreaterThan(jetBrainsIndex);
      expect(newJbUserIndex).toBeLessThan(previousIndex);
    });

    it("skips author with existing email", () => {
      const existingAuthor: Author = {
        name: "Different Name",
        url: "https://github.com/different",
        mail: "existing@example.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [existingAuthor]);

      expect(result.newAuthors).toHaveLength(0);
      expect(result.content).toBe(sampleAuthorsContent);
    });

    it("skips author with existing GitHub URL", () => {
      const existingAuthor: Author = {
        name: "Different Name",
        url: "https://github.com/existinguser",
        mail: "different@example.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [existingAuthor]);

      expect(result.newAuthors).toHaveLength(0);
      expect(result.content).toBe(sampleAuthorsContent);
    });

    it("skips author already in JetBrains IP section", () => {
      const existingJbAuthor: Author = {
        name: "Different Name",
        url: "https://github.com/different",
        mail: "jbuser@jetbrains.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [existingJbAuthor]);

      expect(result.newAuthors).toHaveLength(0);
      expect(result.content).toBe(sampleAuthorsContent);
    });

    it("adds multiple authors to correct sections", () => {
      const regularAuthor: Author = {
        name: "Regular",
        url: "https://github.com/regular",
        mail: "regular@example.com",
      };
      const jbAuthor: Author = {
        name: "JB Employee",
        url: "https://github.com/jbemp",
        mail: "employee@jetbrains.com",
      };
      const result = addAuthorsToContent(sampleAuthorsContent, [regularAuthor, jbAuthor]);

      expect(result.newAuthors).toHaveLength(2);
      expect(result.content).toContain("regular@example.com");
      expect(result.content).toContain("employee@jetbrains.com");
      expect(result.content).toContain("JB Employee (JetBrains employee)");
      // Regular author should NOT have the JetBrains employee note
      expect(result.content).not.toContain("Regular (JetBrains employee)");
    });
  });

  describe("extractExistingEmails", () => {
    it("finds all emails", () => {
      const emails = extractExistingEmails(sampleAuthorsContent);

      expect(emails.has("existing@example.com")).toBe(true);
      expect(emails.has("jbuser@jetbrains.com")).toBe(true);
      expect(emails.has("old@example.com")).toBe(true);
    });
  });

  describe("extractExistingGitHubUrls", () => {
    it("finds all URLs", () => {
      const urls = extractExistingGitHubUrls(sampleAuthorsContent);

      expect(urls.has("https://github.com/existinguser")).toBe(true);
      expect(urls.has("https://github.com/jbuser")).toBe(true);
      expect(urls.has("https://github.com/olduser")).toBe(true);
    });
  });

  describe("findSectionEndOffset", () => {
    it("finds correct position", () => {
      const offset = findSectionEndOffset(
        sampleAuthorsContent,
        "Contributors:",
        "Contributors with JetBrains IP:"
      );

      expect(offset).toBeGreaterThan(0);
      // The offset should be before "Contributors with JetBrains IP:"
      expect(offset).toBeLessThan(
        sampleAuthorsContent.indexOf("Contributors with JetBrains IP:")
      );
      // And after the last contributor entry
      expect(offset).toBeGreaterThan(sampleAuthorsContent.indexOf("Existing User"));
    });
  });

  describe("isJetBrainsEmployee", () => {
    it("returns true for JetBrains email", () => {
      const jbAuthor: Author = {
        name: "Test",
        url: "https://github.com/test",
        mail: "test@jetbrains.com",
      };
      expect(isJetBrainsEmployee(jbAuthor)).toBe(true);
    });

    it("returns false for non-JetBrains email", () => {
      const regularAuthor: Author = {
        name: "Test",
        url: "https://github.com/test",
        mail: "test@example.com",
      };
      expect(isJetBrainsEmployee(regularAuthor)).toBe(false);
    });
  });
});
