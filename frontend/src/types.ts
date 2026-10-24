export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  role: 'CHIEF_OPERATOR' | 'QC_INSPECTOR' | 'MILL_MANAGER' | 'WAREHOUSE_KEEPER';
  plant: string;
  avatar?: string;
  createdAt: string;
}

export interface JumboRoll {
  id: string;
  serialNumber: string;
  epcRfid: string;
  grade: string;
  targetGsm: number;
  deckleWidthMm: number;
  grossWeightKg: number;
  coreTareKg: number;
  netWeightKg: number;
  machineId: string;
  status: 'ACTIVE_WIP' | 'FULLY_SLIT';
  operatorName: string;
  shift: string;
  createdAt: string;
}

export interface SlitCut {
  id: string;
  sequence: number;
  widthMm: number;
  netWeightKg: number;
  serialNumber: string;
  coreMm: number;
  targetDiameterMm: number;
  customer?: string;
}

export interface ChildSlitRoll {
  id: string;
  parentJumboSerial: string;
  serialNumber: string;
  sequence: number;
  widthMm: number;
  netWeightKg: number;
  grossWeightKg: number;
  grade: string;
  gsm: number;
  coreMm: number;
  diameterMm: number;
  status: 'PRODUCED' | 'QC_PASSED' | 'DISPATCHED' | 'REJECTED';
  location: string;
  operatorName: string;
  createdAt: string;
}

export interface QCTest {
  id: string;
  rollSerial: string;
  grade: string;
  actualGsm: number;
  targetGsm: number;
  moisturePct: number;
  caliperMicrons: number;
  burstKpa: number;
  cobb60: number;
  disposition: 'RELEASED' | 'QUARANTINE' | 'REJECTED' | 'DOWNGRADED';
  inspector: string;
  labNotes: string;
  inspectedAt: string;
}

export interface StockMovement {
  id: string;
  timestamp: string;
  rollSerial: string;
  rollCategory: 'جامبو' | 'فرعي';
  movementType: 'PRODUCTION_RECEIPT' | 'STATUS_CHANGE' | 'LOCATION_TRANSFER' | 'DISPATCH' | 'SCRAP';
  netWeightKg: number;
  fromLocation: string;
  toLocation: string;
  actor: string;
  reason: string;
  auditHash: string;
}

export interface MillTelemetry {
  lineSpeedMpm: number;
  webTensionBar: number;
  popeReelSpeedMpm: number;
  dailyGrossTonnage: number;
  targetDailyTonnage: number;
  activeJumboCount: number;
  trimWasteRatioPct: number;
  oeeUtilizationPct: number;
}
