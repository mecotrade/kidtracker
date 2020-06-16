'use strict';

const DEFAULT_MAX_ZOOM = 16;
const KID_POSITION_QUERY_INTERVAL = 10000;

var map = L.map('map');

var locationMarker;
var locationCircle;

var user;
var kids;

var kidTimerId;
var kidMarker;
var kidPopup;

var watchSelf = false;
var watchKid = false;

function startWatchSelf() {
    $('#stop-self-watch-icon').hide();
    $('#self-watch-icon').show();
    map.locate({
        watch: true,
        setView: true,
        maxZoom: Math.max(16, map.getZoom()),
        enableHighAccuracy: true
    });
    watchSelf = true;
    watchKid = false;
}

function startWatchKid() {
    $('#stop-kid-watch-icon').hide();
    $('#kid-watch-icon').show();

    const deviceId = $('#kid-select').children('option:selected').val();
    const kid = kids.find(k => k.deviceId == deviceId);

    async function locateKid() {
        const positionResponse = await fetch(`/api/device/${deviceId}/position/last`);
        const position = await positionResponse.json();
        const datetime = new Date(Date.parse(position.timestamp)).toLocaleString().split(',');
        const content = `<center><img src="${kid.thumb}" width="60"/><br/><b>${kid.name}</b><br/>${datetime[0].trim()}<br/>${datetime[1].trim()}</center>`

        if (!kidPopup) {
            kidPopup = L.popup({closeOnClick: false, autoClose: false, offset: [0, 0]});
        }

        kidPopup.setLatLng([position.latitude, position.longitude]).setContent(content).openOn(map);
        map.setView([position.latitude, position.longitude], map.getZoom(), { animation: true })
    }

    locateKid();

    kidTimerId = setInterval(locateKid, 10000);

    watchKid = true;
    watchSelf = false;
}

function stopWatch() {
    $('#stop-self-watch-icon').show();
    $('#self-watch-icon').hide();
    map.stopLocate();
    watchSelf = false;

    $('#stop-kid-watch-icon').show();
    $('#kid-watch-icon').hide();
    clearInterval(kidTimerId);
    watchKid = false;
}

$('#self-watch').on('click', function onLocateMe() {
    const doWatchSelf = !watchSelf;
    if (watchSelf || watchKid) {
        stopWatch();
    }
    if (doWatchSelf) {
        startWatchSelf();
    }
});

$('#kid-watch').on('click', async function onLocateKid() {
    const doWatchKid = !watchKid;
    if (watchSelf || watchKid) {
        stopWatch();
    }
    if (doWatchKid) {
        startWatchKid();
    }
});

map.on('locationfound', function onLocationFound(e) {

    if (!locationMarker) {
        locationMarker = L.marker(e.latlng).addTo(map);
    } else {
        locationMarker.setLatLng(e.latlng);
    }

    if (!locationCircle) {
        locationCircle = L.circle(e.latlng, e.accuracy, {weight: 0}).addTo(map);
    } else {
        locationCircle.setLatLng(e.latlng).setRadius(e.accuracy);
    }
});

map.on('locationerror', function onLocationError(e) {
    alert(e.message);
});

map.on('drag', function onMouseDrag(e) {
    stopWatch();
});

map.on('zoomend', function onZoomEnd(e) {
    if (watchSelf) {
        map.stopLocate();
        map.locate({
            watch: true,
            setView: true,
            maxZoom: Math.max(16, map.getZoom()),
            enableHighAccuracy: true
        });
    }
});

window.addEventListener('load', async function onload() {

    const userResponse = await fetch(`/api/user/info`);
    user = await userResponse.json();
    $('#user-name').val(user.name)

	const kidsResponse = await fetch(`/api/user/kids`);
    kids = await kidsResponse.json();
    $('#kid-select').html(kids.map(k => '<option value="' + k.deviceId + '">' + k.name + '</option>').reduce((html, option) => html + option, ''));

	map.locate({
        watch: false,
        setView: true,
        maxZoom: 16,
        enableHighAccuracy: true
	});
	L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
		attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
	}).addTo(map);
});