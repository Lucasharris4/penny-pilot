import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import logo from '@/assets/logo.png';

const navItems = [
  { to: '/transactions', label: 'Transactions' },
  { to: '/accounts', label: 'Accounts' },
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/categories', label: 'Categories' },
];

function NavContents({ onNavigate, onCollapse }: { onNavigate?: () => void; onCollapse?: () => void }) {
  const { logout } = useAuth();

  return (
    <>
      <div className="flex items-center justify-between pl-4 pr-2 py-5">
        <img src={logo} alt="Penny Pilot" className="w-34" />
        {onCollapse && (
          <button
            onClick={onCollapse}
            className="p-1 text-sidebar-foreground hover:bg-sidebar-accent/50 rounded-md cursor-pointer"
            aria-label="Collapse sidebar"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="11 4 6 8 11 12" />
            </svg>
          </button>
        )}
      </div>

      <nav className="flex-1 px-2 space-y-1">
        {navItems.map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={onNavigate}
            className={({ isActive }) =>
              `block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                  : 'text-sidebar-foreground hover:bg-sidebar-accent/50'
              }`
            }
          >
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="border-t border-sidebar-border p-2 space-y-1">
        <NavLink
          to="/settings"
          onClick={onNavigate}
          className={({ isActive }) =>
            `block rounded-md px-3 py-2 text-sm font-medium transition-colors ${
              isActive
                ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                : 'text-sidebar-foreground hover:bg-sidebar-accent/50'
            }`
          }
        >
          Settings
        </NavLink>
        <div className="px-1 pt-1">
          <Button variant="outline" size="sm" className="w-full" onClick={logout}>
            Sign out
          </Button>
        </div>
      </div>
    </>
  );
}

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [desktopOpen, setDesktopOpen] = useState(true);

  return (
    <div className="flex min-h-screen bg-background">
      {/* Desktop sidebar */}
      <aside
        className={`hidden md:flex border-r border-sidebar-border bg-sidebar flex-col shrink-0 transition-all duration-200 ${
          desktopOpen ? 'w-56' : 'w-12'
        }`}
      >
        {desktopOpen ? (
          <NavContents onCollapse={() => setDesktopOpen(false)} />
        ) : (
          <div className="flex flex-col items-center pt-4">
            <button
              onClick={() => setDesktopOpen(true)}
              className="p-2 text-sidebar-foreground hover:bg-sidebar-accent/50 rounded-md cursor-pointer"
              aria-label="Expand sidebar"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="5 4 10 8 5 12" />
              </svg>
            </button>
          </div>
        )}
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 md:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Mobile sidebar */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-56 bg-sidebar border-r border-sidebar-border flex flex-col transition-transform md:hidden ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <NavContents onNavigate={() => setMobileOpen(false)} />
      </aside>

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Mobile top bar */}
        <header className="flex items-center h-14 px-4 border-b border-border md:hidden shrink-0">
          <button
            onClick={() => setMobileOpen(true)}
            className="p-2 -ml-2 text-foreground"
            aria-label="Open menu"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
              <line x1="3" y1="5" x2="17" y2="5" />
              <line x1="3" y1="10" x2="17" y2="10" />
              <line x1="3" y1="15" x2="17" y2="15" />
            </svg>
          </button>
          <img src={logo} alt="Penny Pilot" className="h-8 ml-3" />
        </header>

        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
