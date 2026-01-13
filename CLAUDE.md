# CLAUDE.md

Guidance for Claude Code when working with IdeaVim.

## Quick Reference

Essential commands:
- `./gradlew runIde` - Start dev IntelliJ with IdeaVim
- `./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test` - Run standard tests

Avoid running all tests, this takes too long. It's preferred to run specific test.

When running gradle tasks, use `--console=plain` for cleaner output without progress bars.

See CONTRIBUTING.md for architecture details and a complete command list.

## IdeaVim-Specific Notes

- Property tests can be flaky - verify if failures relate to your changes
- Use `<Action>` in mappings, not `:action`
- Config file: `~/.ideavimrc` (XDG supported)
- Goal: Match Vim functionality and architecture

## Issue Tracking

This project uses **YouTrack** for issue tracking, NOT GitHub Issues.
- Tickets are prefixed with `VIM-` (e.g., VIM-1234)
- YouTrack URL: https://youtrack.jetbrains.com/issues/VIM
- `gh issue` commands will NOT work

## Claude Code on Web

When running in Claude Code on web, Gradle/Java builds require special proxy configuration.
This is a [known issue](https://github.com/anthropics/claude-code/issues/13372).

**Detection**: The environment is detected by `CLAUDE_CODE_PROXY_RESOLVES_HOSTS=true` and JWT-based `HTTP_PROXY`.

**Automatic Setup**: The SessionStart hook (`.claude/hooks/session-start.sh`) automatically configures:
- Local proxy shim on `127.0.0.1:3128`
- Gradle and Maven proxy settings
- `JAVA_TOOL_OPTIONS` environment variable

**Running Gradle**: Always prefix with `JAVA_TOOL_OPTIONS`:
```bash
JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128" ./gradlew <task> --console=plain
```

**Full Documentation**: See `CLAUDE_CODE_WEB_SETUP.md` and `.claude/web-instructions.md`.

## Additional Documentation

- Changelog maintenance: Handled by the `changelog` skill (auto-detected when updating changelog)