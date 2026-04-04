import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';

const navItems = [
  { to: '/transactions', label: 'Transactions' },
  { to: '/accounts', label: 'Accounts' },
  { to: '/dashboard', label: 'Dashboard' },
];

export default function AppLayout() {
  const { logout } = useAuth();

  return (
    <div className="flex min-h-screen bg-background">
      <aside className="w-56 border-r border-sidebar-border bg-sidebar flex flex-col">
        <div className="px-4 py-5">
          <h1 className="text-lg font-bold text-sidebar-foreground">Penny Pilot</h1>
        </div>

        <nav className="flex-1 px-2 space-y-1">
          {navItems.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
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

        <div className="border-t border-sidebar-border p-4">
          <Button variant="outline" size="sm" className="w-full" onClick={logout}>
            Sign out
          </Button>
        </div>
      </aside>

      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
