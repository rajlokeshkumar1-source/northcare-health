import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'
import DashboardPage from '@/pages/dashboard/DashboardPage'
import PatientsPage from '@/pages/patients/PatientsPage'
import TelehealthPage from '@/pages/telehealth/TelehealthPage'
import BillingPage from '@/pages/billing/BillingPage'
import InsurancePage from '@/pages/insurance/InsurancePage'
import NotificationsPage from '@/pages/notifications/NotificationsPage'
import ServiceHealthPage from '@/pages/servicehealth/ServiceHealthPage'
import CalendarPage from '@/pages/calendar/CalendarPage'

export default function App() {
  return (
    <MainLayout>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/patients" element={<PatientsPage />} />
        <Route path="/telehealth" element={<TelehealthPage />} />
        <Route path="/calendar" element={<CalendarPage />} />
        <Route path="/billing" element={<BillingPage />} />
        <Route path="/insurance" element={<InsurancePage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/service-health" element={<ServiceHealthPage />} />
      </Routes>
    </MainLayout>
  )
}
