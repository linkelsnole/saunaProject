export interface ClimateSensorDto {
  id: string;
  temperature: number | null;
  humidity: number | null;
  label: string;
}

export interface SensorData {
  id: string;
  name: string;
  value: string | null;
  unit: string;
}

export interface FanFlowData {
  type: string;
  speedPerc: number | null;
  vm: number | null;
  pressure: number | null;
  targetAirFlow: number | null;
}

export interface RecuperatorStatusDto {
  enabled: boolean;
  stateCode: number | null;
  stateName: string;
}

export interface ManualSettingsDto {
  supplySpeed: number | null;
  boostPercent: number | null;
}

export interface TempSettingsDto {
  targetZone1: number | null;
  targetZone2: number | null;
  waterWarnThreshold: number | null;
  waterFailThreshold: number | null;
  inflowWarnThreshold: number | null;
  inflowFailThreshold: number | null;
  ctrlModeZone1: number | null;
  ctrlModeZone2: number | null;
}

export interface DashboardData {
  climateSensors: ClimateSensorDto[];
  wSensors: SensorData[];
  pressureSensors: SensorData[];
  fans: FanFlowData[];
  valves: SensorData[];
  recuperator: SensorData[];
  recuperatorStatus: RecuperatorStatusDto;
  vm1: number | null;
  vm2: number | null;
  recupSpeed: number | null;
  recupCurrent: number | null;
  vManualProcIn: number | null;
  vManualDProcOut: number | null;
  manualSettings: ManualSettingsDto;
  tempSettings: TempSettingsDto;
  damperSupply: boolean | null;   // задвижка притока
  damperExhaust: boolean | null;   // задвижка вытяжки
}

