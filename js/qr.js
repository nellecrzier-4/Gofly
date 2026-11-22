/**
 * Generator QR kecil tanpa dependensi.
 *
 * Cukup untuk tautan berbagi GoFile (maks. ±106 karakter, QR versi 1–6,
 * koreksi galat level L, mode byte UTF-8). Tidak ada koneksi jaringan:
 * seluruh matriks dihitung di browser.
 */

const EXP = new Uint8Array(512);
const LOG = new Uint8Array(256);

(function initGalois() {
  let x = 1;
  for (let i = 0; i < 255; i += 1) {
    EXP[i] = x;
    LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i += 1) EXP[i] = EXP[i - 255];
})();

function gfMul(a, b) {
  if (a === 0 || b === 0) return 0;
  return EXP[LOG[a] + LOG[b]];
}

/** Versi 1–6, level L: [jumlah blok, codeword data per blok]. */
const SPECS = {
  1: { ecc: 7, groups: [[1, 19]] },
  2: { ecc: 10, groups: [[1, 34]] },
  3: { ecc: 15, groups: [[1, 55]] },
  4: { ecc: 20, groups: [[1, 80]] },
  5: { ecc: 26, groups: [[1, 108]] },
  6: { ecc: 18, groups: [[2, 68]] },
};

const ALIGNMENT = {
  1: [],
  2: [6, 18],
  3: [6, 22],
  4: [6, 26],
  5: [6, 30],
  6: [6, 34],
};

/** Format bit untuk level L. */
const ECC_BITS = 1;

function generatorPoly(degree) {
  let poly = new Uint8Array([1]);
  for (let i = 0; i < degree; i += 1) {
    const next = new Uint8Array(poly.length + 1);
    for (let j = 0; j < poly.length; j += 1) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], EXP[i]);
    }
    poly = next;
  }
  return poly;
}

function reedSolomon(data, eccLength) {
  const gen = generatorPoly(eccLength);
  const buffer = new Uint8Array(data.length + eccLength);
  buffer.set(data);
  for (let i = 0; i < data.length; i += 1) {
    const coef = buffer[i];
    if (!coef) continue;
    for (let j = 0; j < gen.length; j += 1) {
      buffer[i + j] ^= gfMul(gen[j], coef);
    }
  }
  return buffer.slice(data.length);
}

function bchTypeInfo(data) {
  let value = data << 10;
  for (let i = 4; i >= 0; i -= 1) {
    if (value & (1 << (i + 10))) value ^= 0x537 << i;
  }
  return ((data << 10) | value) ^ 0x5412;
}

function maskPredicate(mask, row, col) {
  switch (mask) {
    case 0:
      return (row + col) % 2 === 0;
    case 1:
      return row % 2 === 0;
    case 2:
      return col % 3 === 0;
    case 3:
      return (row + col) % 3 === 0;
    case 4:
      return (Math.floor(row / 2) + Math.floor(col / 3)) % 2 === 0;
    case 5:
      return ((row * col) % 2) + ((row * col) % 3) === 0;
    case 6:
      return (((row * col) % 2) + ((row * col) % 3)) % 2 === 0;
    case 7:
      return (((row + col) % 2) + ((row * col) % 3)) % 2 === 0;
    default:
      return false;
  }
}

function penalty(matrix) {
  const size = matrix.length;
  let score = 0;

  // Aturan 1: deretan ≥5 modul sewarna.
  const runScore = (run) => (run >= 5 ? 3 + (run - 5) : 0);
  for (let r = 0; r < size; r += 1) {
    let run = 1;
    for (let c = 1; c < size; c += 1) {
      if (matrix[r][c] === matrix[r][c - 1]) run += 1;
      else {
        score += runScore(run);
        run = 1;
      }
    }
    score += runScore(run);
  }
  for (let c = 0; c < size; c += 1) {
    let run = 1;
    for (let r = 1; r < size; r += 1) {
      if (matrix[r][c] === matrix[r - 1][c]) run += 1;
      else {
        score += runScore(run);
        run = 1;
      }
    }
    score += runScore(run);
  }

  // Aturan 2: blok 2×2 sewarna.
  for (let r = 0; r < size - 1; r += 1) {
    for (let c = 0; c < size - 1; c += 1) {
      const v = matrix[r][c];
      if (v === matrix[r][c + 1] && v === matrix[r + 1][c] && v === matrix[r + 1][c + 1]) {
        score += 3;
      }
    }
  }

  // Aturan 3: pola finder palsu 1011101 dengan 4 modul terang di sisinya.
  const patterns = [
    [1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0],
    [0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1],
  ];
  const matches = (get, start) => {
    for (const pattern of patterns) {
      let ok = true;
      for (let k = 0; k < 11; k += 1) {
        if (get(start + k) !== !!pattern[k]) {
          ok = false;
          break;
        }
      }
      if (ok) score += 40;
    }
  };
  for (let r = 0; r < size; r += 1) {
    for (let c = 0; c <= size - 11; c += 1) matches((i) => matrix[r][i], c);
  }
  for (let c = 0; c < size; c += 1) {
    for (let r = 0; r <= size - 11; r += 1) matches((i) => matrix[i][c], r);
  }

  // Aturan 4: keseimbangan modul gelap/terang.
  let dark = 0;
  for (let r = 0; r < size; r += 1) {
    for (let c = 0; c < size; c += 1) if (matrix[r][c]) dark += 1;
  }
  const percent = (dark * 100) / (size * size);
  score += Math.floor(Math.abs(percent - 50) / 5) * 10;

  return score;
}

