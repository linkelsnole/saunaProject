import { cn } from "@/lib/utils"

export const NeoInput = ({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) => {
  return (
    <input
      {...props}
      className={cn(
        "no-spinner w-full h-12 px-4 rounded-xl",
        "bg-white text-slate-800 font-mono font-bold",
        "outline-none transition-colors",
        "border border-slate-200 hover:border-slate-300",
        "focus:border-blue-500",
        className
      )}
    />
  )
}
