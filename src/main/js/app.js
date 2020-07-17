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

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const ERROR_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm';

const LOST_INTERVAL = 15 * 60 * 1000;       /* 15 min */

const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10 * 1000;      /* 10 sec */

const map = L.map('map');

const $select = $('#kid-select');
const $eye = $('#kid-watch');
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

function updateViewIcons() {

    $('.bi-cursor', $cursor).toggle(view != 'user');
    $('.bi-cursor-fill', $cursor).toggle(view == 'user');
    $('.bi-eye', $eye).toggle(view != 'kid');
    $('.bi-eye-fill', $eye).toggle(view == 'kid');
}

function updateKidPopup(kid, position, snapshot, midnightSnapshot, online, alarm, lastMsg, onTop) {

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

    const $content = $('<div>').attr('id', `kid-popup-${kid.deviceId}`)
            .append($('<center>').append($thumb).append($name).append($time).append($info).append($alert));

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
        if (alarm) {
            await fetchWithRedirect(`/api/device/${kid.deviceId}/alarmoff`);
        }
        updateKidPopup(kid, position, snapshot, midnightSnapshot, online, false, lastMsg, true);
        if (!path) {
            setView(kid);
        }
    });

    $parent.parent().toggleClass('on-top', onTop || alarm)
    $parent.toggleClass('alarm', alarm);
    $tip.toggleClass('alarm', alarm);
    $(`span.${batteryClass}`, $popup).toggleClass('alarm', alarm);
    $('div.kid-popup-alert', $popup).children().each(function(i) {
        $(this).toggleClass('alarm', alarm);
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

async function locateKids() {

    if (new Date().toDateString() != midnight.toDateString()) {
        await updateMidnightSnapshot();
    }

    const deviceId = $select.children('option:selected').val();

    const report = await fetchWithRedirect(`/api/user/kids/report`);
    if (report) {
        report.positions.forEach(p => {
            if (!path || p.deviceId != deviceId) {
                const kid = kids.find(k => k.deviceId == p.deviceId);
                if (kid) {
                    const snapshot = report.snapshots.find(s => s.deviceId == p.deviceId);
                    const alarm = report.alarms.includes(p.deviceId);
                    const lastMsg = p.deviceId in report.last ? moment(report.last[p.deviceId]).toDate() : null;
                    updateKidPopup(kid, p, snapshot, kid.snapshot, true, alarm, lastMsg, kid.deviceId == deviceId);
                    if (!path && kid.deviceId == deviceId) {
                        setView(kid);
                    }
                } else {
                    // TODO if not found
                }
            }
        });
    }
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

    $eye.off('click');
    $eye.click(() => {
        view = view == 'kid' ? 'none' : 'kid';
        updateViewIcons();
        if (view == 'kid') {
            if (path) {
                path.move(path.slider.value());
            } else {
                locateKids();
            }
        }
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
                        updateKidPopup(kid, kidPath[i], null, pathMidnightSnapshot, false, false, null, true);
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
            locateKids();
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
            locateKids();
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
        await showDevice();
        await showNavbar();
    });

    $profile.off('click');
    $profile.click(async () => {
        await showAccount();
        await showNavbar();
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
        user.marker.removeFrom(map);
        user.circle.removeFrom(map);
    }

    user = await fetchWithRedirect(`/api/user/info`);
    if (user) {
        user.marker = L.marker(userProps ? userProps.latlng : [0,0]).addTo(map);
        user.circle = L.circle(userProps ? userProps.latlng : [0,0], userProps ? userProps.radius : 0, {weight: 0, color: 'green'}).addTo(map);
        $username.text(user.name);
        $register.toggle(user.admin);
        if (!user.admin) {
            $register.off('click');
        }
    }

    // kids definition and location
    const popupProps = {};
    if (kids) {
        kids.forEach(k => {
            popupProps[k.deviceId] = {fromNow: k.popupTimeFromNow, latlng: k.popup.getLatLng(), radius: k.circle.getRadius()};
            k.popup.removeFrom(map);
            k.circle.removeFrom(map);
        });
    }

	kids = await fetchWithRedirect(`/api/user/kids/info`);
	if (kids) {
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
            locateKids();
        });
        kids.forEach(k => {
            const props = popupProps[k.deviceId];
            k.popup = L.popup({closeOnClick: false, autoClose: false, closeButton: false, autoPan: false}).setLatLng(props ? props.latlng : [0, 0]).addTo(map);
            k.circle = L.circle(props ? props.latlng : [0, 0], props ? props.radius : 0, {weight: 0}).addTo(map);
            k.popupTimeFromNow = props ? props.fromNow :  true;
        });

        $('ul.navbar-left button').each(function(i) {
            $(this).prop('disabled', kids.length == 0);
        });

        if (kids.length == 0 && (view == 'kid' || view == 'kid-once')) {
            view = 'none';
        }
    }

    updateViewIcons();

    // init pedometer to closest midnight
    await updateMidnightSnapshot();

    locateKids();
}

window.addEventListener('load', async function onload() {

    await initNavbar();
    await showNavbar();

    setInterval(locateKids, KID_POSITION_QUERY_INTERVAL);
});