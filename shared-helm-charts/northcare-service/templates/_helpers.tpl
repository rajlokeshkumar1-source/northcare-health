{{/*
Expand the name of the chart.
*/}}
{{- define "northcare.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a fully qualified app name, capped at 63 chars.
Truncation strips trailing dashes to keep DNS labels valid.
*/}}
{{- define "northcare.fullname" -}}
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
Create chart label value: <name>-<version>
*/}}
{{- define "northcare.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Standard labels applied to every resource.
*/}}
{{- define "northcare.labels" -}}
helm.sh/chart: {{ include "northcare.chart" . }}
{{ include "northcare.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: northcare-health-platform
{{- with .Values.commonLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Selector labels — used in matchLabels and podTemplateSpec.labels.
Must be stable (not change between upgrades).
*/}}
{{- define "northcare.selectorLabels" -}}
app.kubernetes.io/name: {{ include "northcare.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Resolve the ServiceAccount name.
If serviceAccount.create=true and no name override is given, use the fullname.
*/}}
{{- define "northcare.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "northcare.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
