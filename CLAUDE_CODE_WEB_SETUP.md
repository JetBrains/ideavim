# Claude Code on Web - Gradle/Java Project Setup

## Problem Summary

Claude Code on web runs in a sandboxed environment where network access is routed through an authenticated HTTP proxy. **Gradle/Java projects cannot build** because:

1. **DNS Resolution Disabled** - Direct hostname resolution fails (`UnknownHostException`)
2. **Proxy Authentication Required** - The proxy requires JWT-based authentication
3. **Java Proxy Auth Broken for HTTPS** - Java's built-in proxy authentication doesn't work correctly for HTTPS CONNECT tunneling with this proxy type

## Error Symptoms

### Gradle Wrapper Bootstrap Error
```
Exception in thread "main" java.net.UnknownHostException: services.gradle.org
```

### After Configuring Proxy System Properties
```
Exception in thread "main" java.io.IOException: Unable to tunnel through proxy. Proxy returns "HTTP/1.1 401 Unauthorized"
```

### Gradle Daemon Error (Plugin/Dependency Downloads)
```
Failed to get resource: GET. [HTTP HTTP/1.1 401 Unauthorized: https://plugins.gradle.org/m2/...]
```

## Root Cause Analysis

| Component | What Happens | Why |
|-----------|--------------|-----|
| **curl/wget** | ✅ Works | Reads `HTTP_PROXY` env vars and handles proxy auth correctly |
| **Git** | ✅ Works | Pre-configured with proxy settings |
| **npm/yarn** | ✅ Works | Has dedicated `YARN_HTTP_PROXY` env vars |
| **Gradle Wrapper** | ❌ Fails | Java doesn't read `HTTP_PROXY` env vars |
| **Gradle Daemon** | ❌ Fails | Java's `Authenticator` doesn't properly handle proxy auth for HTTPS tunneling |

### Technical Details

Java's HTTP proxy authentication for HTTPS connections (CONNECT tunneling) has known limitations:
- System properties `http.proxyUser`/`http.proxyPassword` are used but may not send auth on first request
- The proxy sends a `407 Proxy Authentication Required` challenge, but Java's response depends on `jdk.http.auth.tunneling.disabledSchemes`
- Even with `Basic` auth enabled, complex credentials (JWT tokens with special chars) may not encode correctly

## Current Workarounds

### Workaround 1: Pre-download Gradle Distribution

The Gradle wrapper can be bypassed by manually downloading and extracting the distribution:

```bash
# 1. Find the expected hash directory
./gradlew --version 2>&1 || true  # Creates hash directory

# 2. Find the hash directory
HASH_DIR=$(ls ~/.gradle/wrapper/dists/gradle-*/  | head -1)

# 3. Download using curl (which respects HTTP_PROXY)
curl -L -o ~/.gradle/wrapper/dists/gradle-*/$HASH_DIR/gradle-X.X.X-bin.zip \
    https://services.gradle.org/distributions/gradle-X.X.X-bin.zip

# 4. Extract and create marker files
cd ~/.gradle/wrapper/dists/gradle-*/$HASH_DIR/
unzip gradle-X.X.X-bin.zip
touch gradle-X.X.X-bin.zip.ok
rm -f gradle-X.X.X-bin.zip.part

# 5. Gradle wrapper now works
./gradlew --version
```

**Result**: ✅ Gradle wrapper works, ❌ but plugin/dependency downloads still fail

### Workaround 2: Pre-populate Gradle Cache

Dependencies can be cached locally, but this requires either:
- Importing from a working machine
- Running Gradle outside the sandboxed environment first

## Proposed Solutions for Claude Code Platform

### Solution 1: Proxy Support for Java (Recommended)

Configure the proxy to support Java's authentication flow. Options:
- Support NTLM/Negotiate auth schemes (Java handles these better)
- Add IP-based allowlisting for certain hostnames (no auth required)
- Implement a local proxy sidecar that handles auth translation

### Solution 2: Pre-populated Gradle Cache

Provide pre-built Docker images or cache layers with common Gradle/Maven dependencies:
- Gradle distributions
- Common plugins (IntelliJ Platform plugin, Kotlin plugin, etc.)
- Core dependencies

