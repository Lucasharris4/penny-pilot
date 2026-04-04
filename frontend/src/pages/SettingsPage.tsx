import { useState, useEffect } from 'react';
import { settingsApi, ApiError } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

export default function SettingsPage() {
  return (
    <div className="p-6 max-w-2xl mx-auto space-y-8">
      <h1 className="text-2xl font-bold">Settings</h1>
      <ChangePasswordSection />
      <hr />
      <SimpleFINSection />
    </div>
  );
}

function ChangePasswordSection() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const passwordsMatch = newPassword === confirmPassword;
  const canSubmit =
    currentPassword.length > 0 &&
    newPassword.length > 0 &&
    confirmPassword.length > 0 &&
    passwordsMatch &&
    !loading;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!passwordsMatch) {
      setError('New passwords do not match');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await settingsApi.changePassword(currentPassword, newPassword);
      setSuccess('Password updated successfully');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to change password');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <h2 className="text-lg font-semibold mb-4">Change Password</h2>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="currentPassword">Current Password</Label>
          <Input
            id="currentPassword"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="newPassword">New Password</Label>
          <Input
            id="newPassword"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="confirmPassword">Confirm New Password</Label>
          <Input
            id="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
          {confirmPassword.length > 0 && !passwordsMatch && (
            <p className="text-sm text-destructive">Passwords do not match</p>
          )}
        </div>
        {error && <p className="text-sm text-destructive">{error}</p>}
        {success && <p className="text-sm text-green-600">{success}</p>}
        <Button type="submit" disabled={!canSubmit}>
          {loading ? 'Updating...' : 'Update Password'}
        </Button>
      </form>
    </section>
  );
}

function SimpleFINSection() {
  const [hasToken, setHasToken] = useState<boolean | null>(null);
  const [setupToken, setSetupToken] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showConfirmRemove, setShowConfirmRemove] = useState(false);

  useEffect(() => {
    settingsApi.getSimpleFINStatus().then((res) => setHasToken(res.hasToken));
  }, []);

  async function handleSaveToken(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await settingsApi.updateSimpleFINToken(setupToken);
      setSuccess('SimpleFIN token updated successfully');
      setSetupToken('');
      setHasToken(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to update token');
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleRemoveToken() {
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      await settingsApi.deleteSimpleFINToken();
      setSuccess('SimpleFIN credentials removed');
      setHasToken(false);
      setShowConfirmRemove(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to remove credentials');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <h2 className="text-lg font-semibold mb-4">SimpleFIN Connection</h2>

      {hasToken === null ? (
        <p className="text-sm text-muted-foreground">Loading...</p>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center gap-2">
            <div
              className={`h-2.5 w-2.5 rounded-full ${hasToken ? 'bg-green-500' : 'bg-muted-foreground'}`}
            />
            <span className="text-sm">
              {hasToken ? 'SimpleFIN credentials saved' : 'No SimpleFIN credentials'}
            </span>
          </div>

          <form onSubmit={handleSaveToken} className="space-y-3">
            <div className="space-y-2">
              <Label htmlFor="setupToken">
                {hasToken ? 'Replace Setup Token' : 'Setup Token'}
              </Label>
              <Input
                id="setupToken"
                type="password"
                placeholder="Paste your SimpleFIN setup token"
                value={setupToken}
                onChange={(e) => setSetupToken(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={!setupToken.trim() || loading}>
              {loading ? 'Saving...' : hasToken ? 'Replace Token' : 'Save Token'}
            </Button>
          </form>

          {hasToken && !showConfirmRemove && (
            <Button
              variant="destructive"
              size="sm"
              onClick={() => setShowConfirmRemove(true)}
            >
              Remove Credentials
            </Button>
          )}

          {showConfirmRemove && (
            <div className="rounded-md border border-destructive/50 p-4 space-y-3">
              <p className="text-sm text-destructive">
                Are you sure? You'll need to generate a new token from SimpleFIN to
                reconnect.
              </p>
              <div className="flex gap-2">
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={handleRemoveToken}
                  disabled={loading}
                >
                  {loading ? 'Removing...' : 'Yes, Remove'}
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowConfirmRemove(false)}
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}
          {success && <p className="text-sm text-green-600">{success}</p>}
        </div>
      )}
    </section>
  );
}
