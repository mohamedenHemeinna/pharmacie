/**
 * Centralized currency configuration.
 *
 * The application currency is the Mauritanian Ouguiya (MRU). It is NOT MRO (the old code).
 * Amounts are stored in the database as NUMERIC(14,2) and are never converted between currencies:
 * this module only formats them for display.
 */
export const CURRENCY = {
  code: 'MRU',
  symbol: 'MRU',
  name: 'Mauritanian Ouguiya',
  nameAr: 'الأوقية الموريتانية',
  /** Decimal places shown for amounts (the ouguiya is divided into 5 khoums). */
  maxFractionDigits: 2,
} as const;

/**
 * Returns the symbol to display. Always MRU: invoice settings may still hold a currency value saved
 * by an older version (for example "INR"), but the application no longer shows it.
 */
export function getCurrencySymbol(_storedCurrency?: string | null): string {
  return CURRENCY.symbol;
}

/**
 * Formats an amount for display, e.g. 1500 -> "1,500 MRU", 12.5 -> "12.5 MRU".
 * Non-finite values are shown as zero so a bad number never leaks "NaN" to a printed invoice.
 */
export function formatMoney(value: number | string | null | undefined): string {
  const n = typeof value === 'string' ? Number(value) : value ?? 0;
  const safe = Number.isFinite(n) ? n : 0;
  const formatted = safe.toLocaleString('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: CURRENCY.maxFractionDigits,
  });
  return `${formatted} ${CURRENCY.symbol}`;
}
