import { Routes, Route, Navigate } from "react-router-dom";
import {Card,CardContent} from "@/shared/ui/card";

import Schema from "@/schema/Schema";
import AppHeader from "@/header/AppHeader";
import { LoginPage } from "@/components/auth/LoginPage";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { PublicRoute } from "@/components/auth/PublicRoute";
import { SettingsWidget } from "@/components/settings/SettingsWidget";

const MainDashboard = () => {
	return (
		<div className="w-full h-full pb-2">
			<div className="md:block h-full">
				<div className="grid grid-cols-1 md:grid-cols-2 gap-6 h-full">
					<Card className="overflow-hidden col-span-1 flex flex-col pt-0 pb-0 h-full">
						<CardContent className="flex-1 flex flex-col h-full px-0">
							<div className="hidden md:block w-full h-full flex items-center justify-center flex-1">
								<Schema />
							</div>
							<div className="block md:hidden w-full h-full flex items-center justify-center flex-1">
							</div>
						</CardContent>
					</Card>
					
					{/* Правая часть - настройки */}
					<div className='hidden md:block h-full overflow-hidden'>
						<Card className="h-full">
							<CardContent className="p-4 lg:p-5 xl:p-6 h-full">
								<SettingsWidget />
							</CardContent>
						</Card>
					</div>
				</div>
			</div>
		</div>
	);
};

const App = () => {
	return (
		<>
			<Routes>
				<Route
					path="/login"
					element={
						<PublicRoute>
							<LoginPage />
						</PublicRoute>
					}
				/>
				<Route
					path="/"
					element={
						<ProtectedRoute>
							<div 
								className="flex flex-1 flex-col w-full h-screen overflow-hidden"
								style={{ overscrollBehavior: 'none' }}
							>
								<div className="flex items-center px-4 py-2">
									<AppHeader/>
								</div>
								<div className="flex flex-1 overflow-hidden">
									<div className="px-4 lg:px-6 w-full h-full">
										<MainDashboard />
									</div>
								</div>
							</div>
						</ProtectedRoute>
					}
				/>
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</>
	);
};

export default App;
