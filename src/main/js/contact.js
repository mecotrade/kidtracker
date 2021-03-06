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
const {showWarning, showError} = require('./notification.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const TABS = {
    'ADMIN': {
        info: "Admin's phones, primary {} and secondary {}, which can manage kid's watch configuration through text messages",
        icons: [
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-shield-check" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M5.443 1.991a60.17 60.17 0 0 0-2.725.802.454.454 0 0 0-.315.366C1.87 7.056 3.1 9.9 4.567 11.773c.736.94 1.533 1.636 2.197 2.093.333.228.626.394.857.5.116.053.21.089.282.11A.73.73 0 0 0 8 14.5c.007-.001.038-.005.097-.023.072-.022.166-.058.282-.111.23-.106.525-.272.857-.5a10.197 10.197 0 0 0 2.197-2.093C12.9 9.9 14.13 7.056 13.597 3.159a.454.454 0 0 0-.315-.366c-.626-.2-1.682-.526-2.725-.802C9.491 1.71 8.51 1.5 8 1.5c-.51 0-1.49.21-2.557.491zm-.256-.966C6.23.749 7.337.5 8 .5c.662 0 1.77.249 2.813.525a61.09 61.09 0 0 1 2.772.815c.528.168.926.623 1.003 1.184.573 4.197-.756 7.307-2.367 9.365a11.191 11.191 0 0 1-2.418 2.3 6.942 6.942 0 0 1-1.007.586c-.27.124-.558.225-.796.225s-.526-.101-.796-.225a6.908 6.908 0 0 1-1.007-.586 11.192 11.192 0 0 1-2.417-2.3C2.167 10.331.839 7.221 1.412 3.024A1.454 1.454 0 0 1 2.415 1.84a61.11 61.11 0 0 1 2.772-.815z"/><path fill-rule="evenodd" d="M10.854 6.146a.5.5 0 0 1 0 .708l-3 3a.5.5 0 0 1-.708 0l-1.5-1.5a.5.5 0 1 1 .708-.708L7.5 8.793l2.646-2.647a.5.5 0 0 1 .708 0z"/></svg>',
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-shield" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M5.443 1.991a60.17 60.17 0 0 0-2.725.802.454.454 0 0 0-.315.366C1.87 7.056 3.1 9.9 4.567 11.773c.736.94 1.533 1.636 2.197 2.093.333.228.626.394.857.5.116.053.21.089.282.11A.73.73 0 0 0 8 14.5c.007-.001.038-.005.097-.023.072-.022.166-.058.282-.111.23-.106.525-.272.857-.5a10.197 10.197 0 0 0 2.197-2.093C12.9 9.9 14.13 7.056 13.597 3.159a.454.454 0 0 0-.315-.366c-.626-.2-1.682-.526-2.725-.802C9.491 1.71 8.51 1.5 8 1.5c-.51 0-1.49.21-2.557.491zm-.256-.966C6.23.749 7.337.5 8 .5c.662 0 1.77.249 2.813.525a61.09 61.09 0 0 1 2.772.815c.528.168.926.623 1.003 1.184.573 4.197-.756 7.307-2.367 9.365a11.191 11.191 0 0 1-2.418 2.3 6.942 6.942 0 0 1-1.007.586c-.27.124-.558.225-.796.225s-.526-.101-.796-.225a6.908 6.908 0 0 1-1.007-.586 11.192 11.192 0 0 1-2.417-2.3C2.167 10.331.839 7.221 1.412 3.024A1.454 1.454 0 0 1 2.415 1.84a61.11 61.11 0 0 1 2.772-.815z"/></svg>'
        ],
        button: $('#contacts-admin')
    },
    'SOS': {
        info: 'Phones where notification and alert text messages are being sent to',
        icons: [
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-exclamation-octagon" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4.54.146A.5.5 0 0 1 4.893 0h6.214a.5.5 0 0 1 .353.146l4.394 4.394a.5.5 0 0 1 .146.353v6.214a.5.5 0 0 1-.146.353l-4.394 4.394a.5.5 0 0 1-.353.146H4.893a.5.5 0 0 1-.353-.146L.146 11.46A.5.5 0 0 1 0 11.107V4.893a.5.5 0 0 1 .146-.353L4.54.146zM5.1 1L1 5.1v5.8L5.1 15h5.8l4.1-4.1V5.1L10.9 1H5.1z"/><path d="M7.002 11a1 1 0 1 1 2 0 1 1 0 0 1-2 0zM7.1 4.995a.905.905 0 1 1 1.8 0l-.35 3.507a.552.552 0 0 1-1.1 0L7.1 4.995z"/></svg>'
        ],
        button: $('#contacts-sos')
    },
    'PHONEBOOK': {
        info: "Kid's watch phone book, first two phones, {} and {}, can be called by long press of watch numbered buttons",
        icons: [
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-smartwatch" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M14 5h.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5H14V5z"/><path fill-rule="evenodd" d="M8.5 4.5A.5.5 0 0 1 9 5v3.5a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h2V5a.5.5 0 0 1 .5-.5z"/><path fill-rule="evenodd" d="M4.5 2h7A2.5 2.5 0 0 1 14 4.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 2 11.5v-7A2.5 2.5 0 0 1 4.5 2zm0 1A1.5 1.5 0 0 0 3 4.5v7A1.5 1.5 0 0 0 4.5 13h7a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 11.5 3h-7z"/><path d="M4 2.05v-.383C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v.383a2.512 2.512 0 0 0-.5-.05h-7c-.171 0-.338.017-.5.05zm0 11.9c.162.033.329.05.5.05h7c.171 0 .338-.017.5-.05v.383c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333v-.383z"/></svg>#1',
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-smartwatch" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M14 5h.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5H14V5z"/><path fill-rule="evenodd" d="M8.5 4.5A.5.5 0 0 1 9 5v3.5a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h2V5a.5.5 0 0 1 .5-.5z"/><path fill-rule="evenodd" d="M4.5 2h7A2.5 2.5 0 0 1 14 4.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 2 11.5v-7A2.5 2.5 0 0 1 4.5 2zm0 1A1.5 1.5 0 0 0 3 4.5v7A1.5 1.5 0 0 0 4.5 13h7a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 11.5 3h-7z"/><path d="M4 2.05v-.383C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v.383a2.512 2.512 0 0 0-.5-.05h-7c-.171 0-.338.017-.5.05zm0 11.9c.162.033.329.05.5.05h7c.171 0 .338-.017.5-.05v.383c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333v-.383z"/></svg>#2',
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-person-circle" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M13.468 12.37C12.758 11.226 11.195 10 8 10s-4.757 1.225-5.468 2.37A6.987 6.987 0 0 0 8 15a6.987 6.987 0 0 0 5.468-2.63z"></path><path fill-rule="evenodd" d="M8 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"></path><path fill-rule="evenodd" d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zM0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8z"></path></svg>'
        ],
        button: $('#contacts-phonebook')
    },
    'WHITELIST': {
        info: "Phones which incoming call will be accepted from by the kid's watch",
        icons: [
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-telephone-inbound" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3.925 1.745a.636.636 0 0 0-.951-.059l-.97.97c-.453.453-.62 1.095-.421 1.658A16.47 16.47 0 0 0 5.49 10.51a16.47 16.47 0 0 0 6.196 3.907c.563.198 1.205.032 1.658-.421l.97-.97a.636.636 0 0 0-.06-.951l-2.162-1.682a.636.636 0 0 0-.544-.115l-2.052.513a1.636 1.636 0 0 1-1.554-.43L5.64 8.058a1.636 1.636 0 0 1-.43-1.554l.513-2.052a.636.636 0 0 0-.115-.544L3.925 1.745zM2.267.98a1.636 1.636 0 0 1 2.448.153l1.681 2.162c.309.396.418.913.296 1.4l-.513 2.053a.636.636 0 0 0 .167.604L8.65 9.654a.636.636 0 0 0 .604.167l2.052-.513a1.636 1.636 0 0 1 1.401.296l2.162 1.681c.777.604.849 1.753.153 2.448l-.97.97c-.693.693-1.73.998-2.697.658a17.471 17.471 0 0 1-6.571-4.144A17.47 17.47 0 0 1 .639 4.646c-.34-.967-.035-2.004.658-2.698l.97-.969zM15.854.146a.5.5 0 0 1 0 .708L11.707 5H14.5a.5.5 0 0 1 0 1h-4a.5.5 0 0 1-.5-.5v-4a.5.5 0 0 1 1 0v2.793L15.146.146a.5.5 0 0 1 .708 0z"/></svg>'
        ],
        button: $('#contacts-whitelist')
    },
    'BUTTON': {
        info: 'Phone numbers which can be quick called with buttons {} and {}',
        icons: [
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-smartwatch" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M14 5h.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5H14V5z"/><path fill-rule="evenodd" d="M8.5 4.5A.5.5 0 0 1 9 5v3.5a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h2V5a.5.5 0 0 1 .5-.5z"/><path fill-rule="evenodd" d="M4.5 2h7A2.5 2.5 0 0 1 14 4.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 2 11.5v-7A2.5 2.5 0 0 1 4.5 2zm0 1A1.5 1.5 0 0 0 3 4.5v7A1.5 1.5 0 0 0 4.5 13h7a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 11.5 3h-7z"/><path d="M4 2.05v-.383C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v.383a2.512 2.512 0 0 0-.5-.05h-7c-.171 0-.338.017-.5.05zm0 11.9c.162.033.329.05.5.05h7c.171 0 .338-.017.5-.05v.383c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333v-.383z"/></svg>#1',
            '<svg width="20px" height="20px" viewBox="0 0 16 16" class="bi bi-smartwatch" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M14 5h.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5H14V5z"/><path fill-rule="evenodd" d="M8.5 4.5A.5.5 0 0 1 9 5v3.5a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h2V5a.5.5 0 0 1 .5-.5z"/><path fill-rule="evenodd" d="M4.5 2h7A2.5 2.5 0 0 1 14 4.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 2 11.5v-7A2.5 2.5 0 0 1 4.5 2zm0 1A1.5 1.5 0 0 0 3 4.5v7A1.5 1.5 0 0 0 4.5 13h7a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 11.5 3h-7z"/><path d="M4 2.05v-.383C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v.383a2.512 2.512 0 0 0-.5-.05h-7c-.171 0-.338.017-.5.05zm0 11.9c.162.033.329.05.5.05h7c.171 0 .338-.017.5-.05v.383c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333v-.383z"/></svg>#2'
        ],
        button: $('#contacts-button')
    }
};

