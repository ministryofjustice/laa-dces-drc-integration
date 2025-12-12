CREATE SEQUENCE IF NOT EXISTS drc_processing_status_gen_seq
    INCREMENT 1
    START 1;

CREATE TABLE IF NOT EXISTS drc_processing_status
(
    id SERIAL PRIMARY KEY,
    maat_id INT,
    concor_contribution_id INT,
    fdc_id INT,
    status_message VARCHAR(255),
    detail TEXT,
    creation_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
