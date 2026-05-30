import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import type { CategoryResponse, CategoryRuleResponse } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

export default function CategoriesPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [rules, setRules] = useState<CategoryRuleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

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

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-foreground">Categories</h1>
        <Button>+ Category</Button>
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
          <Button className="mt-4">+ Category</Button>
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

                  <Button variant="outline" size="sm">Edit</Button>
                  <Button variant="outline" size="sm">Delete</Button>
                </div>

                {isExpanded && (
                  <div className="bg-muted/30 border-t border-border px-4 py-3">
                    <p className="text-sm text-muted-foreground italic">Rules will appear here.</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
