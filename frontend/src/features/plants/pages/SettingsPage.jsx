import { useEffect, useState } from 'react';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { PlantGate } from '../../../layouts/PlantGate';
import { AsyncView, EmptyState, ErrorMessage, FormField, PageHeader, SuccessMessage, Tabs } from '../../../components/common';
import { getSettings, listLines, updateSettings } from '../services/plantService';
import { getDictionary, listFailureModes } from '../../config/services/configService';

/** UpdatePlantSettingsRequest { autoApproveThreshold* (0–1), lowConfidenceThreshold* (0–1), downtimeAlertMinutes* } */
function ThresholdsCard({ plantId }) {
  const settings = useAsync(() => getSettings(plantId), [plantId]);
  const form = useContractForm('UpdatePlantSettingsRequest', {});
  const mutation = useMutation((payload) => updateSettings(plantId, payload));
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const s = settings.data;
    if (s) {
      form.setValues({
        autoApproveThreshold: s.autoApproveThreshold ?? '',
        lowConfidenceThreshold: s.lowConfidenceThreshold ?? '',
        downtimeAlertMinutes: s.downtimeAlertMinutes ?? '',
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settings.data]);

  const onSubmit = async (e) => {
    e.preventDefault();
    setSaved(false);
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) {
      settings.setData(result.data);
      setSaved(true);
    } else form.applyServerErrors(result.error);
  };

  return (
    <div className="card">
      <div className="card-header">
        <h2>Import thresholds & alerts</h2>
        {settings.data?.plantName && <span className="subtle">{settings.data.plantName}</span>}
      </div>
      <AsyncView state={settings}>
        {() => (
          <form className="card-body form" onSubmit={onSubmit} noValidate>
            <ErrorMessage error={mutation.error} title="Could not save settings" />
            {saved && <SuccessMessage>Settings saved.</SuccessMessage>}
            <div className="form-grid">
              <FormField
                label="Auto-approve threshold"
                required
                error={form.errors.autoApproveThreshold}
                hint="Confidence between 0 and 1."
              >
                <input className="input" type="number" step="0.01" {...form.bind('autoApproveThreshold')} />
              </FormField>
              <FormField
                label="Low-confidence threshold"
                required
                error={form.errors.lowConfidenceThreshold}
                hint="Confidence between 0 and 1."
              >
                <input className="input" type="number" step="0.01" {...form.bind('lowConfidenceThreshold')} />
              </FormField>
              <FormField label="Downtime alert (minutes)" required error={form.errors.downtimeAlertMinutes}>
                <input className="input" type="number" min={0} {...form.bind('downtimeAlertMinutes')} />
              </FormField>
            </div>
            <div className="form-actions">
              <button type="submit" className="btn btn-primary" disabled={mutation.pending}>
                {mutation.pending ? 'Saving…' : 'Save settings'}
              </button>
            </div>
          </form>
        )}
      </AsyncView>
    </div>
  );
}

function LinesCard({ plantId }) {
  const lines = useAsync(() => listLines(plantId), [plantId]);
  return (
    <div className="card">
      <div className="card-header">
        <h2>Production lines</h2>
      </div>
      <AsyncView state={lines} isEmpty={(d) => !d.length} empty={<EmptyState title="No lines configured" />}>
        {(list) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                </tr>
              </thead>
              <tbody>
                {list.map((l) => (
                  <tr key={l.id}>
                    <td className="mono">{l.code}</td>
                    <td>{l.name}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AsyncView>
    </div>
  );
}

function FailureModesCard() {
  const modes = useAsync(listFailureModes, []);
  return (
    <div className="card">
      <AsyncView state={modes} isEmpty={(d) => !d.length} empty={<EmptyState title="No failure modes configured" />}>
        {(list) => (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Category</th>
                  <th className="hide-sm">Keywords</th>
                </tr>
              </thead>
              <tbody>
                {list.map((f) => (
                  <tr key={f.id}>
                    <td className="mono">{f.code}</td>
                    <td>{f.name}</td>
                    <td>{f.category ?? '—'}</td>
                    <td className="hide-sm subtle">{f.keywords?.join(', ') || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AsyncView>
    </div>
  );
}

/** getDictionary → map<string, map<string, string[]>>; semantics undocumented, shown as-is. */
function DictionaryCard() {
  const dict = useAsync(getDictionary, []);
  return (
    <div className="card">
      <AsyncView state={dict} isEmpty={(d) => !Object.keys(d ?? {}).length} empty={<EmptyState title="Dictionary is empty" />}>
        {(d) => (
          <div className="card-body stack">
            {Object.entries(d).map(([group, entries]) => (
              <section key={group} className="stack" style={{ gap: 6 }}>
                <h3>{group}</h3>
                <div className="table-wrap">
                  <table className="table">
                    <tbody>
                      {Object.entries(entries ?? {}).map(([term, synonyms]) => (
                        <tr key={term}>
                          <th style={{ width: '30%' }}>{term}</th>
                          <td>{synonyms?.join(', ') || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            ))}
          </div>
        )}
      </AsyncView>
    </div>
  );
}

export function SettingsPage() {
  const { plantId } = usePlant();
  const [tab, setTab] = useState('plant');
  return (
    <>
      <PageHeader title="Settings" subtitle="Plant configuration and reference data." />
      <Tabs
        value={tab}
        onChange={setTab}
        tabs={[
          { value: 'plant', label: 'Plant' },
          { value: 'failure-modes', label: 'Failure modes' },
          { value: 'dictionary', label: 'Dictionary' },
        ]}
      />
      {tab === 'plant' && (
        <PlantGate>
          <div className="grid grid-2" style={{ alignItems: 'start' }}>
            <ThresholdsCard plantId={plantId} />
            <LinesCard plantId={plantId} />
          </div>
        </PlantGate>
      )}
      {tab === 'failure-modes' && <FailureModesCard />}
      {tab === 'dictionary' && <DictionaryCard />}
    </>
  );
}
