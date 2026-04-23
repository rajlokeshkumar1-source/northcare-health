import { useState } from 'react'
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog,
  DialogActions, DialogContent, DialogTitle, FormControl, Grid, InputLabel,
  MenuItem, Select, Snackbar, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import SendIcon from '@mui/icons-material/Send'
import NotificationsIcon from '@mui/icons-material/Notifications'
import WarningAmberIcon from '@mui/icons-material/WarningAmber'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import DescriptionIcon from '@mui/icons-material/Description'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import PageHeader from '@/components/common/PageHeader'
import { notificationsApi } from '@/api/apiClient'

// ── Types ─────────────────────────────────────────────────────────────────────

type NotificationChannel  = 'EMAIL' | 'SMS' | 'IN_APP' | 'PUSH'
type NotificationPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL'
type NotificationStatus   = 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED' | 'READ'
type NotificationType     = 'APPOINTMENT_REMINDER' | 'LAB_RESULT_READY' | 'PRESCRIPTION_READY' | 'EMERGENCY_ALERT' | 'BILLING_DUE' | 'GENERAL'
type RecipientType        = 'PATIENT' | 'DOCTOR' | 'ADMIN' | 'STAFF'

interface NotificationItem {
  id: string; recipientId: string; recipientType: RecipientType; channel: NotificationChannel
  type: NotificationType; subject: string | null; message: string
  priority: NotificationPriority; status: NotificationStatus; sentAt: string | null; createdAt: string
}
interface NotificationTemplate {
  id: string; templateCode: string; type: NotificationType; channel: NotificationChannel
  subject: string | null; body: string; isActive: boolean; createdAt: string
}

const CHANNEL_COLORS: Record<NotificationChannel, 'info' | 'success' | 'warning' | 'secondary'> = {
  EMAIL: 'info', SMS: 'success', IN_APP: 'warning', PUSH: 'secondary',
}
const STATUS_COLORS: Record<NotificationStatus, 'default' | 'info' | 'success' | 'error' | 'primary'> = {
  PENDING: 'default', SENT: 'info', DELIVERED: 'success', FAILED: 'error', READ: 'primary',
}
const PRIORITY_COLORS: Record<NotificationPriority, 'default' | 'warning' | 'error' | 'info'> = {
  LOW: 'default', NORMAL: 'info', HIGH: 'warning', CRITICAL: 'error',
}
const fmtDate = (s: string | null) =>
  s ? new Date(s).toLocaleString('en-CA', { dateStyle: 'medium', timeStyle: 'short' }) : '—'

const SEEDED_RECIPIENTS = [
  '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004',
  '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000006',
]

// ── Stat Card ─────────────────────────────────────────────────────────────────

