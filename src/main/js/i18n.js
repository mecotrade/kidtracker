'use strict';

const i18n = {

    locale: null,

    tokens: {
        ru: {
            'History': 'История',
            'Date': 'Дата',
            'Start': 'Начало',
            'End': 'Конец',
            'Close': 'Закрыть',
            'Error': 'Ошибка',
            'No data': 'Данных нет',
            'Time interval end {} is selected before time interval start {}': 'Конец интервала {} выбран раньше начала интервала {}'
        }
    },

    translate: function(token) {

        const translations = this.locale ? this.tokens[this.locale] : null;
        if (translations) {
            const translation = translations[token];
            return translation ? translation : token;
        }
    },

    apply: function(element) {
        element.text(this.translate(element.text()));
    },

    applyAll: function(elements) {
        elements.forEach(e => this.apply(e));
    },

    format: function(template, params) {
        if (params) {
            params = Array.isArray(params) ? params : [params];
            return this.translate(template).split('{}')
                    .reduce((message, part, idx) => message + part + (idx < params.length ? params[idx] : ''), '');
        } else {
            return this.translate(template);
        }
    },

    setLocale: function(locale) {
        this.locale = locale;
    }
}

module.exports = i18n;