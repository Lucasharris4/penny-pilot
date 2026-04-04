import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import type { AccountResponse, ProviderResponse, SyncResponse } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';

function formatCents(cents: number): string {
  const dollars = Math.abs(cents) / 100;
  const formatted = dollars.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return cents < 0 ? `-$${formatted}` : `$${formatted}`;
}

function formatRelativeTime(dateStr: string | null): string {
  if (!dateStr) return 'Never';
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays}d ago`;
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<AccountResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Link account dialog
  const [showLinkDialog, setShowLinkDialog] = useState(false);
  const [providers, setProviders] = useState<ProviderResponse[]>([]);
  const [selectedProviderId, setSelectedProviderId] = useState<string>('');
  const [setupToken, setSetupToken] = useState('');
  const [linking, setLinking] = useState(false);
  const [linkError, setLinkError] = useState<string | null>(null);

  // Sync state per account
  const [syncingIds, setSyncingIds] = useState<Set<number>>(new Set());
  const [syncResults, setSyncResults] = useState<Record<number, SyncResponse>>({});

  // Delete confirmation
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchAccounts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getAccounts();
      setAccounts(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts]);

  const selectedProvider = providers.find(p => String(p.id) === selectedProviderId);
  const needsSetupToken = selectedProvider?.name === 'SIMPLEFIN';

  const openLinkDialog = async () => {
    setShowLinkDialog(true);
    setSelectedProviderId('');
    setSetupToken('');
    setLinkError(null);
    try {
      const data = await api.getProviders();
      setProviders(data);
    } catch {
      setLinkError('Failed to load providers');
    }
  };

  const handleLink = async () => {
    if (!selectedProviderId) return;
    setLinking(true);
    setLinkError(null);
    try {
      const linked = await api.linkAccounts(
        Number(selectedProviderId),
        needsSetupToken ? setupToken : undefined,
      );
      setShowLinkDialog(false);

      // Auto-sync all linked accounts
      const newSyncing = new Set(linked.map(a => a.id));
      setSyncingIds(newSyncing);
      setAccounts(linked);

      for (const account of linked) {
        try {
          const result = await api.syncAccount(account.id);
          setSyncResults(prev => ({ ...prev, [account.id]: result }));
        } catch {
          // Sync failure is non-fatal — account is still linked
        } finally {
          setSyncingIds(prev => {
            const next = new Set(prev);
            next.delete(account.id);
            return next;
          });
        }
      }
      // Refresh to get updated balances/sync times
      await fetchAccounts();
    } catch (err) {
      setLinkError(err instanceof Error ? err.message : 'Failed to link accounts');
    } finally {
      setLinking(false);
    }
  };

  const handleSync = async (accountId: number) => {
    setSyncingIds(prev => new Set(prev).add(accountId));
    setSyncResults(prev => { const next = { ...prev }; delete next[accountId]; return next; });
    try {
      const result = await api.syncAccount(accountId);
      setSyncResults(prev => ({ ...prev, [accountId]: result }));
      await fetchAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Sync failed');
    } finally {
      setSyncingIds(prev => {
        const next = new Set(prev);
        next.delete(accountId);
        return next;
      });
    }
  };

  const handleDelete = async () => {
    if (deleteConfirmId === null) return;
    setDeleting(true);
    try {
      await api.deleteAccount(deleteConfirmId);
      setDeleteConfirmId(null);
      await fetchAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete account');
    } finally {
      setDeleting(false);
    }
  };

  const deletingAccount = accounts.find(a => a.id === deleteConfirmId);

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-foreground">Accounts</h1>
        {accounts.length === 0 && !loading && (
          <Button onClick={openLinkDialog}>Link Account</Button>
        )}
      </div>

      {error && (
        <div className="mb-4 p-3 rounded-md bg-destructive/10 text-destructive text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-muted-foreground">Loading accounts...</p>
      ) : accounts.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-lg text-muted-foreground mb-4">No accounts linked yet</p>
            <p className="text-sm text-muted-foreground mb-6">
              Link a bank account to start syncing your transactions.
            </p>
            <Button onClick={openLinkDialog}>Link Account</Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {accounts.map(account => (
            <Card key={account.id}>
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg">{account.accountName}</CardTitle>
                  <span className="text-xs text-muted-foreground uppercase">{account.providerName}</span>
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex items-center justify-between">
                  <div className="space-y-1">
                    <p className="text-2xl font-semibold">{formatCents(account.balanceCents)}</p>
                    <p className="text-sm text-muted-foreground">
                      Last synced: {formatRelativeTime(account.lastSyncedAt)}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleSync(account.id)}
                      disabled={syncingIds.has(account.id)}
                    >
                      {syncingIds.has(account.id) ? 'Syncing...' : 'Sync'}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setDeleteConfirmId(account.id)}
                    >
                      Remove
                    </Button>
                  </div>
                </div>
                {syncResults[account.id] && (
                  <p className="mt-2 text-sm text-muted-foreground">
                    Sync complete: {syncResults[account.id].transactionsAdded} added,{' '}
                    {syncResults[account.id].transactionsUpdated} updated,{' '}
                    {syncResults[account.id].transactionsSkipped} skipped
                  </p>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Link Account Dialog */}
      <Dialog open={showLinkDialog} onOpenChange={setShowLinkDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Link Account</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Provider</Label>
              <Select value={selectedProviderId} onValueChange={v => setSelectedProviderId(v ?? '')}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a provider" />
                </SelectTrigger>
                <SelectContent>
                  {providers.map(p => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name === 'SIMPLEFIN' ? 'SimpleFIN' : p.name === 'MOCK' ? 'Sandbox (Mock)' : p.name}
                      <span className="text-muted-foreground ml-2 text-xs">— {p.description}</span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {needsSetupToken && (
              <div className="space-y-2">
                <Label htmlFor="setup-token">SimpleFIN Setup Token</Label>
                <Input
                  id="setup-token"
                  value={setupToken}
                  onChange={e => setSetupToken(e.target.value)}
                  placeholder="Paste your setup token from simplefin.org"
                />
                <p className="text-xs text-muted-foreground">
                  Get a setup token from your SimpleFIN account at simplefin.org
                </p>
              </div>
            )}

            {linkError && (
              <p className="text-sm text-destructive">{linkError}</p>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowLinkDialog(false)}>Cancel</Button>
            <Button
              onClick={handleLink}
              disabled={linking || !selectedProviderId || (needsSetupToken && !setupToken.trim())}
            >
              {linking ? 'Linking...' : 'Link Account'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmId !== null} onOpenChange={() => setDeleteConfirmId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Remove Account</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Are you sure you want to remove <strong>{deletingAccount?.accountName}</strong>? This will
            permanently delete all synced transactions for this account.
          </p>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteConfirmId(null)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete} disabled={deleting}>
              {deleting ? 'Removing...' : 'Remove'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
