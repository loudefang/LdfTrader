import React, { createContext, useContext, useState } from 'react';
import zh from './zh';
import en from './en';

export type Lang = 'zh' | 'en';
export type Messages = typeof zh;

const LANG_KEY = 'ldftrader_lang';

interface ContextValue {
  lang: Lang;
  t: Messages;
  setLang: (l: Lang) => void;
}

const LangContext = createContext<ContextValue>({
  lang: 'zh',
  t: zh,
  setLang: () => {},
});

export function LanguageProvider({ children }: { children: React.ReactNode }) {
  const [lang, setLangState] = useState<Lang>(
    () => (localStorage.getItem(LANG_KEY) as Lang) || 'zh'
  );

  const setLang = (l: Lang) => {
    localStorage.setItem(LANG_KEY, l);
    setLangState(l);
  };

  const t: Messages = lang === 'zh' ? zh : en;

  return (
    <LangContext.Provider value={{ lang, t, setLang }}>
      {children}
    </LangContext.Provider>
  );
}

export const useT = () => useContext(LangContext);
