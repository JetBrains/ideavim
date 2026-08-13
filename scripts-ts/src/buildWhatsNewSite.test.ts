import { describe, it, expect } from "vitest";
import { mkdtempSync, readFileSync, readdirSync } from "node:fs";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import {
  buildSite,
  compareEntries,
  Entry,
  extractStyle,
  parseEntry,
  PAGES_DIR,
  renderIndex,
  renderLatestRedirect,
  renderPage,
} from "./buildWhatsNewSite.js";

const BASE_URL = "https://example.test/ideavim";
const repoRoot = join(dirname(fileURLToPath(import.meta.url)), "..", "..");

const entryFor = (file: string): Entry => {
  const entry = parseEntry(file);
  if (!entry) throw new Error(`${file} was not parsed as a page`);
  return entry;
};

// A miniature of the real pages: same tokens, same footer, same style block.
const samplePage = `<!doctype html>
<html data-theme="__THEME__" lang="en">
<head>
  <title>What's New in IdeaVim __VERSION__</title>
  <style>
      :root { --link: #0033b3; }
  </style>
</head>
<body>
<p class="out">Here's what landed in <span class="v">__VERSION__</span>.</p>
<footer>
  <p>
    <a href="https://github.com/JetBrains/ideavim/blob/master/CHANGES.md">full-changelog</a> ·
    <a href="https://youtrack.jetbrains.com/issues/VIM">report-an-issue</a>
  </p>
</footer>
</body>
</html>
`;

describe("parseEntry", () => {
  it("reads the version out of a released page", () => {
    expect(parseEntry("whatsnew-2.45.2.html")).toMatchObject({
      slug: "2.45.2",
      version: [2, 45, 2],
      label: "2.45.2",
      listed: true,
    });
  });

  it("publishes the unreleased page unlisted, under /preview/", () => {
    expect(parseEntry("whatsnew-tbr.html")).toMatchObject({
      slug: "preview",
      version: null,
      listed: false,
    });
  });

  it("ignores files that are not What's New pages", () => {
    expect(parseEntry("ideavim.xml")).toBeNull();
    expect(parseEntry("whatsnew-2.45.2.html.bak")).toBeNull();
    expect(parseEntry("whatsnew-draft.html")).toBeNull();
  });
});

describe("compareEntries", () => {
  it("orders releases newest first, numerically not lexicographically", () => {
    const files = [
      "whatsnew-2.44.1.html",
      "whatsnew-2.45.10.html",
      "whatsnew-2.45.2.html",
      "whatsnew-2.5.0.html",
      "whatsnew-2.44.0.html",
    ];
    const sorted = files.map(entryFor).sort(compareEntries).map((entry) => entry.slug);
    expect(sorted).toEqual(["2.45.10", "2.45.2", "2.44.1", "2.44.0", "2.5.0"]);
  });

  it("puts the unreleased page above every release", () => {
    const sorted = ["whatsnew-2.45.2.html", "whatsnew-tbr.html"]
      .map(entryFor)
      .sort(compareEntries)
      .map((entry) => entry.slug);
    expect(sorted).toEqual(["preview", "2.45.2"]);
  });
});

describe("renderPage", () => {
  const rendered = renderPage(samplePage, entryFor("whatsnew-2.45.2.html"), BASE_URL);

  it("resolves the IDE-only tokens", () => {
    expect(rendered).not.toContain("__VERSION__");
    expect(rendered).not.toContain("__THEME__");
    expect(rendered).not.toContain("data-theme");
    expect(rendered).toContain(`landed in <span class="v">2.45.2</span>`);
  });

  it("gives the page a title and canonical metadata", () => {
    expect(rendered).toContain("<title>What's New in IdeaVim 2.45.2</title>");
    expect(rendered).toContain(`<link href="${BASE_URL}/2.45.2/" rel="canonical">`);
    expect(rendered).toContain(`<meta content="${BASE_URL}/2.45.2/" property="og:url">`);
    expect(rendered).not.toContain('name="robots"');
  });

  it("links back to the index from the footer", () => {
    expect(rendered).toContain('<a href="../">all-releases</a>');
  });

  it("keeps the unreleased page out of search results", () => {
    const preview = renderPage(samplePage, entryFor("whatsnew-tbr.html"), BASE_URL);
    expect(preview).toContain('<meta content="noindex" name="robots">');
    expect(preview).toContain("<title>What's New in IdeaVim — upcoming release</title>");
    expect(preview).toContain("landed in <span class=\"v\">the upcoming release</span>");
  });

  it("fails loudly when a page has no title to replace", () => {
    expect(() => renderPage("<html><body>hi</body></html>", entryFor("whatsnew-2.45.2.html"), BASE_URL))
      .toThrow(/no <title>/);
  });

  it("tolerates a page without the changelog footer link", () => {
    const withoutFooter = samplePage.replace(/<footer>[\s\S]*<\/footer>/, "");
    const out = renderPage(withoutFooter, entryFor("whatsnew-2.45.2.html"), BASE_URL);
    expect(out).not.toContain("all-releases");
    expect(out).toContain("2.45.2");
  });
});

