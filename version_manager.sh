#!/bin/bash

# Nimbbl WebView SDK Version Manager
# This script helps manage semantic versioning for the SDK

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VERSION_FILE="version.properties"
CHANGELOG_FILE="CHANGELOG.md"

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

# Function to get current version
get_current_version() {
    if [ -f "$VERSION_FILE" ]; then
        grep "SDK_VERSION=" "$VERSION_FILE" | cut -d'=' -f2
    else
        echo "1.0.0"
    fi
}

# Function to get version components
get_version_components() {
    local version=$1
    IFS='.' read -r major minor patch <<< "$version"
    echo "$major $minor $patch"
}

# Function to update version in properties file
update_version() {
    local new_version=$1
    local version_components=($(get_version_components "$new_version"))
    local major=${version_components[0]}
    local minor=${version_components[1]}
    local patch=${version_components[2]}
    local version_code=$((major * 10000 + minor * 100 + patch))
    local build_date=$(date +%Y-%m-%d)
    
    print_info "Updating version to $new_version..."
    
    # Update version.properties
    sed -i.bak "s/SDK_VERSION=.*/SDK_VERSION=$new_version/" "$VERSION_FILE"
    sed -i.bak "s/VERSION_NAME=.*/VERSION_NAME=$new_version/" "$VERSION_FILE"
    sed -i.bak "s/VERSION_CODE=.*/VERSION_CODE=$version_code/" "$VERSION_FILE"
    sed -i.bak "s/BUILD_DATE=.*/BUILD_DATE=$build_date/" "$VERSION_FILE"
    sed -i.bak "s/MAJOR_VERSION=.*/MAJOR_VERSION=$major/" "$VERSION_FILE"
    sed -i.bak "s/MINOR_VERSION=.*/MINOR_VERSION=$minor/" "$VERSION_FILE"
    sed -i.bak "s/PATCH_VERSION=.*/PATCH_VERSION=$patch/" "$VERSION_FILE"
    
    # Remove backup file
    rm -f "$VERSION_FILE.bak"
    
    print_success "Version updated to $new_version (version code: $version_code)"
}

# Function to bump version
bump_version() {
    local bump_type=$1
    local current_version=$(get_current_version)
    local version_components=($(get_version_components "$current_version"))
    local major=${version_components[0]}
    local minor=${version_components[1]}
    local patch=${version_components[2]}
    
    case $bump_type in
        "major")
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        "minor")
            minor=$((minor + 1))
            patch=0
            ;;
        "patch")
            patch=$((patch + 1))
            ;;
        *)
            print_error "Invalid bump type. Use: major, minor, or patch"
            exit 1
            ;;
    esac
    
    local new_version="$major.$minor.$patch"
    update_version "$new_version"
}

# Function to show current version
show_version() {
    local current_version=$(get_current_version)
    print_info "Current SDK version: $current_version"
    
    if [ -f "$VERSION_FILE" ]; then
        echo
        print_info "Version details:"
        grep -E "^(SDK_VERSION|VERSION_NAME|VERSION_CODE|BUILD_DATE|BUILD_TYPE)=" "$VERSION_FILE" | while read line; do
            echo "  $line"
        done
    fi
}

