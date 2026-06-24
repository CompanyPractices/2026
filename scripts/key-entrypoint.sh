#!/bin/sh
set -euo pipefail

# ----------------------------------------------------------------------
# Usage and required tools
# ----------------------------------------------------------------------
if [ $# -ne 1 ]; then
    echo "Usage: $0 <client-type>" >&2
    exit 1
fi

for cmd in jq curl; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
        echo "Error: $cmd is required but not installed." >&2
        exit 1
    fi
done

CLIENT_TYPE="$1"
CLIENT_ID="${CLIENT_TYPE}-${HOSTNAME}"
FETCHED_KEY=false
JAVA_PID=""

# ----------------------------------------------------------------------
# Key management functions
# ----------------------------------------------------------------------
fetch_key() {
    local client_type="$1"
    local client_id="$2"
    local response
    response=$(curl -s -X POST "${KMS_URL}/api/internal/keys/issue" \
                      -H "Content-Type: application/json" \
                      -d "{\"clientType\": \"${client_type}\", \"clientId\": \"${client_id}\"}")

    local is_successful
    is_successful=$(echo "$response" | jq -r '.isSuccessful')
    if [ "$is_successful" != "true" ]; then
        local failure_reason
        failure_reason=$(echo "$response" | jq -r '.failureReason // "unknown"')
        echo "Error: Request failed for clientType '${client_type}': ${failure_reason}" >&2
        exit 1
    fi

    echo "$response" | jq -r '.key.key'
}

revoke_key() {
    if [ -z "${KMS_URL:-}" ]; then
        return 0
    fi
    curl -s -X POST "${KMS_URL}/api/internal/keys/revoke" \
         -H "Content-Type: application/json" \
         -d "{\"clientId\": \"${CLIENT_ID}\"}" >/dev/null 2>&1
}

# ----------------------------------------------------------------------
# Cleanup on termination
# ----------------------------------------------------------------------
cleanup() {
    # Revoke the key only if we fetched one and KMS_URL is available
    if [ "$FETCHED_KEY" = true ] && [ -n "${KMS_URL:-}" ]; then
        revoke_key
    fi

    # Terminate the Java process if it is still running
    if [ -n "$JAVA_PID" ] && kill -0 "$JAVA_PID" 2>/dev/null; then
        kill -TERM "$JAVA_PID"
        wait "$JAVA_PID"
    fi
    exit 0
}

trap cleanup TERM INT

# ----------------------------------------------------------------------
# Fetch the API key (if KMS_URL is provided)
# ----------------------------------------------------------------------
if [ -z "${KMS_URL:-}" ]; then
    echo "Warning: KMS_URL is not set. The application will start without fetching a key." >&2
else
    API_KEY=$(fetch_key "$CLIENT_TYPE" "$CLIENT_ID")
    export API_KEY
    FETCHED_KEY=true
    echo "Keys fetched successfully."
fi

# ----------------------------------------------------------------------
# Start the Java application
# ----------------------------------------------------------------------
java -jar ./app.jar &
JAVA_PID=$!

wait "$JAVA_PID"
