{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for service containers
*/}}
{{- define "laa-dces-drc-integration.env-vars" }}
env:
  - name: AWS_REGION
    value: {{ .Values.aws_region }}
  - name: SENTRY_DSN
    value: {{ .Values.sentry_dsn }}
  - name: HOST_ENV
    value: {{ .Values.java.host_env }}
  - name: DRC_API_BASE_URL
    value: {{.Values.drc.baseUrl }}
  - name: MAAT_API_BASE_URL
    value: {{ .Values.maatApi.baseUrl }}
  - name: MAAT_API_REGISTRATION_ID
    value: {{ .Values.maatApi.registrationId }}
  - name: MAAT_API_OAUTH_URL
    value: {{ .Values.maatApi.oauthUrl }}
  - name: MAAT_API_OAUTH_CLIENT_ID
    valueFrom:
        secretKeyRef:
            name: maat-api-oauth-client-id
            key: MAAT_API_OAUTH_CLIENT_ID
  - name: MAAT_API_OAUTH_CLIENT_SECRET
    valueFrom:
        secretKeyRef:
            name: maat-api-oauth-client-secret
            key: MAAT_API_OAUTH_CLIENT_SECRET
  - name: MAAT_API_OAUTH_SCOPE
    value: {{ .Values.maatApi.oauthScope }}
  - name: LAA_DCES_DRC_INTEGRATION_RESOURCE_SERVER_ISSUER_URI
    value: {{ .Values.resource_server }}
  - name: CLIENT_AUTH_CERTIFICATE
    value: {{ .Values.clientAuth.mountPath }}/tls.crt
  - name: CLIENT_AUTH_PRIVATE_KEY
    value: {{ .Values.clientAuth.mountPath }}/tls.key
  - name: CLIENT_AUTH_PRIVATE_KEY_PASSWORD
    valueFrom:
        secretKeyRef:
            name: {{ .Values.clientAuth.secretName }}
            key: private-key-password
{{- end -}}