function StatCard({ icon, label, value, color }: {
  icon: React.ReactNode; label: string; value: string | number; color: string
}) {
  return (
    <Card sx={{ flex: 1, minWidth: 170 }}>
      <CardContent>
        <Stack direction="row" spacing={2} alignItems="center">
          <Box sx={{ color }}>{icon}</Box>
          <Box>
            <Typography variant="h5" fontWeight="bold">{value}</Typography>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

// ── Send Notification Dialog ───────────────────────────────────────────────────

interface SendForm {
  recipientId: string; recipientType: RecipientType | ''
  channel: NotificationChannel | ''; type: NotificationType | ''
  subject: string; message: string; priority: NotificationPriority | ''
}
const EMPTY_SEND: SendForm = {
  recipientId: '', recipientType: '', channel: '', type: '', subject: '', message: '', priority: 'NORMAL',
}

function SendNotificationDialog({ open, onClose, onSaved, prefill }: {
  open: boolean; onClose: () => void; onSaved: () => void
  prefill?: Partial<SendForm>
}) {
  const [form, setForm] = useState<SendForm>({ ...EMPTY_SEND, ...prefill })
  const [err, setErr] = useState<string | null>(null)

  // reset when prefill changes (e.g. from template)
  useState(() => { setForm({ ...EMPTY_SEND, ...prefill }) })

  const set = (f: keyof SendForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const mutation = useMutation({
    mutationFn: (f: SendForm) => notificationsApi.post('/api/v1/notifications', {
      recipientId: f.recipientId, recipientType: f.recipientType,
      channel: f.channel, type: f.type,
      subject: f.subject || undefined, message: f.message,
      priority: f.priority || 'NORMAL',
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_SEND); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? e.response?.data?.message ?? 'Failed to send'),
  })

  const submit = () => {
    if (!form.recipientId || !form.recipientType || !form.channel || !form.type || !form.message) {
      setErr('Recipient ID, type, channel, notification type and message are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Send Notification</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <TextField label="Recipient ID (UUID) *" value={form.recipientId} onChange={set('recipientId')}
              fullWidth placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Recipient Type *</InputLabel>
              <Select label="Recipient Type *" value={form.recipientType}
                onChange={e => setForm(p => ({ ...p, recipientType: e.target.value as RecipientType }))}>
                <MenuItem value="PATIENT">Patient</MenuItem>
                <MenuItem value="DOCTOR">Doctor</MenuItem>
                <MenuItem value="ADMIN">Admin</MenuItem>
                <MenuItem value="STAFF">Staff</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Channel *</InputLabel>
              <Select label="Channel *" value={form.channel}
                onChange={e => setForm(p => ({ ...p, channel: e.target.value as NotificationChannel }))}>
                <MenuItem value="EMAIL">Email</MenuItem>
                <MenuItem value="SMS">SMS</MenuItem>
                <MenuItem value="IN_APP">In-App</MenuItem>
                <MenuItem value="PUSH">Push</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Notification Type *</InputLabel>
              <Select label="Notification Type *" value={form.type}
                onChange={e => setForm(p => ({ ...p, type: e.target.value as NotificationType }))}>
                <MenuItem value="APPOINTMENT_REMINDER">Appointment Reminder</MenuItem>
                <MenuItem value="LAB_RESULT_READY">Lab Result Ready</MenuItem>
                <MenuItem value="PRESCRIPTION_READY">Prescription Ready</MenuItem>
                <MenuItem value="EMERGENCY_ALERT">Emergency Alert</MenuItem>
                <MenuItem value="BILLING_DUE">Billing Due</MenuItem>
                <MenuItem value="GENERAL">General</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Priority</InputLabel>
              <Select label="Priority" value={form.priority}
                onChange={e => setForm(p => ({ ...p, priority: e.target.value as NotificationPriority }))}>
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="NORMAL">Normal</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField label="Subject" value={form.subject} onChange={set('subject')} fullWidth
              placeholder="Email subject line (optional)" />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Message *" value={form.message} onChange={set('message')}
              fullWidth multiline rows={4} placeholder="Notification body…" />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <SendIcon />}>
          {mutation.isPending ? 'Sending…' : 'Send'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function NotificationsPage() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState(0)
  const [sendOpen, setSendOpen] = useState(false)
  const [prefill, setPrefill] = useState<Partial<SendForm>>({})
  const [snack, setSnack] = useState('')

  const { data: allNotifications = [], isLoading: loadingNotifs } = useQuery<NotificationItem[]>({
    queryKey: ['notifications-all'],
    queryFn: async () => {
      const results = await Promise.all(
        SEEDED_RECIPIENTS.map(id =>
          notificationsApi.get<{ content: NotificationItem[] }>(`/api/v1/notifications/recipient/${id}?size=100`)
            .then(r => r.data.content).catch(() => [] as NotificationItem[])
        )
      )
      const seen = new Set<string>()
      return results.flat().filter(n => { if (seen.has(n.id)) return false; seen.add(n.id); return true })
    },
  })

  const { data: templates = [], isLoading: loadingTemplates } = useQuery<NotificationTemplate[]>({
    queryKey: ['notification-templates'],
    queryFn: () => notificationsApi.get('/api/v1/templates').then(r => r.data),
  })

  const pendingCount   = allNotifications.filter(n => n.status === 'PENDING').length
  const criticalCount  = allNotifications.filter(n => n.priority === 'CRITICAL').length
  const deliveredCount = allNotifications.filter(n => ['SENT','DELIVERED','READ'].includes(n.status)).length

  const openSend = (pre: Partial<SendForm> = {}) => { setPrefill(pre); setSendOpen(true) }

  const notifColumns: Array<GridColDef<NotificationItem>> = [
    { field: 'channel', headerName: 'Channel', width: 110,
      renderCell: p => <Chip label={p.row.channel} color={CHANNEL_COLORS[p.row.channel]} size="small" /> },
    { field: 'type', headerName: 'Type', width: 190,
      renderCell: p => <Chip label={p.row.type.replace(/_/g, ' ')} variant="outlined" size="small" /> },
    { field: 'subject', headerName: 'Subject / Message', flex: 1,
      valueGetter: p => p.row.subject ?? p.row.message.slice(0, 60) },
    { field: 'recipientType', headerName: 'Recipient', width: 110,
      valueGetter: p => p.row.recipientType },
    { field: 'priority', headerName: 'Priority', width: 110,
      renderCell: p => <Chip label={p.row.priority} color={PRIORITY_COLORS[p.row.priority]} size="small" /> },
    { field: 'status', headerName: 'Status', width: 120,
      renderCell: p => <Chip label={p.row.status} color={STATUS_COLORS[p.row.status]} size="small" /> },
    { field: 'createdAt', headerName: 'Created', width: 175, valueGetter: p => fmtDate(p.row.createdAt) },
  ]

  const templateColumns: Array<GridColDef<NotificationTemplate>> = [
    { field: 'templateCode', headerName: 'Template Code', width: 220 },
    { field: 'type', headerName: 'Type', width: 190,
      renderCell: p => <Chip label={p.row.type.replace(/_/g, ' ')} variant="outlined" size="small" /> },
    { field: 'channel', headerName: 'Channel', width: 110,
      renderCell: p => <Chip label={p.row.channel} color={CHANNEL_COLORS[p.row.channel]} size="small" /> },
    { field: 'subject', headerName: 'Subject', flex: 1, valueGetter: p => p.row.subject ?? '—' },
    { field: 'isActive', headerName: 'Active', width: 90,
      renderCell: p => <Chip label={p.row.isActive ? 'Yes' : 'No'} color={p.row.isActive ? 'success' : 'default'} size="small" /> },
    { field: 'use', headerName: 'Action', width: 130, sortable: false,
      renderCell: p => (
        <Button size="small" variant="outlined" startIcon={<SendIcon />}
          onClick={() => openSend({ channel: p.row.channel, type: p.row.type, subject: p.row.subject ?? '', message: p.row.body })}>
          Use Template
        </Button>
      ),
    },
  ]

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={3}>
        <PageHeader title="Notifications" subtitle="Alerts, reminders & emergency broadcasts" />
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => openSend()} sx={{ mt: 1 }}>
          Send Notification
        </Button>
      </Stack>

      <Stack direction="row" spacing={2} mb={3} flexWrap="wrap">
        <StatCard icon={<NotificationsIcon fontSize="large" />} label="Pending Alerts"
          value={pendingCount} color="warning.main" />
        <StatCard icon={<WarningAmberIcon fontSize="large" />} label="Critical Priority"
          value={criticalCount} color="error.main" />
        <StatCard icon={<CheckCircleOutlineIcon fontSize="large" />} label="Sent / Delivered"
          value={deliveredCount} color="success.main" />
        <StatCard icon={<DescriptionIcon fontSize="large" />} label="Templates Available"
          value={templates.length} color="info.main" />
      </Stack>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label={`All Notifications (${allNotifications.length})`} />
        <Tab label={`Templates (${templates.length})`} />
      </Tabs>

      {tab === 0 && (
        <DataGrid rows={allNotifications} columns={notifColumns} loading={loadingNotifs}
          autoHeight pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          disableRowSelectionOnClick />
      )}
      {tab === 1 && (
        <DataGrid rows={templates} columns={templateColumns} loading={loadingTemplates}
          autoHeight pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          disableRowSelectionOnClick />
      )}

      <SendNotificationDialog
        open={sendOpen}
        onClose={() => setSendOpen(false)}
        prefill={prefill}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ['notifications-all'] })
          setSendOpen(false)
          setSnack('Notification sent')
        }}
      />

      <Snackbar open={!!snack} autoHideDuration={3500} onClose={() => setSnack('')}>
        <Alert severity="success" onClose={() => setSnack('')}>{snack}</Alert>
      </Snackbar>
    </Box>
  )
}



