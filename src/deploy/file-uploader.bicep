param storageAccountName string
param fileShareName string
param files array // Each: { fileName: 'foo.sh', targetPath: '/mounts/common/scripts/foo.sh' }
param blobBaseUri string // e.g., https://<storage>.blob.core.windows.net/scripts/
param blobSasToken string = '' // Optional SAS token for private blobs

var storageAccountId = resourceId('Microsoft.Storage/storageAccounts', storageAccountName)

resource uploadScript 'Microsoft.Resources/deploymentScripts@2020-10-01' = {
  name: 'uploadFilesScript'
  location: resourceGroup().location
  kind: 'AzureCLI'
  properties: {
    azCliVersion: '2.38.0'
    timeout: 'PT10M'
    cleanupPreference: 'Always'
    forceUpdateTag: utcNow()
    retentionInterval: 'P1D'
    scriptContent: '''
      #!/bin/bash
      set -e
      accountName=${storageAccountName}
      shareName=${fileShareName}
      mkdir -p tmpfiles
      echo "Uploading files to share: $shareName"

      az storage share create --account-name "$accountName" --name "$shareName" --quota 1

      # Download and upload files
      ''' + join(files, '\n', file => {
        var localPath = 'tmpfiles/' + file.fileName
        var relativePath = replace(file.targetPath, '/mounts/common/', '')
        var blobUri = '${blobBaseUri}' + file.fileName + '${empty(blobSasToken) ? '' : '?' + blobSasToken}'
        '''(
          echo "Processing: ''' + file.fileName + '''"
          if curl --head --silent --fail ''' + blobUri + ''' > /dev/null; then
            curl -sSL ''' + blobUri + ''' -o ''' + localPath + '''
            chmod +x ''' + localPath + '''
            az storage file upload \
              --account-name "$accountName" \
              --share-name "$shareName" \
              --source ''' + localPath + ''' \
              --path ''' + relativePath + ''' \
              --overwrite true
          else
            echo "ERROR: Blob not found for ''' + blobUri + '''" >&2
            exit 1
          fi
        )'''
      }) + '''
    '''
    environmentVariables: [
      {
        name: 'storageAccountName'
        value: storageAccountName
      }
      {
        name: 'fileShareName'
        value: fileShareName
      }
    ]
    supportingScriptUris: []
    storageAccountSettings: {
      storageAccountName: storageAccountName
    }
  }
  dependsOn: []
}