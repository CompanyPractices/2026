#!/bin/sh
set -euo pipefail

: "${KMS_URL:?Environment variable KMS_URL is not set}"

if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed." >&2
    exit 1
fi

CLIENT_TYPE=$1

fetch_key() {
    local client_type="$1"
    local response
    response=$(curl -s -X POST "${KMS_URL}/api/internal/keys/issue" \
                      -H "Content-Type: application/json" \
                      -d "{\"clientType\": \"${client_type}\"}")

    local is_successful
    is_successful=$(echo "$response" | jq -r '.isSuccessful')
    if [[ "$is_successful" != "true" ]]; then
        local failure_reason
        failure_reason=$(echo "$response" | jq -r '.failureReason // "unknown"')
        echo "Error: Request failed for clientType '${client_type}': ${failure_reason}" >&2
        exit 1
    fi

    echo "$response" | jq -r '.key.key'
}

export API_KEY=$(fetch_key "$CLIENT_TYPE")

echo "Keys fetched successfully."

exec java -jar ./app.jar