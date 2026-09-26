import { lazy, Suspense } from 'react';
import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from '../layouts/AppLayout';
import { AuthLayout } from '../layouts/AuthLayout';
import { ProtectedRoute, PublicOnlyRoute } from './guards';
import { EmptyState, LoadingState } from '../components/common';

const page = (loader, name) => lazy(() => loader().then((m) => ({ default: m[name] })));

const LoginPage = page(() => import('../features/auth/pages/LoginPage'), 'LoginPage');
const RegisterPage = page(() => import('../features/auth/pages/RegisterPage'), 'RegisterPage');
const ForgotPasswordPage = page(() => import('../features/auth/pages/ForgotPasswordPage'), 'ForgotPasswordPage');
const ResetPasswordPage = page(() => import('../features/auth/pages/ResetPasswordPage'), 'ResetPasswordPage');
const DashboardPage = page(() => import('../features/dashboard/pages/DashboardPage'), 'DashboardPage');
const MachinesListPage = page(() => import('../features/machines/pages/MachinesListPage'), 'MachinesListPage');
const MachineDetailPage = page(() => import('../features/machines/pages/MachineDetailPage'), 'MachineDetailPage');
const MachineFormPage = page(() => import('../features/machines/pages/MachineFormPage'), 'MachineFormPage');
const RecordsListPage = page(() => import('../features/records/pages/RecordsListPage'), 'RecordsListPage');
const RecordDetailPage = page(() => import('../features/records/pages/RecordDetailPage'), 'RecordDetailPage');
const RecordCreatePage = page(() => import('../features/records/pages/RecordCreatePage'), 'RecordCreatePage');
const EntryPage = page(() => import('../features/entry/pages/EntryPage'), 'EntryPage');
const ImportsPage = page(() => import('../features/imports/pages/ImportsPage'), 'ImportsPage');
const ImportJobPage = page(() => import('../features/imports/pages/ImportJobPage'), 'ImportJobPage');
const ValidationQueuePage = page(() => import('../features/validation/pages/ValidationQueuePage'), 'ValidationQueuePage');
const AliasSuggestionsPage = page(() => import('../features/validation/pages/AliasSuggestionsPage'), 'AliasSuggestionsPage');
const SchedulesPage = page(() => import('../features/schedules/pages/SchedulesPage'), 'SchedulesPage');
const InsightsPage = page(() => import('../features/insights/pages/InsightsPage'), 'InsightsPage');
const AnalyticsPage = page(() => import('../features/analytics/pages/AnalyticsPage'), 'AnalyticsPage');
const PartsListPage = page(() => import('../features/parts/pages/PartsListPage'), 'PartsListPage');
const PartDetailPage = page(() => import('../features/parts/pages/PartDetailPage'), 'PartDetailPage');
const SearchPage = page(() => import('../features/search/pages/SearchPage'), 'SearchPage');
const AssistantPage = page(() => import('../features/assistant/pages/AssistantPage'), 'AssistantPage');
const NotificationsPage = page(() => import('../features/notifications/pages/NotificationsPage'), 'NotificationsPage');
const UsersPage = page(() => import('../features/users/pages/UsersPage'), 'UsersPage');
const SettingsPage = page(() => import('../features/plants/pages/SettingsPage'), 'SettingsPage');
const AuditLogPage = page(() => import('../features/audit/pages/AuditLogPage'), 'AuditLogPage');
const PlatformPage = page(() => import('../features/platform/pages/PlatformPage'), 'PlatformPage');
const OrganisationPage = page(() => import('../features/platform/pages/OrganisationPage'), 'OrganisationPage');

function NotFound() {
  return (
    <div className="card">
      <EmptyState title="Page not found" action={<Link to="/dashboard">Go to dashboard</Link>} />
    </div>
  );
}

export function AppRoutes() {
  return (
    <Suspense fallback={<LoadingState />}>
      <Routes>
        <Route element={<PublicOnlyRoute />}>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          </Route>
        </Route>
        {/* Reachable signed in or not: the emailed reset link may be opened in either state. */}
        <Route element={<AuthLayout />}>
          <Route path="/reset-password" element={<ResetPasswordPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/machines" element={<MachinesListPage />} />
            <Route path="/machines/new" element={<MachineFormPage />} />
            <Route path="/machines/:machineId" element={<MachineDetailPage />} />
            <Route path="/machines/:machineId/edit" element={<MachineFormPage />} />
            <Route path="/records" element={<RecordsListPage />} />
            <Route path="/records/new" element={<RecordCreatePage />} />
            <Route path="/records/:recordId" element={<RecordDetailPage />} />
            <Route path="/entry" element={<EntryPage />} />
            <Route path="/entry/:conversationId" element={<EntryPage />} />
            <Route path="/imports" element={<ImportsPage />} />
            <Route path="/imports/:jobId" element={<ImportJobPage />} />
            <Route path="/validation" element={<ValidationQueuePage />} />
            <Route path="/validation/aliases" element={<AliasSuggestionsPage />} />
            <Route path="/schedules" element={<SchedulesPage />} />
            <Route path="/insights" element={<InsightsPage />} />
            <Route path="/analytics" element={<AnalyticsPage />} />
            <Route path="/parts" element={<PartsListPage />} />
            <Route path="/parts/:partId" element={<PartDetailPage />} />
            <Route path="/search" element={<SearchPage />} />
            {/* Optional segment: asking the first question moves to /assistant/:id without remounting the page. */}
            <Route path="/assistant/:conversationId?" element={<AssistantPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/audit" element={<AuditLogPage />} />
            <Route path="/platform" element={<PlatformPage />} />
            <Route path="/platform/organisations/:organisationId" element={<OrganisationPage />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  );
}
