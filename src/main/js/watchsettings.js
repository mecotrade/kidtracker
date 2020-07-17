'use strict';

const i18n = require('./i18n.js');
const moment = require('moment/min/moment-with-locales.min.js');
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

require('bootstrap-input-spinner/src/bootstrap-input-spinner.js');

const WATCH_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
const WATCH_TIME_FORMAT = 'HH:mm';
const WATCH_COMMAND_TIME_FORMAT = 'HH.mm.ss';
const WATCH_COMMAND_DATE_FORMAT = 'YYYY.MM.DD';

const WATCH_LOCALES = {
    'en': 0,
    'cn': 1,
    'pt': 3,
    'es': 4,
    'de': 5,
    'tr': 7,
    'vi': 8,
    'ru': 9,
    'fr': 10
};

const $modal = $('#show-kid-settings');

function initWatchSettings() {

    $('#kid-settings-uploadinterval-input').inputSpinner();
    $('#kid-settings-worktime-input').inputSpinner();

    $('#kid-settings-datetime').datetimepicker({
        locale: i18n.locale.toLowerCase(),
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().format(WATCH_DATETIME_FORMAT)
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

    initReminder($('#kid-settings-reminder-1'));
    initReminder($('#kid-settings-reminder-2'));
    initReminder($('#kid-settings-reminder-3'));

    $('#input-token-input').off('keydown');
    $('#input-token-input').on('keydown', function(e) {
        if (e.keyCode == 13) {
            $('#input-token-execute')[0].dispatchEvent(new Event('click'));
            e.preventDefault();
        }
    });
}

function initReminder($reminder) {

    const $time = $('input', $reminder);
    const $type = $('select', $reminder);
    const $days = $('.day-select', $reminder);

    $('.bi-toggle-off, .bi-toggle-on', $reminder).each(function(i) {
        $(this).off('click');
        $(this).click(() => {
            $('.bi-toggle-off', $reminder).toggle();
            $('.bi-toggle-on', $reminder).toggle();
        })
    })

    $time.timepicker({
        locale: i18n.locale.toLowerCase(),
        format: 'HH:MM',
        mode: '24hr',
        footer: true,
        modal: true,
        uiLibrary: 'bootstrap4',
        value: moment().format(WATCH_TIME_FORMAT)
    });

    $type.off('change');
    $type.on('change', () => {
        $days.toggle($type.val() == 'choice');
    });

    $days.children('span').each(function(i) {
        $(this).off('click');
        $(this).click(() => {
            $(this).toggleClass('btn-primary');
        });
    });
}

function reminderValue($reminder) {

    const $time = $('input', $reminder);
    const $type = $('select', $reminder);
    const $days = $('.day-select', $reminder);

    const time = $time.val();
    const active = $('.bi-toggle-on', $reminder).is(':visible') ? '1' : '0';
    let type = $type.children('option:selected').val();
    if (type == 'choice') {
        type = '0000000';
        $days.children('span').each(function(i) {
            if ($(this).hasClass('btn-primary')) {
                const index = (i + 1) % 7;
                type = type.substring(0, index) + '1' + type.substring(index + 1);
            }
        });
    }

    return `${time}-${active}-${type}`;
}

function reminderConfig($reminder, value) {

    const $time = $('input', $reminder);
    const $type = $('select', $reminder);
    const $days = $('.day-select', $reminder);

    const [time, active, type] = value ? value.split('-') : [moment().format('HH:mm'), '0', '1']
    $time.val(time);

    $('.bi-toggle-off', $reminder).toggle(active != '1');
    $('.bi-toggle-on', $reminder).toggle(active == '1');

    if (type == '1' || type == '2') {
        $type.val(type);
    } else {
        $type.val('choice');
        $days.children('span').each(function(i) {
            $(this).toggleClass('btn-primary', type[(i + 1) % 7] === '1');
        });
    }

    $type[0].dispatchEvent(new Event('change'));
}

async function showWatchSettings(deviceId) {

    const $close = $('#kid-settings-close');

    const config = await fetchWithRedirect(`/api/device/${deviceId}/config`);
    if (config) {

        initConfig($('#kid-settings-uploadinterval-input'), $('#kid-settings-uploadinterval'), 'UPLOAD', config, deviceId, {
            defaultValue: () => 60,
            value: () => $('#kid-settings-uploadinterval-input').val()
        });

        initConfig($('#kid-settings-worktime-input'), $('#kid-settings-worktime'), 'WORKTIME', config, deviceId, {
            defaultValue: () => 3,
            value: () => $('#kid-settings-worktime-input').val()
        });

        initConfig(null, [$('#kid-settings-profile-sound'), $('#kid-settings-profile-vibro')], 'PROFILE', config, deviceId, {
            init: () => {
                function set(value) {
                    $('#kid-settings-profile-sound')[0].checked = value == 1 || value == 2;
                    $('#kid-settings-profile-vibro')[0].checked = value == 1 || value == 3;
                }
                let done = false;
                config.filter(c => c.parameter == 'PROFILE').forEach(c => {
                    set(c.value);
                    done = true;
                });
                if (done == false) {
                    set(1);
                }
            },
            value: () => {
                return 4 - ($('#kid-settings-profile-sound')[0].checked ? 2 : 0) - ($('#kid-settings-profile-vibro')[0].checked ? 1 : 0);
            }
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
                    const lang = WATCH_LOCALES[i18n.lang] || 0;
                    $('#kid-settings-lang-select').val(lang);
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
                let done = false;
                config.filter(c => c.parameter == 'REMIND').forEach(function (c) {
                    const [reminder1, reminder2, reminder3] = c.value.split(',');
                    reminderConfig($('#kid-settings-reminder-1'), reminder1);
                    reminderConfig($('#kid-settings-reminder-2'), reminder2);
                    reminderConfig($('#kid-settings-reminder-3'), reminder3);
                    done = true;
                });
                if (done == false) {
                    reminderConfig($('#kid-settings-reminder-1'));
                    reminderConfig($('#kid-settings-reminder-2'));
                    reminderConfig($('#kid-settings-reminder-3'));
                }
            },
            value: () => {
                return `${reminderValue($('#kid-settings-reminder-1'))},${reminderValue($('#kid-settings-reminder-2'))},${reminderValue($('#kid-settings-reminder-3'))}`;
            }
        });

        initCommand($('#kid-settings-restart'), 'RESET', deviceId);
        initCommand($('#kid-settings-factory'), 'FACTORY', deviceId);
        initCommand($('#kid-settings-poweroff'), 'POWEROFF', deviceId);
    }

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