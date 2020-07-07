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
            'Call control': 'Упарвление вызовами',
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
            "Phones which incoming call will be accepted from by the kid's watch": 'Телефоны входящие звонки с которых будут приняты на часах ребенка',
            'Phone numbers which can be quick called with buttons {} and {}': 'Телефонные номера доступные для быстрого вызова кнопками {} и {}',
            'Watch Settings': 'Настройки часов',
            'GPS refresh rate': 'Частота обновления координат',
            'Upload interval (sec)': 'Интервал обновления (сек)',
            'Work time after force GPS on (min)': 'Время работы после включения GPS (мин)',
            'SMS alerts': 'Оповещения о событиях',
            'Send SMS when SOS button pressed': 'Отправлять SMS когда нажата кнопка SOS',
            'Send SMS when watch is removed': 'Отправлять SMS когда часы сняты',
            'Send SMS when battery level is low': 'Отправлять SMS когда батарея разряжена',
            'Diverse': 'Разное',
            'Send voice message with button': 'Отправлять голосовые сообщения кнопкой',
            'Accept SMS commands to the watch': 'Принимать SMS команды на часах',
            'Activate pedometer': 'Включить шагомер',
            'Bluetooth': 'Bluetooth',
            'Activate Bluetooth': 'Включить Bluetooth',
            'Makefriend instead of call on button': 'Режим дружбы вместо вызова по кнопке',
            'Bluetooth name': 'Имя Bluetooth',
            'Bold time font': 'Жирный шрифт в часах',
            'Activate contact list': 'Включить список контактов',
            'Regional settings': 'Региональные настройки',
            'Watch Time': 'Время часов',
            'Timezone': 'Часовой пояс',
            '(GMT-12:00) International Date Line West': '(GMT-12:00) Линия перемены даты',
            '(GMT-11:00) Midway Island, Samoa': '(GMT-11:00) Мидуэй, Самоа',
            '(GMT-10:00) Hawaii': '(GMT-10:00) Гавайи',
            '(GMT-09:00) Alaska': '(GMT-09:00) Аляска',
            '(GMT-08:00) Pacific Time (US & Canada)': '(GMT-08:00) Тихоокеанское время (США и Канада)',
            '(GMT-07:00) Mountain Time (US & Canada)': '(GMT-07:00) Горное время (США и Канада)',
            '(GMT-06:00) Central Time (US & Canada)': '(GMT-06:00) Центральное время (США и Канада)',
            '(GMT-05:00) Eastern Time (US & Canada)': '(GMT-05:00) Восточное время (США и Канада)',
            '(GMT-04:00) Atlantic Time (Canada)': '(GMT-04:00) Атлантическое время (Канада)',
            '(GMT-03:30) Newfoundland': '(GMT-03:30) Ньюфаундленд',
            '(GMT-03:00) Brasilia, Greenland': '(GMT-03:00) Бразилиа, Гренландия',
            '(GMT-02:00) Mid-Atlantic': '(GMT-02:00) Атлантическое время',
            '(GMT-01:00) Azores, Cape Verde Is.': '(GMT-01:00) Азорские острова, о. Кабо-Верде',
            '(GMT+00:00) London, Reykjavik': '(GMT+00:00) Лондон, Рейкьявик',
            '(GMT+01:00) Berlin, Rome, Paris': '(GMT+01:00) Берлин, Рим, Париж',
            '(GMT+02:00) Minsk, Helsinki': '(GMT+02:00) Минск, Хельсинки',
            '(GMT+03:00) Moscow, St. Petersburg': '(GMT+03:00) Москва, Санкт-Петербург',
            '(GMT+03:30) Tehran': '(GMT+03:30) Тегеран',
            '(GMT+04:00) Baku, Yerevan': '(GMT+04:00) Баку, Ереван',
            '(GMT+04:30) Kabul': '(GMT+04:30) Кабул',
            '(GMT+05:00) Yekaterinburg, Tashkent': '(GMT+05:00) Екатеринбург, Ташкент',
            '(GMT+05:30) Mumbai, New Delhi': '(GMT+05:30) Мумбаи, Нью-Дели',
            '(GMT+05:45) Kathmandu': '(GMT+05:45) Катманду',
            '(GMT+06:00) Novosibirsk, Almaty': '(GMT+06:00) Новосибирск, Алматы',
            '(GMT+06:30) Yangon (Rangoon)': '(GMT+06:30) Янгон (Рангун)',
            '(GMT+07:00) Krasnoyarsk': '(GMT+07:00) Красноярск',
            '(GMT+08:00) Irkutsk, Beijing, Hong Kong': '(GMT+08:00) Иркутск, Пекин, Гонконг',
            '(GMT+09:00) Yakutsk, Tokyo': '(GMT+09:00) Якутск, Токио',
            '(GMT+09:30) Adelaide, Darwin': '(GMT+09:30) Аделаида, Дарвин',
            '(GMT+10:00) Vladivostok, Melbourne, Sydney': '(GMT+10:00) Владивосток, Мельбурн, Сидней',
            '(GMT+11:00) Magadan, New Caledonia, Solomon Is.': '(GMT+11:00) Магадан, Новая Каледония, Соломоновы Острова',
            '(GMT+12:00) Kamchatka, Fiji, Marshall Is.': '(GMT+12:00) Камчатка, Фиджи, Маршалловы острова',
            "(GMT+13:00) Nuku'alofa": '(GMT+13:00) Нукуалофа',
            'Interface language': 'Язык интерфейса',
            'English': 'Английский',
            'Chinese': 'Китайский',
            'Portuguese': 'Португальский',
            'Spanish': 'Испанский',
            'Deutsch': 'Немецкий',
            'Turkiye': 'Турецкий',
            'Vietnam': 'Вьетнамский',
            'Russian': 'Русский',
            'Francais': 'Французский',
            'Reminders': 'Будильники',
            'Reminder 1': 'Будильник 1',
            'Reminder 2': 'Будильник 2',
            'Reminder 3': 'Будильник 3',
            'Input token': 'Ввод кода',
            'One-time SMS token': 'Одноразовый SMS-код',
            'Once': 'Один раз',
            'Everyday': 'Ежедневно',
            'Select days': 'Выбрать дни',
            'Available': 'Доступно',
            'Selected': 'Выбрано',
            'Sunday': 'Воскресенье',
            'Monday': 'Понедельник',
            'Tuesday': 'Вторник',
            'Wednesday': 'Среда',
            'Thursday': 'Четверг',
            'Friday': 'Пятница',
            'Saturday': 'Суббота',
            'Active': 'Активен',
            'Danger zone': 'Опасные действия',
            'Reboot watch': 'Перезагрузить часы',
            'Switch the watch off': 'Выключить часы',
            'Reset watch to factory config': 'Сбросить все настройки'
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