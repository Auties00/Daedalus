#!/usr/bin/env bash
#
# Builds the Daedalus CLI, together with every datatype extension, as a single self-contained native executable.
#
# The result is the executable itself, dist/daedalus-<label>-<os>-<arch>[.exe]: all intermediate output is deleted.
#
# Usage: daedalus-cli/scripts/build-cli.sh [--label LABEL] [--out DIR] [-- MAVEN_ARGS...]
#   --label LABEL  version label used in the file name (default: the Maven project version)
#   --out DIR      output directory (default: dist)
#
# Needs two JDKs, because the sources use preview APIs of the final JDK 26 that the available GraalVM builds do not have yet:
#   JAVA_HOME      a JDK 26 that compiles the project with Maven (the java on the PATH is used if it is not set)
#   GRAALVM_HOME   a GraalVM based on JDK 26 that only runs native-image

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

NAME="daedalus"
MAIN_CLASS="DaedalusMain"
DEPENDENCY_PLUGIN="org.apache.maven.plugins:maven-dependency-plugin:3.8.1"

LABEL=""
OUT="dist"
MVN_ARGS=()

fail() {
    echo "error: $*" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --label) LABEL="${2:?--label needs a value}"; shift ;;
        --out) OUT="${2:?--out needs a value}"; shift ;;
        --) shift; MVN_ARGS=("$@"); break ;;
        -h|--help) sed -n '2,/^$/p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) fail "unknown option $1" ;;
    esac
    shift
done

# Platform
case "$(uname -s)" in
    Linux*) OS="linux"; CP_SEP=":"; EXE="" ;;
    Darwin*) OS="macos"; CP_SEP=":"; EXE="" ;;
    MINGW*|MSYS*|CYGWIN*) OS="windows"; CP_SEP=";"; EXE=".exe" ;;
    *) fail "unsupported operating system $(uname -s)" ;;
esac
case "$(uname -m)" in
    x86_64|amd64) ARCH="x64" ;;
    arm64|aarch64) ARCH="arm64" ;;
    *) ARCH="$(uname -m)" ;;
esac

# JDKs
if [[ -z "${JAVA_HOME:-}" ]]; then
    command -v java >/dev/null || fail "set JAVA_HOME to a JDK 26 or put it on the PATH"
    JAVA_HOME="$(cd "$(dirname "$(command -v java)")/.." && pwd)"
fi
export JAVA_HOME
JAVA_MAJOR="$("$JAVA_HOME/bin/java" -version 2>&1 | sed -n '1s/[^0-9]*\([0-9]*\).*/\1/p')"
[[ "$JAVA_MAJOR" -ge 26 ]] || fail "JDK 26 or newer is required in JAVA_HOME, found $JAVA_MAJOR at $JAVA_HOME"
[[ -n "${GRAALVM_HOME:-}" ]] || fail "set GRAALVM_HOME to a GraalVM based on JDK 26"
# native-image.cmd breaks on paths with spaces, so Windows gets the short form of the path
if [[ "$OS" == "windows" ]]; then
    GRAALVM_HOME="$(cygpath -m "$(cygpath -d "$GRAALVM_HOME")")"
fi
NATIVE_IMAGE=""
for candidate in "$GRAALVM_HOME/bin/native-image" "$GRAALVM_HOME/bin/native-image.cmd" "$GRAALVM_HOME/bin/native-image.exe"; do
    if [[ -f "$candidate" ]]; then NATIVE_IMAGE="$candidate"; break; fi
done
[[ -n "$NATIVE_IMAGE" ]] || fail "native-image not found in $GRAALVM_HOME/bin"
command -v mvn >/dev/null || fail "Maven is not on the PATH"

# Modules whose jars ship with the CLI: the framework plus every extension
EXTENSIONS=()
while IFS= read -r pom; do
    EXTENSIONS+=("$(dirname "${pom#./}")")
done < <(find daedalus-datatype -name pom.xml -path '*-cli/pom.xml' -not -path '*/target/*' | sort)
[[ ${#EXTENSIONS[@]} -gt 0 ]] || fail "no extension module (*-cli) found under daedalus-datatype"
MODULES=(daedalus-cli "${EXTENSIONS[@]}")
MODULE_LIST="$(IFS=,; echo "${MODULES[*]}")"
echo "Extensions: ${EXTENSIONS[*]}"

# Relative to the root, because native-image.cmd splits arguments at the spaces of absolute paths on Windows
STAGE="target/cli"
LIB="$STAGE/lib"
trap 'rm -rf "$ROOT/$STAGE"' EXIT

VERSION="$(mvn -q -N -B help:evaluate -Dexpression=project.version -DforceStdout)"
[[ -n "$LABEL" ]] || LABEL="$VERSION"

mvn -B clean install -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true "${MVN_ARGS[@]}"
mkdir -p "$LIB"
# All runtime dependencies of the shipped modules on a single flat directory, next to the jars of the modules themselves
mvn -B -pl "$MODULE_LIST" "$DEPENDENCY_PLUGIN:copy-dependencies" \
    -DincludeScope=runtime -DoutputDirectory="$ROOT/$LIB" "${MVN_ARGS[@]}"
for module in "${MODULES[@]}"; do
    jar="$module/target/$(basename "$module")-$VERSION.jar"
    [[ -f "$jar" ]] || fail "$module produced no jar named $(basename "$jar")"
    cp "$jar" "$LIB/"
done

JARS=()
while IFS= read -r jar; do JARS+=("$jar"); done < <(find "$LIB" -maxdepth 1 -name '*.jar' | sort)
CLASSPATH="$(IFS="$CP_SEP"; echo "${JARS[*]}")"

echo "Building the native executable"
# The modules ship their own reflection and resource metadata in META-INF/native-image, which native-image picks up from the jars
mkdir -p "$STAGE/image"
"$NATIVE_IMAGE" --enable-preview --enable-native-access=ALL-UNNAMED --no-fallback \
    -H:+ReportExceptionStackTraces \
    -cp "$CLASSPATH" "$MAIN_CLASS" -o "$STAGE/image/$NAME"

mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
TARGET="$OUT/$NAME-$LABEL-$OS-$ARCH$EXE"
# A previous executable can stay locked for a moment after it exited, for example while it is scanned by an antivirus
for attempt in 1 2 3 4 5 6 7 8 9 10; do
    if cp "$STAGE/image/$NAME$EXE" "$TARGET" 2>/dev/null; then
        break
    fi
    [[ "$attempt" -lt 10 ]] || fail "$TARGET is locked: close the running executable and build again"
    sleep 2
done

echo "Done: $TARGET"
