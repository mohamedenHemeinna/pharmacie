import React, { useState } from 'react';
import {
  Calendar,
  Coins,
  Download,
  Plus,
  Receipt,
  Search,
  Trash2,
  Vault,
  Wallet,
} from 'lucide-react';
import { ExpenseRecord, InvoiceConfig } from '../../types';
import { exportToCSV } from '../../utils/exportCsv';
import { getTodayISODate } from '../../utils/dateUtils';
import { getCurrencySymbol, formatMoney } from '../../utils/currency';
import { expenseCategoryLabel } from '../../utils/expenseCategories';
import { useTranslation } from 'react-i18next';

interface ExpensesTabProps {
  invoiceConfig?: InvoiceConfig;
  expenses: ExpenseRecord[];
  setExpenses?: React.Dispatch<React.SetStateAction<ExpenseRecord[]>>;
  onOpenAddExpenseModal: () => void;
}

export const ExpensesTab: React.FC<ExpensesTabProps> = ({
  invoiceConfig,
  expenses,
  setExpenses,
  onOpenAddExpenseModal,
}) => {
  const currencySymbol = getCurrencySymbol(invoiceConfig?.currency);
  const { t } = useTranslation();
  const today = getTodayISODate();
  const [search, setSearch] = useState('');
  const [selectedCat, setSelectedCat] = useState('all');
  const [selectedDate, setSelectedDate] = useState('');

  const filtered = expenses.filter(e => {
    const q = search.toLowerCase();
    const matchSearch =
      e.desc.toLowerCase().includes(q) ||
      e.cat.toLowerCase().includes(q) ||
      e.date.includes(q);
    const matchCat = selectedCat === 'all' || e.cat === selectedCat;
    const matchDate = !selectedDate || e.date === selectedDate;
    return matchSearch && matchCat && matchDate;
  });

  const totalExpenseSum = expenses.reduce((sum, e) => sum + (Number(e.amt) || 0), 0);
  const todayExpensesSum = expenses
    .filter(e => e.date === today)
    .reduce((sum, e) => sum + (Number(e.amt) || 0), 0);

  const handleExportCSV = () => {
    exportToCSV(
      'daily_expenditures_register',
      ['Date', 'Expense Category', 'Description', 'Amount (INR)'],
      filtered.map(e => [e.date, e.cat, e.desc, Number(e.amt).toFixed(2)])
    );
  };

  const handleDeleteExpense = (id: string, amt: number) => {
    if (!confirm(t('expenses.confirmRemove'))) return;
    if (setExpenses) {
      setExpenses(prev => prev.filter(e => e.id !== id));
    }
  };

  return (
    <div className="space-y-6">
      <div className="p-6 rounded-3xl bg-surface backdrop-blur-xl border border-border shadow-xl flex justify-between items-center flex-wrap gap-4 text-text">
        <div>
          <h3 className="text-base font-bold text-text flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-orange-500/20 border border-orange-500/30 flex items-center justify-center text-orange-600 dark:text-orange-400">
              <Wallet className="w-4 h-4" />
            </div>
            <span>{t('expenses.title')}</span>
          </h3>
          <p className="text-xs text-text-muted mt-1">
            {t('expenses.subtitle')}
          </p>
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          <div className="bg-orange-500/20 border border-orange-500/30 px-3.5 py-2 rounded-2xl text-xs font-bold text-orange-700 dark:text-orange-300 backdrop-blur-md">{t('expenses.todayLogged')}<span className="font-mono text-sm text-orange-800 dark:text-orange-200">{formatMoney(todayExpensesSum)}</span>
          </div>

          <div className="bg-surface border border-border px-3.5 py-2 rounded-2xl text-xs font-bold text-text-muted backdrop-blur-md">{t('expenses.allTimeTotal')}<span className="font-mono text-sm text-text">{formatMoney(totalExpenseSum)}</span>
          </div>

          <button
            onClick={handleExportCSV}
            className="bg-emerald-600 hover:bg-emerald-500 text-text font-bold px-3.5 py-2 rounded-2xl text-xs flex items-center gap-1.5 shadow-lg transition cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" />
            <span>{t('expenses.exportExcel')}</span>
          </button>

          <button
            id="btn-log-expense"
            onClick={onOpenAddExpenseModal}
            className="bg-orange-600 hover:bg-orange-500 text-text px-4 py-2 rounded-2xl text-xs font-bold flex items-center gap-1.5 shadow-lg transition cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>{t('expenses.logExpense')}</span>
          </button>
        </div>
      </div>

      {/* Filter toolbar */}
      <div className="flex justify-between items-center flex-wrap gap-3">
        <div className="flex items-center gap-3 flex-wrap">
          <select
            value={selectedCat}
            onChange={e => setSelectedCat(e.target.value)}
            className="p-2.5 bg-surface border border-border rounded-2xl text-xs text-text outline-none focus:border-orange-400 font-medium"
          >
            <option value="all">{t('expenses.allCategories')}</option>
            <option value="Sample Transport">{t('expenses.catTransport')}</option>
            <option value="Maintenance / Repairs">{t('expenses.catMaintenance')}</option>
            <option value="Bank Deposit Cash">{t('expenses.catBankDeposit')}</option>
            <option value="Staff Daily Wage">{t('expenses.catStaffWage')}</option>
            <option value="Tea & Miscellaneous">{t('expenses.catMisc')}</option>
            <option value="Electricity & Utility">{t('expenses.catUtility')}</option>
            <option value="Distributor Cash Paid">{t('expenses.catDistributor')}</option>
          </select>

          <div className="flex items-center gap-1.5 bg-surface border border-border px-3 py-1.5 rounded-2xl text-xs text-text-muted">
            <Calendar className="w-3.5 h-3.5 text-orange-600 dark:text-orange-400" />
            <input
              type="date"
              value={selectedDate}
              onChange={e => setSelectedDate(e.target.value)}
              className="bg-transparent text-text font-mono text-xs outline-none cursor-pointer"
              title={t('expenses.filterDate')}
            />
            {selectedDate && (
              <button
                type="button"
                onClick={() => setSelectedDate('')}
                className="text-text-muted hover:text-text text-xs px-1"
                title={t('expenses.clearDate')}
              >
                ✕
              </button>
            )}
          </div>
        </div>

        <div className="relative">
          <Search className="w-3.5 h-3.5 text-text-muted absolute left-3 top-3" />
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder={t('expenses.searchPlaceholder')}
            className="pl-9 pr-3 py-2 glass-input rounded-2xl text-xs w-64"
          />
        </div>
      </div>

      <div className="glass-panel rounded-3xl overflow-hidden text-xs text-text">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead className="bg-surface-elevated border-b border-border text-[11px] font-semibold text-text-muted uppercase backdrop-blur-md">
              <tr>
                <th className="p-3.5">{t('expenses.colDate')}</th>
                <th className="p-3.5">{t('expenses.colCategory')}</th>
                <th className="p-3.5">{t('expenses.colDesc')}</th>
                <th className="p-3.5 font-bold text-text text-right">Amount ({currencySymbol})</th>
                {setExpenses && <th className="p-3.5 text-center">{t('expenses.colAction')}</th>}
              </tr>
            </thead>
            <tbody id="expense-table-rows" className="divide-y divide-border text-text">
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={setExpenses ? 5 : 4} className="p-8 text-center text-text-muted">{t('expenses.empty')}</td>
                </tr>
              ) : (
                filtered.map(e => (
                  <tr key={e.id} className="hover:bg-bg transition">
                    <td className="p-3.5 font-mono text-text-muted">{e.date}</td>
                    <td className="p-3.5">
                      <span className="bg-warning/15 text-warning border border-warning/30 font-semibold px-2.5 py-1 rounded-full text-[11px]">
                        {expenseCategoryLabel(e.cat)}
                      </span>
                    </td>
                    <td className="p-3.5 text-text font-medium">{e.desc}</td>
                    <td className="p-3.5 font-mono font-bold text-warning text-right text-sm">
                      {formatMoney(Number(e.amt))}
                    </td>
                    {setExpenses && (
                      <td className="p-3.5 text-center">
                        <button
                          type="button"
                          onClick={() => handleDeleteExpense(e.id, Number(e.amt))}
                          className="p-1.5 bg-danger/15 hover:bg-danger/25 text-danger border border-danger/30 rounded-lg transition cursor-pointer"
                          title={t('expenses.deleteRecord')}
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
