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
  - name: LAA_DCES_DRC_INTEGRATION_RESOURCE_SERVER_ISSUER_URI
    value: {{ .Values.resource_server }}
  - name: DRCCLIENT_KEYSTORE_CERTIFICATE
    value: {{ .Values.drcClient.mountPath }}/tls.crt
  - name: DRCCLIENT_KEYSTORE_PRIVATEKEY
    value: {{ .Values.drcClient.mountPath }}/tls.key
  - name: API_URL
    value: {{ index .Values.ingress.api.hosts 0 }}
{{- end -}}
