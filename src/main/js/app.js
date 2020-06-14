var map = L.map('map');

var locationMarker;
var locationCircle;

var watching = false;

$('#locate-me').on('click', function onLocateMe() {
    if (watching) {
        $('#stop-watch-icon').show();
        $('#watch-icon').hide();
        map.stopLocate();
        watching = false;
    } else {
        $('#stop-watch-icon').hide();
        $('#watch-icon').show();
        map.locate({
            watch: true,
            setView: true,
            maxZoom: 16,
            enableHighAccuracy: true
        });
        watching = true;
    }
});

map.on('locationfound', function onLocationFound(e) {
    var radius = e.accuracy;

    if (locationMarker) locationMarker.remove();
    if (locationCircle) locationCircle.remove();

    locationMarker = L.marker(e.latlng).addTo(map);
    locationCircle = L.circle(e.latlng, radius).addTo(map);
});

map.on('locationerror', function onLocationError(e) {
    alert(e.message);
});

map.on('drag', function onMouseDrag(e) {
    $('#stop-watch-icon').show();
    $('#watch-icon').hide();
    map.stopLocate();
    watching = false;
});

window.addEventListener('load', function onload() {
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