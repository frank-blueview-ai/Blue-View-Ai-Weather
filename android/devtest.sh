#!/usr/bin/env bash
# Blue View Weather — local build / deploy / evidence harness.
#
# One physical device is shared by everything, so this script is the single
# serialized entry point for touching it. Every step emits evidence to $EVID.
#
#   ./devtest.sh build              compile the debug APK
#   ./devtest.sh install            build, then (re)install on the phone
#   ./devtest.sh launch             cold-start the app
#   ./devtest.sh shot <name>        screenshot -> $EVID/<name>.png
#   ./devtest.sh log <name> [pat]   dump app logcat -> $EVID/<name>.log
#   ./devtest.sh clearlog           wipe logcat ring buffer
#   ./devtest.sh e2e <name>         install + launch + settle + shot + log
set -uo pipefail

export JAVA_HOME=${JAVA_HOME:-/home/fperez/.local/jdk/jdk-17.0.20+8}
export ANDROID_HOME=${ANDROID_HOME:-/home/fperez/Android/Sdk}
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

# Sign local builds with the same release key CI uses, so a locally-built APK
# installs over a CI-built one without an uninstall. Supply these from your
# environment (they are repository secrets in CI) — never hardcode them here.
# Without them Gradle falls back to debug signing, which will NOT install over
# a release-signed build.
: "${KEYSTORE_PASSWORD:?set KEYSTORE_PASSWORD (see repo secrets)}"
: "${KEY_ALIAS:?set KEY_ALIAS}"
: "${KEY_PASSWORD:?set KEY_PASSWORD}"
export KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD

DEV=${DEV:-192.168.1.240:38443}
PKG=ai.blueview.weather
DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
APK=$DIR/app/build/outputs/apk/github/debug/app-github-debug.apk
EVID=${EVID:-$DIR/../.evidence}
ADB="$ANDROID_HOME/platform-tools/adb -s $DEV"

mkdir -p "$EVID"

die() { echo "FAIL: $*" >&2; exit 1; }

case "${1:-}" in
  build)
    cd "$DIR" && ./gradlew assembleGithubDebug --no-daemon -q || die "gradle build failed"
    test -f "$APK" || die "APK not produced"
    echo "OK build $(stat -c%s "$APK") bytes"
    ;;

  install)
    "$0" build || exit 1
    $ADB install -r "$APK" 2>&1 | tee /tmp/.inst.$$
    grep -q Success /tmp/.inst.$$ || die "install failed (see output above)"
    rm -f /tmp/.inst.$$
    echo "OK install"
    ;;

  launch)
    $ADB shell am force-stop $PKG
    $ADB shell am start -n $PKG/.MainActivity >/dev/null 2>&1 \
      || $ADB shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    echo "OK launch"
    ;;

  shot)
    n=${2:?name required}
    $ADB shell screencap -p /sdcard/.s.png || die "screencap failed"
    $ADB pull /sdcard/.s.png "$EVID/$n.png" >/dev/null 2>&1 || die "pull failed"
    $ADB shell rm -f /sdcard/.s.png
    echo "OK shot $EVID/$n.png"
    ;;

  log)
    n=${2:?name required}; pat=${3:-}
    pid=$($ADB shell pidof $PKG 2>/dev/null | tr -d '\r')
    if [ -n "$pid" ]; then
      $ADB logcat -d --pid="$pid" > "$EVID/$n.log" 2>&1
    else
      $ADB logcat -d -t 2000 > "$EVID/$n.log" 2>&1
    fi
    [ -n "$pat" ] && grep -iE "$pat" "$EVID/$n.log" | tail -40
    echo "OK log $EVID/$n.log ($(wc -l < "$EVID/$n.log") lines)"
    ;;

  clearlog)
    $ADB logcat -c; echo "OK clearlog"
    ;;

  e2e)
    n=${2:?name required}
    "$0" install  || exit 1
    "$0" clearlog >/dev/null
    "$0" launch   || exit 1
    sleep 12
    "$0" shot "$n"
    "$0" log  "$n"
    pid=$($ADB shell pidof $PKG 2>/dev/null | tr -d '\r')
    [ -n "$pid" ] || die "app is not running after launch (crashed?)"
    if grep -qE "FATAL EXCEPTION|AndroidRuntime.*$PKG" "$EVID/$n.log"; then
      echo "CRASH detected:"; grep -A 12 "FATAL EXCEPTION" "$EVID/$n.log" | head -25; exit 1
    fi
    echo "OK e2e $n (pid $pid, no crash)"
    ;;

  *) sed -n '2,14p' "$0"; exit 1 ;;
esac
