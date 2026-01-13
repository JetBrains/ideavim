#!/bin/bash
# SessionStart hook for Claude Code on web
# Automatically configures Java/Gradle/Maven proxy when running in web environment
#
# Problem: Java/Gradle can't authenticate with Claude Code's JWT-based proxy
# Solution: Local proxy shim that handles auth translation
#
# References:
# - Issue: https://github.com/anthropics/claude-code/issues/13372
# - Solution source: https://github.com/realgenekim/claude-code-web-bootstrap-clojure-sandbox

# Detect Claude Code on web environment
is_claude_code_web() {
    # Check for characteristic environment variables
    [[ -n "$CLAUDE_CODE_PROXY_RESOLVES_HOSTS" ]] && \
    [[ -n "$HTTP_PROXY" ]] && \
    [[ "$HTTP_PROXY" == *"jwt_"* ]]
}

# Exit early if not Claude Code on web
if ! is_claude_code_web; then
    exit 0
fi

echo "🌐 Claude Code on web detected - setting up Java proxy shim..."

# Create proxy shim script
PROXY_SHIM_PATH="$HOME/.local/bin/java-proxy-shim.py"
mkdir -p "$(dirname "$PROXY_SHIM_PATH")"

cat > "$PROXY_SHIM_PATH" << 'PROXY_SCRIPT'
#!/usr/bin/env python3
"""
Local proxy shim for Claude Code web - handles auth translation for Java/Gradle/Maven.

Issue: https://github.com/anthropics/claude-code/issues/13372
Based on: https://github.com/realgenekim/claude-code-web-bootstrap-clojure-sandbox
"""
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
    print(f"Java proxy shim on 127.0.0.1:{LOCAL_PORT}")
    while True:
        cl, _ = srv.accept()
        threading.Thread(target=handle, args=(cl,), daemon=True).start()
PROXY_SCRIPT

chmod +x "$PROXY_SHIM_PATH"

# Kill any existing proxy shim
pkill -f "java-proxy-shim" 2>/dev/null || true
sleep 1

# Start proxy shim in background
nohup python3 "$PROXY_SHIM_PATH" > /tmp/java-proxy-shim.log 2>&1 &
sleep 2

# Verify proxy shim is running
if pgrep -f "java-proxy-shim" > /dev/null; then
    echo "✅ Proxy shim started on 127.0.0.1:3128"
else
    echo "❌ Failed to start proxy shim"
    exit 1
fi

# Configure Gradle
mkdir -p "$HOME/.gradle"
cat > "$HOME/.gradle/gradle.properties" << 'EOF'
# Auto-configured by Claude Code web SessionStart hook
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=3128
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=3128
systemProp.jdk.http.auth.tunneling.disabledSchemes=
systemProp.jdk.http.auth.proxying.disabledSchemes=
EOF
echo "✅ Gradle proxy configured"

# Configure Maven
mkdir -p "$HOME/.m2"
cat > "$HOME/.m2/settings.xml" << 'EOF'
<settings>
  <proxies>
    <proxy><id>https</id><active>true</active><protocol>https</protocol><host>127.0.0.1</host><port>3128</port></proxy>
    <proxy><id>http</id><active>true</active><protocol>http</protocol><host>127.0.0.1</host><port>3128</port></proxy>
  </proxies>
</settings>
EOF
echo "✅ Maven proxy configured"

# Export JAVA_TOOL_OPTIONS (this will be picked up by subprocesses)
export JAVA_TOOL_OPTIONS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128"
echo "✅ JAVA_TOOL_OPTIONS exported"

echo ""
echo "🎉 Java/Gradle/Maven proxy setup complete!"
echo "   Run Gradle with: JAVA_TOOL_OPTIONS=\"\$JAVA_TOOL_OPTIONS\" ./gradlew <task>"
