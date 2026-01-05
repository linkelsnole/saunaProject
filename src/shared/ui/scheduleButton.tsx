import { Button } from "@/shared/ui/button";

export function ScheduleButton({ onClick }: { onClick: () => void }) {
	return (
		<Button
			variant="outline"
			size="icon"
			onClick={onClick}
			className="h-10 w-10 border-border/40"
		>
			<div className="h-[1.4rem] w-[1.4rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0">R</div>
			<span className="sr-only">Расписание</span>
		</Button>
	);
}