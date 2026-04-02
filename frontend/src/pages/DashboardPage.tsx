import { Button } from '@/components/ui/button';
import { useAuth } from '@/context/AuthContext';

export default function DashboardPage() {
  const { logout } = useAuth();

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-foreground">Penny Pilot</h1>
        <p className="mt-2 text-lg text-muted-foreground">
          Dashboard coming soon
        </p>
        <Button variant="outline" className="mt-4" onClick={logout}>
          Sign out
        </Button>
      </div>
    </div>
  );
}
