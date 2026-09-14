import api from './api';

// Backend: AdminOfferController
// OfferRequest { ctcOffered*, offerDate }
// OfferResponse { id, ctcOffered, offerDate, status (PENDING|ACCEPTED|DECLINED) }
// OfferStatusUpdateRequest { status }

export function createOffer(applicationId, request) {
  return api.post(`/api/admin/applications/${applicationId}/offer`, request).then((res) => res.data);
}

export function updateOfferStatus(offerId, status) {
  return api.patch(`/api/admin/offers/${offerId}/status`, { status }).then((res) => res.data);
}
