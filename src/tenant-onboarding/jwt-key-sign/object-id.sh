SVC_NAME=brimma-vallia-docflow-orchestrator-stage-as
RG=brimma-vallia-docflow-stage-rg

az webapp show \
  --name $SVC_NAME \
  --resource-group  $RG\
  --query identity.principalId \
  -o tsv

  az webapp identity assign \
    --name $SVC_NAME \
    --resource-group $RG