import { useState } from 'react';
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
  const [showHexInput, setShowHexInput] = useState(false);

  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <div className="flex flex-wrap gap-2">
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
        <button
          type="button"
          className={`w-7 h-7 rounded-full border-2 border-dashed flex items-center justify-center text-xs text-muted-foreground ${
            showHexInput ? 'border-foreground' : 'border-muted-foreground hover:border-foreground'
          }`}
          onClick={() => setShowHexInput(!showHexInput)}
          title="Enter custom hex code"
        >
          #
        </button>
      </div>
      {showHexInput && (
        <input
          type="color"
          value={value || '#000000'}
          onChange={e => onChange(e.target.value)}
          className="h-8 w-16 cursor-pointer rounded border border-input bg-transparent p-0.5"
        />
      )}
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
