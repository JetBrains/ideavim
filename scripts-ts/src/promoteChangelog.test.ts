import { describe, it, expect } from "vitest";
import { promoteChangelog, TO_BE_RELEASED_HEADER } from "./promoteChangelog.js";

const today = new Date("2026-06-01T08:00:00Z");

const sampleChangelog = `The Changelog
=============

History of changes in IdeaVim for the IntelliJ platform.

${TO_BE_RELEASED_HEADER}

### Features:
* New thing

### Fixes:
* Fixed bug

## 2.35.0, 2026-05-14

### Features:
* Older feature
`;

describe("promoteChangelog", () => {
  describe("minor release", () => {
    it("replaces [To Be Released] with versioned header", () => {
      const result = promoteChangelog({
        version: "2.36.0",
        releaseType: "minor",
        today,
        content: sampleChangelog,
      });
      expect(result).toContain("## 2.36.0, 2026-06-01");
      expect(result).not.toContain(TO_BE_RELEASED_HEADER);
      expect(result).toContain("## 2.35.0, 2026-05-14");
    });

    it("preserves unreleased content under the new header", () => {
      const result = promoteChangelog({
        version: "2.36.0",
        releaseType: "minor",
        today,
        content: sampleChangelog,
      });
      const idx = result.indexOf("## 2.36.0, 2026-06-01");
      const featuresIdx = result.indexOf("### Features:");
      expect(idx).toBeGreaterThan(-1);
      expect(featuresIdx).toBeGreaterThan(idx);
    });

    it("inserts a new section above the first ## when [To Be Released] is missing", () => {
      const content = `Header text

## 2.35.0, 2026-05-14
* something
`;
      const result = promoteChangelog({
        version: "2.36.0",
        releaseType: "minor",
        today,
        content,
      });
      const newIdx = result.indexOf("## 2.36.0, 2026-06-01");
      const oldIdx = result.indexOf("## 2.35.0");
      expect(newIdx).toBeGreaterThan(-1);
      expect(newIdx).toBeLessThan(oldIdx);
    });
  });

  describe("major release", () => {
    it("behaves like minor — replaces [To Be Released]", () => {
      const result = promoteChangelog({
        version: "3.0.0",
        releaseType: "major",
        today,
        content: sampleChangelog,
      });
      expect(result).toContain("## 3.0.0, 2026-06-01");
      expect(result).not.toContain(TO_BE_RELEASED_HEADER);
    });
  });

  describe("patch release", () => {
    it("is a no-op: patches roll into the parent minor section", () => {
      const result = promoteChangelog({
        version: "2.35.1",
        releaseType: "patch",
        today,
        content: sampleChangelog,
      });
      expect(result).toBe(sampleChangelog);
    });
  });
});
