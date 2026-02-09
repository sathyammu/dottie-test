param appServiceName string
param currentSettings object = {}

resource appService 'Microsoft.Web/sites@2022-09-01' existing = {
  name: appServiceName
}

resource appSettings 'Microsoft.Web/sites/config@2022-09-01' = {
  parent: appService
  name: 'appsettings'
  properties: union(currentSettings, {
    'PLAYWRIGHT_BROWSERS_PATH': '/pw-browsers'
    'ZIP_FILE': '/extras/docflow-startup-bundle/bundle.zip'
  })
}