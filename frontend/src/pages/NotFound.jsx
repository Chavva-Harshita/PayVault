import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-paper px-4 text-center">
      <div className="figure text-6xl text-line">404</div>
      <p className="mt-4 font-display text-xl text-ink">This page isn't in the ledger</p>
      <Link to="/dashboard" className="mt-6 text-sm font-medium text-vault hover:underline">
        ← Back to dashboard
      </Link>
    </div>
  )
}
