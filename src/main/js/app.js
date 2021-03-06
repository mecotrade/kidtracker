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
const i18n = require('./i18n.js');
const {showInputToken, fetchWithRedirect, initCommand, initConfig, initCheck} = require('./util.js');
const createSliderControl = require('./slidercontrol.js');
const {showWarning, showError} = require('./notification.js');
const {initHistory, showHistory} = require('./history.js');
const {initPhone, showPhone} = require('./phone.js');
const {initContact, showContact} = require('./contact.js');
const {initWatchSettings, showWatchSettings} = require('./watchsettings.js');
const showDevice = require('./device.js');
const showAccount = require('./account.js');
const showRegister = require('./register.js');
const {showChat, addMessage} = require('./chat.js');
const {initHearts, showHearts} = require('./hearts.js');

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const ERROR_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm';

const LOST_INTERVAL = 15 * 60 * 1000;       /* 15 min */

const DEFAULT_ZOOM = 16;

const map = L.map('map');

const $select = $('#kid-select');
const $eye = $('#kid-watch');
const $hearts = $('#kid-hearts');
const $chat = $('#kid-chat');
const $history = $('#kid-history');
const $phone = $('#kid-phone');
const $contacts = $('#kid-contacts');
const $gps = $('#kid-locate');
const $bell = $('#kid-find');
const $watchSettings = $('#kid-settings');

const $username = $('#user-name');
const $cursor = $('#user-watch');
const $devices = $('#user-devices');
const $profile = $('#user-settings');
const $register = $('#register-user');

var user;
var kids;
var midnight;

var view = 'none';
var path = null;
var selected = null;

var stompClient = null;
var chatShown = false;

function updateViewIcons() {

    $('.bi-cursor', $cursor).toggle(view != 'user');
    $('.bi-cursor-fill', $cursor).toggle(view == 'user');
    $('.bi-eye', $eye).toggle(view != 'kid');
    $('.bi-eye-fill', $eye).toggle(view == 'kid');
}

