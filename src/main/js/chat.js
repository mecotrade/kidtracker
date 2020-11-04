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
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');

const CHAT_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const CHAT_UPDATE_INTERVAL = 10000;

var timerId = null;

(function($) {
    $.fn.hasScrollBar = function() {
        return this.get(0).scrollHeight > this.height();
    }
})(jQuery);

function addMessage(message, deviceId, $body) {

    const $time = $('<div>').addClass('message-timestamp').attr('data-fromnow', false).text(moment(message.timestamp).format(CHAT_TIME_FORMAT));
    const $message = $('<div>').attr('data-media', message.mediaId).addClass(message.source == 'DEVICE' ? 'incoming-message' : 'outgoing-message').append($time)
    if (message.type == 'AUDIO') {
        $message.append($('<audio>').prop('controls', true).attr('src', `/api/device/${deviceId}/media/${message.mediaId}`));
    } else if (message.type == 'IMAGE') {
        $message.append($('<img>').attr('src', `/api/device/${deviceId}/media/${message.mediaId}`));
    } else if (message.type == 'TEXT') {
        $message.append($('<div>').addClass('message-text').append($('<span>').text(message.text)));
    }

    const $messages = $body.children();
    if ($messages.length == 0 || parseInt($($messages[$messages.length - 1]).data('media')) < message.mediaId) {
        $body.append($message);
    } else {
        for (let i = 0; i < $messages.length; i++) {
            const $msg = $($messages[i]);
            const mediaId = parseInt($msg.data('media'));
            if (mediaId === message.mediaId) {
                $msg.replaceWith($message);
                break;
            } else if (mediaId > message.mediaId) {
                $msg.before($message);
                break;
            }
        }
    }

    const $msg = $(`div[data-media="${message.mediaId}"] > div.message-timestamp`, $body);
    $msg.off('click');
    $msg.click(() => {
        const fromNow = $msg.attr('data-fromnow') == 'true';
        $msg.attr('data-fromnow', !fromNow);
        $msg.text(!fromNow ? moment(message.timestamp).fromNow() : moment(message.timestamp).format(CHAT_TIME_FORMAT));
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
        const $messages = $body.children();
        const messages = await fetchWithRedirect($messages.length > 0
            ? `/api/device/${deviceId}/chat/after/${$($messages[$messages.length - 1]).data('media')}`
            : `/api/device/${deviceId}/chat/last`);
        messages.forEach(message => addMessage(message, deviceId, $body));
        if (messages.length > 0) {
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
            const $messages = $body.children();
            if ($messages.length > 0) {
                const messages = await fetchWithRedirect(`/api/device/${deviceId}/chat/before/${$($messages[0]).data('media')}`);
                messages.forEach(message => addMessage(message, deviceId, $body));
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

            const start = moment().startOf('day').toDate().getTime();
            const end = moment().endOf('day').toDate().getTime();

            const msgs = await fetchWithRedirect(`/api/device/${deviceId}/chat/last`);
            msgs.forEach(message => addMessage(message, deviceId, $body));

            const $messages = $body.children();
            while (!$body.hasScrollBar() && $messages.length > 0) {
                const messages = await fetchWithRedirect(`/api/device/${deviceId}/chat/before/${$($messages[0]).data('media')}`);
                if (messages.length > 0) {
                    messages.forEach(message => addMessage(message, deviceId, $body));
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

module.exports = {showChat, addMessage};