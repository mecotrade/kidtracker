'use strict';

const i18n = {

    locale: null,

    tokens: {
        ru: {
            'History': 'История',
            'Date': 'Дата',
            'Start': 'Начало',
            'End': 'Конец',
            'Warning': 'Предупреждение',
            'Error': 'Ошибка',
            'No data': 'Данных нет',
            'Time interval end {} is selected before time interval start {}': 'Конец интервала {} выбран раньше начала интервала {}',
            'Battery': 'Заряд',
            'Battery {}%': 'Заряд {}%',
            'Pedometer': 'Шагомер',
            '{} steps': '{} шагов',
            'Communication': 'Связь с ребенком',
            'Phone': 'Телефон',
            'Message': 'Сообщение',
            "Kid's watch will call or send a text message to provided phone number. Some characters might be sent incorrectly.": 'Часы ребенка совершат вызов или отправят СМС на указанный номер. Некоторые символы могут быть отправлены некорректно.',
            'Command is not completed.': 'Команда не выполнена'
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