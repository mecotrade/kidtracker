'use strict';

const i18n = require('./i18n.js');
const {initNotification, showWarning, showError} = require('./notification.js');

function initCommand($button, command, deviceId, options) {
    options = options || {};
    if (options.init) {
        options.init();
    }
    $button.off('click');
    $button.click(async () => {
        if (options.before) {
            options.before();
        }
        const response = await fetch(`/api/device/${deviceId}/command`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({type: command, payload: options.payload ? options.payload() : []})
        });
        if (response.ok) {
            if (options.after) {
                options.after();
            }
        } else {
            showError(i18n.translate('Command is not completed.'));
            if (options.error) {
                options.error();
            }
        }
    });
}

function initConfig($input, $button, parameter, config, deviceId, options) {
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
            if (done == false && options.defaultValue) {
                $input.val(options.defaultValue());
            }
        }
    }
    $button.off('click');
    $button.click(async () => {
        if (options.before) {
            options.before();
        }
        const response = await fetch(`/api/device/${deviceId}/config`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({parameter: parameter, value: options.value ? options.value() : $input.val()})
        });
        if (response.ok) {
            if (options.after) {
                options.after();
            }
        } else {
            showError(i18n.translate('Command is not completed.'));
            if (options.error) {
                options.error();
            }
        }
    });
}

function initCheck($check, parameter, config, deviceId) {
    config.filter(c => c.parameter == parameter).forEach(c => $check[0].checked = c.value == '1');
    $check.off('change');
    $check.click(async () => {
        const value = $check[0].checked == true ? '1' : '0';
        const response = await fetch(`/api/device/${deviceId}/config`, {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({parameter: parameter, value: value})
        });
        if (!response.ok) {
            $check[0].checked = !$check[0].checked;
            showError(i18n.translate('Command is not completed.'))
        }
    });
}

module.exports = {initCommand, initConfig, initCheck};