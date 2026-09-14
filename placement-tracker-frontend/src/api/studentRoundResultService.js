import api from './api';

// Backend: StudentRoundResultController - PATCH /api/student/round-results/{id}/score
// RoundScoreSubmitRequest { score }. Only works for a round with Round.minScore set (see
// RoundResultService.submitScore) - the backend compares score against minScore and sets
// PASSED/FAILED itself; this is not a free-form status update.

export function submitRoundScore(id, score) {
  return api.patch(`/api/student/round-results/${id}/score`, { score }).then((res) => res.data);
}
