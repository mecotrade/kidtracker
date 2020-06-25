/* Based on LeafletSlider plugin https://github.com/dwilhelm89/LeafletSlider */

'use strict';

L.Control.SliderControl = L.Control.extend({
    options: {
        position: 'topright',
        alwaysShowDate: false,
        length: null,
        slide: null,
    },

    initialize: function (options) {
        L.Util.setOptions(this, options);
        this._layer = this.options.layer;
    },

    setPosition: function (position) {
        var map = this._map;

        if (map) {
            map.removeControl(this);
        }

        this.options.position = position;

        if (map) {
            map.addControl(this);
        }
        this.startSlider();
        return this;
    },

    onAdd: function (map) {
        this.options.map = map;

        let opt = this.options;

        // Create a control sliderContainer with a jquery ui slider
        var sliderContainer = L.DomUtil.create('div', 'slider', this._container);
        $(sliderContainer).append('<div id="leaflet-slider" style="width:200px;margin-top:25px"><div class="ui-slider-handle"></div><div id="slider-timestamp" style="width:200px;margin-top:-25px; background-color:#FFFFFF; text-align:center; border-radius:5px;"></div></div>');

        //Prevent map panning/zooming while using the slider
        $(sliderContainer).mousedown(function () {
            map.dragging.disable();
        });

        $(document).mouseup(function () {
            map.dragging.enable();
            //Hide the slider timestamp if not range and option alwaysShowDate is set on false
            if (!opt.alwaysShowDate) {
                $('#slider-timestamp').html('');
            }
        });

        return sliderContainer;
    },

    onRemove: function (map) {
        $('#leaflet-slider').remove();

        // unbind listeners to prevent memory leaks
        $(document).off("mouseup");
        $(".slider").off("mousedown");
    },

    value: function () {
        return $("#leaflet-slider").slider('value');
    },

    startSlider: function () {
        let opt = this.options;
        function doSlide(i) {
            let timestamp = opt.slide(i);
            if (timestamp) {
                $('#slider-timestamp').html(timestamp);
            }
        }
        $("#leaflet-slider").slider({
            min: 0,
            max: opt.length - 1,
            step: 1,
            slide: function (e, ui) {
                doSlide(ui.value);
            }
        });
        doSlide(0);
    }
});

function createSliderControl(options) {
    return new L.Control.SliderControl(options);
};

module.exports = createSliderControl;