const $modal = $('#contacts');
const $editModal = $('#edit-contact');

var capacity = {};
var contacts = {};

var tab;
var full;

function initContact() {

    // TODO: model dependent
    capacity = {
        'ADMIN': 2,
        'SOS': 3,
        'PHONEBOOK': 10,
        'WHITELIST': 10,
        'BUTTON': 2
    };

    tab = 'ADMIN';
    full = false;
}

async function showTab(deviceId) {

    const tabData = TABS[tab];

    $('div.alert', $modal).html(i18n.format(tabData.info, tabData.icons));
    $('#contacts-alert').toggle(tab == 'SOS');
    $('#contacts-phonebook-activate').toggle(tab == 'PHONEBOOK');
    Object.keys(TABS).forEach(k => {
        TABS[k].button.removeClass('btn-primary').addClass('btn-outline-primary')
    });
    tabData.button.addClass('btn-primary').removeClass('btn-outline-primary');

    const data = await fetchWithRedirect(`/api/device/${deviceId}/contact/${tab}`);
    if (data) {
        contacts[tab] = [];
        data.forEach(c => contacts[c.type][c.index] = c);
    }

    const $tbody = $('<tbody>');

    if (full) {
        for (let i=0; i < capacity[tab]; i++) {
            const $tr = $('<tr>').attr('id', `${tab}_${i}`);
            const $th = $('<th>');
            const $td = $('<td>');
            const $icon = $('<span>');
            $icon.html(tabData.icons[i < tabData.icons.length ? i : tabData.icons.length  - 1]);
            const contact = contacts[tab][i];
            if (contact) {
                $th.html(`${$icon[0].outerHTML} ${contact.name}`)
                $td.text(contact.phone);
            } else {
                $th.html($icon[0].outerHTML)
            }
            $tr.append($th).append($td);
            $tbody.append($tr);
        }
    } else {
        data.forEach(c => {
            const $tr = $('<tr>').attr('id', `${c.type}_${c.index}`);
            const $th = $('<th>');
            const $td = $('<td>');
            const $icon = $('<span>');
            $icon.html(tabData.icons[c.index < tabData.icons.length ? c.index : tabData.icons.length  - 1]);
            $th.html(`${$icon[0].outerHTML} ${c.name}`)
            $td.text(c.phone);
            $tr.append($th).append($td);
            $tbody.append($tr);
        });
    }

    $('table.table', $modal).empty().append($tbody);

    if (full) {
        for (let i=0; i < capacity[tab]; i++) {
            const contactId = `${tab}_${i}`
            $(`#${contactId}`).off('click');
            $(`#${contactId}`).on('click', async () => {
                const reload = await editContact(deviceId, contactId);
                if (reload) {
                    await showTab(deviceId, tab);
                }
            });
        }
    } else {
        data.forEach(c => {
            const contactId = `${c.type}_${c.index}`
            $(`#${contactId}`).off('click');
            $(`#${contactId}`).on('click', async function onIconClick() {
                const reload = await editContact(deviceId, contactId);
                if (reload) {
                    await showTab(deviceId, tab);
                }
            });
        });
    }
}

