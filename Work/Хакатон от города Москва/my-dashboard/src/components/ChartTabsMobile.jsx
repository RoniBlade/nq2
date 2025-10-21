function ChartTabsMobile({ value, onChange }) {
  const tabs = [
    { key: "pressure", label: "Напор воды" },
    { key: "temperature", label: "Температура" },
    { key: "sensors", label: "Датчики давления" },
  ];

  return (
    <div className="w-full flex justify-center">
      <div className="inline-flex items-center rounded-2xl bg-white p-1 shadow-sm ring-1 ring-gray-200 overflow-auto">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => onChange(t.key)}
            className={`${
              value === t.key
                ? "bg-blue-700 text-white"
                : "bg-white text-gray-900 hover:bg-blue-50"
            } h-12 px-5 rounded-xl text-base font-medium whitespace-nowrap`}
            style={{ fontFamily: "Helvetica Neue, sans-serif" }}
          >
            {t.label}
          </button>
        ))}
      </div>
    </div>
  );
}

export default ChartTabsMobile;
