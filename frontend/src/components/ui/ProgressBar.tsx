export function ProgressBar({ percent, tone = "brand" }: { percent: number; tone?: "brand" | "red" | "green" }) {
  const clamped = Math.max(0, Math.min(100, percent));
  const barColor = tone === "red" ? "bg-danger" : tone === "green" ? "bg-teal" : "bg-brand";
  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-divider">
      <div className={`h-full rounded-full ${barColor} transition-all`} style={{ width: `${clamped}%` }} />
    </div>
  );
}
