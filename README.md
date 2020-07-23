# Kid Tracker

## 1. Motivation

The project includes both server side and web application designated to communicate with 
smart baby watch devices of different models and known under various brands. For example, 
it can work with Q50, Q60, Q80, Q90, Q100, Q360, Q523, Q730, Q750, Q8, GW100, GW100S, GW200, 
GW200S, GW300, GW300S, GW400S, GW400X, GW500S, GW600S, GW700, GW800, GW900, GW900S, GW1000, 
GW1000S, EW100, EW100S, EW200, K911, W8, W9, W10, Y3, G36 SAFE KEEPER, DS18, T58, T100, I8, 
G10, G100, D99, D100, D100S, and many others . Roughly speaking, it does the same as
such applications as [SeTracker](https://setracker.org/), [FindMyKids](https://findmykids.org/), and so on. 
The main difference is that all personal data, like phone book contacts, geolocation data, 
communication history, becomes truly private, since it is stored at the machine 
controlled by user.

Another issue which was addressed is the security model of smart baby watch applications.
By default, any person could take control over the device provided he or she knows the device
identifier, with no further confirmation of being a device owner. The identifier, in its turn, 
can be easily obtained by sending a text message to the watch if the watch cellphone number 
is known. There is a password, of course, but all devices has the very same factory password
and there is no annoying notifications asking to change it, therefore no one does that.

In this application the following security scenarios have been implemented: 
1. Device binding is protected by two-factor authentication: both the device identifier, and a 
four-digit token sent to the device is required to bind a device to a user.
2. There is a set of "protected" operations, such as turn the device off, reset to factory settings, 
unbind the device, and so on, which are protected by token sent as a text message to user's cellphone.
3. Users are not allowed to modify their cellphone number once it is entered on creation 
to protect devices from unwilling commands even if user account is compromised.
4. Any user having access to a device can list all other user having access to this device.

And last, but not least, it is free of change and ads :).

## 2. Getting started

Building the project is as simple as
```bash
docker-compose -f builder/docker-compose.yml run --rm builder
```
While building, an SSL certificate required for `https` connections is being created. 
It is possible to specify a domain and/or an ip address using environment variables `DOMAIN` and `IP`
```bash
docker-compose -f builder/docker-compose.yml run --rm -e DOMAIN=example.com -e IP=123.45.67.89 builder
```

The application can be started with command
```bash
docker-compose up -d
```

## 3. Device binding

To bind a device to the application, the latter is to be available from the
mobile internet, this is, it should has a static ip address, and, probably,
an associated domain name. By default, the application listens devices on port `8001`,
suppose the very same port is mapped to public network. Then the following text message
sent to device make to connect to the application:
```
pw,123456,ip,<public IP address>,8001#
```
Here `123456` is the default password, which is most probably set on the device. If, however, the password
was modified, it should be used instead of the default one. 

**It is highly recommended changing the default password!**

## 4. References

- [**GPRS communication protocol by 3g-elec**](https://github.com/tananaev/traccar/files/213814/3g.elec.comm.protocol.docx)
- [**A19 communication protocol**](https://www.istartek.com/wp-content/uploads/2019/03/A19-Communication-Protocol.pdf)
- [**Q50 SMS Codes**](http://deneysel-nadir.blogspot.com/2018/02/q50-sms-codes-q50-smart-watch-q50-sms.html)