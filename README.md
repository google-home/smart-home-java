# Actions on Google: Smart Home sample using Java

This sample contains a fully functioning example of a Smart Home provider cloud
service. This sample is intended to be used together with a Smart Home project
you create in the [Actions console](https://console.actions.google.com), to demonstrate how to integrate
smart devices with the Google Assistant.

This sample includes the following features to help you get started:

- Fulfillment backend for Smart Home Action.
- Mocked user authentication backend which comes prepopulated with sample users.
- Web frontend with an interactive experience for users to control smart devices.

## Get the sample source files

See the developer guide and release notes at [https://developers.google.com/assistant](https://developers.google.com/assistant) for more details.

Clone the project and the included frontend as a subdirectory:

```
git clone https://github.com/actions-on-google/smart-home-java.git
cd smart-home-java
git clone https://github.com/actions-on-google/smart-home-frontend.git
```

## Run the sample

### Set up the Smart Home Action

1. Use the [Actions console](https://console.actions.google.com) to create a new project by clicking **New Project**.
1. Enter a name of your choosing in the text box and click **Create Project**.
1. Select the **Smart Home**, then click **Start Building**.

### Optional: Customize the Action

1. From the top menu under **Develop**, click **Invocation**.
1. Add your Action's name. Click **Save**.
1. From the top menu under **DEPLOY**, click **Directory Information**.
1. Add your Action info, including images, a contact email, and privacy policy. This information can all be edited before submitting for review.
1. Click **Save**.

### Add Request Sync and Report State support
The [Request
Sync](https://developers.google.com/assistant/smarthome/develop/request-sync)
feature allows your cloud integration to send a request to Home Graph to
send a new SYNC request. The [Report
State](https://developers.google.com/assistant/smarthome/develop/report-state)
feature allows your cloud integration to proactively provide the current state of
devices to Home Graph without a `QUERY` request.

1. Navigate to the
[Google Cloud Console API Manager](https://console.developers.google.com/apis)
for your project ID.
1. Enable the [HomeGraph API](https://console.cloud.google.com/apis/api/homegraph.googleapis.com/overview).
1. Navigate to the [Google Cloud Console API & Services page](https://console.cloud.google.com/apis/credentials).
1. Select **Create Credentials** > **Service account**.
    1. Provide a name for the service account and click **Create and continue**.
    1. Select the role **Service Account Token Creator** and click **Continue**.
    1. Click **Done**.
1. Create a key for the service account key account, and download the JSON file.
    1. Click the pencil icon beside the newly created service account.
    1. Select **Keys** > **Add Key** > **Create new key**.
    1. Create JSON key and save the file as `src/main/resources/smart-home-key.json`.

### Connect to Firebase

1. Open your project in the [Firebase console](https://console.firebase.google.com/).
1. Select **Build** > **Firestore database**.
1. Create a new database by clicking **Create database**.
    1. Select the appropriate security rules for your database and click **Next** (We recommend using **test mode** for development, and updating your rules to **production mode** later.).
    1. Select a Firestore location from the dropdown and click **Enable**.
1. From the data tab, select **Start collection** to configure a `users` collection with a default user and a few default fields

```
    users\
        1234
            fakeAccessToken: "123access"
            fakeRefreshToken: "123refresh"
            homegraph: false
```
**Note**: If you are not using Google App Engine to host your server, but still want to
integrate with Firestore:
- Set the `GOOGLE_CLOUD_PROJECT` environment variable to the name of the Firebase project.
- Read [this guide](https://firebase.google.com/docs/admin/setup) on setting up the Firebase Admin SDK.

### Deploy backend to App Engine

1. Run `./gradlew build`
1. Run `./gradlew appengineDeploy`

### Run frontend locally

1. Set up the web frontend

```
cd frontend
npm install
npm run create-firebase-config
npm run serve
```

1. Open the web frontend URL.
1. Add new virtual devices and configure them as you please.
1. Click the cloud icon to enable it for cloud control.

### Start testing

1. Navigate back to the [Actions console](https://console.actions.google.com).
1. From the top menu under **Develop**, click on **Actions** (left nav). Click **Add your first action** and choose your app's language(s).
1. Enter the URL for fulfillment and click **Done**.
    1. If using Google App Engine, the URL will be https://{project-id}.appspot.com/smarthome
1. On the left navigation menu under **ADVANCED OPTIONS**, click on **Account Linking**.
    1. Select **No, I only want to allow account creation on my website**. Click **Next**.
    1. For Linking Type, select **OAuth**.
    1. For Grant Type, select **Authorization Code** for Grant Type.
    1. Under Client Information, enter the client ID and secret as defined below:
        * Client Id: `sampleClientId`
        * Client Secret: `sampleClientSecret`
1. The Authorization URL is the hosted URL of your app with `/fakeauth` as the
path
    1. If using Google App Engine, the URL will be https://{project-id}.appspot.com/fakeauth
1. The Token URL is the hosted URL of your app with `/faketoken` as the path
    1. If using Google App Engine, the URL will be https://{project-id}.appspot.com/faketoken
1. Enter any remaining necessary information you might need for
authentication your app. Click **Save**.
1. On the left navigation menu under **Test**, click on **Simulator**, to begin testing this app.

### Set up account linking

1. On a mobile device with the Google Assistant logged into the same account used
to create the project in the Actions console, enter your Assistant settings.
1. Click Home Control.
1. Click the '+' sign to add a device.
1. Find your app in the list of providers.
1. Log in to your service.
1. Start using the Google Assistant on the mobile device to control your devices. Try saying 'turn my lights on'.

## Get support and report issues
+ Questions? Go to [StackOverflow](https://stackoverflow.com/questions/tagged/google-smart-home), [Assistant Developer Community on Reddit](https://www.reddit.com/r/GoogleAssistantDev/), or [Support](https://developers.google.com/assistant/smarthome/support).
+ For bugs, please report an issue on Github.
+ Smart Home [Documentation](https://developers.google.com/assistant/smarthome/overview)
 
## Contribute
Please read and follow the steps in the [CONTRIBUTING.md](CONTRIBUTING.md).
 
## License
See [LICENSE](LICENSE).
 
## Terms
Your use of this sample is subject to, and by using or downloading the sample
files you agree to comply with the [Google APIs Terms of
Service](https://developers.google.com/terms/).
