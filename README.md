# amazon-sidewalk-android-samples

## Building the Amazon Sidewalk Sample App
1. Open Amazon Sidewalk Sample App project with Android Studio.
2. Follow the Register for Login with Amazon flow documented on [developer.amazon.com](https://developer.amazon.com/docs/login-with-amazon/register-android.html) to obtain an API Key for your bundle ID and security profile.
3. Replace the content in `sample/src/main/assets/api_key.txt` with the API Key.
4. Download the Login with Amazon SDK from [developer.amazon.com](https://developer.amazon.com/docs/apps-and-games/sdk-downloads.html)
5. Unzip LoginWithAmazon_Android.zip and copy `login-with-amazon-sdk.jar` from `LoginWithAmazon-{latest-version}/lib/` to `sample/libs/`
6. Run the app on an Android device, the emulator shouldn't have bluetooth capabilities.

## Testing with the Amazon Sidewalk Sample App
1. The Amazon Sidewalk Sample App provides scanning, registration, deregistration, and secure connect capabilities.
2. Click on Menu -> Sign In to authenticate, use your Amazon test account.
3. Scanning is triggered automatically. This will only discover Amazon Sidewalk devices in OOBE mode or registered under current logged-in account.
	- For each device in the UNREGISTERED DEVICES section, it shows the device name, Sidewalk Manufacturing Serial Number (SMSN), and RSSI.
	- For each device in the REGISTERED DEVICES section, it shows the device name, Sidewalk ID, and RSSI.
4. Press a found device in the UNREGISTERED DEVICES section, and click register to go through the Amazon Sidewalk registration process.
5. A spinner indicates the registration progress. There will be a pop up with an error or success once the process completes.
6. Press a found device in any section, and click Secure Connect to initiate a secure connection. Upon success, this will take you to the secure connection page. You can also register with a secure connection (preferred if you already have one), which is showcased here.
7. Menu -> Deregister allows you to deregister a device by inputting the device's SMSN.
8. Logs may be viewed in the Device Console.

## Security
See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License
This library is licensed under the MIT-0 License. See the [LICENSE](LICENSE) file.

