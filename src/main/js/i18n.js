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
            "Kid's watch will call or send a text message to provided phone number. Some characters might be sent incorrectly.": 'Часы ребенка совершат вызов или отправят SMS на указанный номер. Некоторые символы могут быть отправлены некорректно.',
            'Command is not completed.': 'Команда не выполнена',
            'Contacts': 'Контакты',
            'Edit': 'Редактирование',
            'Name': 'Имя',
            'Phone': 'Телефон',
            'Name should not be empty.': 'Имя должно быть заполнено.',
            'Phone should not be empty.': 'Телефон должен быть заполнен.',
            "Admin's phones, primary {} and secondary {}, which can manage kid's watch configuration through text messages": 'Телефоны администраторов, основного {} и дополнительного {}, имеющих возможность управлять телефоном ребенка при помощи SMS',
            "Kid's watch phone book, first two phones, {} and {}, can be called by long press of watch numbered buttons": 'Телефонная книга часов ребенка, первые два номера, {} и {}, могут быть вызваны долгим нажатием соответствующих кнопок на часах',
            'Phones where notification and alert text messages are being sent to': 'Телефоны на которые будут отправляться SMS с предупреждениями и оповещениями об опасности',
            "Phones which incoming call will be accepted from by the kid's watch": 'Телефоны входящие звонки с которых будут приняты на часах ребенка'
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
            const translation = this.translate(template).split('{}');
            return translation.reduce((message, part, idx) => message + part + (idx < params.length  && idx < translation.length - 1 ? params[idx] : ''), '');
        } else {
            return this.translate(template);
        }
    },

    setLocale: function(locale) {
        this.locale = locale;
    }
}

module.exports = i18n;