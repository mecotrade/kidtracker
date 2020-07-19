'use strict';

const moment = require('moment/min/moment-with-locales.min.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const CHAT_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';

async function showChat(deviceId) {

    const $modal = $('#chat');
    const $body = $('div.modal-body', $modal);
    const $close = $('#chat-close');

    $body.html('');

    const chat = await fetchWithRedirect(`/api/device/${deviceId}/chat`);
    chat.forEach(c => {
        const $time = $('<div>').addClass('message-timestamp').text(moment(c.timestamp).format(CHAT_TIME_FORMAT));
        const $message = $('<div>').addClass(c.source == 'DEVICE' ? 'incoming-message' : 'outgoing-message').append($time)
        if (c.type == 'AUDIO') {
            $message.append($('<audio>').prop('controls', true).attr('src', `/api/device/${deviceId}/media/${c.mediaId}`));
            $body.append($message);
        } else if (c.type == 'IMAGE') {
            $message.append($('<img>').attr('src', `/api/device/${deviceId}/media/${c.mediaId}`));
            $body.append($message);
        }
    })

    return new Promise(resolve => {

        function hide() {

            $close.off('click');

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', function onShow() {
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

module.exports = showChat;