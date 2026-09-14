// Colored-left-border stat tile, replacing StatCard's icon+circle look on the Student
// Dashboard only (StatCard itself is untouched - it's shared with the Admin Dashboard).
// Purely visual: still fed the same real values StudentDashboard already computes.
export default function StudentStatTile({ label, value, color }) {
  return (
    <div className="bg-white rounded-xl p-4" style={{ borderLeft: `3px solid ${color}` }}>
      <p className="text-[13px] text-[#6b6785]">{label}</p>
      <p className="text-[20px] font-medium text-black mt-1">{value}</p>
    </div>
  );
}