function updateKidPopup(kid, position, snapshot, midnightSnapshot, online, alarm, notification, lastMsg, onTop) {

    const now = new Date();

    let battery = position.battery;
    let pedometer = position.pedometer;

    const date = new Date(position.timestamp);
    const snapshotDate = snapshot ? new Date(snapshot.timestamp) : null;

    if (snapshot && snapshotDate > date) {
        battery = snapshot.battery;
        pedometer = snapshot.pedometer;
    }

    if (midnightSnapshot) {
        pedometer -= midnightSnapshot.pedometer;
    }

    const datetime = kid.popupTimeFromNow ? moment(date).fromNow() : moment(date).format(KID_POPUP_TIME_FORMAT);
    const batteryClass = battery < BATTERY_LOW_THRESHOLD ? 'battery-low' : (battery < BATTERY_FULL_THRESHOLD ? 'battery-half' : 'battery-full');

    const $thumb = kid.thumb ? $('<img>').attr('src', kid.thumb).addClass('thumb-img') : $('<div>').addClass('thumb-placeholder');
    const $name = $('<div>').addClass('kid-popup-name').append($('<b>').text(kid.name));
    const $time = $('<div>').addClass('kid-popup-time').text(datetime);
    const $info = $('<div>').append($('<span>').addClass('kid-popup-pedometer').text(pedometer))
            .append($('<span>').addClass(batteryClass).text(`${battery}%`));

    const $alert = $('<div>').addClass('kid-popup-alert');
    if (position.sos) {
        $alert.append($('<div>').addClass('kid-popup-alert-sos'));
    }
    if (online && (!lastMsg || (now.getTime() - lastMsg.getTime() > LOST_INTERVAL))) {
        $alert.append($('<div>').addClass('kid-popup-alert-lost'));
    }
    if (position.takeOff) {
        $alert.append($('<div>').addClass('kid-popup-alert-removed'));
    }
    if (position.lowBattery) {
        $alert.append($('<div>').addClass('kid-popup-alert-battery'));
    }
    if (!position.valid) {
        $alert.append($('<div>').addClass('kid-popup-alert-invalid'));
    }

    if (alarm) {
        $alert.append($('<audio>').prop('autoplay', true).prop('loop', true).attr('src', '/sound/alarm.wav'));
    } else if (notification) {
        $alert.append($('<audio>').prop('autoplay', true).prop('loop', true).attr('src', '/sound/notification.wav'));
    }

    const $content = $('<div>').attr('id', `kid-popup-${kid.deviceId}`)
            .append($('<center>').append($thumb).append($name).append($time).append($info).append($alert));

    if (!kid.popup) {
        kid.popup = L.popup({closeOnClick: false, autoClose: false, closeButton: false, autoPan: false}).setLatLng([position.latitude, position.longitude]).addTo(map);
    }

    if (!kid.circle) {
        kid.circle = L.circle([position.latitude, position.longitude], position.accuracy, {weight: 0}).addTo(map);
    }

    kid.popup.setContent($content[0].outerHTML).setLatLng([position.latitude, position.longitude]);
    kid.circle.setLatLng([position.latitude, position.longitude]).setRadius(position.accuracy);

    const $popup = $(`#kid-popup-${kid.deviceId}`)
    const $parent = $popup.parent().parent();
    const $tip = $('div.leaflet-popup-tip', $parent.parent());
    const $divTime = $('div.kid-popup-time', $popup);

    $parent.off('click');
    $parent.click(async () => {
        selected = kid.deviceId;
        $select.val(selected);
        $('div.leaflet-popup.leaflet-zoom-animated').removeClass('on-top');
        updateKidPopup(kid, position, snapshot, midnightSnapshot, online, false, false, lastMsg, true);
        if (!path) {
            setView(kid);
        }
        if (alarm) {
            await fetchWithRedirect(`/api/device/${kid.deviceId}/off/alarm`);
        }
        if (notification) {
            await fetchWithRedirect(`/api/device/${kid.deviceId}/off/notification`);
            await showChat(kid.deviceId, stompClient);
        }
    });

    $parent.parent().toggleClass('on-top', onTop || alarm || notification)
    $parent.toggleClass('alarm', alarm).toggleClass('notification', notification && !alarm);
    $tip.toggleClass('alarm', alarm).toggleClass('notification', notification && !alarm);

    $(`span.${batteryClass}`, $popup).toggleClass('alarm', alarm);
    $('div.kid-popup-alert', $popup).children().each(function(i) {
        $(this).toggleClass('alarm', alarm)
    });

    $divTime.click(() => {
        kid.popupTimeFromNow = !kid.popupTimeFromNow;
        const time = kid.popupTimeFromNow ? moment(date).fromNow() : moment(date).format(KID_POPUP_TIME_FORMAT);
        $divTime.text(time);
    });
}

function setView(kid) {
    if (view == 'kid' || view == 'kid-once') {
        if (kid.popup) {
            map.setView(kid.popup.getLatLng());
        }
        if (view == 'kid-once') {
            view = 'none';
        }
    }
}

async function updateMidnightSnapshot() {

    midnight = moment().startOf('day').toDate();

    const snapshot = await fetchWithRedirect(`/api/user/kids/snapshot/${midnight.getTime()}`);
    if (snapshot) {
        kids.forEach(k => {
            const kidSnapshot = snapshot.find(s => s.deviceId == k.deviceId);
            if (kidSnapshot) {
                k.snapshot = kidSnapshot;
            }
        });
    }
}

function requestKidReports() {
    if (stompClient) {
        stompClient.send(`/user/${stompClient.userId}/report`);
    }
}

async function onKidReports(reports) {

    reports = Array.isArray(reports) ? reports : [reports];

    if (new Date().toDateString() != midnight.toDateString()) {
        await updateMidnightSnapshot();
    }

    const deviceId = $select.children('option:selected').val();

    reports.forEach(report => {
        if (path && report.notification) {
            removePath();
        }
        if (!path || deviceId != report.deviceId) {
            const kid = kids.find(k => k.deviceId == report.deviceId);
            updateKidPopup(kid,
                    report.position,
                    report.snapshot,
                    kid.snapshot,
                    true,
                    report.alarm,
                    report.notification && !(chatShown && deviceId == report.deviceId),
                    report.last ? moment(report.last).toDate() : null,
                    kid.deviceId == deviceId);
            if (!path && deviceId == report.deviceId) {
                setView(kid);
            }
        }
    });
}

