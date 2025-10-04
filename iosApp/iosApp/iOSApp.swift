import SwiftUI
import ComposeApp
@main
struct iOSApp: App {
	init() {
		// Initialize backend and RevenueCat synchronously
		// This blocks until initialization is complete to ensure RevenueCat is ready
		print("Swift: Calling blocking initialization...")
		IOSAppKt.initializeIOSAppBlocking()
		print("Swift: Initialization complete, showing UI")
	}
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
