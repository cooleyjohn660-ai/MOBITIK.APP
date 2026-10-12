#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo "  MOBITIK Automatic Release AAB & APK Builder"
echo "========================================================"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# 1. Ensure Java 21 & Gradle 9.3.1 (required by AGP 9.1.1) are installed
if ! command -v gradle >/dev/null 2>&1 || ! gradle --version | grep -q "9.3.1"; then
  echo "[1/5] Installing JDK 21 & Gradle 9.3.1 via SDKMAN..."
  if [ ! -d "$HOME/.sdkman" ]; then
    curl -s "https://get.sdkman.io" | bash
  fi
  # shellcheck disable=SC1091
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk install java 21.0.6-tem < /dev/null || true
  sdk use java 21.0.6-tem || true
  sdk install gradle 9.3.1 < /dev/null || true
  sdk use gradle 9.3.1 || true
else
  echo "[1/5] Gradle $(gradle --version | awk '/Gradle /{print $2}') is already installed."
fi

# 2. Ensure Android SDK & API 36.1 (required by compileSdk 36.1) are installed
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

if [ ! -f "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "[2/5] Installing Android SDK Command-Line Tools..."
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  curl -sSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdtools.zip
  unzip -q -o /tmp/cmdtools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools"
  rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm -f /tmp/cmdtools.zip
fi

echo "[2/5] Accepting Android SDK licenses & installing platform 36.1..."
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-36" "platforms;android-36.1" "build-tools;36.0.0"

# 3. Restore debug.keystore from debug.keystore.base64 if missing
if [ ! -f "$ROOT_DIR/debug.keystore" ] && [ -f "$ROOT_DIR/debug.keystore.base64" ]; then
  echo "[3/5] Restoring debug.keystore from debug.keystore.base64..."
  base64 -d "$ROOT_DIR/debug.keystore.base64" > "$ROOT_DIR/debug.keystore"
fi

# 4. Generate or load Release Upload Keystore (my-upload-key.jks)
export KEYSTORE_PATH="${KEYSTORE_PATH:-$ROOT_DIR/my-upload-key.jks}"
export STORE_PASSWORD="${STORE_PASSWORD:-MobitikRelease2026!}"
export KEY_PASSWORD="${KEY_PASSWORD:-$STORE_PASSWORD}"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "[4/5] Generating Release Upload Keystore at $KEYSTORE_PATH..."
  keytool -genkeypair -v \
    -keystore "$KEYSTORE_PATH" \
    -alias upload \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=MOBITIK, OU=Mobile, O=Mobitik, L=City, S=State, C=US"
else
  echo "[4/5] Using existing Release Upload Keystore at $KEYSTORE_PATH"
fi

# 5. Build Signed Release AAB (.aab) and Release APK (.apk)
echo "[5/5] Building Signed Release AAB Bundle and APK..."
gradle :app:bundleRelease :app:assembleRelease --no-daemon

echo ""
echo "========================================================"
echo "  BUILD SUCCEEDED!"
echo "========================================================"
echo "Google Play Console AAB Bundle:"
echo "  app/build/outputs/bundle/release/app-release.aab"
echo ""
echo "Installable Release APK:"
echo "  app/build/outputs/apk/release/app-release.apk"
echo ""
echo "Upload Keystore (save this file safely for future Play Store updates):"
echo "  $KEYSTORE_PATH"
echo "  Password: $STORE_PASSWORD | Alias: upload"
echo ""
echo "Certificate Fingerprints (Add SHA-1 & SHA-256 to Firebase Console):"
keytool -list -v -keystore "$KEYSTORE_PATH" -alias upload -storepass "$STORE_PASSWORD" | grep -E "SHA1:|SHA256:" || true
echo "========================================================"
