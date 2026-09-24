# OkHttpSniffer data handling statement

Draft for maintainer review. Based on the source at commit `8c04745`, reviewed on 2026-09-24. Confirm this statement against the final release before publishing it as a policy.

## HTTP data

The companion Android interceptor records request URLs, methods, headers, bodies, response details, and timing information. It writes this data to Android Logcat. The Android Studio plugin reads these entries through ADB and displays them locally.

The reviewed plugin and companion runtime contain no analytics or telemetry endpoint that uploads captured traffic to mobcoding. This does not prevent the app itself from making its intended network requests.

## Sensitive information and retention

Captured headers and bodies are not automatically redacted and may contain passwords, access tokens, cookies, or personal data. Use the interceptor only in debug builds with appropriate test data.

The plugin holds captured requests in memory and stores device/process preferences through the IDE. The Clear action clears the plugin's current request data and views; it does not erase the device's Logcat buffer, exported files, clipboard contents, or records created by other tools.

Copy and export actions place data in the system clipboard or files selected by the user. File retention, IDE history, device logs, and clipboard retention depend on the operating system, IDE, and other software.

## External services

The plugin can open websites or request URLs when the user selects an external-link action. Those destinations apply their own data policies. In the reviewed version, the documentation button opens the OkHttpSniffer GitHub README and the support button opens the OkHttpSniffer GitHub issue tracker.

Installing dependencies from JitPack, Google Maven, or Maven Central involves requests from the build system to those repositories. Installing or updating the IDE plugin also involves the configured plugin repository. These are distinct from displaying captured traffic.

## Contact

For questions, contact the maintainer through https://github.com/mobcoding/OkHttpSniffer/issues. Do not attach unredacted logs or credentials to public issues. A private contact email has not yet been supplied for this draft.
