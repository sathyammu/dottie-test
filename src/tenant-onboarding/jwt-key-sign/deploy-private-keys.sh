RG=brimma-vallia-saas-dev-rg
OBJECT_ID=c14b7ba8-a321-4a12-88ec-18b8329857bb
az deployment group create \
  --resource-group $RG \
  --template-file provision-keys.bicep \
  --parameters principalObjectId=$OBJECT_ID \
  --parameters keyVaultName=kv-brimma-dev \
  --parameters keyName=jwt-signing-key-wilqo-stage

#
#  az keyvault key show \
#    --vault-name kv-brimma-dev \
#    --name jwt-signing-key \
#    --query key.n -o tsv
#
#    az keyvault key show \
#        --vault-name kv-brimma-dev \
#        --name jwt-signing-key \
#        --query key.e -o tsv