import { Box, Typography, Button } from '@mui/material'

interface PageHeaderProps {
  title: string
  subtitle?: string
  action?: { label: string; onClick: () => void }
}

export default function PageHeader({ title, subtitle, action }: PageHeaderProps) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
      <Box>
        <Typography variant="h4" sx={{ mb: 1 }}>{title}</Typography>
        {subtitle && <Typography color="textSecondary">{subtitle}</Typography>}
      </Box>
      {action && <Button variant="contained" onClick={action.onClick}>{action.label}</Button>}
    </Box>
  )
}
