import { useSyncExternalStore } from 'react';

const query = '(pointer: coarse)';

/** True di perangkat sentuh: di sana dipakai kontrol native (pemilih bawaan ponsel lebih nyaman dan sudah aksesibel). */
export function useCoarsePointer(): boolean {
  return useSyncExternalStore(
    (notify) => {
      const mq = window.matchMedia(query);
      mq.addEventListener('change', notify);
      return () => mq.removeEventListener('change', notify);
    },
    () => window.matchMedia(query).matches,
    () => false,
  );
}
