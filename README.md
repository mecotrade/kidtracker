# Kid Tracker

## Motivation

The project includes both server side and web application designed to communicate with 
smart baby watch devices of different models and known under various brands. For example, 
it can work with Q50, Q60, Q80, Q90, Q100, Q360, Q523, Q730, Q750, Q8, GW100, GW100S, GW200, 
GW200S, GW300, GW300S, GW400S, GW400X, GW500S, GW600S, GW700, GW800, GW900, GW900S, GW1000, 
GW1000S, EW100, EW100S, EW200, K911, W8, W9, W10, Y3, G36 SAFE KEEPER, DS18, T58, T100, I8, 
G10, G100, D99, D100, D100S, and many others . Roughly speaking, it does the same as
such applications as [SeTracker](https://setracker.org/), [FindMyKids](https://findmykids.org/), 
and so on. The main difference is that all personal data, like contacts, positions, 
communication history, and so on, becomes truly private, since it is stored at the machine 
controlled by user.

Another issue which was addressed is the security model of smart baby watch applications.
By default, any person could take control over the device provided the device identifier
is known, with no further confirmation of being a device owner. The identifier, in its turn, 
can be easily obtained by sending a text message to the device if the device cellphone number 
is known. There is a password, of course, but all devices has the very same factory password
and there is no annoying notifications asking to change it, therefore no one does that.

In this application the following security scenarios have been implemented: 
1. Device assignment is protected by two-factor authentication: both the device identifier, and a 
four-digit token sent to the device is required to assign a device to a user.
2. There is a set of "protected" operations, such as turn the device off, reset to factory settings, 
unassign the device, and settings the device password, which are protected by token sent 
in a text message to user cellphone.
3. Users are not allowed to modify their cellphone number once it is entered on creation time
to protect devices from unwilling commands even if user account is compromised.
4. Any user having access to a device can list all other user having access to this device.

And last, but not least, it is free of change and ads :).

## Getting started

Building the project is as simple as
```bash
docker-compose -f docker-builder.yml run --rm builder
```
While building, an SSL certificate required for `https` connections is created. It is possible 
to specify a domain and/or an ip address using environment variables `DOMAIN` and `IP`
```bash
docker-compose -f builder/docker-compose.yml run --rm -e DOMAIN=example.com -e IP=123.45.67.89 builder
```

The application can be started with command
```bash
docker-compose up -d
```

To log in, use default credentials `admin`/`password`.

## Device assignment

To assign a device to a user registered to the application, the application should be available 
from public networks, that is, it should have a public ip address, and, probably,
an associated domain name. By default, the application listens devices on port `8001`,
suppose the very same port is mapped to public network. Then the following text message
sent to device make it start connecting to the application:
```
pw,123456,ip,<public IP address or domain>,8001#
```
Here `123456` is the default password, which is most probably set on the device. If, however, 
the password was changed, the new password should be used instead of the default one. 

**It is highly recommended changing the default password!**

Then click button ![smile](images/smile.svg) on the right-hand side of the navigation 
bar and then click ![person-plus](images/person-plus.svg) button on the footer. The device
identifier is required for device to be assigned. Once the device is connected to 
the application, four-digit confirmation token is sent to the device. To confirm user owns 
the device, this token should be entered to the pop-up form during next 5 minutes, 
after that time token becomes invalid. 

## Usage notices

### User roles
Any user registered to the application can either be a regular user, or an administrator. 
Administrators can register other users, whereas regular users cannot, this is the only 
difference. Administrators can create both regular users and other administrators. For existing
user, administrator privilege can be neither granted nor revoked, the only way to modify it
is to remove user account, and recreated it anew.

For security reason, the user cellphone number cannot be modified neither. Again, to change it
the user account is to be removed and recreate. To remove user account, all devices have to
be unassigned from it.

When run for the very first time, there is a default user account with administrator privilege with 
login `admin` and password `password`. It is recommended either removing this account when other
user accounts are created, or changing the default password. If there is only one account with 
administrator privilege, it cannot be removed. The default administrator account has no valid
cellphone number, and no device can be assigned to it.

### Communication with device
Devices communicate to the application by means of messages. There are several types of messages,
some can provide information about the device location and alerts, others contain actual battery
charge and pedometer value. Some messages can also be sent by the application to a device to make
it perform several actions or modify its settings.

### User interface

