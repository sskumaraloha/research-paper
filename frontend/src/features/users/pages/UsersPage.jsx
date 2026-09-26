import { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
import { usePlant } from '../../../context/PlantContext';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { useContractForm } from '../../../hooks/useContractForm';
import { AsyncView, Badge, ConfirmDialog, EmptyState, ErrorMessage, FormField, Modal, PageHeader, SuccessMessage } from '../../../components/common';
import { createUser, deactivateUser, listRoles, listUsers, updateUser } from '../services/userService';

/**
 * Create: CreateUserRequest { fullName* (≤100), email* (≤150), password* (8–100), role*, phoneNumber (\+?[0-9]{8,15}), plantIds[] }
 * Edit:   UpdateUserRequest { fullName, role, phoneNumber, plantIds[], active }
 */
function UserFormModal({ user, roles, plants, onClose, onSaved }) {
  const isEdit = Boolean(user);
  const form = useContractForm(
    isEdit ? 'UpdateUserRequest' : 'CreateUserRequest',
    isEdit
      ? {
          fullName: user.fullName ?? '',
          role: user.role ?? '',
          phoneNumber: user.phoneNumber ?? '',
          plantIds: (user.plantIds ?? []).map(String),
          active: user.active ?? true,
        }
      : { fullName: '', email: '', password: '', role: '', phoneNumber: '', plantIds: [] },
  );
  const mutation = useMutation((payload) => (isEdit ? updateUser(user.id, payload) : createUser(payload)));

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate(isEdit ? { active: Boolean(form.values.active) } : {});
    if (!payload) return;
    // An edit that clears all plants must send [], not omit the field.
    if (isEdit && !payload.plantIds) payload.plantIds = [];
    const result = await mutation.run(payload);
    if (result.ok) onSaved(result.data, isEdit);
    else form.applyServerErrors(result.error);
  };

  return (
    <Modal title={isEdit ? `Edit ${user.fullName}` : 'Add user'} onClose={onClose}>
      <form className="form" onSubmit={onSubmit} noValidate>
        <ErrorMessage error={mutation.error} title="Could not save user" />
        <div className="form-grid">
          <FormField label="Full name" required={!isEdit} error={form.errors.fullName} className="span-2">
            <input className="input" autoFocus {...form.bind('fullName')} />
          </FormField>
          {isEdit ? (
            <FormField label="Email" hint="Email addresses cannot be changed." className="span-2">
              <input className="input" value={user.email ?? ''} disabled />
            </FormField>
          ) : (
            <>
              <FormField label="Email" required error={form.errors.email}>
                <input className="input" type="email" autoComplete="off" {...form.bind('email')} />
              </FormField>
              <FormField label="Initial password" required error={form.errors.password} hint="At least 8 characters.">
                <input className="input" type="password" autoComplete="new-password" {...form.bind('password')} />
              </FormField>
            </>
          )}
          <FormField label="Role" required={!isEdit} error={form.errors.role}>
            <select className="input" {...form.bind('role')}>
              <option value="">Select a role</option>
              {roles.map((r) => (
                <option key={r.name} value={r.name} title={r.description}>
                  {r.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Phone number" error={form.errors.phoneNumber} hint="Optional. 8–15 digits.">
            <input className="input" type="tel" {...form.bind('phoneNumber')} />
          </FormField>
          <FormField label="Plants" error={form.errors.plantIds} className="span-2" hint="Hold Ctrl/⌘ to select several.">
            <select
              className="input"
              multiple
              value={form.values.plantIds}
              onChange={(e) => form.set('plantIds', [...e.target.selectedOptions].map((o) => o.value))}
            >
              {plants.map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.name}
                </option>
              ))}
            </select>
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
            {mutation.pending ? 'Saving…' : isEdit ? 'Save changes' : 'Create user'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export function UsersPage() {
  const { user: me } = useAuth();
  const { plants } = usePlant();
  const users = useAsync(listUsers, []);
  const roles = useAsync(listRoles, []);
  const [editing, setEditing] = useState(undefined); // undefined closed, null create, object edit
  const [deactivating, setDeactivating] = useState(null);
  const [notice, setNotice] = useState(null);
  const deactivate = useMutation((id) => deactivateUser(id));

  const replace = (saved) => users.setData((list) => (list.some((u) => u.id === saved.id) ? list.map((u) => (u.id === saved.id ? saved : u)) : [...list, saved]));
  const plantName = (id) => plants.find((p) => p.id === id)?.name ?? `#${id}`;

  const onDeactivate = async () => {
    const result = await deactivate.run(deactivating.id);
    if (!result.ok) return;
    replace(result.data);
    setNotice(`${result.data.fullName} was deactivated.`);
    setDeactivating(null);
  };

  return (
    <>
      <PageHeader
        title="Users"
        subtitle="People with access to this organisation."
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setEditing(null)}>
            Add user
          </button>
        }
      />
      <SuccessMessage>{notice}</SuccessMessage>
      <ErrorMessage error={roles.error} title="Could not load roles" onRetry={roles.reload} />
      <div className="card">
        <AsyncView state={users} isEmpty={(d) => !d.length} empty={<EmptyState title="No users" />}>
          {(list) => (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th className="hide-sm">Phone</th>
                    <th>Role</th>
                    <th className="hide-sm">Plants</th>
                    <th>Status</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {list.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <div style={{ fontWeight: 500 }}>{u.fullName}</div>
                        <div className="subtle">{u.email}</div>
                      </td>
                      <td className="hide-sm">{u.phoneNumber ?? '—'}</td>
                      <td>
                        <Badge>{u.role}</Badge>
                      </td>
                      <td className="hide-sm">{u.plantIds?.length ? u.plantIds.map(plantName).join(', ') : '—'}</td>
                      <td>{u.active ? <Badge tone="good">Active</Badge> : <Badge>Inactive</Badge>}</td>
                      <td>
                        <div className="actions" style={{ justifyContent: 'flex-end' }}>
                          <button type="button" className="btn btn-sm" onClick={() => setEditing(u)}>
                            Edit
                          </button>
                          {u.active && u.id !== me?.id && (
                            <button type="button" className="btn btn-sm btn-danger" onClick={() => setDeactivating(u)}>
                              Deactivate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </AsyncView>
      </div>
      {editing !== undefined && (
        <UserFormModal
          user={editing}
          roles={roles.data ?? []}
          plants={plants}
          onClose={() => setEditing(undefined)}
          onSaved={(saved, isEdit) => {
            replace(saved);
            setEditing(undefined);
            setNotice(`${saved.fullName} was ${isEdit ? 'updated' : 'created'}.`);
          }}
        />
      )}
      {deactivating && (
        <ConfirmDialog
          title="Deactivate user"
          confirmLabel="Deactivate"
          danger
          pending={deactivate.pending}
          error={deactivate.error}
          onConfirm={onDeactivate}
          onCancel={() => {
            deactivate.reset();
            setDeactivating(null);
          }}
        >
          <p>
            <strong>{deactivating.fullName}</strong> ({deactivating.email}) will no longer be able to sign in.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
