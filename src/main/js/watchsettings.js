'use strict';

const i18n = require('./i18n.js');
const moment = require('moment/min/moment-with-locales.min.js');
const {initNotification, showWarning, showError} = require('./notification.js');
require('bootstrap-input-spinner/src/bootstrap-input-spinner.js')

const WATCH_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
const WATCH_COMMAND_TIME_FORMAT = 'HH.mm.ss';
const WATCH_COMMAND_DATE_FORMAT = 'YYYY.MM.DD';

const MIN_UPLOAD_INTERVAL = 10;

const $modal = $('#show-kid-settings');

function initWatchSettings() {

    $('#kid-settings-uploadinterval-input').inputSpinner();

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
        $('#kid-settings-datetime-label'),
        $('#kid-settings-sossms-label'),
        $('#kid-settings-voicemsg-label'),
        $('#kid-settings-sms-label'),
        $('#kid-settings-pedometer-label'),
        $('#kid-settings-makefriend-label'),
        $('#kid-settings-bt-label'),
        $('#kid-settings-bigtime-label'),
        $('#kid-settings-contacts-label'),
        $('#kid-settings-tz-input-label'),
        $('#kid-settings-lang-label')
    ]);

    $('#kid-settings-lang-select option').each(function(index) {
        i18n.apply($(this));
    });

    $('#kid-settings-tz-select option').each(function(index) {
        i18n.apply($(this));
    });
}

async function showWatchSettings(deviceId) {

    const configResponse = await fetch(`/api/device/${deviceId}/config`);
    const config = await configResponse.json();

    const $uploadinterval = $('#kid-settings-uploadinterval');
    config.filter(c => c.parameter == 'UPLOAD').forEach(c => $('#kid-settings-uploadinterval-input').val(c.value));
    $uploadinterval.off('click');
    $uploadinterval.click(async function () {
        const value = Number.parseInt($('#kid-settings-uploadinterval-input').val());
        if (!isNaN(value) && value >= MIN_UPLOAD_INTERVAL) {
            const response = await fetch(`/api/device/${deviceId}/config`, {
              method: 'POST',
              headers: {'Content-Type': 'application/json'},
              body: JSON.stringify({parameter: 'UPLOAD', value: value.toString()})
            });
            if (!response.ok) {
                showError(i18n.translate('Command is not completed.'))
            }
        } else {
            showError(i18n.format('Upload interval should not be less than {}', [MIN_UPLOAD_INTERVAL]));
        }
    });

    const $timecustom = $('#kid-settings-timecustom');
    $timecustom.off('click');
    $timecustom.click(async function () {
        const datetime = $('#kid-settings-datetime').val();
        const response = await fetch(`/api/device/${deviceId}/command`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({type: 'TIME', payload: [
            moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_TIME_FORMAT),
            'DATE',
            moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_DATE_FORMAT)
          ]})
        });
        if (!response.ok) {
            showError(i18n.translate('Command is not completed.'))
        }
    })

    function initCommand($button, command, callback) {
        $button.off('click');
        $button.click(async function () {
            if (callback) {
                callback();
            }
            const response = await fetch(`/api/device/${deviceId}/command`, {
              method: 'POST',
              headers: {'Content-Type': 'application/json'},
              body: JSON.stringify({type: command, payload: []})
            });
            if (!response.ok) {
                showError(i18n.translate('Command is not completed.'))
            }
        });
    }

    initCommand($('#kid-settings-timeserver'), 'TIMECALI', function() {
        $('#kid-settings-datetime').val(moment().format(WATCH_DATETIME_FORMAT));
    });

