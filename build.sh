#!/usr/bin/env bash
#
# build.sh — compiles the mod and copies the jar into the project root.
# Auto-generates the Gradle wrapper if it's missing.
#
set -e

cd "$(dirname "$0")"

# --- Ensure the Gradle wrapper exists ---
if [ ! -f ./gradlew ]; then
    echo "⚙️   Gradle wrapper not found. Generating it..."

    GRADLE_VERSION="9.5.1"

    if command -v gradle >/dev/null 2>&1; then
        gradle wrapper --gradle-version "$GRADLE_VERSION"
    else
        echo "   System gradle not found — downloading wrapper files directly..."

        mkdir -p gradle/wrapper

        cat > gradle/wrapper/gradle-wrapper.properties <<EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

        curl -fsSL -o gradle/wrapper/gradle-wrapper.jar \
            "https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/${GRADLE_VERSION}/gradle-wrapper-${GRADLE_VERSION}.jar"

        curl -fsSL -o gradlew \
            "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradlew"
    fi
fi

if [ ! -x ./gradlew ]; then
    chmod +x ./gradlew
fi

# --- Build ---
echo "🔨  Building MacroMod..."
./gradlew clean build

# --- Copy the jar to the project root ---
JAR=""
for f in build/libs/*.jar; do
    case "$f" in
        *-sources.jar) ;;
        *) JAR="$f"; break ;;
    esac
done

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
    echo "❌  No jar found in build/libs/"
    exit 1
fi

JAR_NAME=$(basename "$JAR")
cp "$JAR" "./$JAR_NAME"

echo ""
echo "✅  Build complete!"
echo "    → ./$JAR_NAME"
echo "    Drop it into .minecraft/mods/ alongside Fabric API."