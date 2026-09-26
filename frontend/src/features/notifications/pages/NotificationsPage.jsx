import { useState } from 'react';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { AsyncView, Badge, EmptyState, ErrorMessage, PageHeader, Pagination } from '../../../components/common';
import { listForUser, markAllRead, markRead, unreadCount } from '../services/notificationService';
import { formatDateTime } from '../../../utils/format';

export function NotificationsPage() {
  const { refreshBadges } = usePlant();
  const [page, setPage] = useState(0);
  const notifications = useAsync(() => listForUser({ page, size: 20 }), [page]);
  const unread = useAsync(unreadCount, []);
  const readOne = useMutation(markRead);
  const readAll = useMutation(markAllRead);

  const onRead = async (n) => {
    const result = await readOne.run(n.id);
    if (!result.ok) return;
    notifications.setData((d) => ({ ...d, content: d.content.map((x) => (x.id === n.id ? result.data : x)) }));
    unread.setData((c) => ({ count: Math.max(0, (c?.count ?? 1) - 1) }));
    refreshBadges();
  };

  const onReadAll = async () => {
    const result = await readAll.run();
    if (!result.ok) return;
    notifications.setData((d) => ({ ...d, content: d.content.map((x) => ({ ...x, read: true })) }));
    unread.setData({ count: 0 });
    refreshBadges();
  };

  const unreadTotal = unread.data?.count ?? 0;

  return (
    <>
      <PageHeader
        title="Notifications"
        subtitle={unread.data ? `${unreadTotal} unread` : undefined}
        actions={
          <button type="button" className="btn" onClick={onReadAll} disabled={readAll.pending || unreadTotal === 0}>
            {readAll.pending ? 'Marking…' : 'Mark all as read'}
          </button>
        }
      />
      <ErrorMessage error={readOne.error ?? readAll.error} />
      <div className="card">
        <AsyncView state={notifications} isEmpty={(d) => !d.content?.length} empty={<EmptyState title="You're all caught up" />}>
          {(data) => (
            <>
              <ul className="list">
                {data.content.map((n) => (
                  <li key={n.id} className={n.read ? undefined : 'unread'}>
                    <div className="row between" style={{ alignItems: 'flex-start' }}>
                      <div className="stack" style={{ gap: 4, flex: 1, minWidth: 0 }}>
                        <div className="row">
                          <strong>{n.title}</strong>
                          <Badge>{n.type}</Badge>
                        </div>
                        {n.message && <div className="prose">{n.message}</div>}
                        <span className="subtle">
                          {formatDateTime(n.createdAt)}
                          {n.entityType && ` · ${n.entityType} #${n.entityId}`}
                        </span>
                      </div>
                      {!n.read && (
                        <button type="button" className="btn btn-sm" onClick={() => onRead(n)} disabled={readOne.pending}>
                          Mark read
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
              <Pagination pageData={data} onPageChange={setPage} />
            </>
          )}
        </AsyncView>
      </div>
    </>
  );
}
