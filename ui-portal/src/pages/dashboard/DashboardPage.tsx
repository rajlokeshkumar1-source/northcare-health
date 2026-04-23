import { useMemo } from 'react'
import { Box, Card, CardContent, Chip, CircularProgress, Divider, Grid, LinearProgress, Stack, Tooltip, Typography } from '@mui/material'
import { People, VideoCall, Receipt, HealthAndSafety, LocalHospital, CheckCircle, Warning, ErrorOutline } from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import {
  BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid,
  Tooltip as ReTooltip, ResponsiveContainer, Legend,
} from 'recharts'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader'
import { billingApi, hospitalApi, insuranceApi, telehealthApi } from '@/api/apiClient'

interface PatientPage   { totalElements: number }
interface InvoicePage   { content: Array<{ status: string; totalAmount: number; serviceDate: string }>; totalElements: number }
interface ClaimPage     { content: Array<{ status: string }>; totalElements: number }
interface WardResponse  { bedCount: number; availableBeds: number; name: string; wardType: string }

const PIE_COLORS = ['#1565C0', '#2E7D32', '#F57F17', '#C62828', '#7B1FA2', '#757575']

function LiveStatCard({ label, value, icon, color, loading, sub }: {
  label: string; value: string | number; icon: React.ReactNode
  color: string; loading?: boolean; sub?: string
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
            {loading
              ? <CircularProgress size={24} sx={{ mt: 1 }} />
              : <Typography variant="h4" fontWeight={700} color={color}>{value}</Typography>}
            {sub && <Typography variant="caption" color="text.secondary">{sub}</Typography>}
          </Box>
          <Box sx={{ color, opacity: 0.8 }}>{icon}</Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const { data: patientPage, isLoading: lPat } = useQuery<PatientPage>({
    queryKey: ['dash-patients'],
    queryFn: () => hospitalApi.get('/api/v1/patients?size=1').then(r => r.data),
  })
  const { data: consultations = [], isLoading: lCon } = useQuery<any[]>({
    queryKey: ['dash-consultations'],
    queryFn: () => telehealthApi.get('/api/v1/consultations/today').then(r => r.data),
  })
  const { data: invoicePage, isLoading: lInv } = useQuery<InvoicePage>({
    queryKey: ['dash-invoices'],
    queryFn: () => billingApi.get('/api/v1/invoices?size=100').then(r => r.data),
  })
  const { data: claimPage, isLoading: lClaim } = useQuery<ClaimPage>({
    queryKey: ['dash-claims'],
    queryFn: () => insuranceApi.get('/api/v1/claims?size=100').then(r => r.data),
  })
  const { data: wards = [], isLoading: lWard } = useQuery<WardResponse[]>({
    queryKey: ['dash-wards'],
    queryFn: () => hospitalApi.get('/api/v1/wards').then(r => r.data),
  })

  const invoices    = invoicePage?.content ?? []
  const claims      = claimPage?.content ?? []

  const outstanding = invoices
    .filter(i => ['ISSUED','OVERDUE','PARTIALLY_PAID'].includes(i.status))
    .reduce((s, i) => s + i.totalAmount, 0)
  const fmt = (n: number) => `$${n.toLocaleString('en-CA', { minimumFractionDigits: 0 })}`

  const openClaims = claims.filter(c => ['SUBMITTED','UNDER_REVIEW'].includes(c.status)).length

  const totalBeds     = wards.reduce((s, w) => s + w.bedCount, 0)
  const occupiedBeds  = wards.reduce((s, w) => s + (w.bedCount - w.availableBeds), 0)
  const occupancyPct  = totalBeds > 0 ? Math.round((occupiedBeds / totalBeds) * 100) : 0

  const inProgress   = consultations.filter((c: any) => c.status === 'IN_PROGRESS').length
  const scheduled    = consultations.filter((c: any) => c.status === 'SCHEDULED').length
  const overdueCount = invoices.filter(i => i.status === 'OVERDUE').length

  // Monthly revenue chart data (last 6 months)
  const monthlyRevenue = useMemo(() => {
    const months: Record<string, { month: string; billed: number; paid: number }> = {}
    invoices.forEach(inv => {
      const key = dayjs(inv.serviceDate).format('MMM YY')
      if (!months[key]) months[key] = { month: key, billed: 0, paid: 0 }
      months[key].billed += inv.totalAmount
      if (inv.status === 'PAID') months[key].paid += inv.totalAmount
    })
    return Object.values(months).slice(-6)
  }, [invoices])

  // Invoice status distribution for pie chart
  const statusPieData = useMemo(() => {
    const counts: Record<string, number> = {}
    invoices.forEach(inv => { counts[inv.status] = (counts[inv.status] ?? 0) + 1 })
    return Object.entries(counts).map(([name, value]) => ({ name: name.replace('_', ' '), value }))
  }, [invoices])

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader title="Dashboard" subtitle="NorthCare Health Platform — live overview" />

      {/* Top stat cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <LiveStatCard label="Total Patients" value={patientPage?.totalElements ?? '—'} loading={lPat}
            icon={<People sx={{ fontSize: 40 }} />} color="primary.main"
            sub="registered in system" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <LiveStatCard label="Today's Consultations" value={consultations.length} loading={lCon}
            icon={<VideoCall sx={{ fontSize: 40 }} />} color="secondary.main"
            sub={`${inProgress} in progress · ${scheduled} scheduled`} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <LiveStatCard label="Outstanding (CAD)" value={fmt(outstanding)} loading={lInv}
            icon={<Receipt sx={{ fontSize: 40 }} />} color="warning.main"
            sub={`${overdueCount} overdue invoices`} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <LiveStatCard label="Open Claims" value={openClaims} loading={lClaim}
            icon={<HealthAndSafety sx={{ fontSize: 40 }} />} color="info.main"
            sub={`of ${claims.length} total claims`} />
        </Grid>
      </Grid>

      {/* Ward occupancy */}
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                <LocalHospital sx={{ mr: 1, verticalAlign: 'middle' }} />
                Ward Occupancy
              </Typography>
              {lWard ? <CircularProgress size={20} /> : wards.length === 0
                ? <Typography color="text.secondary">No wards configured</Typography>
                : (
                  <Stack spacing={2}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2" color="text.secondary">
                        {occupiedBeds} / {totalBeds} beds occupied
                      </Typography>
                      <Typography variant="body2" fontWeight={700}
                        color={occupancyPct > 90 ? 'error.main' : occupancyPct > 70 ? 'warning.main' : 'success.main'}>
                        {occupancyPct}%
                      </Typography>
                    </Stack>
                    <LinearProgress variant="determinate" value={occupancyPct}
                      color={occupancyPct > 90 ? 'error' : occupancyPct > 70 ? 'warning' : 'success'} />
                    <Divider />
                    {wards.map(w => (
                      <Stack key={w.name} direction="row" justifyContent="space-between" alignItems="center">
                        <Box>
                          <Typography variant="body2" fontWeight={600}>{w.name}</Typography>
                          <Typography variant="caption" color="text.secondary">{w.wardType}</Typography>
                        </Box>
                        <Tooltip title={`${w.bedCount - w.availableBeds}/${w.bedCount} beds`}>
                          <Chip
                            label={`${w.availableBeds} avail`}
                            size="small"
                            color={w.availableBeds === 0 ? 'error' : w.availableBeds <= 2 ? 'warning' : 'success'}
                          />
                        </Tooltip>
                      </Stack>
                    ))}
                  </Stack>
                )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Billing & Claims Summary</Typography>
              <Stack spacing={2}>
                {[
                  { label: 'Draft Invoices',         value: invoices.filter(i => i.status === 'DRAFT').length,           icon: <Warning color="warning" /> },
                  { label: 'Issued / Outstanding',   value: invoices.filter(i => i.status === 'ISSUED').length,           icon: <Receipt color="info" /> },
                  { label: 'Overdue Invoices',        value: overdueCount,                                                 icon: <ErrorOutline color="error" /> },
                  { label: 'Paid Invoices',           value: invoices.filter(i => i.status === 'PAID').length,             icon: <CheckCircle color="success" /> },
                  { label: 'Claims Under Review',     value: claims.filter(c => c.status === 'UNDER_REVIEW').length,       icon: <HealthAndSafety color="info" /> },
                  { label: 'Approved Claims',         value: claims.filter(c => ['APPROVED','PAID'].includes(c.status)).length, icon: <CheckCircle color="success" /> },
                ].map(row => (
                  <Stack key={row.label} direction="row" justifyContent="space-between" alignItems="center">
                    <Stack direction="row" spacing={1} alignItems="center">
                      {row.icon}
                      <Typography variant="body2">{row.label}</Typography>
                    </Stack>
                    <Typography fontWeight={700}>{lInv || lClaim ? '…' : row.value}</Typography>
                  </Stack>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Revenue Charts */}
      <Grid container spacing={2} sx={{ mt: 0 }}>
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Monthly Revenue (CAD)</Typography>
              {invoices.length === 0
                ? <Typography color="text.secondary">No invoice data yet</Typography>
                : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={monthlyRevenue} margin={{ top: 4, right: 16, left: 0, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="month" tick={{ fontSize: 12 }} />
                      <YAxis tickFormatter={v => `$${(v / 1000).toFixed(1)}k`} tick={{ fontSize: 12 }} />
                      <ReTooltip
                        formatter={(v: number) => [`$${Number(v).toLocaleString('en-CA', { minimumFractionDigits: 2 })}`, '']}
                      />
                      <Legend />
                      <Bar dataKey="billed" name="Total Billed" fill="#1565C0" radius={[3, 3, 0, 0]} />
                      <Bar dataKey="paid"   name="Paid"         fill="#2E7D32" radius={[3, 3, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={5}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Invoice Status Distribution</Typography>
              {statusPieData.length === 0
                ? <Typography color="text.secondary">No invoice data yet</Typography>
                : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={statusPieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        label={({ name, percent }) =>
                          percent > 0.05 ? `${(percent * 100).toFixed(0)}%` : ''
                        }
                      >
                        {statusPieData.map((_e, idx) => (
                          <Cell key={`cell-${idx}`} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                        ))}
                      </Pie>
                      <ReTooltip formatter={(v: number, name: string) => [v, name]} />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
