'use strict';

const moment = require('moment/min/moment-with-locales.min.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const CHAT_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const CHAT_UPDATE_INTERVAL = 10000;

var messages = [];
var timerId = null;

(function($) {
    $.fn.hasScrollBar = function() {
        return this.get(0).scrollHeight > this.height();
    }
})(jQuery);

function addMessage(message, deviceId, $body, prepend) {

    const id = `message-timestamp-${deviceId}-${message.mediaId}`;

    const $time = $('<div>').addClass('message-timestamp').attr('id', id).text(moment(message.timestamp).format(CHAT_TIME_FORMAT));
    const $message = $('<div>').addClass(message.source == 'DEVICE' ? 'incoming-message' : 'outgoing-message').append($time)
    if (message.type == 'AUDIO') {
        $message.append($('<audio>').prop('controls', true).attr('src', `/api/device/${deviceId}/media/${message.mediaId}`));
    } else if (message.type == 'IMAGE') {
        $message.append($('<img>').attr('src', `/api/device/${deviceId}/media/${message.mediaId}`));
    } else if (message.type == 'TEXT') {
        $message.append($('<div>').addClass('message-text').append($('<span>').text(message.text)));
    }

    if (prepend) {
        $body.prepend($message);
    } else {
        $body.append($message);
    }

    message.fromNow = false;
    $(`#${id}`).off('click');
    $(`#${id}`).click(() => {
        message.fromNow = !message.fromNow;
        $(`#${id}`).text(message.fromNow ? moment(message.timestamp).fromNow() : moment(message.timestamp).format(CHAT_TIME_FORMAT));
    });
}

async function showChat(deviceId) {

    const $modal = $('#chat');
    const $body = $('div.modal-body', $modal);
    const $message = $('div.modal-footer textarea', $modal);

    const $snapshot = $('#chat-snapshot');
    const $record = $('#chat-record');
    const $send = $('#chat-send');
    const $last = $('#chat-last');
    const $close = $('#chat-close');

    async function updateChat() {
        const onBottom = ($body[0].scrollTop + $body[0].clientHeight)== $body[0].scrollHeight;
        const msgs = await fetchWithRedirect(messages && messages.length > 0
            ? `/api/device/${deviceId}/chat/after/${messages[messages.length - 1].mediaId}`
            : `/api/device/${deviceId}/chat/last`);
        msgs.forEach(async message => await addMessage(message, deviceId, $body));
        messages = messages.concat(msgs);
        if (msgs.length > 0) {
            if (onBottom) {
                $body[0].scrollTop = $body[0].scrollHeight - $body[0].clientHeight;
            } else {
                $last.addClass('btn-info').removeClass('btn-outline-info');
            }
        }
    }

    initCommand($snapshot, 'RCAPTURE', deviceId);
    initCommand($record, 'RECORD', deviceId);
    initCommand($send, 'MESSAGE', deviceId, {
        payload: () => [$message.val()],
        after: () => {
            $message.val('');
            updateChat();
        }
    });

    $last.off('click');
    $last.click(() => {
        $last.removeClass('btn-info').addClass('btn-outline-info');
        $body[0].scrollTop = $body[0].scrollHeight - $body[0].clientHeight;
    });

    $body.off('scroll');
    $body.scroll(async () => {
        if ($body.scrollTop() == 0) {
            if (messages && messages.length > 0) {
                const msgs = await fetchWithRedirect(`/api/device/${deviceId}/chat/before/${messages[0].mediaId}`);
                if (msgs.length > 0) {
                    msgs.reverse().forEach(async message => await addMessage(message, deviceId, $body, true));
                    messages = msgs.reverse().concat(messages);
                }
            }
        } else if (($body[0].scrollTop + $body[0].clientHeight)== $body[0].scrollHeight) {
            $last.removeClass('btn-info').addClass('btn-outline-info');
        }
    });

    return new Promise(resolve => {

        function hide() {

            $snapshot.off('click');
            $record.off('click');
            $send.off('click');
            $close.off('click');

            clearInterval(timerId);
            timerId = null;

            $modal.modal('hide');
            resolve(null);
        }

        $modal.on('shown.bs.modal', async function onShow() {
            $modal.off('shown.bs.modal', onShow);

            $body.html('');
            messages = [];

            const start = moment().startOf('day').toDate().getTime();
            const end = moment().endOf('day').toDate().getTime();

            messages = await fetchWithRedirect(`/api/device/${deviceId}/chat/last`);
            messages.forEach(async message => await addMessage(message, deviceId, $body));

            while (!$body.hasScrollBar() && messages.length > 0) {
                const msgs = await fetchWithRedirect(`/api/device/${deviceId}/chat/before/${messages[0].mediaId}`);
                if (msgs.length > 0) {
                    msgs.reverse().forEach(async message => await addMessage(message, deviceId, $body, true));
                    messages = msgs.reverse().concat(messages);
                } else {
                    break;
                }
            }

            $body[0].scrollTop = $body[0].scrollHeight;

            timerId = setInterval(updateChat, CHAT_UPDATE_INTERVAL);

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