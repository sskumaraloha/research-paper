import { useState } from 'react';
import { useAsync } from '../../../hooks/useAsync';
import { AsyncView, EmptyState, Pagination } from '../../../components/common';
import { RecordsTable } from '../../records/components/RecordsTable';
import { getTimeline } from '../services/machineService';

export function MachineTimeline({ machineId }) {
  const [page, setPage] = useState(0);
  const timeline = useAsync(() => getTimeline(machineId, { page, size: 20 }), [machineId, page]);
  return (
    <AsyncView state={timeline} isEmpty={(d) => !d.content?.length} empty={<EmptyState title="No maintenance records yet" />}>
      {(data) => (
        <>
          <RecordsTable records={data.content} showMachine={false} />
          <Pagination pageData={data} onPageChange={setPage} />
        </>
      )}
    </AsyncView>
  );
}
