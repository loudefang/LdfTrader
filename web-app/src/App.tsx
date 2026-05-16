import { LanguageProvider } from './i18n';
import Navbar from './components/Navbar';
import KlinePage from './pages/KlinePage';

export default function App() {
  return (
    <LanguageProvider>
      <div className="app-shell">
        <Navbar />
        <KlinePage />
      </div>
    </LanguageProvider>
  );
}
