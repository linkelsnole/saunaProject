"use client";

import * as React from "react";
import { Button } from "@/shared/ui/button";
import { Moon, Sun } from "lucide-react";

export function ThemeToggleButton() {
	const [theme, setThemeState] = React.useState<"dark" | "light">("light");
	const setTheme = (newTheme: "dark" | "light") => {
		setThemeState(newTheme);
		document.documentElement.classList.toggle("dark", newTheme === "dark");
	};

	return (
		<Button
			variant="outline"
			size="icon"
			onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
			title={theme === "dark" ? "Переключить на светлую тему" : "Переключить на темную тему"}
			className="h-10 w-10 border-border/40"
		>
			<Sun className="h-[1.4rem] w-[1.4rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
			<Moon className="absolute h-[1.4rem] w-[1.4rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
			<span className="sr-only">
				{theme === "dark" ? "Переключить на светлую тему" : "Переключить на темную тему"}
			</span>
		</Button>
	);
}
