# CLAUDE.md

Guidance for Claude Code when working with IdeaVim.

## Quick Reference

Essential commands:
- `./gradlew runIde` - Start dev IntelliJ with IdeaVim
- `./gradlew test -x :tests:property-tests:test -x :tests:long-running-tests:test` - Run standard tests

Avoid running all tests, this takes too long. It's preferred to run specific test.

See CONTRIBUTING.md for architecture details and a complete command list.

## IdeaVim-Specific Notes

- Property tests can be flaky - verify if failures relate to your changes
- Use `<Action>` in mappings, not `:action` 
- Config file: `~/.ideavimrc` (XDG supported)
- Goal: Match Vim functionality and architecture

## Additional Documentation

- Changelog maintenance: Handled by the `changelog` skill (auto-detected when updating changelog)