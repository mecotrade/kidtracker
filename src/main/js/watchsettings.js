'use strict';

const i18n = require('./i18n.js');
const moment = require('moment/min/moment-with-locales.min.js');
const {initNotification, showWarning, showError} = require('./notification.js');
require('bootstrap-input-spinner/src/bootstrap-input-spinner.js')

const WATCH_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
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

    $('#kid-settings-reminder-1-input').timepicker({
        uiLibrary: 'bootstrap4'
    });

    i18n.applyAll([
        $('#kid-settings-title'),
        $('#kid-settings-uploadinterval-input-label'),
        $('#kid-settings-worktime-input-label'),
        $('#kid-settings-datetime-label'),
        $('#kid-settings-removesms-label'),
        $('#kid-settings-lowbatsms-label'),
        $('#kid-settings-sossms-label'),
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
        $('#kid-settings-reminder-1-input-label'),
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
        $header.off('click');
        $header.click(() => {
            $('div.card-body', $(this)).toggle();
        });
    });
}

async function showWatchSettings(deviceId) {

    const configResponse = await fetch(`/api/device/${deviceId}/config`);
    const config = await configResponse.json();

    function initCommand($button, command, options) {
        $button.off('click');
        $button.click(async () => {
            if (options.before) {
                options.before();
            }
            const response = await fetch(`/api/device/${deviceId}/command`, {
              method: 'POST',
              headers: {'Content-Type': 'application/json'},
              body: JSON.stringify({type: command, payload: options.payload ? options.payload() : []})
            });
            if (response.ok) {
                if (options.after) {
                    options.after();
                }
            } else {
                showError(i18n.translate('Command is not completed.'));
                if (options.error) {
                    options.error();
                }
            }
        });
    }

    function initConfig($input, $button, parameter, options) {
        if (options.initValue) {
            $input.val(options.initValue());
        } else {
            let isSet = false;
            config.filter(c => c.parameter == parameter).forEach(c => {
                $input.val(c.value);
                isSet = true;
            });
            if (isSet == false && options.defaultValue) {
                $input.val(options.defaultValue());
            }
        }
        $button.off('click');
        $button.click(async () => {
            if (options.before) {
                options.before();
            }
            const response = await fetch(`/api/device/${deviceId}/config`, {
              method: 'POST',
              headers: {'Content-Type': 'application/json'},
              body: JSON.stringify({parameter: parameter, value: options.value ? options.value() : $input.val()})
            });
            if (response.ok) {
                if (options.after) {
                    options.after();
                }
            } else {
                showError(i18n.translate('Command is not completed.'));
                if (options.error) {
                    options.error();
                }
            }
        });
    }

    function initCheck($check, parameter) {
        config.filter(c => c.parameter == parameter).forEach(c => $check[0].checked = c.value == '1');
        $check.off('change');
        $check.click(async () => {
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

    initConfig($('#kid-settings-uploadinterval-input'), $('#kid-settings-uploadinterval'), 'UPLOAD', {
        defaultValue: () => 60,
        value: () => $('#kid-settings-uploadinterval-input').val()
    });

    initConfig($('#kid-settings-worktime-input'), $('#kid-settings-worktime'), 'WORKTIME', {
        defaultValue: () => 3,
        value: () => $('#kid-settings-worktime-input').val()
    });

    initConfig($('#kid-settings-datetime'), $('#kid-settings-timecustom'), 'TIME', {
        initValue: () => moment().format(WATCH_DATETIME_FORMAT),
        value: () => {
            const datetime = $('#kid-settings-datetime').val();
            return `${moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_TIME_FORMAT)},DATE,${moment(datetime, WATCH_DATETIME_FORMAT).format(WATCH_COMMAND_DATE_FORMAT)}`;
        }
    });

    initCommand($('#kid-settings-timeserver'), 'TIMECALI', {after: () => $('#kid-settings-datetime').val(moment().format(WATCH_DATETIME_FORMAT))});

    initCheck($('#kid-settings-removesms'), 'REMOVESMS');
    initCheck($('#kid-settings-lowbatsms'), 'LOWBAT');
    initCheck($('#kid-settings-sossms'), 'SOSSMS');
    initCheck($('#kid-settings-voicemsg'), 'TKONOFF');
    initCheck($('#kid-settings-sms'), 'SMSONOFF');
    initCheck($('#kid-settings-pedometer'), 'PEDO');
    initCheck($('#kid-settings-bt'), 'BT');
    initCheck($('#kid-settings-makefriend'), 'MAKEFRIEND');

    initConfig($('#kid-settings-btname-input'), $('#kid-settings-btname'), 'BTNAME', {});

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
        $('#kid-settings-tz-select').val(-Number(Math.round(((new Date()).getTimezoneOffset() / 60) + "e2") + "e-2"));
    }

    const $timezone = $('#kid-settings-tz');
    $timezone.off('click');
    $timezone.click(clickLangTz);

    const $lang = $('#kid-settings-lang');
    $lang.off('click');
    $lang.click(clickLangTz);

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

    initCommand($('#kid-settings-factory'), 'FACTORY', {after: async () => await showInputToken()});
    initCommand($('#kid-settings-poweroff'), 'POWEROFF', {after: async () => await showInputToken()});
    initCommand($('#kid-settings-restart'), 'RESET', {after: async () => await showInputToken()});

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