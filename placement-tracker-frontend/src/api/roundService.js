import api from './api';

// Backend: AdminRoundController
// RoundRequest { sequence*, name*, roundDate }
// RoundResponse { id, sequence, name, roundDate }
// Round creation/update/delete is ADMIN-only (SecurityConfig: /api/admin/** -> ROLE_ADMIN).
// There is no "type" or per-round "status" field on the backend Round entity - only
// sequence, name, and roundDate exist, so that's all this UI can manage. Per-application
// round outcome (PENDING/PASSED/FAILED) is a separate concept, tracked on RoundResult.

export function getRounds(driveId) {
  return api.get(`/api/admin/drives/${driveId}/rounds`).then((res) => res.data);
}

export function createRound(driveId, request) {
  return api.post(`/api/admin/drives/${driveId}/rounds`, request).then((res) => res.data);
}

export function updateRound(roundId, request) {
  return api.put(`/api/admin/rounds/${roundId}`, request).then((res) => res.data);
}

export function deleteRound(roundId) {
  return api.delete(`/api/admin/rounds/${roundId}`);
}

// Ranking-mode rounds (TOP_N / THRESHOLD_THEN_TOP_N) hold scored results as SCORED until
// finalized - this ranks everyone and resolves PASSED/FAILED for the whole round at once.
// One-shot: the backend rejects a second call once finalizedAt is set.
export function finalizeRound(roundId) {
  return api.post(`/api/admin/rounds/${roundId}/finalize`).then((res) => res.data);
}
