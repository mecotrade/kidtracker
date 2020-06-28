'use strict';

const i18n = require('./i18n.js');
const {initNotification, showWarning, showError} = require('./notification.js');

function initPhone() {

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
                $monitor.off('click', onMonitor);
                const phone = $phone.val();
                const response = await fetch(`/api/device/${deviceId}/monitor/${phone}`);
                $modal.modal('hide');
                resolve(null);
            })
            $call.click(async function onCall() {
                $call.off('click', onCall);
                const phone = $phone.val();
                const response = await fetch(`/api/device/${deviceId}/call/${phone}`);
                $modal.modal('hide');
                resolve(null);
            })
            $sms.click(async function onSms() {
                const text = $text.val();
                if (!text || text === '') {
                    await showError(i18n.translate('Message is empty.'));
                    return;
                } else {
                    $sms.off('click', onSms);
                    const phone = $phone.val();
                    const response = await fetch(`/api/device/${deviceId}/sms/${phone}/${text}`);
                    $modal.modal('hide');
                    resolve(null);
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