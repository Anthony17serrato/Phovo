import Foundation
import Network
import UIKit
import dnssd
import ComposeApp

/// Triggers and observes the iOS local network permission prompt.
///
/// This type deliberately contains no timers, no timeout and no inference: it reports only the
/// discrete signals the system gives it and invokes its callback at most once. Timeouts,
/// cancellation and serialisation of concurrent probes are handled in Kotlin by
/// `IosPermissionRepository`.
final class SwiftLocalNetworkPermissionDelegate: NSObject, LocalNetworkPermissionDelegate, NetServiceDelegate {

    /// The type the probe *browses*, which is what actually triggers the permission prompt.
    ///
    /// This MUST be a type declared in `NSBonjourServices` in Info.plist. iOS rejects a browse for
    /// an undeclared type outright — the browser fails immediately and no prompt is ever shown.
    /// It is also the capability the app genuinely needs authorized, so browsing it here is honest.
    private static let browseServiceType = "_phovo._tcp"

    /// The type the probe *publishes*, used solely to obtain the grant signal.
    ///
    /// This must NOT be `browseServiceType`: the probe publishes a service nothing is listening
    /// on, and server discovery browses `_phovo._tcp` without filtering, so sharing the type would
    /// make the probe appear as a connectable server on every Phovo device on the network.
    private static let probeServiceType = "_phovolnp._tcp"
    private static let probeServiceName = "PhovoLocalNetworkProbe"
    private static let probeServicePort: Int32 = 1100

    private var browser: NWBrowser?
    private var netService: NetService?
    private var onEvent: ((LocalNetworkProbeEvent) -> Void)?

    // MARK: - LocalNetworkPermissionDelegate

    func startProbe(onEvent: @escaping (LocalNetworkProbeEvent) -> Void) {
        // NetService delivers its delegate callbacks on the run loop that published it, so the
        // probe must always be driven from the main run loop. Kotlin/Native gives no guarantee
        // about which thread calls in, so enforce it here rather than relying on the caller.
        onMain { [weak self] in
            guard let self = self else { return }
            self.teardown()
            self.onEvent = onEvent

            let parameters = NWParameters()
            parameters.includePeerToPeer = true
            let browser = NWBrowser(
                for: .bonjour(type: Self.browseServiceType, domain: nil),
                using: parameters
            )
            self.browser = browser

            browser.stateUpdateHandler = { [weak self] newState in
                guard let self = self else { return }
                switch newState {
                case .failed:
                    self.emit(.failed)
                case .waiting(let error):
                    // `.waiting` is also entered for ordinary transient conditions such as a
                    // missing network, so only an explicit policy denial is terminal here.
                    if self.isPolicyDenial(error) {
                        self.emit(.denied)
                    }
                case .ready, .setup, .cancelled:
                    // `.ready` is NOT an authorization signal: the browser can become ready
                    // before the system has evaluated policy. Publishing is the grant signal.
                    break
                @unknown default:
                    break
                }
            }
            browser.start(queue: .main)

            let netService = NetService(
                domain: "local.",
                type: "\(Self.probeServiceType).",
                name: Self.probeServiceName,
                port: Self.probeServicePort
            )
            self.netService = netService
            netService.delegate = self
            netService.publish()
        }
    }

    func cancelProbe() {
        onMain { [weak self] in
            guard let self = self else { return }
            self.onEvent = nil
            self.teardown()
        }
    }

    // MARK: - NetServiceDelegate

    func netServiceDidPublish(_ sender: NetService) {
        emit(.published)
    }

    func netService(_ sender: NetService, didNotPublish errorDict: [String: NSNumber]) {
        emit(.failed)
    }

    // MARK: - Private

    /// Delivers a terminal event at most once, tearing down first so the callback cannot re-enter.
    private func emit(_ event: LocalNetworkProbeEvent) {
        guard let callback = onEvent else { return }
        onEvent = nil
        teardown()
        callback(event)
    }

    private func isPolicyDenial(_ error: NWError) -> Bool {
        switch error {
        case .dns(let code):
            return code == DNSServiceErrorType(kDNSServiceErr_PolicyDenied)
        case .posix(let code):
            return code == .EACCES
        default:
            return false
        }
    }

    private func teardown() {
        browser?.stateUpdateHandler = nil
        browser?.cancel()
        browser = nil
        netService?.delegate = nil
        netService?.stop()
        netService = nil
    }

    private func onMain(_ work: @escaping () -> Void) {
        if Thread.isMainThread {
            work()
        } else {
            DispatchQueue.main.async(execute: work)
        }
    }

    deinit {
        teardown()
    }
}
