import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useAuth } from './AuthContext';
import { listPlants } from '../features/plants/services/plantService';
import { badgeCounts } from '../features/notifications/services/notificationService';

const PlantContext = createContext(null);
const KEY = 'mip.plantId';

function readStoredPlant() {
  try {
    const v = localStorage.getItem(KEY);
    return v ? Number(v) : null;
  } catch {
    return null;
  }
}

export function PlantProvider({ children }) {
  const { user, status } = useAuth();
  const [plants, setPlants] = useState([]);
  const [plantId, setPlantIdState] = useState(readStoredPlant);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  // BadgeCountsResponse { pendingValidation, unreadNotifications, insights }
  const [badges, setBadges] = useState(null);

  const loadPlants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await listPlants();
      setPlants(list ?? []);
      setPlantIdState((current) => {
        if (current && list.some((p) => p.id === current)) return current;
        const preferred = list.find((p) => user?.plantIds?.includes(p.id)) ?? list[0];
        return preferred?.id ?? null;
      });
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (status === 'authenticated') loadPlants();
    else {
      setPlants([]);
      setBadges(null);
    }
  }, [status, loadPlants]);

  useEffect(() => {
    if (!plantId) return;
    try {
      localStorage.setItem(KEY, String(plantId));
    } catch {
      // ignore
    }
  }, [plantId]);

  const refreshBadges = useCallback(async () => {
    if (!plantId || status !== 'authenticated') return;
    try {
      setBadges(await badgeCounts(plantId));
    } catch {
      setBadges(null);
    }
  }, [plantId, status]);

  useEffect(() => {
    refreshBadges();
    const t = setInterval(refreshBadges, 60000);
    return () => clearInterval(t);
  }, [refreshBadges]);

  const value = useMemo(
    () => ({
      plants,
      plantId,
      plant: plants.find((p) => p.id === plantId) ?? null,
      setPlantId: setPlantIdState,
      loading,
      error,
      reload: loadPlants,
      badges,
      refreshBadges,
    }),
    [plants, plantId, loading, error, loadPlants, badges, refreshBadges],
  );
  return <PlantContext.Provider value={value}>{children}</PlantContext.Provider>;
}

export function usePlant() {
  const ctx = useContext(PlantContext);
  if (!ctx) throw new Error('usePlant must be used inside PlantProvider');
  return ctx;
}
