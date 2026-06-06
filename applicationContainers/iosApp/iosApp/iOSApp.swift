import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        AppModuleKt.doInitApplication()
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
