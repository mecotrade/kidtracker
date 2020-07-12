'use strict';

const i18n = require('./i18n.js');
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const $modal = $('#show-user-devices');
const $editModal = $('#edit-device');

async function showDevice() {

    const kids = await fetchWithRedirect(`/api/user/kids/info`);

    const $modal = $('#show-user-devices');
    const $close = $('#user-devices-close');

    const $tbody = $('<tbody>');

    kids.forEach(k => {
        const $tr = $('<tr>');
        const $thThumb = $('<th>').addClass('user-device-thumb');
        const $thName = $('<th>').addClass('user-device-name');
        const $td = $('<td>').addClass('user-device-others');
        const $thumb = k.thumb ? $('<img>').attr('src', k.thumb).addClass('thumb-img') : $('<div>').addClass('thumb-placeholder');
        $thThumb.append($thumb);
        const $divName = $('<div>').text(k.name);
        const $divDeviceId = $('<div>').text(k.deviceId).addClass('user-device-name-deviceid')
        $thName.append($divName).append($divDeviceId);
        const $spanName = $('<span>').addClass('user-device-other-user-name').text();
        k.users.forEach(u => {
            $td.append($('<div>').append($('<span>').addClass('user-device-other-user').append($('<b>').text(u.name)).append(` ${u.phone}`)));
        })
        $tr.append($thThumb).append($thName).append($td);
        $tr.attr('id', `kid-device-${k.deviceId}`);
        $tbody.append($tr);
    });

    $('table.table', $modal).empty().append($tbody);

    kids.forEach(k => {
        const kidDeviceId = `kid-device-${k.deviceId}`
        $(`#${kidDeviceId}`).off('click');
        $(`#${kidDeviceId}`).on('click', async () => {
            await editDevice(k, false);
            await showDevice();
        });
    });

    return new Promise(resolve => {
        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
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

function editDevice(kid, create) {

    const $remove = $('#edit-device-remove');
    const $removeThumb = $('#edit-device-remove-thumb');
    const $addThumb = $('#edit-device-add-thumb');
    const $upload = $('#edit-device-upload');
    const $close = $('#edit-device-close');

    const $thumbRow = $('.thumb-row', $editModal);
    const $deviceid = $('#device-deviceid');
    const $name = $('#device-name');

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
                        render();
                    };
                    reader.readAsDataURL($(this)[0].files[0]);
                });
            }
        })

        $deviceid.val(kid.deviceId);
        $name.val(kid.name);

        $deviceid.prop('disabled', create == false);
    }

    render();

    return new Promise(resolve => {

        function hide() {

            $remove.off('click');
            $removeThumb.off('click');
            $addThumb.off('click');
            $upload.off('click');
            $close.off('click');

            $editModal.modal('hide');
            resolve(null);
        }

        $editModal.on('shown.bs.modal', async function onShow() {
            $editModal.off('shown.bs.modal', onShow);
            $remove.click(async () => {
                await fetchWithRedirect(`/api/user/kid/${kid.deviceId}`, {
                    method: 'DELETE'
                }, () => {
                    showError(i18n.translate('Command is not completed.'))
                });
                hide();
            });
            $removeThumb.click(async () => {
                delete kid.thumb;
                kid.name = $name.val();
                render();
            });
            $upload.click(async () => {
                if (!$name.val()) {
                    showError(i18n.translate('Name should not be empty.'))
                } else {
                    kid.name = $name.val();
                    await fetchWithRedirect('/api/user/kid', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(kid)
                    }, () => {
                        showError(i18n.translate('Command is not completed.'))
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