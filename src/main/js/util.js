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

async function fetchWithRedirect(url, fetchOptions, options) {
    options = options || {};
    if (options.block === true) {
        $.blockUI({
            message: '<img src="images/confirmation.gif">',
            css: {
                border: 'none',
                backgroundColor: 'transparent',
                centerX: true,
                centerY: true
            },
            baseZ: 10000
        });
    }
    const response = await fetch(url, fetchOptions);
    if (options.block === true) {
        $.unblockUI();
    }
    if (response.redirected) {
        window.location = response.url;
    } else if (response.ok) {
        if (options.success) {
            options.success();
        }
        if (response.status == 202) {
            await showInputToken(options.deviceId);
        }
        if (response.status != 204) {
            return await response.json();
        }
    } else {
        if (options.error) {
            const text = await response.text();
            options.error(text ? JSON.parse(text).message : null);
        }
    }
}

function initCommand($button, command, deviceId, options) {
    options = options || {};
    if (options.init) {
        options.init();
    }
    $button.off('click');
    $button.click(async () => {
        if (options.before) {
            const message = options.before();
            if (message) {
                showError(i18n.translate(message));
                return;
            }
        }
        if (options.device) {
            deviceId = options.device();
        }
        await fetchWithRedirect(`/api/device/${deviceId}/command`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({type: command, payload: options.payload ? options.payload() : []})
        },
        {
            error: message => {
                showError(i18n.translate(message || 'Command is not completed'));
                if (options.error) {
                    options.error(message);
                }
            },
            success: () => {
                if (options.after) {
                    options.after();
                }
            },
            deviceId: deviceId,
            block: true
        });
    });
}

function initConfig($input, $elements, parameter, config, deviceId, options) {
    options = options || {};
    if (options.init) {
        options.init();
    } else {
        if (options.initValue) {
            $input.val(options.initValue());
        } else {
            let done = false;
            config.filter(c => c.parameter == parameter).forEach(c => {
                $input.val(c.value);
                done = true;
            });
            if (done == false) {
                $input.val(options.defaultValue ? options.defaultValue() : '');
            }
        }
    }
    if (!Array.isArray($elements)) {
        $elements = [$elements];
    }
    $elements.forEach($element => {
        $element.off('click');
        $element.click(async () => {
            if (options.before) {
                options.before();
            }
            await fetchWithRedirect(`/api/device/${deviceId}/config`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({parameter: parameter, value: options.value ? options.value() : $input.val()})
            },
            {
                error: message => {
                    showError(i18n.translate(message || 'Command is not completed'));
                    if (options.error) {
                        options.error(message);
                    }
                },
                success: () => {
                    if (options.after) {
                        options.after();
                    }
                },
                block: true
            });
        });
    });
}

function initCheck($check, parameter, config, deviceId, defaultValue) {
    let done = false;
    config.filter(c => c.parameter == parameter).forEach(c => {
        $check[0].checked = c.value == '1';
        done = true;
    });
    if (done == false) {
        $check[0].checked = !!defaultValue;
    }
    $check.off('click');
    $check.click(async () => {
        const value = $check[0].checked == true ? '1' : '0';
        await fetchWithRedirect(`/api/device/${deviceId}/config`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({parameter: parameter, value: value})
        },
        {
            error: message => {
                $check[0].checked = !$check[0].checked;
                showError(i18n.translate(message || 'Command is not completed'));
            },
            block: true
        });
    });
}

async function showInputToken(deviceId) {

    const $modalToken = $('#input-token');

    const $inputToken = $('#input-token-input');
    const $closeToken = $('#input-token-close');
    const $executeToken = $('#input-token-execute');

    $inputToken.val('');

    return new Promise(resolve => {

        function hide() {

            $closeToken.off('click');
            $executeToken.off('click');
            $modalToken.modal('hide');

            resolve(null);
        }

        $modalToken.on('shown.bs.modal', function onShow() {
            $modalToken.off('shown.bs.modal', onShow);
            $closeToken.click(() => {
                hide();
            });
            $executeToken.click(async () => {
                const token = $inputToken.val();
                await fetchWithRedirect(deviceId ? `/api/device/${deviceId}/execute/${token}` : `/api/user/token/${token}`, {},
                {
                    error: message => {
                        showError(i18n.translate(message || 'Command is not completed'));
                    },
                    block: true
                });
                hide();
            });
        });

        $modalToken.modal({
            backdrop: 'static',
            focus: true,
            keyboard: false,
            show: true
        });
    });
}

module.exports = {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck};