#!/bin/sh
# Nova Client - 26.3 development client (macOS / Linux). Loads straight into "New World" if it exists.
cd "$(dirname "$0")/mod/v26" || exit 1
chmod +x ./gradlew
./gradlew runClient "-PquickPlay=New World" --console=plain
