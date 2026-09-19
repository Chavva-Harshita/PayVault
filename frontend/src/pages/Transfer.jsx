import { useState } from 'react'
import { Link } from 'react-router-dom'
import TransferForm from '../components/TransferForm'
import { formatCurrency } from '../utils/formatCurrency'

export default function Transfer() {
  const [lastTransaction, setLastTransaction] = useState(null)

  return (
    <div className="mx-auto max-w-md space-y-6">
      <h1 className="font-display text-2xl text-ink">Send money</h1>

      {lastTransaction && (
        <div className="ledger-stub p-4 text-sm">
          <p className="font-medium text-vault">Sent successfully</p>
          <p className="mt-1 text-fade">
            {formatCurrency(lastTransaction.amount, lastTransaction.currency)} to{' '}
            <span className="figure">{lastTransaction.receiverId}</span>
          </p>
          <Link to="/transactions" className="mt-2 inline-block text-vault hover:underline">
            View in ledger →
          </Link>
        </div>
      )}

      <div className="rounded-xl border border-line bg-paper-raised p-6">
        <TransferForm onSuccess={setLastTransaction} />
      </div>
    </div>
  )
}
