var backgroundColors = [
    'rgba(255, 99, 132, 0.2)',
    'rgba(255, 159, 64, 0.2)',
    'rgba(255, 205, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(153, 102, 255, 0.2)',
    'rgba(201, 203, 207, 0.2)'
];

var borderColors = [
    'rgb(255, 99, 132)',
    'rgb(255, 159, 64)',
    'rgb(255, 205, 86)',
    'rgb(75, 192, 192)',
    'rgb(54, 162, 235)',
    'rgb(153, 102, 255)',
    'rgb(201, 203, 207)'
];

const chartConnectionPoolGauges = new Chart(document.getElementById(
        "chart-container-connection-pool-1"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "Metric (gauge)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Connections'
            },
        },
        responsive: true,
    },
});
const chartConnectionPoolTimes = new Chart(document.getElementById(
        "chart-container-connection-pool-2"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "Duration (ms)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Timings'
            },
        },
        responsive: true,
    },
});

const PoolChartsDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

PoolChartsDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
                stompClient = Stomp.over(socket),
                _this = this;
        // stompClient.log = (log) => {};
        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.refresh, function () {
                location.reload();
            });

            stompClient.subscribe(_this.settings.topics.charts, function () {
                _this.handleChartsUpdate();
            });
        });
    },

    handleChartsUpdate: function() {
        var _this = this;

        const queryString = window.location.search;

        $.getJSON("pool/data-points/gauge" + queryString, function(json) {
            _this.updateChart(chartConnectionPoolGauges, json);
        });
        $.getJSON("pool/data-points/time" + queryString, function(json) {
            _this.updateChart(chartConnectionPoolTimes, json);
        });
    },

    updateChart: function (chart,json) {
        const xValues = json[0]["data"];

        const yValues = json.filter((item, idx) => idx > 0)
                .map(function(item) {
                    var id = item["id"];
                    var bgColor = backgroundColors[id % backgroundColors.length];
                    var ogColor = borderColors[id % borderColors.length];
                    return {
                        label: item["name"],
                        data: item["data"],
                        backgroundColor: bgColor,
                        borderColor: ogColor,
                        fill: false,
                        tension: 1.2,
                        cubicInterpolationMode: 'monotone',
                        borderWidth: 1,
                        hoverOffset: 4,
                    };
                });

        const visibleStates=[];
        chart.data.datasets.forEach((dataset, datasetIndex) => {
            visibleStates.push(chart.isDatasetVisible(datasetIndex));
        });

        chart.config.data.labels = xValues;
        chart.config.data.datasets = yValues;

        if (visibleStates.length > 0) {
            chart.data.datasets.forEach((dataset, datasetIndex) => {
                chart.setDatasetVisibility(datasetIndex, visibleStates[datasetIndex]);
            });
        }

        chart.update('none');
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new PoolChartsDashboard({
        endpoints: {
            socket: '/pool-configurer',
        },

        topics: {
            charts: '/topic/chart/pool/update',
            refresh: '/topic/chart/pool/refresh',
        },
    });
});

