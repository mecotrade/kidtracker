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

const moment = require('moment/min/moment-with-locales.min.js');
const i18n = require('./i18n.js');
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const $modal = $('#show-user-devices');
const $editModal = $('#edit-device');

const DEVICE_STATUS_QUERY_INTERVAL = 60000;
const LAST_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';

var timerId = null;
var absoluteTime = {};

async function updateStatus() {
    const status = await fetchWithRedirect(`/api/user/kids/status`);
    status.forEach(s => {
        const $tr = $(`#kid-device-${s.deviceId}`).parent();
        $('div.user-device-name-deviceid', $tr).toggleClass('online', s.online).toggleClass('offline', !s.online);
        if (s.date) {
            const time = absoluteTime[s.deviceId] ? moment(s.date).format(LAST_MESSAGE_TIME_FORMAT) : moment(s.date).fromNow();
            $('div.user-device-name-time', $tr).text(time);
        }
    });
}

async function showDevice() {

    const kids = await fetchWithRedirect(`/api/user/kids/info`);

    const $modal = $('#show-user-devices');
    const $add = $('#user-devices-add');
    const $close = $('#user-devices-close');

    const $tbody = $('<tbody>');

    kids.forEach(k => {
        const $tr = $('<tr>');
        const $thThumb = $('<th>').addClass('user-device-thumb');
        const $thName = $('<th>').addClass('user-device-name');
        const $td = $('<td>').addClass('user-device-others');
        const $thumb = k.thumb ? $('<img>').attr('src', k.thumb).addClass('thumb-img') : $('<div>').addClass('thumb-placeholder');
        $thThumb.append($thumb).attr('id', `kid-device-${k.deviceId}`);
        const $divName = $('<div>').text(k.name);
        const $divDeviceId = $('<div>').text(k.deviceId).addClass('user-device-name-deviceid');
        const $divTime = $('<div>').addClass('user-device-name-time');
        $thName.append($divName).append($divDeviceId).append($divTime);
        const $spanName = $('<span>').addClass('user-device-other-user-name').text();
        k.users.forEach(u => {
            $td.append($('<div>').append($('<span>').addClass('user-device-other-user').append($('<b>').text(u.name)).append(` ${u.phone}`)));
        })
        $tr.append($thThumb).append($thName).append($td);
        $tbody.append($tr);
    });

    $('table.table', $modal).empty().append($tbody);

    kids.forEach(k => {
        const $thThumb = $(`#kid-device-${k.deviceId}`);
        $thThumb.off('click');
        $thThumb.on('click', async () => {
            await editDevice(k);
            await showDevice();
        });
        const $divTime = $('div.user-device-name-time', $thThumb.parent());
        $divTime.off('click');
        $divTime.click(() => {
            absoluteTime[k.deviceId] = !absoluteTime[k.deviceId];
            updateStatus();
        });
    });

    updateStatus();

    if (!timerId) {
        timerId = setInterval(updateStatus, DEVICE_STATUS_QUERY_INTERVAL);
    }

    return new Promise(resolve => {

        function hide() {

            $close.off('click');
            $add.off('click');

            clearInterval(timerId);
            timerId = null;

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $add.click(async () => {
                await editDevice();
                await showDevice();
            });
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

async function editDevice(kid) {

    const $remove = $('#edit-device-remove');
    const $removeThumb = $('#edit-device-remove-thumb');
    const $addThumb = $('#edit-device-add-thumb');
    const $upload = $('#edit-device-upload');
    const $add = $('#edit-device-add');
    const $close = $('#edit-device-close');

    const $thumbRow = $('.thumb-row', $editModal);
    const $deviceId = $('#device-deviceid');
    const $name = $('#device-name');

    const $info = $('div.alert-info', $editModal);

    const create = !kid;
    if (create) {
        kid = {};
        const serverConfig = await fetchWithRedirect('/api/user/config');
        $sms = $('<span>').addClass('user-add-device-sms').text(`pw,123456,ip,${window.location.host.split(':')[0]},${serverConfig.messagePort}#`);
        $password = $('<span>').addClass('user-add-device-sms').text('123456');
        $info.html(i18n.format('Send text message to the device {}If the device password was changed, put it instead of {}', ['<br>'+$sms[0].outerHTML+'<br>', $password[0].outerHTML]));
    }

    $info.toggle(create);

    $remove.toggle(create == false);
    $upload.toggle(create == false);
    $add.toggle(create == true);

    function render() {

        if (kid.thumb) {
            const $img = $('<img>').attr('src', kid.thumb).addClass('thumb-img-lg');
            $thumbRow.empty().append($img)
        } else {
            const $input = $('<input>').attr('type', 'file').prop('hidden', true);
            const $label = $('<label>').addClass('thumb-placeholder-lg').append($input);
            $thumbRow.empty().append($label);
        }
        $removeThumb.toggle(!!kid.thumb);
        $addThumb.toggle(!kid.thumb);
        $('label > input', $editModal).each(function (i) {
            $(this).off('change');
            if (!kid.thumb) {
                $(this).change(() => {
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        kid.thumb = e.target.result;
                        kid.name = $name.val();
                        kid.deviceId = $deviceId.val();
                        render();
                    };
                    reader.readAsDataURL($(this)[0].files[0]);
                });
            }
        })

        $deviceId.val(kid.deviceId);
        $name.val(kid.name);

        $deviceId.prop('disabled', create == false);
    }

    render();

    return new Promise(resolve => {

        function hide() {

            $remove.off('click');
            $removeThumb.off('click');
            $addThumb.off('click');
            $upload.off('click');
            $add.off('click');
            $close.off('click');

            $editModal.modal('hide');
            resolve(null);
        }

        $editModal.on('shown.bs.modal', async function onShow() {
            $editModal.off('shown.bs.modal', onShow);
            $remove.click(async () => {
                await fetchWithRedirect('/api/user/kid', {
                    method: 'DELETE',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(kid)
                },
                {
                    error: message => {
                        showError(i18n.translate(message || 'Command is not completed.'));
                    }
                });
                hide();
            });
            $removeThumb.click(async () => {
                delete kid.thumb;
                kid.name = $name.val();
                kid.deviceId = $deviceId.val();
                render();
            });
            $upload.click(async () => {
                if (!$name.val()) {
                    showError(i18n.translate('Name should not be empty.'))
                } else {
                    kid.name = $name.val();
                    await fetchWithRedirect('/api/user/kid', {
                        method: 'PUT',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(kid)
                    },
                    {
                        error: message => {
                            showError(i18n.translate(message || 'Command is not completed.'));
                        }
                    });
                    hide();
                }
            });
            $add.click(async () => {
                if (!$name.val()) {
                    showError(i18n.translate('Name should not be empty.'))
                } else if (!$deviceId.val()) {
                    showError(i18n.translate('Device identifier should not be empty.'))
                } else {
                    kid.deviceId = $deviceId.val();
                    kid.name = $name.val();
                    await fetchWithRedirect('/api/user/kid', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(kid)
                    },
                    {
                        error: message => {
                            showError(i18n.translate(message || 'Command is not completed.'));
                        }
                    });
                    hide();
                }
            });
            $close.click(() => {
                hide();
            });
        });

        $editModal.modal({
            backdrop: 'static',
            focus: true,
            keyboard: false,
            show: true
        });
    });
}

module.exports = showDevice;