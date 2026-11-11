import { MAX_IMAGE_BYTES, moveImage, validateImageFile } from './images';

const file = (over: Partial<{ type: string; size: number; name: string }> = {}) => ({ type: 'image/jpeg', size: 1000, name: 'a.jpg', ...over });

describe('validateImageFile', () => {
  it('menerima jpg, png, webp sampai 5 MB', () => {
    expect(validateImageFile(file(), 0)).toBeNull();
    expect(validateImageFile(file({ type: 'image/png' }), 5)).toBeNull();
    expect(validateImageFile(file({ type: 'image/webp', size: MAX_IMAGE_BYTES }), 0)).toBeNull();
  });
  it('menolak format lain, terlalu besar, kosong, dan melebihi 6 foto', () => {
    expect(validateImageFile(file({ type: 'image/gif' }), 0)).toMatch(/jpg, png, atau webp/);
    expect(validateImageFile(file({ size: MAX_IMAGE_BYTES + 1 }), 0)).toMatch(/melebihi batas 5 MB/);
    expect(validateImageFile(file({ size: 0 }), 0)).toMatch(/kosong/);
    expect(validateImageFile(file(), 6)).toMatch(/Maksimal 6/);
  });
});

describe('moveImage', () => {
  it('menggeser satu langkah', () => {
    expect(moveImage([1, 2, 3], 2, -1)).toEqual([2, 1, 3]);
    expect(moveImage([1, 2, 3], 2, 1)).toEqual([1, 3, 2]);
  });
  it('null di tepi atau id tidak dikenal', () => {
    expect(moveImage([1, 2, 3], 1, -1)).toBeNull();
    expect(moveImage([1, 2, 3], 3, 1)).toBeNull();
    expect(moveImage([1, 2, 3], 9, 1)).toBeNull();
  });
});
