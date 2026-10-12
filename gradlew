#!/usr/bin/env bash
#
# Gradle start up script for POSIX generated for RoadSense Vision.
#
# The previous version of this script exec'd itself (infinite recursion) and
# looked for gradle-wrapper.properties in the wrong directory, so the wrapper
# jar was never invoked. This version runs org.gradle.wrapper.GradleWrapperMain
# from gradle/wrapper/gradle-wrapper.jar, exactly like the stock Gradle wrapper.

set -e
set -o nounset
set -o pipefail

APP_HOME=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)

# AGP 8.7.x requires JDK 17+. Prefer an explicit JAVA_HOME only when it is a
# supported major version (17-24); Gradle 8.14 cannot run on JDK 25, which is
# the Homebrew default `openjdk` on this machine.
pick_java_home() {
    local candidates=()
    [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
    candidates+=(
        "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
        "$HOME/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home"
        "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
    )
    local ch v major
    for ch in "${candidates[@]}"; do
        [ -x "$ch/bin/java" ] || continue
        v=$("$ch/bin/java" -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}')
        case "$ch" in *jdk-17*) v=17 ;; esac
        major=${v:-0}
        if [ "$major" -ge 17 ] 2>/dev/null && [ "$major" -le 24 ] 2>/dev/null; then
            echo "$ch"
            return 0
        fi
    done
    return 1
}

if JAVA_HOME_RESOLVED=$(pick_java_home); then
    export JAVA_HOME="$JAVA_HOME_RESOLVED"
else
    echo "ERROR: no JDK between 17 and 24 found" >&2
    exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
    echo "ERROR: gradle-wrapper.jar missing at $CLASSPATH" >&2
    exit 1
fi

DEFAULT_JVM_OPTS='"-Xmx4g" "-Dfile.encoding=UTF-8"'

# shellcheck disable=SC2086
eval set -- \
    ${DEFAULT_JVM_OPTS:-} \
    ${JAVA_OPTS:-} \
    ${GRADLE_OPTS:-} \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"

exec "$JAVA_HOME/bin/java" "$@"
