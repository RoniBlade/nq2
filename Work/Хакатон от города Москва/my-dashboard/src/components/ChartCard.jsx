import Chart from "react-apexcharts";

function ChartCard({
  title,
  series,
  categories,
  color = "#3b82f6",
  yUnit = "",
  rightControls = null,
}) {
  const chartConfig = {
    type: "line",
    height: "100%",
    series,
    options: {
      chart: {
        toolbar: { show: false },
        fontFamily: "Helvetica Neue, sans-serif",
        animations: { enabled: true },
        parentHeightOffset: 0,
      },
      dataLabels: { enabled: false },
      colors: [color],
      stroke: { lineCap: "round", curve: "smooth", width: 3 },
      markers: { size: 0 },
      xaxis: {
        categories,
        axisTicks: { show: false },
        axisBorder: { show: false },
        labels: {
          style: {
            colors: "#616161",
            fontSize: "11px",
            fontFamily: "inherit",
            fontWeight: 400,
          },
        },
      },
      yaxis: {
        labels: {
          formatter: (val) => {
            const n = Number(val);
            const num = Number.isFinite(n) ? n.toFixed(2) : val;
            return yUnit ? `${num} ${yUnit}` : String(num);
          },
          style: {
            colors: "#616161",
            fontSize: "11px",
            fontFamily: "inherit",
            fontWeight: 400,
          },
        },
      },
      grid: {
        show: true,
        borderColor: "#e5e7eb",
        strokeDashArray: 5,
        xaxis: { lines: { show: true } },
        padding: { top: 5, right: 20 },
      },
      fill: { opacity: 0.2, type: "gradient" },
      tooltip: { theme: "dark" },
      title: { text: undefined },
    },
  };

  return (
    <div className="w-full">
      <div className="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center mb-4 sm:mb-6">
        <h2
          className="text-xl sm:text-2xl font-bold text-black"
          style={{ fontFamily: "Helvetica Neue, sans-serif" }}
        >
          {title}
        </h2>
        <div className="flex flex-wrap items-center gap-3">{rightControls}</div>
      </div>
      <div className="px-1 sm:px-2 pb-0 h-60 sm:h-64">
        <Chart {...chartConfig} />
      </div>
    </div>
  );
}

export default ChartCard;
