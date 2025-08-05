#!/bin/bash

# Build Examples for Nimbbl SDK with gradle.properties Configuration

echo "=== Nimbbl SDK Build Examples (gradle.properties) ==="
echo ""

echo "1. Quick Environment Setup:"
echo "   # For Development:"
echo "   cp gradle-dev.properties gradle.properties"
echo "   ./gradlew assembleDebug"
echo ""

echo "   # For Pre-Production:"
echo "   cp gradle-pp.properties gradle.properties"
echo "   ./gradlew assembleRelease"
echo ""

echo "   # For Production:"
echo "   cp gradle-prod.properties gradle.properties"
echo "   ./gradlew assembleRelease"
echo ""

echo "2. Building with Custom Configuration:"
echo "   ./gradlew assembleRelease \\"
echo "     -PEVENT_LOG_URL=\"https://your-custom-endpoint.com/v1/log\" \\"
echo "     -PDEFAULT_TENANT_ID=\"your-custom-tenant-id\""
echo ""

echo "3. Building with Environment Variables:"
echo "   export EVENT_LOG_URL=\"https://your-endpoint.com/v1/log\""
echo "   export DEFAULT_TENANT_ID=\"your-tenant-id\""
echo "   ./gradlew assembleRelease"
echo ""

echo "4. Building Complete SDK Stack:"
echo "   ./gradlew assembleRelease"
echo ""

echo "5. Building Sample App:"
echo "   ./gradlew :nimbbl_mobile_android_sample_app:assembleRelease"
echo ""

echo "=== Environment Configurations ==="
echo "| Environment | Properties File | Event Log URL |"
echo "|-------------|----------------|---------------|"
echo "| Development | gradle-dev.properties | https://eventlogdev.nimbbl.tech/v1/log |"
echo "| Pre-Production | gradle-pp.properties | https://eventlogpp.nimbbl.tech/v1/log |"
echo "| Production | gradle-prod.properties | https://eventlogpipepp.nimbbl.tech/v1/log |"
echo ""

echo "=== Verification ==="
echo "To verify the configuration being used:"
echo "1. Check the generated BuildConfig class"
echo "2. Add logging to see values at runtime"
echo "3. Check build output for configuration details"
echo ""

echo "=== Notes ==="
echo "- All build types now use gradle.properties configuration"
echo "- Easy environment switching via properties files"
echo "- CI/CD friendly with environment variables"
echo "- Backward compatible with existing code" 