import api from './api'

/**
 * Every transfer attempt gets a fresh key. If the same click needs to be
 * retried (e.g. the request timed out and the caller decides to resubmit
 * with the SAME key deliberately), that's the caller's choice - this
 * function itself always mints a new one, matching "one click = one new
 * attempt" as the default, safe behavior.
 */
export function transfer({ receiverId, amount, note }) {
  const idempotencyKey = crypto.randomUUID()
  return api
    .post(
      '/api/transactions/transfer',
      { receiverId, amount, note },
      { headers: { 'Idempotency-Key': idempotencyKey } }
    )
    .then((res) => res.data)
}

export function listTransactions({ page = 0, size = 20 } = {}) {
  return api.get('/api/transactions', { params: { page, size } }).then((res) => res.data)
}

export function getTransaction(transactionId) {
  return api.get(`/api/transactions/${transactionId}`).then((res) => res.data)
}
