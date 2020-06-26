'use strict';

require('./jquery-ui-custom.js')

const moment = require('moment/min/moment-with-locales.min.js');
const i18n = require('./i18n.js');

const RANGE_PICKER_DATETIME_FORMAT = 'DD/MM/YYYY HH:mm';
const RANGE_PICKER_DATE_FORMAT = 'DD/MM/YYYY';

function initRangePicker() {

    $('#range-start').datetimepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().startOf('day').format(RANGE_PICKER_DATETIME_FORMAT)
    });

    $('#range-start-label').on('click', function () {
        $('#range-date').val(moment($('#range-start').val(), RANGE_PICKER_DATETIME_FORMAT).format(RANGE_PICKER_DATE_FORMAT));
        $('#range-end').val(moment($('#range-start').val(), RANGE_PICKER_DATETIME_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').hide();
        $('#range-picker-title').hide();
        $('#date-picker-input').show();
        $('#date-picker-title').show();
    });

    $('#range-end').datetimepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1},
        uiLibrary: 'bootstrap4',
        value: moment().endOf('day').format(RANGE_PICKER_DATETIME_FORMAT)
    });

    $('#range-end-label').on('click', function () {
        $('#range-date').val(moment($('#range-end').val(), RANGE_PICKER_DATETIME_FORMAT).format(RANGE_PICKER_DATE_FORMAT));
        $('#range-start').val(moment($('#range-end').val(), RANGE_PICKER_DATETIME_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').hide();
        $('#range-picker-title').hide();
        $('#date-picker-input').show();
        $('#date-picker-title').show();
    });

    $('#range-date').datepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy',
        footer: true,
        modal: true,
        weekStartDay: 1,
        uiLibrary: 'bootstrap4',
        value: moment().format(RANGE_PICKER_DATE_FORMAT)
    });

    $('#range-date').on('change', function () {
        $('#range-start').val(moment($('#range-date').val(), RANGE_PICKER_DATE_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-end').val(moment($('#range-date').val(), RANGE_PICKER_DATE_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
    });

    $('#range-date-label').on('click', function () {
        $('#range-start').val(moment($('#range-date').val(), RANGE_PICKER_DATE_FORMAT).startOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-end').val(moment($('#range-date').val(), RANGE_PICKER_DATE_FORMAT).endOf('day').format(RANGE_PICKER_DATETIME_FORMAT));
        $('#range-picker-input').show();
        $('#range-picker-title').show();
        $('#date-picker-input').hide();
        $('#date-picker-title').hide();
    });

    i18n.applyAll([
        $('#history-title'),
        $('#range-start-label'),
        $('#range-end-label'),
        $('#range-date-label'),
    ]);
}

async function pickRange() {

    const $modal = $('#range-picker');
    const $show = $('#history-path');
    const $close = $('#history-close');
    const $start = $('#range-start');
    const $end = $('#range-end');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $show.click(function onShow() {
                $show.off('click', onShow);
                $modal.modal('hide');
                let start = moment($start.val(), RANGE_PICKER_DATETIME_FORMAT).toDate();
                let end = moment($end.val(), RANGE_PICKER_DATETIME_FORMAT).toDate();
                if (isNaN(start.getTime())) {
                    start = moment().startOf('day').toDate();
                    $start.datetimepicker('setOptions', {value: moment(start).format(RANGE_PICKER_DATETIME_FORMAT)});
                }
                if (isNaN(end.getTime())) {
                    end = moment().endOf('day').toDate();
                    $end.datetimepicker('setOptions', {value: moment(end).format(RANGE_PICKER_DATETIME_FORMAT)});
                }
                resolve({start: start, end: end});
            });
            $close.click(function onCancel() {
                $close.off('click', onCancel);
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

module.exports = {initRangePicker, pickRange};