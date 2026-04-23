import { useState } from 'react'
import {
  Box, Button, Card, CardContent, Chip, FormControl, Grid, IconButton,
  InputLabel, MenuItem, Select, Stack, Tab, Tabs, Tooltip, Typography,
} from '@mui/material'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import TodayIcon from '@mui/icons-material/Today'
import CalendarMonthIcon from '@mui/icons-material/CalendarMonth'
import GroupIcon from '@mui/icons-material/Group'
import { useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import PageHeader from '@/components/common/PageHeader'
import { telehealthApi } from '@/api/apiClient'

// ── Types ─────────────────────────────────────────────────────────────────────

interface ConsultationEvent {
  id: string
  patientId: string
  doctorId: string
  doctorName: string
  scheduledAt: string
  status: string
  consultationType: string
  chiefComplaint: string
}
interface ConsultPage { content: ConsultationEvent[]; totalElements: number }

// ── Constants ─────────────────────────────────────────────────────────────────

const DOCTORS = [
  { id: '10000000-0000-0000-0000-000000000001', name: 'Dr. Sarah Chen',      color: '#1976D2' },
  { id: '10000000-0000-0000-0000-000000000002', name: 'Dr. James Wilson',    color: '#388E3C' },
  { id: '10000000-0000-0000-0000-000000000003', name: 'Dr. Maria Rodriguez', color: '#7B1FA2' },
  { id: '10000000-0000-0000-0000-000000000004', name: 'Dr. Robert Kim',      color: '#E65100' },
]

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
  SCHEDULED: 'warning', IN_PROGRESS: 'info', COMPLETED: 'success',
  CANCELLED: 'error', NO_SHOW: 'error',
}

function doctorColor(doctorId: string): string {
  return DOCTORS.find(d => d.id === doctorId)?.color ?? '#757575'
}

// ── Calendar Page ─────────────────────────────────────────────────────────────

