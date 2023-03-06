# amazon-sidewalk-android-samples

## Building the Sidewalk Sample App
1. Open Sidewalk Sample App project with Android Studio.
2. Follow the Register for Login with Amazon flow documented on [developer.amazon.com](https://developer.amazon.com/docs/login-with-amazon/register-android.html) to obtain an API Key for your bundle ID and security profile.
3. Replace the content in `sample/src/main/assets/api_key.txt` with the API Key.
4. Download the Login with Amazon SDK from [developer.amazon.com](https://developer.amazon.com/docs/apps-and-games/sdk-downloads.html)
5. Unzip LoginWithAmazon_Android.zip and copy `login-with-amazon-sdk.jar` from `LoginWithAmazon-3.1.0/lib/` to `sample/libs/`
6. Run the app on an Android device, the emulator shouldn't have bluetooth capabilities.

## Testing with the Sidewalk Sample App
1. The Sidewalk Sample App provides scanning, registration, deregistration, and secure connect capabilities.
2. Click on Login to authenticate, use your Amazon test account. **NOTE:** The Amazon account is required to be linked with a Ring account for Sidewalk functionalities.
3. Scanning is triggered automatically. This will only discover Sidewalk devices in registration mode
4. Press the found device to establish a secure channel or start Sidewalk registration process.
5. There is no visual indication of the registration progress. There will be a pop up with an error or success once the process completes.
6. Logs may be viewed in the Device Console. When complete without errors, the console will say "Registration succeeded: $wirelessDeviceId".

## Security
See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License
This library is licensed under the MIT-0 License. See the [LICENSE](LICENSE) file.

