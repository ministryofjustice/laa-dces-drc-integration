# This should be kept in lockstep with the integrationTest application.yaml

server:
  port: 8089

management:
  server:
    port: 8188
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

sentry:
  debug: off
  dsn: https://73c8b29cf60925dfb51ff991af098c8f@o345774.ingest.sentry.io/4506031610462208
  # With a sample rate of 1, all traffic will be sent to Sentry. Note, this is only possible because this project will have very low traffic (2 calls  month)
  traces-sample-rate: 1.0
  environment: ${HOST_ENV}
  release: 1.0.0
  servername: aws-eks-container
  send-default-pii: true
  attach-stacktrace: true
  # With a sample rate of 1, all traffic will be sent to Sentry. Note, this is only possible because this project will have very low traffic (2 calls  month)
  sample-rate: 1.00
  logging:
    minimum-event-level: warn
    minimum-breadcrumb-level: info
  in-app-includes: uk.gov.justice.laa.crime.dces.integration

logging:
  config: "classpath:logback-json.xml"
  level:
    level: debug
    uk.gov.justice.laa.crime.dces.integration: debug
    org.springframework.web: debug
    org.springframework.security: debug
    org.springframework.http: debug
    org.springframework.security.oauth2: debug
    org.springframework.security.oauth2.client: debug

services:
  maat-api:
    baseUrl: ${MAATCDAPI_BASEURL}
    oAuthEnabled: true
    maxBufferSize: 16
    getContributionBatchSize: ${GET_CONTRIBUTION_BATCH_SIZE:300}
  drc-client-api:
    baseUrl: ${DRCCLIENT_BASEURL}
    oAuthEnabled: ${DRCCLIENT_OAUTH2_ENABLED:false}

scheduling:
  # default is disabled. "-"
  # Example to schedule daily file processing at 8pm Mon to Sat
  # "0 0 20 * * MON-SAT"
  cron:
    process-fdc-files: ${DCES_CRON_FDC:-}
    process-contributions-files: ${DCES_CRON_CONTRIBUTIONS:-}
  lock:
    at-least: 30s
    at-most: 15m

spring:
  security:
    oauth2:
      client:
        provider:
          maatapi:
            token-uri: ${MAATCDAPI_OAUTH2_TOKENURI}
          drc-client-api:
            token-uri: ${DRCCLIENT_OAUTH2_TOKENURI}
        registration:
          maatapi:
            client-id: ${MAATCDAPI_OAUTH2_CLIENTID}
            client-secret: ${MAATCDAPI_OAUTH2_CLIENTSECRET}
            authorization-grant-type: client_credentials
            scope:
              - ${MAATCDAPI_OAUTH2_SCOPE}
          drc-client-api:
            client-id: ${DRCCLIENT_OAUTH2_CLIENTID}
            client-secret: ${DRCCLIENT_OAUTH2_CLIENTSECRET}
            authorization-grant-type: client_credentials
            scope:
              - ${DRCCLIENT_OAUTH2_SCOPE}
      resource-server:
        jwt:
          issuer-uri: ${LAA_DCES_DRC_INTEGRATION_RESOURCE_SERVER_ISSUER_URI}

  datasource:
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    show-sql: false
    generate-ddl: false
  flyway:
    baselineOnMigrate: true
    enabled: true
  web:
    cors:
      mappings:
        "/**":
          allowed-origins:
            {{- range .Values.ingress.mon.hosts }}
            - "https://{{ . }}"
            {{- end }}
          allowed-methods:
            - GET
            - POST
            - PUT
            - DELETE
          allowed-headers: "*"
          allow-credentials: true
springdoc:
  swagger-ui:
    path: /swagger-ui-custom.html
    enabled: true
    operations-sorter: method
    api-url: https://${API_URL}
    oauth-token-url: ${DCES_DRC_API_OAUTH_TOKEN_URL}
  api-docs:
    path: "/api-docs"
    enabled: true
  writer-with-order-by-keys: true
  writer-with-default-pretty-printer: true
  packages-to-scan: uk.gov.justice.laa.crime.dces.integration.controller

api:
  title: DCES DRC Integration RestAPIs
  description: DCES DRC Integration Services.
  version: 1.0
  contactName: DCES Support Team
  contactEmail: laa-dces@digital.justice.gov.uk
  contactUrl: justice.gov.uk/laa

resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 2s
        retry-exceptions:
          - org.springframework.web.reactive.function.client.WebClientRequestException
          - org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
          - org.springframework.web.reactive.function.client.WebClientResponseException.ServiceUnavailable
          - org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
          - org.springframework.web.reactive.function.client.WebClientResponseException.NotImplemented
          - org.springframework.web.reactive.function.client.WebClientResponseException.GatewayTimeout

    instances:
      FdcService:
        base-config: default
      ContributionService:
        base-config: default
