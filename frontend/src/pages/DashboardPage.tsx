import { useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import type { DashboardSummaryResponse, CategoryResponse } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Popover, PopoverTrigger, PopoverContent } from '@/components/ui/popover';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { Skeleton } from '@/components/ui/skeleton';
import ColorPicker from '@/components/ColorPicker';

function formatCents(cents: number): string {
  const dollars = Math.abs(cents) / 100;
  const formatted = dollars.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return cents < 0 ? `-$${formatted}` : `$${formatted}`;
}

function formatMonthLabel(month: string): string {
  const [year, m] = month.split('-');
  const date = new Date(Number(year), Number(m) - 1);
  return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
}

function getStartAndEndDates(month: string): { startDate: string; endDate: string } {
  const [year, m] = month.split('-').map(Number);
  const startDate = `${month}-01`;
  const lastDay = new Date(year, m, 0).getDate();
  const endDate = `${month}-${String(lastDay).padStart(2, '0')}`;
  return { startDate, endDate };
}

export default function DashboardPage() {
  const [months, setMonths] = useState<string[]>([]);
  const [selectedMonth, setSelectedMonth] = useState<string>('');
  const [summary, setSummary] = useState<DashboardSummaryResponse | null>(null);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [colorUpdateId, setColorUpdateId] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([api.getAvailableMonths(), api.getCategories()])
      .then(([monthsRes, catsRes]) => {
        setCategories(catsRes);
        setMonths(monthsRes.months);
        if (monthsRes.months.length > 0) {
          setSelectedMonth(monthsRes.months[0]);
        } else {
          setLoading(false);
        }
      })
      .catch(() => {
        setError('Failed to load dashboard data');
        setLoading(false);
      });
  }, []);

  const fetchSummary = useCallback(async (month: string) => {
    setLoading(true);
    setError(null);
    try {
      const { startDate, endDate } = getStartAndEndDates(month);
      const data = await api.getDashboardSummary(startDate, endDate);
      setSummary(data);
    } catch {
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  }, []);

  const handleColorChange = useCallback(async (categoryId: number, newColor: string) => {
    const cat = categories.find(c => c.id === categoryId);
    if (!cat) return;
    setColorUpdateId(categoryId);
    try {
      await api.updateCategory(categoryId, cat.name, cat.icon ?? null, newColor || null);
      setCategories(prev => prev.map(c => c.id === categoryId ? { ...c, color: newColor || null } : c));
      if (selectedMonth) fetchSummary(selectedMonth);
    } catch {
      setError('Failed to update category color');
    } finally {
      setColorUpdateId(null);
    }
  }, [categories, selectedMonth, fetchSummary]);

  useEffect(() => {
    if (selectedMonth) {
      fetchSummary(selectedMonth);
    }
  }, [selectedMonth, fetchSummary]);

  if (months.length === 0 && !loading) {
    return (
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <p className="text-lg text-muted-foreground">No transaction data yet</p>
          <p className="text-sm text-muted-foreground mt-1">
            Link an account and sync to see your dashboard.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
        {months.length > 0 && (
          <Select value={selectedMonth} onValueChange={v => v && setSelectedMonth(v)}>
            <SelectTrigger className="w-52">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {months.map(m => (
                <SelectItem key={m} value={m}>{formatMonthLabel(m)}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {error && (
        <div className="mb-4 p-3 rounded-md bg-destructive/10 text-destructive text-sm flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="text-destructive hover:opacity-70 ml-3 shrink-0">&times;</button>
        </div>
      )}

      {loading ? (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            {[0, 1, 2].map(i => (
              <Card key={i}>
                <CardHeader className="pb-2"><Skeleton className="h-4 w-20" /></CardHeader>
                <CardContent><Skeleton className="h-8 w-28" /></CardContent>
              </Card>
            ))}
          </div>
          <Card>
            <CardHeader><Skeleton className="h-5 w-40" /></CardHeader>
            <CardContent><Skeleton className="h-72 w-full" /></CardContent>
          </Card>
        </>
      ) : summary && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Income</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-2xl font-bold text-green-600">
                  {formatCents(summary.incomeCents)}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Expenses</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-2xl font-bold text-red-600">
                  {formatCents(summary.expensesCents)}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium text-muted-foreground">Net Cash Flow</CardTitle>
              </CardHeader>
              <CardContent>
                <p className={`text-2xl font-bold ${summary.netCents >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                  {formatCents(summary.netCents)}
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Category Breakdown */}
          {summary.byCategory.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Spending by Category</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col md:flex-row items-center gap-8">
                  <div className="w-72 h-72 shrink-0">
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={summary.byCategory}
                          dataKey="amountCents"
                          nameKey="categoryName"
                          cx="50%"
                          cy="50%"
                          innerRadius={60}
                          outerRadius={100}
                          paddingAngle={2}
                        >
                          {summary.byCategory.map((entry, i) => (
                            <Cell key={i} fill={entry.categoryColor} />
                          ))}
                        </Pie>
                        <Tooltip
                          formatter={(value) => formatCents(Number(value))}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                  <div className="flex-1 space-y-2">
                    {summary.byCategory.map((cat, i) => (
                      <div key={i} className="flex items-center justify-between text-sm">
                        <div className="flex items-center gap-2">
                          {cat.categoryId != null ? (
                            <Popover>
                              <PopoverTrigger
                                className="w-3 h-3 rounded-full cursor-pointer ring-offset-background transition-opacity hover:opacity-80 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50"
                                style={{ backgroundColor: cat.categoryColor ?? '#9E9E9E' }}
                                disabled={colorUpdateId === cat.categoryId}
                                aria-label={`Change color for ${cat.categoryName}`}
                              />
                              <PopoverContent className="w-auto">
                                <ColorPicker
                                  label={`${cat.categoryName} color`}
                                  value={cat.categoryColor ?? ''}
                                  onChange={color => handleColorChange(cat.categoryId!, color)}
                                />
                              </PopoverContent>
                            </Popover>
                          ) : (
                            <div
                              className="w-3 h-3 rounded-full"
                              style={{ backgroundColor: cat.categoryColor ?? '#9E9E9E' }}
                            />
                          )}
                          <span>{cat.categoryName}</span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-muted-foreground">{cat.percentage}%</span>
                          <span className="font-medium w-24 text-right">{formatCents(cat.amountCents)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}
