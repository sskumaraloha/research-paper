import { request } from '../../../api/client';

/** GET /api/schedules?plantId&includeInactive → ScheduleResponse[] */
export const listSchedules = ({ plantId, includeInactive }) =>
  request({ method: 'GET', url: '/api/schedules', params: { plantId, includeInactive } });

/** GET /api/schedules/due?plantId → ScheduleResponse[] */
export const listDue = (plantId) => request({ method: 'GET', url: '/api/schedules/due', params: { plantId } });

/** POST /api/schedules — CreateScheduleRequest → 201 ScheduleResponse */
export const createSchedule = (payload) => request({ method: 'POST', url: '/api/schedules', data: payload });

/** PUT /api/schedules/{scheduleId} — UpdateScheduleRequest → ScheduleResponse */
export const updateSchedule = (scheduleId, payload) =>
  request({ method: 'PUT', url: `/api/schedules/${scheduleId}`, data: payload });

/** POST /api/schedules/{scheduleId}/complete — CompleteScheduleRequest → ScheduleResponse */
export const completeSchedule = (scheduleId, payload) =>
  request({ method: 'POST', url: `/api/schedules/${scheduleId}/complete`, data: payload });
