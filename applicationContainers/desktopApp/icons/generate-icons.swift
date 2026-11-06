// Regenerates the desktop packaging icons from the Phovo logo.
//
//   swift applicationContainers/desktopApp/icons/generate-icons.swift
//
// Run from the repository root on macOS (needs AppKit and iconutil). Writes:
//   phovo.icns  macOS: logo on a white rounded tile following Apple's 1024 grid, so
//               macOS 26 does not box it in a grey tile like a legacy icon
//   phovo.ico   Windows: transparent logo, 16-256 px PNG entries
//   phovo.png   Linux: transparent logo, 512 px
//   phovo_window_icon.png  window/taskbar icon set at runtime (Windows and Linux use it)
import AppKit
import Foundation

let source = "docs/images/phovo_icon.png"
let outDir = "applicationContainers/desktopApp/icons"
let windowIconOut = "applicationContainers/desktopApp/src/commonMain/composeResources/drawable/phovo_window_icon.png"

guard let logo = NSImage(contentsOfFile: source) else {
    fatalError("Cannot read \(source); run from the repository root")
}

func render(_ size: Int, _ draw: (CGContext, CGFloat) -> Void) -> Data {
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size,
        bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
        colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
    )!
    NSGraphicsContext.saveGraphicsState()
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!
    ctx.imageInterpolation = .high
    NSGraphicsContext.current = ctx
    draw(ctx.cgContext, CGFloat(size))
    NSGraphicsContext.restoreGraphicsState()
    return rep.representation(using: .png, properties: [:])!
}

/// Transparent logo with a small margin so it does not touch the icon edge.
func flatIcon(_ size: Int) -> Data { render(size) { _, s in
    let inset = s * 0.04
    logo.draw(in: NSRect(x: inset, y: inset, width: s - 2 * inset, height: s - 2 * inset))
} }

/// Logo on a white rounded tile: 824/1024 tile, 185/1024 corner radius, per Apple's macOS grid.
func macIcon(_ size: Int) -> Data { render(size) { cg, s in
    let u = s / 1024
    let tile = NSRect(x: 100 * u, y: 100 * u, width: 824 * u, height: 824 * u)
    let path = NSBezierPath(roundedRect: tile, xRadius: 185 * u, yRadius: 185 * u)
    cg.saveGState()
    cg.setShadow(offset: CGSize(width: 0, height: -10 * u), blur: 20 * u,
                 color: NSColor.black.withAlphaComponent(0.3).cgColor)
    NSColor.white.setFill()
    path.fill()
    cg.restoreGState()
    let logoSide = 640 * u
    logo.draw(in: NSRect(x: (s - logoSide) / 2, y: (s - logoSide) / 2, width: logoSide, height: logoSide))
} }

func write(_ data: Data, _ path: String) {
    try! data.write(to: URL(fileURLWithPath: path))
    print("wrote \(path)")
}

// macOS .icns via iconutil
let iconset = NSTemporaryDirectory() + "phovo.iconset"
try? FileManager.default.removeItem(atPath: iconset)
try! FileManager.default.createDirectory(atPath: iconset, withIntermediateDirectories: true)
for base in [16, 32, 128, 256, 512] {
    write(macIcon(base), "\(iconset)/icon_\(base)x\(base).png")
    write(macIcon(base * 2), "\(iconset)/icon_\(base)x\(base)@2x.png")
}
let iconutil = Process()
iconutil.executableURL = URL(fileURLWithPath: "/usr/bin/iconutil")
iconutil.arguments = ["-c", "icns", iconset, "-o", "\(outDir)/phovo.icns"]
try! iconutil.run()
iconutil.waitUntilExit()
precondition(iconutil.terminationStatus == 0, "iconutil failed")
print("wrote \(outDir)/phovo.icns")

// Windows .ico with PNG-compressed entries (supported since Vista)
let icoSizes = [16, 24, 32, 48, 64, 128, 256]
let images = icoSizes.map(flatIcon)
var ico = Data()
func le16(_ v: Int) { ico.append(contentsOf: [UInt8(v & 0xff), UInt8((v >> 8) & 0xff)]) }
func le32(_ v: Int) { le16(v & 0xffff); le16((v >> 16) & 0xffff) }
le16(0); le16(1); le16(icoSizes.count)
var offset = 6 + 16 * icoSizes.count
for (size, png) in zip(icoSizes, images) {
    ico.append(UInt8(size % 256)) // 0 means 256
    ico.append(UInt8(size % 256))
    ico.append(0); ico.append(0)
    le16(1); le16(32)
    le32(png.count); le32(offset)
    offset += png.count
}
images.forEach { ico.append($0) }
write(ico, "\(outDir)/phovo.ico")

// Linux .png and the runtime window icon
write(flatIcon(512), "\(outDir)/phovo.png")
write(flatIcon(256), windowIconOut)
