'use strict';

const i18n = require('./i18n.js');
const moment = require('moment/min/moment-with-locales.min.js');
const {initNotification, showWarning, showError} = require('./notification.js');
const {initCommand, initConfig, initCheck} = require('./util.js');
require('bootstrap-input-spinner/src/bootstrap-input-spinner.js');

const WATCH_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
const WATCH_TIME_FORMAT = 'HH:mm';
const WATCH_COMMAND_TIME_FORMAT = 'HH.mm.ss';
const WATCH_COMMAND_DATE_FORMAT = 'YYYY.MM.DD';

const $modal = $('#show-kid-settings');

function initWatchSettings() {

    $('#kid-settings-uploadinterval-input').inputSpinner();
    $('#kid-settings-worktime-input').inputSpinner();

    $('#kid-settings-datetime').datetimepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().format(WATCH_DATETIME_FORMAT)
    });

    i18n.applyAll([
        $('#kid-settings-title'),
        $('#kid-settings-uploadinterval-input-label'),
        $('#kid-settings-worktime-input-label'),
        $('#kid-settings-datetime-label'),
        $('#kid-settings-voicemsg-label'),
        $('#kid-settings-sms-label'),
        $('#kid-settings-pedometer-label'),
        $('#kid-settings-bt-label'),
        $('#kid-settings-makefriend-label'),
        $('#kid-settings-btname-input-label'),
        $('#kid-settings-bigtime-label'),
        $('#kid-settings-contacts-label'),
        $('#kid-settings-tz-input-label'),
        $('#kid-settings-lang-label'),
        $('#input-token-input-label'),
        $('#input-token-title')
    ]);

    $('#kid-settings-lang-select option').each(function (i) {
        i18n.apply($(this));
    });

    $('#kid-settings-tz-select option').each(function (i)  {
        i18n.apply($(this));
    });

    $('#show-kid-settings div.card').each(function (i)  {
        const $header = $('div.card-header', $(this));
        i18n.apply($('span', $header));
        $('div.form-group > span', $(this)).each(function (i) {
            i18n.apply($(this));
        });
        $header.off('click');
        $header.click(() => {
            $('div.card-body', $(this)).toggle();
            $('ul.list-group.list-group-flush', $(this)).toggle();
        });
    });

    initReminder('kid-settings-reminder-1');
    initReminder('kid-settings-reminder-2');
    initReminder('kid-settings-reminder-3');

    $('#input-token-input').off('keydown');
    $('#input-token-input').on('keydown', function(e) {
        if (e.keyCode == 13) {
            $('#input-token-execute')[0].dispatchEvent(new Event('click'));
            e.preventDefault();
        }
    });
}

function initReminder(prefix) {

    const $typeSelect = $(`#${prefix}-select`);
    const $daysRow = $(`#${prefix}-days`);

    $(`#${prefix}-time`).timepicker({
        locale: 'ru-ru',
        format: 'HH:MM',
        mode: '24hr',
        footer: true,
        modal: true,
        uiLibrary: 'bootstrap4',
        value: moment().format(WATCH_TIME_FORMAT)
    });

    i18n.applyAll([
        $(`#${prefix}-time-label`),
        $(`#${prefix}-on-label`)
    ]);

    $typeSelect.off('change');
    $typeSelect.on('change', () => {
        if ($typeSelect.val() == 'choice') {
            $daysRow.show();
        } else {
            $daysRow.hide();
        }
    });

    $('option', $typeSelect).each(function (i) {
        i18n.apply($(this));
    });

    $('option', $daysRow).each(function(i) {
        i18n.apply($(this));
    });

    $('select', $daysRow).multiSelect({
        selectableHeader: i18n.translate('Available'),
        selectionHeader: i18n.translate('Selected'),
        cssClass: 'reminder-multiselect-container'
    });
}

function reminderValue(prefix) {

    const time = $(`#${prefix}-time`).val();
    const active = $(`#${prefix}-on`)[0].checked ? '1' : '0';
    let type = $(`#${prefix}-select`).children('option:selected').val();
    if (type == 'choice') {
        type = '0000000';
        $(`#${prefix}-days select`).val().forEach(i => {
            const index = parseInt(i);
            type = type.substring(0, index) + '1' + type.substring(index + 1);
        })
    }

    return `${time}-${active}-${type}`;
}

function reminderConfig(prefix, value) {

    const [time, active, type] = value.split('-');
    $(`#${prefix}-time`).val(time);
    $(`#${prefix}-on`)[0].checked = active == '1';
    if (type == '1' || type == '2') {
        $(`#${prefix}-select`).val(type);
    } else {
        $(`#${prefix}-select`).val('choice');
        const value = [];
        for(let i=0; i < type.length; i++) {
            if (type[i] === '1') value.push(i.toString());
        }
        $(`#${prefix}-days select`).multiSelect('select', value);
    }

    $(`#${prefix}-select`)[0].dispatchEvent(new Event('change'));
}

