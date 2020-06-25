'use strict';

const moment = require('moment/min/moment-with-locales.min.js');
const i18n = require('./i18n.js');

const RANGE_PICKER_TIME_FORMAT = 'DD/MM/YYYY HH:mm';

function initRangePicker() {

    $('#range-start').datetimepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1 },
        value: moment().startOf('day').format(RANGE_PICKER_TIME_FORMAT)
    });

    $('#range-end').datetimepicker({
        locale: 'ru-ru',
        format: 'dd/mm/yyyy HH:MM',
        footer: true,
        modal: true,
        datepicker: { weekStartDay: 1 },
        value: moment().endOf('day').format(RANGE_PICKER_TIME_FORMAT)
    });

    i18n.applyAll([
        $('button.btn-primary', $('#range-picker')),
        $('button.btn-secondary', $('#range-picker')),
        $('#range-picker-title'),
        $('#range-start-label'),
        $('#range-end-label'),
    ]);
}

async function pickRange() {

    const $modal = $('#range-picker');
    const $show = $('button.btn-primary', $modal);
    const $cancel = $('button.btn-secondary', $modal);
    const $start = $('#range-start');
    const $end = $('#range-end');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $show.click(function onShow() {
                $show.off('click', onShow);
                $modal.modal('hide');
                let start = moment($start.val(), RANGE_PICKER_TIME_FORMAT).toDate();
                let end = moment($end.val(), RANGE_PICKER_TIME_FORMAT).toDate();
                if (isNaN(start.getTime())) {
                    start = moment().startOf('day').toDate();
                    $start.datetimepicker('setOptions', {value: moment(start).format(RANGE_PICKER_TIME_FORMAT)});
                }
                if (isNaN(end.getTime())) {
                    end = moment().endOf('day').toDate();
                    $end.datetimepicker('setOptions', {value: moment(end).format(RANGE_PICKER_TIME_FORMAT)});
                }
                resolve({start: start, end: end});
            });
            $cancel.click(function onCancel() {
                $cancel.off('click', onCancel);
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