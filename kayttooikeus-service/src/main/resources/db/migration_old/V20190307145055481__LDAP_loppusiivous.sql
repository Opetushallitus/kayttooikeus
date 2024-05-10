DROP TABLE ldap_synchronization_data;
DROP TABLE ldap_update_data;

DELETE FROM scheduled_tasks WHERE task_name='ldap synkronointi task';