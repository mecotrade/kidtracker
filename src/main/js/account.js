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

async function showAccount() {

    const $modal = $('#user-profile');

    const $remove = $('#user-profile-remove');
    const $update = $('#user-profile-update');
    const $close = $('#user-profile-close');

    const $username = $('#user-profile-username');
    const $phone = $('#user-profile-phone');
    const $admin = $('#user-profile-admin');
    const $name = $('#user-profile-name');
    const $password = $('#user-profile-password');
    const $newPassword = $('#user-profile-new-password');

    function clear() {
        $password.val('');
        $newPassword.val('');
    }

    const user = await fetchWithRedirect(`/api/user/info`);
    if (user) {
        $username.val(user.credentials.username);
        $phone.val(user.phone);
        $admin[0].checked = user.admin;
        $name.val(user.name);
    } else {
        $username.val('');
        $phone.val('');
        $admin[0].checked = false;
        $name.val('');
    }
    clear();

    return new Promise(resolve => {

        function hide() {

            clear();

            $remove.off('click');
            $update.off('click');
            $close.off('click');

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', function onShow() {
            $modal.off('shown.bs.modal', onShow);
            $remove.click(async () => {
                user.credentials = {password: $password.val(), newPassword: $newPassword.val()};
                user.name = $name.val();
                if (!user.credentials.password) {
                    await showError(i18n.translate('Enter current password.'));
                } else {
                    await fetchWithRedirect('/api/user/info', {
                        method: 'DELETE',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(user)
                    },
                    {
                        error: message => {
                            clear();
                            showError(i18n.translate(message));
                        },
                        success: () => {
                            hide();
                        }
                    });
                }
            });
            $update.click(async () => {
                user.credentials = {password: $password.val(), newPassword: $newPassword.val()};
                user.name = $name.val();
                if (user.credentials.newPassword && !user.credentials.password) {
                    await showError(i18n.translate('Please enter current password.'));
                } else {
                    await fetchWithRedirect('/api/user/info', {
                        method: 'PUT',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify(user)
                    },
                    {
                        error: message => {
                            clear();
                            showError(i18n.translate(message || 'Command is not completed'));
                        },
                        success: () => {
                            hide();
                        }
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

module.exports = showAccount;