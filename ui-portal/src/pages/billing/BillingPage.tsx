import { useState } from 'react'
import {
  Alert, Box, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogTitle, Divider, FormControl, Grid, IconButton, InputLabel,
  MenuItem, Paper, Select, Snackbar, Stack, Tab, Tabs, TextField, Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import SendIcon from '@mui/icons-material/Send'
import PaymentIcon from '@mui/icons-material/Payment'
import PrintIcon from '@mui/icons-material/Print'
import DownloadIcon from '@mui/icons-material/Download'
import { DataGrid, GridColDef } from '@mui/x-data-grid'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import PageHeader from '@/components/common/PageHeader'
import { billingApi, hospitalApi } from '@/api/apiClient'
import { downloadCsv } from '@/utils/csvExport'

// ── Types ─────────────────────────────────────────────────────────────────────

type InvoiceStatus = 'DRAFT' | 'ISSUED' | 'PAID' | 'PARTIALLY_PAID' | 'OVERDUE' | 'CANCELLED' | 'WRITTEN_OFF'
type PaymentMethod  = 'CREDIT_CARD' | 'BANK_TRANSFER' | 'INSURANCE' | 'CASH' | 'CHEQUE'

interface LineItemResponse { id: string; serviceCode: string; description: string; quantity: number; unitPrice: number; lineTotal: number }
interface InvoiceResponse {
  id: string; invoiceNumber: string; patientId: string; patientName: string
  serviceDate: string; dueDate: string; subtotal: number; taxAmount: number
  totalAmount: number; currency: string; status: InvoiceStatus
  lineItems: LineItemResponse[]; notes: string | null
}
interface InvoicePage { content: InvoiceResponse[]; totalElements: number }
interface PatientBrief { id: string; firstName: string; lastName: string }
interface PatientPage { content: PatientBrief[] }

const STATUS_COLOR: Record<InvoiceStatus, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  DRAFT: 'default', ISSUED: 'info', PAID: 'success',
  PARTIALLY_PAID: 'warning', OVERDUE: 'error', CANCELLED: 'default', WRITTEN_OFF: 'default',
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

// ── Print Invoice Dialog ──────────────────────────────────────────────────────

function PrintInvoiceDialog({ invoice, onClose }: { invoice: InvoiceResponse | null; onClose: () => void }) {
  if (!invoice) return null
  const printFn = () => {
    const win = window.open('', '_blank', 'width=800,height=700')
    if (!win) return
    win.document.write(`
      <html><head><title>Invoice ${invoice.invoiceNumber}</title>
      <style>
        body { font-family: Arial, sans-serif; padding: 32px; color: #111; }
        h1 { color: #1976d2; margin-bottom: 4px; }
        .sub { color: #555; margin-bottom: 24px; font-size: 14px; }
        table { width: 100%; border-collapse: collapse; margin-top: 16px; }
        th { background: #1976d2; color: #fff; padding: 8px 12px; text-align: left; }
        td { padding: 7px 12px; border-bottom: 1px solid #eee; }
        .total-row td { font-weight: bold; background: #f5f5f5; }
        .meta { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 16px; font-size: 14px; }
        .meta .label { color: #666; } .meta .val { font-weight: 600; }
        @media print { button { display: none; } }
      </style></head><body>
      <h1>NorthCare Health</h1>
      <div class="sub">Invoice #${invoice.invoiceNumber} &nbsp;·&nbsp; Status: ${invoice.status}</div>
      <div class="meta">
        <span class="label">Patient</span><span class="val">${invoice.patientName}</span>
        <span class="label">Service Date</span><span class="val">${invoice.serviceDate}</span>
        <span class="label">Due Date</span><span class="val">${invoice.dueDate}</span>
        <span class="label">Currency</span><span class="val">${invoice.currency}</span>
      </div>
      <table>
        <thead><tr><th>Code</th><th>Description</th><th>Qty</th><th>Unit Price</th><th>Total</th></tr></thead>
        <tbody>
          ${invoice.lineItems.map(l => `<tr>
            <td>${l.serviceCode}</td><td>${l.description}</td>
            <td>${l.quantity}</td><td>$${Number(l.unitPrice).toFixed(2)}</td>
            <td>$${Number(l.lineTotal).toFixed(2)}</td>
          </tr>`).join('')}
          <tr class="total-row"><td colspan="4">Subtotal</td><td>$${Number(invoice.subtotal).toFixed(2)}</td></tr>
          <tr class="total-row"><td colspan="4">Tax</td><td>$${Number(invoice.taxAmount).toFixed(2)}</td></tr>
          <tr class="total-row"><td colspan="4"><strong>Total</strong></td><td><strong>$${Number(invoice.totalAmount).toFixed(2)} ${invoice.currency}</strong></td></tr>
        </tbody>
      </table>
      ${invoice.notes ? `<p style="margin-top:16px;color:#555;font-size:13px"><em>Notes: ${invoice.notes}</em></p>` : ''}
      </body></html>
    `)
    win.document.close()
    win.print()
  }

  return (
    <Dialog open={!!invoice} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Invoice {invoice.invoiceNumber}</DialogTitle>
      <DialogContent dividers>
        <Grid container spacing={1}>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Patient</Typography><Typography>{invoice.patientName}</Typography></Grid>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Status</Typography><Chip label={invoice.status.replace('_',' ')} color={STATUS_COLOR[invoice.status]} size="small" /></Grid>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Service Date</Typography><Typography>{invoice.serviceDate}</Typography></Grid>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Due Date</Typography><Typography>{invoice.dueDate}</Typography></Grid>
          <Grid item xs={12}><Typography variant="body2" color="text.secondary">Line Items</Typography>
            {invoice.lineItems.map(l => (
              <Stack key={l.id} direction="row" justifyContent="space-between" sx={{ py: 0.5 }}>
                <Typography variant="body2">{l.description} ×{l.quantity}</Typography>
                <Typography variant="body2" fontWeight={600}>{fmt(l.lineTotal)}</Typography>
              </Stack>
            ))}
          </Grid>
          <Grid item xs={12}><Divider /></Grid>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Subtotal</Typography><Typography>{fmt(invoice.subtotal)}</Typography></Grid>
          <Grid item xs={6}><Typography variant="body2" color="text.secondary">Tax</Typography><Typography>{fmt(invoice.taxAmount)}</Typography></Grid>
          <Grid item xs={12}><Typography variant="h6" fontWeight={700} textAlign="right">Total: {fmt(invoice.totalAmount)} {invoice.currency}</Typography></Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        <Button variant="contained" startIcon={<PrintIcon />} onClick={printFn}>Print / Save PDF</Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Create Invoice Dialog ─────────────────────────────────────────────────────

interface LineItemForm { serviceCode: string; description: string; quantity: number; unitPrice: string }
const EMPTY_LINE: LineItemForm = { serviceCode: '', description: '', quantity: 1, unitPrice: '' }

interface InvoiceForm {
  patientId: string; patientName: string
  serviceDate: string; dueDate: string; notes: string
  lineItems: LineItemForm[]
}
const EMPTY_INVOICE: InvoiceForm = {
  patientId: '', patientName: '', serviceDate: '', dueDate: '', notes: '',
  lineItems: [{ ...EMPTY_LINE }],
}

function CreateInvoiceDialog({ open, onClose, onSaved }: {
  open: boolean; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<InvoiceForm>(EMPTY_INVOICE)
  const [err, setErr] = useState<string | null>(null)

  const { data: patientPage } = useQuery<PatientPage>({
    queryKey: ['patients-brief'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=100').then(r => r.data),
    enabled: open,
  })
  const patients = patientPage?.content ?? []

  const lineTotal = (l: LineItemForm) => l.quantity * (parseFloat(l.unitPrice) || 0)
  const subtotal  = form.lineItems.reduce((s, l) => s + lineTotal(l), 0)

  const addLine    = () => setForm(p => ({ ...p, lineItems: [...p.lineItems, { ...EMPTY_LINE }] }))
  const removeLine = (i: number) => setForm(p => ({ ...p, lineItems: p.lineItems.filter((_, j) => j !== i) }))
  const updateLine = (i: number, f: keyof LineItemForm, v: string | number) =>
    setForm(p => ({ ...p, lineItems: p.lineItems.map((li, j) => j === i ? { ...li, [f]: v } : li) }))

  const onPatientSelect = (id: string) => {
    const pt = patients.find(p => p.id === id)
    setForm(p => ({ ...p, patientId: id, patientName: pt ? `${pt.firstName} ${pt.lastName}` : '' }))
  }

  const mutation = useMutation({
    mutationFn: (f: InvoiceForm) => billingApi.post('/api/v1/invoices', {
      patientId: f.patientId, patientName: f.patientName,
      serviceDate: f.serviceDate, dueDate: f.dueDate, notes: f.notes || undefined,
      lineItems: f.lineItems.map(l => ({
        serviceCode: l.serviceCode, description: l.description,
        quantity: l.quantity, unitPrice: parseFloat(l.unitPrice),
      })),
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_INVOICE); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to create invoice'),
  })

  const submit = () => {
    if (!form.patientId || !form.serviceDate || !form.dueDate) {
      setErr('Patient, service date and due date are required.'); return
    }
    if (form.lineItems.some(l => !l.serviceCode || !l.description || !l.unitPrice)) {
      setErr('All line item fields are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Create Invoice</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel>Patient *</InputLabel>
              <Select label="Patient *" value={form.patientId} onChange={e => onPatientSelect(e.target.value)}>
                {patients.map(pt => <MenuItem key={pt.id} value={pt.id}>{pt.firstName} {pt.lastName}</MenuItem>)}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6}>
            <TextField label="Patient Name *" value={form.patientName} fullWidth InputProps={{ readOnly: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Service Date *" type="date" value={form.serviceDate}
              onChange={e => setForm(p => ({ ...p, serviceDate: e.target.value }))}
              fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={6}>
            <TextField label="Due Date *" type="date" value={form.dueDate}
              onChange={e => setForm(p => ({ ...p, dueDate: e.target.value }))}
              fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>

          {/* Line Items */}
          <Grid item xs={12}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="subtitle2">Line Items</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={addLine}>Add Item</Button>
            </Stack>
            {form.lineItems.map((li, i) => (
              <Grid container spacing={1} key={i} sx={{ mb: 1 }} alignItems="center">
                <Grid item xs={2}>
                  <TextField label="Code *" value={li.serviceCode} size="small" fullWidth
                    onChange={e => updateLine(i, 'serviceCode', e.target.value)}
                    placeholder="CPT code" />
                </Grid>
                <Grid item xs={4}>
                  <TextField label="Description *" value={li.description} size="small" fullWidth
                    onChange={e => updateLine(i, 'description', e.target.value)} />
                </Grid>
                <Grid item xs={2}>
                  <TextField label="Qty *" type="number" value={li.quantity} size="small" fullWidth
                    inputProps={{ min: 1 }} onChange={e => updateLine(i, 'quantity', parseInt(e.target.value) || 1)} />
                </Grid>
                <Grid item xs={2}>
                  <TextField label="Unit Price *" type="number" value={li.unitPrice} size="small" fullWidth
                    inputProps={{ min: 0, step: '0.01' }} onChange={e => updateLine(i, 'unitPrice', e.target.value)} />
                </Grid>
                <Grid item xs={1}>
                  <Typography variant="body2" color="text.secondary" sx={{ pt: 1 }}>
                    {fmt(lineTotal(li))}
                  </Typography>
                </Grid>
                <Grid item xs={1}>
                  <IconButton size="small" color="error" onClick={() => removeLine(i)}
                    disabled={form.lineItems.length === 1}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Grid>
              </Grid>
            ))}
            <Typography variant="body2" align="right" sx={{ mt: 1 }}>
              <strong>Subtotal: {fmt(subtotal)}</strong>
            </Typography>
          </Grid>
          <Grid item xs={12}>
            <TextField label="Notes" value={form.notes}
              onChange={e => setForm(p => ({ ...p, notes: e.target.value }))}
              fullWidth multiline rows={2} />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <AddIcon />}>
          {mutation.isPending ? 'Creating…' : 'Create Invoice'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Record Payment Dialog ─────────────────────────────────────────────────────

interface PaymentForm { amount: string; paymentDate: string; paymentMethod: PaymentMethod | ''; referenceNumber: string }
const EMPTY_PAYMENT: PaymentForm = { amount: '', paymentDate: '', paymentMethod: '', referenceNumber: '' }

function RecordPaymentDialog({ invoice, open, onClose, onSaved }: {
  invoice: InvoiceResponse | null; open: boolean; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<PaymentForm>(EMPTY_PAYMENT)
  const [err, setErr] = useState<string | null>(null)

  const set = (f: keyof PaymentForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm(p => ({ ...p, [f]: e.target.value }))

  const mutation = useMutation({
    mutationFn: (f: PaymentForm) => billingApi.post(`/api/v1/invoices/${invoice!.id}/payments`, {
      amount: parseFloat(f.amount),
      paymentDate: f.paymentDate,
      paymentMethod: f.paymentMethod,
      referenceNumber: f.referenceNumber || undefined,
    }),
    onSuccess: () => { onSaved(); setForm(EMPTY_PAYMENT); setErr(null) },
    onError: (e: any) => setErr(e.response?.data?.detail ?? 'Failed to record payment'),
  })

  const submit = () => {
    if (!form.amount || !form.paymentDate || !form.paymentMethod) {
      setErr('Amount, date and payment method are required.'); return
    }
    mutation.mutate(form)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Record Payment — {invoice?.invoiceNumber}</DialogTitle>
      <DialogContent dividers>
        {err && <Alert severity="error" sx={{ mb: 2 }}>{err}</Alert>}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Total: {fmt(invoice?.totalAmount ?? 0)} · Status: {invoice?.status}
        </Typography>
        <Grid container spacing={2} sx={{ pt: 1 }}>
          <Grid item xs={12}>
            <TextField label="Amount (CAD) *" type="number" value={form.amount} onChange={set('amount')}
              fullWidth inputProps={{ min: 0.01, step: '0.01' }} />
          </Grid>
          <Grid item xs={12}>
            <TextField label="Payment Date *" type="date" value={form.paymentDate} onChange={set('paymentDate')}
              fullWidth InputLabelProps={{ shrink: true }} />
          </Grid>
          <Grid item xs={12}>
            <FormControl fullWidth>
              <InputLabel>Payment Method *</InputLabel>
              <Select label="Payment Method *" value={form.paymentMethod}
                onChange={e => setForm(p => ({ ...p, paymentMethod: e.target.value as PaymentMethod }))}>
                <MenuItem value="CREDIT_CARD">Credit Card</MenuItem>
                <MenuItem value="BANK_TRANSFER">Bank Transfer</MenuItem>
                <MenuItem value="INSURANCE">Insurance</MenuItem>
                <MenuItem value="CASH">Cash</MenuItem>
                <MenuItem value="CHEQUE">Cheque</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12}>
            <TextField label="Reference Number (optional)" value={form.referenceNumber} onChange={set('referenceNumber')} fullWidth
              placeholder="Transaction ID, cheque #, etc." />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" color="success" onClick={submit} disabled={mutation.isPending}
          startIcon={mutation.isPending ? <CircularProgress size={16} /> : <PaymentIcon />}>
          {mutation.isPending ? 'Recording…' : 'Record Payment'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function BillingPage() {
  const queryClient = useQueryClient()
  const [tab, setTab] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [payInvoice, setPayInvoice] = useState<InvoiceResponse | null>(null)
  const [printInvoice, setPrintInvoice] = useState<InvoiceResponse | null>(null)
  const [snack, setSnack] = useState('')

  const { data: invoicePage } = useQuery<InvoicePage>({
    queryKey: ['invoices'],
    queryFn: () => billingApi.get('/api/v1/invoices?size=100').then(r => r.data),
  })
  const { data: overdue = [] } = useQuery<InvoiceResponse[]>({
    queryKey: ['invoices-overdue'],
    queryFn: () => billingApi.get('/api/v1/invoices/overdue').then(r => r.data),
  })

  const invoices = invoicePage?.content ?? []
  const outstanding = invoices.filter(i => !['PAID','CANCELLED','WRITTEN_OFF'].includes(i.status))
    .reduce((s, i) => s + i.totalAmount, 0)

  const issueMutation = useMutation({
    mutationFn: (id: string) => billingApi.put(`/api/v1/invoices/${id}/issue`),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['invoices'] }); setSnack('Invoice issued') },
    onError: (e: any) => setSnack(e.response?.data?.detail ?? 'Failed to issue invoice'),
  })

  const columns: Array<GridColDef<InvoiceResponse>> = [
    { field: 'invoiceNumber', headerName: 'Invoice #', width: 145 },
    { field: 'patientName', headerName: 'Patient', flex: 1 },
    { field: 'serviceDate', headerName: 'Service', width: 110 },
    { field: 'dueDate', headerName: 'Due', width: 110 },
    { field: 'totalAmount', headerName: 'Total (CAD)', width: 125, valueGetter: p => fmt(p.row.totalAmount) },
    {
      field: 'status', headerName: 'Status', width: 135,
      renderCell: p => <Chip label={p.row.status.replace('_', ' ')} color={STATUS_COLOR[p.row.status]} size="small" />,
    },
    {
      field: 'actions', headerName: 'Actions', width: 270, sortable: false,
      renderCell: p => (
        <Stack direction="row" spacing={0.5} alignItems="center" height="100%">
          {p.row.status === 'DRAFT' && (
            <Button size="small" color="info" startIcon={<SendIcon />}
              onClick={() => issueMutation.mutate(p.row.id)}
              disabled={issueMutation.isPending}>
              Issue
            </Button>
          )}
          {['ISSUED', 'PARTIALLY_PAID', 'OVERDUE'].includes(p.row.status) && (
            <Button size="small" color="success" startIcon={<PaymentIcon />}
              onClick={() => setPayInvoice(p.row)}>
              Pay
            </Button>
          )}
          <Button size="small" color="inherit" startIcon={<PrintIcon />}
            onClick={() => setPrintInvoice(p.row)}>
            Print
          </Button>
        </Stack>
      ),
    },
  ]

  const refreshAll = () => {
    queryClient.invalidateQueries({ queryKey: ['invoices'] })
    queryClient.invalidateQueries({ queryKey: ['invoices-overdue'] })
  }

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={3}>
        <PageHeader title="Billing" subtitle={`${invoicePage?.totalElements ?? 0} invoices · ${overdue.length} overdue`} />
        <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
          <Button variant="outlined" startIcon={<DownloadIcon />}
            onClick={() => downloadCsv(invoices.map(i => ({
              'Invoice #': i.invoiceNumber,
              Patient: i.patientName,
              'Service Date': i.serviceDate,
              'Due Date': i.dueDate,
              'Total (CAD)': i.totalAmount.toFixed(2),
              Status: i.status,
              Notes: i.notes ?? '',
            })), 'northcare-invoices')}>
            Export CSV
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Create Invoice
          </Button>
        </Stack>
      </Stack>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}><StatCard label="Total Invoices" value={invoicePage?.totalElements ?? '—'} /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Outstanding (CAD)" value={fmt(outstanding)} color="warning.main" /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Overdue" value={overdue.length} color={overdue.length > 0 ? 'error.main' : undefined} /></Grid>
        <Grid item xs={6} sm={3}><StatCard label="Paid" value={invoices.filter(i => i.status === 'PAID').length} color="success.main" /></Grid>
      </Grid>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label={`All Invoices (${invoices.length})`} />
        <Tab label={`Overdue (${overdue.length})`} />
      </Tabs>

      {tab === 0 && (
        <DataGrid rows={invoices} columns={columns} pageSizeOptions={[10, 25, 50]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          autoHeight disableRowSelectionOnClick />
      )}
      {tab === 1 && (
        <DataGrid rows={overdue} columns={columns} pageSizeOptions={[10, 25]}
          initialState={{ pagination: { paginationModel: { pageSize: 10 } } }}
          autoHeight disableRowSelectionOnClick />
      )}

      <CreateInvoiceDialog open={createOpen} onClose={() => setCreateOpen(false)}
        onSaved={() => { refreshAll(); setCreateOpen(false); setSnack('Invoice created') }} />

      <RecordPaymentDialog invoice={payInvoice} open={!!payInvoice}
        onClose={() => setPayInvoice(null)}
        onSaved={() => { refreshAll(); setPayInvoice(null); setSnack('Payment recorded') }} />

      <PrintInvoiceDialog invoice={printInvoice} onClose={() => setPrintInvoice(null)} />

      <Snackbar open={!!snack} autoHideDuration={3500} onClose={() => setSnack('')}>
        <Alert severity="success" onClose={() => setSnack('')}>{snack}</Alert>
      </Snackbar>
    </Box>
  )
}