describe("renderIndex", () => {
  const entries = ["whatsnew-tbr.html", "whatsnew-2.45.2.html", "whatsnew-2.44.0.html"]
    .map(entryFor)
    .sort(compareEntries);
  const dates = new Map([["2.45.2", "2026-08-01"]]);
  const index = renderIndex(extractStyle(samplePage), entries, dates, BASE_URL);

  it("links every released page, newest first", () => {
    const links = [...index.matchAll(/<a href="([^"]+)\/">/g)].map((match) => match[1]);
    expect(links).toEqual(["2.45.2", "2.44.0"]);
  });

  it("does not link the unreleased page", () => {
    expect(index).not.toContain("preview/");
  });

  it("marks the newest release and shows the dates it knows", () => {
    expect(index).toContain('<a href="2.45.2/">2.45.2</a> <span class="date">— 2026-08-01</span>');
    expect(index).toContain('<span class="tag">[latest]</span>');
    // 2.44.0 has no date in the map, so it is rendered without one.
    expect(index).toContain('<a href="2.44.0/">2.44.0</a></li>');
  });

  it("reuses the style block from the pages", () => {
    expect(index).toContain("--link: #0033b3;");
  });
});

describe("renderLatestRedirect", () => {
  it("redirects to the newest release and stays out of search", () => {
    const html = renderLatestRedirect(entryFor("whatsnew-2.45.2.html"), BASE_URL);
    expect(html).toContain('<meta content="0; url=../2.45.2/" http-equiv="refresh">');
    expect(html).toContain(`<link href="${BASE_URL}/2.45.2/" rel="canonical">`);
    expect(html).toContain('<meta content="noindex" name="robots">');
  });
});

// Builds the site from the pages actually in the repository, which is what the
// publish workflow does. Guards against a real page drifting out of the shape the
// renderer expects (a renamed token, a restyled head) going unnoticed until deploy.
describe("buildSite over the real pages", () => {
  const outDir = mkdtempSync(join(tmpdir(), "whatsnew-site-"));
  const result = buildSite(repoRoot, outDir, BASE_URL);

  it("publishes one page per bundled What's New file", () => {
    const bundled = readdirSync(join(repoRoot, PAGES_DIR)).filter((file) => parseEntry(file));
    expect(bundled.length).toBeGreaterThan(0);
    expect(result.entries).toHaveLength(bundled.length);
    for (const entry of result.entries) {
      expect(result.written).toContain(`${entry.slug}/index.html`);
    }
  });

  it("writes an index and a latest redirect", () => {
    expect(result.written).toContain("index.html");
    expect(result.written).toContain("latest/index.html");
  });

  it("leaves no unresolved IDE tokens anywhere in the output", () => {
    for (const path of result.written) {
      const content = readFileSync(join(outDir, path), "utf-8");
      expect(content, `${path} still has a token`).not.toMatch(/__VERSION__|__THEME__/);
    }
  });

  it("links every listed release from the index", () => {
    const index = readFileSync(join(outDir, "index.html"), "utf-8");
    for (const entry of result.entries.filter((candidate) => candidate.listed)) {
      expect(index).toContain(`<a href="${entry.slug}/">`);
    }
  });
});
