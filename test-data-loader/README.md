
## Preparing a test run

1. Ask Peter to clear down contributions that were previously sent.  The contribution IDs can be taken from the test data spreadsheet.
2. Clear any contributions in ACTIVE state
   ```sql
     update togdata.CONCOR_CONTRIBUTIONS set STATUS = 'SENT', USER_MODIFIED = 'DCESPhase2', DATE_MODIFIED = sysdate
     where STATUS = 'ACTIVE';
    ```
3. Load error-scenarios-setup test data
   * Connect to the UAT database and ensure these env variables are set in the TestDataLoader run configuration
     * SPRING_DATASOURCE_PASSWORD=<pw>
     * SPRING_DATASOURCE_URL=jdbc:oracle:thin:@//localhost:<port>/maatdb
     * SPRING_DATASOURCE_USERNAME=togdata
   * Update the resource path in TestDataLoader to be `error-scenarios-setup`
   * Run TestDataLoader, this should load one XML file into the DB and set it to active state

4. Check one file is ready to be sent
   ```sql
    select * from TOGDATA.CONCOR_CONTRIBUTIONS where STATUS = 'ACTIVE';
    ```
5. Alter the `DCES_CRON_CONTRIBUTIONS` CRON entry to schedule DCES to send contributions

6. Check the logs to ensure the contribution was sent successfully.

7. Load error-scenarios test data
    * Update the resource path in TestDataLoader to be `error-scenarios`
    * Run TestDataLoader, this should load all files from the directory into the DB and set it to active state

8. Check files is ready to be sent
   ```sql
    select * from TOGDATA.CONCOR_CONTRIBUTIONS where STATUS = 'ACTIVE';
    ```
9. Alter the `DCES_CRON_CONTRIBUTIONS` CRON entry to schedule DCES to send contributions

10. Check the logs to ensure the contribution was sent successfully.

11. Tell Peter to run the processing at his side.

## Check test run results

Once Advantis processing has been done and responses received these steps can be run to check the results

1. Check responses in `drc_processing_status`
   ```sql
    select *
      from drc_processing_status
     where creation_timestamp > 'YYYY-MM-DDTHH:MM:SS'
     order by creation_timestamp;
    ```
2. Generate the report.
   * Login to the report server and generate the report.  To get results for tests run today, you'll need to set tomorrows date in the CURL command
   ```shell
     kubectl -n laa-dces-report-service-uat exec -it <pod> -- sh
     curl http://localhost:8089/api/internal/v1/dces/report/submission-error/TestReport/YYYY-MM-DD
   ```