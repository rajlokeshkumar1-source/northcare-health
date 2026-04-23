import { Box, Typography, Button, Alert } from '@mui/material'

interface ErrorStateProps {
  error: string
  onRetry?: () => void
}

export default function ErrorState({ error, onRetry }: ErrorStateProps) {
  return (
    <Box sx={{ py: 8 }}>
      <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
      {onRetry && <Button variant="contained" onClick={onRetry}>Retry</Button>}
    </Box>
  )
}
