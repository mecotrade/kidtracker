'use strict';

const i18n = require('./i18n.js');

function initPhone() {

    const $text = $('#sms-input');
    const $sms = $('#phone-sms');

    $text.on('keyup', function () {
        $sms.attr('disabled', $text.val() == '');
    });

    i18n.applyAll([
        $('#phone-title'),
        $('#phone-input-label'),
        $('#sms-input-label'),
        $('#phone-info')
    ]);
}

async function showPhone(user, deviceId) {

    const $modal = $('#phone');
    const $phone = $('#phone-input');
    const $text = $('#sms-input');
    const $monitor = $('#phone-monitor');
    const $call = $('#phone-call');
    const $sms = $('#phone-sms');
    const $close = $('#phone-close');

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $phone.val(user.phone);
            $monitor.click(async function onMonitor() {
                const phone = $phone.val();
                const response = await fetch(`/api/device/${deviceId}/command`, {
                  method: 'POST',
                  headers: {'Content-Type': 'application/json'},
                  body: JSON.stringify({type: 'MONITOR', payload: [phone]})
                });
                if (response.ok) {
                    $monitor.off('click', onMonitor);
                    $modal.modal('hide');
                    resolve(null);
                } else {
                    showError(i18n.translate('Command is not completed.'))
                }
            })
            $call.click(async function onCall() {
                const phone = $phone.val();
                const response = await fetch(`/api/device/${deviceId}/command`, {
                  method: 'POST',
                  headers: {'Content-Type': 'application/json'},
                  body: JSON.stringify({type: 'CALL', payload: [phone]})
                });
                if (response.ok) {
                    $call.off('click', onCall);
                    $modal.modal('hide');
                    resolve(null);
                } else {
                    showError(i18n.translate('Command is not completed.'))
                }
            })
            $sms.click(async function onSms() {
                const text = $text.val();
                const phone = $phone.val();
                const response = await fetch(`/api/device/${deviceId}/command`, {
                  method: 'POST',
                  headers: {'Content-Type': 'application/json'},
                  body: JSON.stringify({type: 'SMS', payload: [phone, text]})
                });
                if (response.ok) {
                    $sms.off('click', onSms);
                    $modal.modal('hide');
                    resolve(null);
                } else {
                    showError(i18n.translate('Command is not completed.'))
                }
            })
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

module.exports = {initPhone, showPhone};