//    const $timeserver = $('#kid-settings-timeserver');
//    $timeserver.off('click');
//    $timeserver.click(async function () {
//        $('#kid-settings-datetime').val(moment().format(WATCH_DATETIME_FORMAT));
//        const response = await fetch(`/api/device/${deviceId}/command`, {
//          method: 'POST',
//          headers: {'Content-Type': 'application/json'},
//          body: JSON.stringify({type: 'TIMECALI', payload: []})
//        });
//        if (!response.ok) {
//            showError(i18n.translate('Command is not completed.'))
//        }
//    });

    function initCheck($check, parameter) {
        config.filter(c => c.parameter == parameter).forEach(c => $check[0].checked = c.value == '1');
        $check.off('change');
        $check.click(async function () {
            const value = $check[0].checked == true ? '1' : '0';
            const response = await fetch(`/api/device/${deviceId}/config`, {
              method: 'POST',
              headers: {'Content-Type': 'application/json'},
              body: JSON.stringify({parameter: parameter, value: value})
            });
            if (!response.ok) {
                $check[0].checked = !$check[0].checked;
                showError(i18n.translate('Command is not completed.'))
            }
        });
    }

    initCheck($('#kid-settings-sossms'), 'SOSSMS');
    initCheck($('#kid-settings-voicemsg'), 'TKONOFF');
    initCheck($('#kid-settings-sms'), 'SMSONOFF');
    initCheck($('#kid-settings-pedometer'), 'PEDO');
    initCheck($('#kid-settings-makefriend'), 'MAKEFRIEND');
    initCheck($('#kid-settings-bt'), 'BT');
    initCheck($('#kid-settings-bigtime'), 'BIGTIME');
    initCheck($('#kid-settings-contacts'), 'PHBONOFF');

    async function clickLangTz() {
        const lang = $('#kid-settings-lang-select').children('option:selected').val();
        const tz = $('#kid-settings-tz-select').children('option:selected').val();
        const response = await fetch(`/api/device/${deviceId}/config`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({parameter: 'LZ', value: `${lang},${tz}`})
        });
        if (!response.ok) {
            showError(i18n.translate('Command is not completed.'))
        }
    }

    let tzSet = false;
    config.filter(c => c.parameter == 'LZ').forEach(function(c) {
        const [lang, tz] = c.value.split(',');
        $('#kid-settings-tz-select').val(tz);
        $('#kid-settings-lang-select').val(lang);
        tzSet = true;
    });

    if (!tzSet) {
        $('#kid-settings-tz-select').val(-Math.round((new Date()).getTimezoneOffset() / 60));
    }

    const $timezone = $('#kid-settings-tz');
    $timezone.off('click');
    $timezone.click(clickLangTz);

    const $lang = $('#kid-settings-lang');
    $lang.off('click');
    $lang.click(clickLangTz);

    initCommand($('#kid-settings-factory'), 'FACTORY');
    initCommand($('#kid-settings-poweroff'), 'POWEROFF');
    initCommand($('#kid-settings-restart'), 'RESET');

//    const $factory = $('#kid-settings-factory');
//    $factory.off('click');
//    $factory.click(async function () {
//        const response = await fetch(`/api/device/${deviceId}/command`, {
//          method: 'POST',
//          headers: {'Content-Type': 'application/json'},
//          body: JSON.stringify({type: 'FACTORY', payload: []})
//        });
//        if (!response.ok) {
//            showError(i18n.translate('Command is not completed.'))
//        }
//    });

//    const $poweroff = $('#kid-settings-poweroff');
//    $poweroff.off('click');
//    $poweroff.click(async function () {
//        const response = await fetch(`/api/device/${deviceId}/command`, {
//          method: 'POST',
//          headers: {'Content-Type': 'application/json'},
//          body: JSON.stringify({type: 'POWEROFF', payload: []})
//        });
//        if (!response.ok) {
//            showError(i18n.translate('Command is not completed.'))
//        }
//    });

//    const $restart = $('#kid-settings-restart');
//    $restart.off('click');
//    $restart.click(async function () {
//        const response = await fetch(`/api/device/${deviceId}/command`, {
//          method: 'POST',
//          headers: {'Content-Type': 'application/json'},
//          body: JSON.stringify({type: 'RESET', payload: []})
//        });
//        if (!response.ok) {
//            showError(i18n.translate('Command is not completed.'))
//        }
//    });


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