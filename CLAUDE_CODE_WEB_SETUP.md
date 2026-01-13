# Claude Code on Web - Gradle/Java Project Setup

> **Status: VERIFIED WORKING** ✅
> This solution was tested on 2026-01-13 and successfully builds IdeaVim with Gradle 9.2.1.

## Problem Summary

Claude Code on web runs in a sandboxed environment where network access is routed through an authenticated HTTP proxy. **Gradle/Java projects cannot build** because:

1. **DNS Resolution Disabled** - Direct hostname resolution fails (`UnknownHostException`)
2. **Proxy Authentication Required** - The proxy requires JWT-based authentication
3. **Java Proxy Auth Broken for HTTPS** - Java's built-in proxy authentication doesn't work correctly for HTTPS CONNECT tunneling with this proxy type

This is a **known issue** tracked at [anthropics/claude-code#13372](https://github.com/anthropics/claude-code/issues/13372).

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

Claude Code's proxy returns `401 Unauthorized` instead of the RFC 9110 compliant `407 Proxy Authentication Required`, which confuses Java's HTTP client. Additionally:

- Java 8u111+ disabled Basic authentication for HTTPS tunneling by default ([Atlassian KB](https://confluence.atlassian.com/kb/basic-authentication-fails-for-outgoing-proxy-in-java-8u111-909643110.html))
- Even with `-Djdk.http.auth.tunneling.disabledSchemes=""`, complex credentials (JWT tokens) may not encode correctly
- This is a [known JDK bug](https://bugs.openjdk.org/browse/JDK-8210814)

## Working Solution: Local Proxy Shim

The solution is to run a **local proxy shim** that:
1. Accepts unauthenticated connections from Java on `127.0.0.1:3128`
2. Extracts JWT credentials from `HTTP_PROXY` environment variable
3. Forwards requests to Claude's upstream proxy with proper authentication

This approach is documented in [realgenekim/claude-code-web-bootstrap-clojure-sandbox](https://github.com/realgenekim/claude-code-web-bootstrap-clojure-sandbox).

### Step 1: Create the Proxy Shim Script

Create `~/.local/bin/java-proxy-shim.py`:

```python
#!/usr/bin/env python3
"""Local proxy shim that handles auth translation for Claude Code web proxy."""
import socket
import threading
import os
import base64
import select
from urllib.parse import urlparse, unquote

LOCAL_PORT = 3128
UPSTREAM = os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY')

def get_upstream_config():
    """Parse upstream proxy URL to extract host, port, and credentials."""
    p = urlparse(UPSTREAM)
    return p.hostname, p.port, unquote(p.username or ''), unquote(p.password or '')

def handle_client(client_socket):
    """Handle a single client connection."""
    try:
        # Read the initial request
        request = b''
        while b'\r\n\r\n' not in request:
            chunk = client_socket.recv(4096)
            if not chunk:
                return
            request += chunk

        # Parse the request line
        first_line = request.split(b'\r\n')[0].decode()
        method, target, _ = first_line.split(' ', 2)

        # Get upstream proxy config
        proxy_host, proxy_port, user, password = get_upstream_config()
        auth = base64.b64encode(f"{user}:{password}".encode()).decode()

        # Connect to upstream proxy
        upstream = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        upstream.connect((proxy_host, proxy_port))

        if method == 'CONNECT':
            # HTTPS tunneling - send CONNECT with auth to upstream
            connect_req = (
                f"CONNECT {target} HTTP/1.1\r\n"
                f"Host: {target}\r\n"
                f"Proxy-Authorization: Basic {auth}\r\n"
                f"\r\n"
            )
            upstream.send(connect_req.encode())

            # Read upstream response
            response = b''
            while b'\r\n\r\n' not in response:
                chunk = upstream.recv(4096)
                if not chunk:
                    return
                response += chunk

            # Check if tunnel established
            status_line = response.split(b'\r\n')[0].decode()
            if '200' in status_line:
                # Tell client tunnel is established
                client_socket.send(b'HTTP/1.1 200 Connection Established\r\n\r\n')

                # Bidirectional relay
                client_socket.setblocking(False)
                upstream.setblocking(False)

                while True:
                    readable, _, _ = select.select([client_socket, upstream], [], [], 30)
                    if not readable:
                        break
                    for sock in readable:
                        try:
                            data = sock.recv(8192)
                            if not data:
                                return
                            dest = upstream if sock is client_socket else client_socket
                            dest.sendall(data)
                        except:
                            return
            else:
                # Forward error to client
                client_socket.send(response)
        else:
            # Plain HTTP - inject Proxy-Authorization header
            lines = request.split(b'\r\n')
            new_lines = [lines[0]]
            for line in lines[1:]:
                if line.lower().startswith(b'proxy-authorization:'):
                    continue  # Remove existing
                new_lines.append(line)
            # Insert auth header after first line
            new_lines.insert(1, f"Proxy-Authorization: Basic {auth}".encode())
            modified_request = b'\r\n'.join(new_lines)

            upstream.send(modified_request)

            # Relay response
            while True:
                readable, _, _ = select.select([upstream], [], [], 30)
                if not readable:
                    break
                data = upstream.recv(8192)
                if not data:
                    break
                client_socket.sendall(data)

    except Exception as e:
        print(f"Error: {e}")
    finally:
        try:
            client_socket.close()
        except:
            pass
        try:
            upstream.close()
        except:
            pass

def main():
    if not UPSTREAM:
        print("ERROR: HTTP_PROXY or HTTPS_PROXY not set")
        return

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(('127.0.0.1', LOCAL_PORT))
    server.listen(10)

    print(f"Java proxy shim listening on 127.0.0.1:{LOCAL_PORT}")
    print(f"Upstream proxy: {urlparse(UPSTREAM).hostname}:{urlparse(UPSTREAM).port}")

    while True:
        client, addr = server.accept()
        thread = threading.Thread(target=handle_client, args=(client,), daemon=True)
        thread.start()

if __name__ == '__main__':
    main()
```

### Step 2: Start the Proxy Shim

```bash
mkdir -p ~/.local/bin
# Save the script above to ~/.local/bin/java-proxy-shim.py
chmod +x ~/.local/bin/java-proxy-shim.py

# Start in background
python3 ~/.local/bin/java-proxy-shim.py &
```

### Step 3: Configure Gradle to Use the Local Proxy

Create `~/.gradle/gradle.properties`:

```properties
# Use local proxy shim (no auth needed)
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=3128
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=3128

# Enable Basic auth for HTTPS tunneling (Java 8u111+ requirement)
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
```

### Step 4: Configure Maven (if needed)

Create `~/.m2/settings.xml`:

```xml
<settings>
  <proxies>
    <proxy>
      <id>local-shim</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>127.0.0.1</host>
      <port>3128</port>
    </proxy>
    <proxy>
      <id>local-shim-http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>127.0.0.1</host>
      <port>3128</port>
    </proxy>
  </proxies>
</settings>
```

### Step 5: Pre-download Gradle Distribution

The Gradle wrapper bootstrap runs before the daemon reads gradle.properties, so you need to download it manually:

```bash
# Trigger wrapper to create hash directory (will fail, but creates directory)
./gradlew --version 2>&1 || true

# Find the hash directory
GRADLE_VERSION=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | sed 's/.*gradle-\([0-9.]*\)-.*/\1/')
HASH_DIR=$(ls ~/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin/ 2>/dev/null | grep -v '\.zip' | head -1)

if [ -n "$HASH_DIR" ]; then
    cd ~/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin/$HASH_DIR

    # Download using curl (works with HTTP_PROXY)
    curl -L -o gradle-${GRADLE_VERSION}-bin.zip \
        "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

    # Extract and mark as complete
    unzip -q gradle-${GRADLE_VERSION}-bin.zip
    touch gradle-${GRADLE_VERSION}-bin.zip.ok
    rm -f gradle-${GRADLE_VERSION}-bin.zip.part

    echo "Gradle ${GRADLE_VERSION} ready"
fi
```

### Complete Setup Script

Save as `setup-java-proxy.sh` and run at the start of each session:

```bash
#!/bin/bash
# setup-java-proxy.sh - Complete Java/Gradle/Maven proxy setup for Claude Code web

set -e

if [ -z "$HTTP_PROXY" ]; then
    echo "Not running in Claude Code web environment (HTTP_PROXY not set)"
    exit 0
fi

echo "Setting up Java proxy shim for Claude Code web..."

# 1. Create proxy shim script
mkdir -p ~/.local/bin
cat > ~/.local/bin/java-proxy-shim.py << 'PROXY_SCRIPT'
#!/usr/bin/env python3
"""Local proxy shim for Claude Code web - handles auth translation."""
import socket, threading, os, base64, select
from urllib.parse import urlparse, unquote

LOCAL_PORT = 3128
UP = os.environ.get('HTTPS_PROXY') or os.environ.get('HTTP_PROXY')

def get_up():
    p = urlparse(UP)
    return p.hostname, p.port, unquote(p.username or ''), unquote(p.password or '')

def handle(c):
    try:
        req = b''
        while b'\r\n\r\n' not in req:
            d = c.recv(4096)
            if not d: return
            req += d

        line = req.split(b'\r\n')[0].decode()
        method, target, _ = line.split(' ', 2)
        host, port, user, pwd = get_up()
        auth = base64.b64encode(f"{user}:{pwd}".encode()).decode()

        up = socket.socket()
        up.connect((host, port))

        if method == 'CONNECT':
            up.send(f"CONNECT {target} HTTP/1.1\r\nHost: {target}\r\nProxy-Authorization: Basic {auth}\r\n\r\n".encode())
            resp = b''
            while b'\r\n\r\n' not in resp:
                d = up.recv(4096)
                if not d: return
                resp += d
            if b'200' in resp.split(b'\r\n')[0]:
                c.send(b'HTTP/1.1 200 Connection Established\r\n\r\n')
                c.setblocking(False); up.setblocking(False)
                while True:
                    r, _, _ = select.select([c, up], [], [], 30)
                    if not r: break
                    for s in r:
                        d = s.recv(8192)
                        if not d: return
                        (up if s is c else c).sendall(d)
            else:
                c.send(resp)
        else:
            lines = req.split(b'\r\n')
            lines.insert(1, f"Proxy-Authorization: Basic {auth}".encode())
            up.send(b'\r\n'.join(lines))
            while True:
                r, _, _ = select.select([up], [], [], 30)
                if not r: break
                d = up.recv(8192)
                if not d: break
                c.sendall(d)
    except: pass
    finally:
        try: c.close()
        except: pass

if __name__ == '__main__':
    if not UP: print("ERROR: No proxy"); exit(1)
    srv = socket.socket()
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(('127.0.0.1', LOCAL_PORT))
    srv.listen(10)
    print(f"Proxy shim on 127.0.0.1:{LOCAL_PORT}")
    while True:
        cl, _ = srv.accept()
        threading.Thread(target=handle, args=(cl,), daemon=True).start()
PROXY_SCRIPT
chmod +x ~/.local/bin/java-proxy-shim.py

# 2. Start proxy shim (kill existing if running)
pkill -f "java-proxy-shim" 2>/dev/null || true
sleep 1
python3 ~/.local/bin/java-proxy-shim.py &
sleep 2

# 3. Configure Gradle
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << 'EOF'
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=3128
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=3128
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
EOF

# 4. Configure Maven
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << 'EOF'
<settings>
  <proxies>
    <proxy><id>https</id><active>true</active><protocol>https</protocol><host>127.0.0.1</host><port>3128</port></proxy>
    <proxy><id>http</id><active>true</active><protocol>http</protocol><host>127.0.0.1</host><port>3128</port></proxy>
  </proxies>
</settings>
EOF

# 5. Set JAVA_TOOL_OPTIONS for any Java process
export JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128"

echo "Java proxy setup complete!"
echo "Proxy shim running on 127.0.0.1:3128"
```

## Testing

```bash
# 1. Run setup
source setup-java-proxy.sh

# 2. Test proxy shim is working
curl -x http://127.0.0.1:3128 https://repo.maven.apache.org/maven2/ | head

# 3. Test Java directly
java -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
     -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128 \
     -version

# 4. Test Gradle (IMPORTANT: use JAVA_TOOL_OPTIONS)
JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128" \
    ./gradlew help --console=plain --no-daemon

# 5. Test Maven
mvn dependency:resolve
```

## Critical: JAVA_TOOL_OPTIONS

The most reliable way to ensure ALL Java processes use the proxy shim is via `JAVA_TOOL_OPTIONS`:

```bash
export JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128"
```

This ensures the Gradle wrapper, daemon, and all spawned Java processes use the correct proxy settings.

## Known Limitations

1. **Google Cloud Dependencies** - DNS lookups for some Google Cloud artifacts bypass proxy configuration entirely. Workaround: pre-download BOMs using curl.

2. **Session Persistence** - The proxy shim must be restarted each session. Consider adding to a SessionStart hook.

3. **First-time Gradle Wrapper** - Must manually download the Gradle distribution before the wrapper can use it.

## References

- [GitHub Issue #13372 - Maven/Gradle builds fail](https://github.com/anthropics/claude-code/issues/13372)
- [Java 8u111 Basic Auth KB](https://confluence.atlassian.com/kb/basic-authentication-fails-for-outgoing-proxy-in-java-8u111-909643110.html)
- [JDK-8210814 - Cannot use Proxy Authentication with HTTPS](https://bugs.openjdk.org/browse/JDK-8210814)
- [Claude Code Web Bootstrap for Clojure](https://github.com/realgenekim/claude-code-web-bootstrap-clojure-sandbox)
