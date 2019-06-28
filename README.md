# Actions on Google: Java Smart Home Sample

This sample to help you get started quickly with the Java smart home library for Actions on Google.

## Setup Instructions

See the developer guide and release notes at https://developers.google.com/actions/ for more details.

### Fulfillment
All smart home intents are located inside the `MySmartHomeApp` class which extends `SmartHomeApp`.
Specifically, there are four intents that must be implemented:

1. onSync
1. onQuery
1. onExecute
1. onDisconnect

### Action configuration

1. Use the [Actions on Google Console](https://console.actions.google.com) to add a new project with a name of your choosing and click **Create Project**.
1. Click **Home Control**, then click **Smart Home**.
1. From the top menu under **Develop**, click on **Invocation** (left nav).
1. Add your App's name. Click **Save**.

Optionally, you can add directory information as well:
1. From the top menu under **Deploy**, click on **Directory Information** (left nav).
1. Add your App info, including images, a contact email and privacy policy. This information can all be edited before submitting for review.
1. Click **Save**.

### Directory Structure
This projects uses a collection of HttpServlets and extends the `SmartHomeApp` class to receive
requests both from the frontend application and the Google Assistant platform.

### Credentials
To fully utilize the features of this project, including the Report State API, you must setup
account credentials.
1. Navigate to the [Google Cloud Console API & Services page](https://console.cloud.google.com/apis/credentials)
2. Be sure you are currently inside your project (view dropdown on the top of the page)
1. Select **Create Credentials** and create a **Service account key**
1. Create the account and download a JSON file.
   Save this as `src/main/webapp/WEB-INF/smart-home-key.json`.


#### Start testing

1. Navigate back to the [Actions on Google Console](https://console.actions.google.com).
1. From the top menu under **Develop**, click on **Actions** (left nav). Click on **Add your first action** and choose your app's language(s).
1. Enter the URL for fulfillment, e.g. https://xyz123.appspot.com/smarthome, click **Done**.
1. From the top menu under **Develop**, click on **Account Linking** (left nav).
1. Select **No, I only want to allow account creation on my website**. Click **Next**.
1. For Linking Type, select **OAuth**.
1. For Grant Type, select 'Authorization Code' for Grant Type.
1. Under Client Information, enter the client ID and secret from earlier.
1. The Authorization URL is the hosted URL of your app with '/fakeauth' as the
path, e.g. https://xyz123.appspot.com/fakeauth
1. The Token URL is the hosted URL of your app with '/faketoken' as the path,
e.g. https://xyz123.appspot.com/faketoken
1. Enter any remaining necessary information you might need for
authentication your app. Click **Save**.
1. On the left navigation menu under **Test**, click on **Simulator**, to begin testing this app.

#### Setup Account linking

1. On a device with the Google Assistant logged into the same account used
to create the project in the Actions Console, enter your Assistant settings.
1. Click Home Control.
1. Click the '+' sign to add a device.
1. Find your app in the list of providers.
1. Log in to your service.
1. Start using the Google Assistant in the Actions Console to control your devices. Try saying 'turn
 my lights on'.
1. (Optionial) You can also follow the setup for a local frontend to test adding and testing devices
 here: git clone https://github.com/actions-on-google/smart-home-frontend.git

Assistant will only provide you control over items that are registered, so if you visit your front
end and click the add icon to create a device your server will receive a
new SYNC command.

### Connect to Firebase

1. Open your project in the Firebase console, and configure a Cloud **Firestore** database.
1. Configure a `users` collection with a default user and a few default fields that match exactly:

```
    users\
        1234
            fakeAccessToken: "123access"
            fakeRefreshToken: "123refresh"
            homegraph: false
```

1. Update the `DATABASE_URL` variable in [`MyDataStore`](src/main/java/com/example/MyDataStore.java)
 with your Firestore database.

**Note**: If you are not using Google App Engine to host your server, but still want to
integrate with Firestore, read [this guide](https://firebase.google.com/docs/admin/setup) on
setting up the Firebase Admin SDK.


### Webhook

When a new project is created using the Actions Console, it also creates a Google Cloud project in the background.
Copy the name of this project from the Action Console project settings page.

#### Build for Google Cloud Platform

   1. Instructions for [Google Cloud App Engine Standard Environment](https://cloud.google.com/appengine/docs/standard/java/)
    1. Use gcloud CLI to set the project to the name of your Actions project. Use 'gcloud init' to initialize and set your Google cloud project to the name of the Actions project.
    1. Deploy to [App Engine using Gradle](https://cloud.google.com/appengine/docs/flexible/java/using-gradle) by running the following command: `gradle appengineDeploy`. You can do this directly from
    IntelliJ by opening the Gradle tray and running the appEngineDeploy task. This will start the process to deploy the fulfillment code to Google Cloud App Engine.


For more detailed information on deployment, see the [documentation](https://developers.google.com/actions/dialogflow/deploy-fulfillment).

## References & Issues
+ Questions? Go to [StackOverflow](https://stackoverflow.com/questions/tagged/actions-on-google), [Assistant Developer Community on Reddit](https://www.reddit.com/r/GoogleAssistantDev/) or [Support](https://developers.google.com/actions/support/).
+ For bugs, please report an issue on Github.
+ Actions on Google [Documentation](https://developers.google.com/actions/extending-the-assistant)
+ Actions on Google [Codelabs](https://codelabs.developers.google.com/?cat=Assistant).

## How to make contributions?
Please read and follow the steps in the [CONTRIBUTING.md](CONTRIBUTING.md).

## License
See [LICENSE](LICENSE).

## Terms
Your use of this sample is subject to, and by using or downloading the sample files you agree to comply with, the [Google APIs Terms of Service](https://developers.google.com/terms/).
