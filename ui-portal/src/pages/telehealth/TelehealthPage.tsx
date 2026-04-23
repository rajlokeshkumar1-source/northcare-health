import { useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, Grid, InputLabel, MenuItem, Select, Snackbar,
  Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import CheckIcon from '@mui/icons-material/Check'
import CancelIcon from '@mui/icons-material/Cancel'
import ShowChartIcon from '@mui/icons-material/ShowChart'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip as ReTooltip,
  ResponsiveContainer,
} from 'recharts'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader'
import { hospitalApi, telehealthApi } from '@/api/apiClient'

// ── Types ─────────────────────────────────────────────────────────────────────

type ConsultationStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'
type ConsultationType   = 'VIDEO' | 'AUDIO' | 'CHAT'
type DeviceType         = 'HEART_RATE_MONITOR' | 'GLUCOMETER' | 'BLOOD_PRESSURE' | 'PULSE_OXIMETER' | 'THERMOMETER'

interface ConsultationResponse {
  id: string; patientId: string; doctorId: string; doctorName: string
  scheduledAt: string; actualStartAt: string | null; actualEndAt: string | null
  status: ConsultationStatus; consultationType: ConsultationType
  chiefComplaint: string; meetingUrl: string | null; durationMinutes: number | null
}
interface MonitoringResponse {
  id: string; patientId: string; deviceId: string; deviceType: DeviceType
  metricName: string; value: number; unit: string; isAlert: boolean
  alertThresholdMin: number | null; alertThresholdMax: number | null; recordedAt: string
}
interface MonitoringPage { content: MonitoringResponse[]; totalElements: number }
interface PatientBrief { id: string; firstName: string; lastName: string }
interface PatientPage { content: PatientBrief[] }

const DOCTORS = [
  { id: '10000000-0000-0000-0000-000000000001', name: 'Dr. Sarah Chen' },
  { id: '10000000-0000-0000-0000-000000000002', name: 'Dr. James Wilson' },
  { id: '10000000-0000-0000-0000-000000000003', name: 'Dr. Maria Rodriguez' },
  { id: '10000000-0000-0000-0000-000000000004', name: 'Dr. Robert Kim' },
]

const TYPE_COLORS: Record<ConsultationType, 'primary' | 'secondary' | 'info'> = {
  VIDEO: 'primary', AUDIO: 'secondary', CHAT: 'info',
}
const STATUS_COLORS: Record<ConsultationStatus, 'warning' | 'success' | 'default' | 'error' | 'info'> = {
  SCHEDULED: 'warning', IN_PROGRESS: 'info', COMPLETED: 'success',
  CANCELLED: 'error', NO_SHOW: 'error',
}

// ── Schedule Consultation Dialog ───────────────────────────────────────────────

interface ConsultForm {
  patientId: string; doctorId: string
  scheduledAt: string; consultationType: ConsultationType | ''
  chiefComplaint: string
}
const EMPTY_CONSULT: ConsultForm = { patientId: '', doctorId: '', scheduledAt: '', consultationType: '', chiefComplaint: '' }