function editContact(deviceId, contactId) {

    const $remove = $('#edit-contact-remove');
    const $upload = $('#edit-contact-upload');
    const $close = $('#edit-contact-close');

    const $phone = $('#contact-phone');
    const $name = $('#contact-name');

    const [type, index] = contactId.split('_');
    const contact = contacts[type][index] || {type: type, index: index, name: '', phone: ''};
    $phone.val(contact.phone);
    $name.val(contact.name);

    return new Promise(resolve => {

        function hide(result) {

            $remove.off('click');
            $upload.off('click');
            $close.off('click');

            $editModal.modal('hide');
            resolve(result);
        }

        $editModal.on('shown.bs.modal', async function onShow() {
            $editModal.off('shown.bs.modal', onShow);
            $remove.click(async () => {
                const response = await fetchWithRedirect(`/api/device/${deviceId}/contact/${type}/${index}`, {
                    method: 'DELETE'
                },
                {
                    error: message => {
                        showError(i18n.translate(message || 'Command is not completed'));
                    },
                    block: true
                });
                hide(true);
            });
            $upload.click(async () => {
                if (!$name.val() && tab == PHONEBOOK) {
                    showError(i18n.translate('Name should not be empty.'))
                } else if (!$phone.val()) {
                    showError(i18n.translate('Phone should not be empty.'))
                } else {
                    await fetchWithRedirect(`/api/device/${deviceId}/contact`, {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({type: type, index: index, phone: $phone.val(), name: $name.val()})
                    },
                    {
                        error: message => {
                            showError(i18n.translate(message || 'Command is not completed'));
                        },
                        block: true
                    });
                    hide(true);
                }
            });
            $close.click(() => {
                hide(false);
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

async function showContact(deviceId) {

    const $update = $('#contacts-update');
    const $close = $('#contacts-close');

    Object.keys(TABS).forEach(k => {
        TABS[k].button.off('click');
        TABS[k].button.click(function () {
            tab = k;
            showTab(deviceId)
        });
    });

    $('#contacts-list').off('click');
    $('#contacts-list').click(function () {
        full = !full;
        $('#contacts-list').toggleClass('btn-info', full).toggleClass('btn-outline-info', !full);
        showTab(deviceId)
    });

    const config = await fetchWithRedirect(`/api/device/${deviceId}/config`);
    if (config) {
        initCheck($('#kid-settings-removesms'), 'REMOVESMS', config, deviceId);
        initCheck($('#kid-settings-lowbatsms'), 'LOWBAT', config, deviceId);
        initCheck($('#kid-settings-sossms'), 'SOSSMS', config, deviceId);
        initCheck($('#kid-settings-contacts'), 'PHBONOFF', config, deviceId);
    }

    $('#contacts-list').toggleClass('btn-info', full).toggleClass('btn-outline-info', !full);
    await showTab(deviceId);

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

module.exports = {initContact, showContact};