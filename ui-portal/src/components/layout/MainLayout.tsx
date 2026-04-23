import { Box, AppBar, Badge, Drawer, IconButton, InputAdornment, List, ListItem,
  ListItemButton, ListItemIcon, ListItemText, Paper, Popper, Stack, TextField,
  Toolbar, Tooltip, Typography, useMediaQuery, useTheme } from '@mui/material'
import { useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { LocalHospital, People, VideoCall, Receipt, HealthAndSafety, Notifications,
  Favorite, Menu as MenuIcon, CalendarMonth, Brightness4, Brightness7, Search, Close } from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import { useColorMode } from '@/context/ThemeContext'
import { billingApi, hospitalApi, notificationsApi } from '@/api/apiClient'

const DRAWER_WIDTH = 240

interface MainLayoutProps {
  children: React.ReactNode
}

export default function MainLayout({ children }: MainLayoutProps) {
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const searchAnchorRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const location = useLocation()
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const { mode, toggle } = useColorMode()

  // Real-time notification badge — polls every 30 s
  const { data: pendingNotifs = [] } = useQuery<any[]>({
    queryKey: ['notif-badge'],
    queryFn: () => notificationsApi.get('/api/v1/notifications/pending').then(r => r.data),
    refetchInterval: 30_000,
  })
  const badgeCount = pendingNotifs.length

  // Global search
  const { data: searchResults } = useQuery({
    queryKey: ['global-search', searchQuery],
    queryFn: async () => {
      const [pRes, iRes] = await Promise.allSettled([
        hospitalApi.get('/api/v1/patients?size=100').then(r => r.data.content as any[]),
        billingApi.get('/api/v1/invoices?size=100').then(r => r.data.content as any[]),
      ])
      const q = searchQuery.toLowerCase()
      const patients = pRes.status === 'fulfilled'
        ? pRes.value.filter((p: any) =>
            `${p.firstName} ${p.lastName}`.toLowerCase().includes(q)
          ).slice(0, 4)
        : []
      const invoices = iRes.status === 'fulfilled'
        ? iRes.value.filter((i: any) =>
            (i.patientName ?? '').toLowerCase().includes(q) ||
            (i.invoiceNumber ?? '').toLowerCase().includes(q)
          ).slice(0, 3)
        : []
      return { patients, invoices }
    },
    enabled: searchQuery.length >= 2,
  })

  const hasResults =
    !!searchResults &&
    (searchResults.patients.length > 0 || searchResults.invoices.length > 0)

  const menuItems = [
    { label: 'Dashboard',  path: '/dashboard',  icon: <Favorite /> },
    { label: 'Patients',   path: '/patients',   icon: <People /> },
    { label: 'Telehealth', path: '/telehealth', icon: <VideoCall /> },
    { label: 'Calendar',   path: '/calendar',   icon: <CalendarMonth /> },
    { label: 'Billing',    path: '/billing',    icon: <Receipt /> },
    { label: 'Insurance',  path: '/insurance',  icon: <HealthAndSafety /> },
    {
      label: 'Notifications',
      path: '/notifications',
      icon: (
        <Badge badgeContent={badgeCount || undefined} color="error" max={99}>
          <Notifications />
        </Badge>
      ),
    },
  ]

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: theme.zIndex.drawer + 1 }}>
        <Toolbar>
          {isMobile && (
            <IconButton color="inherit" onClick={() => setDrawerOpen(!drawerOpen)} sx={{ mr: 2 }}>
              <MenuIcon />
            </IconButton>
          )}
          <LocalHospital sx={{ mr: 1 }} />
          {!searchOpen && (
            <Typography variant="h6" sx={{ flexGrow: 1 }}>NorthCare Health Platform</Typography>
          )}
          {searchOpen && <Box sx={{ flexGrow: 1 }} />}

          {/* Global search */}
          <Box ref={searchAnchorRef} sx={{ display: 'flex', alignItems: 'center', mr: 1 }}>
            {searchOpen ? (
              <TextField
                size="small"
                placeholder="Search patients, invoices…"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                autoFocus
                sx={{
                  width: 300,
                  '& .MuiOutlinedInput-root': { bgcolor: 'rgba(255,255,255,0.15)', borderRadius: 2 },
                  '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.3)' },
                  input: { color: 'white' },
                }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <Search sx={{ color: 'rgba(255,255,255,0.7)', fontSize: 20 }} />
                    </InputAdornment>
                  ),
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        sx={{ color: 'rgba(255,255,255,0.7)' }}
                        onClick={() => { setSearchOpen(false); setSearchQuery('') }}
                      >
                        <Close fontSize="small" />
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
            ) : (
              <Tooltip title="Search">
                <IconButton color="inherit" onClick={() => setSearchOpen(true)}>
                  <Search />
                </IconButton>
              </Tooltip>
            )}
          </Box>

          {/* Dark mode toggle */}
          <Tooltip title={mode === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode'}>
            <IconButton color="inherit" onClick={toggle}>
              {mode === 'dark' ? <Brightness7 /> : <Brightness4 />}
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      {/* Search results dropdown */}
      <Popper
        open={hasResults && searchOpen}
        anchorEl={searchAnchorRef.current}
        placement="bottom-end"
        style={{ zIndex: theme.zIndex.modal }}
      >
        <Paper elevation={8} sx={{ mt: 0.5, width: 320, maxHeight: 320, overflow: 'auto' }}>
          {searchResults?.patients.map((p: any) => (
            <ListItemButton
              key={p.id}
              onClick={() => { navigate('/patients'); setSearchOpen(false); setSearchQuery('') }}
              sx={{ py: 0.5 }}
            >
              <People sx={{ mr: 1.5, fontSize: 20, color: 'primary.main' }} />
              <Stack>
                <Typography variant="body2" fontWeight={600}>{p.firstName} {p.lastName}</Typography>
                <Typography variant="caption" color="text.secondary">
                  Patient · {p.gender} · {p.bloodType ?? 'Unknown blood type'}
                </Typography>
              </Stack>
            </ListItemButton>
          ))}
          {searchResults?.invoices.map((i: any) => (
            <ListItemButton
              key={i.id}
              onClick={() => { navigate('/billing'); setSearchOpen(false); setSearchQuery('') }}
              sx={{ py: 0.5 }}
            >
              <Receipt sx={{ mr: 1.5, fontSize: 20, color: 'warning.main' }} />
              <Stack>
                <Typography variant="body2" fontWeight={600}>{i.invoiceNumber}</Typography>
                <Typography variant="caption" color="text.secondary">Invoice · {i.patientName}</Typography>
              </Stack>
            </ListItemButton>
          ))}
        </Paper>
      </Popper>

      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={!isMobile || drawerOpen}
        onClose={() => setDrawerOpen(false)}
        sx={{
          width: DRAWER_WIDTH,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box', mt: 8 },
        }}
      >
        <List>
          {menuItems.map(item => (
            <ListItem key={item.path} disablePadding>
              <ListItemButton
                onClick={() => { navigate(item.path); setDrawerOpen(false) }}
                selected={location.pathname === item.path}
                sx={{ '&.Mui-selected': { bgcolor: 'action.selected' } }}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Drawer>

      <Box sx={{ flexGrow: 1, p: 3, mt: 8 }}>
        {children}
      </Box>
    </Box>
  )
}
