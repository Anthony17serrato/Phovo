// Rebuilds the desktop AWT splash image: rounded card, icon, wordmark beneath it.
// The JVM -splash: flag takes one flat PNG, so the lockup has to be baked in.
import Foundation
import CoreText
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers

func tag(_ s: String) -> Int { var v = 0; for b in s.utf8 { v = (v << 8) | Int(b) }; return v }

let fontPath = CommandLine.arguments[1]
let iconPath = CommandLine.arguments[2]
let outPath  = CommandLine.arguments[3]
let s        = Double(CommandLine.arguments[4])!   // 1 or 2
let inkHex   = Int(CommandLine.arguments[5], radix: 16)!

// Layout in @1x pixels, matching the existing 864x486 card.
let W = 864.0, H = 486.0, radius = 20.0
let bg = (r: 0xF5 / 255.0, g: 0xF5 / 255.0, b: 0xF3 / 255.0)
let ink = (r: Double((inkHex >> 16) & 0xFF) / 255.0,
           g: Double((inkHex >> 8) & 0xFF) / 255.0,
           b: Double(inkHex & 0xFF) / 255.0)
let iconH = 240.0, gap = 28.0, markW = 200.0

// --- wordmark outlines -------------------------------------------------------
guard let fdata = NSData(contentsOfFile: fontPath) as Data?,
      let provider = CGDataProvider(data: fdata as CFData),
      let cgFont = CGFont(provider) else { fatalError("font") }
let baseFont = CTFontCreateWithGraphicsFont(cgFont, 512, nil, nil)
let desc = CTFontDescriptorCreateCopyWithAttributes(
    CTFontCopyFontDescriptor(baseFont),
    [kCTFontVariationAttribute: [tag("opsz"): 144, tag("wght"): 600,
                                 tag("SOFT"): 100, tag("WONK"): 1] as CFDictionary] as CFDictionary)
let font = CTFontCreateWithFontDescriptor(desc, 512, nil)
let line = CTLineCreateWithAttributedString(NSAttributedString(string: "Phovo", attributes: [
    kCTFontAttributeName as NSAttributedString.Key: font,
    kCTKernAttributeName as NSAttributedString.Key: NSNumber(value: -0.015 * 512),
]))
let markPath = CGMutablePath()
for run in (CTLineGetGlyphRuns(line) as! [CTRun]) {
    let n = CTRunGetGlyphCount(run)
    var glyphs = [CGGlyph](repeating: 0, count: n), pos = [CGPoint](repeating: .zero, count: n)
    CTRunGetGlyphs(run, CFRangeMake(0, n), &glyphs)
    CTRunGetPositions(run, CFRangeMake(0, n), &pos)
    let rf = (CTRunGetAttributes(run) as NSDictionary)[kCTFontAttributeName] as! CTFont
    for i in 0..<n {
        if let g = CTFontCreatePathForGlyph(rf, glyphs[i], nil) {
            markPath.addPath(g, transform: CGAffineTransform(translationX: pos[i].x, y: pos[i].y))
        }
    }
}
let markBox = markPath.boundingBoxOfPath
let markH = markW * markBox.height / markBox.width

// --- icon, measured by its ink so padding in the source never shifts the layout ---
let iconImg = CGImageSourceCreateImageAtIndex(
    CGImageSourceCreateWithURL(URL(fileURLWithPath: iconPath) as CFURL, nil)!, 0, nil)!
let iw = iconImg.width, ih = iconImg.height
var ibuf = [UInt8](repeating: 0, count: iw * ih * 4)
let ictx = CGContext(data: &ibuf, width: iw, height: ih, bitsPerComponent: 8, bytesPerRow: iw * 4,
                     space: CGColorSpaceCreateDeviceRGB(),
                     bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
ictx.draw(iconImg, in: CGRect(x: 0, y: 0, width: iw, height: ih))
var minX = iw, maxX = -1, minY = ih, maxY = -1
for y in 0..<ih {
    for x in 0..<iw where ibuf[(y * iw + x) * 4 + 3] > 8 {
        if x < minX { minX = x }; if x > maxX { maxX = x }
        if y < minY { minY = y }; if y > maxY { maxY = y }
    }
}
let inkW = Double(maxX - minX + 1), inkH = Double(maxY - minY + 1)
let iconScale = iconH / inkH
let iconW = inkW * iconScale

// --- compose -----------------------------------------------------------------
let pxW = Int(W * s), pxH = Int(H * s)
let ctx = CGContext(data: nil, width: pxW, height: pxH, bitsPerComponent: 8, bytesPerRow: 0,
                    space: CGColorSpaceCreateDeviceRGB(),
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
ctx.scaleBy(x: s, y: s)
ctx.setAllowsAntialiasing(true)
ctx.interpolationQuality = .high

ctx.addPath(CGPath(roundedRect: CGRect(x: 0, y: 0, width: W, height: H),
                   cornerWidth: radius, cornerHeight: radius, transform: nil))
ctx.setFillColor(CGColor(red: bg.r, green: bg.g, blue: bg.b, alpha: 1))
ctx.fillPath()

let blockH = iconH + gap + markH
let topY = (H - blockH) / 2                      // distance from the top of the card

// CoreGraphics is Y-up; convert from the top-down layout above.
let iconRectY = H - topY - iconH
ctx.saveGState()
ctx.translateBy(x: (W - iconW) / 2, y: iconRectY)
ctx.scaleBy(x: iconScale, y: iconScale)
ctx.translateBy(x: -Double(minX), y: -Double(ih - 1 - maxY))
ctx.draw(iconImg, in: CGRect(x: 0, y: 0, width: Double(iw), height: Double(ih)))
ctx.restoreGState()

let markScale = markW / markBox.width
ctx.saveGState()
ctx.translateBy(x: (W - markW) / 2, y: iconRectY - gap - markH)
ctx.scaleBy(x: markScale, y: markScale)
ctx.translateBy(x: -markBox.minX, y: -markBox.minY)
ctx.setFillColor(CGColor(red: ink.r, green: ink.g, blue: ink.b, alpha: 1))
ctx.addPath(markPath)
ctx.fillPath()
ctx.restoreGState()

let out = CGImageDestinationCreateWithURL(
    URL(fileURLWithPath: outPath) as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(out, ctx.makeImage()!, nil)
CGImageDestinationFinalize(out)
print("\(outPath) \(pxW)x\(pxH) icon ink \(Int(inkW))x\(Int(inkH)) -> \(Int(iconW))x\(Int(iconH)), mark \(Int(markW))x\(Int(markH))")
