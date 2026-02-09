@description('Name of the Key Vault')
param keyVaultName string

@description('Name of the X.509 certificate to create')
param certificateName string

@description('Subject name in the X.509 certificate')
param subjectName string

resource keyVault 'Microsoft.KeyVault/vaults@2024-12-01-preview' existing = {
  name: keyVaultName
}

resource certificate 'Microsoft.KeyVault/vaults/certificates@2024-12-01-preview' = {
  name: certificateName
  parent: keyVault
  properties: {
    certificatePolicy: {
      keyProperties: {
        exportable: true
        keyType: 'RSA'
        keySize: 2048
        reuseKey: true
      }
      secretProperties: {
        contentType: 'application/x-pem-file'
      }
      x509CertificateProperties: {
        subject: subjectName
        validityInMonths: 12
        ekus: [
          '1.3.6.1.5.5.7.3.2' // Client Authentication
        ]
        keyUsage: [
          'digitalSignature'
          'keyEncipherment'
        ]
      }
      issuerParameters: {
        name: 'Self'
      }
      attributes: {
        enabled: true
      }
    }
  }
}