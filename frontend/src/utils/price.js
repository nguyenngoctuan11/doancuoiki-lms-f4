export function normalizePriceValue(value) {
  if (value === null || value === undefined) return null;
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }
  if (typeof value === "string") {
    const digits = value.replace(/[^\d]/g, "");
    if (!digits) return null;
    const parsed = Number(digits);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

export function resolveIsFree(price, explicitFlag) {
  if (explicitFlag !== undefined && explicitFlag !== null) {
    return Boolean(explicitFlag);
  }
  const normalized = normalizePriceValue(price);
  return normalized === null || normalized <= 0;
}
