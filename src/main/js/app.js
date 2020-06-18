'use strict';

var moment = require('moment/min/moment-with-locales.min.js');

const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

const BATTERY_LOW_THRESHOLD = 20;
const BATTERY_FULL_THRESHOLD = 70;

const WATCH_ON_ICON = '<svg class="bi bi-watch" width="1.5em" height="1em" style="padding-right: 0.5em" viewBox="0 0 16 16" fill="green" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4 14.333v-1.86A5.985 5.985 0 0 1 2 8c0-1.777.772-3.374 2-4.472V1.667C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v1.86A5.985 5.985 0 0 1 14 8a5.985 5.985 0 0 1-2 4.472v1.861c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333zM13 8A5 5 0 1 0 3 8a5 5 0 0 0 10 0z"/><rect width="1" height="2" x="13.5" y="7" rx=".5"/><path fill-rule="evenodd" d="M8 4.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h1.5V5a.5.5 0 0 1 .5-.5z"/></svg>';
const WATCH_OFF_ICON = '<svg class="bi bi-watch" width="1.5em" height="1em"  style="padding-right: 0.5em" viewBox="0 0 16 16" fill="red" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M4 14.333v-1.86A5.985 5.985 0 0 1 2 8c0-1.777.772-3.374 2-4.472V1.667C4 .747 4.746 0 5.667 0h4.666C11.253 0 12 .746 12 1.667v1.86A5.985 5.985 0 0 1 14 8a5.985 5.985 0 0 1-2 4.472v1.861c0 .92-.746 1.667-1.667 1.667H5.667C4.747 16 4 15.254 4 14.333zM13 8A5 5 0 1 0 3 8a5 5 0 0 0 10 0z"/><rect width="1" height="2" x="13.5" y="7" rx=".5"/><path fill-rule="evenodd" d="M8 4.5a.5.5 0 0 1 .5.5v3a.5.5 0 0 1-.5.5H6a.5.5 0 0 1 0-1h1.5V5a.5.5 0 0 1 .5-.5z"/></svg>';

const KID_POPUP_TIME_FORMAT = 'D MMMM YYYY, HH:mm, dddd';

const userId = 1;

var map = L.map('map');

var user;
var kids;

var view = 'none';

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

        locateKids();
    }
});

map.on('locationfound', function onLocationFound(e) {

    console.debug('self location found');

    user.marker.setLatLng(e.latlng);
    user.circle.setLatLng(e.latlng).setRadius(e.accuracy);

    if (view == 'self' || view == 'self-once') {
        map.setView(e.latlng, map.getZoom());
        if (view == 'self-once') {
            view = 'none';
        }
    }
});

map.on('locationerror', function onLocationError(e) {
    alert(e.message);
});

map.on('drag', function onMouseDrag(e) {
    $('#stop-self-watch-icon').show();
    $('#self-watch-icon').hide();
    $('#stop-kid-watch-icon').show();
    $('#kid-watch-icon').hide();
    view = 'none';
});

async function locateKids() {
    const kidsPositionResponse = await fetch(`/api/user/${userId}/kids/position`);
    const kidsPosition = await kidsPositionResponse.json();
    kidsPosition.forEach(p => {
        // TODO if not found
        const kid = kids[p.deviceId];
        const date = new Date(Date.parse(p.timestamp));
        const datetime = kid.popupTimeFromNow ? moment(date).fromNow() : moment(date).format(KID_POPUP_TIME_FORMAT);
        const watchIcon = p.takeOff ? WATCH_OFF_ICON : WATCH_ON_ICON;
        const batteryClass = p.battery < BATTERY_LOW_THRESHOLD ? 'battery-low' : (p.battery < BATTERY_FULL_THRESHOLD ? 'battery-half' : 'battery-full');
        const content = `<center><img src="${kid.thumb}" width="80"/><div class="kid-popup-name">${watchIcon}<b>${kid.name}</b></div><div id="kid-popup-${kid.deviceId}" class="kid-popup-time">${datetime}</div><div><span class="kid-popup-steps">${p.pedometer}</span><span class="${batteryClass}">${p.battery}%</span></div></center>`;
        kid.popup.setContent(content).setLatLng([p.latitude, p.longitude]);
        kid.circle.setLatLng([p.latitude, p.longitude]).setRadius(p.accuracy);

        $(`#kid-popup-${kid.deviceId}`).on('click', function() {
            if (kid.popupTimeFromNow) {
                $(`#kid-popup-${kid.deviceId}`).text(moment(date).format(KID_POPUP_TIME_FORMAT));
                kid.popupTimeFromNow = false;
            } else {
                $(`#kid-popup-${kid.deviceId}`).text(moment(date).fromNow());
                kid.popupTimeFromNow = true;
            }
        });
    });

    if (view == 'kid' || view == 'kid-once') {
        const deviceId = $('#kid-select').children('option:selected').val();
        map.setView(kids[deviceId].popup.getLatLng());
        if (view == 'kid-once') {
            view = 'none';
        }
    }
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
        k.circle = L.circle([0,0], 0, {weight: 0, color: 'green'}).addTo(map);
    });
    kids = kids.reduce((m, k) => { m[k.deviceId] = k; return m;}, {});

    locateKids();
    setInterval(locateKids, KID_POSITION_QUERY_INTERVAL);

    // user definition and location
    const userResponse = await fetch(`/api/user/${userId}/info`);
    user = await userResponse.json();
    $('#user-name').text(user.name);
    user.marker = L.marker([0,0]).addTo(map);
    user.circle = L.circle([0, 0], 0, {weight: 0}).addTo(map);

    view = 'self-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
});