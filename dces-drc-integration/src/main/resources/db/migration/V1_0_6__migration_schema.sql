CREATE TABLE IF NOT EXISTS case_migration (
                                 maat_id INT PRIMARY KEY,
                                 record_type VARCHAR(255) NOT NULL,
                                 concor_contribution_id INT,
                                 fdc_id INT,
                                 batch_id INT,
                                 is_processed BOOLEAN,
                                 processed_date TIMESTAMP,
                                 http_status INT,
                                 payload TEXT,
                                 CONSTRAINT fk_record_type
                                     FOREIGN KEY(record_type)
                                         REFERENCES record_type(name)
);

comment on column case_migration.maat_id is 'The maat ID related to the entry.';
comment on column case_migration.record_type is 'Simple entry to show if entry is for an FDC, or ConcorContribution';
comment on column case_migration.concor_contribution_id is 'Populated if the entry is for a ConcorContribution';
comment on column case_migration.fdc_id is 'Populated if the entry is for an FDC';
comment on column case_migration.batch_id is 'The id assigned for an entire batch process run. Each id will point to all interactions attempted in a single run of the process. Will not be populated for an async response.';
comment on column case_migration.is_processed is 'True if the fdc/concor has been sent to the DRC';
comment on column case_migration.processed_date is 'Date and time of being sent to the DRC.';
comment on column case_migration.http_status is 'Http Status related to the entry.';
comment on column case_migration.payload is 'Free text fields for entry of payload/json/etc related to the entry.';
