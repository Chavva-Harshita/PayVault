import { formatCurrency } from '../utils/formatCurrency'

export default function BalanceCard({ wallet }) {
  if (!wallet) return null

  const statusColor = wallet.status === 'ACTIVE' ? 'text-vault' : 'text-clay'

  return (
    <div className="rounded-xl border border-line bg-ink p-8 text-paper">
      <div className="font-mono text-xs uppercase tracking-widest text-paper/60">Current balance</div>
      <div className="figure mt-2 text-5xl font-medium">
        {formatCurrency(wallet.balance, wallet.currency)}
      </div>
      <div className="mt-4 flex items-center gap-4 text-sm text-paper/70">
        <span>{wallet.currency}</span>
        <span aria-hidden="true">·</span>
        <span className={statusColor}>{wallet.status}</span>
      </div>
    </div>
  )
}
