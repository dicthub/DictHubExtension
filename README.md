[ ![Kotlin](https://img.shields.io/badge/Kotlin-1.3.20-green.svg) ](https://kotlinlang.org/)
[![Build Status](https://travis-ci.org/dicthub/DictHubExtension.svg?branch=master)](https://travis-ci.org/dicthub/DictHubExtension) 
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

# DictHubExtension

DictHubExtension is a browser extension aiming to bring convenient and consistent word query experience in modern 
browsers.

Internally, it fetches web page from translation/dictionary website, and shows result in same content page to avoid
opening new tab.

To support different translation/dictionary sources, a light-weight plugin system is built to help user customize the
the best translation preference.  


## Browser Support
*(Waiting for Edge to switch to Chromium!)*

![Chrome](https://raw.githubusercontent.com/alrra/browser-logos/master/src/chrome/chrome_48x48.png)
![Firefox](https://raw.githubusercontent.com/alrra/browser-logos/master/src/firefox/firefox_48x48.png)
![Edge](https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png)

## Language Support

DictHubExtension's translation is backed by translation/dictionary sources. 

With proper plugin, DictHubExtension can support any needed language.


## What's interesting?

DictHubExtension (and some plugins) is written mostly in `Kotlin` lang, and compile into `JavaScript`.


# How to use develop version

1. Run `./gradlew build` in root directory, unpacked extension is under `build/chrome` and `build/firefox` directory
2. Load unpacked version from build: 
   [Chrome](https://developer.chrome.com/extensions/getstarted#unpacked)
   [Firefox](https://developer.mozilla.org/en-US/Add-ons/WebExtensions/Temporary_Installation_in_Firefox) 