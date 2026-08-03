import * as React from "react"
import { cn } from "@/lib/utils"

function Input({ className, type, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        "h-9 w-full min-w-0 rounded border border-[#E5E7EB] bg-transparent px-3 py-1.5 text-sm text-[#022F40] outline-none transition-colors placeholder:text-[#80727B] focus:border-[#022F40] focus:ring-2 focus:ring-[#022F40]/10 disabled:cursor-not-allowed disabled:opacity-50",
        className
      )}
      {...props}
    />
  )
}

export { Input }
