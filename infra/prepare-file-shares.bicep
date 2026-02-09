param storageAccountName string
param fileShareName string
param storageLocation string
param appServiceName string
param mountPath string
param appServiceResourceGroup string
param storageAccessKey string

var storageAccountId = resourceId('Microsoft.Storage/storageAccounts', storageAccountName)

resource storage 'Microsoft.Storage/storageAccounts@2022-09-01' = {
  name: storageAccountName
  location: storageLocation
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    allowBlobPublicAccess: false
    minimumTlsVersion: 'TLS1_2'
  }
}

resource fileShare 'Microsoft.Storage/storageAccounts/fileServices/shares@2022-09-01' = {
  name: '${storageAccountName}/default/${fileShareName}'
  properties: {
    accessTier: 'TransactionOptimized'
  }
  dependsOn: [storage]
}

module appServiceModule './appServiceModule.bicep' = {
  name: 'attachExtrasFileMountToAppService'
  scope: resourceGroup(appServiceResourceGroup)
  params: {
    appServiceName: appServiceName
    storageAccountName: storageAccountName
    fileShareName: fileShareName
    mountPath: mountPath
    storageAccessKey: storageAccessKey
  }
}

output storageAccount string = storageAccountName
output fileShare string = fileShareName
