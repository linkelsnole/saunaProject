import type { ScheduleData, TemperatureControlType } from '@/api/typesApi';

export type DayKey = 'mon' | 'tue' | 'wed' | 'thu' | 'fri' | 'sat' | 'sun';

export const DAY_KEYS: DayKey[] = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];

export const GROUP_IDS: number[][] = [
  [4, 5, 6],
  [7, 8],
  [9, 10],
  [11, 12, 13],
  [14, 15],
  [16, 17, 18],
  [19, 20],
];

export interface ScheduleTemperatureSelection {
  zone1Mode: TemperatureControlType;
  zone1Target: number;
  zone2Mode: TemperatureControlType;
  zone2Target: number;
}

export type ScheduleZone = 1 | 2;

export const normalizeScheduleTemperatureSelection = (
  selection: ScheduleTemperatureSelection,
): ScheduleTemperatureSelection => {
  if (selection.zone1Mode !== 'OV' && selection.zone2Mode !== 'OV') return selection;
  const sharedTarget = selection.zone1Mode === 'OV'
    ? selection.zone1Target
    : selection.zone2Target;
  return {
    ...selection,
    zone1Mode: 'OV',
    zone2Mode: 'OV',
    zone1Target: sharedTarget,
    zone2Target: sharedTarget,
  };
};

export const changeScheduleTemperatureMode = (
  selection: ScheduleTemperatureSelection,
  zone: ScheduleZone,
  nextMode: TemperatureControlType,
): ScheduleTemperatureSelection => {
  if (nextMode === 'OV') {
    const sharedTarget = zone === 1 ? selection.zone1Target : selection.zone2Target;
    return {
      ...selection,
      zone1Mode: 'OV',
      zone2Mode: 'OV',
      zone1Target: sharedTarget,
      zone2Target: sharedTarget,
    };
  }

  if (selection.zone1Mode === 'OV' || selection.zone2Mode === 'OV') {
    return {
      ...selection,
      zone1Mode: nextMode,
      zone2Mode: nextMode,
    };
  }

  return zone === 1
    ? { ...selection, zone1Mode: nextMode }
    : { ...selection, zone2Mode: nextMode };
};

export const changeScheduleTemperatureTarget = (
  selection: ScheduleTemperatureSelection,
  zone: ScheduleZone,
  nextTarget: number,
): ScheduleTemperatureSelection => {
  if (selection.zone1Mode === 'OV' && selection.zone2Mode === 'OV') {
    return { ...selection, zone1Target: nextTarget, zone2Target: nextTarget };
  }
  return zone === 1
    ? { ...selection, zone1Target: nextTarget }
    : { ...selection, zone2Target: nextTarget };
};

export const parseScheduleDate = (encoded: number, year = new Date().getFullYear()): Date | null => {
  if (!encoded) return null;
  const month = (encoded >> 8) & 0xff;
  const day = encoded & 0xff;
  const result = new Date(year, month - 1, day);
  return month >= 1 && month <= 12 && result.getMonth() === month - 1 && result.getDate() === day
    ? result
    : null;
};

export const encodeScheduleDate = (date: Date | null): number => {
  if (!date) return 0;
  return ((date.getMonth() + 1) << 8) | date.getDate();
};

export const parseWeekDays = (mask: number): DayKey[] =>
  DAY_KEYS.filter((_day, index) => (mask & (1 << index)) !== 0);

export const formatScheduleTime = (encoded: number): string => {
  const hours = (encoded >> 8) & 0xff;
  const minutes = encoded & 0xff;
  return `${String(Math.min(hours, 23)).padStart(2, '0')}:${String(Math.min(minutes, 59)).padStart(2, '0')}`;
};

export const encodeScheduleTime = (value: string): number => {
  const [hoursText = '0', minutesText = '0'] = value.split(':');
  const hours = Math.max(0, Math.min(23, Number(hoursText) || 0));
  const minutes = Math.max(0, Math.min(59, Number(minutesText) || 0));
  return (hours << 8) | minutes;
};

export const buildScheduleGroups = (schedules: ScheduleData[]) =>
  GROUP_IDS.map((ids, index) => ({
    id: index + 1,
    schedules: schedules
      .filter((schedule) => ids.includes(schedule.scheduleId) && schedule.mode !== 'USERSET')
      .sort((left, right) => left.timeRange.startTime - right.timeRange.startTime),
  })).filter((group) => group.schedules.length > 0);
