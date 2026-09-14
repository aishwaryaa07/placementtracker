import { useState } from 'react';
import { validateRound } from './roundFormUtils';

export default function RoundForm({
  initialValues,
  existingRounds,
  editingRoundId,
  submitting,
  formError,
  fieldErrors,
  onSubmit,
  onCancel,
}) {
  const [values, setValues] = useState(initialValues);
  const [localErrors, setLocalErrors] = useState({});

  function handleChange(field) {
    return (e) => setValues((prev) => ({ ...prev, [field]: e.target.value }));
  }

  function handleSubmit(e) {
    e.preventDefault();
    const errors = validateRound(values, existingRounds, editingRoundId);
    if (Object.keys(errors).length > 0) {
      setLocalErrors(errors);
      return;
    }
    setLocalErrors({});
    onSubmit({
      sequence: Number(values.sequence),
      name: values.name.trim(),
      roundDate: values.roundDate || null,
      description: values.description.trim() || null,
      minScore: values.minScore === '' ? null : Number(values.minScore),
      selectionMode: values.selectionMode,
      topN: values.topN === '' ? null : Number(values.topN),
    });
  }

  const errors = { ...localErrors, ...fieldErrors };
  const isRankingMode = values.selectionMode === 'TOP_N' || values.selectionMode === 'THRESHOLD_THEN_TOP_N';

  return (
    <form onSubmit={handleSubmit}>
      {formError && <div className="form-error">{formError}</div>}

      <label htmlFor="round-sequence">
        Sequence<span className="required-mark">*</span>
      </label>
      <input
        id="round-sequence"
        type="number"
        step="1"
        min="1"
        value={values.sequence}
        onChange={handleChange('sequence')}
        placeholder="1"
      />
      {errors.sequence && <div className="field-error">{errors.sequence}</div>}

      <label htmlFor="round-name">
        Name<span className="required-mark">*</span>
      </label>
      <input
        id="round-name"
        value={values.name}
        onChange={handleChange('name')}
        placeholder="e.g. Technical Interview"
      />
      {errors.name && <div className="field-error">{errors.name}</div>}

      <label htmlFor="round-date">Round Date</label>
      <input id="round-date" type="date" value={values.roundDate} onChange={handleChange('roundDate')} />
      {errors.roundDate && <div className="field-error">{errors.roundDate}</div>}

      <label htmlFor="round-description">Description</label>
      <textarea
        id="round-description"
        rows={3}
        value={values.description}
        onChange={handleChange('description')}
        placeholder="What this round covers, and what students need to know or prepare - visible to students."
      />
      {errors.description && <div className="field-error">{errors.description}</div>}

      <label htmlFor="round-min-score">Minimum Score to Pass</label>
      <input
        id="round-min-score"
        type="number"
        step="0.01"
        min="0"
        value={values.minScore}
        onChange={handleChange('minScore')}
        placeholder="Leave blank if you'll grade this round yourself"
      />
      <div className="field-hint">
        If set, students can submit their own score for this round. For Threshold mode it's the pass/fail cutoff;
        for Threshold-then-Top-N it's the floor a student must clear before being ranked.
      </div>
      {errors.minScore && <div className="field-error">{errors.minScore}</div>}

      <label htmlFor="round-selection-mode">Selection Mode</label>
      <select id="round-selection-mode" value={values.selectionMode} onChange={handleChange('selectionMode')}>
        <option value="THRESHOLD">Threshold - pass/fail instantly against Minimum Score</option>
        <option value="TOP_N">Top N - only the highest N scorers pass</option>
        <option value="THRESHOLD_THEN_TOP_N">Threshold then Top N - must clear Minimum Score, then ranked</option>
      </select>
      <div className="field-hint">
        Top N modes don&apos;t decide anyone&apos;s result until you click Finalize on the round (once every
        candidate has a score) - individual scores are recorded but held as &quot;Scored&quot; until then.
      </div>

      {isRankingMode && (
        <>
          <label htmlFor="round-top-n">
            Top N<span className="required-mark">*</span>
          </label>
          <input
            id="round-top-n"
            type="number"
            step="1"
            min="1"
            value={values.topN}
            onChange={handleChange('topN')}
            placeholder="e.g. 20"
          />
          {errors.topN && <div className="field-error">{errors.topN}</div>}
        </>
      )}

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save Round'}
        </button>
      </div>
    </form>
  );
}