export default function CalendarPage() {
  const [weekStart, setWeekStart] = useState(() => dayjs().startOf('week'))
  const [doctorFilter, setDoctorFilter] = useState('')
  const [viewTab, setViewTab] = useState(0)

  const { data: scheduledPage } = useQuery<ConsultPage>({
    queryKey: ['cal-scheduled'],
    queryFn: () =>
      telehealthApi.get('/api/v1/consultations/status/SCHEDULED?size=200').then(r => r.data),
    staleTime: 60_000,
  })

  const { data: todayList = [] } = useQuery<ConsultationEvent[]>({
    queryKey: ['cal-today'],
    queryFn: () => telehealthApi.get('/api/v1/consultations/today').then(r => r.data),
    staleTime: 30_000,
  })

  const scheduledList = scheduledPage?.content ?? []
  const allConsultations: ConsultationEvent[] = [
    ...todayList,
    ...scheduledList.filter(s => !todayList.find(t => t.id === s.id)),
  ]

  const filtered = doctorFilter
    ? allConsultations.filter(c => c.doctorId === doctorFilter)
    : allConsultations

  const weekDays = Array.from({ length: 7 }, (_, i) => weekStart.add(i, 'day'))

  const consultationsForDay = (day: dayjs.Dayjs) =>
    filtered
      .filter(c => dayjs(c.scheduledAt).isSame(day, 'day'))
      .sort((a, b) => dayjs(a.scheduledAt).diff(dayjs(b.scheduledAt)))

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" mb={2} flexWrap="wrap" gap={2}>
        <PageHeader
          title="Appointment Calendar"
          subtitle={`${allConsultations.length} consultations loaded`}
        />
        <FormControl size="small" sx={{ minWidth: 220 }}>
          <InputLabel>Filter by Doctor</InputLabel>
          <Select
            label="Filter by Doctor"
            value={doctorFilter}
            onChange={e => setDoctorFilter(e.target.value)}
          >
            <MenuItem value=""><em>All Doctors</em></MenuItem>
            {DOCTORS.map(d => (
              <MenuItem key={d.id} value={d.id}>{d.name}</MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      <Tabs value={viewTab} onChange={(_, v) => setViewTab(v)} sx={{ mb: 2 }}>
        <Tab label="Week View" icon={<CalendarMonthIcon />} iconPosition="start" />
        <Tab label="By Doctor" icon={<GroupIcon />} iconPosition="start" />
      </Tabs>

      {/* ── WEEK VIEW ────────────────────────────────────────────────────── */}
      {viewTab === 0 && (
        <>
          <Stack direction="row" alignItems="center" spacing={1} mb={2}>
            <Tooltip title="Previous week">
              <IconButton onClick={() => setWeekStart(w => w.subtract(1, 'week'))}>
                <ChevronLeftIcon />
              </IconButton>
            </Tooltip>
            <Typography variant="h6" sx={{ minWidth: 240, textAlign: 'center' }}>
              {weekStart.format('MMM D')} – {weekStart.add(6, 'day').format('MMM D, YYYY')}
            </Typography>
            <Tooltip title="Next week">
              <IconButton onClick={() => setWeekStart(w => w.add(1, 'week'))}>
                <ChevronRightIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="This week">
              <IconButton onClick={() => setWeekStart(dayjs().startOf('week'))}>
                <TodayIcon />
              </IconButton>
            </Tooltip>
          </Stack>

          <Grid container spacing={1}>
            {weekDays.map(day => {
              const dayConsults = consultationsForDay(day)
              const isToday = day.isSame(dayjs(), 'day')
              return (
                <Grid item xs key={day.format('YYYY-MM-DD')}>
                  <Box sx={{ minHeight: 400 }}>
                    <Box
                      sx={{
                        p: 1, textAlign: 'center', mb: 1, borderRadius: 1,
                        bgcolor: isToday ? 'primary.main' : 'action.hover',
                        color: isToday ? 'white' : 'text.primary',
                      }}
                    >
                      <Typography variant="caption" display="block" sx={{ opacity: 0.8 }}>
                        {day.format('ddd')}
                      </Typography>
                      <Typography variant="body1" fontWeight={700}>{day.format('D')}</Typography>
                    </Box>

                    <Stack spacing={0.5}>
                      {dayConsults.map(c => (
                        <Tooltip
                          key={c.id}
                          title={`${c.chiefComplaint} · ${c.doctorName} · ${c.consultationType}`}
                        >
                          <Card
                            variant="outlined"
                            sx={{ borderLeft: `4px solid ${doctorColor(c.doctorId)}`, cursor: 'default' }}
                          >
                            <CardContent sx={{ p: '4px 8px !important' }}>
                              <Typography variant="caption" fontWeight={600} display="block">
                                {dayjs(c.scheduledAt).format('HH:mm')}
                              </Typography>
                              <Typography
                                variant="caption"
                                color="text.secondary"
                                display="block"
                                noWrap
                                sx={{ fontSize: '0.65rem' }}
                              >
                                {c.doctorName.replace('Dr. ', 'Dr.')}
                              </Typography>
                              <Chip
                                label={c.status.replace('_', ' ')}
                                size="small"
                                color={STATUS_COLOR[c.status] ?? 'default'}
                                sx={{ height: 16, fontSize: '0.6rem', '& .MuiChip-label': { px: '4px' } }}
                              />
                            </CardContent>
                          </Card>
                        </Tooltip>
                      ))}
                      {dayConsults.length === 0 && (
                        <Typography
                          variant="caption"
                          color="text.disabled"
                          sx={{ textAlign: 'center', pt: 3, display: 'block' }}
                        >
                          —
                        </Typography>
                      )}
                    </Stack>
                  </Box>
                </Grid>
              )
            })}
          </Grid>
        </>
      )}

      {/* ── BY DOCTOR ────────────────────────────────────────────────────── */}
      {viewTab === 1 && (
        <Grid container spacing={2}>
          {DOCTORS.map(doctor => {
            const dConsults = allConsultations.filter(c => c.doctorId === doctor.id)
            const upcoming = dConsults
              .filter(c => c.status === 'SCHEDULED')
              .sort((a, b) => dayjs(a.scheduledAt).diff(dayjs(b.scheduledAt)))
            const todayCount = dConsults.filter(c =>
              dayjs(c.scheduledAt).isSame(dayjs(), 'day')
            ).length
            return (
              <Grid item xs={12} sm={6} key={doctor.id}>
                <Card sx={{ borderLeft: `4px solid ${doctor.color}`, height: '100%' }}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
                      <Typography variant="h6">{doctor.name}</Typography>
                      <Stack direction="row" spacing={0.5}>
                        <Chip label={`${todayCount} today`}    size="small" color="info" />
                        <Chip label={`${upcoming.length} upcoming`} size="small" color="warning" />
                      </Stack>
                    </Stack>

                    {upcoming.length === 0 ? (
                      <Typography variant="body2" color="text.secondary">
                        No upcoming consultations
                      </Typography>
                    ) : (
                      <Stack spacing={0.5}>
                        {upcoming.slice(0, 6).map(c => (
                          <Stack key={c.id} direction="row" spacing={1} alignItems="center" sx={{ py: 0.3 }}>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              sx={{ minWidth: 90, fontVariantNumeric: 'tabular-nums' }}
                            >
                              {dayjs(c.scheduledAt).format('MMM D HH:mm')}
                            </Typography>
                            <Chip label={c.consultationType} size="small" variant="outlined" />
                            <Typography variant="caption" color="text.secondary" noWrap>
                              {c.chiefComplaint}
                            </Typography>
                          </Stack>
                        ))}
                        {upcoming.length > 6 && (
                          <Typography variant="caption" color="text.secondary">
                            +{upcoming.length - 6} more
                          </Typography>
                        )}
                      </Stack>
                    )}
                  </CardContent>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}

      {/* Doctor color legend */}
      <Stack direction="row" spacing={2} mt={3} flexWrap="wrap">
        {DOCTORS.map(d => (
          <Stack key={d.id} direction="row" spacing={0.5} alignItems="center">
            <Box sx={{ width: 12, height: 12, borderRadius: '50%', bgcolor: d.color }} />
            <Typography variant="caption" color="text.secondary">{d.name}</Typography>
          </Stack>
        ))}
      </Stack>
    </Box>
  )
}
