export default function LoadingSpinner({ label = 'Loading' }) {
  return (
    <div className="flex items-center gap-3 text-fade">
      <span className="inline-block h-4 w-4 animate-spin rounded-full border-2 border-line border-t-vault" />
      <span className="font-mono text-sm">{label}…</span>
    </div>
  )
}
