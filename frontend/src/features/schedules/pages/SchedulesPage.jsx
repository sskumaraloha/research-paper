import { useState } from 'react';
import { Link } from 'react-router-dom';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, Badge, EmptyState, ErrorMessage, FormField, Modal, PageHeader, SuccessMessage, Tabs } from '../../../components/common';
import { completeSchedule, createSchedule, listDue, listSchedules, updateSchedule } from '../services/scheduleService';
import { listFilterOptions } from '../../records/services/recordService';
import { formatDate, todayIso } from '../../../utils/format';

/**
 * Create: CreateScheduleRequest { machineId*, title* (≤150), description (≤1000), intervalDays (1–3650), firstDueOn }
 * Edit:   UpdateScheduleRequest { title, description, intervalDays, nextDueOn, active }
 */
function ScheduleFormModal({ schedule, machines, onClose, onSaved }) {
  const isEdit = Boolean(schedule);
  const form = useContractForm(
    isEdit ? 'UpdateScheduleRequest' : 'CreateScheduleRequest',
    isEdit
      ? {
          title: schedule.title ?? '',
          description: schedule.description ?? '',
          intervalDays: schedule.intervalDays ?? '',
          nextDueOn: schedule.nextDueOn ?? '',
          active: schedule.active ?? true,
        }
      : { machineId: '', title: '', description: '', intervalDays: '', firstDueOn: '' },
  );
  const mutation = useMutation((payload) => (isEdit ? updateSchedule(schedule.id, payload) : createSchedule(payload)));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate(isEdit ? { active: Boolean(form.values.active) } : {});
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) onSaved(result.data, isEdit);
    else form.applyServerErrors(result.error);
  };

  return (
    <Modal title={isEdit ? 'Edit schedule' : 'New maintenance schedule'} onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={mutation.error} title="Could not save schedule" />
        <div className="form-grid">
          {isEdit ? (
            <FormField label="Machine" className="span-2">
              <input className="input" value={schedule.machineName ?? ''} disabled />
            </FormField>
          ) : (
            <FormField label="Machine" required error={form.errors.machineId} className="span-2">
              <select className="input" {...form.bind('machineId')}>
                <option value="">Select a machine</option>
                {machines.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </FormField>
          )}
          <FormField label="Title" required={!isEdit} error={form.errors.title} className="span-2">
            <input className="input" autoFocus {...form.bind('title')} />
          </FormField>
          <FormField label="Interval (days)" error={form.errors.intervalDays} hint="1–3650 days.">
            <input className="input" type="number" {...form.bind('intervalDays')} />
          </FormField>
          {isEdit ? (
            <FormField label="Next due on" error={form.errors.nextDueOn}>
              <input className="input" type="date" {...form.bind('nextDueOn')} />
            </FormField>
          ) : (
            <FormField label="First due on" error={form.errors.firstDueOn}>
              <input className="input" type="date" {...form.bind('firstDueOn')} />
            </FormField>
          )}
          <FormField label="Description" error={form.errors.description} className="span-2">
            <textarea className="input" rows={3} {...form.bind('description')} />
          </FormField>
          {isEdit && (
            <label className="checkbox span-2">
              <input type="checkbox" {...form.bindCheckbox('active')} />
              Active
            </label>
          )}
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
            {mutation.pending ? 'Saving…' : isEdit ? 'Save changes' : 'Create schedule'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

/** CompleteScheduleRequest { performedOn, downtimeMinutes, notes (≤2000), technician (≤100) } */
function CompleteModal({ schedule, onClose, onDone }) {
  const form = useContractForm('CompleteScheduleRequest', { performedOn: todayIso() });
  const mutation = useMutation((payload) => completeSchedule(schedule.id, payload));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) onDone(result.data);
    else form.applyServerErrors(result.error);
  };

  return (
    <Modal title={`Complete: ${schedule.title}`} onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <p className="muted">{schedule.machineName}</p>
        <ErrorMessage error={mutation.error} title="Could not complete schedule" />
        <div className="form-grid">
          <FormField label="Performed on" error={form.errors.performedOn}>
            <input className="input" type="date" max={todayIso()} {...form.bind('performedOn')} />
          </FormField>
          <FormField label="Downtime (minutes)" error={form.errors.downtimeMinutes}>
            <input className="input" type="number" min={0} {...form.bind('downtimeMinutes')} />
          </FormField>
          <FormField label="Technician" error={form.errors.technician} className="span-2">
            <input className="input" {...form.bind('technician')} />
          </FormField>
          <FormField label="Notes" error={form.errors.notes} className="span-2">
            <textarea className="input" rows={3} {...form.bind('notes')} />
          </FormField>
        </div>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
            {mutation.pending ? 'Saving…' : 'Mark complete'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

function SchedulesTable({ schedules, onEdit, onComplete }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Machine</th>
            <th className="num hide-sm">Interval</th>
            <th className="hide-sm">Last performed</th>
            <th>Next due</th>
            <th>Status</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {schedules.map((s) => (
            <tr key={s.id}>
              <td>
                <div style={{ fontWeight: 500 }}>{s.title}</div>
                {s.description && <div className="subtle truncate">{s.description}</div>}
                {s.active === false && <Badge>inactive</Badge>}
              </td>
              <td>
                <Link to={`/machines/${s.machineId}`}>{s.machineName}</Link>
              </td>
              <td className="num hide-sm">{s.intervalDays ? `${s.intervalDays} d` : '—'}</td>
              <td className="hide-sm">{formatDate(s.lastPerformedOn)}</td>
              <td>
                <div>{formatDate(s.nextDueOn)}</div>
                {s.daysUntilDue !== null && s.daysUntilDue !== undefined && (
                  <div className="subtle">
                    {s.daysUntilDue < 0 ? `${Math.abs(s.daysUntilDue)} days overdue` : `in ${s.daysUntilDue} days`}
                  </div>
                )}
              </td>
              <td>
                <Badge>{s.status}</Badge>
              </td>
              <td>
                <div className="actions" style={{ justifyContent: 'flex-end' }}>
                  <button type="button" className="btn btn-sm btn-primary" onClick={() => onComplete(s)}>
                    Complete
                  </button>
                  <button type="button" className="btn btn-sm" onClick={() => onEdit(s)}>
                    Edit
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function SchedulesPage() {
  const { plantId } = usePlant();
  const [tab, setTab] = useState('all');
  const [includeInactive, setIncludeInactive] = useState(false);
  const [editing, setEditing] = useState(undefined); // undefined = closed, null = create, object = edit
  const [completing, setCompleting] = useState(null);
  const [notice, setNotice] = useState(null);

  const enabled = Boolean(plantId);
  const all = useAsync(() => listSchedules({ plantId, includeInactive }), [plantId, includeInactive], { enabled: enabled && tab === 'all' });
  const due = useAsync(() => listDue(plantId), [plantId], { enabled: enabled && tab === 'due' });
  const options = useAsync(() => listFilterOptions(plantId), [plantId], { enabled });
  const current = tab === 'all' ? all : due;

  const refresh = () => (tab === 'all' ? all.reload() : due.reload());

  return (
    <>
      <PageHeader
        title="Maintenance schedules"
        subtitle="Recurring preventive maintenance."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setEditing(null)}>
            New schedule
          </button>
        }
      />
      <SuccessMessage>{notice}</SuccessMessage>
      <PlantGate>
        <div className="card">
          <div className="row between" style={{ paddingRight: 16 }}>
            <Tabs
              value={tab}
              onChange={setTab}
              tabs={[
                { value: 'all', label: 'All schedules' },
                { value: 'due', label: 'Due' },
              ]}
            />
            {tab === 'all' && (
              <label className="checkbox subtle">
                <input type="checkbox" checked={includeInactive} onChange={(e) => setIncludeInactive(e.target.checked)} />
                Include inactive
              </label>
            )}
          </div>
          <AsyncView
            state={current}
            isEmpty={(d) => !d.length}
            empty={<EmptyState title={tab === 'due' ? 'Nothing due' : 'No schedules yet'} />}
          >
            {(data) => <SchedulesTable schedules={data} onEdit={setEditing} onComplete={setCompleting} />}
          </AsyncView>
        </div>
      </PlantGate>
      {editing !== undefined && (
        <ScheduleFormModal
          schedule={editing}
          machines={options.data?.machines ?? []}
          onClose={() => setEditing(undefined)}
          onSaved={(saved, isEdit) => {
            setEditing(undefined);
            setNotice(`Schedule “${saved.title}” ${isEdit ? 'updated' : 'created'}.`);
            refresh();
          }}
        />
      )}
      {completing && (
        <CompleteModal
          schedule={completing}
          onClose={() => setCompleting(null)}
          onDone={(s) => {
            setCompleting(null);
            setNotice(`“${s.title}” completed. Next due ${formatDate(s.nextDueOn)}.`);
            refresh();
          }}
        />
      )}
    </>
  );
}
