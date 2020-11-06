/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.mecotrade.kidtracker.util;

public class ValidationUtils {

    private final static String PHONE_NUMBER_REGEX = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$";

    private final static String DATE_REGEX = "^\\d{4}\\.(0[1-9]|1[0-2])\\.(0[1-9]|[1-2][0-9]|3[0-1])$";

    private final static String DOT_TIME_REGEX = "^([0-1]?[0-9]|2[0-3])\\.[0-5][0-9]\\.[0-5][0-9]$";

    private final static String TIME_REGEX = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$";

    private final static String NUMBER_REGEX = "^[0-9]+$";

    private final static String SWITCH_REGEX = "^[01]$";

    private final static String REMINDER_TYPE_REGEX = "^([12]|[01]{7})$";

    private final static String PROFILE_REGEX = "^([1234])$";

    private final static String PASSWORD_REGEX = "^\\d{6}$";

    // only acceptable languages:
    //  0:English,
    //  1:Chinese,
    //  3:Portuguese
    //  4:Spanish
    //  5:Deutsch
    //  7:Turkiye
    //  8:Vietnam
    //  9:Russian
    //  10:Francais
    private final static String LANGUAGE_CODE_REGEX = "^(0|1|3|4|5|7|8|9|10)$";

    private final static String TIMEZONE_REGEX = "^(-12|-11|-10|-9|-8|-7|-6|-5|-4|-3\\.50|-3|-2|-1|0|1|2|3|3\\.50|4|4\\.30|5|5\\.50|5\\.75|6|6\\.50|7|8|9|9\\.50|10|11|12|13)$";

    private final static String IP_ADDRESS_REGEX = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    // this is not standard regex for mac address, however, some devices reduce 0x part to simply x
    private final static String MAC_ADDRESS_REGEX = "^([0-9A-Fa-f]{1,2}[:-]){5}([0-9A-Fa-f]{1,2})$";

    private final static String HOSTNAME_REGEX = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    private final static String PORT_REGEX = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$";

    public static boolean isValid(String string, String pattern) {
        return string != null && string.matches(pattern);
    }

    public static boolean isValidPhone(String phone) {
        return isValid(phone, PHONE_NUMBER_REGEX);
    }

    public static boolean isValidDate(String date) {
        return isValid(date, DATE_REGEX);
    }

    public static boolean isValidDotTime(String time) {
        return isValid(time, DOT_TIME_REGEX);
    }

    public static boolean isValidTime(String time) {
        return isValid(time, TIME_REGEX);
    }

    public static boolean isValidNumber(String number) {
        return isValid(number, NUMBER_REGEX);
    }

    public static boolean isValidSwitch(String sw) {
        return isValid(sw, SWITCH_REGEX);
    }

    public static boolean isValidReminderType(String reminderType) {
        return isValid(reminderType, REMINDER_TYPE_REGEX);
    }

    public static boolean isValidProfile(String profile) {
        return isValid(profile, PROFILE_REGEX);
    }

    public static boolean isValidLanguageCode(String languageCode) {
        return isValid(languageCode, LANGUAGE_CODE_REGEX);
    }

    public static boolean isValidTimezone(String timezone) {
        return isValid(timezone, TIMEZONE_REGEX);
    }

    public static boolean isValidPassword(String password) {
        return isValid(password, PASSWORD_REGEX);
    }

    public static boolean isValidIpAddress(String ipAddress) {
        return isValid(ipAddress, IP_ADDRESS_REGEX);
    }

    public static boolean isValidMacAddress(String macAddress) {
        return isValid(macAddress, MAC_ADDRESS_REGEX);
    }

    public static boolean isValidHostname(String hostName) {
        return isValid(hostName, HOSTNAME_REGEX);
    }

    public static boolean isValidHost(String host) {
        return isValidIpAddress(host) || isValidHostname(host);
    }

    public static boolean isValidPort(String port) {
        return isValid(port, PORT_REGEX);
    }
}
