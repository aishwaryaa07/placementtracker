import { useState } from 'react';
import { Link } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function dateKey(year, month, day) {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

// Parses a plain YYYY-MM-DD (no time component) as a LOCAL date, never via `new Date(string)`
// - that constructor treats a date-only string as UTC midnight, which can display as the
// previous day in any timezone behind UTC.
function parseDateKey(key) {
  const [y, m, d] = key.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export default function DrivesCalendar({ drives }) {
  const today = new Date();
  const [monthCursor, setMonthCursor] = useState(new Date(today.getFullYear(), today.getMonth(), 1));
  const [selectedDate, setSelectedDate] = useState(null);

  const drivesByDate = new Map();
  drives.forEach((d) => {
    if (!d.driveDate) return;
    if (!drivesByDate.has(d.driveDate)) drivesByDate.set(d.driveDate, []);
    drivesByDate.get(d.driveDate).push(d);
  });

  const year = monthCursor.getFullYear();
  const month = monthCursor.getMonth();
  const startWeekday = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  // Always pad to a full 6 rows (42 cells) - a 28-31 day month starting on any weekday needs
  // between 4 and 6 rows, so without this the panel visibly resizes as you navigate months
  // (e.g. August 2026 needs 6 rows, September 2026 only 5). Fixed row count keeps every
  // month's calendar - and the panel around it - the same height.
  const cells = [];
  for (let i = 0; i < startWeekday; i++) cells.push(null);
  for (let day = 1; day <= daysInMonth; day++) cells.push(day);
  while (cells.length < 42) cells.push(null);

  const todayKey = dateKey(today.getFullYear(), today.getMonth(), today.getDate());
  const selectedDrives = selectedDate ? drivesByDate.get(selectedDate) || [] : [];

  function changeMonth(delta) {
    setSelectedDate(null);
    setMonthCursor(new Date(year, month + delta, 1));
  }

  return (
    <div className="drives-calendar">
      <div className="drives-calendar-nav">
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => changeMonth(-1)} aria-label="Previous month">
          &lsaquo;
        </button>
        <div className="drives-calendar-title">
          {monthCursor.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}
        </div>
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => changeMonth(1)} aria-label="Next month">
          &rsaquo;
        </button>
      </div>

      <div className="drives-calendar-grid">
        {WEEKDAYS.map((d) => (
          <div key={d} className="drives-calendar-weekday">
            {d}
          </div>
        ))}
        {cells.map((day, i) => {
          if (day === null) return <div key={`pad-${i}`} className="drives-calendar-cell empty" />;
          const key = dateKey(year, month, day);
          const dayDrives = drivesByDate.get(key) || [];
          const hasDrives = dayDrives.length > 0;
          const classes = ['drives-calendar-cell'];
          if (key === todayKey) classes.push('today');
          if (key === selectedDate) classes.push('selected');
          if (hasDrives) classes.push('has-drives');
          const visibleTags = dayDrives.slice(0, 2);
          const overflowCount = dayDrives.length - visibleTags.length;
          return (
            <button
              type="button"
              key={key}
              className={classes.join(' ')}
              disabled={!hasDrives}
              onClick={() => setSelectedDate((prev) => (prev === key ? null : key))}
              title={hasDrives ? `${dayDrives.length} drive(s) on this date` : undefined}
            >
              <span className="drives-calendar-day-number">{day}</span>
              {hasDrives && (
                <span className="drives-calendar-tags">
                  {visibleTags.map((d) => (
                    <span key={d.id} className="drives-calendar-tag">
                      {d.company.name}
                    </span>
                  ))}
                  {overflowCount > 0 && <span className="drives-calendar-tag more">+{overflowCount}</span>}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {selectedDate && selectedDrives.length > 0 && (
        <div className="drives-calendar-details">
          <div className="drives-calendar-details-date">
            {parseDateKey(selectedDate).toLocaleDateString(undefined, {
              weekday: 'long',
              year: 'numeric',
              month: 'long',
              day: 'numeric',
            })}
          </div>
          <ul>
            {selectedDrives.map((d) => (
              <li key={d.id}>
                <Link to={`/drives/${d.id}`} className="link-button">
                  {d.role} @ {d.company.name}
                </Link>
                <StatusBadge status={d.status} />
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
