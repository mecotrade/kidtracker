'use strict';

const DEFAULT_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

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
        const datetime = new Date(Date.parse(p.timestamp)).toLocaleString().split(',');
        const content = `<center><img src="${kid.thumb}" width="60"/><br/><b>${kid.name}</b><br/>${datetime[0].trim()}<br/>${datetime[1].trim()}</center>`;
        kid.popup.setContent(content).setLatLng([p.latitude, p.longitude]);
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

	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
	}).addTo(map);

	map.setZoom(DEFAULT_ZOOM);

    // kids definition and location
	const kidsResponse = await fetch(`/api/user/${userId}/kids/info`);
    kids = await kidsResponse.json();
    $('#kid-select').html(kids.map(k => `<option value="${k.deviceId}">${k.name}</option>`).reduce((html, option) => html + option, ''));
    // TODO: add kid circle
    kids.forEach(k => k.popup = L.popup({closeOnClick: false, autoClose: false, closeButton: false}).setLatLng([0, 0]).addTo(map));
    kids = kids.reduce(function(m, k) { m[k.deviceId] = k; return m;}, {});

    locateKids();
    setInterval(locateKids, KID_POSITION_QUERY_INTERVAL);

    // user definition and location
    const userResponse = await fetch(`/api/user/${userId}/info`);
    user = await userResponse.json();
    $('#user-name').val(user.name);
    user.marker = L.marker([0,0]).addTo(map);
    user.circle = L.circle([0, 0], 0, {weight: 0}).addTo(map);

    view = 'self-once';
    map.locate({
        watch: true,
        setView: false,
        enableHighAccuracy: true
    });
});