import { useEffect, useRef } from 'react';
import { Label } from '@/components/ui/label';

const PRESET_COLORS = [
  '#4CAF50', '#8BC34A', '#CDDC39', '#FFC107',
  '#FF9800', '#FF5722', '#F44336', '#E91E63',
  '#9C27B0', '#673AB7', '#3F51B5', '#2196F3',
  '#03A9F4', '#00BCD4', '#009688', '#607D8B',
  '#795548', '#9E9E9E',
];

interface ColorPickerProps {
  value: string;
  onChange: (color: string) => void;
  label?: string;
}

export default function ColorPicker({ value, onChange, label = 'Color (optional)' }: ColorPickerProps) {
  const colorInputRef = useRef<HTMLInputElement>(null);

  // React's onChange maps to the DOM `input` event (fires on every drag).
  // The DOM `change` event fires only when the picker is committed/closed.
  useEffect(() => {
    const input = colorInputRef.current;
    if (!input) return;
    const handleChange = (e: Event) => onChange((e.target as HTMLInputElement).value);
    input.addEventListener('change', handleChange);
    return () => input.removeEventListener('change', handleChange);
  }, [onChange]);

  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <div className="flex flex-wrap gap-2 items-center">
        {PRESET_COLORS.map(color => (
          <button
            key={color}
            type="button"
            className={`w-7 h-7 rounded-full border-2 transition-transform ${
              value === color ? 'border-foreground scale-110' : 'border-transparent hover:scale-105'
            }`}
            style={{ backgroundColor: color }}
            onClick={() => onChange(color)}
            title={color}
          />
        ))}
        {/* Transparent color input sits on top of the # button so the OS picker
            opens anchored to the right spot without a programmatic .click() */}
        <div className="relative w-7 h-7">
          <div className="absolute inset-0 rounded-full border-2 border-dashed flex items-center justify-center text-xs text-muted-foreground border-muted-foreground pointer-events-none">
            #
          </div>
          <input
            ref={colorInputRef}
            type="color"
            defaultValue={value || '#000000'}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
            tabIndex={-1}
            title="Open color picker"
          />
        </div>
      </div>
      {value && (
        <button
          type="button"
          className="ml-1 text-xs text-muted-foreground hover:text-foreground"
          onClick={() => onChange('')}
        >
          Clear selection
        </button>
      )}
    </div>
  );
}
