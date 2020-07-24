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
const {showWarning, showError} = require('./notification.js');

function showRegister() {

    const $modal = $('#create-user');

    const $create = $('#create-user-create')
    const $close = $('#create-user-close');

    const $username = $('#create-user-username');
    const $password = $('#create-user-password');
    const $repeat = $('#create-user-repeat');
    const $name = $('#create-user-name');
    const $phone = $('#create-user-phone');
    const $admin = $('#create-user-admin');

    function clear() {
        $username.val('');
        $password.val('');
        $repeat.val('');
        $name.val('');
        $admin[0].checked = false;
    }

    clear();

    return new Promise(resolve => {

        function hide() {

            clear();

            $create.off('click');
            $close.off('click');

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $create.click(async () => {
                if (!$password.val() || !$repeat.val() || $password.val() != $repeat.val()) {
                    showError(i18n.translate('Passwords do not match.'));
                } else if (!$username.val()) {
                    showError(i18n.translate('Username should not be empty.'));
                } else if (!$phone.val()) {
                    showError(i18n.translate('Phone should not be empty.'));
                } else {
                    const user = {
                        credentials: {
                            username: $username.val(),
                            password: $password.val()
                        },
                        name: $name.val(),
                        phone: $phone.val(),
                        admin: $admin[0].checked
                    };
                    await fetchWithRedirect('/api/admin/user', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(user)
                    }, message => {
                        $password.val('');
                        $repeat.val('');
                        showError(i18n.translate(message));
                    }, () => {
                        hide();
                    });
                }
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

module.exports = showRegister;