# Function to validate version format
validate_version() {
    local version=$1
    if [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        return 0
    else
        return 1
    fi
}

# Function to create changelog entry
create_changelog_entry() {
    local version=$1
    local build_date=$(date +%Y-%m-%d)
    
    if [ ! -f "$CHANGELOG_FILE" ]; then
        cat > "$CHANGELOG_FILE" << EOF
# Changelog

All notable changes to the Nimbbl WebView SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Semantic versioning support
- Centralized version management
- Automated version bumping scripts

EOF
    fi
    
    # Add new version entry
    local temp_file=$(mktemp)
    echo "## [$version] - $build_date" > "$temp_file"
    echo "" >> "$temp_file"
    echo "### Added" >> "$temp_file"
    echo "- " >> "$temp_file"
    echo "" >> "$temp_file"
    echo "### Changed" >> "$temp_file"
    echo "- " >> "$temp_file"
    echo "" >> "$temp_file"
    echo "### Fixed" >> "$temp_file"
    echo "- " >> "$temp_file"
    echo "" >> "$temp_file"
    echo "### Removed" >> "$temp_file"
    echo "- " >> "$temp_file"
    echo "" >> "$temp_file"
    
    # Insert after [Unreleased] section
    awk '/## \[Unreleased\]/{print; print ""; while((getline line < "'$temp_file'") > 0) print line; next}1' "$CHANGELOG_FILE" > "$CHANGELOG_FILE.tmp"
    mv "$CHANGELOG_FILE.tmp" "$CHANGELOG_FILE"
    rm -f "$temp_file"
    
    print_success "Changelog entry created for version $version"
}

# Function to show help
show_help() {
    echo "Nimbbl WebView SDK Version Manager"
    echo
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo
    echo "Commands:"
    echo "  show                    Show current version information"
    echo "  bump [TYPE]             Bump version (major|minor|patch)"
    echo "  set [VERSION]           Set specific version (e.g., 4.1.0)"
    echo "  changelog [VERSION]     Create changelog entry for version"
    echo "  build                   Build the project with current version"
    echo "  release [TYPE]          Create release (bump + build + changelog)"
    echo "  help                    Show this help message"
    echo
    echo "Examples:"
    echo "  $0 show                 # Show current version"
    echo "  $0 bump patch           # Bump patch version (1.0.0 -> 1.0.1)"
    echo "  $0 bump minor           # Bump minor version (1.0.0 -> 1.1.0)"
    echo "  $0 bump major           # Bump major version (1.0.0 -> 2.0.0)"
    echo "  $0 set 4.2.0            # Set version to 4.2.0"
    echo "  $0 release patch        # Bump patch, create changelog, and build"
    echo
    echo "Semantic Versioning:"
    echo "  MAJOR: Breaking changes that require code changes"
    echo "  MINOR: New features that are backward compatible"
    echo "  PATCH: Bug fixes that are backward compatible"
}

# Function to build project
build_project() {
    print_info "Building project..."
    ./gradlew clean build
    print_success "Build completed successfully"
}

# Function to create release
create_release() {
    local bump_type=$1
    
    if [ -z "$bump_type" ]; then
        print_error "Release type required (major|minor|patch)"
        exit 1
    fi
    
    print_info "Creating $bump_type release..."
    
    # Bump version
    bump_version "$bump_type"
    
    # Create changelog entry
    local new_version=$(get_current_version)
    create_changelog_entry "$new_version"
    
    # Build project
    build_project
    
    print_success "Release $new_version created successfully!"
    print_info "Next steps:"
    echo "  1. Review and update CHANGELOG.md"
    echo "  2. Commit changes: git add . && git commit -m \"Release v$new_version\""
    echo "  3. Create tag: git tag v$new_version"
    echo "  4. Push: git push origin main --tags"
    echo "  5. Publish: ./publish_to_jitpack.sh v$new_version"
}

# Main script logic
case "${1:-help}" in
    "show")
        show_version
        ;;
    "bump")
        if [ -z "$2" ]; then
            print_error "Bump type required (major|minor|patch)"
            exit 1
        fi
        bump_version "$2"
        ;;
    "set")
        if [ -z "$2" ]; then
            print_error "Version required (e.g., 4.1.0)"
            exit 1
        fi
        if validate_version "$2"; then
            update_version "$2"
        else
            print_error "Invalid version format. Use MAJOR.MINOR.PATCH (e.g., 4.1.0)"
            exit 1
        fi
        ;;
    "changelog")
        local version=${2:-$(get_current_version)}
        create_changelog_entry "$version"
        ;;
    "build")
        build_project
        ;;
    "release")
        create_release "$2"
        ;;
    "help"|*)
        show_help
        ;;
esac
