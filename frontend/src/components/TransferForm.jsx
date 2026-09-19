import { useState } from 'react'
import * as transactionService from '../services/transactionService'
import { toMinorUnits } from '../utils/formatCurrency'
import { extractErrorMessage } from '../services/api'

export default function TransferForm({ onSuccess }) {
  const [receiverId, setReceiverId] = useState('')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)

    const minorUnits = toMinorUnits(amount)
    if (!minorUnits) {
      setError('Enter an amount greater than zero')
      return
    }
    if (!receiverId.trim()) {
      setError('Enter who you\'re sending to')
      return
    }

    setIsSubmitting(true)
    try {
      const transaction = await transactionService.transfer({
        receiverId: receiverId.trim(),
        amount: minorUnits,
        note: note.trim() || undefined,
      })
      setReceiverId('')
      setAmount('')
      setNote('')
      onSuccess?.(transaction)
    } catch (err) {
      setError(extractErrorMessage(err))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-ink" htmlFor="receiverId">
          Receiver
        </label>
        <input
          id="receiverId"
          type="text"
          value={receiverId}
          onChange={(e) => setReceiverId(e.target.value)}
          placeholder="Receiver's user ID"
          className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-ink" htmlFor="amount">
          Amount (₹)
        </label>
        <input
          id="amount"
          type="number"
          step="0.01"
          min="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0.00"
          className="figure mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-ink" htmlFor="note">
          Note <span className="text-fade">(optional)</span>
        </label>
        <input
          id="note"
          type="text"
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="What's this for?"
          className="mt-1 w-full rounded-md border border-line bg-white px-3 py-2 text-sm focus:border-vault focus:outline-none focus:ring-1 focus:ring-vault"
        />
      </div>

      {error && <p className="text-sm text-clay">{error}</p>}

      <button
        type="submit"
        disabled={isSubmitting}
        className="w-full rounded-md bg-vault px-4 py-2 text-sm font-medium text-white transition hover:bg-vault-dark disabled:opacity-50"
      >
        {isSubmitting ? 'Sending…' : 'Send money'}
      </button>
    </form>
  )
}