function ScheduleConsultationDialog({ open, onClose, onSaved }: {
  open: boolean; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<ConsultForm>(EMPTY_CONSULT)
  const [err, setErr] = useState<string | null>(null)

  const { data: patientPage } = useQuery<PatientPage>({
    queryKey: ['patients-brief'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=100').then(r => r.data),
    enabled: open,
  })
  const patients = patientPage?.content ?? []

  const set = (f: keyof ConsultForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const selectedDoctor = DOCTORS.find(d => d.id === form.doctorId)

  const mutation = useMutation({
    mutationFn: (f: ConsultForm) => telehealthApi.post('/api/v1/consultations', {
      patientId: f.patientId,
      doctorId: f.doctorId,
      doctorName: selectedDoctor?.name ?? 'Unknown Doctor',
      scheduledAt: f.scheduledAt,
      consultationType: f.consultationType,
      chiefComplaint: f.chiefComplaint,
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_CONSULT); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? e.response?.data?.message ?? 'Failed to schedule'),
  })

  const submit = () => {
    if (!form.patientId || !form.doctorId || !form.scheduledAt || !form.consultationType || !form.chiefComplaint) {
      setErr('All fields are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Schedule Consultation</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Patient *</InputLabel>
              <Select label="Patient *" value={form.patientId}
                onChange={e => setForm(p => ({ ...p, patientId: e.target.value }))}>
                {patients.map(pt => (
                  <MenuItem key={pt.id} value={pt.id}>{pt.firstName} {pt.lastName}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Doctor *</InputLabel>
              <Select label="Doctor *" value={form.doctorId}
                onChange={e => setForm(p => ({ ...p, doctorId: e.target.value }))}>
                {DOCTORS.map(d => <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Type *</InputLabel>
              <Select label="Type *" value={form.consultationType}
                onChange={e => setForm(p => ({ ...p, consultationType: e.target.value as ConsultationType }))}>
                <MenuItem value="VIDEO">Video Call</MenuItem>
                <MenuItem value="AUDIO">Audio Call</MenuItem>
                <MenuItem value="CHAT">Chat</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <TextField label="Scheduled At *" type="datetime-local" value={form.scheduledAt}
              onChange={set('scheduledAt')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Chief Complaint *" value={form.chiefComplaint} onChange={set('chiefComplaint')}
              fullWidth multiline rows={3} placeholder="Describe the patient's primary concern…" />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Scheduling…' : 'Schedule'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Complete Consultation Dialog ───────────────────────────────────────────────

function CompleteDialog({ consultationId, open, onClose, onDone }: {
  consultationId: string; open: boolean; onClose: () => void; onDone: () => void
}) {
  const [notes, setNotes] = useState('')
  const mutation = useMutation({
    mutationFn: () => telehealthApi.patch(`/api/v1/consultations/${consultationId}/complete`, { notes }),
    onSuccess: () => { onDone(); setNotes('') },
  })
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Complete Consultation</DialogTitle>
      <DialogContent dividers>
        <TextField label="Clinical Notes (optional)" value={notes} onChange={e => setNotes(e.target.value)}
          fullWidth multiline rows={4} placeholder="Diagnosis, treatment plan, follow-up instructions…" />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" color="success" onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <CheckIcon />}>
          Complete
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Add Monitoring Reading Dialog ──────────────────────────────────────────────

interface MonitorForm {
  patientId: string; deviceId: string; deviceType: DeviceType | ''
  metricName: string; value: string; unit: string; recordedAt: string
  alertThresholdMin: string; alertThresholdMax: string
}
const EMPTY_MONITOR: MonitorForm = {
  patientId: '', deviceId: '', deviceType: '', metricName: '', value: '', unit: '',
  recordedAt: '', alertThresholdMin: '', alertThresholdMax: '',
}
const DEVICE_METRICS: Partial<Record<DeviceType, { metric: string; unit: string }>> = {
  HEART_RATE_MONITOR: { metric: 'Heart Rate',    unit: 'bpm' },
  GLUCOMETER:         { metric: 'Blood Glucose',  unit: 'mmol/L' },
  BLOOD_PRESSURE:     { metric: 'Systolic BP',    unit: 'mmHg' },
  PULSE_OXIMETER:     { metric: 'SpO2',           unit: '%' },
  THERMOMETER:        { metric: 'Temperature',    unit: '°C' },
}

function AddMonitoringDialog({ open, onClose, onSaved }: {
  open: boolean; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<MonitorForm>(EMPTY_MONITOR)
  const [err, setErr] = useState<string | null>(null)

  const { data: patientPage } = useQuery<PatientPage>({
    queryKey: ['patients-brief'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=100').then(r => r.data),
    enabled: open,
  })
  const patients = patientPage?.content ?? []

  const set = (f: keyof MonitorForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const onDeviceChange = (dt: DeviceType) => {
    const preset = DEVICE_METRICS[dt]
    setForm(p => ({ ...p, deviceType: dt, metricName: preset?.metric ?? '', unit: preset?.unit ?? '' }))
  }

  const mutation = useMutation({
    mutationFn: (f: MonitorForm) => telehealthApi.post('/api/v1/monitoring/readings', {
      patientId: f.patientId, deviceId: f.deviceId, deviceType: f.deviceType,
      metricName: f.metricName, value: parseFloat(f.value), unit: f.unit,
      recordedAt: f.recordedAt,
      alertThresholdMin: f.alertThresholdMin ? parseFloat(f.alertThresholdMin) : undefined,
      alertThresholdMax: f.alertThresholdMax ? parseFloat(f.alertThresholdMax) : undefined,
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_MONITOR); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to add reading'),
  })

  const submit = () => {
    if (!form.patientId || !form.deviceId || !form.deviceType || !form.value || !form.recordedAt) {
      setErr('Patient, device, value and time are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add Monitoring Reading</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Patient *</InputLabel>
              <Select label="Patient *" value={form.patientId}
                onChange={e => setForm(p => ({ ...p, patientId: e.target.value }))}>
                {patients.map(pt => <MenuItem key={pt.id} value={pt.id}>{pt.firstName} {pt.lastName}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Device Type *</InputLabel>
              <Select label="Device Type *" value={form.deviceType}
                onChange={e => onDeviceChange(e.target.value as DeviceType)}>
                <MenuItem value="HEART_RATE_MONITOR">Heart Rate Monitor</MenuItem>
                <MenuItem value="GLUCOMETER">Glucometer</MenuItem>
                <MenuItem value="BLOOD_PRESSURE">Blood Pressure</MenuItem>
                <MenuItem value="PULSE_OXIMETER">Pulse Oximeter</MenuItem>
                <MenuItem value="THERMOMETER">Thermometer</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <TextField label="Device ID *" value={form.deviceId} onChange={set('deviceId')} fullWidth placeholder="e.g. DEV-001" />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Metric Name *" value={form.metricName} onChange={set('metricName')} fullWidth />
          </Grid>
          <Grid item xs={3}>
            <TextField label="Value *" type="number" value={form.value} onChange={set('value')} fullWidth />
          </Grid>
          <Grid item xs={3}>
            <TextField label="Unit" value={form.unit} onChange={set('unit')} fullWidth />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Recorded At *" type="datetime-local" value={form.recordedAt}
              onChange={set('recordedAt')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Alert Min (optional)" type="number" value={form.alertThresholdMin} onChange={set('alertThresholdMin')} fullWidth />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Alert Max (optional)" type="number" value={form.alertThresholdMax} onChange={set('alertThresholdMax')} fullWidth />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Saving…' : 'Add Reading'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Consultation Columns ───────────────────────────────────────────────────────

function ConsultationGrid({ rows, onAction }: {
  rows: ConsultationResponse[]
  onAction: (id: string, action: 'start' | 'complete' | 'cancel') => void
}) {
  const columns: Array<GridColDef<ConsultationResponse>> = [
    {
      field: 'scheduledAt', headerName: 'Scheduled', width: 165,
      valueGetter: p => new Date(p.row.scheduledAt).toLocaleString('en-CA', { dateStyle: 'short', timeStyle: 'short' }),
    },
    { field: 'doctorName', headerName: 'Doctor', flex: 1 },
    { field: 'chiefComplaint', headerName: 'Chief Complaint', flex: 1.5 },
    {
      field: 'consultationType', headerName: 'Type', width: 100,
      renderCell: p => <Chip label={p.row.consultationType} color={TYPE_COLORS[p.row.consultationType]} size="small" />,
    },
    {
      field: 'status', headerName: 'Status', width: 125,
      renderCell: p => <Chip label={p.row.status.replace('_', ' ')} color={STATUS_COLORS[p.row.status]} size="small" />,
    },
    {
      field: 'actions', headerName: 'Actions', width: 210, sortable: false,
      renderCell: p => {
        const s = p.row.status
        return (
          <Stack direction="row" spacing={0.5} alignItems="center" height="100%">
            {s === 'SCHEDULED' && (
              <Button size="small" color="info" startIcon={<PlayArrowIcon />}
                onClick={() => onAction(p.row.id, 'start')}>Start</Button>
            )}
            {s === 'IN_PROGRESS' && (
              <Button size="small" color="success" startIcon={<CheckIcon />}
                onClick={() => onAction(p.row.id, 'complete')}>Complete</Button>
            )}
            {(s === 'SCHEDULED' || s === 'IN_PROGRESS') && (
              <Button size="small" color="error" startIcon={<CancelIcon />}
                onClick={() => onAction(p.row.id, 'cancel')}>Cancel</Button>
            )}
          </Stack>
        )
      },
    },
  ]
  return (
    <DataGrid rows={rows} columns={columns} pageSizeOptions={[10, 25]}
      initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
      autoHeight disableRowSelectionOnClick />
  )
}

const monitoringColumns: Array<GridColDef<MonitoringResponse>> = [
  {
    field: 'recordedAt', headerName: 'Time', width: 165,
    valueGetter: p => new Date(p.row.recordedAt).toLocaleString('en-CA', { dateStyle: 'short', timeStyle: 'short' }),
  },
  { field: 'deviceId', headerName: 'Device', width: 110 },
  { field: 'deviceType', headerName: 'Device Type', flex: 1, valueGetter: p => p.row.deviceType.replace(/_/g, ' ') },
  { field: 'metricName', headerName: 'Metric', flex: 1 },
  { field: 'value', headerName: 'Reading', width: 120, valueGetter: p => `${p.row.value} ${p.row.unit}` },
  {
    field: 'isAlert', headerName: 'Status', width: 100,
    renderCell: p => <Chip label={p.row.isAlert ? '⚠ Alert' : 'Normal'} color={p.row.isAlert ? 'error' : 'success'} size="small" />,
  },
]

const MONITORED_PATIENTS = [
  '550e8400-e29b-41d4-a716-446655440002',
  '550e8400-e29b-41d4-a716-446655440004',
]

// ── Monitoring Charts Tab ─────────────────────────────────────────────────────

function MonitoringChartsTab({ patients }: { patients: PatientBrief[] }) {
  const [selectedPatientId, setSelectedPatientId] = useState('')

  const { data: readingsPage, isLoading } = useQuery<MonitoringPage>({
    queryKey: ['monitoring-chart', selectedPatientId],
    queryFn: () =>
      telehealthApi
        .get(`/api/v1/monitoring/patients/${selectedPatientId}/readings?size=100`)
        .then(r => r.data),
    enabled: !!selectedPatientId,
  })

  const readings = readingsPage?.content ?? []

  const metricGroups = readings.reduce<Record<string, Array<{ time: string; value: number; isAlert: boolean }>>>(
    (acc, r) => {
      const key = `${r.metricName} (${r.unit})`
      if (!acc[key]) acc[key] = []
      acc[key].push({
        time: dayjs(r.recordedAt).format('MM/DD HH:mm'),
        value: r.value,
        isAlert: r.isAlert,
      })
      return acc
    },
    {}
  )

  Object.keys(metricGroups).forEach(k => {
    metricGroups[k].sort((a, b) => a.time.localeCompare(b.time))
  })

  return (
    <Box>
      <FormControl sx={{ mb: 3, minWidth: 280 }}>
        <InputLabel>Select Patient</InputLabel>
        <Select
          label="Select Patient"
          value={selectedPatientId}
          onChange={e => setSelectedPatientId(e.target.value)}
        >
          {patients.map(p => (
            <MenuItem key={p.id} value={p.id}>{p.firstName} {p.lastName}</MenuItem>
          ))}
        </Select>
      </FormControl>

      {!selectedPatientId && (
        <Typography color="text.secondary">
          Select a patient to view their monitoring charts
        </Typography>
      )}
      {selectedPatientId && isLoading && <CircularProgress />}
      {selectedPatientId && !isLoading && readings.length === 0 && (
        <Typography color="text.secondary">No monitoring readings for this patient</Typography>
      )}

      <Grid container spacing={2}>
        {Object.entries(metricGroups).map(([metric, data]) => (
          <Grid item xs={12} md={6} key={metric}>
            <Card>
              <CardContent>
                <Typography variant="subtitle1" fontWeight={600} gutterBottom>
                  <ShowChartIcon sx={{ verticalAlign: 'middle', mr: 0.5, fontSize: 18 }} />
                  {metric}
                </Typography>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={data}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} interval="preserveStartEnd" />
                    <YAxis tick={{ fontSize: 11 }} />
                    <ReTooltip />
                    <Line
                      type="monotone"
                      dataKey="value"
                      stroke="#1565C0"
                      strokeWidth={2}
                      dot={(props: any) => (
                        <circle
                          key={`${props.cx}-${props.cy}`}
                          cx={props.cx}
                          cy={props.cy}
                          r={4}
                          fill={props.payload?.isAlert ? '#C62828' : '#1565C0'}
                          stroke="white"
                          strokeWidth={1}
                        />
                      )}
                    />
                  </LineChart>
                </ResponsiveContainer>
                <Typography variant="caption" color="text.secondary">
                  {data.filter(d => d.isAlert).length} alert reading(s) shown in red
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}

export default function TelehealthPage() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState(0)
  const [scheduleOpen, setScheduleOpen] = useState(false)
  const [monitorOpen, setMonitorOpen] = useState(false)
  const [completeId, setCompleteId] = useState<string | null>(null)
  const [snack, setSnack] = useState('')

  const { data: consultations = [] } = useQuery<ConsultationResponse[]>({
    queryKey: ['consultations-today'],
    queryFn: () => telehealthApi.get('/api/v1/consultations/today').then(r => r.data),
  })

  const { data: allPatients } = useQuery<PatientPage>({
    queryKey: ['patients-brief-tele'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=100').then(r => r.data),
  })
  const patientsList: PatientBrief[] = allPatients?.content ?? []

  const { data: monitoringRows = [] } = useQuery<MonitoringResponse[]>({
    queryKey: ['monitoring-readings'],
    queryFn: async () => {
      const results = await Promise.all(
        MONITORED_PATIENTS.map(pid =>
          telehealthApi.get<MonitoringPage>(`/api/v1/monitoring/patients/${pid}/readings`).then(r => r.data.content)
        )
      )
      return results.flat()
    },
  })

  const startMutation = useMutation({
    mutationFn: (id: string) => telehealthApi.patch(`/api/v1/consultations/${id}/start`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['consultations-today'] }); setSnack('Consultation started') },
    onError: (e: any) => setSnack(e.response?.data?.detail ?? 'Failed to start'),
  })

  const cancelMutation = useMutation({
    mutationFn: (id: string) => telehealthApi.patch(`/api/v1/consultations/${id}/cancel`, { reason: 'Cancelled by staff' }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['consultations-today'] }); setSnack('Consultation cancelled') },
    onError: (e: any) => setSnack(e.response?.data?.detail ?? 'Failed to cancel'),
  })

  const handleAction = (id: string, action: 'start' | 'complete' | 'cancel') => {
    if (action === 'start')    startMutation.mutate(id)
    else if (action === 'cancel') cancelMutation.mutate(id)
    else setCompleteId(id)
  }

  const activeAlerts = monitoringRows.filter(r => r.isAlert).length

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="Telehealth"
        subtitle={`${consultations.length} consultations today · ${activeAlerts} monitoring alerts`} />

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label={`Consultations (${consultations.length})`} />
        <Tab label={`Remote Monitoring (${monitoringRows.length})`} />
        <Tab label="Monitoring Charts" icon={<ShowChartIcon />} iconPosition="start" />
      </Tabs>

      {tab === 0 && (
        <>
          <Stack direction="row" justifyContent="flex-end" mb={2}>
            <Button variant="contained" startIcon={<AddIcon />} onClick={() => setScheduleOpen(true)}>
              Schedule Consultation
            </Button>
          </Stack>
          <ConsultationGrid rows={consultations} onAction={handleAction} />
        </>
      )}

      {tab === 1 && (
        <>
          <Stack direction="row" justifyContent="flex-end" mb={2}>
            <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setMonitorOpen(true)}>
              Add Reading
            </Button>
          </Stack>
          <DataGrid rows={monitoringRows} columns={monitoringColumns}
            pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            autoHeight disableRowSelectionOnClick />
        </>
      )}

      {tab === 2 && <MonitoringChartsTab patients={patientsList} />}

      <ScheduleConsultationDialog
        open={scheduleOpen}
        onClose={() => setScheduleOpen(false)}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ['consultations-today'] })
          setScheduleOpen(false)
          setSnack('Consultation scheduled')
        }}
      />

      <AddMonitoringDialog
        open={monitorOpen}
        onClose={() => setMonitorOpen(false)}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ['monitoring-readings'] })
          setMonitorOpen(false)
          setSnack('Reading recorded')
        }}
      />

      {completeId && (
        <CompleteDialog
          consultationId={completeId}
          open={!!completeId}
          onClose={() => setCompleteId(null)}
          onDone={() => {
            queryClient.invalidateQueries({ queryKey: ['consultations-today'] })
            setCompleteId(null)
            setSnack('Consultation completed')
          }}
        />
      )}

      <Snackbar open={!!snack} autoHideDuration={3500} onClose={() => setSnack('')}>
        <Alert severity="success" onClose={() => setSnack('')}>{snack}</Alert>
      </Snackbar>
    </Box>
  )
}



