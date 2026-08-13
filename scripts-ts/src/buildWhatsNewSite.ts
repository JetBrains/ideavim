#!/usr/bin/env npx tsx
/**
 * Builds the public "What's New" site from the pages bundled with the plugin.
 *
 * The pages in `src/main/resources/whatsnew-*.html` are written for the IDE: they
 * carry a `__THEME__` and a `__VERSION__` token that `WhatsNewHelper` substitutes
 * at runtime. This script resolves those tokens for the web, wraps every page in
 * its own directory so it gets a clean URL, and generates an index that links all
 * of them. The generated site is what the `publishWhatsNewPages` workflow deploys
 * to GitHub Pages.
 *
 * Usage:
 *   npx tsx scripts-ts/src/buildWhatsNewSite.ts [root-dir] [out-dir]
 *
 * Examples:
 *   npx tsx scripts-ts/src/buildWhatsNewSite.ts . _site
 *   WHATSNEW_BASE_URL=https://whatsnew.ideavim.dev npx tsx scripts-ts/src/buildWhatsNewSite.ts
 */

import { execFileSync } from "node:child_process";
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { join, posix } from "node:path";

/** Where the plugin keeps the pages, relative to the repository root. */
export const PAGES_DIR = join("src", "main", "resources");

/** Canonical origin of the published site. Overridable for a custom domain. */
export const DEFAULT_BASE_URL = "https://jetbrains.github.io/ideavim";

/** URL segment of the not-yet-released page (`whatsnew-tbr.html`). */
export const PREVIEW_SLUG = "preview";

export interface Entry {
  /** Source file name, e.g. `whatsnew-2.45.2.html`. */
  file: string;
  /** URL segment the page is published under, e.g. `2.45.2` or `preview`. */
  slug: string;
  /** Version as numbers for sorting; `null` for the unreleased page. */
  version: number[] | null;
  /** What `__VERSION__` becomes on the page. */
  label: string;
  /** Whether the index links to it — the unreleased page stays unlisted. */
  listed: boolean;
}

/**
 * Parses a bundled page name into an entry, or returns `null` for a file that is
 * not a What's New page (or carries a version this script cannot order).
 */
export function parseEntry(file: string): Entry | null {
  const match = /^whatsnew-(.+)\.html$/.exec(file);
  if (!match) return null;

  const id = match[1];
  if (id === "tbr") {
    return {
      file,
      slug: PREVIEW_SLUG,
      version: null,
      label: "the upcoming release",
      listed: false,
    };
  }
  if (!/^\d+(\.\d+)*$/.test(id)) return null;

  return {
    file,
    slug: id,
    version: id.split(".").map(Number),
    label: id,
    listed: true,
  };
}

/**
 * Orders entries as the index shows them: the unreleased page first, then
 * releases newest to oldest. The comparison is numeric per component, so 2.45.10
 * correctly outranks 2.45.2.
 */
