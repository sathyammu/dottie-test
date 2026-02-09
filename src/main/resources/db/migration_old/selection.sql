select de.id, de.document_type, de.loan_number from document_extraction_v2 v 
inner join "Document_Extraction" de 
on v.doc_extraction_id = de.id;


select jsonb_path_query(v.extracted_json ::jsonb, 
'strict $.** ?  (@.text == "Section 6: Acknowledgements and Agreements.")') from 
document_extraction_v2 v where v.doc_extraction_id = 38;

gives pageNo: 6


select jsonb_path_query(v.extracted_json ::jsonb, 
'strict $.** ?  (@.prov[0].page_no == 6 && @.text like_regex "Borr.*Sign") ') from 
document_extraction_v2 v where v.doc_extraction_id = 38;

select count(*) from 
document_extraction_v2 v where v.doc_extraction_id = 38;