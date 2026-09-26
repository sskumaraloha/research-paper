import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../context/AuthContext';
import { useContractForm } from '../../../hooks/useContractForm';
import { useMutation } from '../../../hooks/useAsync';
import { ErrorMessage, FormField } from '../../../components/common';
import { demoLogin, login } from '../services/authService';

export function LoginPage() {
  const { signIn, sessionExpired } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const form = useContractForm('LoginRequest', { email: '', password: '' });
  const loginMutation = useMutation(login);
  const demoMutation = useMutation(demoLogin);
  const pending = loginMutation.pending || demoMutation.pending;
  const destination = location.state?.from?.pathname ?? '/dashboard';

  const finish = (result) => {
    if (!result.ok) return;
    signIn(result.data);
    navigate(destination, { replace: true });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = form.validate();
    if (!payload) return;
    demoMutation.reset();
    const result = await loginMutation.run(payload);
    if (!result.ok) form.applyServerErrors(result.error);
    finish(result);
  };

  const onDemo = async () => {
    loginMutation.reset();
    finish(await demoMutation.run());
  };

  return (
    <div className="stack" style={{ gap: 18 }}>
      <div>
        <h1>Sign in</h1>
        <p className="muted">Welcome back. Enter your credentials to continue.</p>
      </div>
      {sessionExpired && !loginMutation.error && (
        <div className="alert alert-info">Your session has expired. Please sign in again.</div>
      )}
      <ErrorMessage error={loginMutation.error ?? demoMutation.error} title="Sign-in failed" />
      <form className="form" onSubmit={onSubmit} noValidate>
        <FormField label="Email" required error={form.errors.email}>
          <input className="input" type="email" autoComplete="username" autoFocus {...form.bind('email')} />
        </FormField>
        <FormField label="Password" required error={form.errors.password}>
          <input className="input" type="password" autoComplete="current-password" {...form.bind('password')} />
        </FormField>
        <div className="row between">
          <Link to="/forgot-password">Forgot password?</Link>
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={pending}>
          {loginMutation.pending ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
      <div className="divider">or</div>
      <button type="button" className="btn btn-block" onClick={onDemo} disabled={pending}>
        {demoMutation.pending ? 'Starting demo…' : 'Explore with a demo account'}
      </button>
      <p className="muted" style={{ textAlign: 'center' }}>
        No account? <Link to="/register">Create one</Link>
      </p>
    </div>
  );
}
