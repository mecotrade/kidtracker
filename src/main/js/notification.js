'use strict';

const i18n = require('./i18n.js');

function initNotification() {
    i18n.applyAll([
        $('#warning-title'),
        $('#error-title')
    ]);
}

async function showWarning(warning) {

    const $modal = $('#show-warning');
    const $close = $('#warning-close');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $('div.alert', $modal).html(warning);
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

async function showError(error) {

    const $modal = $('#show-error');
    const $close = $('#error-close');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $('div.alert', $modal).html(error);
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

module.exports = {initNotification, showWarning, showError};