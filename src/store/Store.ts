import { configureStore } from '@reduxjs/toolkit'

export const store = configureStore({
	reducer: {
		dummy: (state = {}) => state
	},
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch

