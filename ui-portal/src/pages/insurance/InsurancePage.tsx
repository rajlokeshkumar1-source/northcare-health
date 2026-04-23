import { useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, Divider, FormControl, Grid, InputLabel, MenuItem, Paper, Select, Snackbar,
  Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RateReviewIcon from '@mui/icons-material/RateReview'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import CancelIcon from '@mui/icons-material/Cancel'
import GavelIcon from '@mui/icons-material/Gavel'
import AttachMoneyIcon from '@mui/icons-material/AttachMoney'
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety'
import DownloadIcon from '@mui/icons-material/Download'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import PageHeader from '@/components/common/PageHeader'
import { insuranceApi } from '@/api/apiClient'
import { downloadCsv } from '@/utils/csvExport'

// ── Types ─────────────────────────────────────────────────────────────────────

type PolicyType   = 'BASIC' | 'EXTENDED' | 'PREMIUM' | 'GOVERNMENT'
type PolicyStatus = 'ACTIVE' | 'EXPIRED' | 'SUSPENDED' | 'CANCELLED'
type ClaimStatus  = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'PARTIALLY_APPROVED' | 'DENIED' | 'APPEALED' | 'PAID'

interface InsurancePolicy {
  id: string; policyNumber: string; patientId: string; providerName: string
  policyType: PolicyType; coverageStartDate: string; coverageEndDate: string
  deductibleAmount: number | null; coverageLimit: number; coveredAmount: number
  status: PolicyStatus; groupNumber: string | null; memberNumber: string | null
}

interface InsuranceClaim {
  id: string; claimNumber: string; policyId: string; patientId: string
  serviceDate: string; billedAmount: number; approvedAmount: number | null
  status: ClaimStatus; submittedAt: string; denialReason: string | null
}

interface PolicyPage { content: InsurancePolicy[]; totalElements: number }
interface ClaimPage  { content: InsuranceClaim[];  totalElements: number }

const PROVIDERS = [
  { id: '20000000-0000-0000-0000-000000000001', name: 'Blue Cross Blue Shield' },
  { id: '20000000-0000-0000-0000-000000000002', name: 'Aetna Health Insurance' },
  { id: '20000000-0000-0000-0000-000000000003', name: 'Ontario Health Insurance Plan (OHIP)' },
  { id: '20000000-0000-0000-0000-000000000004', name: 'Sun Life Financial' },
]

const POLICY_COLORS: Record<PolicyStatus, 'success' | 'error' | 'warning' | 'default'> = {
  ACTIVE: 'success', EXPIRED: 'default', SUSPENDED: 'warning', CANCELLED: 'error',
}
const CLAIM_COLORS: Record<ClaimStatus, 'warning' | 'info' | 'success' | 'default' | 'error'> = {
  SUBMITTED: 'warning', UNDER_REVIEW: 'info', APPROVED: 'success',
  PARTIALLY_APPROVED: 'warning', DENIED: 'error', APPEALED: 'warning', PAID: 'success',
}
const fmt = (n: number) => `$${Number(n).toLocaleString('en-CA', { minimumFractionDigits: 2 })}`

// ── Stat Card ─────────────────────────────────────────────────────────────────

function StatCard({ label, value, color }: { label: string; value: string | number; color?: string }) {
  return (
    <Paper variant="outlined" sx={{ p: 2, textAlign: 'center' }}>
      <Typography variant="h5" fontWeight={700} color={color ?? 'text.primary'}>{value}</Typography>
      <Typography variant="caption" color="text.secondary">{label}</Typography>
    </Paper>
  )
}

// ── Coverage Check Dialog ─────────────────────────────────────────────────────

interface CoverageCheckResult {
  covered: boolean; coverageLimit?: number; coveredAmount?: number
  deductibleAmount?: number; remainingCoverage?: number; message?: string
  [key: string]: unknown
}

function CoverageCheckDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [patientId, setPatientId] = useState('')
  const [amount, setAmount]       = useState('')
  const [result, setResult]       = useState<CoverageCheckResult | null>(null)
  const [err, setErr]             = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => insuranceApi.post<CoverageCheckResult>('/api/v1/policies/coverage-check', {
      patientId, amount: parseFloat(amount),
    }),
    onSuccess: (res) => { setResult(res.data); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? e.message ?? 'Coverage check failed'),
  })

  const reset = () => { setPatientId(''); setAmount(''); setResult(null); setErr(null) }

  return (
    <Dialog open={open} onClose={() => { onClose(); reset() }} maxWidth="sm" fullWidth>
      <DialogTitle>Coverage Check</DialogTitle>
      <DialogContent dividers>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Enter a patient UUID and the billed amount to verify insurance coverage.
        </Typography>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField label="Patient ID (UUID) *" value={patientId} onChange={e => setPatientId(e.target.value)}
              fullWidth placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Amount (CAD) *" type="number" value={amount}
              onChange={e => setAmount(e.target.value)} fullWidth inputProps={{ min: 0.01, step: '0.01' }} />
          </Grid>
        </Grid>
        {result !== null && (
          <Box sx={{ mt: 2 }}>
            <Divider sx={{ mb: 2 }} />
            <Alert severity={result.covered ? 'success' : 'warning'} sx={{ mb: 1 }}>
              {result.covered ? '✓ Patient is covered for this amount' : '⚠ Coverage insufficient or not found'}
            </Alert>
            <Grid container spacing={1}>
              {result.coverageLimit   !== undefined && <Grid item xs={6}><Typography variant="body2" color="text.secondary">Coverage Limit</Typography><Typography fontWeight={600}>${Number(result.coverageLimit).toLocaleString('en-CA', { minimumFractionDigits: 2 })}</Typography></Grid>}
              {result.coveredAmount   !== undefined && <Grid item xs={6}><Typography variant="body2" color="text.secondary">Covered Amount</Typography><Typography fontWeight={600} color="success.main">${Number(result.coveredAmount).toLocaleString('en-CA', { minimumFractionDigits: 2 })}</Typography></Grid>}
              {result.deductibleAmount !== undefined && <Grid item xs={6}><Typography variant="body2" color="text.secondary">Deductible</Typography><Typography fontWeight={600}>${Number(result.deductibleAmount).toLocaleString('en-CA', { minimumFractionDigits: 2 })}</Typography></Grid>}
              {result.remainingCoverage !== undefined && <Grid item xs={6}><Typography variant="body2" color="text.secondary">Remaining Coverage</Typography><Typography fontWeight={600}>${Number(result.remainingCoverage).toLocaleString('en-CA', { minimumFractionDigits: 2 })}</Typography></Grid>}
              {result.message && <Grid item xs={12}><Typography variant="body2" color="text.secondary">{result.message}</Typography></Grid>}
            </Grid>
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={() => { onClose(); reset() }}>Close</Button>
        <Button variant="contained" startIcon={mutation.isPending ? <CircularProgress size={16} /> : <HealthAndSafetyIcon />}
          disabled={!patientId || !amount || mutation.isPending}
          onClick={() => mutation.mutate()}>
          {mutation.isPending ? 'Checking…' : 'Check Coverage'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── New Policy Dialog ─────────────────────────────────────────────────────────

interface PolicyForm {
  patientId: string; providerId: string; providerName: string
  policyType: PolicyType | ''; coverageStartDate: string; coverageEndDate: string
  deductibleAmount: string; coverageLimit: string; groupNumber: string; memberNumber: string
}
const EMPTY_POLICY: PolicyForm = {
  patientId: '', providerId: '', providerName: '', policyType: '',
  coverageStartDate: '', coverageEndDate: '', deductibleAmount: '',
  coverageLimit: '', groupNumber: '', memberNumber: '',
}

function NewPolicyDialog({ open, onClose, onSaved }: {
  open: boolean; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<PolicyForm>(EMPTY_POLICY)
  const [err, setErr] = useState<string | null>(null)

  const set = (f: keyof PolicyForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const onProviderChange = (id: string) => {
    const prov = PROVIDERS.find(p => p.id === id)
    setForm(p => ({ ...p, providerId: id, providerName: prov?.name ?? '' }))
  }

  const mutation = useMutation({
    mutationFn: (f: PolicyForm) => insuranceApi.post('/api/v1/policies', {
      patientId: f.patientId,
      providerId: f.providerId,
      providerName: f.providerName,
      policyType: f.policyType,
      coverageStartDate: f.coverageStartDate,
      coverageEndDate: f.coverageEndDate,
      deductibleAmount: f.deductibleAmount ? parseFloat(f.deductibleAmount) : undefined,
      coverageLimit: f.coverageLimit ? parseFloat(f.coverageLimit) : undefined,
      groupNumber: f.groupNumber || undefined,
      memberNumber: f.memberNumber || undefined,
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_POLICY); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to create policy'),
  })

  const submit = () => {
    if (!form.patientId || !form.providerId || !form.policyType || !form.coverageStartDate || !form.coverageEndDate) {
      setErr('Patient ID, provider, type and coverage dates are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>New Insurance Policy</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <TextField label="Patient ID (UUID) *" value={form.patientId} onChange={set('patientId')} fullWidth
              placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
          </Grid>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Insurance Provider *</InputLabel>
              <Select label="Insurance Provider *" value={form.providerId} onChange={e => onProviderChange(e.target.value)}>
                {PROVIDERS.map(p => <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Policy Type *</InputLabel>
              <Select label="Policy Type *" value={form.policyType}
                onChange={e => setForm(p => ({ ...p, policyType: e.target.value as PolicyType }))}>
                <MenuItem value="BASIC">Basic</MenuItem>
                <MenuItem value="EXTENDED">Extended</MenuItem>
                <MenuItem value="PREMIUM">Premium</MenuItem>
                <MenuItem value="GOVERNMENT">Government</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <TextField label="Coverage Limit (CAD)" type="number" value={form.coverageLimit}
              onChange={set('coverageLimit')} fullWidth />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Coverage Start *" type="date" value={form.coverageStartDate}
              onChange={set('coverageStartDate')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Coverage End *" type="date" value={form.coverageEndDate}
              onChange={set('coverageEndDate')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Group Number" value={form.groupNumber} onChange={set('groupNumber')} fullWidth />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Member Number" value={form.memberNumber} onChange={set('memberNumber')} fullWidth />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Deductible Amount (CAD)" type="number" value={form.deductibleAmount}
              onChange={set('deductibleAmount')} fullWidth />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Creating…' : 'Create Policy'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Submit Claim Dialog ───────────────────────────────────────────────────────

interface ClaimForm {
  policyId: string; patientId: string; serviceDate: string
  billedAmount: string; diagnosisCodes: string; procedureCodes: string
}
const EMPTY_CLAIM: ClaimForm = {
  policyId: '', patientId: '', serviceDate: '', billedAmount: '', diagnosisCodes: '', procedureCodes: '',
}

function SubmitClaimDialog({ open, onClose, onSaved, policies }: {
  open: boolean; onClose: () => void; onSaved: () => void; policies: InsurancePolicy[]
}) {
  const [form, setForm] = useState<ClaimForm>(EMPTY_CLAIM)
  const [err, setErr] = useState<string | null>(null)

  const set = (f: keyof ClaimForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const onPolicyChange = (pid: string) => {
    const pol = policies.find(p => p.id === pid)
    setForm(p => ({ ...p, policyId: pid, patientId: pol?.patientId ?? '' }))
  }

  const mutation = useMutation({
    mutationFn: (f: ClaimForm) => insuranceApi.post('/api/v1/claims', {
      policyId: f.policyId,
      patientId: f.patientId,
      serviceDate: f.serviceDate,
      billedAmount: parseFloat(f.billedAmount),
      diagnosisCodes: f.diagnosisCodes.split(',').map(s => s.trim()).filter(Boolean),
      procedureCodes: f.procedureCodes.split(',').map(s => s.trim()).filter(Boolean),
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_CLAIM); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to submit claim'),
  })

  const submit = () => {
    if (!form.policyId || !form.serviceDate || !form.billedAmount) {
      setErr('Policy, service date and billed amount are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Submit Insurance Claim</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Policy *</InputLabel>
              <Select label="Policy *" value={form.policyId} onChange={e => onPolicyChange(e.target.value)}>
                {policies.map(p => (
                  <MenuItem key={p.id} value={p.id}>
                    {p.policyNumber} — {p.providerName} ({p.policyType})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField label="Patient ID" value={form.patientId} fullWidth InputProps={{ readOnly: true }}
              placeholder="Auto-filled from policy" />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Service Date *" type="date" value={form.serviceDate}
              onChange={set('serviceDate')} fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Billed Amount (CAD) *" type="number" value={form.billedAmount}
              onChange={set('billedAmount')} fullWidth inputProps={{ min: 0.01, step: '0.01' }} />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Diagnosis Codes (comma-separated)" value={form.diagnosisCodes}
              onChange={set('diagnosisCodes')} fullWidth placeholder="J06.9, M54.5" />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Procedure Codes (comma-separated)" value={form.procedureCodes}
              onChange={set('procedureCodes')} fullWidth placeholder="99213, 93000" />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Submitting…' : 'Submit Claim'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Simple Reason Dialog (Approve / Deny / Appeal) ────────────────────────────

function ReasonDialog({ open, onClose, onConfirm, title, label, pending, requireAmount }: {
  open: boolean; onClose: () => void; onConfirm: (text: string) => void
  title: string; label: string; pending: boolean; requireAmount?: boolean
}) {
  const [val, setVal] = useState('')
  const submit = () => { if (val.trim()) { onConfirm(val); setVal('') } }
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent dividers>
        <TextField label={label} value={val} onChange={e => setVal(e.target.value)}
          type={requireAmount ? 'number' : 'text'}
          inputProps={requireAmount ? { min: 0.01, step: '0.01' } : undefined}
          fullWidth multiline={!requireAmount} rows={requireAmount ? 1 : 3} />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={pending || !val.trim()}>
          {pending ? <CircularProgress size={16} /> : 'Confirm'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

type DialogState = { type: 'approve' | 'deny' | 'appeal'; claimId: string } | null

export default function InsurancePage() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState(0)
  const [policyOpen, setPolicyOpen] = useState(false)
  const [claimOpen, setClaimOpen] = useState(false)
  const [checkOpen, setCheckOpen] = useState(false)
  const [reasonState, setReasonState] = useState<DialogState>(null)
  const [snack, setSnack] = useState('')

  const { data: policyPage } = useQuery<PolicyPage>({
    queryKey: ['insurance-policies'],
    queryFn: () => insuranceApi.get('/api/v1/policies?size=100').then(r => r.data),
  })
  const { data: claimPage } = useQuery<ClaimPage>({
    queryKey: ['insurance-claims'],
    queryFn: () => insuranceApi.get('/api/v1/claims?size=100').then(r => r.data),
  })

  const policies = policyPage?.content ?? []
  const claims   = claimPage?.content ?? []
  const activePolicies = policies.filter(p => p.status === 'ACTIVE').length
  const openClaims     = claims.filter(c => ['SUBMITTED', 'UNDER_REVIEW'].includes(c.status)).length

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['insurance-policies'] })
    queryClient.invalidateQueries({ queryKey: ['insurance-claims'] })
  }

  const reviewMutation = useMutation({
    mutationFn: (id: string) => insuranceApi.put(`/api/v1/claims/${id}/review`),
    onSuccess: () => { refreshAll(); setSnack('Claim moved to Under Review') },
  })
  const approveMutation = useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: number }) =>
      insuranceApi.put(`/api/v1/claims/${id}/approve`, { approvedAmount: amount }),
    onSuccess: () => { refreshAll(); setSnack('Claim approved'); setReasonState(null) },
    onError: (e: any) => setSnack(e.response?.data?.detail ?? 'Failed to approve'),
  })
  const denyMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      insuranceApi.put(`/api/v1/claims/${id}/deny`, { denialReason: reason }),
    onSuccess: () => { refreshAll(); setSnack('Claim denied'); setReasonState(null) },
  })
  const appealMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      insuranceApi.put(`/api/v1/claims/${id}/appeal`, { appealReason: reason }),
    onSuccess: () => { refreshAll(); setSnack('Appeal submitted'); setReasonState(null) },
  })
  const payMutation = useMutation({
    mutationFn: (id: string) => insuranceApi.put(`/api/v1/claims/${id}/pay`),
    onSuccess: () => { refreshAll(); setSnack('Claim payment processed') },
  })

  const handleReason = (text: string) => {
    if (!reasonState) return
    if (reasonState.type === 'approve')
      approveMutation.mutate({ id: reasonState.claimId, amount: parseFloat(text) })
    else if (reasonState.type === 'deny')
      denyMutation.mutate({ id: reasonState.claimId, reason: text })
    else
      appealMutation.mutate({ id: reasonState.claimId, reason: text })
  }

  const policyColumns: Array<GridColDef<InsurancePolicy>> = [
    { field: 'policyNumber', headerName: 'Policy #', width: 140 },
    { field: 'providerName', headerName: 'Provider', flex: 1 },
    { field: 'policyType', headerName: 'Type', width: 110 },
    { field: 'coverageStartDate', headerName: 'Start', width: 110 },
    { field: 'coverageEndDate', headerName: 'End', width: 110 },
    { field: 'coverageLimit', headerName: 'Limit (CAD)', width: 130, valueGetter: p => fmt(p.row.coverageLimit) },
    {
      field: 'status', headerName: 'Status', width: 110,
      renderCell: p => <Chip label={p.row.status} color={POLICY_COLORS[p.row.status]} size="small" />,
    },
  ]

  const claimColumns: Array<GridColDef<InsuranceClaim>> = [
    { field: 'claimNumber', headerName: 'Claim #', width: 140 },
    { field: 'serviceDate', headerName: 'Service Date', width: 115 },
    { field: 'billedAmount', headerName: 'Billed', width: 110, valueGetter: p => fmt(p.row.billedAmount) },
    { field: 'approvedAmount', headerName: 'Approved', width: 115, valueGetter: p => p.row.approvedAmount != null ? fmt(p.row.approvedAmount) : '—' },
    {
      field: 'status', headerName: 'Status', width: 145,
      renderCell: p => <Chip label={p.row.status.replace('_', ' ')} color={CLAIM_COLORS[p.row.status]} size="small" />,
    },
    {
      field: 'actions', headerName: 'Actions', width: 230, sortable: false,
      renderCell: p => {
        const s = p.row.status
        return (
          <Stack direction="row" spacing={0.5} alignItems="center" height="100%">
            {s === 'SUBMITTED' && (
              <Button size="small" color="info" startIcon={<RateReviewIcon />}
                onClick={() => reviewMutation.mutate(p.row.id)} disabled={reviewMutation.isPending}>
                Review
              </Button>
            )}
            {s === 'UNDER_REVIEW' && <>
              <Button size="small" color="success" startIcon={<CheckCircleIcon />}
                onClick={() => setReasonState({ type: 'approve', claimId: p.row.id })}>
                Approve
              </Button>
              <Button size="small" color="error" startIcon={<CancelIcon />}
                onClick={() => setReasonState({ type: 'deny', claimId: p.row.id })}>
                Deny
              </Button>
            </>}
            {(s === 'DENIED' || s === 'PARTIALLY_APPROVED') && (
              <Button size="small" color="warning" startIcon={<GavelIcon />}
                onClick={() => setReasonState({ type: 'appeal', claimId: p.row.id })}>
                Appeal
              </Button>
            )}
            {s === 'APPROVED' && (
              <Button size="small" color="success" startIcon={<AttachMoneyIcon />}
                onClick={() => payMutation.mutate(p.row.id)} disabled={payMutation.isPending}>
                Pay
              </Button>
            )}
          </Stack>
        )
      },
    },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={3}>
        <PageHeader title="Insurance" subtitle={`${activePolicies} active policies · ${openClaims} open claims`} />
        <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
          <Button variant="outlined" color="secondary" startIcon={<HealthAndSafetyIcon />} onClick={() => setCheckOpen(true)}>
            Check Coverage
          </Button>
          <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setClaimOpen(true)}>
            Submit Claim
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setPolicyOpen(true)}>
            New Policy
          </Button>
        </Stack>
      </Stack>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}><StatCard label="Total Policies" value={policyPage?.totalElements ?? '—'} /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Active Policies" value={activePolicies} color="success.main" /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Total Claims" value={claimPage?.totalElements ?? '—'} /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Open Claims" value={openClaims} color={openClaims > 0 ? 'warning.main' : undefined} /></Grid>
      </Grid>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label={`Policies (${policies.length})`} />
        <Tab label={`Claims (${claims.length})`} />
      </Tabs>

      {tab === 0 && (
        <>
          <Stack direction="row" justifyContent="flex-end" mb={1}>
            <Button size="small" variant="outlined" startIcon={<DownloadIcon />}
              onClick={() => downloadCsv(policies.map(p => ({
                'Policy #': p.policyNumber,
                Provider: p.providerName,
                Type: p.policyType,
                'Start Date': p.coverageStartDate,
                'End Date': p.coverageEndDate,
                'Coverage Limit': p.coverageLimit.toFixed(2),
                Status: p.status,
              })), 'northcare-policies')}>
              Export CSV
            </Button>
          </Stack>
          <DataGrid rows={policies} columns={policyColumns} pageSizeOptions={[10, 25]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            autoHeight disableRowSelectionOnClick />
        </>
      )}
      {tab === 1 && (
        <>
          <Stack direction="row" justifyContent="flex-end" mb={1}>
            <Button size="small" variant="outlined" startIcon={<DownloadIcon />}
              onClick={() => downloadCsv(claims.map(c => ({
                'Claim #': c.claimNumber,
                'Service Date': c.serviceDate,
                'Billed (CAD)': c.billedAmount.toFixed(2),
                'Approved (CAD)': c.approvedAmount != null ? c.approvedAmount.toFixed(2) : '',
                Status: c.status,
              })), 'northcare-claims')}>
              Export CSV
            </Button>
          </Stack>
          <DataGrid rows={claims} columns={claimColumns} pageSizeOptions={[10, 25]}
            initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
            autoHeight disableRowSelectionOnClick />
        </>
      )}

      <NewPolicyDialog open={policyOpen} onClose={() => setPolicyOpen(false)}
        onSaved={() => { refreshAll(); setPolicyOpen(false); setSnack('Policy created') }} />

      <SubmitClaimDialog open={claimOpen} onClose={() => setClaimOpen(false)} policies={policies}
        onSaved={() => { refreshAll(); setClaimOpen(false); setSnack('Claim submitted') }} />

      <CoverageCheckDialog open={checkOpen} onClose={() => setCheckOpen(false)} />

      <ReasonDialog
        open={reasonState?.type === 'approve'}
        onClose={() => setReasonState(null)}
        onConfirm={handleReason}
        title="Approve Claim"
        label="Approved Amount (CAD) *"
        pending={approveMutation.isPending}
        requireAmount
      />
      <ReasonDialog
        open={reasonState?.type === 'deny'}
        onClose={() => setReasonState(null)}
        onConfirm={handleReason}
        title="Deny Claim"
        label="Denial Reason *"
        pending={denyMutation.isPending}
      />
      <ReasonDialog
        open={reasonState?.type === 'appeal'}
        onClose={() => setReasonState(null)}
        onConfirm={handleReason}
        title="Submit Appeal"
        label="Appeal Reason *"
        pending={appealMutation.isPending}
      />

      <Snackbar open={!!snack} autoHideDuration={3500} onClose={() => setSnack('')}>
        <Alert severity="success" onClose={() => setSnack('')}>{snack}</Alert>
      </Snackbar>
    </Box>
  )
}


