ALTER TABLE document_metadata ADD COLUMN page_count int not null default 1;

ALTER TABLE "Document_Extraction" alter column page_number type JSON
using convert_to_jsonb(page_number);

ALTER TABLE "Document_Extraction" rename column page_number TO  split_page_positions;

