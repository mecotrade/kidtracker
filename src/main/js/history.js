/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

require('./jquery-ui-custom.js')

const moment = require('moment/min/moment-with-locales.min.js');
const i18n = require('./i18n.js');
require('chart.js/dist/Chart.min.js');
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');
const {showChat, addMessage} = require('./chat.js');

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

    const $chart = $('#history-chart');
    const $messages = $('#history-chat-messages');

    const $show = $('#history-path');
    const $close = $('#history-close');
    const $chat = $('#history-chat')
    const $draw = $('#history-draw')
    const $today = $('#history-today')

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
                $messages.hide();
            } else {
                await showWarning(i18n.format('No data'));
            }
        }
    }

    async function showChatHistory() {

        const range = await getRange();
        if (range) {
            $messages.html('');
            const messages = await fetchWithRedirect(`/api/device/${deviceId}/chat/${range.start.getTime()}/${range.end.getTime()}`);
            if (messages && messages.length) {
                $chart.hide();
                $messages.show();
                messages.forEach(async message => await addMessage(message, deviceId, $messages));
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

    $draw.off('click');
    $draw.on('click', () => {
        drawHistoryData();
    });

    $chat.off('click');
    $chat.click(() => {
        showChatHistory();
    });

    $today.off('click');
    $today.click(() => {
        $('#history-date').val(moment().format(RANGE_PICKER_DATE_FORMAT));
        $('#history-start').val(moment().startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#history-end').val(moment().endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
    });

    return new Promise(resolve => {

        function hide(range) {
            $close.off('click');
            $show.off('click');

            destroyChart();
            $chart.hide();
            $messages.hide();

            $modal.modal('hide');

            resolve(range);
        }

        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $show.click(async () => {
                hide(await getRange());
            });
            $close.click(() => {
                hide(null);
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