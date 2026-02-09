#!/bin/bash

# Variables
SOURCE_KEYVAULT_NAME="docflow-local-saro" # "kv-brimma-dev"
TARGET_KEYVAULT_NAME="docflow-local-saro-s2"
#RESOURCE_GROUP="rg-brimma-dev"
#LOCATION="westus2" # Location for the new Key Vault

## Step 1: Create the target Key Vault
#echo "Creating target Key Vault: $TARGET_KEYVAULT_NAME"
#az keyvault create --name "$TARGET_KEYVAULT_NAME" --resource-group "$RESOURCE_GROUP" --location "$LOCATION"

# Step 2: List all secrets in the source Key Vault
#echo "Fetching secrets from source Key Vault: $SOURCE_KEYVAULT_NAME"
SECRETS=$(az keyvault secret list --vault-name "$SOURCE_KEYVAULT_NAME" --query "[].id" -o tsv)
#
#echo $SECRETS

## Step 3: Loop through each secret and copy it to the target Key Vault
for SECRET_ID in $SECRETS; do
    SECRET_NAME=$(basename "$SECRET_ID")
    SECRET_VALUE=$(az keyvault secret show --id "$SECRET_ID" --query "value" -o tsv)
    echo $SECRET_VALUE
#
#    echo "Cloning secret: $SECRET_NAME"
    az keyvault secret set --vault-name "$TARGET_KEYVAULT_NAME" --name "$SECRET_NAME" --value "$SECRET_VALUE"
done

echo "Key Vault cloning completed successfully!"