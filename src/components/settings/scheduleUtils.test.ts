import { describe, expect, it } from 'vitest';

import type { ScheduleData } from '@/api/typesApi';
import {
  buildScheduleGroups,
  changeScheduleTemperatureMode,
  changeScheduleTemperatureTarget,
  encodeScheduleDate,
  encodeScheduleTime,
  formatScheduleTime,
  normalizeScheduleTemperatureSelection,
  parseScheduleDate,
  parseWeekDays,
} from './scheduleUtils';

const schedule = (scheduleId: number, mode: ScheduleData['mode'], startTime: number): ScheduleData => ({
  scheduleId,
  mode,
  status: 'INACTIVE',
  statusRawCode: 0,
  weekDayMask: 0,
  group: 0,
  dateRange: { startDate: 0, endDate: 0 },
  timeRange: { startTime, endTime: 0 },
  duration: 0,
  airControl: { mode: 'ECO', targetAirflow: null },
  temperatureModeZone1: 'AUTO',
  temperatureZone1: { controlType: 'ORP', targetTemperature: null },
  temperatureModeZone2: 'AUTO',
  temperatureZone2: { controlType: 'ORP', targetTemperature: null },
});

describe('scheduleUtils', () => {
  it('encodes and parses the PLC month/day date format', () => {
    const date = new Date(2026, 2, 31);
    expect(encodeScheduleDate(date)).toBe(0x031f);
    expect(parseScheduleDate(0x031f, 2026)).toEqual(date);
    expect(parseScheduleDate(0x0230, 2026)).toBeNull();
  });

  it('encodes and formats PLC time values', () => {
    expect(encodeScheduleTime('21:18')).toBe(0x1512);
    expect(formatScheduleTime(0x1512)).toBe('21:18');
  });

  it('decodes the Monday-first weekday mask', () => {
    expect(parseWeekDays(0b1000101)).toEqual(['mon', 'wed', 'sun']);
  });

  it('keeps the 301 groups, sorts intervals, and excludes UserSet', () => {
    const groups = buildScheduleGroups([
      schedule(5, 'DATES', 0x1200),
      schedule(4, 'DATES', 0x0800),
      schedule(6, 'USERSET', 0x0600),
      schedule(7, 'DATES', 0x0900),
    ]);

    expect(groups).toHaveLength(2);
    expect(groups[0].schedules.map((item) => item.scheduleId)).toEqual([4, 5]);
    expect(groups[1].schedules.map((item) => item.scheduleId)).toEqual([7]);
  });

  it('normalizes an inconsistent OV value from the PLC before editing', () => {
    expect(normalizeScheduleTemperatureSelection({
      zone1Mode: 'PV',
      zone1Target: 19,
      zone2Mode: 'OV',
      zone2Target: 21,
    })).toEqual({
      zone1Mode: 'OV',
      zone1Target: 21,
      zone2Mode: 'OV',
      zone2Target: 21,
    });
  });

  it('selects OV and its target for both zones at once', () => {
    expect(changeScheduleTemperatureMode({
      zone1Mode: 'PV',
      zone1Target: 19,
      zone2Mode: 'ORP',
      zone2Target: 22,
    }, 1, 'OV')).toEqual({
      zone1Mode: 'OV',
      zone1Target: 19,
      zone2Mode: 'OV',
      zone2Target: 19,
    });
  });

  it('leaves OV for both zones when either zone changes mode', () => {
    expect(changeScheduleTemperatureMode({
      zone1Mode: 'OV',
      zone1Target: 20,
      zone2Mode: 'OV',
      zone2Target: 20,
    }, 2, 'ORP')).toEqual({
      zone1Mode: 'ORP',
      zone1Target: 20,
      zone2Mode: 'ORP',
      zone2Target: 20,
    });
  });

  it('keeps OV targets equal while preserving independent PV targets', () => {
    const ov = changeScheduleTemperatureTarget({
      zone1Mode: 'OV',
      zone1Target: 20,
      zone2Mode: 'OV',
      zone2Target: 20,
    }, 2, 22.5);
    expect(ov.zone1Target).toBe(22.5);
    expect(ov.zone2Target).toBe(22.5);

    const pv = changeScheduleTemperatureTarget({
      zone1Mode: 'PV',
      zone1Target: 19,
      zone2Mode: 'PV',
      zone2Target: 21,
    }, 1, 18.5);
    expect(pv.zone1Target).toBe(18.5);
    expect(pv.zone2Target).toBe(21);
  });
});
