#!/bin/bash

# Nimbbl WebView SDK - JitPack Publishing Script
# This script publishes the SDK to JitPack with semantic versioning support

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Load environment variables from .env file if it exists
if [ -f ".env" ]; then
    set -a  # automatically export all variables
    source .env
    set +a  # stop automatically exporting
fi

# --------- Configuration ---------
BITBUCKET_USERNAME="$BITBUCKET_USERNAME"
REPO_NAME="$REPO_NAME"
VERSION_TAG=$1                   # e.g. v4.1.0
DRY_RUN=false                    # Set to true for dry run mode
SNAPSHOT_MODE=false              # Set to true for snapshot publishing
REPO_IS_PRIVATE="$REPO_IS_PRIVATE"            # Set to true if your repository is private
JITPACK_TRIGGER_BUILD="$JITPACK_TRIGGER_BUILD"        # Set to true only for private repos or manual triggers
JITPACK_API_TOKEN="$JITPACK_API_TOKEN" # Use env var
VERSION_FILE="version.properties"
# ----------------------------------

# Function to validate required environment variables
validate_environment() {
    local missing_vars=()
    
    if [ -z "$BITBUCKET_USERNAME" ]; then
        missing_vars+=("BITBUCKET_USERNAME")
    fi
    
    if [ -z "$REPO_NAME" ]; then
        missing_vars+=("REPO_NAME")
    fi
    
    if [ -z "$JITPACK_API_TOKEN" ]; then
        missing_vars+=("JITPACK_API_TOKEN")
    fi
    
    if [ ${#missing_vars[@]} -gt 0 ]; then
        print_error "Missing required environment variables:"
        for var in "${missing_vars[@]}"; do
            print_error "  - $var"
        done
        print_info "Please create a .env file with the required variables:"
        print_info "  BITBUCKET_USERNAME=your_bitbucket_username"
        print_info "  REPO_NAME=your_repository_name"
        print_info "  JITPACK_API_TOKEN=your_jitpack_token"
        exit 1
    fi
}

# Function to print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_dry_run() {
    echo -e "${YELLOW}🧪 [DRY RUN] $1${NC}"
}

# Function to get current version from properties
get_current_version() {
    if [ -f "$VERSION_FILE" ]; then
        grep "^SDK_VERSION=" "$VERSION_FILE" | cut -d'=' -f2
    else
        echo "1.0.0"
    fi
}

# Function to generate snapshot version tag
generate_snapshot_tag() {
    local base_version=$1
    local timestamp=$(date +"%Y%m%d-%H%M%S")
    local commit_hash=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    echo "${base_version}-SNAPSHOT-${timestamp}-${commit_hash}"
}

# Function to validate version tag format
validate_version_tag() {
    local tag=$1
    # Allow standard versions (v1.0.0) and snapshot versions (v1.0.0-SNAPSHOT-timestamp-hash)
    if [[ $tag =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT-[0-9]{8}-[0-9]{6}-[a-f0-9]+)?$ ]]; then
        return 0
    else
        return 1
    fi
}

# Function to check if git is available
check_git() {
    if ! command -v git &> /dev/null; then
        print_error "Git is not installed or not in PATH"
        exit 1
    fi
}

# Function to check if we're in a git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a git repository"
        exit 1
    fi
}

# Function to check if tag already exists
check_tag_exists() {
    local tag=$1
    if git tag -l | grep -q "^$tag$"; then
        print_error "Tag $tag already exists"
        print_info "Use 'git tag -d $tag' to delete local tag"
        print_info "Use 'git push origin :refs/tags/$tag' to delete remote tag"
        exit 1
    fi
}

# Function to check if there are uncommitted changes
check_uncommitted_changes() {
    if ! git diff-index --quiet HEAD --; then
        print_warning "There are uncommitted changes"
        print_info "Consider committing changes before publishing"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Publishing cancelled"
            exit 0
        fi
    fi
}

# Function to check git remote
check_git_remote() {
    if ! git remote get-url origin > /dev/null 2>&1; then
        print_error "No 'origin' remote found"
        exit 1
    fi
}

