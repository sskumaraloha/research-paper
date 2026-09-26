import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useContractForm } from '../../../hooks/useContractForm';
import { useAsync, useMutation } from '../../../hooks/useAsync';
import { ErrorMessage, FormField, LoadingState, SuccessMessage } from '../../../components/common';
import { resetPassword, validateResetToken } from '../services/authService';

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const tokenCheck = useAsync(() => validateResetToken(token), [token], { enabled: Boolean(token) });
  // ResetPasswordRequest { token*, newPassword* (8–100) }
  const form = useContractForm('ResetPasswordRequest', { token, newPassword: '' });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [confirmError, setConfirmError] = useState(null);
  const mutation = useMutation(resetPassword);
  const [success, setSuccess] = useState(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate({ token });
    const mismatch = form.values.newPassword !== confirmPassword ? 'Passwords do not match.' : null;
    setConfirmError(mismatch);
    if (!payload || mismatch) return;
    const result = await mutation.run(payload);
    if (result.ok) setSuccess(result.data?.message ?? 'Your password has been reset.');
    else form.applyServerErrors(result.error);
  };

  let body;
  if (!token) {
    body = <ErrorMessage error={{ message: 'This reset link is missing its token.' }} title="Invalid link" />;
  } else if (tokenCheck.loading) {
    body = <LoadingState label="Checking your reset link…" />;
  } else if (tokenCheck.error) {
    body = <ErrorMessage error={tokenCheck.error} title="This reset link is invalid or has expired" />;
  } else if (success) {
    body = (
      <>
        <SuccessMessage>{success}</SuccessMessage>
        <Link className="btn btn-primary btn-block" to="/login">
          Sign in
        </Link>
      </>
    );
  } else {
    body = (
      <>
        <ErrorMessage error={mutation.error} />
        <form className="form" onSubmit={onSubmit} noValidate>
          <FormField label="New password" required error={form.errors.newPassword} hint="At least 8 characters.">
            <input className="input" type="password" autoComplete="new-password" autoFocus {...form.bind('newPassword')} />
          </FormField>
          <FormField label="Confirm new password" required error={confirmError}>
            <input
              className="input"
              type="password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
          </FormField>
          <button type="submit" className="btn btn-primary btn-block" disabled={mutation.pending}>
            {mutation.pending ? 'Saving…' : 'Set new password'}
          </button>
        </form>
      </>
    );
  }

  return (
    <div className="stack" style={{ gap: 18 }}>
      <div>
        <h1>Choose a new password</h1>
        {tokenCheck.data?.message && !success && <p className="muted">{tokenCheck.data.message}</p>}
      </div>
      {body}
      <p className="muted" style={{ textAlign: 'center' }}>
        <Link to="/login">Back to sign in</Link>
      </p>
    </div>
  );
}
