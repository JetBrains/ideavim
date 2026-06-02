import { describe, expect, it } from "vitest";
import {
  buildEapReleaseComment,
  READY_TO_RELEASE_NOT_IN_EAP_QUERY,
  resolveReleaseVersion,
} from "./eapReleaseActionsCore.js";

describe("eapReleaseActions", () => {
  it("uses the explicit version argument first", () => {
    expect(resolveReleaseVersion(["2.37.0-eap.2"], {
      ORG_GRADLE_PROJECT_version: "2.37.0-eap.1",
    })).toBe("2.37.0-eap.2");
  });

  it("falls back to ORG_GRADLE_PROJECT_version", () => {
    expect(resolveReleaseVersion([], {
      ORG_GRADLE_PROJECT_version: "2.37.0-eap.1",
    })).toBe("2.37.0-eap.1");
  });

  it("treats an empty version argument as missing", () => {
    expect(resolveReleaseVersion([""], {
      ORG_GRADLE_PROJECT_version: "2.37.0-eap.1",
    })).toBe("2.37.0-eap.1");
  });

  it("uses the Ready To Release tickets that are not already tagged as EAP-released", () => {
    expect(READY_TO_RELEASE_NOT_IN_EAP_QUERY).toBe(
      "#{Ready To Release} tag: -{IdeaVim Released In EAP}",
    );
  });

  it("builds the release comment with the EAP version", () => {
    expect(buildEapReleaseComment("2.37.0-eap.1")).toContain("IdeaVim 2.37.0-eap.1");
    expect(buildEapReleaseComment("2.37.0-eap.1")).toContain("https://jb.gg/ideavim-eap");
  });
});
