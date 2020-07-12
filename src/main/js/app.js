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

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const WATCH_OFF_ICON = '<svg width="20px" height="16px" viewBox="0 0 16 16" class="bi bi-smartwatch" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M14 5h.5a.5.5 0 0 1 .5.5v2a.5.5 0 0 1-.5.5H14V5z"/><path fill-rule="evenodd" d="M8.5 4.5A.5.5 0 0 1 9 5v3.5a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h2V5a.5.5 0 0 1 .5-.5z"/><path fill-rule="evenodd" d="M4.5 2h7A2.5 2.5 0 0 1 14 4.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 2 11.5v-7A2.5 2.5 0 0 1 4.5 2zm0 1A1.5 1.5 0 0 0 3 4.5v7A1.5 1.5 0 0 0 4.5 13h7a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 11.5 3h-7z"/><path d="M4 2.05v-.383C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v.383a2.512 2.512 0 0 0-.5-.05h-7c-.171 0-.338.017-.5.05zm0 11.9c.162.033.329.05.5.05h7c.171 0 .338-.017.5-.05v.383c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333v-.383z"/></svg>'
const LOW_BATTERY_ICON = '<svg class="bi bi-battery" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M12 5H2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zM2 4a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H2z"/><path d="M14.5 9.5a1.5 1.5 0 0 0 0-3v3z"/></svg>';
const SOS_ICON = '<svg class="bi bi-exclamation-octagon-fill" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M11.46.146A.5.5 0 0 0 11.107 0H4.893a.5.5 0 0 0-.353.146L.146 4.54A.5.5 0 0 0 0 4.893v6.214a.5.5 0 0 0 .146.353l4.394 4.394a.5.5 0 0 0 .353.146h6.214a.5.5 0 0 0 .353-.146l4.394-4.394a.5.5 0 0 0 .146-.353V4.893a.5.5 0 0 0-.146-.353L11.46.146zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/></svg>';
const LOST_ICON = '<svg class="bi bi-x-circle" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"></path><path fill-rule="evenodd" d="M11.854 4.146a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708-.708l7-7a.5.5 0 0 1 .708 0z"></path><path fill-rule="evenodd" d="M4.146 4.146a.5.5 0 0 0 0 .708l7 7a.5.5 0 0 0 .708-.708l-7-7a.5.5 0 0 0-.708 0z"></path></svg>'

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const ERROR_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm';

const LOST_INTERVAL = 15 * 60 * 1000;

const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

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

var user;
var kids;
var midnight;

var view = 'none';
var path = null;

function updateViewIcons() {

    $('.bi-cursor', $cursor).toggle(view != 'user');
    $('.bi-cursor-fill', $cursor).toggle(view == 'user');
    $('.bi-eye', $eye).toggle(view != 'kid');
    $('.bi-eye-fill', $eye).toggle(view == 'kid');
}

function updateKidPopup(kid, position, snapshot, midnightSnapshot, online, setView, alarm) {

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

    let alert = (position.sos ? SOS_ICON : '')
            + (online && (!snapshot || (now.getTime() - snapshotDate.getTime() > LOST_INTERVAL)) ? LOST_ICON : '')
            + (position.takeOff ? WATCH_OFF_ICON : '')
            + (position.lowBattery ? LOW_BATTERY_ICON : '');

    const $thumb = $('<img>').attr('src', kid.thumb).addClass('kid-popup-thumb');
    const $name = $('<div>').addClass('kid-popup-name').append($('<b>').text(kid.name));
    const $time = $('<div>').addClass('kid-popup-time').text(datetime);
    const $info = $('<div>').append($('<span>').addClass('kid-popup-pedometer').text(pedometer))
            .append($('<span>').addClass(batteryClass).text(`${battery}%`))
            .append($('<div>').addClass('kid-popup-alert').html(alert));
    const $content = $('<div>').attr('id', `kid-popup-${kid.deviceId}`)
            .append($('<center>').append($thumb).append($name).append($time).append($info));

    kid.popup.setContent($content[0].outerHTML).setLatLng([position.latitude, position.longitude]);
    kid.circle.setLatLng([position.latitude, position.longitude]).setRadius(position.accuracy);

    if (alarm == true) {

        const $parent = $(`#kid-popup-${kid.deviceId}`).parent().parent();
        $parent.attr('style', 'background: red');
        $('div.leaflet-popup-tip', $parent.parent()).attr('style', 'background: red');
        $(`#kid-popup-${kid.deviceId} span.${batteryClass}`).attr('style', 'color: white');
        $(`#kid-popup-${kid.deviceId} div.kid-popup-alert`).attr('style', 'color: white');

        $(`#kid-popup-${kid.deviceId}`).off('click');
        $(`#kid-popup-${kid.deviceId}`).click(async function () {
            const $parent = $(`#kid-popup-${kid.deviceId}`).parent().parent();
            $parent.removeAttr('style');
            $('div.leaflet-popup-tip', $parent.parent()).removeAttr('style');
            $(`#kid-popup-${kid.deviceId} span.${batteryClass}`).removeAttr('style');
            $(`#kid-popup-${kid.deviceId} div.kid-popup-alert`).removeAttr('style');
            await fetchWithRedirect(`/api/device/${kid.deviceId}/alarmoff`);
        });
    }

    $(`#kid-popup-${kid.deviceId} div.kid-popup-time`).click(function () {
        if (kid.popupTimeFromNow) {
            $(`#kid-popup-${kid.deviceId} div.kid-popup-time`).text(moment(date).format(KID_POPUP_TIME_FORMAT));
            kid.popupTimeFromNow = false;
        } else {
            $(`#kid-popup-${kid.deviceId} div.kid-popup-time`).text(moment(date).fromNow());
            kid.popupTimeFromNow = true;
        }
    });

    if (setView && (view == 'kid' || view == 'kid-once')) {
        map.setView(kid.popup.getLatLng());
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
                    const setView = !path && kid.deviceId == deviceId
                    const alarm = report.alarms.includes(p.deviceId);
                    updateKidPopup(kid, p, snapshot, kid.snapshot, true, setView, alarm);
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

    // init i18n
    const locale = navigator.language ? navigator.language.split('-')[0] : null;
    if (locale) {
        moment.locale(locale);
        i18n.setLocale(locale);
    }

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
                        updateKidPopup(kid, kidPath[i], null, pathMidnightSnapshot, false, true, false);
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

                    path = {track: track, slider: slider, move: move};

                } else {

                    await showWarning(i18n.translate('No data'));

                    $('#kid-path-switch-icon').show();
                    $('#kid-geo-switch-icon').hide();
                }
            }
        } else {
            map.removeControl(path.slider);
            map.removeLayer(path.track);
            path = null;

            $('.bi-calendar2-week-fill', $history).show();
            $('.bi-geo-alt', $history).hide();

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

    initCommand($gps, 'CR', null, {device: () => $select.children('option:selected').val()});
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

    view = 'user-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
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
        kids.forEach(k => {
            const props = popupProps[k.deviceId];
            k.popup = L.popup({closeOnClick: false, autoClose: false, closeButton: false, autoPan: false}).setLatLng(props ? props.latlng : [0, 0]).addTo(map);
            k.circle = L.circle(props ? props.latlng : [0, 0], props ? props.radius : 0, {weight: 0}).addTo(map);
            k.popupTimeFromNow = props ? props.fromNow :  true;
        });
    }

    // init pedometer to closest midnight
    await updateMidnightSnapshot();

    locateKids();
}

window.addEventListener('load', async function onload() {

    await initNavbar();
    await showNavbar();

    setInterval(locateKids, KID_POSITION_QUERY_INTERVAL);
});