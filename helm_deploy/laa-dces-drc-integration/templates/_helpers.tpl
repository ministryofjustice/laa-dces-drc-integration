{{/*
Expand the name of the chart.
*/}}
{{- define "laa-dces-drc-integration.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "laa-dces-drc-integration.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "laa-dces-drc-integration.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "laa-dces-drc-integration.labels" -}}
helm.sh/chart: {{ include "laa-dces-drc-integration.chart" . }}
{{ include "laa-dces-drc-integration.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "laa-dces-drc-integration.selectorLabels" -}}
app.kubernetes.io/name: {{ include "laa-dces-drc-integration.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "laa-dces-drc-integration.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "laa-dces-drc-integration.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Inbound IP filtering for the mTLS ingress
*/}}
{{- define "laa-dces-drc-integration.allowlistApi" -}}
{{- if .Values.privateIngress.allowlist.api_include_laai }}
nginx.ingress.kubernetes.io/whitelist-source-range: {{ .Values.privateIngress.allowlist.api | default "127.0.0.1/32" }},{{ .Values.generatedIngress.allowlist.laai | default "127.0.0.1/32" }}
{{- else }}
nginx.ingress.kubernetes.io/whitelist-source-range: {{ .Values.privateIngress.allowlist.api | default "127.0.0.1/32" }}
{{- end }}
{{- end }}

{{/*
Inbound IP filtering for the non-mTLS ingress
*/}}
{{- define "laa-dces-drc-integration.allowlistMon" -}}
{{- if .Values.privateIngress.allowlist.mon_include_laai }}
nginx.ingress.kubernetes.io/whitelist-source-range: {{ .Values.privateIngress.allowlist.mon | default "127.0.0.1/32" }},{{ .Values.generatedIngress.allowlist.monx | default "127.0.0.1/32" }},{{ .Values.generatedIngress.allowlist.laai | default "127.0.0.1/32" }}
{{- else }}
nginx.ingress.kubernetes.io/whitelist-source-range: {{ .Values.privateIngress.allowlist.mon | default "127.0.0.1/32" }},{{ .Values.generatedIngress.allowlist.monx | default "127.0.0.1/32" }}
{{- end }}
{{- end }}
