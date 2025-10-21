function TimeTabs({
  value,
  onChange,
  options = [15, 30, 45, 60],
  showCurrent = true,
}) {
  return (
    <div className="inline-flex items-center rounded-2xl bg-white p-1 shadow-sm ring-1 ring-gray-200 max-w-full overflow-auto">
      {showCurrent && (
        <button
          onClick={() => onChange("now")}
          className={`${
            value === "now"
              ? "bg-blue-700 text-white"
              : "bg-white text-gray-900 hover:bg-blue-50"
          } h-12 px-5 rounded-xl text-base font-medium leading-none transition-colors mr-1 min-w-[9rem]`}
          style={{ fontFamily: "Helvetica Neue, sans-serif" }}
        >
          <span className="inline-flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-500 opacity-60"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-blue-600"></span>
            </span>
            Текущее состояние
          </span>
        </button>
      )}
      {options.map((opt, idx) => {
        const isActive = value === opt;
        const label = opt === 60 ? "1 ч" : `${opt} мин`;
        return (
          <button
            key={opt}
            onClick={() => onChange(opt)}
            className={`${
              isActive
                ? "bg-blue-700 text-white"
                : "bg-white text-gray-900 hover:bg-blue-50"
            } h-12 px-4 sm:px-5 rounded-xl text-sm sm:text-base font-medium leading-none transition-colors whitespace-nowrap min-w-[5.5rem]`}
            style={{ fontFamily: "Helvetica Neue, sans-serif" }}
          >
            {label}
          </button>
        );
      })}
    </div>
  );
}

export default TimeTabs;
