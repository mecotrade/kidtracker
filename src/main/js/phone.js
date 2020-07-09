'use strict';

const i18n = require('./i18n.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

function initPhone() {

    const $text = $('#sms-input');
    const $sms = $('#phone-sms');

    $text.on('keyup', function () {
        $sms.attr('disabled', $text.val() == '');
    });

    i18n.apply($('#phone-info'));
}

async function showPhone(user, deviceId) {

    const $modal = $('#phone');
    const $phone = $('#phone-input');
    const $text = $('#sms-input');
    const $monitor = $('#phone-monitor');
    const $call = $('#phone-call');
    const $sms = $('#phone-sms');
    const $close = $('#phone-close');

    $phone.val(user.phone);

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            initCommand($monitor, 'MONITOR', deviceId, {
                payload: () => [$phone.val()],
                after: () => {
                    $monitor.off('click');
                    $modal.modal('hide');
                    resolve(null);
                }
            });
            initCommand($call, 'CALL', deviceId, {
                payload: () => [$phone.val()],
                after: () => {
                    $call.off('click');
                    $modal.modal('hide');
                    resolve(null);
                }
            })
            initCommand($sms, 'SMS', deviceId, {
                payload: () => [$phone.val(), $text.val()],
                after: () => {
                    $sms.off('click');
                    $modal.modal('hide');
                    resolve(null);
                }
            })
            $close.click(function onClose() {
                $monitor.off('click');
                $call.off('click');
                $sms.off('click');
                $close.off('click');
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

module.exports = {initPhone, showPhone};