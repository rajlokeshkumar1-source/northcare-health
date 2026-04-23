import { Card, CardContent, Typography, Box, Chip } from '@mui/material'

interface StatCardProps {
  title: string
  value: string | number
  icon: React.ReactNode
  color?: string
  trend?: { value: number; label: string }
}

export default function StatCard({ title, value, icon, color = 'primary', trend }: StatCardProps) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography color="textSecondary" gutterBottom>{title}</Typography>
          <Box sx={{ color: `${color}.main` }}>{icon}</Box>
        </Box>
        <Typography variant="h5" sx={{ mb: 1 }}>{value}</Typography>
        {trend && <Chip label={`${trend.value > 0 ? '+' : ''}${trend.value}% ${trend.label}`} size="small" color={trend.value > 0 ? 'success' : 'error'} variant="outlined" />}
      </CardContent>
    </Card>
  )
}
