import { useQuery } from "@tanstack/react-query";
import type { DashboardData } from "./typesApi";
import { fetchWithAuth } from "./authApi";

export const useDashboardData = () => {
	const query = useQuery<DashboardData>({
		queryKey: ['dashboard'],
		queryFn: async () => {
			const response = await fetchWithAuth('/api/v1/devices/dashboard');
			if (!response.ok) throw new Error('Network response was not ok');
			return response.json();
		},
		refetchInterval: 7000,
	});
	
	return { 
		data: query.data, 
		isLoading: query.isLoading, 
		error: query.error,
		refetch: query.refetch
	};
};


