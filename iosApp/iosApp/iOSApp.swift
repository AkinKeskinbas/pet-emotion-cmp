import SwiftUI
import ComposeApp
@main
struct iOSApp: App {
	init() {
		// Configure RevenueCat with error handling for sandbox/simulator environments
		do {
			RevenueCatInit.shared.configure(apiKey: PlatformKeys.shared.revenuecatApiKey,appUserId: nil)
		} catch {
			// RevenueCat errors are non-critical, app should continue working
			print("RevenueCat initialization warning (non-critical): \(error.localizedDescription)")
		}
	}
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