async function showWatchSettings(deviceId) {

    const configResponse = await fetch(`/api/device/${deviceId}/config`);
    const config = await configResponse.json();

    initConfig($('#kid-settings-uploadinterval-input'), $('#kid-settings-uploadinterval'), 'UPLOAD', config, deviceId, {
        defaultValue: () => 60,
        value: () => $('#kid-settings-uploadinterval-input').val()
    });

    initConfig($('#kid-settings-worktime-input'), $('#kid-settings-worktime'), 'WORKTIME', config, deviceId, {
        defaultValue: () => 3,
        value: () => $('#kid-settings-worktime-input').val()
    });

    initCommand($('#kid-settings-timecustom'), 'TIME', deviceId, {
        init: () => {
            $('#kid-settings-datetime').val(moment().format(WATCH_DATETIME_FORMAT));
        },
        payload: () => {
            const datetime = $('#kid-settings-datetime').val();
            return [
                moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_TIME_FORMAT),
                'DATE',
                moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_DATE_FORMAT)
            ];
        }
    });

    initCommand($('#kid-settings-timeserver'), 'TIMECALI', deviceId, {after: () => $('#kid-settings-datetime').val(moment().format(WATCH_DATETIME_FORMAT))});

    initCheck($('#kid-settings-voicemsg'), 'TKONOFF', config, deviceId);
    initCheck($('#kid-settings-sms'), 'SMSONOFF', config, deviceId);
    initCheck($('#kid-settings-pedometer'), 'PEDO', config, deviceId);
    initCheck($('#kid-settings-bt'), 'BT', config, deviceId);
    initCheck($('#kid-settings-makefriend'), 'MAKEFRIEND', config, deviceId);

    initConfig($('#kid-settings-btname-input'), $('#kid-settings-btname'), 'BTNAME', config, deviceId);

    initCheck($('#kid-settings-bigtime'), 'BIGTIME', config, deviceId);
    initCheck($('#kid-settings-contacts'), 'PHBONOFF', config, deviceId);

    initConfig(null, $('#kid-settings-langtz'), 'LZ', config, deviceId, {
        init: function() {
            let done = false;
            config.filter(c => c.parameter == 'LZ').forEach(function(c) {
                const [lang, tz] = c.value.split(',');
                $('#kid-settings-tz-select').val(tz);
                $('#kid-settings-lang-select').val(lang);
                done = true;
            });

            if (!done) {
                $('#kid-settings-tz-select').val(-Number(Math.round(((new Date()).getTimezoneOffset() / 60) + "e2") + "e-2"));
            }
        },
        value: function() {
            const lang = $('#kid-settings-lang-select').children('option:selected').val();
            const tz = $('#kid-settings-tz-select').children('option:selected').val();
            return `${lang},${tz}`;
        }
    });

    initConfig(null, $('#kid-settings-reminder'), 'REMIND', config, deviceId, {
        init: () => {
            config.filter(c => c.parameter == 'REMIND').forEach(function (c) {
                const [reminder1, reminder2, reminder3] = c.value.split(',');
                reminderConfig('kid-settings-reminder-1', reminder1);
                reminderConfig('kid-settings-reminder-2', reminder2);
                reminderConfig('kid-settings-reminder-3', reminder3);
            });
        },
        value: () => {
            return `${reminderValue('kid-settings-reminder-1')},${reminderValue('kid-settings-reminder-2')},${reminderValue('kid-settings-reminder-3')}`;
        }
    });

    async function showInputToken() {

        const $modalToken = $('#input-token');

        const $closeToken = $('#input-token-close');
        const $executeToken = $('#input-token-execute');

        function hide() {
            $closeToken.off('click');
            $executeToken.off('click');
            $modalToken.modal('hide');
        }

        return new Promise(resolve => {

            $modalToken.on('shown.bs.modal', function onShow() {
                $modalToken.off('shown.bs.modal', onShow);
                $closeToken.click(function () {
                    hide();
                    resolve(null);
                });
                $executeToken.click(async function () {
                    const token = $('#input-token-input').val();
                    const response = await fetch(`/api/device/${deviceId}/execute/${token}`);
                    if (!response.ok) {
                        showError(i18n.translate('Command is not completed.'))
                    }
                    hide();
                    resolve(null);
                });
            });

            $modalToken.modal({
                backdrop: 'static',
                focus: true,
                keyboard: false,
                show: true
            });
        });
    }

    initCommand($('#kid-settings-restart'), 'RESET', deviceId);
    initCommand($('#kid-settings-factory'), 'FACTORY', deviceId, {after: async () => await showInputToken()});
    initCommand($('#kid-settings-poweroff'), 'POWEROFF', deviceId, {after: async () => await showInputToken()});

    const $close = $('#kid-settings-close');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $close.click(function onClose() {
                $close.off('click', onClose);
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

module.exports = {initWatchSettings, showWatchSettings};