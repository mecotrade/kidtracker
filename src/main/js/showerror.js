'use strict';

const i18n = require('./i18n.js');

function initError() {
    i18n.applyAll([
        $('#error-title')
    ]);
}

async function showError(error) {

    const $modal = $('#show-error');
    const $close = $('#error-close', $modal);

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $('div.alert', $modal).html(error);
            $close.click(function onJoin() {
                $close.off('click', onJoin);
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

module.exports = {initError, showError};