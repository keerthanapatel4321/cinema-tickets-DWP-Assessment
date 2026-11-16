#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mapfile -d '' sources < <(find "$ROOT/src/main/java" -type f -name '*.java' -print0)
javac --release 21 -d "$TMP" "${sources[@]}" "$ROOT/scripts/SmokeCheck.java"
java -cp "$TMP" SmokeCheck
