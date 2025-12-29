/**
 * Simple test script to verify TeamCity can run TypeScript scripts.
 * Run with: npx tsx src/teamcityTest.ts
 */

console.log("=== TeamCity TypeScript Test Script ===");
console.log(`Node version: ${process.version}`);
console.log(`Platform: ${process.platform}`);
console.log(`Current directory: ${process.cwd()}`);
console.log(`Script arguments: ${process.argv.slice(2).join(", ") || "(none)"}`);

// Test that we can import modules
import * as fs from "fs";
import * as path from "path";

const packageJsonPath = path.join(process.cwd(), "package.json");
if (fs.existsSync(packageJsonPath)) {
  const pkg = JSON.parse(fs.readFileSync(packageJsonPath, "utf-8"));
  console.log(`Package name: ${pkg.name}`);
  console.log(`Package version: ${pkg.version}`);
}

// Demonstrate TeamCity service messages (for build status reporting)
// See: https://www.jetbrains.com/help/teamcity/service-messages.html
console.log("");
console.log("##teamcity[message text='TypeScript script executed successfully' status='NORMAL']");

// Exit with success
console.log("");
console.log("✓ Test completed successfully!");
process.exit(0);
