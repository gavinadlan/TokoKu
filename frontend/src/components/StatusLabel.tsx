import { STATUS_LABEL, STATUS_TONE, type OrderStatus } from '../lib/status';

export function StatusLabel({ status }: { status: OrderStatus }) {
  return <span className={`status ${STATUS_TONE[status]}`}>{STATUS_LABEL[status]}</span>;
}
