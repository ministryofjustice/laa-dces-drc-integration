CREATE TABLE IF NOT EXISTS case_migration (
                                 maat_id INT,
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
                                         REFERENCES record_type(name),
                                 PRIMARY KEY (maat_id, concor_contribution_id, fdc_id)
);

comment on column case_migration.maat_id is 'The maat ID related to the entry.';
comment on column case_migration.concor_contribution_id is 'Populated if the entry has a ConcorContribution entry that has been sent to the DRC';
comment on column case_migration.fdc_id is 'Populated if the entry has an FDC entry that has been sent to the DRC';
comment on column case_migration.batch_id is 'The id for the batch assigned for the maat id. These will be discrete blocks of maat ids which will be sent serially.';
comment on column case_migration.is_processed is 'True if the fdc/concor has been sent to the DRC';
comment on column case_migration.processed_date is 'Date and time of being sent to the DRC.';
comment on column case_migration.http_status is 'Http Status related to the entry.';
comment on column case_migration.payload is 'Free text fields for entry of payload/json/etc related to the entry.';
