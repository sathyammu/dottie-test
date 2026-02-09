param appServiceName string
param storageAccountName string
param fileShareName string
param mountPath string
@secure()
param storageAccessKey string

resource appService 'Microsoft.Web/sites@2022-09-01' existing = {
  name: appServiceName
}

resource appServiceStorageMount 'Microsoft.Web/sites/config@2022-09-01' = {
  parent: appService
  name: 'web'
  properties: {
    appCommandLine: '/extras/docflow-startup-bundle/startup.sh'
    azureStorageAccounts: {
      storageAccountName: {
        type: 'AzureFiles'
        accountName: storageAccountName
        shareName: fileShareName
        mountPath: mountPath
        accessKey: storageAccessKey
      }
    }
  }
}

output appServiceResourceId string = appService.id