function initMap() {

    // todo: add more geoservices
	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
	}).addTo(map);

	map.setZoom(DEFAULT_ZOOM);

    map.off('locationfound');
    map.on('locationfound', e => {

        if (user) {
            user.marker.setLatLng(e.latlng);
            user.circle.setLatLng(e.latlng).setRadius(e.accuracy);
        }

        if (view == 'user' || view == 'user-once') {
            map.setView(e.latlng, map.getZoom());
            if (view == 'user-once') {
                view = 'none';
            }
        }

        $cursor.attr('disabled', false);
    });

    map.off('locationerror');
    map.on('locationerror', e => {
        $cursor.attr('disabled', true);
    });

    map.off('drag');
    map.on('drag', function (e) {
        view = 'none';
        updateViewIcons();
    });
}

async function initNavbar() {

    moment.locale(i18n.lang);

    $('h5.modal-title').each(function (i) {
        i18n.apply($(this));
    });

    $('label').each(function (i) {
        const $this = $(this);
        if ($this.has('span').length) {
            $('span', $this).each(function (i) {
                i18n.apply($(this));
            })
        } else if ($(this).hasClass('btn')) {
            // do nothing
        } else {
            i18n.apply($this);
        }
    });

    $('option').each(function (i) {
        i18n.apply($(this));
    });

    initMap();

    initHistory();
    initPhone();
    initContact();
    initWatchSettings();
    initHearts();

    $eye.off('click');
    $eye.click(() => {
        view = view == 'kid' ? 'none' : 'kid';
        updateViewIcons();
        if (view == 'kid') {
            if (path) {
                path.move(path.slider.value());
            } else {
                requestKidReports();
            }
        }
    });

    $hearts.off('click');
    $hearts.click(async () => {
        await showHearts($select.children('option:selected').val());
    });

    $chat.off('click');
    $chat.click(async () => {
        const deviceId = $select.children('option:selected').val()
        await fetchWithRedirect(`/api/device/${deviceId}/off/notification`);
        requestKidReports();
        chatShown = true;
        await showChat(deviceId, stompClient);
        chatShown = false;
    });

    $history.off('click');
    $history.click(async () => {

        if (!path) {

            const deviceId = $select.children('option:selected').val();
            const pathRange = await showHistory(deviceId);

            if (pathRange) {

                let start = pathRange.start.getTime();
                let end = pathRange.end.getTime();

                $('.bi-calendar2-week-fill', $history).hide();
                $('.bi-geo-alt', $history).show();

                const kid = kids.find(k => k.deviceId == deviceId);

                // TODO if not found

                const kidPath = await fetchWithRedirect(`/api/device/${deviceId}/path/${start}/${end}`);

                if (kidPath.length > 0) {

                    const track = L.polyline(kidPath.map(p => [p.latitude, p.longitude]), {dashArray: '4'}).addTo(map);

                    let snapshotDate = moment(start).startOf('day').toDate();
                    const pathMidnightSnapshot = await fetchWithRedirect(`/api/device/${deviceId}/snapshot/${snapshotDate.getTime()}`) || {
                        deviceId: kidPath[0].deviceId,
                        timestamp: kidPath[0].timestamp,
                        pedometer: kidPath[0].pedometer,
                        rolls: kidPath[0].rolls,
                        battery: kidPath[0].battery
                    }

                    function move(i) {
                        updateKidPopup(kid, kidPath[i], null, pathMidnightSnapshot, false, false, false, null, true);
                        setView(kid);
                        return moment(new Date(kidPath[i].timestamp)).format(KID_POPUP_TIME_FORMAT);
                    }

                    const slider = createSliderControl({
                        position: "topright",
                        alwaysShowDate: true,
                        length: kidPath.length,
                        slide: move
                    });
                    map.addControl(slider);
                    slider.startSlider();

                    path = {deviceId: kid.deviceId, track: track, slider: slider, move: move};

                } else {

                    await showWarning(i18n.translate('No data'));

                    $('.bi-calendar2-week-fill', $history).show();
                    $('.bi-geo-alt', $history).hide();
                }
            }
        } else {
            removePath();
            requestKidReports();
        }
    });

    $phone.off('click');
    $phone.click(async () => {
        const deviceId = $select.children('option:selected').val();
        await showPhone(user, deviceId);
    });

    $contacts.off('click');
    $contacts.click(async () => {
        const deviceId = $select.children('option:selected').val();
        await showContact(deviceId);
    });

    initCommand($gps, 'CR', null, {
        device: () => $select.children('option:selected').val(),
        after: () => {
            view = 'kid';
            updateViewIcons();
            requestKidReports();
        }
    });
    initCommand($bell, 'FIND', null, {device: () => $select.children('option:selected').val()});

    $watchSettings.off('click');
    $watchSettings.click(async () => {
        const deviceId = $select.children('option:selected').val();
        await showWatchSettings(deviceId);
    });

    $cursor.off('click');
    $cursor.click(() => {
        view = view == 'user' ? 'none' : 'user';
        updateViewIcons();
        if (view == 'user') {
            map.setView(user.marker.getLatLng(), map.getZoom());
        }
    });

    $devices.off('click');
    $devices.click(async () => {
        await showDevice(stompClient);
        await showNavbar();
        requestKidReports();
    });

    $profile.off('click');
    $profile.click(async () => {
        await showAccount();
        await showNavbar();
        requestKidReports();
    });

    $register.off('click');
    $register.click(async () => {
        await showRegister();
    })

    view = 'user-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
}