function buildCodewords(bytes, version) {
  const spec = SPECS[version];
  const capacityBits = spec.groups.reduce((sum, [count, length]) => sum + count * length, 0) * 8;
  const bits = [];
  const push = (value, length) => {
    for (let i = length - 1; i >= 0; i -= 1) bits.push((value >> i) & 1);
  };

  push(0b0100, 4); // mode byte
  push(bytes.length, 8);
  for (const byte of bytes) push(byte, 8);

  const terminator = Math.min(4, capacityBits - bits.length);
  for (let i = 0; i < terminator; i += 1) bits.push(0);
  while (bits.length % 8 !== 0) bits.push(0);
  const padBytes = [0xec, 0x11];
  let padIndex = 0;
  while (bits.length < capacityBits) {
    push(padBytes[padIndex % 2], 8);
    padIndex += 1;
  }

  const data = new Uint8Array(bits.length / 8);
  for (let i = 0; i < data.length; i += 1) {
    let value = 0;
    for (let j = 0; j < 8; j += 1) value = (value << 1) | bits[i * 8 + j];
    data[i] = value;
  }

  const blocks = [];
  let offset = 0;
  for (const [count, length] of spec.groups) {
    for (let i = 0; i < count; i += 1) {
      const chunk = data.slice(offset, offset + length);
      offset += length;
      blocks.push({ data: chunk, ecc: reedSolomon(chunk, spec.ecc) });
    }
  }

  const result = [];
  const maxData = Math.max(...blocks.map((block) => block.data.length));
  for (let i = 0; i < maxData; i += 1) {
    for (const block of blocks) if (i < block.data.length) result.push(block.data[i]);
  }
  for (let i = 0; i < spec.ecc; i += 1) {
    for (const block of blocks) result.push(block.ecc[i]);
  }
  return new Uint8Array(result);
}

function emptyMatrix(size) {
  return Array.from({ length: size }, () => new Array(size).fill(false));
}

