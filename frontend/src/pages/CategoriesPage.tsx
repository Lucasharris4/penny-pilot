import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import type { CategoryResponse, CategoryRuleResponse } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import ColorPicker from '@/components/ColorPicker';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [rules, setRules] = useState<CategoryRuleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  const [categoryDialog, setCategoryDialog] = useState<{ open: boolean; editing: CategoryResponse | null }>({ open: false, editing: null });
  const [catName, setCatName] = useState('');
  const [catIcon, setCatIcon] = useState('');
  const [catColor, setCatColor] = useState('');
  const [catError, setCatError] = useState<string | null>(null);
  const [catSaving, setCatSaving] = useState(false);

  const [deleteCatTarget, setDeleteCatTarget] = useState<CategoryResponse | null>(null);
  const [deleteCatError, setDeleteCatError] = useState<string | null>(null);
  const [deleteCatSaving, setDeleteCatSaving] = useState(false);

  const [ruleDialog, setRuleDialog] = useState<{ open: boolean; editing: CategoryRuleResponse | null; categoryId: number | null }>({ open: false, editing: null, categoryId: null });
  const [rulePattern, setRulePattern] = useState('');
  const [ruleError, setRuleError] = useState<string | null>(null);
  const [ruleSaving, setRuleSaving] = useState(false);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [cats, rls] = await Promise.all([api.getCategories(), api.listRules()]);
      setCategories(cats);
      setRules(rls);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load categories');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const toggleAccordion = (id: number) => {
    setExpandedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const rulesForCategory = (catId: number) =>
    rules.filter(r => r.categoryId === catId).sort((a, b) => b.priority - a.priority);

  const openCreateCategory = () => {
    setCategoryDialog({ open: true, editing: null });
    setCatName('');
    setCatIcon('');
    setCatColor('');
    setCatError(null);
  };

  const openEditCategory = (cat: CategoryResponse) => {
    setCategoryDialog({ open: true, editing: cat });
    setCatName(cat.name);
    setCatIcon(cat.icon ?? '');
    setCatColor(cat.color ?? '');
    setCatError(null);
  };

  const confirmDeleteCategory = async () => {
    if (!deleteCatTarget) return;
    setDeleteCatSaving(true);
    setDeleteCatError(null);
    try {
      await api.deleteCategory(deleteCatTarget.id);
      setDeleteCatTarget(null);
      await fetchData();
    } catch (err) {
      setDeleteCatError(err instanceof Error ? err.message : 'Failed to delete category');
    } finally {
      setDeleteCatSaving(false);
    }
  };

  const openCreateRule = (categoryId: number) => {
    setRuleDialog({ open: true, editing: null, categoryId });
    setRulePattern('');
    setRuleError(null);
  };

  const openEditRule = (rule: CategoryRuleResponse) => {
    setRuleDialog({ open: true, editing: rule, categoryId: rule.categoryId });
    setRulePattern(rule.matchPattern);
    setRuleError(null);
  };

  const saveRuleDialog = async () => {
    if (!rulePattern.trim()) return;
    setRuleSaving(true);
    setRuleError(null);
    try {
      if (ruleDialog.editing) {
        await api.updateRule(ruleDialog.editing.id, rulePattern.trim(), ruleDialog.editing.categoryId);
      } else {
        await api.createRule(rulePattern.trim(), ruleDialog.categoryId!);
      }
      setRuleDialog({ open: false, editing: null, categoryId: null });
      await fetchData();
    } catch (err) {
      setRuleError(err instanceof Error ? err.message : 'Failed to save rule');
    } finally {
      setRuleSaving(false);
    }
  };

  const deleteRule = async (rule: CategoryRuleResponse) => {
    try {
      await api.deleteRule(rule.id);
      await fetchData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete rule');
    }
  };

  const saveCategoryDialog = async () => {
    if (!catName.trim()) return;
    setCatSaving(true);
    setCatError(null);
    try {
      if (categoryDialog.editing) {
        await api.updateCategory(categoryDialog.editing.id, catName.trim(), catIcon || null, catColor || null);
      } else {
        await api.createCategory(catName.trim(), catIcon || undefined, catColor || undefined);
      }
      setCategoryDialog({ open: false, editing: null });
      await fetchData();
    } catch (err) {
      setCatError(err instanceof Error ? err.message : 'Failed to save category');
    } finally {
      setCatSaving(false);
    }
  };

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-foreground">Categories</h1>
        <Button onClick={openCreateCategory}>+ Category</Button>
      </div>

      {error && (
        <div className="bg-destructive/10 text-destructive p-3 rounded-md text-sm flex items-center justify-between mb-4">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="text-destructive hover:opacity-70 ml-3 shrink-0">&times;</button>
        </div>
      )}

      {loading ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3 px-4 py-3 border rounded-md">
              <Skeleton className="h-4 w-4 rounded-full" />
              <Skeleton className="h-4 w-32" />
              <Skeleton className="h-5 w-14 rounded-full ml-auto" />
              <Skeleton className="h-8 w-14" />
              <Skeleton className="h-8 w-16" />
            </div>
          ))}
        </div>
      ) : categories.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-lg text-muted-foreground">No categories yet</p>
          <p className="text-sm text-muted-foreground mt-1">Create a category to start organizing your transactions.</p>
          <Button className="mt-4" onClick={openCreateCategory}>+ Category</Button>
        </div>
      ) : (
        <div className="border rounded-md divide-y divide-border">
          {categories.map(cat => {
            const catRules = rulesForCategory(cat.id);
            const isExpanded = expandedIds.has(cat.id);
            return (
              <div key={cat.id}>
                <div className="flex items-center gap-3 px-4 py-3">
                  <button
                    onClick={() => toggleAccordion(cat.id)}
                    className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
                    aria-label={isExpanded ? 'Collapse' : 'Expand'}
                  >
                    <svg
                      width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                      className={`transition-transform ${isExpanded ? 'rotate-90' : ''}`}
                    >
                      <polyline points="6 4 10 8 6 12" />
                    </svg>
                  </button>

                  <div
                    className="w-3.5 h-3.5 rounded-full shrink-0 border border-black/10"
                    style={{ backgroundColor: cat.color ?? '#9E9E9E' }}
                  />

                  <span className="font-medium text-foreground flex-1 min-w-0 truncate">
                    {cat.icon ? `${cat.icon} ` : ''}{cat.name}
                  </span>

                  <Badge variant="secondary" className="shrink-0">
                    {catRules.length} {catRules.length === 1 ? 'rule' : 'rules'}
                  </Badge>

                  <Button variant="outline" size="sm" onClick={() => openEditCategory(cat)}>Edit</Button>
                  <Button variant="outline" size="sm" onClick={() => { setDeleteCatTarget(cat); setDeleteCatError(null); }}>Delete</Button>
                </div>

                {isExpanded && (
                  <div className="bg-muted/30 border-t border-border">
                    {catRules.length === 0 ? (
                      <div className="px-10 py-3 text-sm text-muted-foreground">No rules configured.</div>
                    ) : (
                      <div className="divide-y divide-border/50">
                        {catRules.map(rule => (
                          <div key={rule.id} className="flex items-center gap-3 px-10 py-2.5">
                            <span className="font-mono text-sm text-foreground flex-1">{rule.matchPattern}</span>
                            <Button variant="ghost" size="sm" onClick={() => openEditRule(rule)}>Edit</Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-destructive hover:text-destructive"
                              onClick={() => deleteRule(rule)}
                            >
                              Delete
                            </Button>
                          </div>
                        ))}
                      </div>
                    )}
                    <div className="px-10 py-2.5">
                      <Button variant="ghost" size="sm" className="text-muted-foreground" onClick={() => openCreateRule(cat.id)}>+ Add rule</Button>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
      <Dialog open={ruleDialog.open} onOpenChange={open => setRuleDialog(d => ({ ...d, open }))}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{ruleDialog.editing ? 'Edit Rule' : 'New Rule'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="rule-pattern">Pattern</Label>
              <Input
                id="rule-pattern"
                value={rulePattern}
                onChange={e => setRulePattern(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && saveRuleDialog()}
                placeholder="e.g. STARBUCKS* or *AMAZON*"
              />
              <p className="text-xs text-muted-foreground">Glob syntax — <code>*</code> matches any characters. Case-insensitive.</p>
            </div>
            {ruleError && <p className="text-sm text-destructive">{ruleError}</p>}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRuleDialog({ open: false, editing: null, categoryId: null })}>Cancel</Button>
            <Button onClick={saveRuleDialog} disabled={!rulePattern.trim() || ruleSaving}>
              {ruleSaving ? 'Saving…' : ruleDialog.editing ? 'Save' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleteCatTarget} onOpenChange={open => { if (!open) setDeleteCatTarget(null); }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete "{deleteCatTarget?.name}"?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground py-2">
            Transactions in this category will become uncategorized.
          </p>
          {deleteCatError && <p className="text-sm text-destructive">{deleteCatError}</p>}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteCatTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={confirmDeleteCategory} disabled={deleteCatSaving}>
              {deleteCatSaving ? 'Deleting…' : 'Delete'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={categoryDialog.open} onOpenChange={open => setCategoryDialog(d => ({ ...d, open }))}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{categoryDialog.editing ? 'Edit Category' : 'New Category'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label htmlFor="cat-name">Name</Label>
              <Input
                id="cat-name"
                value={catName}
                onChange={e => setCatName(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && saveCategoryDialog()}
                placeholder="e.g., Groceries"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="cat-icon">Icon (optional)</Label>
              <Input
                id="cat-icon"
                value={catIcon}
                onChange={e => setCatIcon(e.target.value)}
                placeholder="e.g., 🛒"
              />
            </div>
            <ColorPicker value={catColor} onChange={setCatColor} />
            {catError && <p className="text-sm text-destructive">{catError}</p>}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCategoryDialog({ open: false, editing: null })}>Cancel</Button>
            <Button onClick={saveCategoryDialog} disabled={!catName.trim() || catSaving}>
              {catSaving ? 'Saving…' : categoryDialog.editing ? 'Save' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
