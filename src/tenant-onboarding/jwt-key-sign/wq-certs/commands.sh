RG=brimma-vallia-saas-dev-rg
KV_NAME=kv-brimma-dev
CERT_NAME="brimma-wq-stage-cert"
SUBJ="CN=brimmatech.com"


az keyvault certificate create \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --policy "$(az keyvault certificate get-default-policy \
               | jq '.x509CertificateProperties.subject="CN=brimmatech.com"' \
               | jq '.keyProperties.keySize=2048' \
               | jq '.keyProperties.keyType="RSA"' \
               | jq '.secretProperties.contentType="application/x-pem-file"')"


CERT_NAME="brimma-wq-non-prod-cert"
az keyvault certificate download \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --encoding PEM \
  --file wq-certs/$CERT_NAME.pem