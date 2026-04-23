import axios from 'axios'

const createClient = (baseURL: string) => axios.create({
  baseURL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' }
})

export const hospitalApi = createClient('/api/hospital')
export const telehealthApi = createClient('/api/telehealth')
export const billingApi = createClient('/api/billing')
export const insuranceApi = createClient('/api/insurance')
export const notificationsApi = createClient('/api/notifications')
export const eurekaApi = createClient('/api/eureka')
