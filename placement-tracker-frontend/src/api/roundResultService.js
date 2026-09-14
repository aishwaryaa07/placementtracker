import api from './api';

// Backend: AdminRoundResultController
// RoundResultUpdateRequest { status (PENDING|PASSED|FAILED)*, remarks }

export function updateRoundResult(id, request) {
  return api.patch(`/api/admin/round-results/${id}`, request).then((res) => res.data);
}
