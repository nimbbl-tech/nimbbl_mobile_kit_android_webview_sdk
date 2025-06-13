#!/bin/bash

# --------- Configuration ---------
GITHUB_USERNAME="tools-nimbbl"
REPO_NAME="nimbbl_mobile_kit_android_webview_sdk"
VERSION_TAG=$1                   # e.g. v1.0.0
JITPACK_TRIGGER_BUILD=true         # Set to false if auto-trigger is sufficient
JITPACK_API_TOKEN="jp_admn30arbgunrf2ab92qlk59a1" # Only needed for private repo builds
# ----------------------------------

# Safety check
if [ -z "$VERSION_TAG" ]; then
  echo "❌ Usage: ./publish_to_jitpack.sh $1"
  exit 1
fi

# Step 1: Commit and Tag
echo "🔧 Tagging release: $VERSION_TAG"
git tag "$VERSION_TAG"
git push origin "$VERSION_TAG"

# Step 2: Optional JitPack Build Trigger
if [ "$JITPACK_TRIGGER_BUILD" = true ]; then
  echo "🚀 Triggering JitPack build for $GITHUB_USERNAME/$REPO_NAME..."

  curl -s -X POST "https://jitpack.io/api/builds" \
    -H "Authorization: Bearer $JITPACK_API_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"repo\":\"$GITHUB_USERNAME/$REPO_NAME\"}"

  echo "🧪 Build triggered. Monitor at: https://jitpack.io/#$GITHUB_USERNAME/$REPO_NAME/$VERSION_TAG"
else
  echo "📦 Build will be triggered automatically by JitPack on tag push."
fi

# Step 3: Show Gradle Dependency
echo
echo "📌 Add this to your Gradle dependencies:"
echo "----------------------------------------"
echo "repositories { maven { url 'https://jitpack.io' } }"
echo "dependencies { implementation 'com.github.$GITHUB_USERNAME:$REPO_NAME:$VERSION_TAG' }"
echo "----------------------------------------"