### Solution 3: Local Repository Mirror

Run a local repository manager (like Nexus/Artifactory) inside the sandbox that:
- Caches Maven Central, Gradle Plugin Portal, JetBrains repos
- Handles authentication with the external proxy
- Serves cached artifacts without auth to local clients

### Solution 4: HTTP Proxy Wrapper

Provide a proxy wrapper tool that:
- Reads `HTTP_PROXY` environment variable
- Accepts local connections without auth
- Forwards to the real proxy with proper authentication

## Environment Variables in Claude Code Web

```bash
# Primary proxy settings
HTTP_PROXY=http://<container_id>:<jwt_token>@<proxy_ip>:<port>
HTTPS_PROXY=...  # Same format

# Bypass list (doesn't help for external repos)
NO_PROXY=localhost,127.0.0.1,169.254.169.254,*.googleapis.com,*.google.com

# Important flag - indicates proxy does DNS resolution
CLAUDE_CODE_PROXY_RESOLVES_HOSTS=true

# Pre-configured tool-specific proxy settings
YARN_HTTP_PROXY=...
YARN_HTTPS_PROXY=...
GLOBAL_AGENT_HTTP_PROXY=...  # Node.js global-agent
```

## Partial Solution Script

This script sets up what can be configured, but won't fully solve the auth issue:

```bash
#!/bin/bash
# setup-gradle-proxy.sh

if [ -z "$HTTP_PROXY" ]; then
    echo "Not running in Claude Code web environment"
    exit 0
fi

# Parse proxy URL
STRIPPED="${HTTP_PROXY#http://}"
CREDS="${STRIPPED%@*}"
HOST_PORT="${STRIPPED##*@}"
PROXY_USER="${CREDS%%:*}"
PROXY_PASS="${CREDS#*:}"
PROXY_HOST="${HOST_PORT%%:*}"
PROXY_PORT="${HOST_PORT##*:}"

# Configure ~/.gradle/gradle.properties
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << EOF
# Proxy configuration (authentication may not work for HTTPS)
systemProp.http.proxyHost=$PROXY_HOST
systemProp.http.proxyPort=$PROXY_PORT
systemProp.https.proxyHost=$PROXY_HOST
systemProp.https.proxyPort=$PROXY_PORT
systemProp.http.proxyUser=$PROXY_USER
systemProp.http.proxyPassword=$PROXY_PASS
systemProp.https.proxyUser=$PROXY_USER
systemProp.https.proxyPassword=$PROXY_PASS

# Enable Basic auth for HTTPS tunneling (Java 8u111+)
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
EOF

# Also set GRADLE_OPTS for wrapper bootstrap
export GRADLE_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT"
export GRADLE_OPTS="$GRADLE_OPTS -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT"
export GRADLE_OPTS="$GRADLE_OPTS -Djdk.http.auth.tunneling.disabledSchemes="
export GRADLE_OPTS="$GRADLE_OPTS -Djdk.http.auth.proxying.disabledSchemes="

echo "Gradle proxy configured for $PROXY_HOST:$PROXY_PORT"
echo "NOTE: Authentication may fail for HTTPS connections"
```

## Testing Commands

```bash
# Test curl (should work)
curl -v https://services.gradle.org 2>&1 | head -20

# Test Gradle wrapper (fails at bootstrap without manual download)
./gradlew --version

# Check proxy auth failure in logs
cat ~/.gradle/daemon/*/daemon-*.log | grep -i "401\|unauthorized\|failed"
```

## Summary

**Current State**: Gradle/Maven/Java projects cannot fully build in Claude Code on web due to Java's incompatibility with the proxy's authentication mechanism for HTTPS connections.

**What Works**:
- Gradle wrapper bootstrap (after manual distribution download)
- Tasks that don't require network access

**What Doesn't Work**:
- Gradle plugin downloads
- Maven dependency resolution
- Any network operation from the Gradle daemon

**Required Platform Change**: The Claude Code web environment needs one of the proposed solutions to support Java's HTTP client behavior with authenticated proxies.
