import { request } from '../../../api/client';

/** GET /api/analytics/downtime-trend?plantId&months → TrendResponse */
export const downtimeTrend = ({ plantId, months }) =>
  request({ method: 'GET', url: '/api/analytics/downtime-trend', params: { plantId, months } });

/** GET /api/analytics/top-downtime-machines?plantId&from&to&limit → MachineDowntimeResponse[] */
export const topMachinesByDowntime = ({ plantId, from, to, limit }) =>
  request({ method: 'GET', url: '/api/analytics/top-downtime-machines', params: { plantId, from, to, limit } });

/** GET /api/analytics/pareto?plantId&from&to → ParetoBucketResponse[] */
export const pareto = ({ plantId, from, to }) =>
  request({ method: 'GET', url: '/api/analytics/pareto', params: { plantId, from, to } });

/** GET /api/analytics/line-downtime-share?plantId&from&to → LineDowntimeShareResponse[] */
export const lineDowntimeShare = ({ plantId, from, to }) =>
  request({ method: 'GET', url: '/api/analytics/line-downtime-share', params: { plantId, from, to } });

/** GET /api/analytics/failure-mode-stats?plantId&from&to → FailureModeStatsResponse[] */
export const failureModeStats = ({ plantId, from, to }) =>
  request({ method: 'GET', url: '/api/analytics/failure-mode-stats', params: { plantId, from, to } });

/** GET /api/analytics/part-replacement-intervals?plantId → PartIntervalResponse[] */
export const partReplacementIntervals = (plantId) =>
  request({ method: 'GET', url: '/api/analytics/part-replacement-intervals', params: { plantId } });
