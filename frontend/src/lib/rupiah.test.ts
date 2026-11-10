import { caretAfterFormat, formatThousands, parseRupiahInput } from './rupiah';

describe('rupiah input', () => {
  it('memformat dengan titik ribuan', () => {
    expect(formatThousands(85000)).toBe('85.000');
    expect(formatThousands(1450000)).toBe('1.450.000');
    expect(formatThousands(0)).toBe('0');
  });
  it('hanya menerima digit', () => {
    expect(parseRupiahInput('Rp 85.000')).toBe(85000);
    expect(parseRupiahInput('1.4a5b0')).toBe(1450);
    expect(parseRupiahInput('')).toBeUndefined();
    expect(parseRupiahInput('abc')).toBeUndefined();
  });
  it('dibatasi 10 digit', () => {
    expect(parseRupiahInput('123456789012')).toBe(1234567890);
  });
  it('kursor mengikuti digit setelah diformat ulang', () => {
    // mengetik "5" di akhir "1.234" -> "12.345", kursor di akhir
    expect(caretAfterFormat('1.2345', '12.345')).toBe(6);
    // kursor setelah 2 digit pertama pada "12.345"
    expect(caretAfterFormat('12', '12.345')).toBe(2);
    expect(caretAfterFormat('123', '12.345')).toBe(4);
    expect(caretAfterFormat('', '12.345')).toBe(0);
  });
});
