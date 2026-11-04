/**
 * Dynamic Date Utilities for Pharma Care Pro
 * Always uses the active system date rather than hardcoded dates.
 */

export const getTodayISODate = (): string => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getRelativeISODate = (daysOffset: number): string => {
  const d = new Date();
  d.setDate(d.getDate() + daysOffset);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getRelativeMonthYear = (monthsOffset: number): string => {
  const d = new Date();
  d.setMonth(d.getMonth() + monthsOffset);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
};

import i18n from '../i18n';

/** Date locale follows the active language; Arabic keeps Latin digits for readability. */
export const dateLocale = (): string => (i18n.language.startsWith('ar') ? 'ar-u-nu-latn' : 'en-GB');

export const formatReadableDate = (isoDateStr?: string): string => {
  if (!isoDateStr) return '';
  try {
    const [y, m, d] = isoDateStr.split('-').map(Number);
    if (!y || !m || !d) return isoDateStr;
    const dateObj = new Date(y, m - 1, d);
    return dateObj.toLocaleDateString(dateLocale(), {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return isoDateStr;
  }
};

export const formatFullDateWithDay = (isoDateStr?: string): string => {
  const d = isoDateStr ? new Date(isoDateStr) : new Date();
  return d.toLocaleDateString(dateLocale(), {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

export const formatShortDayMonth = (isoDateStr?: string): string => {
  if (!isoDateStr) return '';
  try {
    const parts = isoDateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}`;
    }
  } catch {}
  return isoDateStr;
};

/**
 * Returns past N days array of { date: YYYY-MM-DD, label: DD/MM } ending with today
 */
export const getPastNDays = (count: number = 7): Array<{ date: string; label: string }> => {
  const list: Array<{ date: string; label: string }> = [];
  for (let i = count - 1; i >= 0; i--) {
    const iso = getRelativeISODate(-i);
    list.push({
      date: iso,
      label: formatShortDayMonth(iso),
    });
  }
  return list;
};
