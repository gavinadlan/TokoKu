import { STATUS_LABEL, STATUS_TONE, type OrderStatus } from '../lib/status';
import { Badge } from './Badge';

export function StatusLabel({ status }: { status: OrderStatus }) {
  return <Badge tone={STATUS_TONE[status]}>{STATUS_LABEL[status]}</Badge>;
}
