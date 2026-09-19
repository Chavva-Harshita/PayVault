import { formatCurrency, formatDate } from '../utils/formatCurrency'

const STATUS_STYLES = {
  SUCCESS: 'text-vault',
  PENDING: 'text-amber',
  FAILED: 'text-clay',
}

export default function TransactionCard({ transaction, currentUserId }) {
  const isOutgoing = transaction.senderId === currentUserId
  const sign = isOutgoing ? '−' : '+'
  const amountColor = isOutgoing ? 'text-clay' : 'text-vault'

  return (
    <div className="ledger-stub flex items-center justify-between gap-4 py-4 pr-4">
      <div className="min-w-0">
        <div className="truncate text-sm font-medium text-ink">
          {isOutgoing ? `To ${transaction.receiverId}` : `From ${transaction.senderId}`}
        </div>
        <div className="mt-0.5 flex items-center gap-2 font-mono text-xs text-fade">
          <span>{formatDate(transaction.createdAt)}</span>
          <span aria-hidden="true">·</span>
          <span className={STATUS_STYLES[transaction.status] || 'text-fade'}>{transaction.status}</span>
        </div>
        {transaction.note && <div className="mt-1 truncate text-xs text-fade italic">"{transaction.note}"</div>}
      </div>
      <div className={`figure shrink-0 text-lg font-medium ${amountColor}`}>
        {sign}
        {formatCurrency(transaction.amount, transaction.currency)}
      </div>
    </div>
  )
}
