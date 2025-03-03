{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for service containers
*/}}
{{- define "laa-dces-drc-integration-int-test.env-vars" }}
env:
  - name: AWS_REGION
    value: {{ .Values.aws_region }}
  - name: HOST_ENV
    value: {{ .Values.java.host_env }}
{{- end -}}
