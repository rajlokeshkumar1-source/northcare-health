import { Box, Card, CardContent, Typography, Alert, Chip, Grid } from '@mui/material'
import PageHeader from '@/components/common/PageHeader'
import { useEurekaApps } from '@/hooks/useEureka'
import LoadingState from '@/components/common/LoadingState'
import ErrorState from '@/components/common/ErrorState'

export default function ServiceHealthPage() {
  const { data: apps, isLoading, error } = useEurekaApps()

  if (isLoading) return <LoadingState message="Loading Eureka registry..." />
  if (error) return <ErrorState error="Failed to load service registry" />

  return (
    <Box>
      <PageHeader title="Service Health" subtitle="Live from Eureka Service Discovery" />
      
      <Alert severity="info" sx={{ mb: 3 }}>
        ℹ️ Each entry represents one running pod. Scale a service and watch new pods appear here automatically.
      </Alert>

      <Grid container spacing={2}>
        {apps?.map((app) => {
          const instances = Array.isArray(app.instance) ? app.instance : [app.instance]
          return (
            <Grid item xs={12} sm={6} md={4} key={app.name}>
              <Card>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 1 }}>{app.name}</Typography>
                  <Chip label={`${instances.length} pods`} size="small" color={instances.some((i: any) => i.status === 'UP') ? 'success' : 'error'} sx={{ mb: 2 }} />
                  <Box sx={{ mt: 2 }}>
                    {instances.map((inst: any, idx: number) => (
                      <Typography key={idx} variant="body2" sx={{ color: inst.status === 'UP' ? 'green' : 'red' }}>
                        • {inst.ipAddr} ({inst.status})
                      </Typography>
                    ))}
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>
    </Box>
  )
}
