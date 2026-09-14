export default function TableSkeleton({ rows = 5, columns = 5 }) {
  return (
    <div className="table-card">
      <div className="table-scroll">
        <table className="data-table table-skeleton">
          <tbody>
            {Array.from({ length: rows }).map((_, rowIndex) => (
              <tr key={rowIndex}>
                {Array.from({ length: columns }).map((__, colIndex) => (
                  <td key={colIndex}>
                    <div className="skeleton skeleton-text" style={{ width: colIndex === 0 ? '70%' : '50%' }} />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
