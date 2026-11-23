import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import clsx from 'clsx';
import { CircleHelp, FileSpreadsheet, Gavel, LayoutDashboard, Languages, LogOut, Moon, Package, Sun, Truck } from 'lucide-react';
import { useMe } from '@/lib/queries';
import { HowItWorks } from './HowItWorks';
import { BRAND, productTitle } from '@/brand/brand';
import { useAuth } from '@/lib/auth';
import { useI18n, type MessageKey } from '@/lib/i18n';
import { initials } from '@/lib/format';
import { useTheme } from '@/lib/theme';
import { ProductSwitcher } from './ProductSwitcher';
import { IconButton } from './ui/Button';
import { Popover, menuItem } from './ui/Popover';

const NAV: Array<{ to: string; key: MessageKey; icon: typeof LayoutDashboard }> = [
  { to: '/', key: 'nav.overview', icon: LayoutDashboard },
  { to: '/statements', key: 'nav.statements', icon: FileSpreadsheet },
  { to: '/disputes', key: 'nav.disputes', icon: Gavel },
  { to: '/orders', key: 'nav.orders', icon: Package },
  { to: '/couriers', key: 'nav.couriers', icon: Truck },
];

/** Keeps the browser tab title in sync with the current section: "Disputes · MSE Settle". */
function useDocumentTitle() {
  const { t } = useI18n();
  const { pathname } = useLocation();
  useEffect(() => {
    const item = [...NAV].reverse().find((n) => (n.to === '/' ? pathname === '/' : pathname.startsWith(n.to)));
    document.title = item ? `${t(item.key)} · ${productTitle()}` : productTitle();
  }, [pathname, t]);
}

function UserMenu({ onHowItWorks }: { onHowItWorks: () => void }) {
  const { t } = useI18n();
  const { session, logout, isStaff } = useAuth();
  const me = useMe(!!session && !isStaff);
  if (!session) return null;
  const who = isStaff ? t(`role.${session.role}`) : me.data?.merchantName ?? t(`role.${session.role}`);
  const avatar = 'avatar h-7 w-7 text-[11px]';
  return (
    <Popover label={t('user.menu')} align="end" testId="user-menu"
      trigger={(open) => (
        <span className={clsx('flex items-center gap-2 rounded-full border border-line bg-panel py-0.5 pe-3 ps-0.5 transition-colors hover:bg-subtle', open && 'bg-subtle')}
          data-testid="user-chip">
          <span className={avatar}>{initials(session.username)}</span>
          <span className="hidden text-start leading-tight sm:block">
            <span className="block text-[12.5px] font-medium">{session.username}</span>
            <span className="block text-[10.5px] text-faint">{who}</span>
          </span>
        </span>
      )}>
      {(close) => (
        <>
          <div className="flex items-center gap-2.5 border-b border-line px-2.5 pb-2.5 pt-1.5">
            <span className={avatar}>{initials(session.username)}</span>
            <div className="min-w-0">
              <p className="truncate text-[13px] font-medium">{session.username}</p>
              <p className="truncate text-[12px] text-muted">
                {isStaff ? t(`role.${session.role}`) : `${me.data?.merchantName ?? t('user.merchant', { id: session.merchantId ?? '' })} · ${t(`role.${session.role}`)}`}
              </p>
            </div>
          </div>
          <div className="pt-1.5">
            <button type="button" role="menuitem" className={menuItem} data-testid="how-it-works-open" onClick={() => { close(); onHowItWorks(); }}>
              <CircleHelp className="h-4 w-4 text-muted" aria-hidden />{t('help.howItWorks')}
            </button>
            <button type="button" role="menuitem" className={menuItem} data-testid="logout" onClick={() => { close(); logout(); }}>
              <LogOut className="h-4 w-4 text-muted" aria-hidden />{t('actions.logout')}
            </button>
          </div>
        </>
      )}
    </Popover>
  );
}

export function AppShell() {
  const { t, lang, setLang } = useI18n();
  const { theme, toggle } = useTheme();
  const [howOpen, setHowOpen] = useState(false);
  useDocumentTitle();

  return (
    <div className="flex min-h-full flex-col">
      <a href="#main" className="sr-only focus:not-sr-only focus:absolute focus:start-2 focus:top-2 focus:z-50 focus:rounded-md focus:bg-panel focus:px-3 focus:py-2">
        {t('a11y.skip')}
      </a>
      <header className="sticky top-0 z-30 border-b border-line bg-bg/85 backdrop-blur-md">
        <div className="mx-auto flex h-14 max-w-[1280px] items-center gap-2 px-4 sm:gap-3 sm:px-6">
          <ProductSwitcher />
          <span className="hidden h-5 w-px bg-line lg:block" aria-hidden />
          <span className="hidden whitespace-nowrap text-[13px] text-muted lg:inline">{t('app.tagline')}</span>
          <div className="ms-auto flex items-center gap-1 sm:gap-1.5">
            <IconButton onClick={() => setLang(lang === 'en' ? 'fr' : 'en')} title={t('lang.switch')} aria-label={t('lang.switch')} data-testid="lang-toggle">
              <span className="flex items-center gap-1 text-[11px] font-semibold"><Languages className="h-3.5 w-3.5" />{lang.toUpperCase()}</span>
            </IconButton>
            <IconButton onClick={toggle} title={t('actions.theme')} aria-label={t('actions.theme')} data-testid="theme-toggle">
              {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </IconButton>
            <span className="mx-1 hidden h-5 w-px bg-line sm:block" aria-hidden />
            <UserMenu onHowItWorks={() => setHowOpen(true)} />
          </div>
        </div>
        <nav className="scrollbar-thin mx-auto flex max-w-[1280px] gap-1 overflow-x-auto px-3 sm:px-5" aria-label={t('a11y.mainNav')}>
          {NAV.map(({ to, key, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === '/'}
              className={({ isActive }) => clsx(
                'focus-ring relative inline-flex items-center gap-1.5 whitespace-nowrap rounded-md px-2.5 pb-2.5 pt-1.5 text-[13px] font-medium transition-colors',
                isActive ? 'text-accent after:absolute after:inset-x-2 after:bottom-0 after:h-0.5 after:rounded-full after:bg-accent' : 'text-muted hover:text-fg',
              )}>
              {({ isActive }) => (
                <>
                  <Icon className="h-3.5 w-3.5" strokeWidth={2} aria-hidden />{t(key)}
                  {/* the destination dot from the logo marks where you are */}
                  {isActive && <span className="h-1.5 w-1.5 rounded-full bg-signal" aria-hidden />}
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </header>
      <main id="main" className="mx-auto w-full max-w-[1280px] flex-1 space-y-6 px-4 py-6 sm:px-6">
        <Outlet />
      </main>
      <footer className="mx-auto flex w-full max-w-[1280px] flex-wrap items-center justify-between gap-2 border-t border-line px-4 py-4 text-[12px] text-faint sm:px-6">
        <span>{productTitle()} · {t('footer.part', { brand: BRAND.suite })}</span>
        <span>© {new Date().getFullYear()} {BRAND.suite}</span>
      </footer>
      <HowItWorks open={howOpen} onOpenChange={setHowOpen} />
    </div>
  );
}