# Function to show help
show_help() {
    echo "Nimbbl WebView SDK - JitPack Publishing Script"
    echo
    echo "Usage: $0 [VERSION_TAG] [OPTIONS]"
    echo
    echo "Arguments:"
    echo "  VERSION_TAG             Version tag to publish (e.g., v4.1.0)"
    echo
    echo "Options:"
    echo "  --current               Use current version from version.properties"
    echo "  --snapshot              Publish snapshot version (for testing)"
    echo "  --dry-run               Simulate publishing without creating tags or pushing"
    echo "  --private               Use for private repositories (requires JitPack access)"
    echo "  --help                  Show this help message"
    echo
    echo "Examples:"
    echo "  $0 v4.1.0              # Publish specific version (public repo)"
    echo "  $0 --current           # Publish current version from properties"
    echo "  $0 --snapshot          # Publish snapshot version for testing"
    echo "  $0 v4.1.0 --dry-run    # Test publishing without actually doing it"
    echo "  $0 v4.1.0 --private    # Publish from private repository"
    echo
    echo "Snapshot Publishing:"
    echo "  --snapshot creates a version like: v4.1.0-SNAPSHOT-20241219-143022-abc1234"
    echo "  Perfect for testing before stable releases"
    echo "  No need to specify version tag - uses current version as base"
    echo
    echo "Repository Types:"
    echo "  Public Repository:"
    echo "    - JitPack automatically builds when you push tags"
    echo "    - No authentication needed"
    echo "    - No API calls needed"
    echo
    echo "  Private Repository:"
    echo "    - Requires JitPack access to your private repo"
    echo "    - Grant access at: https://jitpack.io"
    echo "    - May require manual build triggers"
    echo
    local current_ver=$(get_current_version)
    echo "Current version: $current_ver"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --help)
            show_help
            exit 0
            ;;
        --current)
            VERSION_TAG="v$(get_current_version)"
            print_info "Using current version: $VERSION_TAG"
            shift
            ;;
        --snapshot)
            SNAPSHOT_MODE=true
            current_version=$(get_current_version)
            VERSION_TAG="v$(generate_snapshot_tag "$current_version")"
            print_info "Snapshot mode enabled - using version: $VERSION_TAG"
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            print_dry_run "Dry run mode enabled - no actual changes will be made"
            shift
            ;;
        --private)
            REPO_IS_PRIVATE=true
            JITPACK_TRIGGER_BUILD=true
            print_info "Private repository mode enabled - will trigger manual builds"
            shift
            ;;
        -*)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            if [ -z "$VERSION_TAG" ] || [ "$VERSION_TAG" = "$1" ]; then
                VERSION_TAG="$1"
            else
                print_error "Multiple version tags specified: $VERSION_TAG and $1"
                exit 1
            fi
            shift
            ;;
    esac
done

# Safety check
if [ -z "$VERSION_TAG" ]; then
    print_error "Version tag is required"
    show_help
    exit 1
fi

# Validate version tag format
if ! validate_version_tag "$VERSION_TAG"; then
    print_error "Invalid version tag format. Use vMAJOR.MINOR.PATCH (e.g., v4.1.0)"
    print_info "Current version: $(get_current_version)"
    exit 1
fi

# Step 1: Pre-publish validation
print_info "Validating publish requirements..."

# Validate environment variables
validate_environment

# Check git availability and repository
check_git
check_git_repo
check_git_remote

# Check if version.properties exists
if [ ! -f "$VERSION_FILE" ]; then
    print_error "Version properties file not found: $VERSION_FILE"
    exit 1
fi

# Check if tag already exists
check_tag_exists "$VERSION_TAG"

# Check for uncommitted changes
check_uncommitted_changes

