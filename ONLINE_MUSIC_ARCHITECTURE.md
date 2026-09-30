# Online music integration — architecture and licensing boundary

## Status

This change establishes an independently authored provider contract only. It
does **not** add a YouTube Music implementation, Google sign-in, stream
extraction, or UI. No DA Tunes source files or implementation details are
copied into this repository.

## Licensing boundary

- The repository's existing `LICENSE` remains unchanged.
- Existing proprietary and third-party contributions are not relicensed.
- New code in `app/src/main/java/com/theveloper/pixelplay/online/OnlineMusicProvider.kt`
  is independently authored for this repository and does not incorporate
  DA Tunes code.
- Do not copy GPL-licensed DA Tunes files, snippets, algorithms, or generated
  artifacts into this project without the required permissions and a separate
  licensing review.
- Review the license and notice obligations of every future dependency before
  adding it. This document is engineering guidance, not legal advice.

## Design

`OnlineMusicProvider` is a provider-neutral boundary. A future provider can
implement account state, catalog search, playlists, and playback resolution
without coupling the app's UI or Media3 layer to a particular service.

The app should adapt a resolved, authorized playback item into its existing
Media3 playback pipeline. Provider credentials must never be logged or placed
in source control. Persist tokens only using an approved secure storage
mechanism, request the minimum necessary scopes, and support sign-out/revocation.

## Authentication and service constraints

Google account authentication is not itself permission to access YouTube Music
catalog data or stream audio. Before implementing a service adapter, verify
that the relevant official API/interface supports the required operation and
that the app's use complies with the provider's terms. Do not implement
unofficial scraping, cookie harvesting, or stream URL extraction.

If no supported, authorized playback interface is available, keep playback
resolution unavailable and present that limitation in the UI rather than
bypassing access controls.

## Suggested implementation sequence

1. Confirm the target provider's documented API, allowed scopes, and playback
   rights.
2. Implement an adapter in a separate package, with tests and no dependency on
   DA Tunes.
3. Add secure account lifecycle handling and explicit user consent.
4. Adapt authorized playback items to the existing Media3 service.
5. Add catalog/library UI only after the adapter is testable.
6. Audit transitive dependencies, notices, logging, and distribution terms.
