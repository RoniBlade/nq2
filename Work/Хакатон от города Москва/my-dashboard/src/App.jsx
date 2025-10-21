import { useMemo, useState } from "react";
import { AnimatePresence, motion as Motion } from "framer-motion";
import ChartCard from "./components/ChartCard.jsx";
import ChartTabsMobile from "./components/ChartTabsMobile.jsx";
import TimeTabs from "./components/TimeTabs.jsx";
import chartMock from "../mock_chart_multi.json";
import cardsNow from "../mock_cards_now.json";
import cardsHistoryPage1 from "../mock_cards_history_page1.json";

function App() {
  const [activeTab, setActiveTab] = useState("pressure");
  const [range, setRange] = useState(15); // 15, 30, 45, 60 минут
  const [circuit, setCircuit] = useState("Контур A");

  const charts = {
    pressure: {
      title: "Напор воды",
      color: "#3b82f6",
      series: [
        {
          name: "Расход",
          data: chartMock.series[0].points.map(
            (p) => Math.round(p.value * 10000) / 100
          ),
        },
      ],
    },
    temperature: {
      title: "Температура",
      color: "#ef4444",
      series: [
        {
          name: "Температура",
          data: [18, 19, 21, 22, 24, 25, 24, 23, 22, 21, 20, 19],
        },
      ],
    },
    sensors: {
      title: "Датчики давления",
      color: "#10b981",
      series: [
        {
          name: "Срабатывания",
          data: [1, 2, 3, 2, 4, 3, 5, 4, 3, 2, 4, 5],
        },
      ],
    },
  };

  const baseCategories = chartMock.series[0].points.map((p) => {
    const d = new Date(p.ts);
    return `${String(d.getHours()).padStart(2, "0")}:${String(
      d.getMinutes()
    ).padStart(2, "0")}`;
  });

  const rangesToPoints = { 15: 5, 30: 7, 45: 9, 60: 12 };

  const timeCategories = useMemo(() => {
    const points = rangesToPoints[range] || 5;
    return baseCategories.slice(0, points);
  }, [range]);

  const currentChart = useMemo(() => {
    const cfg = charts[activeTab];
    const points = rangesToPoints[range] || 5;
    const circuitShift =
      circuit === "Контур B" ? 5 : circuit === "Контур C" ? -5 : 0;
    const sliced = cfg.series.map((s) => ({
      ...s,
      data: s.data.slice(0, points).map((v) => v + circuitShift),
    }));
    return { ...cfg, series: sliced };
  }, [activeTab, range, circuit]);

  // Cards data based on range and circuit
  const nowMap = useMemo(() => {
    const map = new Map();
    cardsNow.forEach((item) => map.set(item.title, item));
    return map;
  }, []);

  const historyByTitle = useMemo(() => {
    const by = new Map();
    if (cardsHistoryPage1?.items) {
      cardsHistoryPage1.items.forEach((it) => {
        if (!by.has(it.title)) by.set(it.title, []);
        by.get(it.title).push(it);
      });
      // newest first
      for (const [, arr] of by)
        arr.sort((a, b) => new Date(b.ts) - new Date(a.ts));
    }
    return by;
  }, []);

  const selectedCard = useMemo(() => {
    if (range === "now") return nowMap.get(circuit);
    const arr = historyByTitle.get(circuit);
    if (!arr || arr.length === 0) return nowMap.get(circuit);
    // pick the first Nth depending on range to simulate different slices
    const idx = Math.min((rangesToPoints[range] || 1) - 1, arr.length - 1);
    return arr[idx];
  }, [range, circuit, nowMap, historyByTitle]);

  const alarmText = useMemo(() => {
    const alarm =
      (range === "now" ? nowMap.get(circuit)?.alarm : selectedCard?.alarm) ||
      null;
    return alarm ? `Авария: ${alarm}` : "Предупреждение";
  }, [range, circuit, nowMap, selectedCard]);

  const stateTitle = useMemo(() => {
    if (range === "now") return "Текущее состояние";
    if (range === 60) return "Состояние 1 час назад";
    return `Состояние ${range} минут назад`;
  }, [range]);
  const hasAlarm = useMemo(() => {
    const current = range === "now" ? nowMap.get(circuit) : selectedCard;
    return Boolean(current?.alarm);
  }, [range, circuit, nowMap, selectedCard]);

  const lastAlarm = useMemo(() => {
    const nowAlarm = nowMap.get(circuit)?.alarm || null;
    if (nowAlarm) return nowAlarm;
    const arr = historyByTitle.get(circuit) || [];
    const found = arr.find((it) => Boolean(it.alarm));
    return found?.alarm || "—";
  }, [circuit, nowMap, historyByTitle]);

  const avgPressure = useMemo(() => {
    const series0 = currentChart.series?.[0];
    const data = Array.isArray(series0?.data) ? series0.data : [];
    if (!data.length) return "—";
    const sum = data.reduce((acc, v) => acc + (Number(v) || 0), 0);
    const avg = sum / data.length;
    return `${avg.toFixed(2)} л/мин`;
  }, [currentChart]);

  const aiRisk = useMemo(() => {
    const src = range === "now" ? nowMap.get(circuit) : selectedCard;
    const value = src?.aiForecast?.value;
    return value != null ? `${Math.round(value * 100)}%` : "—";
  }, [range, circuit, nowMap, selectedCard]);
  const [showHelp, setShowHelp] = useState(false);
  return (
    <div className="min-h-screen bg-gray-100 p-6 sm:p-6 relative overflow-hidden">
      {/* Background grid */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 -z-10 opacity-40"
        style={{
          backgroundImage:
            "linear-gradient(to right, rgba(0,0,0,0.03) 1px, transparent 1px), linear-gradient(to bottom, rgba(0,0,0,0.03) 1px, transparent 1px)",
          backgroundSize: "24px 24px",
        }}
      />
      {/* Gradient blobs */}
      <div
        aria-hidden
        className="pointer-events-none absolute -z-10 -top-24 -left-24 w-[480px] h-[480px] rounded-full blur-3xl"
        style={{
          background:
            "radial-gradient(circle at center, rgba(59,130,246,0.18), rgba(59,130,246,0) 60%)",
        }}
      />
      <div
        aria-hidden
        className="pointer-events-none absolute -z-10 -bottom-24 -right-24 w-[520px] h-[520px] rounded-full blur-3xl"
        style={{
          background:
            "radial-gradient(circle at center, rgba(16,185,129,0.16), rgba(16,185,129,0) 60%)",
        }}
      />
      {/* Заголовок */}
      <div className="mb-10 sm:mb-12 text-center">
        <h1
          className="text-3xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-blue-700 via-indigo-600 to-emerald-600 bg-clip-text text-transparent"
          style={{ fontFamily: "Helvetica Neue, sans-serif" }}
        >
          Система мониторинга
        </h1>
        <p
          className="mt-3 text-sm sm:text-base text-gray-600"
          style={{ fontFamily: "Helvetica Neue, sans-serif" }}
        >
          Реальное время · Аналитика · Рекомендации
        </p>
      </div>

      {/* Панель: время + контур по центру */}
      <div className="flex flex-col md:flex-row justify-center items-stretch md:items-center gap-2 sm:gap-4 mb-4 sm:mb-8 px-2">
        <TimeTabs value={range} onChange={setRange} />
        <select
          value={circuit}
          onChange={(e) => setCircuit(e.target.value)}
          className="bg-white ring-1 ring-gray-200 text-black rounded-xl text-sm sm:text-base font-medium px-3 sm:px-4 h-12 shadow-sm md:w-auto w-full"
          style={{ fontFamily: "Helvetica Neue, sans-serif" }}
        >
          <option>Контур A</option>
          <option>Контур B</option>
          <option>Контур C</option>
        </select>
      </div>

      {/* Верхняя секция с карточками */}
      <div
        className={`flex flex-col md:flex-row ${
          !hasAlarm ? "items-center" : "items-stretch"
        } justify-center gap-4 sm:gap-10 mb-6 sm:mb-10 px-2`}
      >
        <AnimatePresence initial={false} mode="popLayout">
          {/* Белая карточка */}
          <Motion.div
            key={`white-${circuit}-${range}`}
            layout
            initial={{ opacity: 0, y: 12, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -12, scale: 0.98 }}
            transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            className="bg-white shadow-lg rounded-2xl p-4 sm:p-6 w-full md:w-[420px] min-h-[240px] sm:h-[260px] flex flex-col justify-between"
          >
            <div>
              <h2
                className="text-xl sm:text-2xl font-bold mb-3 sm:mb-4 text-black"
                style={{ fontFamily: "Helvetica Neue, sans-serif" }}
              >
                {stateTitle}
              </h2>
              <p
                className="mb-2 text-base sm:text-lg text-black"
                style={{ fontFamily: "Helvetica Neue, sans-serif" }}
              >
                Важность: • {selectedCard?.importance || "Средняя"}
              </p>
              <p
                className="mb-2 text-base sm:text-lg text-black"
                style={{ fontFamily: "Helvetica Neue, sans-serif" }}
              >
                Уверенность:{" "}
                {selectedCard?.confidence != null
                  ? Math.round(selectedCard.confidence * 100) + "%"
                  : "—"}
              </p>
              <p
                className="mt-4 sm:mt-6 text-base sm:text-lg text-black"
                style={{ fontFamily: "Helvetica Neue, sans-serif" }}
              >
                Рекомендация: {selectedCard?.recommendation || "—"}
              </p>
            </div>
          </Motion.div>

          {/* Красная карточка */}
          {hasAlarm && (
            <Motion.div
              key={`red-${circuit}-${range}`}
              layout
              initial={{ opacity: 0, y: 12, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -12, scale: 0.98 }}
              transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
              className="bg-red-600 text-white shadow-lg rounded-2xl p-4 sm:p-6 w-full md:w-[420px] min-h-[240px] sm:h-[260px] flex flex-col justify-between"
            >
              <div>
                <h2
                  className="text-xl sm:text-2xl font-bold mb-3 sm:mb-4"
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  {alarmText}
                </h2>
                <p
                  className="text-base sm:text-lg mb-2"
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Важность: • {selectedCard?.importance || "Высокая"}
                </p>
                <p
                  className="text-base sm:text-lg"
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  AI прогноз:{" "}
                  {selectedCard?.aiForecast
                    ? Math.round(selectedCard.aiForecast.value * 100) +
                      "% " +
                      (selectedCard.aiForecast.note || "")
                    : "—"}
                </p>
              </div>

              {/* Кнопки */}
              <div className="flex flex-col sm:flex-row gap-3 sm:gap-4">
                <button
                  className="px-5 sm:px-7 py-3 sm:py-3.5 bg-red-700 text-white rounded-xl hover:bg-red-800 text-base sm:text-lg font-medium w-full sm:w-auto"
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Правильно
                </button>
                <button
                  className="px-5 sm:px-7 py-3 sm:py-3.5 bg-red-700 text-white rounded-xl hover:bg-red-800 text-base sm:text-lg font-medium w-full sm:w-auto"
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Неправильно
                </button>
              </div>
            </Motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Compact chips bar */}
      <div className="px-2 mb-3 sm:mb-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 sm:gap-3">
          <div className="inline-flex items-center gap-2 bg-white/90 backdrop-blur-sm text-gray-900 ring-1 ring-gray-200 rounded-full px-3 py-1.5 text-xs sm:text-sm font-medium min-w-0">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500 inline-block" />
            <span className="truncate">
              <span className="hidden sm:inline">Последняя авария: </span>
              <span className="sm:hidden">Авария: </span>
              {lastAlarm}
            </span>
          </div>
          <div className="inline-flex items-center gap-2 bg-white/90 backdrop-blur-sm text-gray-900 ring-1 ring-gray-200 rounded-full px-3 py-1.5 text-xs sm:text-sm font-medium">
            <span className="text-blue-700">⌀</span>
            <span className="truncate">
              <span className="hidden sm:inline">Средний расход: </span>
              <span className="sm:hidden">Ср. расход: </span>
              {avgPressure}
            </span>
          </div>
          <div className="inline-flex items-center gap-2 bg-white/90 backdrop-blur-sm text-gray-900 ring-1 ring-gray-200 rounded-full px-3 py-1.5 text-xs sm:text-sm font-medium">
            <span className="text-rose-600">AI</span>
            <span className="truncate">
              <span className="hidden sm:inline">Риск: </span>
              <span className="sm:hidden">Риск: </span>
              {aiRisk}
            </span>
          </div>
        </div>
      </div>

      <Motion.div
        layout
        className="bg-white shadow-lg rounded-2xl p-4 sm:p-6 w-full"
      >
        {/* Mobile tabs styled like desktop */}
        <div className="mb-3 md:hidden">
          <ChartTabsMobile value={activeTab} onChange={setActiveTab} />
        </div>
        <ChartCard
          title={currentChart.title}
          series={currentChart.series}
          categories={timeCategories}
          color={currentChart.color}
          yUnit="л/мин"
          rightControls={
            <>
              {/* Табы справа */}
              <div className="hidden md:flex gap-2">
                <button
                  onClick={() => setActiveTab("pressure")}
                  className={`${
                    activeTab === "pressure"
                      ? "bg-blue-700 text-white"
                      : "bg-white text-gray-900 hover:bg-blue-50 ring-1 ring-gray-200"
                  } px-5 py-2.5 rounded-xl text-base font-medium`}
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Напор воды
                </button>
                <button
                  onClick={() => setActiveTab("temperature")}
                  className={`${
                    activeTab === "temperature"
                      ? "bg-blue-700 text-white"
                      : "bg-white text-gray-900 hover:bg-blue-50 ring-1 ring-gray-200"
                  } px-5 py-2.5 rounded-xl text-base font-medium`}
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Температура
                </button>
                <button
                  onClick={() => setActiveTab("sensors")}
                  className={`${
                    activeTab === "sensors"
                      ? "bg-blue-700 text-white"
                      : "bg-white text-gray-900 hover:bg-blue-50 ring-1 ring-gray-200"
                  } px-5 py-2.5 rounded-xl text-base font-medium`}
                  style={{ fontFamily: "Helvetica Neue, sans-serif" }}
                >
                  Датчики давления
                </button>
              </div>
            </>
          }
        />
      </Motion.div>
      {/* Floating help button */}
      <button
        type="button"
        onClick={() => setShowHelp((v) => !v)}
        className="fixed bottom-6 right-6 z-20 inline-flex items-center gap-2 bg-blue-700 text-white shadow-lg hover:bg-blue-800 active:bg-blue-900 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-700 rounded-full px-4 py-3 text-sm font-medium"
        style={{ fontFamily: "Helvetica Neue, sans-serif" }}
      >
        <span className="w-2.5 h-2.5 rounded-full bg-white/80" />
        Помощь
      </button>

      {/* Help panel */}
      <AnimatePresence>
        {showHelp && (
          <Motion.div
            key="help-panel"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 16 }}
            transition={{ duration: 0.2 }}
            className="fixed bottom-20 right-6 z-20 w-[320px] bg-white rounded-2xl shadow-2xl ring-1 ring-gray-200 p-4"
          >
            <div className="flex items-start justify-between gap-3">
              <h3
                className="text-base font-semibold text-gray-900"
                style={{ fontFamily: "Helvetica Neue, sans-serif" }}
              >
                Как читать экран
              </h3>
              <button
                onClick={() => setShowHelp(false)}
                className="ml-2 text-gray-500 hover:text-gray-700"
                aria-label="Закрыть"
              >
                ✕
              </button>
            </div>
            <ul
              className="mt-3 space-y-2 text-sm text-gray-700"
              style={{ fontFamily: "Helvetica Neue, sans-serif" }}
            >
              <li>
                <strong>Карточка состояния</strong>: сводка состояния и
                рекомендации.
              </li>
              <li>
                <strong>Карточка аварии</strong>: отображается только при
                зафиксированной аварии.
              </li>
              <li>
                <strong>Табы времени</strong>: срез данных за выбранный период.
              </li>
              <li>
                <strong>Чипы</strong>: последняя авария, средний расход, риск
                AI.
              </li>
              <li>
                <strong>График</strong>: значения по времени; цвет соответствует
                метрике.
              </li>
            </ul>
          </Motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default App;
