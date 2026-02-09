@description('Name of the Key Vault to create')
param keyVaultName string

// @description('Location for the Key Vault')
// param location string = resourceGroup().location

@description('Name of the RSA key inside the vault')
param keyName string = 'jwt-signing-key'

@description('The principal (user/app/managed identity) Object ID to assign RBAC role to')
param principalObjectId string

resource keyVault 'Microsoft.KeyVault/vaults@2023-02-01' existing = {
  name: keyVaultName

//   location: location
//   properties: {
//     tenantId: subscription().tenantId
//     sku: {
//       name: 'standard'
//       family: 'A'
//     }
//     enableRbacAuthorization: true
//     enabledForDeployment: true
//     enabledForTemplateDeployment: true
//     enabledForDiskEncryption: false
//   }
}

resource rsaKey 'Microsoft.KeyVault/vaults/keys@2023-02-01' = {
  parent: keyVault
  name: keyName
  properties: {
    kty: 'RSA'
    keySize: 2048
    keyOps: [
      'sign'
      'verify'
    ]
  }
}

// Assign RBAC role: Key Vault Crypto User to the principal at key scope
resource roleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(rsaKey.id, 'keyvault-crypto-user', principalObjectId)
  scope: rsaKey
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '14b46e9e-c2b7-41b4-b07b-48a6ebf60603') // Key Vault Crypto User
    principalId: principalObjectId
    principalType: 'ServicePrincipal'
  }
}