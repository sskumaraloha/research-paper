import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useContractForm } from '../../../hooks/useContractForm';
import { useMutation } from '../../../hooks/useAsync';
import { ErrorMessage, FormField, SuccessMessage } from '../../../components/common';
import { forgotPassword } from '../services/authService';

export function ForgotPasswordPage() {
  const form = useContractForm('ForgotPasswordRequest', { email: '' });
  const mutation = useMutation((payload) => forgotPassword(payload.email));
  const [message, setMessage] = useState(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (result.ok) setMessage(result.data?.message ?? 'Check your email for a reset link.');
  };

  return (
    <div className="stack" style={{ gap: 18 }}>
      <div>
        <h1>Reset password</h1>
        <p className="muted">We'll email you a link to choose a new password.</p>
      </div>
      <ErrorMessage error={mutation.error} />
      <SuccessMessage>{message}</SuccessMessage>
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormField label="Email" required error={form.errors.email}>
          <input className="input" type="email" autoComplete="email" autoFocus {...form.bind('email')} />
        </FormField>
        <button type="submit" className="btn btn-primary btn-block" disabled={mutation.pending}>
          {mutation.pending ? 'Sending…' : 'Send reset link'}
        </button>
      </form>
      <p className="muted" style={{ textAlign: 'center' }}>
        <Link to="/login">Back to sign in</Link>
      </p>
    </div>
  );
}
