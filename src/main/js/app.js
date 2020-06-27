'use strict';

const moment = require('moment/min/moment-with-locales.min.js');
const createSliderControl = require('./slidercontrol.js');
const i18n = require('./i18n.js');
const {initHistory, pickRange} = require('./history.js');
const {initError, showError} = require('./showerror.js');


const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const WATCH_OFF_ICON = '<svg class="bi bi-watch" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4 14.333v-1.86A5.985 5.985 0 0 1 2 8c0-1.777.772-3.374 2-4.472V1.667C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v1.86A5.985 5.985 0 0 1 14 8a5.985 5.985 0 0 1-2 4.472v1.861c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333zM13 8A5 5 0 1 0 3 8a5 5 0 0 0 10 0z"/><rect width="1" height="2" x="13.5" y="7" rx=".5"/><path fill-rule="evenodd" d="M8 4.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h1.5V5a.5.5 0 0 1 .5-.5z"/></svg>';
const LOW_BATTERY_ICON = '<svg class="bi bi-battery" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M12 5H2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zM2 4a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H2z"/><path d="M14.5 9.5a1.5 1.5 0 0 0 0-3v3z"/></svg>';
const SOS_ICON = '<svg class="bi bi-exclamation-octagon-fill" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M11.46.146A.5.5 0 0 0 11.107 0H4.893a.5.5 0 0 0-.353.146L.146 4.54A.5.5 0 0 0 0 4.893v6.214a.5.5 0 0 0 .146.353l4.394 4.394a.5.5 0 0 0 .353.146h6.214a.5.5 0 0 0 .353-.146l4.394-4.394a.5.5 0 0 0 .146-.353V4.893a.5.5 0 0 0-.146-.353L11.46.146zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/></svg>';
const LOST_ICON = '<svg class="bi bi-x-circle" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"></path><path fill-rule="evenodd" d="M11.854 4.146a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708-.708l7-7a.5.5 0 0 1 .708 0z"></path><path fill-rule="evenodd" d="M4.146 4.146a.5.5 0 0 0 0 .708l7 7a.5.5 0 0 0 .708-.708l-7-7a.5.5 0 0 0-.708 0z"></path></svg>'

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';
const ERROR_MESSAGE_TIME_FORMAT = 'D MMMM YYYY HH:mm';

const LOST_INTERVAL = 15 * 60 * 1000;

const userId = 1;

var map = L.map('map');

var user;
var kids;
var midnight;

var view = 'none';
var path = null;

$('#user-watch').on('click', function onLocateMe() {
    if (view == 'user') {
        $('#stop-user-watch-icon').show();
        $('#user-watch-icon').hide();
        view = 'none';
    } else {
        if (view == 'kid') {
            $('#stop-kid-watch-icon').show();
            $('#kid-watch-icon').hide();
        }
        $('#stop-user-watch-icon').hide();
        $('#user-watch-icon').show();
        view = 'user';

        map.setView(user.marker.getLatLng(), map.getZoom());
    }
});

$('#kid-watch').on('click', async function onLocateKid() {
    if (view == 'kid') {
        $('#stop-kid-watch-icon').show();
        $('#kid-watch-icon').hide();
        view = 'none';
    } else {
        if (view == 'user') {
            $('#stop-user-watch-icon').show();
            $('#user-watch-icon').hide();
        }
        $('#stop-kid-watch-icon').hide();
        $('#kid-watch-icon').show();
        view = 'kid';

        if (path) {
            path.move(path.slider.value());
        } else {
            locateKids();
        }
    }
});

