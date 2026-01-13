# Claude Code on Web - IdeaVim Development Instructions

This file is loaded when Claude Code detects it's running in the web environment.

**Known Issue**: https://github.com/anthropics/claude-code/issues/13372
**Solution Source**: https://github.com/realgenekim/claude-code-web-bootstrap-clojure-sandbox

## Environment Detection

Claude Code on web is detected by these environment variables:
- `CLAUDE_CODE_PROXY_RESOLVES_HOSTS=true`
- `HTTP_PROXY` contains `jwt_` (JWT-based authentication)
- `IS_SANDBOX=true`

## Automatic Setup

The SessionStart hook (`.claude/hooks/session-start.sh`) automatically:
1. Creates and starts a local proxy shim on `127.0.0.1:3128`
2. Configures `~/.gradle/gradle.properties` for Gradle
3. Configures `~/.m2/settings.xml` for Maven
4. Exports `JAVA_TOOL_OPTIONS` for all Java processes

## Running Gradle Commands

Always use `JAVA_TOOL_OPTIONS` when running Gradle:

```bash
JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128" ./gradlew <task> --console=plain
```

### Examples

```bash
# Build
JAVA_TOOL_OPTIONS="..." ./gradlew build --console=plain

# Run tests
JAVA_TOOL_OPTIONS="..." ./gradlew :test --tests "org.jetbrains.plugins.ideavim.action.ChangeCaseTest" --console=plain

# Check version
JAVA_TOOL_OPTIONS="..." ./gradlew --version --console=plain
```

## First-Time Setup

The first Gradle run may need to download the Gradle distribution manually:

```bash
# If Gradle wrapper fails with UnknownHostException, download manually:
./gradlew --version 2>&1 || true  # Creates hash directory

GRADLE_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\([0-9.]*\)-.*/\1/')
HASH_DIR=$(ls ~/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin/ 2>/dev/null | head -1)

cd ~/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin/$HASH_DIR
curl -L -o gradle-${GRADLE_VERSION}-bin.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
unzip -q gradle-${GRADLE_VERSION}-bin.zip
touch gradle-${GRADLE_VERSION}-bin.zip.ok
rm -f gradle-${GRADLE_VERSION}-bin.zip.part
```

## Troubleshooting

### Check proxy shim is running
```bash
ps aux | grep java-proxy-shim
cat /tmp/java-proxy-shim.log
```

### Test proxy connectivity
```bash
curl -x http://127.0.0.1:3128 https://repo.maven.apache.org/maven2/ | head
```

### Restart proxy shim
```bash
pkill -f java-proxy-shim
python3 ~/.local/bin/java-proxy-shim.py &
```

## Known Limitations

1. **Long first build** - First Gradle build downloads ~1GB+ of IntelliJ Platform dependencies
2. **503 errors** - Occasional 503 errors are usually transient; Gradle will retry
3. **Google Cloud deps** - Some Google Cloud artifacts may fail (DNS bypass issue)

## Full Documentation

See `CLAUDE_CODE_WEB_SETUP.md` in the repository root for complete documentation.
