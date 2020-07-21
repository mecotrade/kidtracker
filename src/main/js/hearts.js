'use strict';

const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const $modal = $('#hearts');
const $reset = $('#hearts-reset');
const $update = $('#hearts-update');
const $close = $('#hearts-close');
const $input = $('#hearts-input');

function initHearts() {
    $input.inputSpinner();
}

async function showHearts(deviceId) {

    $reset.off('click');
    $reset.click(() => {
        $input.val(0);
    });

    const config = await fetchWithRedirect(`/api/device/${deviceId}/config`);
    if (config) {
        initConfig($input, $update, 'FLOWER', config, deviceId, {
            defaultValue: () => 0,
            value: () => $input.val()
        });
    }

    return new Promise(resolve => {

        function hide() {

            $close.off('click');

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', async function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $close.click(() => {
                hide();
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

module.exports = {initHearts, showHearts};