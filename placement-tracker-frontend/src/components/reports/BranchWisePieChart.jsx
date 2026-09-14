import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from 'recharts';

// Branch-wise accepted-offers pie chart, added to Reports & Analytics alongside the existing
// Applications by Status / Placement Rate by Branch / Company Conversion panels.
//
// data: [{ branch, offers }] - "offers" here is accepted offers per branch (computeBranchStats'
// `placed` field), the same number already shown in the Placement Rate by Branch table on this
// page, just visualized as a share instead of a per-row count.

// Validated categorical palette (dataviz skill six-checks: lightness band, chroma floor, CVD
// separation, normal-vision floor, contrast - all PASS light+dark) in fixed hue order.
const COLORS = ['#7c5cfc', '#159a56', '#1f6ba5', '#d97706', '#db2777'];

export default function BranchWisePieChart({ data }) {
  const chartData = data.filter((d) => d.offers > 0);

  return (
    <div className="bg-white rounded-2xl border border-[#e7e4fa] p-5">
      <h3 className="text-[15px] font-medium text-black mb-1">Branch-wise accepted offers</h3>
      <p className="text-[13px] text-[#6b6785] mb-3">Share of accepted offers, by branch.</p>

      {chartData.length === 0 ? (
        <p className="text-[13px] text-[#6b6785] py-8 text-center">No accepted offers yet.</p>
      ) : (
        <div style={{ width: '100%', height: 240 }}>
          <ResponsiveContainer>
            <PieChart>
              <Pie data={chartData} dataKey="offers" nameKey="branch" innerRadius={50} outerRadius={85} paddingAngle={2}>
                {chartData.map((entry, i) => (
                  <Cell key={entry.branch} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}
