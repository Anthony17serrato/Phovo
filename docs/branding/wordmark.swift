// Renders the Phovo wordmark from the variable Fraunces face at fixed axis values.
// Emits either a transparent PNG or Android VectorDrawable path data.
import Foundation
import CoreText
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

func tag(_ s: String) -> Int {
    var v = 0
    for b in s.utf8 { v = (v << 8) | Int(b) }
    return v
}

struct Args {
    var font = "", text = "Phovo", mode = "png", out = ""
    var width = 1200.0, color = "171D1B", tracking = -0.015
}
var a = Args()
var it = CommandLine.arguments.dropFirst().makeIterator()
while let k = it.next() {
    guard let v = it.next() else { break }
    switch k {
    case "--font": a.font = v
    case "--text": a.text = v
    case "--mode": a.mode = v
    case "--out": a.out = v
    case "--width": a.width = Double(v) ?? a.width
    case "--color": a.color = v
    case "--tracking": a.tracking = Double(v) ?? a.tracking
    default: break
    }
}

// Fraunces at the settings chosen for the wordmark: display optical size, semibold,
// fully soft terminals, wonky alternates on.
let axes: [Int: Double] = [
    tag("opsz"): 144, tag("wght"): 600, tag("SOFT"): 100, tag("WONK"): 1,
]

let size = 512.0
guard let data = NSData(contentsOfFile: a.font) as Data?,
      let provider = CGDataProvider(data: data as CFData),
      let cgFont = CGFont(provider) else { fatalError("cannot read font at \(a.font)") }
let base = CTFontCreateWithGraphicsFont(cgFont, size, nil, nil)
let desc = CTFontDescriptorCreateCopyWithAttributes(
    CTFontCopyFontDescriptor(base),
    [kCTFontVariationAttribute: axes as CFDictionary] as CFDictionary
)
let font = CTFontCreateWithFontDescriptor(desc, size, nil)

let attributed = NSAttributedString(string: a.text, attributes: [
    kCTFontAttributeName as NSAttributedString.Key: font,
    kCTKernAttributeName as NSAttributedString.Key: NSNumber(value: a.tracking * size),
])
let line = CTLineCreateWithAttributedString(attributed)

// Outline every glyph so the mark can be rasterised or emitted as vector paths.
let path = CGMutablePath()
for run in (CTLineGetGlyphRuns(line) as! [CTRun]) {
    let n = CTRunGetGlyphCount(run)
    var glyphs = [CGGlyph](repeating: 0, count: n)
    var pos = [CGPoint](repeating: .zero, count: n)
    CTRunGetGlyphs(run, CFRangeMake(0, n), &glyphs)
    CTRunGetPositions(run, CFRangeMake(0, n), &pos)
    let runFont = (CTRunGetAttributes(run) as NSDictionary)[kCTFontAttributeName] as! CTFont
    for i in 0..<n {
        guard let g = CTFontCreatePathForGlyph(runFont, glyphs[i], nil) else { continue }
        path.addPath(g, transform: CGAffineTransform(translationX: pos[i].x, y: pos[i].y))
    }
}

let box = path.boundingBoxOfPath
let scale = a.width / box.width
let outW = Int((box.width * scale).rounded())
let outH = Int((box.height * scale).rounded())

if a.mode == "png" {
    var r = 0.0, g = 0.0, b = 0.0
    let hex = Int(a.color, radix: 16) ?? 0
    r = Double((hex >> 16) & 0xFF) / 255
    g = Double((hex >> 8) & 0xFF) / 255
    b = Double(hex & 0xFF) / 255

    guard let ctx = CGContext(
        data: nil, width: outW, height: outH, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
    ) else { fatalError("cannot create bitmap context") }
    ctx.setAllowsAntialiasing(true)
    ctx.scaleBy(x: scale, y: scale)
    ctx.translateBy(x: -box.minX, y: -box.minY)
    ctx.setFillColor(CGColor(red: r, green: g, blue: b, alpha: 1))
    ctx.addPath(path)
    ctx.fillPath()

    guard let img = ctx.makeImage(),
          let dest = CGImageDestinationCreateWithURL(
            URL(fileURLWithPath: a.out) as CFURL, UTType.png.identifier as CFString, 1, nil)
    else { fatalError("cannot encode png") }
    CGImageDestinationAddImage(dest, img, nil)
    CGImageDestinationFinalize(dest)
    print("\(a.out) \(outW)x\(outH)")
} else {
    // VectorDrawable coordinates are Y-down, font coordinates are Y-up.
    var d = ""
    func fmt(_ p: CGPoint) -> String {
        let x = (p.x - box.minX) * scale
        let y = (box.maxY - p.y) * scale
        return String(format: "%.2f,%.2f", x, y)
    }
    path.applyWithBlock { el in
        let e = el.pointee
        switch e.type {
        case .moveToPoint: d += "M\(fmt(e.points[0]))"
        case .addLineToPoint: d += "L\(fmt(e.points[0]))"
        case .addQuadCurveToPoint: d += "Q\(fmt(e.points[0])) \(fmt(e.points[1]))"
        case .addCurveToPoint: d += "C\(fmt(e.points[0])) \(fmt(e.points[1])) \(fmt(e.points[2]))"
        case .closeSubpath: d += "Z"
        @unknown default: break
        }
    }
    try! "\(outW)\n\(outH)\n\(d)\n".write(toFile: a.out, atomically: true, encoding: .utf8)
    print("\(a.out) \(outW)x\(outH) path chars: \(d.count)")
}
