import React, { useState } from 'react';
import {
  Activity,
  Boxes,
  Calculator,
  ChevronDown,
  CreditCard,
  FileCheck2,
  FileText,
  IdCard,
  LogOut,
  Megaphone,
  Receipt,
  ScanLine,
  ShieldCheck,
  Stethoscope,
  Users,
  Wallet,
  X,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { CurrentUser, TabType } from '../types';

interface SidebarProps {
  currentTab: TabType;
  setCurrentTab: (tab: TabType) => void;
  currentUser?: CurrentUser;
  onOpenSwitchUser?: () => void;
  isOpenMobile?: boolean;
  onCloseMobile?: () => void;
  dueKhataCount?: number;
  neededMedsCount?: number;
  opdCount?: number;
  userPermissions?: TabType[];
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  setCurrentTab,
  currentUser,
  onOpenSwitchUser,
  isOpenMobile = false,
  onCloseMobile = () => {},
  dueKhataCount = 0,
  neededMedsCount = 0,
  opdCount = 0,
  userPermissions,
}) => {
  const { t } = useTranslation();
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({
    accounting: true,
    clinical: true,
    stock: true,
    admin: true,
  });

  const toggleGroup = (group: string) => {
    setOpenGroups(prev => ({ ...prev, [group]: !prev[group] }));
  };

  const isPermitted = (tab: TabType) => {
    if (!currentUser && !userPermissions) return true;
    if (currentUser?.role === 'admin') return true;
    const permissions = userPermissions || currentUser?.permissions || [];
    return permissions.includes(tab);
  };

  const handleTabClick = (tab: TabType) => {
    if (isPermitted(tab)) {
      setCurrentTab(tab);
      onCloseMobile();
    } else {
      alert(t('nav.accessDenied'));
    }
  };

  const activeUserRole =
    currentUser?.role === 'admin' ? t('nav.superAdmin') : currentUser?.name || t('nav.director');

  const isTabActive = (tab: TabType) => currentTab === tab;

  const navItemClass = (active: boolean) =>
    `w-full flex items-center justify-between px-3 py-2.5 rounded-xl transition-all font-medium ${
      active
        ? 'bg-primary/10 border border-primary/30 text-primary font-semibold shadow-sm'
        : 'text-text-muted hover:text-text hover:bg-bg border border-transparent'
    }`;

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpenMobile && (
        <div
          id="mobile-sidebar-backdrop"
          onClick={onCloseMobile}
          className="fixed inset-0 bg-[var(--color-overlay)] backdrop-blur-md z-40 lg:hidden transition-opacity"
        />
      )}

      <aside
        id="app-sidebar"
        className={`glass-panel-subtle glass-sheen fixed lg:static top-0 start-0 bottom-0 z-40 w-72 border-e border-border text-text-muted flex flex-col shrink-0 transition-transform duration-200 ease-in-out ${
          isOpenMobile ? 'translate-x-0' : '-translate-x-full rtl:translate-x-full lg:translate-x-0 lg:rtl:translate-x-0'
        }`}
      >
        {/* Brand Header */}
        <div className="p-4 sm:p-5 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img
              src="/logo.png"
              alt={t('nav.logoAlt')}
              className="w-10 h-10 rounded-xl shadow-lg shadow-primary/25 object-cover"
            />
            <div>
              <h1 className="text-base font-bold text-text tracking-tight flex items-center gap-1.5">
                {t('nav.brand')}
              </h1>
              <div className="flex items-center gap-1.5 mt-0.5">
                <span className="inline-block w-2 h-2 rounded-full bg-success animate-pulse" />
                <p className="text-xs text-text-muted font-medium">{activeUserRole}</p>
              </div>
            </div>
          </div>
          <button
            id="close-mobile-nav-btn"
            onClick={onCloseMobile}
            className="p-1.5 rounded-lg text-text-muted hover:text-text hover:bg-bg lg:hidden"
            aria-label={t('nav.closeSidebar')}
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Sections */}
        <nav className="flex-1 p-3.5 space-y-3 overflow-y-auto text-xs custom-scrollbar">
          {/* GROUP 1: ACCOUNTING & POS */}
          <div className="space-y-1">
            <button
              type="button"
              onClick={() => toggleGroup('accounting')}
              className="w-full flex justify-between items-center px-3 py-1.5 text-text-muted hover:text-text transition rounded-lg hover:bg-bg select-none text-start"
            >
              <span className="text-[10px] font-bold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                <Calculator className="w-3.5 h-3.5 text-primary" />
                {t('nav.groups.accounting')}
              </span>
              <ChevronDown
                className={`w-3.5 h-3.5 text-text-muted transition-transform duration-200 ${
                  openGroups.accounting ? 'rotate-0' : '-rotate-90'
                }`}
              />
            </button>

            {openGroups.accounting && (
              <div className="space-y-1 ps-1">
                {isPermitted('dashboard') && (
                  <button
                    id="nav-dashboard"
                    onClick={() => handleTabClick('dashboard')}
                    className={navItemClass(isTabActive('dashboard'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Activity className="w-4 h-4 text-primary" />
                      <span>{t('nav.dashboard')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('daily-sales') && (
                  <button
                    id="nav-daily-calc"
                    onClick={() => handleTabClick('daily-sales')}
                    className={navItemClass(isTabActive('daily-sales'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Calculator className="w-4 h-4 text-warning" />
                      <span>{t('nav.dailySales')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('pos') && (
                  <button
                    id="nav-pos"
                    onClick={() => handleTabClick('pos')}
                    className={navItemClass(isTabActive('pos'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Receipt className="w-4 h-4 text-success" />
                      <span>{t('nav.pos')}</span>
                    </div>
                    <span className="bg-success/15 text-success border border-success/30 text-[10px] font-bold px-2 py-0.5 rounded-full">
                      {t('nav.live')}
                    </span>
                  </button>
                )}

                {isPermitted('due-khata') && (
                  <button
                    id="nav-due-khata"
                    onClick={() => handleTabClick('due-khata')}
                    className={navItemClass(isTabActive('due-khata'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <CreditCard className="w-4 h-4 text-danger" />
                      <span>{t('nav.dueRegister')}</span>
                    </div>
                    {dueKhataCount > 0 && (
                      <span className="bg-danger/15 text-danger border border-danger/30 text-[10px] font-bold px-2 py-0.5 rounded-full">
                        {dueKhataCount}
                      </span>
                    )}
                  </button>
                )}
              </div>
            )}
          </div>

          {/* GROUP 2: CLINICAL & PATIENTS */}
          <div className="space-y-1 pt-1">
            <button
              type="button"
              onClick={() => toggleGroup('clinical')}
              className="w-full flex justify-between items-center px-3 py-1.5 text-text-muted hover:text-text transition rounded-lg hover:bg-bg select-none text-start"
            >
              <span className="text-[10px] font-bold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                <Stethoscope className="w-3.5 h-3.5 text-accent" />
                {t('nav.groups.clinical')}
              </span>
              <ChevronDown
                className={`w-3.5 h-3.5 text-text-muted transition-transform duration-200 ${
                  openGroups.clinical ? 'rotate-0' : '-rotate-90'
                }`}
              />
            </button>

            {openGroups.clinical && (
              <div className="space-y-1 ps-1">
                {isPermitted('opd') && (
                  <button
                    id="nav-opd"
                    onClick={() => handleTabClick('opd')}
                    className={navItemClass(isTabActive('opd'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Stethoscope className="w-4 h-4 text-accent" />
                      <span>{t('nav.opd')}</span>
                    </div>
                    {opdCount > 0 && (
                      <span className="bg-accent/15 text-accent border border-accent/30 text-[10px] font-bold px-2 py-0.5 rounded-full">
                        {opdCount}
                      </span>
                    )}
                  </button>
                )}

                {isPermitted('patients') && (
                  <button
                    id="nav-patients"
                    onClick={() => handleTabClick('patients')}
                    className={navItemClass(isTabActive('patients'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <IdCard className="w-4 h-4 text-primary" />
                      <span>{t('nav.patients')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('medicine-orders') && (
                  <button
                    id="nav-medicine-orders"
                    onClick={() => handleTabClick('medicine-orders')}
                    className={navItemClass(isTabActive('medicine-orders'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <FileCheck2 className="w-4 h-4 text-primary" />
                      <span>{t('nav.neededOrders')}</span>
                    </div>
                    {neededMedsCount > 0 && (
                      <span className="bg-primary/15 text-primary border border-primary/30 text-[10px] font-bold px-2 py-0.5 rounded-full">
                        {neededMedsCount}
                      </span>
                    )}
                  </button>
                )}
              </div>
            )}
          </div>

          {/* GROUP 3: STOCK & OPERATIONS */}
          <div className="space-y-1 pt-1">
            <button
              type="button"
              onClick={() => toggleGroup('stock')}
              className="w-full flex justify-between items-center px-3 py-1.5 text-text-muted hover:text-text transition rounded-lg hover:bg-bg select-none text-start"
            >
              <span className="text-[10px] font-bold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                <Boxes className="w-3.5 h-3.5 text-accent" />
                {t('nav.groups.stock')}
              </span>
              <ChevronDown
                className={`w-3.5 h-3.5 text-text-muted transition-transform duration-200 ${
                  openGroups.stock ? 'rotate-0' : '-rotate-90'
                }`}
              />
            </button>

            {openGroups.stock && (
              <div className="space-y-1 ps-1">
                {isPermitted('inventory') && (
                  <button
                    id="nav-inventory"
                    onClick={() => handleTabClick('inventory')}
                    className={navItemClass(isTabActive('inventory'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Boxes className="w-4 h-4 text-accent" />
                      <span>{t('nav.medicineStock')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('inward-ocr') && (
                  <button
                    id="nav-inward"
                    onClick={() => handleTabClick('inward-ocr')}
                    className={navItemClass(isTabActive('inward-ocr'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <ScanLine className="w-4 h-4 text-warning" />
                      <span>{t('nav.ocrBills')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('expenses') && (
                  <button
                    id="nav-expenses"
                    onClick={() => handleTabClick('expenses')}
                    className={navItemClass(isTabActive('expenses'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Wallet className="w-4 h-4 text-warning" />
                      <span>{t('nav.expenses')}</span>
                    </div>
                  </button>
                )}
              </div>
            )}
          </div>

          {/* GROUP 4: ADMIN & SETTINGS */}
          <div className="space-y-1 pt-1">
            <button
              type="button"
              onClick={() => toggleGroup('admin')}
              className="w-full flex justify-between items-center px-3 py-1.5 text-text-muted hover:text-text transition rounded-lg hover:bg-bg select-none text-start"
            >
              <span className="text-[10px] font-bold uppercase tracking-wider text-text-muted flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-text-muted" />
                {t('nav.groups.admin')}
              </span>
              <ChevronDown
                className={`w-3.5 h-3.5 text-text-muted transition-transform duration-200 ${
                  openGroups.admin ? 'rotate-0' : '-rotate-90'
                }`}
              />
            </button>

            {openGroups.admin && (
              <div className="space-y-1 ps-1">
                {isPermitted('business-dev') && (
                  <button
                    id="nav-business-dev"
                    onClick={() => handleTabClick('business-dev')}
                    className={navItemClass(isTabActive('business-dev'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Megaphone className="w-4 h-4 text-accent" />
                      <span>{t('nav.campaigns')}</span>
                    </div>
                  </button>
                )}

                {isPermitted('employee-mgmt') && (
                  <button
                    id="nav-employee-mgmt"
                    onClick={() => handleTabClick('employee-mgmt')}
                    className={navItemClass(isTabActive('employee-mgmt'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <Users className="w-4 h-4 text-accent" />
                      <span>{t('nav.employees')}</span>
                    </div>
                  </button>
                )}

                {(isPermitted('invoice-settings') || isPermitted('system-reset')) && (
                  <button
                    id="nav-settings"
                    onClick={() => handleTabClick('invoice-settings')}
                    className={navItemClass(isTabActive('invoice-settings') || isTabActive('system-reset'))}
                  >
                    <div className="flex items-center gap-2.5">
                      <FileText className="w-4 h-4 text-success" />
                      <span>{t('nav.settings')}</span>
                    </div>
                  </button>
                )}
              </div>
            )}
          </div>
        </nav>

        {/* Sidebar Footer with Switch User Button */}
        <div className="p-3 border-t border-border text-[11px] flex items-center justify-between">
          {onOpenSwitchUser && (
            <button
              id="switch-user-btn"
              onClick={onOpenSwitchUser}
              className="text-primary hover:text-text flex items-center gap-1.5 font-semibold transition px-2.5 py-1.5 rounded-xl hover:bg-bg cursor-pointer"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>{t('nav.switchUser')}</span>
            </button>
          )}
          <span className="text-text-muted font-mono text-[10px] bg-bg px-2 py-0.5 rounded-lg border border-border ms-auto">
            v11.0
          </span>
        </div>

        {/* Brand Footer — bottom-left, nodedr branding (consistent across all Nodedr apps) */}
        <a
          href="https://kinetirx.nodedr.com/"
          target="_blank"
          rel="noopener noreferrer"
          className="block px-4 py-2.5 border-t border-border text-center text-[11px] leading-tight text-text-muted/70 hover:text-text-muted hover:underline transition"
        >
          {t('nav.brandCredit')}
        </a>
      </aside>
    </>
  );
};
