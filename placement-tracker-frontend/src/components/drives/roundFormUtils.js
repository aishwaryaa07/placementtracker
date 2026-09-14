export const emptyRoundForm = {
  sequence: '',
  name: '',
  roundDate: '',
  description: '',
  minScore: '',
  selectionMode: 'THRESHOLD',
  topN: '',
};

export function roundToFormValues(round) {
  return {
    sequence: round.sequence ?? '',
    name: round.name || '',
    roundDate: round.roundDate || '',
    description: round.description || '',
    minScore: round.minScore ?? '',
    selectionMode: round.selectionMode || 'THRESHOLD',
    topN: round.topN ?? '',
  };
}

// Duplicate sequence numbers within a drive are also rejected server-side (RoundService) -
// this client-side check just gives faster, friendlier feedback before the round-trip.
export function validateRound(values, existingRounds, editingRoundId) {
  const errors = {};

  if (values.sequence === '') {
    errors.sequence = 'Sequence is required.';
  } else if (!Number.isInteger(Number(values.sequence)) || Number(values.sequence) < 1) {
    errors.sequence = 'Sequence must be a whole number, 1 or higher.';
  } else {
    const duplicate = existingRounds.some(
      (r) => r.sequence === Number(values.sequence) && r.id !== editingRoundId
    );
    if (duplicate) {
      errors.sequence = `Sequence ${values.sequence} is already used by another round in this drive.`;
    }
  }

  if (!values.name.trim()) {
    errors.name = 'Name is required.';
  }

  if (values.minScore !== '' && (Number.isNaN(Number(values.minScore)) || Number(values.minScore) < 0)) {
    errors.minScore = 'Minimum score must be a non-negative number.';
  }

  const isRankingMode = values.selectionMode === 'TOP_N' || values.selectionMode === 'THRESHOLD_THEN_TOP_N';
  if (isRankingMode) {
    if (values.topN === '' || !Number.isInteger(Number(values.topN)) || Number(values.topN) < 1) {
      errors.topN = 'Top N is required and must be a whole number, 1 or higher, for this selection mode.';
    }
    if (values.selectionMode === 'THRESHOLD_THEN_TOP_N' && values.minScore === '') {
      errors.minScore = 'A minimum score is required as the pass floor for this selection mode.';
    }
  }

  return errors;
}
