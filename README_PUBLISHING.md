## Nimbbl WebView SDK – Versioning and Publishing

This guide explains how to bump the SDK version and publish to **Maven Central** (Central Portal) or **JitPack** for the WebView SDK.

---

## Maven Central (Central Portal)

WebView SDK publishes to the same Central Portal as Core API SDK. Use the same `.env` (or `centralPortalUsername` / `centralPortalPassword` or `ossrhTokenUsername` / `ossrhTokenSecret`) and signing keys.

### Prerequisites
- **Core API SDK** must be published first to Maven Central (e.g. `tech.nimbbl:core-api-sdk:4.0.11`). WebView SDK depends on it.
- `.env` in this project (or Core API SDK project if copying) with:
  - `centralPortalUsername` / `centralPortalPassword` (or `ossrhTokenUsername` / `ossrhTokenSecret`)
  - `signing.keyId`, `signing.password`, `signing.secretKeyRingFile`
- Namespace `tech.nimbbl` verified at [central.sonatype.com](https://central.sonatype.com).

### Publish command
```bash
./gradlew publishReleasePublicationToSonatypeRepository --no-daemon
```

### After upload
1. **Create deployment** via manual API (same as Core API SDK):
   - `GET` `https://ossrh-staging-api.central.sonatype.com/manual/search/repositories` with `Authorization: Bearer <base64(username:password)>` → get repo `key`.
   - `POST` `https://ossrh-staging-api.central.sonatype.com/manual/upload/repository/<key>`.
2. Open [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) → click **Publish** on the deployment.

See the Core API SDK **PUBLISHING.md** for full details.

---

## JitPack

### Prerequisites
- Clean working tree on the intended branch (snapshot allowed on any branch if your script permits it).
- `version.properties` is present and updated.
- Local Java/Gradle setup.
- WebView SDK depends on Core API SDK. Ensure a working version is available (local project dependency or a published tag).

### Files
- `version_manager.sh`: Manage `version.properties` and version bumps.
- `publish_to_jitpack.sh`: Creates and pushes tags; JitPack builds artifacts from those tags.

### Environment
Export the following variables in your shell/session:

```bash
export GITHUB_USERNAME=nimbbl-tech
export REPO_NAME=nimbbl_mobile_kit_android_webview_sdk
# Optional for private/manual triggers only
export JITPACK_API_TOKEN=your_jitpack_token
```

### Bump Version
Use the version manager to bump as needed:

```bash
./version_manager.sh show
./version_manager.sh bump patch   # 1.0.0 -> 1.0.1
./version_manager.sh bump minor   # 1.0.0 -> 1.1.0
./version_manager.sh bump major   # 1.0.0 -> 2.0.0
./version_manager.sh set 4.1.0    # set exact
```

Commit the change:

```bash
git add version.properties
git commit -m "chore(version): bump to $(grep '^SDK_VERSION=' version.properties | cut -d'=' -f2)"
```

### Publish a Snapshot (recommended first)

```bash
./publish_to_jitpack.sh --snapshot
```

You will get a tag like `vX.Y.Z-SNAPSHOT-YYYYMMDD-HHMMSS-<hash>` and a Gradle line:

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies {
    implementation 'com.github.nimbbl-tech:nimbbl-checkout-sdk:vX.Y.Z-SNAPSHOT-YYYYMMDD-HHMMSS-<hash>'
}
```

### Publish a Stable Release

```bash
# Use the current version from version.properties
./publish_to_jitpack.sh --current

# Or specify explicitly
./publish_to_jitpack.sh vX.Y.Z
```

Gradle usage for stable:

```groovy
repositories { maven { url 'https://jitpack.io' } }
dependencies {
    implementation 'com.github.nimbbl-tech:nimbbl-checkout-sdk:vX.Y.Z'
}
```

### Notes
- WebView SDK’s artifactId is `nimbbl-checkout-sdk` (set in the module POM). The publish script prints this in the dependency example.
- Ensure Core API SDK dependency in `build.gradle.kts` points to a valid version (local project or published tag) before tagging.
- Verify release builds locally (`./gradlew clean assembleRelease`) before publishing.

