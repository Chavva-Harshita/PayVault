/**
 * All monetary amounts from the backend arrive as integers in minor units
 * (paise), never as floating point - see the architecture doc's monetary
 * representation decision. These two functions are the single boundary
 * where the UI converts to/from the rupee figures a person actually types
 * and reads.
 */

export function formatCurrency(minorUnits, currency = 'INR') {
  const symbol = currency === 'INR' ? '₹' : currency + ' '
  const rupees = minorUnits / 100
  return `${symbol}${rupees.toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

/**
 * Converts a rupee amount typed into a form (e.g. "150.50") into an
 * integer number of paise (15050). Returns null if the input isn't a
 * valid positive number, so callers can distinguish "bad input" from "0".
 */
export function toMinorUnits(rupeesInput) {
  const value = Number(rupeesInput)
  if (Number.isNaN(value) || value <= 0) return null
  return Math.round(value * 100)
}

export function formatDate(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleString('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}
