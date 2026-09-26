import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader and tracks { data, error, loading }. Re-runs when deps change; stale
 * responses from earlier runs are ignored. Pass `enabled: false` to skip (e.g. no plant selected).
 */
export function useAsync(loader, deps, { enabled = true } = {}) {
  const [state, setState] = useState({ data: undefined, error: null, loading: enabled });
  const runId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const run = useCallback(async () => {
    const id = ++runId.current;
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const data = await loaderRef.current();
      if (id === runId.current) setState({ data, error: null, loading: false });
      return data;
    } catch (error) {
      if (id === runId.current) setState((s) => ({ ...s, error, loading: false }));
      return undefined;
    }
  }, []);

  useEffect(() => {
    if (enabled) run();
    else setState({ data: undefined, error: null, loading: false });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, ...deps]);

  const setData = useCallback((updater) => {
    setState((s) => ({ ...s, data: typeof updater === 'function' ? updater(s.data) : updater }));
  }, []);

  return { ...state, reload: run, setData };
}

/**
 * Tracks a mutation (form submit / action button): { run, pending, error, reset }.
 * `run` never throws; it resolves to { ok: true, data } or { ok: false, error } and sets `error`.
 */
export function useMutation(action) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(null);
  const actionRef = useRef(action);
  actionRef.current = action;

  const run = useCallback(async (...args) => {
    setPending(true);
    setError(null);
    try {
      return { ok: true, data: await actionRef.current(...args) };
    } catch (e) {
      setError(e);
      return { ok: false, error: e };
    } finally {
      setPending(false);
    }
  }, []);

  return { run, pending, error, reset: () => setError(null) };
}
