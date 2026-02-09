CREATE or replace TRIGGER audit_task_routes
AFTER UPDATE OF name, config ON task_routes
FOR EACH ROW
EXECUTE FUNCTION generic_audit_trigger('name', 'config');