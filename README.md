# Scripter (DCU Third Year Project)

![Scripter logo](./code/Scripter/app/src/main/res/drawable/scripter_icon.png)

### What is Scripter?
Scripter is a task automation app for Android which gives you greater control over your device by allowing you to explain simple logic in JavaScript which can be used to run tasks automatically in the background. The app was written by [Amy Leitch]('https://github.com/amyleitch94') and me as our third year project while studying at DCU and is now completely open source.

### How does it work?
You can write scripts using JavaScript and our API (details of which are in the Help section of the app) in order to perform tasks on your device based on certain triggers. An example script can be found below:

```javascript
var dependencies = ["latitude", "longitude"];

if (withinMeterRadiusOf(15, 37.4223, -122.082)) {
  turnOnSilent();
}
```

This script listens for changes in the position of the device and when the current location of the device is within 15 meters of the Googleplex building in California (which exists at 37.4223, -122.082) it will turn silent mode on. This is an example of one of the many possible uses of Scripter!

### Contributors
* [Amy Leitch]('https://github.com/amyleitch94')
* Graham Bartley