#### Device markers
Position markers of all assigned devices together with user position marker share the same
map. Device marker contains information about time of last known location, 
battery charge, pedometer value, and eventually the device take off alert 
![smartwatch](images/smartwatch.svg), low battery alert ![battery](images/battery.svg), 
and connection lost alert ![x-circle](images/x-circle.svg). The device is considered 
to be lost if the last message from the device was received more than 15 minutes ago. 

Notice that the low battery alert ![battery](images/battery.svg), and actual charge value
come in different types of messages. Messages with actual charge come more frequently 
(each 5 minutes) than messages with locations and alerts, which can be not sent for hours.
Therefore, it is not uncommon a marker shows 100% of battery charge together with
low battery alert. In this case the priority is for numerical value of the battery charge and 
low battery alert can be ignored.

Device sends alarm messages when the SOS button is pressed. The device marker 
becomes red in this case, and a siren sound is played until the marker is clicked. 
Marker clicking results moving it on top and switching kid select. When kid select is switched,
the selected device marker goes on top.

#### Timestamps
Anywhere in UI timestamps are clickable, and by a click can be switched between absolute date 
and time value and time interval from now.

#### Map view
There are two icons which have two states, filled and wired. First one ![eye](images/eye.svg)
becomes filled ![eye-fill](images/eye-fill.svg) when map view follows selected device marker. 
Its state can be toggle by clicking on it. Another one ![cursor](images/cursor.svg) becomes 
filled ![cursor-fill](images/cursor-fill.svg) when map view follows user position marker. 
Its state can also be toggled by clicking on it. When one icon becomes filled another becomes 
wired and vice-versa. Both icons become wired when map is dragged.

#### Wake up GPS
By clicking on ![search](images/search.svg) a command to wake up GPS and provide the current 
position is sent to the device. The map view starts follow the device marker, 
the follow device icon ![eye](images/eye.svg) becomes filled.

#### Chat dialog
Chat with device ![chat-dots](images/chat-dots.svg) is asymmetric. Text chat messages can be
sent to device with button ![chat-text](images/chat-text.svg), whereas the device can send 
short audio messages and, if device is equipped with camera, snapshots.
Device can also be forced to take a snapshot with button ![camera](images/camera.svg)
or make a 15 seconds audio record with button ![voicemail](images/voicemail.svg).


#### History dialog
History dialog ![calendar-week](images/calendar-week.svg) allows choosing time interval in two
modes, the date, from one midnight to the next one, and general, where both the start, 
and the end of the interval can be picked by user. To switch between modes click on date and time
picker labels. 

When time interval is picked, the chat history ![chat-dots](images/chat-dots.svg), or pedometer 
and battery charge graph ![graph-up](images/graph-up.svg) can be shown. 
The device track for picked time interval ![map](images/map.svg) can be put on the map as well. 
A slider appears at the right top corner, it is used to move device 
marker along the track. When history track is inspected, the history dialog icon 
![calendar-week](images/calendar-week.svg) is changed to ![geo-alt](images/geo-alt.svg). 
To remove the track and get back to online marker positions, click on it. 

#### Contacts dialog
Contacts dialog ![people](images/people.svg) allows editing primary and secondary
device administrator ![shield-check](images/shield-check.svg), SOS numbers 
![alert-octagon](images/alert-octagon.svg), device contacts 
![people-circle](images/people-circle.svg), numbers allowed to make calls to the
device ![telephone-outbound](images/telephone-outbound.svg), and quick call numbers
![smartwatch](images/smartwatch.svg) assigned to device buttons. For all contact categories 
there is a fixed number of slots which can be filled. By default, empty slots are hidden, 
but can be shown by clicking on ![list](images/list.svg) button at the dialog footer.

#### Kids dialog 
Kids dialog ![smile](images/smile.svg) allows user to assign and unassign kid devices, 
change thumb, and obtain some general information about the device and its actual status. 
In the left-hand side column a clickable thumb is placed, clicking on it results the
kid edit dialog. In the middle column there is the device identifier. If the device is online, 
its identifier is in green, otherwise it is in red. Below the device identifier there is 
the time when last message was received from the device.
All users having access to the device are listed at the rightmost column with their cellphones.

## References

- [**GPRS communication protocol by 3g-elec**](https://github.com/tananaev/traccar/files/213814/3g.elec.comm.protocol.docx)
- [**A19 communication protocol**](https://www.istartek.com/wp-content/uploads/2019/03/A19-Communication-Protocol.pdf)
- [**Q50 SMS Codes**](http://deneysel-nadir.blogspot.com/2018/02/q50-sms-codes-q50-smart-watch-q50-sms.html)