#!/bin/bash

#INSTRUCTIONS - READ THIS BEFORE RUNNING THE FILE
#Needs tenant_name in the argument. Warning: duplicate tenants by name can be created when we run this file twice.
# to do basic cleanup of tenant, use the following queries

#delete from tenant_settings ts where ts.tenant_id =  (select id from "Tenant" where tenant_name='apple');
#delete from "Tenant" where "Tenant".tenant_name = 'apple'

source ../../.env;

TENANT_NAME=$1
psql -c "INSERT INTO public.\"Tenant\" \
    (sor_id, doc_classifier_id, tenant_name, is_active, doc_classifier_metadata, \
    sor_metadata, created_date, last_updated_date, archived_time, is_archived, \
    created_by, last_updated_by, pipeline_request, connection_status, rule_strategy, email)\
VALUES\
  (1, 3, '$TENANT_NAME', true, NULL,\
  '', \
  '2024-10-03 15:43:15.789', '2024-10-03 15:43:15.789', NULL, false, 'admin', 'admin', '', \
  NULL, NULL, NULL)"

TENANT_ID=`psql -AXqt \
               -c "select id from \"Tenant\" where tenant_name =  '$TENANT_NAME';"`

echo $TENANT_ID

ROUTING_CONFIG=$(cat <<EOF
  [
    {
      "eventType": "invoke_api",
      "action": {
        "type": "task_via_routing",
        "actionArgs": {
          "routeName": "EXTRACT_UPLOADED_DOCS",
          "checksumBehaviour": "run_always"
        }
      }
    }
  ]
EOF
)


ROUTING_QUERY=$(cat <<EOF
insert into tenant_settings (tenant_id, category, meta, strategy)
values ($TENANT_ID, 'Task', '$ROUTING_CONFIG', 'task_route');
EOF
)

psql -AXqt -c "$ROUTING_QUERY";

NEW_API_KEY=`curl http://localhost:8081`

API_KEY=`curl -X POST  http://localhost:8081/v3/auth/keys/regenerate \
-H "X-API-KEY: MAGIC-3030303" \
    -H "X-TENANT-ID: $TENANT_ID" | jq .data`

echo $API_KEY, $TENANT_ID

