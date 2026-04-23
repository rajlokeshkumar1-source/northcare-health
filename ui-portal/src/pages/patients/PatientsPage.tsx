import { useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress,
  Dialog, DialogActions, DialogContent, DialogTitle, Divider,
  FormControl, Grid, IconButton, InputAdornment, InputLabel,
  MenuItem, Select, Snackbar, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import CloseIcon from '@mui/icons-material/Close'
import SearchIcon from '@mui/icons-material/Search'
import LocalHospitalIcon from '@mui/icons-material/LocalHospital'
import ChecklistIcon from '@mui/icons-material/Checklist'
import ReceiptIcon from '@mui/icons-material/Receipt'
import VideoCallIcon from '@mui/icons-material/VideoCall'
import { DataGrid, GridColDef, GridRowSelectionModel } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import PageHeader from '@/components/common/PageHeader'
import { billingApi, hospitalApi, telehealthApi } from '@/api/apiClient'

// ── Types ─────────────────────────────────────────────────────────────────────

type Gender   = 'MALE' | 'FEMALE' | 'OTHER'
type WardType = 'ICU' | 'GENERAL' | 'PEDIATRIC' | 'EMERGENCY'

interface WardResponse { id: string; name: string; wardType: WardType; floor: number; bedCount: number; availableBeds: number }
interface PatientResponse {
  id: string; firstName: string; lastName: string
  dateOfBirth: string; gender: Gender
  bloodType: string | null
  allergies: string[] | null; diagnosisCodes: string[] | null; medications: string[] | null
  ward: Pick<WardResponse,'id'|'name'|'wardType'> | null; admissionDate: string | null
  active: boolean; createdAt: string
}
interface PatientPage { content: PatientResponse[]; totalElements: number }
interface ConsultationBrief {
  id: string; doctorName: string; scheduledAt: string; status: string; consultationType: string; chiefComplaint: string
}
interface ConsultPage { content: ConsultationBrief[]; totalElements: number }
interface InvoiceBrief { id: string; invoiceNumber: string; patientId: string; totalAmount: number; status: string; serviceDate: string }

const BLOOD_TYPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
const fmtDate = (s: string | null) => s ? new Date(s).toLocaleDateString('en-CA') : '—'
const fmtDT   = (s: string) => new Date(s).toLocaleString('en-CA', { dateStyle: 'short', timeStyle: 'short' })

// ── Add Patient Dialog ─────────────────────────────────────────────────────────

interface PatientForm {
  firstName: string; lastName: string; dateOfBirth: string; gender: Gender | ''
  bloodType: string; allergies: string; medications: string; diagnosisCodes: string
}
const EMPTY: PatientForm = { firstName: '', lastName: '', dateOfBirth: '', gender: '', bloodType: '', allergies: '', medications: '', diagnosisCodes: '' }

function AddPatientDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<PatientForm>(EMPTY)
  const [err, setErr]   = useState<string | null>(null)
  const set = (f: keyof PatientForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => setForm(p => ({ ...p, [f]: e.target.value }))

  const mutation = useMutation({
    mutationFn: (f: PatientForm) => hospitalApi.post('/api/v1/patients', {
      firstName: f.firstName, lastName: f.lastName, dateOfBirth: f.dateOfBirth, gender: f.gender,
      bloodType: f.bloodType || undefined,
      allergies:      f.allergies      ? f.allergies.split(',').map(s => s.trim()).filter(Boolean)      : [],
      medications:    f.medications    ? f.medications.split(',').map(s => s.trim()).filter(Boolean)    : [],
      diagnosisCodes: f.diagnosisCodes ? f.diagnosisCodes.split(',').map(s => s.trim()).filter(Boolean) : [],
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to create patient'),
  })

  const submit = () => {
    if (!form.firstName || !form.lastName || !form.dateOfBirth || !form.gender) {
      setErr('First name, last name, date of birth and gender are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add New Patient</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={6}><TextField label="First Name *" value={form.firstName} onChange={set('firstName')} fullWidth /></Grid>
          <Grid item xs={6}><TextField label="Last Name *"  value={form.lastName}  onChange={set('lastName')}  fullWidth /></Grid>
          <Grid item xs={6}>
            <TextField label="Date of Birth *" type="date" value={form.dateOfBirth} onChange={set('dateOfBirth')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth><InputLabel>Gender *</InputLabel>
              <Select label="Gender *" value={form.gender} onChange={e => setForm(p => ({ ...p, gender: e.target.value as Gender }))}>
                <MenuItem value="MALE">Male</MenuItem><MenuItem value="FEMALE">Female</MenuItem><MenuItem value="OTHER">Other</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth><InputLabel>Blood Type</InputLabel>
              <Select label="Blood Type" value={form.bloodType} onChange={e => setForm(p => ({ ...p, bloodType: e.target.value }))}>
                <MenuItem value=""><em>Unknown</em></MenuItem>
                {BLOOD_TYPES.map(bt => <MenuItem key={bt} value={bt}>{bt}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6} />
          <Grid item xs={12}>
            <TextField label="Allergies (comma-separated)" value={form.allergies} onChange={set('allergies')} fullWidth
              placeholder="e.g. Penicillin, Latex" helperText="Separate multiple entries with commas" />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Current Medications (comma-separated)" value={form.medications} onChange={set('medications')} fullWidth multiline rows={2} />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Diagnosis Codes – ICD-10 (comma-separated)" value={form.diagnosisCodes} onChange={set('diagnosisCodes')} fullWidth placeholder="e.g. E11.9, I10" />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Saving…' : 'Add Patient'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Patient Detail Card (with consultation history + timeline) ────────────────

function PatientDetailCard({ patient, onClose }: { patient: PatientResponse; onClose: () => void }) {
  const [detailTab, setDetailTab] = useState(0)

  const { data: consultPage, isLoading: lCon } = useQuery<ConsultPage>({
    queryKey: ['patient-consults', patient.id],
    queryFn: () => telehealthApi.get(`/api/v1/consultations/patient/${patient.id}?size=20`).then(r => r.data),
  })
  const { data: invoicePage, isLoading: lInv } = useQuery<{ content: InvoiceBrief[] }>({
    queryKey: ['patient-invoices', patient.id],
    queryFn: () => billingApi.get(`/api/v1/invoices?size=100`).then(r => r.data),
  })

  const consultations = consultPage?.content ?? []
  const invoices = (invoicePage?.content ?? []).filter(i => i.patientId === patient.id)

  type TimelineEvent =
    | { kind: 'consult'; date: string; item: ConsultationBrief }
    | { kind: 'invoice'; date: string; item: InvoiceBrief }

  const timeline: TimelineEvent[] = [
    ...consultations.map(c => ({ kind: 'consult' as const, date: c.scheduledAt, item: c })),
    ...invoices.map(i => ({ kind: 'invoice' as const, date: i.serviceDate, item: i })),
  ].sort((a, b) => b.date.localeCompare(a.date))

  return (
    <Card variant="outlined" sx={{ mt: 2, position: 'relative' }}>
      <IconButton size="small" onClick={onClose} sx={{ position: 'absolute', right: 8, top: 8 }}>
        <CloseIcon fontSize="small" />
      </IconButton>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          {patient.firstName} {patient.lastName}
          <Chip label={patient.gender} size="small" sx={{ ml: 1 }} />
          {patient.bloodType && <Chip label={patient.bloodType} color="error" size="small" sx={{ ml: 1 }} />}
          <Chip label={patient.active ? 'Active' : 'Discharged'} color={patient.active ? 'success' : 'default'} size="small" sx={{ ml: 1 }} />
        </Typography>

        <Tabs value={detailTab} onChange={(_, v) => setDetailTab(v)} sx={{ mb: 2 }} textColor="primary" indicatorColor="primary">
          <Tab label="Overview" />
          <Tab label={`Timeline (${timeline.length})`} />
        </Tabs>

        {detailTab === 0 && (
          <Grid container spacing={2}>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary">Date of Birth</Typography>
              <Typography>{fmtDate(patient.dateOfBirth)}</Typography>
            </Grid>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary">Ward</Typography>
              <Typography>{patient.ward ? `${patient.ward.name} (${patient.ward.wardType})` : 'Unassigned'}</Typography>
            </Grid>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary">Admitted</Typography>
              <Typography>{fmtDate(patient.admissionDate)}</Typography>
            </Grid>
            <Grid item xs={12}><Divider /></Grid>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary" gutterBottom>Allergies</Typography>
              {patient.allergies?.length
                ? <Stack direction="row" flexWrap="wrap" gap={0.5}>{patient.allergies.map(a => <Chip key={a} label={a} color="warning" size="small" />)}</Stack>
                : <Typography variant="body2" color="text.secondary">None recorded</Typography>}
            </Grid>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary" gutterBottom>Medications</Typography>
              {patient.medications?.length
                ? <Stack spacing={0.5}>{patient.medications.map(m => <Typography key={m} variant="body2">• {m}</Typography>)}</Stack>
                : <Typography variant="body2" color="text.secondary">None recorded</Typography>}
            </Grid>
            <Grid item xs={4}>
              <Typography variant="body2" color="text.secondary" gutterBottom>ICD-10 Codes</Typography>
              {patient.diagnosisCodes?.length
                ? <Stack direction="row" flexWrap="wrap" gap={0.5}>{patient.diagnosisCodes.map(d => <Chip key={d} label={d} variant="outlined" size="small" />)}</Stack>
                : <Typography variant="body2" color="text.secondary">None recorded</Typography>}
            </Grid>
            <Grid item xs={12}><Divider /></Grid>
            <Grid item xs={12}>
              <Typography variant="body2" color="text.secondary" gutterBottom fontWeight={600}>
                Recent Consultations {lCon && <CircularProgress size={12} sx={{ ml: 1 }} />}
              </Typography>
              {!lCon && consultations.length === 0
                ? <Typography variant="body2" color="text.secondary">No consultations on record</Typography>
                : consultations.slice(0, 5).map(c => (
                  <Stack key={c.id} direction="row" spacing={2} alignItems="center" sx={{ mb: 0.5 }}>
                    <Chip label={c.status.replace('_',' ')} size="small"
                      color={c.status === 'COMPLETED' ? 'success' : c.status === 'SCHEDULED' ? 'warning' : 'default'} />
                    <Chip label={c.consultationType} size="small" variant="outlined" />
                    <Typography variant="body2">{c.doctorName} — {fmtDT(c.scheduledAt)}</Typography>
                    <Typography variant="body2" color="text.secondary">{c.chiefComplaint}</Typography>
                  </Stack>
                ))}
            </Grid>
          </Grid>
        )}

        {detailTab === 1 && (
          <Box>
            {(lCon || lInv) && <CircularProgress size={20} sx={{ mb: 2 }} />}
            {timeline.length === 0 && !lCon && !lInv && (
              <Typography color="text.secondary">No events on record for this patient</Typography>
            )}
            <Stack spacing={0}>
              {timeline.map((ev, idx) => (
                <Stack key={ev.kind + idx} direction="row" spacing={2} alignItems="flex-start" sx={{ py: 1.5 }}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', pt: 0.5 }}>
                    <Box sx={{
                      width: 32, height: 32, borderRadius: '50%',
                      bgcolor: ev.kind === 'consult' ? 'primary.main' : 'success.main',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      {ev.kind === 'consult'
                        ? <VideoCallIcon sx={{ color: 'white', fontSize: 16 }} />
                        : <ReceiptIcon sx={{ color: 'white', fontSize: 16 }} />}
                    </Box>
                    {idx < timeline.length - 1 && (
                      <Box sx={{ width: 2, flex: 1, minHeight: 24, bgcolor: 'divider', mt: 0.5 }} />
                    )}
                  </Box>
                  <Box sx={{ flex: 1, pb: 1 }}>
                    {ev.kind === 'consult' ? (
                      <>
                        <Typography variant="body2" fontWeight={600}>
                          {ev.item.consultationType} Consultation — {ev.item.doctorName}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {fmtDT(ev.item.scheduledAt)}
                        </Typography>
                        <Stack direction="row" spacing={1} mt={0.5}>
                          <Chip label={(ev.item as ConsultationBrief).status.replace('_',' ')} size="small"
                            color={(ev.item as ConsultationBrief).status === 'COMPLETED' ? 'success' : 'warning'} />
                          <Typography variant="body2" color="text.secondary">{ev.item.chiefComplaint}</Typography>
                        </Stack>
                      </>
                    ) : (
                      <>
                        <Typography variant="body2" fontWeight={600}>
                          Invoice {(ev.item as InvoiceBrief).invoiceNumber} — ${(ev.item as InvoiceBrief).totalAmount.toLocaleString('en-CA', { minimumFractionDigits: 2 })}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          Service date: {(ev.item as InvoiceBrief).serviceDate}
                        </Typography>
                        <Box mt={0.5}>
                          <Chip label={(ev.item as InvoiceBrief).status} size="small"
                            color={(ev.item as InvoiceBrief).status === 'PAID' ? 'success' : (ev.item as InvoiceBrief).status === 'OVERDUE' ? 'error' : 'default'} />
                        </Box>
                      </>
                    )}
                  </Box>
                </Stack>
              ))}
            </Stack>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

// ── Create Ward Dialog ─────────────────────────────────────────────────────────

interface WardForm { name: string; wardType: WardType | ''; floor: string; bedCount: string; availableBeds: string }
const EMPTY_WARD: WardForm = { name: '', wardType: '', floor: '', bedCount: '', availableBeds: '' }

function CreateWardDialog({ open, onClose, onSaved }: { open: boolean; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<WardForm>(EMPTY_WARD)
  const [err, setErr]   = useState<string | null>(null)
  const set = (f: keyof WardForm) => (e: React.ChangeEvent<HTMLInputElement>) => setForm(p => ({ ...p, [f]: e.target.value }))

  const mutation = useMutation({
    mutationFn: (f: WardForm) => hospitalApi.post('/api/v1/wards', {
      name: f.name, wardType: f.wardType,
      floor: parseInt(f.floor), bedCount: parseInt(f.bedCount), availableBeds: parseInt(f.availableBeds),
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_WARD); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to create ward'),
  })

  const submit = () => {
    if (!form.name || !form.wardType || !form.floor || !form.bedCount) {
      setErr('Name, type, floor and bed count are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Create Ward</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}><TextField label="Ward Name *" value={form.name} onChange={set('name')} fullWidth placeholder="e.g. Ward A" /></Grid>
          <Grid item xs={12}>
            <FormControl fullWidth><InputLabel>Ward Type *</InputLabel>
              <Select label="Ward Type *" value={form.wardType} onChange={e => setForm(p => ({ ...p, wardType: e.target.value as WardType }))}>
                <MenuItem value="ICU">ICU</MenuItem><MenuItem value="GENERAL">General</MenuItem>
                <MenuItem value="PEDIATRIC">Pediatric</MenuItem><MenuItem value="EMERGENCY">Emergency</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={4}><TextField label="Floor *" type="number" value={form.floor} onChange={set('floor')} fullWidth inputProps={{ min: 1 }} /></Grid>
          <Grid item xs={4}><TextField label="Total Beds *" type="number" value={form.bedCount} onChange={set('bedCount')} fullWidth inputProps={{ min: 1 }} /></Grid>
          <Grid item xs={4}><TextField label="Available" type="number" value={form.availableBeds} onChange={set('availableBeds')} fullWidth inputProps={{ min: 0 }} /></Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Creating…' : 'Create Ward'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Assign Ward Dialog (batch) ─────────────────────────────────────────────────

function AssignWardDialog({
  open, onClose, onSaved, selectedIds, allPatients, wards,
}: {
  open: boolean; onClose: () => void; onSaved: () => void
  selectedIds: GridRowSelectionModel; allPatients: PatientResponse[]; wards: WardResponse[]
}) {
  const [wardId, setWardId] = useState('')
  const [err, setErr] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async (wId: string) => {
      const selected = allPatients.filter(p => selectedIds.includes(p.id))
      await Promise.all(selected.map(p =>
        hospitalApi.put(`/api/v1/patients/${p.id}`, {
          firstName: p.firstName, lastName: p.lastName,
          dateOfBirth: p.dateOfBirth, gender: p.gender,
          bloodType: p.bloodType ?? undefined,
          allergies: p.allergies ?? [],
          medications: p.medications ?? [],
          diagnosisCodes: p.diagnosisCodes ?? [],
          wardId: wId || null,
        })
      ))
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['patients'] })
      onSaved(); setWardId(''); setErr(null)
    },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to assign ward'),
  })

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>
        <ChecklistIcon sx={{ verticalAlign: 'middle', mr: 1 }} />
        Assign Ward — {selectedIds.length} patient{selectedIds.length !== 1 ? 's' : ''}
      </DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <FormControl fullWidth>
          <InputLabel>Target Ward</InputLabel>
          <Select label="Target Ward" value={wardId} onChange={e => setWardId(e.target.value)}>
            <MenuItem value=""><em>Unassign (no ward)</em></MenuItem>
            {wards.map(w => (
              <MenuItem key={w.id} value={w.id} disabled={w.availableBeds === 0}>
                {w.name} ({w.wardType}) — {w.availableBeds} beds available
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={() => mutation.mutate(wardId)} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <LocalHospitalIcon />}>
          {mutation.isPending ? 'Assigning…' : 'Assign'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

const patientCols: Array<GridColDef<PatientResponse>> = [
  { field: 'fullName', headerName: 'Patient', flex: 1.5, valueGetter: p => `${p.row.lastName}, ${p.row.firstName}` },
  { field: 'dateOfBirth', headerName: 'Date of Birth', width: 130, valueGetter: p => fmtDate(p.row.dateOfBirth) },
  { field: 'gender', headerName: 'Gender', width: 90, valueGetter: p => p.row.gender },
  { field: 'bloodType', headerName: 'Blood', width: 90,
    renderCell: p => p.row.bloodType ? <Chip label={p.row.bloodType} color="error" size="small" /> : <span>—</span> },
  { field: 'ward', headerName: 'Ward', flex: 1, valueGetter: p => p.row.ward?.name ?? 'Unassigned' },
  { field: 'allergies', headerName: 'Allergies', width: 115,
    renderCell: p => <Chip label={p.row.allergies?.length ? `${p.row.allergies.length} known` : 'None'}
      color={p.row.allergies?.length ? 'warning' : 'default'} size="small" /> },
  { field: 'active', headerName: 'Status', width: 115,
    renderCell: p => <Chip label={p.row.active ? 'Active' : 'Discharged'} color={p.row.active ? 'success' : 'default'} size="small" /> },
]

const wardCols: Array<GridColDef<WardResponse>> = [
  { field: 'name', headerName: 'Ward', flex: 1 },
  { field: 'wardType', headerName: 'Type', width: 130,
    renderCell: p => <Chip label={p.row.wardType} variant="outlined" size="small" /> },
  { field: 'floor', headerName: 'Floor', width: 80 },
  { field: 'bedCount', headerName: 'Total Beds', width: 110 },
  { field: 'availableBeds', headerName: 'Available', width: 110,
    renderCell: p => <Chip label={p.row.availableBeds} size="small"
      color={p.row.availableBeds === 0 ? 'error' : p.row.availableBeds <= 2 ? 'warning' : 'success'} /> },
  { field: 'occupancy', headerName: 'Occupancy', width: 115,
    valueGetter: p => `${Math.round(((p.row.bedCount - p.row.availableBeds) / p.row.bedCount) * 100)}%` },
]

// ── Page ──────────────────────────────────────────────────────────────────────

export default function PatientsPage() {
  const queryClient = useQueryClient()
  const [mainTab, setMainTab]   = useState(0)
  const [addOpen, setAddOpen]   = useState(false)
  const [wardOpen, setWardOpen] = useState(false)
  const [assignOpen, setAssignOpen] = useState(false)
  const [selected, setSelected] = useState<PatientResponse | null>(null)
  const [selectionModel, setSelectionModel] = useState<GridRowSelectionModel>([])
  const [snack, setSnack]       = useState('')
  const [search, setSearch]     = useState('')
  const [filterBlood, setFilterBlood]   = useState('')
  const [filterWardType, setFilterWardType] = useState('')

  const { data, isLoading, isError, error } = useQuery<PatientPage>({
    queryKey: ['patients'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=100').then(r => r.data),
  })
  const { data: wards = [], isLoading: lWards } = useQuery<WardResponse[]>({
    queryKey: ['wards'],
    queryFn: () => hospitalApi.get('/api/v1/wards').then(r => r.data),
  })

  const allPatients = data?.content ?? []
  const filtered = allPatients.filter(p => {
    const name = `${p.firstName} ${p.lastName}`.toLowerCase()
    if (search && !name.includes(search.toLowerCase())) return false
    if (filterBlood && p.bloodType !== filterBlood) return false
    if (filterWardType && p.ward?.wardType !== filterWardType) return false
    return true
  })

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2}>
        <PageHeader title="Patients" subtitle={`${data?.totalElements ?? 0} registered`} />
        {mainTab === 0
          ? <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAddOpen(true)} sx={{ mt: 1 }}>Add Patient</Button>
          : <Button variant="contained" startIcon={<LocalHospitalIcon />} onClick={() => setWardOpen(true)} sx={{ mt: 1 }}>Create Ward</Button>}
      </Stack>

      <Tabs value={mainTab} onChange={(_, v) => { setMainTab(v); setSelected(null) }} sx={{ mb: 2 }}>
        <Tab label={`Patients (${allPatients.length})`} />
        <Tab label={`Wards (${wards.length})`} />
      </Tabs>

      {mainTab === 0 && (
        <>
          {/* Search + filter bar */}
          <Stack direction="row" spacing={2} mb={2} flexWrap="wrap" alignItems="center">
            <TextField size="small" placeholder="Search by name…" value={search}
              onChange={e => setSearch(e.target.value)} sx={{ minWidth: 220 }}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }} />
            <FormControl size="small" sx={{ minWidth: 130 }}>
              <InputLabel>Blood Type</InputLabel>
              <Select label="Blood Type" value={filterBlood} onChange={e => setFilterBlood(e.target.value)}>
                <MenuItem value=""><em>Any</em></MenuItem>
                {BLOOD_TYPES.map(bt => <MenuItem key={bt} value={bt}>{bt}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 140 }}>
              <InputLabel>Ward Type</InputLabel>
              <Select label="Ward Type" value={filterWardType} onChange={e => setFilterWardType(e.target.value)}>
                <MenuItem value=""><em>Any</em></MenuItem>
                <MenuItem value="ICU">ICU</MenuItem><MenuItem value="GENERAL">General</MenuItem>
                <MenuItem value="PEDIATRIC">Pediatric</MenuItem><MenuItem value="EMERGENCY">Emergency</MenuItem>
              </Select>
            </FormControl>
            {(search || filterBlood || filterWardType) && (
              <Button size="small" onClick={() => { setSearch(''); setFilterBlood(''); setFilterWardType('') }}>
                Clear Filters
              </Button>
            )}
            <Typography variant="body2" color="text.secondary" sx={{ alignSelf: 'center' }}>
              {filtered.length} result{filtered.length !== 1 ? 's' : ''}
            </Typography>
            {selectionModel.length > 0 && (
              <Button size="small" variant="outlined" color="primary" startIcon={<LocalHospitalIcon />}
                onClick={() => setAssignOpen(true)}>
                Assign Ward ({selectionModel.length})
              </Button>
            )}
          </Stack>

          {isLoading ? <Typography color="text.secondary">Loading…</Typography>
            : isError  ? <Alert severity="error">{(error as Error).message}</Alert>
            : (
              <DataGrid rows={filtered} columns={patientCols}
                pageSizeOptions={[10, 25, 50]}
                initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
                checkboxSelection
                rowSelectionModel={selectionModel}
                onRowSelectionModelChange={setSelectionModel}
                autoHeight
                onRowClick={({ row }) => setSelected(p => p?.id === row.id ? null : row as PatientResponse)}
                sx={{ cursor: 'pointer' }} />
            )}
          {selected && <PatientDetailCard patient={selected} onClose={() => setSelected(null)} />}
        </>
      )}

      {mainTab === 1 && (
        <DataGrid rows={wards} columns={wardCols} loading={lWards}
          pageSizeOptions={[10, 25]} initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          autoHeight disableRowSelectionOnClick />
      )}

      <AddPatientDialog open={addOpen} onClose={() => setAddOpen(false)}
        onSaved={() => { queryClient.invalidateQueries({ queryKey: ['patients'] }); setAddOpen(false); setSnack('Patient added') }} />

      <CreateWardDialog open={wardOpen} onClose={() => setWardOpen(false)}
        onSaved={() => { queryClient.invalidateQueries({ queryKey: ['wards'] }); setWardOpen(false); setSnack('Ward created') }} />

      <AssignWardDialog
        open={assignOpen} onClose={() => setAssignOpen(false)}
        onSaved={() => { setAssignOpen(false); setSelectionModel([]); setSnack(`Ward assigned to ${selectionModel.length} patient(s)`) }}
        selectedIds={selectionModel} allPatients={allPatients} wards={wards}
      />

      <Snackbar open={!!snack} autoHideDuration={3500} onClose={() => setSnack('')}>
        <Alert severity="success" onClose={() => setSnack('')}>{snack}</Alert>
      </Snackbar>
    </Box>
  )
}


