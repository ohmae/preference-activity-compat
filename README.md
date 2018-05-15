# PreferenceActivityCompat
[![license](https://img.shields.io/github/license/ohmae/preference-activity-compat.svg)](./LICENSE)
[![GitHub release](https://img.shields.io/github/release/ohmae/preference-activity-compat.svg)](https://github.com/ohmae/preference-activity-compat/releases)
[![GitHub issues](https://img.shields.io/github/issues/ohmae/preference-activity-compat.svg)](https://github.com/ohmae/preference-activity-compat/issues)
[![GitHub closed issues](https://img.shields.io/github/issues-closed/ohmae/preference-activity-compat.svg)](https://github.com/ohmae/preference-activity-compat/issues?q=is%3Aissue+is%3Aclosed)


This is a compatibility library of PreferenceActivity.

This class can be used in much the same way as PreferenceActivity.
Moreover, the material design is applied, It is also possible to manage PreferenceFragmentCompat instead of PreferenceFragment.

## Background

As you know,
[PreferenceFragmentCompat](https://developer.android.com/reference/android/support/v7/preference/PreferenceFragmentCompat)
which is a compatibility class corresponding to [PreferenceFragment](https://developer.android.com/reference/android/preference/PreferenceFragment)
is provided in SupportLibrary.
([com.android.support:preference-v7](https://developer.android.com/topic/libraries/support-library/packages#v7-preference))
But compatibility class corresponding to [PreferenceActivity](https://developer.android.com/reference/android/preference/PreferenceActivity) is not provided.

AppCompatPreferenceActivity is created as a template, but it can only use some APIs  and is not compatible with material design.
Also, since it can handle only native Fragment, it can not handle PreferenceFragmentCompat inheriting Fragment of support library.
There is also a Support Library version of [PreferenceFragment](https://developer.android.com/reference/android/support/v14/preference/PreferenceFragment)
that inherits the native Fragment, but the native Fragment is Deprecated.

## Screenshots

### Phone Android 7.1

#### PreferenceActivityCompat:smile:

|![](readme/7C1.png)|![](readme/7C2.png)|![](readme/7C3.png)|
|-|-|-|

#### Native PreferenceActivity:smile:

|![](readme/7N1.png)|![](readme/7N2.png)|![](readme/7N3.png)|
|-|-|-|

### Phone Android 4.4

#### PreferenceActivityCompat:smile:

|![](readme/4C1.png)|![](readme/4C2.png)|![](readme/4C3.png)|
|-|-|-|

#### Native PreferenceActivity:scream:

|![](readme/4N1.png)|![](readme/4N2.png)|![](readme/4N3.png)|
|-|-|-|


### Tablet Android 7.1

|PreferenceActivityCompat:smile:|Native PreferenceActivity:smile:|
|-|-|
|![](readme/7C4.png)|![](readme/7N4.png)|
|![](readme/7C5.png)|![](readme/7N5.png)|

### Tablet Android 4.4

|PreferenceActivityCompat:smile:|Native PreferenceActivity:scream:|
|-|-|
|![](readme/4C4.png)|![](readme/4N4.png)|
|![](readme/4C5.png)|![](readme/4N5.png)|

## How to use

## Limitation

## Author
大前 良介 (OHMAE Ryosuke)
http://www.mm2d.net/

## License
[MIT License](./LICENSE)