# Check if version matches
current_version=$(get_current_version)
tag_version=${VERSION_TAG#v}  # Remove 'v' prefix

if [ "$current_version" != "$tag_version" ]; then
    print_warning "Version mismatch:"
    print_warning "  Current version: $current_version"
    print_warning "  Tag version: $tag_version"
    print_info "Consider updating version.properties or using --current option"
fi

# Step 2: Commit and Tag
if [ "$DRY_RUN" = true ]; then
    print_dry_run "Would create tag: $VERSION_TAG"
    print_dry_run "Would push tag to origin: $VERSION_TAG"
    print_dry_run "Tag $VERSION_TAG would be created and pushed"
else
    print_info "Tagging release: $VERSION_TAG"

    # Create tag
    if ! git tag "$VERSION_TAG"; then
        print_error "Failed to create tag $VERSION_TAG"
        exit 1
    fi

    # Push tag
    if ! git push origin "$VERSION_TAG"; then
        print_error "Failed to push tag $VERSION_TAG to origin"
        print_info "Tag was created locally but not pushed"
        exit 1
    fi

    print_success "Tag $VERSION_TAG created and pushed"
fi

# Step 3: JitPack Build Information
if [ "$REPO_IS_PRIVATE" = true ]; then
    if [ "$DRY_RUN" = true ]; then
        print_dry_run "Private repository detected - manual build trigger may be required"
        print_dry_run "Monitor build progress at: https://jitpack.io/#$BITBUCKET_USERNAME/$REPO_NAME/$VERSION_TAG"
        print_dry_run "Make sure JitPack has access to your private repository"
    else
        print_info "Private repository detected - manual build trigger may be required"
        print_info "Monitor build progress at: https://jitpack.io/#$BITBUCKET_USERNAME/$REPO_NAME/$VERSION_TAG"
        print_warning "Ensure JitPack has access to your private repository at: https://jitpack.io"
    fi
else
    if [ "$DRY_RUN" = true ]; then
        print_dry_run "JitPack will automatically detect the new tag and start building"
        print_dry_run "Monitor build progress at: https://jitpack.io/#$BITBUCKET_USERNAME/$REPO_NAME/$VERSION_TAG"
    else
        print_info "JitPack will automatically detect the new tag and start building"
        print_info "Monitor build progress at: https://jitpack.io/#$BITBUCKET_USERNAME/$REPO_NAME/$VERSION_TAG"
    fi
fi

# Manual build trigger for private repos or troubleshooting
if [ "$JITPACK_TRIGGER_BUILD" = true ]; then
    if [ "$DRY_RUN" = true ]; then
        if [ "$REPO_IS_PRIVATE" = true ]; then
            print_dry_run "Would manually trigger JitPack build (required for private repos)"
        else
            print_dry_run "Would manually trigger JitPack build (not needed for public repos)"
        fi
        print_dry_run "Would make API call to: https://jitpack.io/api/builds"
    else
        if [ "$REPO_IS_PRIVATE" = true ]; then
            print_info "Manually triggering JitPack build (required for private repos)"
        else
            print_warning "Manually triggering JitPack build (usually not needed for public repos)"
        fi
        
        # Check if curl is available
        if ! command -v curl &> /dev/null; then
            print_warning "curl is not available, skipping manual build trigger"
            if [ "$REPO_IS_PRIVATE" = true ]; then
                print_error "Manual build trigger is required for private repositories"
                print_info "Please install curl or trigger the build manually at: https://jitpack.io"
                exit 1
            else
                print_info "Build will be triggered automatically by JitPack on tag push"
            fi
        else
            # Make API call with error handling
            response=$(curl -s -w "%{http_code}" -X POST "https://jitpack.io/api/builds" \
                -H "Authorization: Bearer $JITPACK_API_TOKEN" \
                -H "Content-Type: application/json" \
                -d "{\"repo\":\"$BITBUCKET_USERNAME/$REPO_NAME\"}")
            
            http_code="${response: -3}"
            body="${response%???}"
            
            if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
                print_success "Manual build trigger successful"
            else
                print_warning "Manual build trigger failed (HTTP $http_code)"
                if [ "$REPO_IS_PRIVATE" = true ]; then
                    print_error "Build trigger failed for private repository"
                    print_info "Please check:"
                    print_info "  1. JitPack has access to your private repository"
                    print_info "  2. Your JitPack API token is valid"
                    print_info "  3. Repository name is correct: $BITBUCKET_USERNAME/$REPO_NAME"
                    exit 1
                else
                    print_info "Build will still be triggered automatically by JitPack"
                fi
                if [ -n "$body" ]; then
                    print_info "Response: $body"
                fi
            fi
        fi
    fi
fi

# Step 4: Show Gradle Dependency
echo
if [ "$DRY_RUN" = true ]; then
    print_dry_run "Publishing simulation completed successfully!"
    echo
    print_dry_run "Would add this to your Gradle dependencies:"
else
    print_success "Publishing completed successfully!"
    echo
    print_info "Add this to your Gradle dependencies:"
fi
echo "----------------------------------------"
echo "repositories {"
echo "    maven { url 'https://jitpack.io' }"
echo "}"
echo
echo "dependencies {"
echo "    implementation 'com.github.$BITBUCKET_USERNAME:$REPO_NAME:$VERSION_TAG'"
echo "}"
echo "----------------------------------------"
echo
print_info "SDK Version: $tag_version"
print_info "JitPack URL: https://jitpack.io/#$BITBUCKET_USERNAME/$REPO_NAME/$VERSION_TAG"

if [ "$SNAPSHOT_MODE" = true ]; then
    echo
    print_info "📸 Snapshot Version Published!"
    print_info "This is a snapshot version for testing purposes"
    print_info "Use this version to test your changes before releasing a stable version"
    print_warning "Snapshot versions are not recommended for production use"
fi

if [ "$DRY_RUN" = true ]; then
    echo
    print_dry_run "This was a dry run - no actual changes were made"
    print_info "Run without --dry-run to actually publish the version"
fi
