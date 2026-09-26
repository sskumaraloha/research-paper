import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Filter + page state kept in the URL query string, so filters survive reloads and back navigation.
 * Changing any filter resets `page` to 0.
 */
export function useSearchState(defaults) {
  const [params, setParams] = useSearchParams();
  const state = {};
  for (const [key, def] of Object.entries(defaults)) {
    const raw = params.get(key);
    state[key] = raw === null ? def : typeof def === 'number' ? Number(raw) : raw;
  }
  const update = useCallback(
    (changes) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          const resetPage = !('page' in changes);
          for (const [k, v] of Object.entries(changes)) {
            if (v === '' || v === null || v === undefined) next.delete(k);
            else next.set(k, String(v));
          }
          if (resetPage) next.delete('page');
          return next;
        },
        { replace: true },
      );
    },
    [setParams],
  );
  return [state, update];
}
