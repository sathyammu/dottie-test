INSERT INTO public.task_routes
("name", config)
VALUES('CLASSIFY_VIA_LLM', '[{"topic": "UPLOAD_DOCUMENT_FOR_CX_EX"}, {"topic": "EXTRACT_DOC_TYPE"}, {"topic": "PARSE_PAGINATION"},
{"topic": "GROUP_PAGES"},
{"topic": "END_RUN"}]'::jsonb);
