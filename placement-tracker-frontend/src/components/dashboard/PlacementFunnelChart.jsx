// Single-series (one metric: application count) across N ordered stages, so no legend is
// needed - the stage-name + value direct labels carry identity. Ramp is a validated 5-step
// single-hue purple sequential scale (light -> dark = earliest -> latest stage) - see the
// --seq-1..5 comment in index.css for the validator run.
const RAMP = ['var(--seq-1)', 'var(--seq-2)', 'var(--seq-3)', 'var(--seq-4)', 'var(--seq-5)'];
const TEXT_ON_RAMP = ['var(--peacock)', '#ffffff', '#ffffff', '#ffffff', '#ffffff'];

const CHART_WIDTH = 700;
const CHART_HEIGHT = 260;
const TOP_MARGIN = 34; // room for the stage-name label above each segment
const BOTTOM_MARGIN = 26; // room for the % label below each segment
const MAX_BLOCK_HEIGHT = CHART_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN;
const MIN_BLOCK_HEIGHT = 24;
const MID_Y = TOP_MARGIN + MAX_BLOCK_HEIGHT / 2;

export default function PlacementFunnelChart({ stages }) {
  const firstValue = stages[0]?.value || 1;
  const segWidth = CHART_WIDTH / stages.length;

  // Always at least MIN_BLOCK_HEIGHT, even at 0 - a zero-value stage still needs to render as
  // a thin visible sliver so the funnel reads as tapering to almost nothing, not as a gap
  // where a segment silently failed to draw.
  const heights = stages.map((s) => {
    const ratio = firstValue === 0 ? 0 : Math.min(1, s.value / firstValue);
    return MIN_BLOCK_HEIGHT + ratio * (MAX_BLOCK_HEIGHT - MIN_BLOCK_HEIGHT);
  });

  return (
    <svg
      viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
      width="100%"
      role="img"
      aria-label={`Placement funnel: ${stages.map((s) => `${s.label} ${s.value}`).join(', ')}`}
    >
      {stages.map((stage, i) => {
        const x0 = i * segWidth;
        const x1 = x0 + segWidth;
        const leftH = heights[i];
        const rightH = i === stages.length - 1 ? heights[i] : heights[i + 1];
        const points = [
          `${x0},${MID_Y - leftH / 2}`,
          `${x1},${MID_Y - rightH / 2}`,
          `${x1},${MID_Y + rightH / 2}`,
          `${x0},${MID_Y + leftH / 2}`,
        ].join(' ');
        const pct = Math.round((stage.value / firstValue) * 100);
        const labelColor = TEXT_ON_RAMP[i % TEXT_ON_RAMP.length];

        return (
          <g key={stage.key}>
            <title>
              {stage.label}: {stage.value} ({pct}% of {stages[0].label})
            </title>
            <text x={x0 + segWidth / 2} y={TOP_MARGIN - 12} textAnchor="middle" className="funnel-label">
              {stage.label}
            </text>
            <polygon points={points} fill={RAMP[i % RAMP.length]} stroke="var(--surface)" strokeWidth="2" />
            {stage.value > 0 && (
              <text x={x0 + segWidth / 2} y={MID_Y + 5} textAnchor="middle" className="funnel-value-inline" fill={labelColor}>
                {stage.value}
              </text>
            )}
            <text x={x0 + segWidth / 2} y={CHART_HEIGHT - 8} textAnchor="middle" className="funnel-pct">
              {pct}%
            </text>
          </g>
        );
      })}
    </svg>
  );
}
