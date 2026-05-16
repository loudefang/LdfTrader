import { useT } from '../i18n';

export default function Navbar() {
  const { t, lang, setLang } = useT();

  return (
    <nav className="app-navbar">
      <div className="brand">{t.nav.brand}</div>
      <div className="nav-links">
        <span className="nav-link active">{t.nav.kline}</span>
        <span className="nav-link disabled" title={t.nav.comingSoon}>{t.nav.strategy}</span>
        <span className="nav-link disabled" title={t.nav.comingSoon}>{t.nav.trading}</span>
        <span className="nav-link disabled" title={t.nav.comingSoon}>{t.nav.account}</span>
      </div>
      <button
        className="lang-toggle"
        onClick={() => setLang(lang === 'zh' ? 'en' : 'zh')}
        title={lang === 'zh' ? 'Switch to English' : '切换为中文'}
      >
        {t.nav.langSwitch}
      </button>
    </nav>
  );
}
