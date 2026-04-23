import { useQuery } from '@tanstack/react-query'
import { eurekaApi } from '@/api/apiClient'

export interface EurekaInstance {
  instanceId: string
  app: string
  ipAddr: string
  port: { $: number }
  status: 'UP' | 'DOWN'
}

export interface EurekaApp {
  name: string
  instance: EurekaInstance | EurekaInstance[]
}

export const useEurekaApps = () => {
  return useQuery({
    queryKey: ['eureka-apps'],
    queryFn: async () => {
      const response = await eurekaApi.get('/eureka/apps', {
        headers: { Accept: 'application/json', Authorization: 'Basic ZXVyZWthLWFkbWluOm5vcnRoY2FyZS1ldXJla2EtMjAyNQ==' }
      })
      return response.data?.applications?.application as EurekaApp[]
    },
    refetchInterval: 15_000,
    retry: false
  })
}
