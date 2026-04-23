import { createContext, useContext, useState, useMemo } from 'react'
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material'

type ColorMode = 'light' | 'dark'

interface ThemeContextValue {
  mode: ColorMode
  toggle: () => void
}

const ThemeCtx = createContext<ThemeContextValue>({ mode: 'light', toggle: () => {} })

export const useColorMode = () => useContext(ThemeCtx)

function makeTheme(mode: ColorMode) {
  return createTheme({
    palette: {
      mode,
      primary: { main: '#1565C0' },
      secondary: { main: '#00897B' },
      error: { main: '#C62828' },
      warning: { main: '#F57F17' },
      success: { main: '#2E7D32' },
      background: mode === 'light'
        ? { default: '#F5F7FA', paper: '#FFFFFF' }
        : { default: '#0D1117', paper: '#161B22' },
    },
    typography: {
      fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
      h4: { fontWeight: 700 },
      h5: { fontWeight: 600 },
      h6: { fontWeight: 600 },
    },
    shape: { borderRadius: 10 },
    components: {
      MuiCard: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: ({ theme }) => ({ border: `1px solid ${theme.palette.divider}` }),
        },
      },
      MuiButton: { defaultProps: { disableElevation: true } },
      MuiChip: { styleOverrides: { root: { fontWeight: 500 } } },
    },
  })
}

export function AppThemeProvider({ children }: { children: React.ReactNode }) {
  const stored =
    typeof window !== 'undefined'
      ? (localStorage.getItem('nc-color-mode') as ColorMode | null)
      : null
  const [mode, setMode] = useState<ColorMode>(stored ?? 'light')

  const toggle = () => {
    setMode(prev => {
      const next = prev === 'light' ? 'dark' : 'light'
      localStorage.setItem('nc-color-mode', next)
      return next
    })
  }

  const theme = useMemo(() => makeTheme(mode), [mode])

  return (
    <ThemeCtx.Provider value={{ mode, toggle }}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </ThemeCtx.Provider>
  )
}
