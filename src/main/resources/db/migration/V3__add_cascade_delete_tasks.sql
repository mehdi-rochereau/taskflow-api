ALTER TABLE tasks DROP FOREIGN KEY fk_tasks_project;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE;