export const emptyInterviewerDriveForm = {
  role: '',
  description: '',
  ctc: '',
  minCgpa: '',
  eligibleBranches: '',
  applicationDeadline: '',
  driveDate: '',
  minQualification: 'EITHER',
  freshersOnly: false,
};

export function driveToFormValues(drive) {
  return {
    role: drive.role || '',
    description: drive.description || '',
    ctc: drive.ctc ?? '',
    minCgpa: drive.minCgpa ?? '',
    eligibleBranches: (drive.eligibleBranches || []).join(', '),
    applicationDeadline: drive.applicationDeadline || '',
    driveDate: drive.driveDate || '',
    minQualification: drive.minQualification || 'EITHER',
    freshersOnly: Boolean(drive.freshersOnly),
  };
}

export function formValuesToRequest(values) {
  return {
    role: values.role.trim(),
    description: values.description.trim() || null,
    ctc: values.ctc === '' ? null : Number(values.ctc),
    minCgpa: values.minCgpa === '' ? null : Number(values.minCgpa),
    eligibleBranches: values.eligibleBranches
      .split(/[,\s]+/)
      .map((s) => s.trim())
      .filter(Boolean),
    applicationDeadline: values.applicationDeadline || null,
    driveDate: values.driveDate || null,
    minQualification: values.minQualification || 'EITHER',
    freshersOnly: Boolean(values.freshersOnly),
  };
}
