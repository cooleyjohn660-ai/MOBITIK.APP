#!/usr/bin/env bash
set -eo pipefail

echo "========================================================"
echo "  MOBITIK Automatic Release AAB & APK Builder"
echo "========================================================"

# Find project root containing settings.gradle.kts
if [ -f "settings.gradle.kts" ]; then
  ROOT_DIR="$(pwd)"
else
  FOUND_SETTINGS="$(find . -maxdepth 3 -name "settings.gradle.kts" | head -n 1)"
  if [ -n "$FOUND_SETTINGS" ]; then
    ROOT_DIR="$(cd "$(dirname "$FOUND_SETTINGS")" && pwd)"
  else
    echo "ERROR: Could not find settings.gradle.kts in the current directory."
    exit 1
  fi
fi

cd "$ROOT_DIR"
echo "Project directory: $ROOT_DIR"

# 1. Ensure Java 21 is active if available
if ! java -version 2>&1 | grep -q "version \"21"; then
  if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    export PATH="$JAVA_HOME/bin:$PATH"
  elif command -v sdk >/dev/null 2>&1 || [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh" || true
    sdk install java 21.0.6-tem < /dev/null || true
    sdk use java 21.0.6-tem || true
  fi
fi

# 2. Install Gradle 9.3.1 directly (required by AGP 9.1.1)
if ! command -v gradle >/dev/null 2>&1 || ! gradle --version | grep -q "9.3.1"; then
  echo "[1/5] Downloading and installing Gradle 9.3.1..."
  curl -sSL https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -o /tmp/gradle-9.3.1-bin.zip
  mkdir -p "$HOME/.gradle-dist"
  unzip -q -o /tmp/gradle-9.3.1-bin.zip -d "$HOME/.gradle-dist"
  rm -f /tmp/gradle-9.3.1-bin.zip
  export PATH="$HOME/.gradle-dist/gradle-9.3.1/bin:$PATH"
else
  echo "[1/5] Gradle 9.3.1 is already installed."
fi

# 3. Install Android SDK & API 36.1 (required by compileSdk 36.1)
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

# 4. Restore debug.keystore from debug.keystore.base64 if missing
if [ ! -f "$ROOT_DIR/debug.keystore" ] && [ -f "$ROOT_DIR/debug.keystore.base64" ]; then
  echo "[3/5] Restoring debug.keystore from debug.keystore.base64..."
  base64 -d "$ROOT_DIR/debug.keystore.base64" > "$ROOT_DIR/debug.keystore"
elif [ ! -f "$ROOT_DIR/debug.keystore" ]; then
  echo "[3/5] Generating fallback debug.keystore..."
  keytool -genkeypair -v \
    -keystore "$ROOT_DIR/debug.keystore" \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass android \
    -keypass android \
    -dname "CN=Android Debug,O=Android,C=US"
fi

# 5. Generate or load Release Upload Keystore (my-upload-key.jks)
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

# 6. Build Signed Release AAB (.aab) and Release APK (.apk)
echo "[5/5] Building Signed Release AAB Bundle and APK..."
gradle :app:bundleRelease :app:assembleRelease --no-daemon

# 7. Copy outputs to easy-to-find top-level folder
mkdir -p "$ROOT_DIR/RELEASE_FILES"
cp -f "$ROOT_DIR/app/build/outputs/bundle/release/app-release.aab" "$ROOT_DIR/RELEASE_FILES/app-release.aab"
cp -f "$ROOT_DIR/app/build/outputs/apk/release/app-release.apk" "$ROOT_DIR/RELEASE_FILES/app-release.apk"
cp -f "$KEYSTORE_PATH" "$ROOT_DIR/RELEASE_FILES/my-upload-key.jks"

echo ""
echo "========================================================"
echo "  BUILD SUCCEEDED!"
echo "========================================================"
echo "All files have been copied to the RELEASE_FILES/ folder:"
echo "  1. RELEASE_FILES/app-release.aab   (Upload to Google Play Console)"
echo "  2. RELEASE_FILES/app-release.apk   (Install directly on Android)"
echo "  3. RELEASE_FILES/my-upload-key.jks (Save safely for future updates)"
echo ""
echo "Certificate Fingerprints (Add SHA-1 & SHA-256 to Firebase Console):"
keytool -list -v -keystore "$KEYSTORE_PATH" -alias upload -storepass "$STORE_PASSWORD" | grep -E "SHA1:|SHA256:" || true
echo "========================================================"
