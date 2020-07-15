'use strict';

require('./jquery-ui-custom.js')

const moment = require('moment/min/moment-with-locales.min.js');
const i18n = require('./i18n.js');
require('chart.js/dist/Chart.min.js');
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const RANGE_PICKER_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
const RANGE_PICKER_DATE_FORMAT = 'DD/MM/YYYY';
const TOOLTIP_TIME_FORMAT = 'D MMMM YYYY HH:mm';
const TIME_AXIS_DISPLAY_FORMAT = {
    second: 'HH:mm',
    minute: 'HH:mm',
    hour: 'HH:mm'
};
const ERROR_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm';

function initHistory() {

    console.log(i18n.locale);
    console.log(i18n.lang);

    $('#history-start').datetimepicker({
        locale: i18n.locale.toLowerCase(),
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().startOf('day').format(RANGE_PICKER_DATETIME_FORMAT)
    });

    $('#history-start-label').on('click', function () {
        $('#history-date').val(moment($('#history-start').val(), RANGE_PICKER_DATETIME_FORMAT).format(RANGE_PICKER_DATE_FORMAT));
        $('#history-end').val(moment($('#history-start').val(), RANGE_PICKER_DATETIME_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').hide();
        $('#range-picker-title').hide();
        $('#date-picker-input').show();
        $('#date-picker-title').show();
    });

    $('#history-end').datetimepicker({
        locale: i18n.locale.toLowerCase(),
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().endOf('day').format(RANGE_PICKER_DATETIME_FORMAT)
    });

    $('#history-end-label').on('click', function () {
        $('#history-date').val(moment($('#history-end').val(), RANGE_PICKER_DATETIME_FORMAT).format(RANGE_PICKER_DATE_FORMAT));
        $('#history-start').val(moment($('#history-end').val(), RANGE_PICKER_DATETIME_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').hide();
        $('#range-picker-title').hide();
        $('#date-picker-input').show();
        $('#date-picker-title').show();
    });

    $('#history-date').datepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy',
        footer: true,
        modal: true,
        weekStartDay: 1,
        uiLibrary: 'bootstrap4',
        value: moment().format(RANGE_PICKER_DATE_FORMAT)
    });

    $('#history-date').on('change', function () {
        $('#history-start').val(moment($('#history-date').val(), RANGE_PICKER_DATE_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#history-end').val(moment($('#history-date').val(), RANGE_PICKER_DATE_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
    });

    $('#history-date-label').on('click', function () {
        $('#history-start').val(moment($('#history-date').val(), RANGE_PICKER_DATE_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#history-end').val(moment($('#history-date').val(), RANGE_PICKER_DATE_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').show();
        $('#range-picker-title').show();
        $('#date-picker-input').hide();
        $('#date-picker-title').hide();
    });
}

async function showHistory(deviceId) {

    const $modal = $('#history');
    const $show = $('#history-path');
    const $close = $('#history-close');
    const $chart = $('#history-chart');

    var chart = null;

    function destroyChart() {
        if (chart != null) {
            chart.destroy();
        }
    }

    async function drawHistoryData() {

        const range = await getRange();

        if (range) {

            const history = await fetchWithRedirect(`/api/device/${deviceId}/history/${range.start.getTime()}/${range.end.getTime()}`);

            if (history && history.length > 0) {
                destroyChart();

                const $chart = $('#history-chart');
                const ctx = $chart[0].getContext('2d');


                const batteryDataset = {
                    label: i18n.translate('Battery'),
                    yAxisID: 'battery',
                    borderWidth: 2,
                    lineTension: 0,
                    borderColor: 'blue',
                    backgroundColor: 'blue',
                    fill: false,
                    data: history.map(function(s) { return {x: moment(s.timestamp).toDate(), y: s.battery}; }),
                    pointRadius: 0
                };

                const pedometerDataset = {
                    label: i18n.translate('Pedometer'),
                    yAxisID: 'pedometer',
                    borderWidth: 2,
                    lineTension: 0,
                    borderColor: 'red',
                    backgroundColor: 'red',
                    fill: false,
                    data: history.map(function(s) { return {x: moment(s.timestamp).toDate(), y: s.pedometer - history[0].pedometer}; }),
                    pointRadius: 0
                };

                const tooltipTiles = ['Battery {}%', '{} steps'];

                chart = new Chart(ctx, {
                    type: 'line',
                    data: {
                        datasets: [
                            batteryDataset,
                            pedometerDataset
                        ]
                    },
                    options: {
                        aspectRatio: 1,
                        scales: {
                            xAxes: [
                                {
                                    type: 'time',
                                    time: {
                                        displayFormats: TIME_AXIS_DISPLAY_FORMAT
                                    }
                                }
                            ],
                            yAxes: [
                                {
                                    id: 'battery',
                                    type: 'linear',
                                    position: 'left',
                                },
                                {
                                    id: 'pedometer',
                                    type: 'linear',
                                    position: 'right',
                                }
                            ]
                        },
                        tooltips: {
                            callbacks: {
                                label: function(tooltipItem, data) {
                                    return i18n.format(tooltipTiles[tooltipItem.datasetIndex], [data.datasets[tooltipItem.datasetIndex].data[tooltipItem.index].y]);
                                },
                                title: function(tooltipItem, data) {
                                    return moment(data.datasets[tooltipItem[0].datasetIndex].data[tooltipItem[0].index].x).format(TOOLTIP_TIME_FORMAT);
                                }
                            }
                        }
                    }
                });
                $chart.show();
            } else {
                await showWarning(i18n.format('No data'));
            }
        }
    }

    async function getRange() {

        const start = moment($('#history-start').val(), RANGE_PICKER_DATETIME_FORMAT).toDate();
        const end = moment($('#history-end').val(), RANGE_PICKER_DATETIME_FORMAT).toDate();

        if (end.getTime() < start.getTime()) {
            await showError(i18n.format('Time interval end {} is selected before time interval start {}', [
                moment(end).format(ERROR_MESSAGE_TIME_FORMAT),
                moment(start).format(ERROR_MESSAGE_TIME_FORMAT)]));
            return null;
        }

        return {start: start, end: end};
    }

    $('#history-draw').off('click');
    $('#history-draw').on('click', function () {
        drawHistoryData();
    });

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $show.click(async function onShow() {
                $show.off('click', onShow);
                destroyChart();
                $chart.hide();
                $modal.modal('hide');
                resolve(await getRange());
            });
            $close.click(function onCancel() {
                $close.off('click', onCancel);
                destroyChart();
                $chart.hide();
                $modal.modal('hide');
                resolve(null);
            });
        });

        $modal.modal({
	        backdrop: 'static',
	        focus: true,
	        keyboard: false,
	        show: true
        });
    });
}

module.exports = {initHistory, showHistory};