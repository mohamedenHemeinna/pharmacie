import i18n from '../i18n';

/**
 * Status values are stored in the database under their English names (they are constrained by
 * CHECK clauses and compared in code). This module only translates them for display.
 * Unknown values are shown as stored.
 */
const STATUS_KEYS = {
  neededMed: {
    'Distributor Ordered': 'status.neededMed.distributorOrdered',
    Processing: 'status.neededMed.processing',
    Pending: 'status.neededMed.pending',
    Delivered: 'status.neededMed.delivered',
    Cancelled: 'status.neededMed.cancelled',
  },
  campaign: {
    '7-Day Alert Active': 'status.campaign.alertActive',
    Upcoming: 'status.campaign.upcoming',
    Planned: 'status.campaign.planned',
    Completed: 'status.campaign.completed',
  },
  task: {
    Pending: 'status.task.pending',
    'In Progress': 'status.task.inProgress',
    Planned: 'status.task.planned',
    Completed: 'status.task.completed',
  },
} as const;

export type StatusKind = keyof typeof STATUS_KEYS;

export function statusLabel(kind: StatusKind, value: string): string {
  const table: Record<string, string> = STATUS_KEYS[kind];
  const key = table[value];
  return key ? i18n.t(key) : value;
}
