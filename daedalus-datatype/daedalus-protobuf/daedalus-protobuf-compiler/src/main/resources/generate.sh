#!/bin/bash
set -euo pipefail

REPO_URL="https://github.com/protocolbuffers/protobuf.git"
PROTOBUF_TAG="v34.0"
CLONE_DIR="$(mktemp -d)"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

cleanup() {
    echo "Cleaning up $CLONE_DIR..."
    rm -rf "$CLONE_DIR"
}
trap cleanup EXIT

proto_files=(
    "src/google/protobuf/any.proto:google/protobuf/any.proto"
    "src/google/protobuf/api.proto:google/protobuf/api.proto"
    "src/google/protobuf/compiler/plugin.proto:google/protobuf/compiler/plugin.proto"
    "src/google/protobuf/cpp_features.proto:google/protobuf/cpp_features.proto"
    "src/google/protobuf/descriptor.proto:google/protobuf/descriptor.proto"
    "src/google/protobuf/duration.proto:google/protobuf/duration.proto"
    "src/google/protobuf/empty.proto:google/protobuf/empty.proto"
    "src/google/protobuf/field_mask.proto:google/protobuf/field_mask.proto"
    "src/google/protobuf/source_context.proto:google/protobuf/source_context.proto"
    "src/google/protobuf/struct.proto:google/protobuf/struct.proto"
    "src/google/protobuf/timestamp.proto:google/protobuf/timestamp.proto"
    "src/google/protobuf/type.proto:google/protobuf/type.proto"
    "src/google/protobuf/wrappers.proto:google/protobuf/wrappers.proto"
    "java/core/src/main/resources/google/protobuf/java_features.proto:google/protobuf/java_features.proto"
    "java/core/src/main/resources/google/protobuf/java_mutable_features.proto:google/protobuf/java_mutable_features.proto"
)

echo "Deleting old output..."
rm -rf "$SCRIPT_DIR/google"

echo "Cloning protobuf $PROTOBUF_TAG..."
git clone --depth 1 --branch "$PROTOBUF_TAG" "$REPO_URL" "$CLONE_DIR" 2>&1 | tail -1

echo "Copying proto files..."
copied=0
missing=0
for entry in "${proto_files[@]}"; do
    IFS=':' read -r src dest <<< "$entry"
    src_full="$CLONE_DIR/$src"
    dest_full="$SCRIPT_DIR/$dest"
    if [ -f "$src_full" ]; then
        mkdir -p "$(dirname "$dest_full")"
        cp "$src_full" "$dest_full"
        ((copied++))
    else
        echo "  MISSING: $src"
        ((missing++))
    fi
done

echo ""
if [ $missing -gt 0 ]; then
    echo "ERROR: $missing files missing!"
    exit 1
else
    echo "SUCCESS: $copied files copied to google/protobuf/"
fi