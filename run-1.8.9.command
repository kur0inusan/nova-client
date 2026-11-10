#!/bin/sh
# Nova Client - 1.8.9 development client (macOS / Linux)
cd "$(dirname "$0")/mod/v1_8_9" || exit 1
chmod +x ./gradlew
./gradlew runClient --console=plain
