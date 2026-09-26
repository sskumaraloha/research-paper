import { request } from '../../../api/client';

/** GET /api/notifications?page&size → PageResponseNotificationResponse */
export const listForUser = ({ page, size } = {}) =>
  request({ method: 'GET', url: '/api/notifications', params: { page, size } });

/** GET /api/notifications/unread-count → CountResponse */
export const unreadCount = () => request({ method: 'GET', url: '/api/notifications/unread-count' });

/** GET /api/notifications/badge-counts?plantId → BadgeCountsResponse */
export const badgeCounts = (plantId) =>
  request({ method: 'GET', url: '/api/notifications/badge-counts', params: { plantId } });

/** POST /api/notifications/{notificationId}/read → NotificationResponse */
export const markRead = (notificationId) =>
  request({ method: 'POST', url: `/api/notifications/${notificationId}/read` });

/** POST /api/notifications/read-all → CountResponse */
export const markAllRead = () => request({ method: 'POST', url: '/api/notifications/read-all' });
