import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import ar from './locales/ar.json';
import en from './locales/en.json';

/** Arabic is the default application language. The user's choice is remembered in localStorage. */
export const DEFAULT_LANGUAGE = 'ar';
export const LANGUAGE_STORAGE_KEY = 'kinetirx-lang';

export type AppLanguage = 'ar' | 'en';

export const SUPPORTED_LANGUAGES: { code: AppLanguage; label: string }[] = [
  { code: 'ar', label: 'العربية' },
  { code: 'en', label: 'English' },
];

function storedLanguage(): AppLanguage {
  try {
    const v = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (v === 'ar' || v === 'en') return v;
  } catch {
    // storage unavailable: fall back to the default
  }
  return DEFAULT_LANGUAGE;
}

/** Keeps the document's language and direction in sync with the active language. */
export function applyDocumentLanguage(lng: string): void {
  const isArabic = lng.startsWith('ar');
  document.documentElement.lang = isArabic ? 'ar' : 'en';
  document.documentElement.dir = isArabic ? 'rtl' : 'ltr';
}

void i18n.use(initReactI18next).init({
  resources: {
    ar: { translation: ar },
    en: { translation: en },
  },
  lng: storedLanguage(),
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
  returnNull: false,
});

applyDocumentLanguage(i18n.language);
i18n.on('languageChanged', lng => {
  applyDocumentLanguage(lng);
  try {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, lng.startsWith('ar') ? 'ar' : 'en');
  } catch {
    // ignore
  }
});

export default i18n;
