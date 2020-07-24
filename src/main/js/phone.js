/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                before: () => {
                    if (!$phone.val()) {
                        return 'Phone should not be empty.';
                    }
                },
                payload: () => [$phone.val()],
                after: () => {
                    $monitor.off('click');
                    $modal.modal('hide');
                    resolve(null);
                }
            });
            initCommand($call, 'CALL', deviceId, {
                before: () => {
                    if (!$phone.val()) {
                        return 'Phone should not be empty.';
                    }
                },
                payload: () => [$phone.val()],
                after: () => {
                    $call.off('click');
                    $modal.modal('hide');
                    resolve(null);
                }
            })
            initCommand($sms, 'SMS', deviceId, {
                before: () => {
                    if (!$phone.val()) {
                        return 'Phone should not be empty.';
                    } else if (!$text.val()) {
                        return 'Text message should not be empty.';
                    }
                },
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