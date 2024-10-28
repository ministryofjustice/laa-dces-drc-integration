INSERT INTO record_type VALUES ('Contribution'), ('Fdc') ON CONFLICT DO NOTHING;
INSERT INTO event_type (description) VALUES ('FetchedFomMAAT'),('SyncRequestResponseToDrc'),('SyncResponseLoggedToMAAT'),('AsyncRequestResponseFromDrc') ON CONFLICT DO NOTHING;

