
TRUNCATE drc_processing_status;
ALTER TABLE drc_processing_status ADD COLUMN ack_response_status INTEGER NOT NULL;

ALTER TABLE drc_processing_status ALTER COLUMN maat_id SET NOT NULL;
ALTER TABLE drc_processing_status ALTER COLUMN status_message SET NOT NULL;
ALTER TABLE drc_processing_status ALTER COLUMN creation_timestamp SET NOT NULL;
ALTER TABLE drc_processing_status ALTER COLUMN drc_processing_timestamp SET NOT NULL;

ALTER TABLE drc_processing_status ADD CONSTRAINT chk_status_message_not_whitespace CHECK (status_message !~ '^[\s]+$');
