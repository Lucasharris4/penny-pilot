import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api } from '@/lib/api';
import type { TransactionResponse, CategoryResponse, TransactionFilters } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Checkbox } from '@/components/ui/checkbox';
import { Badge } from '@/components/ui/badge';
import ColorPicker from '@/components/ColorPicker';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';

const PAGE_SIZE = 20;

function formatCents(cents: number): string {
  return (cents / 100).toFixed(2);
}

function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-');
  return `${month}/${day}/${year}`;
}

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<string>('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortField, setSortField] = useState('date');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  // Selection
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  // Inline category edit
  const [editingId, setEditingId] = useState<number | null>(null);

  // New category dialog
  const [showNewCategory, setShowNewCategory] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [newCategoryIcon, setNewCategoryIcon] = useState('');
  const [newCategoryColor, setNewCategoryColor] = useState('');
  const [pendingCategoryTxnId, setPendingCategoryTxnId] = useState<number | null>(null);
  const [pendingBulk, setPendingBulk] = useState(false);

  // Bulk categorize dialog
  const [showBulkDialog, setShowBulkDialog] = useState(false);
  const [bulkCategoryId, setBulkCategoryId] = useState<string>('');

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters: TransactionFilters = {
        page,
        size: PAGE_SIZE,
        sort: `${sortField},${sortDir}`,
      };
      if (search) filters.search = search;
      if (categoryFilter) filters.categoryId = Number(categoryFilter);
      if (startDate) filters.startDate = startDate;
      if (endDate) filters.endDate = endDate;

      const result = await api.getTransactions(filters);
      setTransactions(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  }, [page, search, categoryFilter, startDate, endDate, sortField, sortDir]);

  const fetchCategories = useCallback(async () => {
    try {
      const cats = await api.getCategories();
      setCategories(cats);
    } catch {
      // Categories failing shouldn't block the page
    }
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const handleSort = (field: string) => {
    if (sortField === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('desc');
    }
    setPage(0);
  };

  const handleSearch = () => {
    setPage(0);
    fetchTransactions();
  };

  const toggleSelect = (id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === transactions.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(transactions.map(t => t.id)));
    }
  };

  const handleCategoryChange = async (txnId: number, categoryId: string) => {
    if (categoryId === '__new__') {
      setPendingCategoryTxnId(txnId);
      setPendingBulk(false);
      setShowNewCategory(true);
      setEditingId(null);
      return;
    }

    const txn = transactions.find(t => t.id === txnId);
    if (!txn) return;

    try {
      const catId = categoryId === '__none__' ? null : Number(categoryId);
      await api.updateTransaction(txnId, {
        categoryId: catId,
        amountCents: txn.amountCents,
        transactionType: txn.transactionType,
        description: txn.description,
        merchantName: txn.merchantName,
        date: txn.date,
      });
      await fetchTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update category');
    }
    setEditingId(null);
  };

  const handleBulkCategorize = async () => {
    if (!bulkCategoryId || selectedIds.size === 0) return;

    if (bulkCategoryId === '__new__') {
      setPendingBulk(true);
      setPendingCategoryTxnId(null);
      setShowNewCategory(true);
      setShowBulkDialog(false);
      return;
    }

    try {
      await api.bulkCategorize(Array.from(selectedIds), Number(bulkCategoryId));
      setSelectedIds(new Set());
      setShowBulkDialog(false);
      setBulkCategoryId('');
      await fetchTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to bulk categorize');
    }
  };

  const handleCreateCategory = async () => {
    if (!newCategoryName.trim()) return;

    try {
      const created = await api.createCategory(
        newCategoryName.trim(),
        newCategoryIcon || undefined,
        newCategoryColor || undefined
      );
      await fetchCategories();
      setShowNewCategory(false);
      setNewCategoryName('');
      setNewCategoryIcon('');
      setNewCategoryColor('');

      if (pendingBulk && selectedIds.size > 0) {
        await api.bulkCategorize(Array.from(selectedIds), created.id);
        setSelectedIds(new Set());
        setBulkCategoryId('');
        await fetchTransactions();
      } else if (pendingCategoryTxnId != null) {
        const txn = transactions.find(t => t.id === pendingCategoryTxnId);
        if (txn) {
          await api.updateTransaction(pendingCategoryTxnId, {
            categoryId: created.id,
            amountCents: txn.amountCents,
            transactionType: txn.transactionType,
            description: txn.description,
            merchantName: txn.merchantName,
            date: txn.date,
          });
          await fetchTransactions();
        }
      }

      setPendingCategoryTxnId(null);
      setPendingBulk(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create category');
    }
  };

  const sortIndicator = (field: string) => {
    if (sortField !== field) return '';
    return sortDir === 'asc' ? ' ↑' : ' ↓';
  };

  return (
    <div className="p-6 max-w-7xl mx-auto">
      <h1 className="text-2xl font-bold text-foreground mb-4">Transactions</h1>

      <div className="space-y-4">
        {/* Filters */}
        <div className="flex flex-wrap gap-3 items-end">
          <div className="space-y-1">
            <Label htmlFor="search">Search</Label>
            <Input
              id="search"
              placeholder="Description or merchant..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              className="w-56"
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="category-filter">Category</Label>
            <Select value={categoryFilter} onValueChange={v => { setCategoryFilter(!v || v === '__all__' ? '' : v); setPage(0); }}>
              <SelectTrigger className="w-40" id="category-filter">
                <SelectValue placeholder="All" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="__all__">All</SelectItem>
                {categories.map(c => (
                  <SelectItem key={c.id} value={String(c.id)}>
                    {c.icon ? `${c.icon} ` : ''}{c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1">
            <Label htmlFor="start-date">From</Label>
            <Input
              id="start-date"
              type="date"
              value={startDate}
              onChange={e => { setStartDate(e.target.value); setPage(0); }}
              className="w-40"
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="end-date">To</Label>
            <Input
              id="end-date"
              type="date"
              value={endDate}
              onChange={e => { setEndDate(e.target.value); setPage(0); }}
              className="w-40"
            />
          </div>
          <Button variant="outline" onClick={handleSearch}>Filter</Button>
        </div>

        {/* Bulk actions */}
        {selectedIds.size > 0 && (
          <div className="flex items-center gap-3 p-3 bg-muted rounded-md">
            <span className="text-sm text-muted-foreground">
              {selectedIds.size} selected
            </span>
            <Button size="sm" onClick={() => setShowBulkDialog(true)}>
              Set Category
            </Button>
            <Button size="sm" variant="outline" onClick={() => setSelectedIds(new Set())}>
              Clear
            </Button>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="bg-destructive/10 text-destructive p-3 rounded-md text-sm flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => setError(null)} className="text-destructive hover:opacity-70 ml-3 shrink-0">&times;</button>
          </div>
        )}

        {/* Table */}
        {loading ? (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-10"><Skeleton className="h-4 w-4" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-12" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-24" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-20" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-16" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-16" /></TableHead>
                  <TableHead><Skeleton className="h-4 w-12" /></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {Array.from({ length: 8 }).map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><Skeleton className="h-4 w-4" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-20" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-14 rounded-full" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : transactions.length === 0 ? (
          <div className="text-center py-12">
            {search || categoryFilter || startDate || endDate ? (
              <>
                <p className="text-lg text-muted-foreground">No matching transactions</p>
                <p className="text-sm text-muted-foreground mt-1">
                  Try adjusting your filters.
                </p>
              </>
            ) : (
              <>
                <p className="text-lg text-muted-foreground">No transactions yet</p>
                <p className="text-sm text-muted-foreground mt-1">
                  Link a bank account to get started.
                </p>
                <Link to="/accounts">
                  <Button className="mt-4">Link Account</Button>
                </Link>
              </>
            )}
          </div>
        ) : (
          <>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <Checkbox
                        checked={selectedIds.size === transactions.length && transactions.length > 0}
                        onCheckedChange={toggleSelectAll}
                      />
                    </TableHead>
                    <TableHead
                      className="cursor-pointer select-none"
                      onClick={() => handleSort('date')}
                    >
                      Date{sortIndicator('date')}
                    </TableHead>
                    <TableHead>Description</TableHead>
                    <TableHead>Merchant</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead
                      className="cursor-pointer select-none text-right"
                      onClick={() => handleSort('amountCents')}
                    >
                      Amount{sortIndicator('amountCents')}
                    </TableHead>
                    <TableHead>Type</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {transactions.map(txn => (
                    <TableRow key={txn.id} className={selectedIds.has(txn.id) ? 'bg-muted/50' : ''}>
                      <TableCell>
                        <Checkbox
                          checked={selectedIds.has(txn.id)}
                          onCheckedChange={() => toggleSelect(txn.id)}
                        />
                      </TableCell>
                      <TableCell className="whitespace-nowrap">{formatDate(txn.date)}</TableCell>
                      <TableCell className="max-w-48 truncate">{txn.description}</TableCell>
                      <TableCell className="max-w-32 truncate">{txn.merchantName || '—'}</TableCell>
                      <TableCell>
                        {editingId === txn.id ? (
                          <Select
                            value={txn.categoryId != null ? String(txn.categoryId) : '__none__'}
                            onValueChange={v => v && handleCategoryChange(txn.id, v)}
                          >
                            <SelectTrigger className="w-36 h-8">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="__none__">Uncategorized</SelectItem>
                              {categories.map(c => (
                                <SelectItem key={c.id} value={String(c.id)}>
                                  {c.icon ? `${c.icon} ` : ''}{c.name}
                                </SelectItem>
                              ))}
                              <SelectItem value="__new__">+ New Category...</SelectItem>
                            </SelectContent>
                          </Select>
                        ) : (
                          <Badge
                            variant={txn.categoryId ? 'secondary' : 'outline'}
                            className="cursor-pointer"
                            onClick={() => setEditingId(txn.id)}
                          >
                            {txn.categoryName}
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right whitespace-nowrap font-mono">
                        ${formatCents(txn.amountCents)}
                      </TableCell>
                      <TableCell>
                        <Badge variant={txn.transactionType === 'CREDIT' ? 'default' : 'secondary'}>
                          {txn.transactionType}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {/* Pagination */}
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                {totalElements} transaction{totalElements !== 1 ? 's' : ''}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage(p => p - 1)}
                >
                  Previous
                </Button>
                <span className="flex items-center text-sm text-muted-foreground">
                  Page {page + 1} of {Math.max(totalPages, 1)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                >
                  Next
                </Button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Bulk categorize dialog */}
      <Dialog open={showBulkDialog} onOpenChange={setShowBulkDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Categorize {selectedIds.size} transaction{selectedIds.size !== 1 ? 's' : ''}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-4">
            <Label>Category</Label>
            <Select value={bulkCategoryId} onValueChange={v => setBulkCategoryId(v ?? '')}>
              <SelectTrigger>
                <SelectValue placeholder="Select a category" />
              </SelectTrigger>
              <SelectContent>
                {categories.map(c => (
                  <SelectItem key={c.id} value={String(c.id)}>
                    {c.icon ? `${c.icon} ` : ''}{c.name}
                  </SelectItem>
                ))}
                <SelectItem value="__new__">+ New Category...</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowBulkDialog(false)}>Cancel</Button>
            <Button onClick={handleBulkCategorize} disabled={!bulkCategoryId}>Apply</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* New category dialog */}
      <Dialog open={showNewCategory} onOpenChange={setShowNewCategory}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New Category</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-4">
            <div className="space-y-1">
              <Label htmlFor="new-cat-name">Name</Label>
              <Input
                id="new-cat-name"
                value={newCategoryName}
                onChange={e => setNewCategoryName(e.target.value)}
                placeholder="e.g., Eating Out"
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="new-cat-icon">Icon (optional)</Label>
              <Input
                id="new-cat-icon"
                value={newCategoryIcon}
                onChange={e => setNewCategoryIcon(e.target.value)}
                placeholder="e.g., 🍕"
              />
            </div>
            <ColorPicker value={newCategoryColor} onChange={setNewCategoryColor} />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowNewCategory(false)}>Cancel</Button>
            <Button onClick={handleCreateCategory} disabled={!newCategoryName.trim()}>Create</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
