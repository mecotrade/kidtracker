# Changelog
All changes to the project will be documented in this file.

## v2.0.0 - 24 May 2021
- PostgreSQL can be used as backed database as well as H2 (embedded),
  however, migration on the fly is not supported: if switched to
  PostgreSQL, all users and devices can be created anew.
- All users tracking the device are notified when another user 
  starts tracking this device.
- Builder script bugs fixed.
- Device chat notification bugs fixed.

## v1.3.1 - 18 Dec 2020
- Fixed few reports and status update bugs.

## v1.3.0 - 6 Nov 2020
- Device chat messages, location reports and status updates are now 
  communicated with WebSockets, so any device events 
  are delivered to the application as fast as possible,
- When new chat message arrives, corresponding device marker becomes blue, 
  and vintage phone ring sound is played, no message will be missed anymore,
- Access Points names containing commas are handled properly while parsing 
  location messages, Various bugs are fixed.

## v1.2.0 - 15 Oct 2020
- Message receiving confirmed by device
- Remember me in login page
- Non-clickable timestamp bug fixed
- On/Off Wi-Fi config added

## v1.1.0 - 28 Jul 2020
- Alert when location is obsolete
- Resize kid's images, so large images upload is possible
- Start/stop debug and IP address change commands added

## v1.0.0 - 25 Jul 2020
- First public release