
select * from document_model dm ;

select regexp_replace('vallia_utility_escrow_agreement_v1.0.0', '(.*)_v\d+.\d+.\d+', '\1'); 

alter table document_model add constraint unique_name_constraint unique(document_type);

select * from 


UPDATE document_model 
SET model_id = new_models.model_id
from new_models  where document_model.model_id like regexp_replace(new_models.model_id, '(.*)_v\d+.\d+.\d+', '\1') || '%';

 not in (

select * from new_models where regexp_replace(model_id, '(.*)_v\d+.\d+.\d+', '\1') not in (
	select regexp_replace(model_id, '(.*)_v\d+.\d+.\d+', '\1')  from document_model 
)

--To find duplicate matches. 
select * from document_model dm where dm.model_id in (
	select model_id from document_model dm group by model_id having count(*)>1
) 



insert into document_model (document_type, model_id) 
select regexp_replace(model_id, '(.*)_v\d+.\d+.\d+', '\1')  as document_type, model_id from new_models where regexp_replace(model_id, '(.*)_v\d+.\d+.\d+', '\1') not in (
	select regexp_replace(model_id, '(.*)_v\d+.\d+.\d+', '\1')  from document_model 
)
 
)
SET model_id = new_models.model_id
from new_models  where document_model.model_id like regexp_replace(new_models.model_id, '(.*)_v\d+.\d+.\d+', '\1') || '%';



CREATE TEMP TABLE new_models (model_id text);

insert into new_models (model_id) values 
('vallia_utility_escrow_agreement_v1.0.0'),
('vallia_affidavit_v1.0.0'),
('vallia_real_estate_tax_assessment_agreement_v1.0.0'),
('vallia_affidavit_of_occupancy_v1.0.0'),
('vallia_signature_name_affidavit_v1.0.0'),
('vallia_instructions_to_escrow_title_closing_agent_v1.0.0'),
('vallia_usa_patriot_act_information_form_v1.0.1'),
('vallia_tax_record_information_sheet_v1.0.0'),
('vallia_first_payment_letter_v1.0.0'),
('vallia_deed_v1.0.0'),
('vallia_deed_of_trust_v1.0.0'),
('vallia_compliance_agreement_v1.0.0'),
('vallia_closing_instructions_v1.0.0'),
('vallia_closing_protection_letter_v1.16'),
('vallia_title_insurance_commitment_v1.20.0'),
('vallia_purchase_conditions_v1.7.0'),
('vallia_settlement_statement_v1.2.0'),
('vallia_hazard_insurance_binder_v1.21.0'),
('vallia_certificate_of_occupancy_v1.0.0'),
('vallia_property_sales_contract_v1.0.1'),
('vallia_request_for_title_commitment_v1.0.1'),
('vallia_hud_addendum_to_uniform_residential_application_v1.0.1'),
('vallia_form_90_verbal_verification_of_employment_v1.0.0'),
('vallia_hud_addendum_to_uniform_residential_application_v1.0.0'),
('vallia_request_for_verification_of_employment_v1.0.0')
;