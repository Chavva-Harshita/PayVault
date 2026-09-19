import { useEffect, useState, useCallback } from 'react'
import { useAuth } from '../hooks/useAuth'
import * as transactionService from '../services/transactionService'
import TransactionTable from '../components/TransactionTable'
import LoadingSpinner from '../components/LoadingSpinner'
import { extractErrorMessage } from '../services/api'

export default function Transactions() {
  const { user } = useAuth()
  const [page, setPage] = useState(0)
  const [data, setData] = useState({ content: [], totalPages: 0 })
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback((pageNumber) => {
    setIsLoading(true)
    setError(null)
    transactionService
      .listTransactions({ page: pageNumber, size: 10 })
      .then(setData)
      .catch((err) => setError(extractErrorMessage(err)))
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(() => {
    load(page)
  }, [page, load])

  return (
    <div className="space-y-6">
      <h1 className="font-display text-2xl text-ink">Ledger</h1>

      {error && <p className="rounded-md bg-clay/10 p-3 text-sm text-clay">{error}</p>}

      {isLoading ? (
        <div className="flex justify-center py-16">
          <LoadingSpinner label="Loading ledger" />
        </div>
      ) : (
        <TransactionTable
          transactions={data.content}
          currentUserId={user?.userId}
          page={data.page ?? page}
          totalPages={data.totalPages ?? 0}
          onPageChange={setPage}
        />
      )}
    </div>
  )
}
