import TransactionCard from './TransactionCard'

export default function TransactionTable({ transactions, currentUserId, page, totalPages, onPageChange }) {
  if (transactions.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-line py-16 text-center text-fade">
        <p className="font-display text-lg">No entries yet</p>
        <p className="mt-1 text-sm">Transfers you send or receive will show up here.</p>
      </div>
    )
  }

  return (
    <div>
      <div className="space-y-3">
        {transactions.map((transaction) => (
          <TransactionCard
            key={transaction.transactionId}
            transaction={transaction}
            currentUserId={currentUserId}
          />
        ))}
      </div>

      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-3 font-mono text-sm">
          <button
            onClick={() => onPageChange(page - 1)}
            disabled={page <= 0}
            className="rounded-md border border-line px-3 py-1 disabled:opacity-40"
          >
            ← Prev
          </button>
          <span className="text-fade">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => onPageChange(page + 1)}
            disabled={page + 1 >= totalPages}
            className="rounded-md border border-line px-3 py-1 disabled:opacity-40"
          >
            Next →
          </button>
        </div>
      )}
    </div>
  )
}
