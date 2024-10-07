CREATE SEQUENCE batch_id_seq
    INCREMENT 1
    START 1;

CREATE SEQUENCE interaction_id_seq
    INCREMENT 1
    START 1;

CREATE TABLE record_type (
    name VARCHAR(255) PRIMARY KEY
);


CREATE TABLE event_type (
                            ID SERIAL PRIMARY KEY,
                            DESCRIPTION VARCHAR(255)
);

CREATE TABLE case_submission (
                                 id SERIAL PRIMARY KEY,
                                 batch_id INT,
                                 trace_id INT,
                                 maat_id INT,
                                 concor_contribution_id INT,
                                 fdc_id INT,
                                 record_type VARCHAR(255),
                                 processed_date TIMESTAMP,
                                 event_type INT,
                                 http_status INT,
                                 error_message TEXT,
                                 CONSTRAINT fk_event_type
                                     FOREIGN KEY(event_type)
                                         REFERENCES event_type(ID),
                                 CONSTRAINT fk_record_type
                                     FOREIGN KEY(record_type)
                                         REFERENCES record_type(name)
);