import { fetchWithAuth } from './authApi';

export const triggerReconnect = async () => {
  await fetchWithAuth('/api/v1/devices/reconnect', { 
    method: 'POST' 
  });
};