$('#kid-history').on('click', async function onKidPath() {

    if (!path) {

        const pathRange = await pickRange();
        if (pathRange) {

            let start = pathRange.start.getTime();
            let end = pathRange.end.getTime();

            $('#kid-path-switch-icon').hide();
            $('#kid-geo-switch-icon').show();

            const deviceId = $('#kid-select').children('option:selected').val();

            const kid = kids.find(k => k.deviceId == deviceId);

            // TODO if not found

            const kidPathResponse = await fetch(`/api/device/${deviceId}/path/${start}/${end}`);
            const kidPath = await kidPathResponse.json();

            if (kidPath.length > 0) {

                const track = L.polyline(kidPath.map(p => [p.latitude, p.longitude]), {dashArray: '4'}).addTo(map);

                let snapshotDate = moment(start).startOf('day').toDate();
                const snapshotResponse = await fetch(`/api/device/${deviceId}/snapshot/${snapshotDate.getTime()}`);
                const pathMidnightSnapshot = snapshotResponse.status == 200 ? await snapshotResponse.json() : {
                    deviceId: kidPath[0].deviceId,
                    timestamp: kidPath[0].timestamp,
                    pedometer: kidPath[0].pedometer,
                    rolls: kidPath[0].rolls,
                    battery: kidPath[0].battery
                };

                function move(i) {
                    updateKidPopup(kid, kidPath[i], null, pathMidnightSnapshot, false, true);
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

                await showError(i18n.format('No data'));

                $('#kid-path-switch-icon').show();
                $('#kid-geo-switch-icon').hide();
            }
        }
    } else {
        map.removeControl(path.slider);
        map.removeLayer(path.track);
        path = null;

        $('#kid-path-switch-icon').show();
        $('#kid-geo-switch-icon').hide();

        locateKids();
    }
});

$('#kid-refresh').on('click', async function onForceGeo() {
    const deviceId = $('#kid-select').children('option:selected').val();
    await fetch(`/api/device/${deviceId}/locate`);
});

$('#kid-find').on('click', async function onForceGeo() {
    const deviceId = $('#kid-select').children('option:selected').val();
    await fetch(`/api/device/${deviceId}/find`);
});

map.on('locationfound', function onLocationFound(e) {

    user.marker.setLatLng(e.latlng);
    user.circle.setLatLng(e.latlng).setRadius(e.accuracy);

    if (view == 'user' || view == 'user-once') {
        map.setView(e.latlng, map.getZoom());
        if (view == 'user-once') {
            view = 'none';
        }
    }

    $('#user-watch').attr('disabled', false);
});

map.on('locationerror', function onLocationError(e) {
    $('#user-watch').attr('disabled', true);
    console.error(e.message);
});

map.on('drag', function onMouseDrag(e) {
    $('#stop-user-watch-icon').show();
    $('#user-watch-icon').hide();
    $('#stop-kid-watch-icon').show();
    $('#kid-watch-icon').hide();
    view = 'none';
});

function updateKidPopup(kid, position, snapshot, midnightSnapshot, online, setView) {

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

    const content = `<center><img src="${kid.thumb}" class="kid-popup-thumb"/><div class="kid-popup-name"><b>${kid.name}</b></div><div id="kid-popup-${kid.deviceId}" class="kid-popup-time">${datetime}</div><div><span class="kid-popup-pedometer">${pedometer}</span><span class="${batteryClass}">${battery}%</span><div class="kid-popup-alert">${alert}</div></div></center>`;
    kid.popup.setContent(content).setLatLng([position.latitude, position.longitude]);
    kid.circle.setLatLng([position.latitude, position.longitude]).setRadius(position.accuracy);

    $(`#kid-popup-${kid.deviceId}`).on('click', function() {
        if (kid.popupTimeFromNow) {
            $(`#kid-popup-${kid.deviceId}`).text(moment(date).format(KID_POPUP_TIME_FORMAT));
            kid.popupTimeFromNow = false;
        } else {
            $(`#kid-popup-${kid.deviceId}`).text(moment(date).fromNow());
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

async function locateKids() {

    if (new Date().toDateString() != midnight.toDateString()) {
        await updateMidnightSnapshot();
    }

    const deviceId = $('#kid-select').children('option:selected').val();

    const reportResponse = await fetch(`/api/user/${userId}/kids/report`);
    const report = await reportResponse.json();
    report.positions.forEach(p => {
        if (!path || p.deviceId != deviceId) {
            const kid = kids.find(k => k.deviceId == p.deviceId);
            if (kid) {
                const snapshot = report.snapshots.find(s => s.deviceId == p.deviceId);
                updateKidPopup(kid, p, snapshot, kid.snapshot, true, !path && kid.deviceId == deviceId);
            } else {
                // TODO if not found
            }
        }
    });
}

async function updateMidnightSnapshot() {

    midnight = moment().startOf('day').toDate();

    const snapshotResponse = await fetch(`/api/user/${userId}/kids/snapshot/${midnight.getTime()}`);
    const snapshot = await snapshotResponse.json();
    kids.forEach(k => {
        const kidSnapshot = snapshot.find(s => s.deviceId == k.deviceId);
        if (kidSnapshot) {
            k.snapshot = kidSnapshot;
        }
    });
}

window.addEventListener('load', async function onload() {

    const locale = navigator.language ? navigator.language.split('-')[0] : null;
    if (locale) {
        moment.locale(locale);
        i18n.setLocale(locale);
    }

    initHistory();
    initError();

	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
	}).addTo(map);

	map.setZoom(DEFAULT_ZOOM);

    // kids definition and location
	const kidsResponse = await fetch(`/api/user/${userId}/kids/info`);
    kids = await kidsResponse.json();
    $('#kid-select').html(kids.map(k => `<option value="${k.deviceId}">${k.name}</option>`).reduce((html, option) => html + option, ''));
    kids.forEach(k => {
        k.popup = L.popup({closeOnClick: false, autoClose: false, closeButton: false, autoPan: false}).setLatLng([0, 0]).addTo(map);
        k.popupTimeFromNow = true;
        k.circle = L.circle([0,0], 0, {weight: 0}).addTo(map);
    });

    // init pedometer to closest midnight
    await updateMidnightSnapshot();

    locateKids();
    setInterval(locateKids, KID_POSITION_QUERY_INTERVAL);

    // user definition and location
    const userResponse = await fetch(`/api/user/${userId}/info`);
    user = await userResponse.json();
    $('#user-name').text(user.name);
    user.marker = L.marker([0,0]).addTo(map);
    user.circle = L.circle([0, 0], 0, {weight: 0, color: 'green'}).addTo(map);

    view = 'user-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
});