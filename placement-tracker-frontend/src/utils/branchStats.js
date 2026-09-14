// Shared by Dashboard's "Top Performing Branches" panel and the Reports page's full ranking -
// one computation, two views (top-N slice vs. the whole list).
export function computeBranchStats(applications) {
  const byBranch = new Map();
  applications.forEach((a) => {
    const branch = a.studentBranch || 'Unknown';
    if (!byBranch.has(branch)) byBranch.set(branch, { branch, applications: 0, placed: 0 });
    const entry = byBranch.get(branch);
    entry.applications += 1;
    if (a.offer?.status === 'ACCEPTED') entry.placed += 1;
  });
  return [...byBranch.values()]
    .map((entry) => ({ ...entry, rate: entry.applications === 0 ? 0 : Math.round((entry.placed / entry.applications) * 100) }))
    .sort((a, b) => b.placed - a.placed || b.rate - a.rate);
}
