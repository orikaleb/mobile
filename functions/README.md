# NexiRide Cloud Functions

This folder holds the server-side code that turns Firestore notification
documents into real push notifications on users' phones. The app writes a
document to `notifications/{id}` (from `FirestoreAdminRepository.broadcastNotification`
or from a successful booking) and `pushBroadcastNotification` here fans it
out to every device token registered under
`users/{uid}.fcmTokens` and `drivers/{uid}.fcmTokens`.

## Prerequisites

1. A Firebase **Blaze** (pay-as-you-go) plan is required for Cloud Functions.
   The free tier still applies — you won't be charged for normal broadcast
   volumes.
2. The Firebase CLI:

   ```bash
   npm install -g firebase-tools
   firebase login
   ```

## First-time setup

From the repository root:

```bash
cd functions
npm install
```

## Deploy

Rules and functions are described by the top-level `firebase.json`. Deploy
just the functions (or everything) from the repo root:

```bash
# Only the push fan-out function
firebase deploy --only functions

# Or everything (rules + functions)
firebase deploy
```

After the first successful deploy, any new document created under
`notifications/{id}` with `userId`, `title`, and `message` fields will trigger
a push to every device the user is signed in on.

## Local testing

```bash
cd functions
npm run serve
```

The emulator will pick up documents created in Firestore (point the app at
the emulator or manually add docs in the Firestore UI).

## Troubleshooting

- `Error: HTTP Error: 403, The project ... must be on the Blaze plan`: upgrade
  the project's billing plan in the Firebase console.
- `No FCM tokens for user X`: the user has either never signed in on a device
  since push support was added, or denied the runtime notification permission.
  Ask them to re-open the app and accept the prompt.
