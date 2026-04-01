#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

PROTOBUF_TAG="v34.0"
PROTOBUF_DIR="$(mktemp -d)"

cleanup() {
    echo "Cleaning up $PROTOBUF_DIR..."
    rm -rf "$PROTOBUF_DIR"
}
trap cleanup EXIT

echo "Cloning protobuf $PROTOBUF_TAG to $PROTOBUF_DIR..."
git clone --depth 1 --branch "$PROTOBUF_TAG" https://github.com/protocolbuffers/protobuf.git "$PROTOBUF_DIR"

rm -rf build
mkdir build
cd build

echo "Configuring CMake..."
if command -v ninja &> /dev/null; then
    GENERATOR="-G Ninja"
else
    GENERATOR=""
fi

cmake .. $GENERATOR -DCMAKE_BUILD_TYPE=Release -DPROTOBUF_SRC_DIR="$PROTOBUF_DIR"

echo "Building protoc_bridge..."
cmake --build . --config Release

OUTPUT_DIR="$SCRIPT_DIR/../src/test/resources/native"
mkdir -p "$OUTPUT_DIR"

LIB_FILE=$(find . -name "libprotoc_bridge.so" -o -name "libprotoc_bridge.dylib" -o -name "protoc_bridge.dll" | head -1)
if [ -n "$LIB_FILE" ]; then
    cp "$LIB_FILE" "$OUTPUT_DIR/"
    echo "Build complete: $OUTPUT_DIR/$(basename "$LIB_FILE")"
else
    echo "Build failed: library not found"
    exit 1
fi
