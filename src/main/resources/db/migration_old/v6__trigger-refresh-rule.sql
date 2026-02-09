CREATE OR REPLACE FUNCTION refresh_rules_on_rule_entity_create() 
   RETURNS TRIGGER 
   LANGUAGE PLPGSQL
AS $$
DECLARE 
 LOAN_NUM text;
 ExecutionId int;
   
BEGIN

--ln = new_rule_entity.dex.loan_number
--exec_id = select \* from rule_execution where loan_number = ln (limit 1 order by createddate desc)
--trigger task with topic "refresh-rules" and identifier as exec_id;

select loan_number into LOAN_NUM  from "Document_Extraction" de where de.id = NEW.doc_extraction_id;
if FOUND THEN 
 select re.execution_id into  ExecutionId from rule_execution re     where re.loannumber= LOAN_NUM limit 1;

  IF FOUND THEN
    RAISE INFO 'ExecutionId %',ExecutionId;
	INSERT INTO task(topic, identifier, state, descent) 
	  VALUES('EXTRACT_DOC_TYPE', ExecutionId, 1, 3);
 END IF;
END IF;
  

RETURN NEW;
   -- -- trigger logic, this can include an insert trigger
END;
$$

CREATE or replace TRIGGER on_rule_entity_insert 
   AFTER insert 
   ON rule_entity
   FOR each  ROW 
       EXECUTE PROCEDURE refresh_rules_on_rule_entity_create();
       
  