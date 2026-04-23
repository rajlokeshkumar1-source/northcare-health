import { Box, CircularProgress, Typography } from '@mui/material'

interface LoadingStateProps {
  message?: string
}

export default function LoadingState({ message = 'Loading...' }: LoadingStateProps) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', py: 8 }}>
      <CircularProgress sx={{ mb: 2 }} />
      <Typography variant="body2" color="textSecondary">{message}</Typography>
    </Box>
  )
}
