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

const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const $modal = $('#hearts');
const $reset = $('#hearts-reset');
const $update = $('#hearts-update');
const $close = $('#hearts-close');
const $input = $('#hearts-input');

function initHearts() {
    $input.inputSpinner();
}

async function showHearts(deviceId) {

    $reset.off('click');
    $reset.click(() => {
        $input.val(0);
    });

    const config = await fetchWithRedirect(`/api/device/${deviceId}/config`);
    if (config) {
        initConfig($input, $update, 'FLOWER', config, deviceId, {
            defaultValue: () => 0,
            value: () => $input.val()
        });
    }

    return new Promise(resolve => {

        function hide() {

            $close.off('click');

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', async function onShow() {
            $modal.off('shown.bs.modal', onShow);
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

module.exports = {initHearts, showHearts};