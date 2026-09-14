// Backend error shapes (GlobalExceptionHandler):
// - business/not-found/auth errors: { message: "..." }
// - bean validation errors (400): { fieldName: "validation message", ... }
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data;
  if (!data) return error?.message || fallback;
  if (typeof data.message === 'string') return data.message;
  if (typeof data === 'object') {
    const fieldMessages = Object.values(data).filter((v) => typeof v === 'string');
    if (fieldMessages.length > 0) return fieldMessages.join(' ');
  }
  return fallback;
}

// Bean validation errors come back as a flat { fieldName: "message" } map (no "message" key).
// Business errors come back as { message: "..." }. This distinguishes the two so a form
// can show inline field errors instead of (or in addition to) a general banner.
export function getFieldErrors(error) {
  const data = error?.response?.data;
  if (!data || typeof data !== 'object' || typeof data.message === 'string') return {};
  const fields = {};
  Object.entries(data).forEach(([key, value]) => {
    if (typeof value === 'string') fields[key] = value;
  });
  return fields;
}
