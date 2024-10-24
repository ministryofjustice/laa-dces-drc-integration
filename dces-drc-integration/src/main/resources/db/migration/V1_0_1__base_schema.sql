CREATE SEQUENCE IF NOT EXISTS batch_id_seq
    INCREMENT 1
    START 1;

COMMENT ON SEQUENCE batch_id_seq is 'Used for populating batch id used in case_submission';

CREATE SEQUENCE IF NOT EXISTS trace_id_seq
    INCREMENT 1
    START 1;

COMMENT ON SEQUENCE trace_id_seq is 'Used for populating trace id used in case_submission';

CREATE TABLE IF NOT EXISTS record_type (
    name VARCHAR(255) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS event_type (
                            id SERIAL PRIMARY KEY,
                            description VARCHAR(255) UNIQUE
);

CREATE TABLE IF NOT EXISTS case_submission (
                                 id SERIAL PRIMARY KEY,
                                 batch_id INT,
                                 trace_id INT,
                                 maat_id INT,
                                 concor_contribution_id INT,
                                 fdc_id INT,
                                 record_type VARCHAR(255) NOT NULL,
                                 processed_date TIMESTAMP,
                                 event_type INT NOT NULL,
                                 http_status INT,
                                 payload TEXT,
                                 CONSTRAINT fk_event_type
                                     FOREIGN KEY(event_type)
                                         REFERENCES event_type(id),
                                 CONSTRAINT fk_record_type
                                     FOREIGN KEY(record_type)
                                         REFERENCES record_type(name)
);

comment on column case_submission.batch_id is 'The id assigned for an entire batch process run. Each id will point to all interactions attempted in a single run of the process. Will not be populated for an async response.';
comment on column case_submission.trace_id is 'TBD if dropped or recycled into showing the micrometer trace id'; --TODO: Decision
comment on column case_submission.maat_id is 'The maat ID related to the entry.';
comment on column case_submission.concor_contribution_id is 'Populated if the entry is for a ConcorContribution';
comment on column case_submission.fdc_id is 'Populated if the entry is for an FDC';
comment on column case_submission.record_type is 'Simple entry to show if entry is for an FDC, or ConcorContribution';
comment on column case_submission.processed_date is 'Date and time of processing.';
comment on column case_submission.event_type is 'Type of event, which part of the process we are in.';
comment on column case_submission.http_status is 'Http Status related to the entry, if applicable.';
comment on column case_submission.payload is 'Free text fields for entry of payload/json/etc related to the entry.';
