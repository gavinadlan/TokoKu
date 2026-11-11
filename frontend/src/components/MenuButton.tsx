import * as Menu from '@radix-ui/react-dropdown-menu';
import type { ReactNode } from 'react';

export interface MenuEntry {
  key: string;
  label: ReactNode;
  onSelect?: () => void;
  /** Dirender sebagai elemen ini (mis. <Link>) alih-alih tombol. */
  asChild?: ReactNode;
  danger?: boolean;
  disabled?: boolean;
  separatorBefore?: boolean;
}

/** Dropdown (Radix): Enter/Spasi membuka, panah memilih, Escape menutup dan mengembalikan fokus ke pemicu. */
export function MenuButton({ trigger, entries, triggerClassName, ariaLabel, align = 'end', header }: {
  trigger: ReactNode;
  entries: MenuEntry[];
  triggerClassName?: string;
  ariaLabel?: string;
  align?: 'start' | 'end';
  header?: ReactNode;
}) {
  return (
    <Menu.Root>
      <Menu.Trigger className={triggerClassName ?? 'menu-trigger'} aria-label={ariaLabel}>
        {trigger}
      </Menu.Trigger>
      <Menu.Portal>
        <Menu.Content className="pop menu-content" align={align} sideOffset={6} collisionPadding={8}>
          {header && <div className="menu-header">{header}</div>}
          {entries.map((e) => (
            <div key={e.key} className="contents">
              {e.separatorBefore && <Menu.Separator className="pop-sep" />}
              {e.asChild ? (
                <Menu.Item asChild className="pop-item">
                  {e.asChild}
                </Menu.Item>
              ) : (
                <Menu.Item className={`pop-item ${e.danger ? 'danger' : ''}`} disabled={e.disabled} onSelect={e.onSelect}>
                  {e.label}
                </Menu.Item>
              )}
            </div>
          ))}
        </Menu.Content>
      </Menu.Portal>
    </Menu.Root>
  );
}
