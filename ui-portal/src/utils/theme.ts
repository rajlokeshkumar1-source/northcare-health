import { createTheme } from '@mui/material/styles'

export const northcareTheme = createTheme({
  palette: {
    primary: { main: '#1565C0' },
    secondary: { main: '#00897B' },
    error: { main: '#C62828' },
    warning: { main: '#F57F17' },
    success: { main: '#2E7D32' },
    background: { default: '#F5F7FA', paper: '#FFFFFF' }
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 }
  },
  shape: { borderRadius: 10 },
  components: {
    MuiCard: { defaultProps: { elevation: 0 }, styleOverrides: { root: { border: '1px solid #E8EDF2' } } },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiChip: { styleOverrides: { root: { fontWeight: 500 } } }
  }
})
