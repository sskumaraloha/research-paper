import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { useContractForm } from '../../../hooks/useContractForm';
import { useMutation } from '../../../hooks/useAsync';
import { ErrorMessage, FormField } from '../../../components/common';
import { register } from '../services/authService';

export function RegisterPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  // RegistrationRequest { fullName*, email*, password* (8–100), phoneNumber? (\+?[0-9]{8,15}) }
  const form = useContractForm('RegistrationRequest', { fullName: '', email: '', password: '', phoneNumber: '' });
  const mutation = useMutation(register);

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    const result = await mutation.run(payload);
    if (!result.ok) return form.applyServerErrors(result.error);
    signIn(result.data);
    navigate('/dashboard', { replace: true });
  };

  return (
    <div className="stack" style={{ gap: 18 }}>
      <div>
        <h1>Create account</h1>
        <p className="muted">Register to start recording maintenance.</p>
      </div>
      <ErrorMessage error={mutation.error} title="Registration failed" />
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormField label="Full name" required error={form.errors.fullName}>
          <input className="input" autoComplete="name" autoFocus {...form.bind('fullName')} />
        </FormField>
        <FormField label="Email" required error={form.errors.email}>
          <input className="input" type="email" autoComplete="email" {...form.bind('email')} />
        </FormField>
        <FormField label="Password" required error={form.errors.password} hint="At least 8 characters.">
          <input className="input" type="password" autoComplete="new-password" {...form.bind('password')} />
        </FormField>
        <FormField label="Phone number" error={form.errors.phoneNumber} hint="Optional. 8–15 digits, optional leading +.">
          <input className="input" type="tel" autoComplete="tel" {...form.bind('phoneNumber')} />
        </FormField>
        <button type="submit" className="btn btn-primary btn-block" disabled={mutation.pending}>
          {mutation.pending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="muted" style={{ textAlign: 'center' }}>
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </div>
  );
}
