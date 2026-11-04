import React from 'react';
import { Calendar, KeyRound, LogOut, Menu, Moon, PlusCircle, Sparkles, Sun, User, Zap } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { CurrentUser, TabId } from '../types';
import { useTheme } from '../hooks/useTheme';

interface HeaderProps {
  currentTab: TabId;
  currentUser?: CurrentUser;
  currentUserName?: string;
  onOpenMobileMenu?: () => void;
  onOpenNewBill?: () => void;
  onOpenAddStock?: () => void;
  onOpenAIFinder?: () => void;
  onOpenLoginModal?: () => void;
  onOpenChangePassword?: () => void;
  onLogout?: () => void;
  todayDate?: string;
}

/** Tab identifiers that have a header title. Texts live under header.tabs.<id> in the locale files. */
const TAB_IDS = [
  'dashboard',
  'daily-sales',
  'pos',
  'due-khata',
  'opd',
  'patients',
  'medicine-orders',
  'inventory',
  'inward-ocr',
  'expenses',
  'business-dev',
  'employee-mgmt',
  'invoice-settings',
  'system-reset',
] as const;

export const Header: React.FC<HeaderProps> = ({
  currentTab,
  currentUser,
  currentUserName,
  onOpenMobileMenu,
  onOpenNewBill,
  onOpenAddStock,
  onOpenAIFinder,
  onOpenLoginModal,
  onOpenChangePassword,
  onLogout,
  todayDate,
}) => {
  const { t, i18n } = useTranslation();
  const isArabic = i18n.language.startsWith('ar');
  const knownTab = (TAB_IDS as readonly string[]).includes(currentTab);
  const currentInfo = knownTab
    ? { title: t(`header.tabs.${currentTab}.title`), subtitle: t(`header.tabs.${currentTab}.subtitle`) }
    : { title: t('header.appName'), subtitle: '' };
  const displayName = currentUser?.name || currentUserName || t('header.defaultUser');
  const isSuperAdmin = currentUser ? currentUser.role === 'admin' : true;
  const { theme, toggleTheme } = useTheme();
  // Dates use Latin digits in both languages so that invoices and registers stay easy to read.
  const dateLocale = isArabic ? 'ar-u-nu-latn' : 'en-GB';
  const dateText =
    todayDate ??
    new Date().toLocaleDateString(dateLocale, {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });

  return (
    <header
      id="app-header"
      className="glass-panel-subtle border-b border-border px-4 sm:px-6 py-3 flex justify-between items-center shrink-0 z-20 text-text"
    >
      <div className="flex items-center gap-3">
        {onOpenMobileMenu && (
          <button
            id="open-mobile-menu-btn"
            onClick={onOpenMobileMenu}
            className="p-2 rounded-xl text-text-muted hover:bg-bg lg:hidden transition border border-border"
            aria-label={t('header.openMenu')}
          >
            <Menu className="w-5 h-5" />
          </button>
        )}

        <div>
          <h2 className="text-base sm:text-lg font-extrabold text-text flex items-center gap-2 tracking-tight">
            <span>{currentInfo.title}</span>
            <span className="text-xs text-text-muted font-normal hidden sm:inline">
              ({currentInfo.subtitle})
            </span>
          </h2>
          <p className="text-[11px] text-text-muted flex items-center gap-1.5 mt-0.5">
            <User className="w-3 h-3 text-primary" />
            <span>
              {t('header.activeUser')}{' '}
              <strong className="text-primary font-medium">
                {displayName} ({isSuperAdmin ? t('header.superAdmin') : t('header.staff')})
              </strong>
            </span>
          </p>
        </div>
      </div>

      <div className="flex items-center flex-wrap justify-end gap-2 sm:gap-3">
        {/* Theme Toggle */}
        <button
          id="theme-toggle-btn"
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? t('header.switchToLight') : t('header.switchToDark')}
          title={theme === 'dark' ? t('header.switchToLight') : t('header.switchToDark')}
          className="p-2 rounded-xl text-text-muted hover:text-text border border-border hover:bg-bg transition cursor-pointer"
        >
          {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>

        {/* AI Generic Finder */}
        {onOpenAIFinder && (
          <button
            id="header-ai-finder-btn"
            onClick={onOpenAIFinder}
            className="flex items-center gap-1.5 bg-primary/10 hover:bg-primary/20 text-primary border border-primary/30 px-3 py-1.5 rounded-xl text-xs font-bold transition shadow-lg shadow-primary/10 backdrop-blur-md cursor-pointer"
          >
            <Sparkles className="w-3.5 h-3.5 animate-pulse" />
            <span className="hidden sm:inline">{t('header.aiFinder')}</span>
            <span className="sm:hidden">{t('header.aiFinderShort')}</span>
          </button>
        )}

        {/* Quick Action POS Bill */}
        {onOpenNewBill && (
          <button
            id="quick-new-bill-btn"
            onClick={onOpenNewBill}
            className="hidden md:flex items-center gap-1.5 bg-success/10 hover:bg-success/20 text-success border border-success/30 px-2.5 py-1.5 rounded-xl text-xs font-bold transition shadow-md backdrop-blur-md cursor-pointer"
          >
            <Zap className="w-3.5 h-3.5" />
            <span>{t('header.quickPosBill')}</span>
          </button>
        )}

        {/* Quick Action Add Stock */}
        {onOpenAddStock && (
          <button
            id="quick-add-stock-btn"
            onClick={onOpenAddStock}
            className="hidden md:flex items-center gap-1.5 bg-bg hover:bg-border text-text border border-border px-2.5 py-1.5 rounded-xl text-xs font-bold transition backdrop-blur-md cursor-pointer"
          >
            <PlusCircle className="w-3.5 h-3.5 text-primary" />
            <span>{t('header.quickAddStock')}</span>
          </button>
        )}

        {/* Switch User Login Button */}
        {onOpenLoginModal && (
          <button
            id="header-switch-user-btn"
            onClick={onOpenLoginModal}
            className="text-xs text-text-muted hover:text-text border border-border px-2.5 py-1.5 rounded-xl font-medium hover:bg-bg transition backdrop-blur-md cursor-pointer"
          >
            {t('nav.switchUser')}
          </button>
        )}

        {/* Change Own Password — available to any signed-in employee, not
            just from the admin-only Employee Control tab. */}
        {onOpenChangePassword && (
          <button
            id="header-change-password-btn"
            onClick={onOpenChangePassword}
            title={t('header.changePassword')}
            aria-label={t('header.changePassword')}
            className="p-2 rounded-xl text-text-muted hover:text-primary border border-border hover:bg-bg transition cursor-pointer"
          >
            <KeyRound className="w-4 h-4" />
          </button>
        )}

        {/* Log Out */}
        {onLogout && (
          <button
            id="header-logout-btn"
            onClick={onLogout}
            title={t('header.logout')}
            aria-label={t('header.logout')}
            className="p-2 rounded-xl text-text-muted hover:text-rose-600 dark:hover:text-rose-400 border border-border hover:bg-bg transition cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
          </button>
        )}

        {/* Live Date Badge */}
        <div className="bg-bg border border-border px-2.5 sm:px-3 py-1 rounded-xl text-xs font-medium text-text-muted flex items-center gap-1.5 backdrop-blur-md">
          <Calendar className="w-3.5 h-3.5 text-primary" />
          <span className="font-mono text-text">{dateText}</span>
        </div>
      </div>
    </header>
  );
};
