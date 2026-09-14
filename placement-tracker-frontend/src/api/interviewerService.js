import api from './api';

// Backend: InterviewerAssignmentController, InterviewerRoundResultController
// GET /api/interviewer/assignments -> PanelAssignmentResponse[] (only this interviewer's own)
// PATCH /api/interviewer/round-results/{id}/score -> RoundResultResponse
// Server-side enforced: an interviewer can only score a round result they have a
// PanelAssignment for (404 otherwise) - selection is never a free-text id, only rows
// already in their own assignment list.

export function getMyAssignments() {
  return api.get('/api/interviewer/assignments').then((res) => res.data);
}

export function submitInterviewerScore(roundResultId, score, remarks) {
  return api.patch(`/api/interviewer/round-results/${roundResultId}/score`, { score, remarks }).then((res) => res.data);
}
