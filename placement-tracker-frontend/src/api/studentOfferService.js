import api from './api';

// Backend: StudentOfferController - PATCH /api/student/offers/{id}/status
// (there is no separate /accept or /decline endpoint; OfferService.respondToOffer
// restricts a student to only ACCEPTED or DECLINED, and only while the offer is PENDING)
// OfferStatusUpdateRequest { status }

export function respondToOffer(id, status) {
  return api.patch(`/api/student/offers/${id}/status`, { status }).then((res) => res.data);
}