function removePath() {

    if (path) {
        map.removeControl(path.slider);
        map.removeLayer(path.track);
    }

    path = null;

    $('.bi-calendar2-week-fill', $history).show();
    $('.bi-geo-alt', $history).hide();
}

async function showNavbar() {

    // user definition and location
    let userProps;
    if (user) {
        userProps = {latlng: user.marker.getLatLng(), radius: user.circle.getRadius()};
        if (user.marker) {
            user.marker.removeFrom(map);
            delete user.marker;
        }
        if (user.circle) {
            user.circle.removeFrom(map);
            delete user.circle;
        }
    }

    user = await fetchWithRedirect(`/api/user/info`);
    user.marker = L.marker(userProps ? userProps.latlng : [0,0]).addTo(map);
    user.circle = L.circle(userProps ? userProps.latlng : [0,0], userProps ? userProps.radius : 0, {weight: 0, color: 'green'}).addTo(map);
    $username.text(user.name);
    $register.toggle(user.admin);
    if (!user.admin) {
        $register.off('click');
    }
    if (!user.phone) {
        $devices.prop('disabled', true);
        $devices.off('click');
    }

    // kids definition and location
    const fromNow = {};
    if (kids) {
        kids.forEach(k => {
            fromNow[k.deviceId] = k.popupTimeFromNow;
            if (k.popup) {
                k.popup.removeFrom(map);
                delete k.popup;
            }
            if (k.circle) {
                k.circle.removeFrom(map);
                delete k.circle;
            }
        });
    }

	kids = await fetchWithRedirect(`/api/user/kids/info`);
    $select.html(kids.map(k => `<option value="${k.deviceId}">${k.name}</option>`).reduce((html, option) => html + option, ''));
    if (selected) {
        $select.val(selected);
    }
    selected = $select.children('option:selected').val();
    $select.off('change');
    $select.change(() => {
        selected = $select.children('option:selected').val();
        kids.filter(k => k.deviceId == selected).forEach(k => setView(k));
        if (path && path.deviceId != selected) {
            removePath();
        }
        requestKidReports();
    });
    kids.forEach(k => {
        k.popupTimeFromNow = k.deviceId in fromNow ? fromNow[k.deviceId] : true;
    });

    $('ul.navbar-left button').each(function(i) {
        $(this).prop('disabled', kids.length == 0);
    });

    if (kids.length == 0 && (view == 'kid' || view == 'kid-once')) {
        view = 'none';
    }

    updateViewIcons();

    // init pedometer to closest midnight
    await updateMidnightSnapshot();
}

function connectStompClient() {
    return new Promise(function(resolve) {
        const client = Stomp.over(new SockJS('/device'));
        console.log(user.credentials.username);
        client.userId = user.credentials.username;
        client.connect({}, frame => resolve(client), error => window.location.replace('/'));
    });
}

$(async () => {
    await initNavbar();
    await showNavbar();

    stompClient = await connectStompClient();
    stompClient.subscribe('/user/queue/report', response => onKidReports(JSON.parse(response.body)));
});
