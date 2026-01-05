import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateTempSettings } from '@/api/hooks';
import { Input } from '@/shared/ui/input';
import { Badge } from '@/shared/ui/badge';

interface TempInputProps {
  label: string;
  value: number | null | undefined;
  onChange: (val: number) => void;
  unit?: string;
  decimals?: number;
}

const TempInput = ({ label, value, onChange, unit = '°C', decimals = 1 }: TempInputProps) => {
  const [localValue, setLocalValue] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (!isEditing && value !== null && value !== undefined) {
      setLocalValue(value.toFixed(decimals));
    }
  }, [value, isEditing, decimals]);

  const handleBlur = () => {
    setIsEditing(false);
    const numVal = parseFloat(localValue.replace(',', '.'));

    if (!isNaN(numVal) && Math.abs(numVal - (value || 0)) > 0.01) {
      onChange(numVal);
    } else {
      if (value !== null && value !== undefined) {
        setLocalValue(value.toFixed(decimals));
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') e.currentTarget.blur();
  };

  return (
    <div className="flex flex-col gap-1">
      <span className="text-[11px] text-slate-500 font-medium uppercase tracking-wide px-1">
        {label}
      </span>
      <div className="relative">
        <Input
          type="number"
          step={decimals === 0 ? '1' : '0.1'}
          value={localValue}
          onChange={(e) => {
            setIsEditing(true);
            setLocalValue(e.target.value);
          }}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          className="pr-10 font-mono text-lg font-bold border-slate-200 hover:border-slate-300 focus:border-blue-500 h-10 text-slate-800"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-medium">
          {unit}
        </span>
      </div>
    </div>
  );
};

const getModeLabel = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'N/A';
  switch (mode) {
    case 0: return 'Нейтральная';
    case 1: return 'Нагрев';
    case 2: return 'Охлаждение';
    default: return 'Неизвестно';
  }
};

const getModeColor = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'bg-slate-100 text-slate-500';
  switch (mode) {
    case 0: return 'bg-slate-100 text-slate-600';
    case 1: return 'bg-red-100 text-red-700';
    case 2: return 'bg-blue-100 text-blue-700';
    default: return 'bg-slate-100 text-slate-500';
  }
};

export const TempSettingsWidget = () => {
  const { data } = useDashboardData();
  const { mutate } = useUpdateTempSettings();

  const tempSettings = data?.tempSettings;

  return (
    <div className="space-y-4">
      {/* Целевые температуры зон */}
      <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
        <h4 className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-3">Целевые температуры зон</h4>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2">
            <TempInput
              label="Зона 1"
              value={tempSettings?.targetZone1}
              onChange={(val) => mutate({ targetZone1: val })}
              decimals={0}
            />
            <Badge className={`${getModeColor(tempSettings?.ctrlModeZone1)} w-full justify-center py-1.5 text-xs`}>
              {getModeLabel(tempSettings?.ctrlModeZone1)}
            </Badge>
          </div>
          <div className="space-y-2">
            <TempInput
              label="Зона 2"
              value={tempSettings?.targetZone2}
              onChange={(val) => mutate({ targetZone2: val })}
              decimals={0}
            />
            <Badge className={`${getModeColor(tempSettings?.ctrlModeZone2)} w-full justify-center py-1.5 text-xs`}>
              {getModeLabel(tempSettings?.ctrlModeZone2)}
            </Badge>
          </div>
        </div>
      </div>

      {/* Пороги температуры воды */}
      <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
        <h4 className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-3">Пороги температуры воды</h4>
        <div className="grid grid-cols-2 gap-3">
          <TempInput
            label="Снижение оборотов"
            value={tempSettings?.waterWarnThreshold}
            onChange={(val) => mutate({ waterWarnThreshold: val })}
          />
          <TempInput
            label="Авария"
            value={tempSettings?.waterFailThreshold}
            onChange={(val) => mutate({ waterFailThreshold: val })}
          />
        </div>
      </div>

      {/* Пороги температуры ПВ */}
      <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
        <h4 className="text-xs font-semibold text-slate-600 uppercase tracking-wide mb-3">Пороги температуры ПВ</h4>
        <div className="grid grid-cols-2 gap-3">
          <TempInput
            label="Снижение оборотов"
            value={tempSettings?.inflowWarnThreshold}
            onChange={(val) => mutate({ inflowWarnThreshold: val })}
          />
          <TempInput
            label="Авария"
            value={tempSettings?.inflowFailThreshold}
            onChange={(val) => mutate({ inflowFailThreshold: val })}
          />
        </div>
      </div>
    </div>
  );
};

