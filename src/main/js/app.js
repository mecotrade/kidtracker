'use strict';

const moment = require('moment/min/moment-with-locales.min.js');
const createSliderControl = require('./slidercontrol.js');

const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const WATCH_OFF_ICON = '<svg class="bi bi-watch" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4 14.333v-1.86A5.985 5.985 0 0 1 2 8c0-1.777.772-3.374 2-4.472V1.667C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v1.86A5.985 5.985 0 0 1 14 8a5.985 5.985 0 0 1-2 4.472v1.861c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333zM13 8A5 5 0 1 0 3 8a5 5 0 0 0 10 0z"/><rect width="1" height="2" x="13.5" y="7" rx=".5"/><path fill-rule="evenodd" d="M8 4.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h1.5V5a.5.5 0 0 1 .5-.5z"/></svg>';
const LOW_BATTERY_ICON = '<svg class="bi bi-battery" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M12 5H2a1 1 0 0 0-1 1v4a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zM2 4a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2H2z"/><path d="M14.5 9.5a1.5 1.5 0 0 0 0-3v3z"/></svg>';
const SOS_ICON = '<svg class="bi bi-exclamation-octagon-fill" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M11.46.146A.5.5 0 0 0 11.107 0H4.893a.5.5 0 0 0-.353.146L.146 4.54A.5.5 0 0 0 0 4.893v6.214a.5.5 0 0 0 .146.353l4.394 4.394a.5.5 0 0 0 .353.146h6.214a.5.5 0 0 0 .353-.146l4.394-4.394a.5.5 0 0 0 .146-.353V4.893a.5.5 0 0 0-.146-.353L11.46.146zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z"/></svg>';
const LOST_ICON = '<svg class="bi bi-x-circle" width="20px" height="16px" viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M8 15A7 7 0 1 0 8 1a7 7 0 0 0 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z"></path><path fill-rule="evenodd" d="M11.854 4.146a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708-.708l7-7a.5.5 0 0 1 .708 0z"></path><path fill-rule="evenodd" d="M4.146 4.146a.5.5 0 0 0 0 .708l7 7a.5.5 0 0 0 .708-.708l-7-7a.5.5 0 0 0-.708 0z"></path></svg>'

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY HH:mm ddd';

const LOST_INTERVAL = 15 * 60 * 1000;

const userId = 1;

var map = L.map('map');

var user;
var kids;
var midnight;

var view = 'none';
var path = null;

$('#self-watch').on('click', function onLocateMe() {
    if (view == 'self') {
        $('#stop-self-watch-icon').show();
        $('#self-watch-icon').hide();
        view = 'none';
    } else {
        if (view == 'kid') {
            $('#stop-kid-watch-icon').show();
            $('#kid-watch-icon').hide();
        }
        $('#stop-self-watch-icon').hide();
        $('#self-watch-icon').show();
        view = 'self';

        map.setView(user.marker.getLatLng(), map.getZoom());
    }
});

$('#kid-watch').on('click', async function onLocateKid() {
    if (view == 'kid') {
        $('#stop-kid-watch-icon').show();
        $('#kid-watch-icon').hide();
        view = 'none';
    } else {
        if (view == 'self') {
            $('#stop-self-watch-icon').show();
            $('#self-watch-icon').hide();
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

$('#kid-path').on('click', async function onKidPath() {

    if (!path) {

        $('#kid-path-switch-icon').hide();
        $('#kid-geo-switch-icon').show();

        const deviceId = $('#kid-select').children('option:selected').val();

        const kid = kids.find(k => k.deviceId == deviceId);

        // TODO if not found

        const from = new Date(2020, 5, 19);
        const till = new Date(2020, 5, 20);

        const kidPathResponse = await fetch(`/api/device/${deviceId}/path/${from.getTime()}/${till.getTime()}`);
        const kidPath = await kidPathResponse.json();

        // TODO: if path is empty

        kidPath.map(p => [p.latitude, p.longitude])
        const track = L.polyline(kidPath.map(p => [p.latitude, p.longitude]), {dashArray: '4'}).addTo(map);

        const snapshotResponse = await fetch(`/api/user/${userId}/kids/snapshot/${from.getTime()}`);
        const snapshot = await snapshotResponse.json();
        const pathMidnightSnapshot = snapshot.find(s => s.deviceId == deviceId);

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
        map.removeControl(path.slider);
        map.removeLayer(path.track);
        path = null;

        $('#kid-path-switch-icon').show();
        $('#kid-geo-switch-icon').hide();

        locateKids();
    }
});

$('#kid-force-geo').on('click', async function onForceGeo() {
    const deviceId = $('#kid-select').children('option:selected').val();
    await fetch(`/api/device/${deviceId}/command/CR`);
});

$('#kid-find').on('click', async function onForceGeo() {
    const deviceId = $('#kid-select').children('option:selected').val();
    await fetch(`/api/device/${deviceId}/command/FIND`);
});

map.on('locationfound', function onLocationFound(e) {

    user.marker.setLatLng(e.latlng);
    user.circle.setLatLng(e.latlng).setRadius(e.accuracy);

    if (view == 'self' || view == 'self-once') {
        map.setView(e.latlng, map.getZoom());
        if (view == 'self-once') {
            view = 'none';
        }
    }

    $('#self-watch').attr('disabled', false);
});

map.on('locationerror', function onLocationError(e) {
    $('#self-watch').attr('disabled', true);
    console.error(e.message);
});

map.on('drag', function onMouseDrag(e) {
    $('#stop-self-watch-icon').show();
    $('#self-watch-icon').hide();
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

    if (kid.snapshot) {
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
    midnight = new Date();
    midnight.setHours(0, 0, 0, 0);
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
    }

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

    view = 'self-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
});