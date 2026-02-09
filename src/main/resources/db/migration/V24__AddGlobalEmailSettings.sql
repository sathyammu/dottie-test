INSERT INTO tenant_settings (tenant_id, category, strategy, meta)
SELECT
    id,
    'email',
    'rx',
    '{
       "topicRecipients": {
         "PACKAGE_SPLIT": {
           "to": ["Sathya.Selvi@brimmatech.com"],
           "cc": [],
           "bcc": []
         },
         "EXCEPTION_NOTIFICATION": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_FEE_UPDATE": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_INVALID_PASSWORD": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_PROGRESS_NOTIFICATION": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "USNAT_SITE_NOT_REACHABLE": {
           "to": [],
           "cc": [],
           "bcc": []
         },
         "BRIMMA_GLOBAL_SUPPORT": {
           "to": ["saro@brimmatech.com", "gokulp@brimmatech.com"],
           "cc": [""],
           "bcc": [""]
         }
       }
     }'
FROM "Tenant"
WHERE tenant_name = '__vdx__';