export function compareEntries(a: Entry, b: Entry): number {
  if (!a.version || !b.version) {
    if (a.version) return 1;
    if (b.version) return -1;
    return a.slug.localeCompare(b.slug);
  }
  const length = Math.max(a.version.length, b.version.length);
  for (let i = 0; i < length; i++) {
    const diff = (b.version[i] ?? 0) - (a.version[i] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

export function titleFor(entry: Entry): string {
  return entry.version
    ? `What's New in IdeaVim ${entry.label}`
    : "What's New in IdeaVim — upcoming release";
}

export function descriptionFor(entry: Entry): string {
  return entry.version
    ? `Features, improvements and fixes in IdeaVim ${entry.label}.`
    : "A preview of the features, improvements and fixes in the next IdeaVim release.";
}

export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * Turns a bundled page into a standalone web page: resolves the IDE-only tokens,
 * gives it real metadata, and adds a way back to the other releases.
 */
export function renderPage(html: string, entry: Entry, baseUrl: string): string {
  const title = escapeHtml(titleFor(entry));
  const description = escapeHtml(descriptionFor(entry));
  const url = `${trimSlash(baseUrl)}/${entry.slug}/`;

  const head = [
    `<title>${title}</title>`,
    `  <link href="${url}" rel="canonical">`,
    `  <meta content="${description}" name="description">`,
    `  <meta content="${title}" property="og:title">`,
    `  <meta content="${description}" property="og:description">`,
    `  <meta content="${url}" property="og:url">`,
    `  <meta content="article" property="og:type">`,
    // The upcoming-release page describes unshipped work, so keep it out of search.
    ...(entry.listed ? [] : [`  <meta content="noindex" name="robots">`]),
  ].join("\n");

  let out = html
    // The IDE injects its theme here. On the web nothing does, and the pages'
    // `prefers-color-scheme` block already covers that case — as long as the
    // unresolved token is gone, since it would otherwise match no theme rule.
    .replace(/\s+data-theme="__THEME__"/, "")
    .replace(/__VERSION__/g, () => entry.label);

  out = replaceFirst(out, /<title>[\s\S]*?<\/title>/, head, `no <title> in ${entry.file}`);

  // Inside the IDE there is nowhere to navigate back to, so the index link only
  // exists on the site. A page whose footer was restyled simply doesn't get one.
  const footerLink = /<a href="https:\/\/github\.com\/JetBrains\/ideavim\/blob\/master\/CHANGES\.md">/;
  if (footerLink.test(out)) {
    out = out.replace(footerLink, (match) => `<a href="../">all-releases</a> ·\n        ${match}`);
  } else {
    console.warn(`WARN: ${entry.file} has no changelog footer link; skipping the all-releases link`);
  }

  return out;
}

/** Extracts the `<style>` block (tags included) so the index can reuse it verbatim. */
export function extractStyle(html: string): string {
  const match = /<style>[\s\S]*?<\/style>/.exec(html);
  if (!match) throw new Error("Could not find a <style> block to reuse for the index");
  return match[0];
}

/**
 * Renders the index. The style block is taken from the newest page rather than
 * maintained here, so the hub keeps matching the pages as their design evolves;
 * only the handful of rules the pages have no use for are added on top.
 */
export function renderIndex(
  styleBlock: string,
  entries: Entry[],
  dates: Map<string, string>,
  baseUrl: string,
): string {
  const listed = entries.filter((entry) => entry.listed);
  const url = `${trimSlash(baseUrl)}/`;
  const description = "Release notes for every version of IdeaVim, newest first.";

  const items = listed.map((entry, index) => {
    const date = dates.get(entry.slug);
    const meta = date ? ` <span class="date">— ${escapeHtml(date)}</span>` : "";
    const tag = index === 0 ? ` <span class="tag">[latest]</span>` : "";
    return `        <li><a href="${entry.slug}/">${escapeHtml(entry.label)}</a>${meta}${tag}</li>`;
  });

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta content="width=device-width, initial-scale=1" name="viewport">
  <title>What's New in IdeaVim</title>
  <link href="${url}" rel="canonical">
  <meta content="${description}" name="description">
  <meta content="What's New in IdeaVim" property="og:title">
  <meta content="${description}" property="og:description">
  <meta content="${url}" property="og:url">
  <meta content="website" property="og:type">
  ${styleBlock.split("\n").join("\n  ")}
  <style>
      /* Index-only additions: the shared block above styles links in the footer only. */
      .releases a {
          color: var(--link);
          font-weight: 700;
          text-decoration: none;
      }

      .releases a:hover {
          text-decoration: underline;
      }

      .releases .date {
          color: var(--fg-dim);
      }

      .releases .tag {
          color: var(--accent);
      }
  </style>
</head>
<body>

<div class="term">
  <div class="titlebar">
    <span class="dot r"></span><span class="dot y"></span><span class="dot g"></span>
    <span class="title">ideavim — whats-new — 80×24</span>
  </div>

  <div class="body">
    <p class="runline"><span class="p">~ $</span> <span class="cmd">ls</span> <span class="flag">-t</span>
      <span class="cmd">whats-new/</span></p>
    <p class="out">${description}</p>

    <div class="section">
      <p class="kicker">releases</p>
      <ul class="releases">
${items.join("\n")}
      </ul>
    </div>

    <footer>
      <p><span class="p">~ $</span> # happy editing<span class="caret"></span></p>
      <p>
        <a href="https://plugins.jetbrains.com/plugin/164-ideavim">get-ideavim</a> ·
        <a href="https://github.com/JetBrains/ideavim/blob/master/CHANGES.md">full-changelog</a> ·
        <a href="https://youtrack.jetbrains.com/issues/VIM">report-an-issue</a> ·
        <a href="https://jb.gg/ideavim-eap">join-the-eap</a>
      </p>
    </footer>
  </div>
</div>

</body>
</html>
`;
}

/**
 * Renders `/latest/` — a redirect to the newest release, so the plugin page and
 * the docs can link to a URL that never goes stale.
 */
export function renderLatestRedirect(entry: Entry, baseUrl: string): string {
  const target = `${trimSlash(baseUrl)}/${entry.slug}/`;
  const title = escapeHtml(titleFor(entry));
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta content="0; url=../${entry.slug}/" http-equiv="refresh">
  <link href="${target}" rel="canonical">
  <meta content="noindex" name="robots">
  <title>${title}</title>
</head>
<body>
<p><a href="../${entry.slug}/">${title}</a></p>
</body>
</html>
`;
}

/**
 * Release dates, taken from the commit that added each page. The changelog only
 * has dated headings for some versions (patches roll into their parent minor),
 * so git is the more complete source. Returns an empty map without a git history
 * — the index just omits the dates then.
 */
export function readPageDates(root: string, entries: Entry[]): Map<string, string> {
  const dates = new Map<string, string>();
  for (const entry of entries) {
    try {
      const log = execFileSync(
        "git",
        ["log", "--diff-filter=A", "--format=%as", "--", posix.join("src/main/resources", entry.file)],
        { cwd: root, encoding: "utf-8", stdio: ["ignore", "pipe", "ignore"] },
      );
      // Newest first, so the last line is the commit that first added the page.
      const added = log.trim().split("\n").filter(Boolean).at(-1);
      if (added) dates.set(entry.slug, added);
    } catch {
      // No git, shallow clone, or an untracked page — a missing date is fine.
    }
  }
  return dates;
}

export interface BuildResult {
  entries: Entry[];
  written: string[];
}

/** Reads the bundled pages and writes the whole site into `outDir`. */
export function buildSite(root: string, outDir: string, baseUrl = DEFAULT_BASE_URL): BuildResult {
  const pagesDir = join(root, PAGES_DIR);
  const entries = readdirSync(pagesDir)
    .map(parseEntry)
    .filter((entry): entry is Entry => entry !== null)
    .sort(compareEntries);

  if (entries.length === 0) {
    throw new Error(`No whatsnew-*.html pages found in ${pagesDir}`);
  }

  const written: string[] = [];
  const write = (relativePath: string, content: string) => {
    const target = join(outDir, relativePath);
    mkdirSync(join(target, ".."), { recursive: true });
    writeFileSync(target, content);
    written.push(relativePath);
  };

  let newestStyle: string | null = null;
  for (const entry of entries) {
    const html = readFileSync(join(pagesDir, entry.file), "utf-8");
    write(posix.join(entry.slug, "index.html"), renderPage(html, entry, baseUrl));
    // Entries are sorted newest-first, so the first released page sets the style.
    if (entry.listed && newestStyle === null) newestStyle = extractStyle(html);
  }

  const newest = entries.find((entry) => entry.listed);
  if (!newest || !newestStyle) {
    throw new Error("No released whatsnew page found; refusing to publish an index of previews only");
  }

  write("index.html", renderIndex(newestStyle, entries, readPageDates(root, entries), baseUrl));
  write(posix.join("latest", "index.html"), renderLatestRedirect(newest, baseUrl));

  return { entries, written };
}

function trimSlash(url: string): string {
  return url.replace(/\/+$/, "");
}

function replaceFirst(text: string, pattern: RegExp, replacement: string, error: string): string {
  if (!pattern.test(text)) throw new Error(error);
  return text.replace(pattern, () => replacement);
}

const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
  const [rootDir = process.cwd(), outDir = "_site"] = process.argv.slice(2);
  const baseUrl = process.env.WHATSNEW_BASE_URL ?? DEFAULT_BASE_URL;
  const { entries, written } = buildSite(rootDir, outDir, baseUrl);
  const listed = entries.filter((entry) => entry.listed).map((entry) => entry.label);
  console.log(`Built ${written.length} files in ${outDir} (base URL ${baseUrl})`);
  console.log(`Releases: ${listed.join(", ")}`);
  const unlisted = entries.filter((entry) => !entry.listed).map((entry) => entry.slug);
  if (unlisted.length > 0) console.log(`Unlisted: ${unlisted.join(", ")}`);
}
