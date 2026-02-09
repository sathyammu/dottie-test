alter table task_routes add constraint unique_task_route_name UNIQUE (name);

INSERT INTO public.task_routes
("name", config)
VALUES('EXTRACT_UPLOADED_DOCS_USING_CULMINATE', '[{"topic": "UPLOAD_DOCUMENT_FOR_CX_EX", "creationMeta": {"priority": 100}}, {"topic": "CLASSIFY_PDF", "creationMeta": {"priority": 100}, "notificationTopic": "after_classify"}, {"topic": "SPLIT_PDF"}, {"topic": "EXTRACT_DOC_TYPE", "creationMeta": {"priority": 100, "strategy": "CONTENT_UNDERSTANDING", "pollStrategy": "REST_API"}, "notificationTopic": "after_extract"}, {"topic": "END_RUN"}]'::jsonb) on conflict do nothing;