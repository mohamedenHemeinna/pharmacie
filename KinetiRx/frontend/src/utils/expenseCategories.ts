import i18n from '../i18n';

/**
 * Expense categories are stored in the database under their English values (for example
 * "Maintenance / Repairs"). The values never change, so existing records keep working; only the
 * label shown to the user is translated here.
 */
export const EXPENSE_CATEGORY_VALUES = [
  'Sample Transport',
  'Maintenance / Repairs',
  'Bank Deposit Cash',
  'Staff Daily Wage',
  'Tea & Miscellaneous',
  'Electricity & Utility',
  'Distributor Cash Paid',
] as const;

const KEYS: Record<string, string> = {
  'Sample Transport': 'expenses.categories.transport',
  'Maintenance / Repairs': 'expenses.categories.maintenance',
  'Bank Deposit Cash': 'expenses.categories.bankDeposit',
  'Staff Daily Wage': 'expenses.categories.staffWage',
  'Tea & Miscellaneous': 'expenses.categories.misc',
  'Electricity & Utility': 'expenses.categories.utility',
  'Distributor Cash Paid': 'expenses.categories.distributorPaid',
};

/** Returns the translated label for a stored category value. Unknown (custom) values are shown as stored. */
export function expenseCategoryLabel(value: string): string {
  const key = KEYS[value];
  return key ? i18n.t(key) : value;
}
