import { NavLink, Outlet } from 'react-router-dom';
import clsx from 'clsx';
import { FileSpreadsheet, Gavel, LayoutDashboard, Languages, LogOut, Moon, Package, Sun, Truck } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { useI18n, type MessageKey } from '@/lib/i18n';
import { initials } from '@/lib/format';
import { useTheme } from '@/lib/theme';
import { Logo } from './Logo';
import { IconButton } from './ui/Button';

const NAV: Array<{ to: string; key: MessageKey; icon: typeof LayoutDashboard }> = [
  { to: '/', key: 'nav.overview', icon: LayoutDashboard },
  { to: '/statements', key: 'nav.statements', icon: FileSpreadsheet },
  { to: '/disputes', key: 'nav.disputes', icon: Gavel },
  { to: '/orders', key: 'nav.orders', icon: Package },
  { to: '/couriers', key: 'nav.couriers', icon: Truck },
];

export function AppShell() {
  const { t, lang, setLang } = useI18n();
  const { session, logout, isStaff } = useAuth();
  const { theme, toggle } = useTheme();

  return (
    <div className="min-h-full">
      <header className="sticky top-0 z-30 border-b border-line bg-bg/80 backdrop-blur-md">
        <div className="mx-auto flex h-14 max-w-[1280px] items-center gap-2 px-4 sm:gap-3">
          <div className="flex items-center gap-2.5">
            <Logo />
            <span className="whitespace-nowrap text-[14px] font-semibold tracking-tight">{t('app.name')}</span>
            <span className="hidden text-[13px] text-faint lg:inline">/</span>
            <span className="hidden whitespace-nowrap text-[13px] text-muted lg:inline">{t('app.tagline')}</span>
          </div>
          <div className="ml-auto flex items-center gap-1 sm:gap-1.5">
            {session && (
              <div className="mr-1 hidden items-center gap-2 rounded-full border border-line bg-panel py-0.5 pl-0.5 pr-2.5 sm:flex" data-testid="user-chip">
                <span className={clsx('inline-flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-semibold',
                  isStaff ? 'bg-amber-500/15 text-amber-700 dark:text-amber-300' : 'bg-accent/15 text-accent')}>
                  {initials(session.username)}
                </span>
                <span className="font-mono text-[12px]">{session.username}</span>
                <span className="text-[10px] font-semibold uppercase tracking-wide text-faint">{t(`role.${session.role}`)}</span>
              </div>
            )}
            <IconButton onClick={() => setLang(lang === 'en' ? 'fr' : 'en')} title={t('lang.switch')} aria-label={t('lang.switch')} data-testid="lang-toggle">
              <span className="flex items-center gap-1 text-[11px] font-semibold"><Languages className="h-3.5 w-3.5" />{lang.toUpperCase()}</span>
            </IconButton>
            <IconButton onClick={toggle} title={t('actions.theme')} aria-label={t('actions.theme')} data-testid="theme-toggle">
              {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </IconButton>
            <IconButton onClick={logout} title={t('actions.logout')} aria-label={t('actions.logout')} data-testid="logout">
              <LogOut className="h-4 w-4" />
            </IconButton>
          </div>
        </div>
        <nav className="scrollbar-thin mx-auto flex max-w-[1280px] gap-1 overflow-x-auto px-3" aria-label="Main">
          {NAV.map(({ to, key, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === '/'}
              className={({ isActive }) => clsx(
                'focus-ring relative inline-flex items-center gap-1.5 whitespace-nowrap rounded-md px-2.5 pb-2.5 pt-1.5 text-[13px] font-medium transition-colors',
                isActive ? 'text-fg after:absolute after:inset-x-2 after:bottom-0 after:h-0.5 after:rounded-full after:bg-fg' : 'text-muted hover:text-fg',
              )}>
              <Icon className="h-3.5 w-3.5" />{t(key)}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-[1280px] space-y-5 px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
