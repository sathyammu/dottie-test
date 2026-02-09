#!/bin/bash
set -xe
set -o pipefail  # Catch failures in pipelines

# Needs $SRC_ROOT, $SHARE_NAME
# Configuration

export UPLOAD_DIR="./to_be_uploaded"

# Ensure Azure CLI is logged in and has access
echo "[downloads.sh] Verifying Azure login..."
az account show > /dev/null

# Clean and create structure
echo "[downloads.sh] Setting up staging directory..."
rm -rf "$UPLOAD_DIR"

mkdir -p $UPLOAD_DIR





# Download and copy startup.sh
echo "[downloads.sh] Creating startup.sh..."
cat <<'EOF' > "$UPLOAD_DIR/startup.sh"
#!/bin/sh
set -xe
cp /extras/docflow-startup-bundle/sshd_config /etc/ssh/
service ssh start

ls -al /pw-browsers
ls -al /extras
exec java -DPLAYWRIGHT_BROWSERS_PATH=/pw-browsers  -jar /extras/docflow-startup-bundle/docflow-rule-engine-1.0.0.jar
EOF

chmod +x "$UPLOAD_DIR/startup.sh"

echo "[downloads.sh] Showing generated startup.sh"
cat "$UPLOAD_DIR/startup.sh"

# Upload everything to the share
# Copy the JAR file to the upload directory
echo "[downloads.sh] Copying JAR file to upload directory..."
cp "$SRC_ROOT/target/docflow-rule-engine-1.0.0.jar" $UPLOAD_DIR
cp "$SRC_ROOT/src/deploy/ssh/sshd_config" $UPLOAD_DIR

# Upload both the zip file and startup.sh to the share
echo "[downloads.sh] Uploading files to Azure File Share..."
az storage file upload-batch \
  --account-name "$STORAGE_ACCOUNT_NAME" \
  --destination "$SHARE_NAME" \
  --source "$UPLOAD_DIR" \
  --destination-path "/docflow-startup-bundle"

echo "[downloads.sh] Done."
