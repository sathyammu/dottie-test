#--Commands 

## To Initialize a target database with bare minimum data

# Source 

pg_dump  -h brimma-vallia-stage-psql.postgres.database.azure.com -U btadmin -p 5432 \ 
    --schema-only \ 
    -x vallia_docflow_dev \
    -f dev_ddl.sql \
    -c 

pg_dump  -h brimma-vallia-stage-psql.postgres.database.azure.com \
-U btadmin \
-p 5432 \
--data-only \
-x vallia_docflow_dev \
-f data.sql \
-t document_model  \
-t '"Doc_Classifiers"'  \
-t '"System_Of_Records"'  \
-t '"Tenant"' \
-t tenant_settings \
-t tenant_rule  

#Target

psql -h 172.19.0.2 -U postgres -d vallia_docflow_dev -f dev_ddl.sql -abe -L out.log > output.log 
psql -h 172.19.0.2 -U postgres -d vallia_docflow_dev -f data.sql -abe -L import.log > import_shell.log