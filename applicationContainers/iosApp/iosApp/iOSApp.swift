import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
	// TODO this can be made lazy so instances are not immediately created
    private let permissionDelegate = SwiftLocalNetworkPermissionDelegate()

    init() {
        AppModuleKt.doInitIosApplication(permissionDelegate)
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