function buildModules(version, codewords, mask) {
  const size = version * 4 + 17;
  const modules = emptyMatrix(size);
  const isFunction = Array.from({ length: size }, () => new Array(size).fill(false));

  const setFunction = (row, col, dark) => {
    if (row < 0 || col < 0 || row >= size || col >= size) return;
    modules[row][col] = dark;
    isFunction[row][col] = true;
  };

  const placeFinder = (row, col) => {
    for (let r = -1; r <= 7; r += 1) {
      for (let c = -1; c <= 7; c += 1) {
        const rr = row + r;
        const cc = col + c;
        if (rr < 0 || cc < 0 || rr >= size || cc >= size) continue;
        const dark =
          (r >= 0 && r <= 6 && (c === 0 || c === 6)) ||
          (c >= 0 && c <= 6 && (r === 0 || r === 6)) ||
          (r >= 2 && r <= 4 && c >= 2 && c <= 4);
        setFunction(rr, cc, dark);
      }
    }
  };

  placeFinder(0, 0);
  placeFinder(0, size - 7);
  placeFinder(size - 7, 0);

  // Timing pattern.
  for (let i = 8; i < size - 8; i += 1) {
    setFunction(6, i, i % 2 === 0);
    setFunction(i, 6, i % 2 === 0);
  }

  // Alignment pattern.
  const positions = ALIGNMENT[version] || [];
  for (const row of positions) {
    for (const col of positions) {
      const nearFinder =
        (row === 6 && col === 6) ||
        (row === 6 && col === size - 7) ||
        (row === size - 7 && col === 6);
      if (nearFinder) continue;
      for (let r = -2; r <= 2; r += 1) {
        for (let c = -2; c <= 2; c += 1) {
          const dark = Math.max(Math.abs(r), Math.abs(c)) !== 1;
          setFunction(row + r, col + c, dark);
        }
      }
    }
  }

  // Cadangkan area format + modul gelap tetap.
  for (let i = 0; i < 9; i += 1) {
    if (!isFunction[8][i]) setFunction(8, i, false);
    if (!isFunction[i][8]) setFunction(i, 8, false);
  }
  for (let i = 0; i < 8; i += 1) {
    setFunction(8, size - 1 - i, false);
    setFunction(size - 1 - i, 8, false);
  }
  setFunction(size - 8, 8, true);

  // Tempatkan bit data (zig-zag dari kanan bawah).
  let bitIndex = 0;
  let row = size - 1;
  let direction = -1;
  for (let col = size - 1; col > 0; col -= 2) {
    if (col === 6) col -= 1;
    for (;;) {
      for (let offset = 0; offset < 2; offset += 1) {
        const cc = col - offset;
        if (isFunction[row][cc]) continue;
        let dark = false;
        if (bitIndex < codewords.length * 8) {
          dark = ((codewords[bitIndex >> 3] >> (7 - (bitIndex & 7))) & 1) === 1;
          bitIndex += 1;
        }
        modules[row][cc] = dark;
      }
      row += direction;
      if (row < 0 || row >= size) {
        row -= direction;
        direction = -direction;
        break;
      }
    }
  }

  // Terapkan mask ke seluruh modul data.
  for (let r = 0; r < size; r += 1) {
    for (let c = 0; c < size; c += 1) {
      if (!isFunction[r][c] && maskPredicate(mask, r, c)) modules[r][c] = !modules[r][c];
    }
  }

  // Tulis info format.
  const bits = bchTypeInfo((ECC_BITS << 3) | mask);
  for (let i = 0; i < 15; i += 1) {
    const dark = ((bits >> i) & 1) === 1;
    if (i < 6) modules[i][8] = dark;
    else if (i < 8) modules[i + 1][8] = dark;
    else modules[size - 15 + i][8] = dark;

    if (i < 8) modules[8][size - i - 1] = dark;
    else if (i < 9) modules[8][15 - i - 1 + 1] = dark;
    else modules[8][15 - i - 1] = dark;
  }
  modules[size - 8][8] = true;

  return modules;
}

function chooseVersion(byteLength) {
  for (let version = 1; version <= 6; version += 1) {
    const spec = SPECS[version];
    const capacity = spec.groups.reduce((sum, [count, length]) => sum + count * length, 0) * 8;
    if (4 + 8 + byteLength * 8 <= capacity) return version;
  }
  return 0;
}

/**
 * Matriks QR (tanpa quiet zone). Opsi `mask` memaksa pola mask tertentu —
 * dipakai oleh test; produksi memilih mask dengan penalti terendah.
 */
export function qrMatrix(text, { mask = null } = {}) {
  const bytes = new TextEncoder().encode(String(text ?? ""));
  const version = chooseVersion(bytes.length);
  if (!version) {
    throw new Error("Teks terlalu panjang untuk QR (maks. ±106 byte).");
  }
  const codewords = buildCodewords(bytes, version);

  if (Number.isInteger(mask) && mask >= 0 && mask <= 7) {
    return { size: version * 4 + 17, modules: buildModules(version, codewords, mask), version, mask };
  }

  let best = null;
  for (let candidate = 0; candidate < 8; candidate += 1) {
    const modules = buildModules(version, codewords, candidate);
    const score = penalty(modules);
    if (!best || score < best.score) best = { modules, mask: candidate, score };
  }
  return { size: version * 4 + 17, modules: best.modules, version, mask: best.mask };
}

/** QR sebagai string SVG siap-tempel (latar putih, quiet zone 4 modul). */
export function qrSvg(text, { size = 220, margin = 4 } = {}) {
  const { size: modules, modules: matrix } = qrMatrix(text);
  const total = modules + margin * 2;
  const segments = [];
  for (let row = 0; row < modules; row += 1) {
    let col = 0;
    while (col < modules) {
      if (!matrix[row][col]) {
        col += 1;
        continue;
      }
      let run = 1;
      while (col + run < modules && matrix[row][col + run]) run += 1;
      segments.push(`M${col + margin} ${row + margin}h${run}v1h-${run}z`);
      col += run;
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${total} ${total}" role="img" aria-label="Kode QR tautan berbagi" shape-rendering="crispEdges"><rect width="${total}" height="${total}" fill="#ffffff"/><path d="${segments.join("")}" fill="#000000"/></svg>`;
}
